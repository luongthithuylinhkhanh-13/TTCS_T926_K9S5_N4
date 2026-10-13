package com.ntdhtcct.domain.acceptance;

import com.ntdhtcct.auth.context.UserSecurityContext;
import com.ntdhtcct.common.exception.BadRequestException;
import com.ntdhtcct.common.exception.ResourceNotFoundException;
import com.ntdhtcct.common.exception.UnauthorizedException;
import com.ntdhtcct.domain.acceptance.dto.AcceptanceRecordRequest;
import com.ntdhtcct.entity.User;
import com.ntdhtcct.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
public class AcceptanceRecordService {

    private final AcceptanceRecordRepository recordRepository;
    private final AcceptanceRecordAuditLogRepository auditLogRepository;
    private final UserRepository userRepository;

    public AcceptanceRecordService(
            AcceptanceRecordRepository recordRepository,
            AcceptanceRecordAuditLogRepository auditLogRepository,
            UserRepository userRepository
    ) {
        this.recordRepository = recordRepository;
        this.auditLogRepository = auditLogRepository;
        this.userRepository = userRepository;
    }

    @Transactional(readOnly = true)
    public List<AcceptanceRecord> getRecords(UUID projectId) {
        return recordRepository.findByProjectIdOrderByUpdatedAtDesc(projectId);
    }

    @Transactional(readOnly = true)
    public AcceptanceRecord getRecord(UUID projectId, UUID recordId) {
        return findRecord(projectId, recordId);
    }

    @Transactional(readOnly = true)
    public List<AcceptanceRecordAuditLog> getHistory(UUID projectId, UUID recordId) {
        AcceptanceRecord record = findRecord(projectId, recordId);
        return auditLogRepository.findByAcceptanceRecordIdOrderByPerformedAtAscIdAsc(record.getId());
    }

    @Transactional
    public AcceptanceRecord create(UUID projectId, AcceptanceRecordRequest request) {
        AcceptanceRecord record = new AcceptanceRecord(
                projectId,
                request.title().trim(),
                request.inspectionDate(),
                request.description().trim()
        );
        AcceptanceRecord saved = recordRepository.save(record);
        appendAuditLog(saved, AcceptanceRecordAction.CREATE, null);
        return saved;
    }

    @Transactional
    public AcceptanceRecord update(UUID projectId, UUID recordId, AcceptanceRecordRequest request) {
        AcceptanceRecord record = findRecord(projectId, recordId);
        if (record.getStatus() == AcceptanceRecordStatus.APPROVED) {
            throw new BadRequestException(
                    "ACCEPTANCE_RECORD_APPROVED",
                    "Không thể chỉnh sửa phiếu nghiệm thu đã được duyệt"
            );
        }
        record.setTitle(request.title().trim());
        record.setInspectionDate(request.inspectionDate());
        record.setDescription(request.description().trim());
        if (record.getStatus() == AcceptanceRecordStatus.RETURNED) {
            record.setStatus(AcceptanceRecordStatus.PENDING_APPROVAL);
        }
        AcceptanceRecord saved = recordRepository.save(record);
        appendAuditLog(saved, AcceptanceRecordAction.EDIT, null);
        return saved;
    }

    @Transactional
    public AcceptanceRecord approve(UUID projectId, UUID recordId) {
        AcceptanceRecord record = findRecord(projectId, recordId);
        requirePendingApproval(record);
        record.setStatus(AcceptanceRecordStatus.APPROVED);
        AcceptanceRecord saved = recordRepository.save(record);
        appendAuditLog(saved, AcceptanceRecordAction.APPROVE, null);
        return saved;
    }

    @Transactional
    public AcceptanceRecord returnForChanges(UUID projectId, UUID recordId, String comment) {
        AcceptanceRecord record = findRecord(projectId, recordId);
        requirePendingApproval(record);
        record.setStatus(AcceptanceRecordStatus.RETURNED);
        AcceptanceRecord saved = recordRepository.save(record);
        appendAuditLog(saved, AcceptanceRecordAction.RETURN, comment == null ? null : comment.trim());
        return saved;
    }

    private AcceptanceRecord findRecord(UUID projectId, UUID recordId) {
        return recordRepository.findByIdAndProjectId(recordId, projectId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "ACCEPTANCE_RECORD_NOT_FOUND",
                        "Không tìm thấy phiếu nghiệm thu trong công trình này"
                ));
    }

    private void requirePendingApproval(AcceptanceRecord record) {
        if (record.getStatus() != AcceptanceRecordStatus.PENDING_APPROVAL) {
            throw new BadRequestException(
                    "ACCEPTANCE_RECORD_NOT_PENDING",
                    "Chỉ có thể duyệt hoặc trả lại phiếu đang chờ duyệt"
            );
        }
    }

    private void appendAuditLog(
            AcceptanceRecord record,
            AcceptanceRecordAction action,
            String comment
    ) {
        UUID actorId = UserSecurityContext.getUserId();
        if (actorId == null) {
            throw new UnauthorizedException(
                    "AUDIT_ACTOR_MISSING",
                    "Không xác định được người thực hiện thao tác"
            );
        }
        User actor = userRepository.findById(actorId)
                .orElseThrow(() -> new UnauthorizedException(
                        "AUDIT_ACTOR_NOT_FOUND",
                        "Không tìm thấy tài khoản người thực hiện thao tác"
                ));
        String actorName = actor.getFullName();
        if (actorName == null || actorName.isBlank()) {
            actorName = actor.getEmail();
        }
        auditLogRepository.save(new AcceptanceRecordAuditLog(
                record.getId(),
                actorId,
                actorName,
                action,
                comment
        ));
    }
}
