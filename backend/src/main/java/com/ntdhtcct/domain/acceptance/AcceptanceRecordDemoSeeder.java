package com.ntdhtcct.domain.acceptance;

import com.ntdhtcct.domain.project.Project;
import com.ntdhtcct.domain.project.ProjectRepository;
import com.ntdhtcct.entity.User;
import com.ntdhtcct.repository.UserRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.List;

@Component
@Order(5)
public class AcceptanceRecordDemoSeeder implements CommandLineRunner {

    private static final String PROJECT_CODE = "DA-001";
    private static final String ADMIN_EMAIL = "lanc5676@gmail.com";
    private static final String FALLBACK_EMAIL = "test@test.com";

    private final ProjectRepository projectRepository;
    private final AcceptanceRecordRepository recordRepository;
    private final AcceptanceRecordAuditLogRepository auditLogRepository;
    private final UserRepository userRepository;

    public AcceptanceRecordDemoSeeder(
            ProjectRepository projectRepository,
            AcceptanceRecordRepository recordRepository,
            AcceptanceRecordAuditLogRepository auditLogRepository,
            UserRepository userRepository
    ) {
        this.projectRepository = projectRepository;
        this.recordRepository = recordRepository;
        this.auditLogRepository = auditLogRepository;
        this.userRepository = userRepository;
    }

    @Override
    public void run(String... args) {
        Project project = projectRepository.findByCode(PROJECT_CODE).orElse(null);
        User actor = userRepository.findByEmailIgnoreCase(ADMIN_EMAIL)
                .or(() -> userRepository.findByEmailIgnoreCase(FALLBACK_EMAIL))
                .orElse(null);
        if (project == null || actor == null) {
            return;
        }

        seed(
                project,
                actor,
                "Nghiệm thu phần móng",
                LocalDate.of(2026, 10, 2),
                "Nghiệm thu cọc, cốt thép và bê tông phần móng.",
                AcceptanceRecordStatus.APPROVED,
                List.of(new AuditSeed(AcceptanceRecordAction.CREATE, null),
                        new AuditSeed(AcceptanceRecordAction.EDIT, null),
                        new AuditSeed(AcceptanceRecordAction.APPROVE, null))
        );
        seed(
                project,
                actor,
                "Nghiệm thu công tác cốt thép tầng 1",
                LocalDate.of(2026, 10, 9),
                "Kiểm tra lắp đặt cốt thép dầm, sàn tầng 1 theo bản vẽ được duyệt.",
                AcceptanceRecordStatus.RETURNED,
                List.of(new AuditSeed(AcceptanceRecordAction.CREATE, null),
                        new AuditSeed(AcceptanceRecordAction.RETURN,
                                "Bổ sung biên bản kiểm tra đường kính và khoảng cách cốt thép."))
        );
        seed(
                project,
                actor,
                "Nghiệm thu hệ thống điện tạm công trường",
                LocalDate.of(2026, 10, 13),
                "Kiểm tra tủ điện, dây dẫn và thiết bị bảo vệ trước khi đưa vào sử dụng.",
                AcceptanceRecordStatus.PENDING_APPROVAL,
                List.of(new AuditSeed(AcceptanceRecordAction.CREATE, null))
        );
    }

    private void seed(
            Project project,
            User actor,
            String title,
            LocalDate inspectionDate,
            String description,
            AcceptanceRecordStatus status,
            List<AuditSeed> auditSeeds
    ) {
        AcceptanceRecord record = recordRepository
                .findByProjectIdAndTitle(project.getId(), title)
                .orElseGet(() -> recordRepository.save(new AcceptanceRecord(
                        project.getId(), title, inspectionDate, description
                )));
        String actorName = actor.getFullName();
        if (actorName == null || actorName.isBlank()) {
            actorName = actor.getEmail();
        }

        if (auditLogRepository.findByAcceptanceRecordIdOrderByPerformedAtAscIdAsc(record.getId()).isEmpty()) {
            for (AuditSeed auditSeed : auditSeeds) {
                auditLogRepository.save(new AcceptanceRecordAuditLog(
                        record.getId(),
                        actor.getId(),
                        actorName,
                        auditSeed.action(),
                        auditSeed.comment()
                ));
            }
        }

        if (record.getStatus() != status) {
            record.setStatus(status);
            recordRepository.save(record);
        }
    }

    private record AuditSeed(AcceptanceRecordAction action, String comment) {
    }
}
