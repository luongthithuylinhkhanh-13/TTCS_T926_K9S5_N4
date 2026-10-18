package com.ntdhtcct.domain.acceptance;

import com.ntdhtcct.auth.service.AuthorizationService;
import com.ntdhtcct.common.exception.ResourceNotFoundException;
import com.ntdhtcct.domain.acceptance.dto.AcceptanceDossierItemResponse;
import com.ntdhtcct.domain.acceptance.dto.AcceptanceDossierResponse;
import com.ntdhtcct.domain.project.Project;
import com.ntdhtcct.domain.project.ProjectRepository;
import com.ntdhtcct.domain.wbs.WbsItem;
import com.ntdhtcct.domain.wbs.WbsItemRepository;
import com.ntdhtcct.entity.User;
import com.ntdhtcct.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class AcceptanceDossierService {

    private static final String[] REPORT_VIEW_ROLES = {
            "ADMIN", "PROJECT_MANAGER", "SITE_ENGINEER", "WORKER", "VIEWER"
    };

    private final AcceptanceReportRepository reportRepository;
    private final AcceptanceRecordRepository recordRepository;
    private final AcceptanceRecordAuditLogRepository auditLogRepository;
    private final ProjectRepository projectRepository;
    private final WbsItemRepository wbsItemRepository;
    private final UserRepository userRepository;
    private final AuthorizationService authorizationService;

    public AcceptanceDossierService(
            AcceptanceReportRepository reportRepository,
            AcceptanceRecordRepository recordRepository,
            AcceptanceRecordAuditLogRepository auditLogRepository,
            ProjectRepository projectRepository,
            WbsItemRepository wbsItemRepository,
            UserRepository userRepository,
            AuthorizationService authorizationService
    ) {
        this.reportRepository = reportRepository;
        this.recordRepository = recordRepository;
        this.auditLogRepository = auditLogRepository;
        this.projectRepository = projectRepository;
        this.wbsItemRepository = wbsItemRepository;
        this.userRepository = userRepository;
        this.authorizationService = authorizationService;
    }

    @Transactional(readOnly = true)
    public AcceptanceDossierResponse getDossier(UUID reportId, UUID userId) {
        AcceptanceReport report = reportRepository.findById(reportId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "ACCEPTANCE_REPORT_NOT_FOUND",
                        "Không tìm thấy phiếu nghiệm thu"
                ));

        authorizationService.checkProjectAccess(report.getProjectId(), userId, REPORT_VIEW_ROLES);

        Project project = projectRepository.findById(report.getProjectId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "PROJECT_NOT_FOUND",
                        "Không tìm thấy công trình"
                ));

        Map<UUID, WbsItem> wbsItems = wbsItemRepository
                .findByProjectIdOrderByWbsCodeAsc(project.getId())
                .stream()
                .collect(Collectors.toMap(WbsItem::getId, item -> item));

        List<AcceptanceDossierItemResponse> items = report.getItems().stream()
                .map(item -> {
                    WbsItem wbsItem = item.getWbsItemId() == null
                            ? null
                            : wbsItems.get(item.getWbsItemId());
                    return new AcceptanceDossierItemResponse(
                            item.getId(),
                            item.getItemName(),
                            wbsItem == null ? null : wbsItem.getWbsCode(),
                            wbsItem == null ? null : getWbsLocation(wbsItem, wbsItems),
                            item.getUnit(),
                            item.getPlannedQuantity(),
                            item.getAcceptedQuantity(),
                            item.getCumulativeQuantity(),
                            item.getUnitPrice(),
                            item.getAmount(),
                            item.getNotes()
                    );
                })
                .toList();

        User creator = report.getCreatorId() == null
                ? null
                : userRepository.findById(report.getCreatorId()).orElse(null);
        ApprovalDetails approval = getApprovalDetails(report);
        String result = resolveResult(report.getStatus(), approval.status());

        return new AcceptanceDossierResponse(
                report.getId(),
                project.getId(),
                project.getCode(),
                project.getName(),
                report.getReportNumber(),
                report.getName(),
                report.getPeriodNumber(),
                report.getAcceptanceDate(),
                report.getLocation(),
                result,
                report.getStatus(),
                report.getNotes(),
                report.getOwnerRepresentativeName(),
                report.getSupervisorRepresentativeName(),
                report.getContractorRepresentativeName(),
                creator == null ? null : displayName(creator),
                report.getTotalAmount(),
                items,
                approval.recordId(),
                approval.title(),
                approval.description(),
                approval.status(),
                approval.approverName(),
                approval.comment(),
                approval.approvedAt()
        );
    }

    private ApprovalDetails getApprovalDetails(AcceptanceReport report) {
        UUID recordId = report.getAcceptanceRecordId();
        if (recordId == null) {
            return ApprovalDetails.empty();
        }

        AcceptanceRecord record = recordRepository.findByIdAndProjectId(recordId, report.getProjectId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "ACCEPTANCE_RECORD_NOT_FOUND",
                        "Phiếu duyệt S-39 liên kết với biên bản này không còn tồn tại"
                ));

        List<AcceptanceRecordAuditLog> auditLogs = auditLogRepository
                .findByAcceptanceRecordIdOrderByPerformedAtAscIdAsc(recordId);
        AcceptanceRecordAction expectedAction = record.getStatus() == AcceptanceRecordStatus.APPROVED
                ? AcceptanceRecordAction.APPROVE
                : record.getStatus() == AcceptanceRecordStatus.RETURNED
                        ? AcceptanceRecordAction.RETURN
                        : null;

        AcceptanceRecordAuditLog outcome = expectedAction == null
                ? null
                : auditLogs.stream()
                        .filter(log -> log.getAction() == expectedAction)
                        .reduce((first, second) -> second)
                        .orElse(null);

        return new ApprovalDetails(
                record.getId(),
                record.getTitle(),
                record.getDescription(),
                record.getStatus().name(),
                outcome == null ? null : outcome.getActorName(),
                outcome == null ? null : outcome.getComment(),
                outcome == null ? null : outcome.getPerformedAt()
        );
    }

    private String resolveResult(String reportStatus, String approvalStatus) {
        if ("APPROVED".equalsIgnoreCase(approvalStatus)) {
            return "Đạt";
        }
        if ("RETURNED".equalsIgnoreCase(approvalStatus)) {
            return "Không đạt";
        }
        if ("APPROVED".equalsIgnoreCase(reportStatus)) {
            return "Đạt";
        }
        if ("REJECTED".equalsIgnoreCase(reportStatus)) {
            return "Không đạt";
        }
        return "Chờ nghiệm thu";
    }

    private String getWbsLocation(WbsItem item, Map<UUID, WbsItem> wbsItems) {
        List<String> path = new ArrayList<>();
        Set<UUID> visited = new HashSet<>();
        WbsItem current = item;
        while (current != null && visited.add(current.getId())) {
            String label = current.getWbsCode() == null || current.getWbsCode().isBlank()
                    ? current.getName()
                    : current.getWbsCode() + " - " + current.getName();
            path.add(0, label);
            current = current.getParentId() == null ? null : wbsItems.get(current.getParentId());
        }
        return String.join(" / ", path);
    }

    private String displayName(User user) {
        return user.getFullName() == null || user.getFullName().isBlank()
                ? user.getEmail()
                : user.getFullName();
    }

    private record ApprovalDetails(
            UUID recordId,
            String title,
            String description,
            String status,
            String approverName,
            String comment,
            java.time.OffsetDateTime approvedAt
    ) {
        private static ApprovalDetails empty() {
            return new ApprovalDetails(null, null, null, null, null, null, null);
        }
    }
}
