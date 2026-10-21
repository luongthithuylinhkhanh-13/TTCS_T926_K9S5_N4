package com.ntdhtcct.domain.milestone;

import com.ntdhtcct.domain.project.Project;
import com.ntdhtcct.domain.project.ProjectRepository;
import com.ntdhtcct.domain.wbs.WbsItem;
import com.ntdhtcct.domain.wbs.WbsItemRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.time.LocalDate;

@Component
@Profile("!test")
@Order(7)
public class MilestoneDemoSeeder implements CommandLineRunner {

    private final ProjectRepository projectRepository;
    private final MilestoneRepository milestoneRepository;
    private final WbsItemRepository wbsItemRepository;

    public MilestoneDemoSeeder(
            ProjectRepository projectRepository,
            MilestoneRepository milestoneRepository,
            WbsItemRepository wbsItemRepository
    ) {
        this.projectRepository = projectRepository;
        this.milestoneRepository = milestoneRepository;
        this.wbsItemRepository = wbsItemRepository;
    }

    @Override
    public void run(String... args) {
        Project project = projectRepository.findByCode("DA-001").orElse(null);
        if (project == null) {
            return;
        }

        if (!milestoneRepository.findByProjectIdOrderByTargetDateAsc(project.getId()).isEmpty()) {
            return;
        }

        WbsItem phase12 = wbsItemRepository.findByProjectIdAndWbsCode(project.getId(), "1.2")
                .or(() -> wbsItemRepository.findByProjectIdAndWbsCode(project.getId(), "1.0"))
                .orElse(null);

        WbsItem phase13 = wbsItemRepository.findByProjectIdAndWbsCode(project.getId(), "1.3")
                .or(() -> wbsItemRepository.findByProjectIdAndWbsCode(project.getId(), "1.0"))
                .orElse(null);

        if (phase12 == null) {
            return;
        }

        // 1. Mốc kích hoạt cảnh báo trễ hạn (Overrun Milestone)
        Milestone m1 = new Milestone(
                project.getId(),
                phase12,
                "Hoàn thành phần móng và cọc D400",
                LocalDate.of(2026, 9, 20),
                "Nghiệm thu toàn bộ cọc ly tâm D400 và kết cấu đài giằng móng trước mùa mưa bão."
        );
        milestoneRepository.save(m1);

        // 2. Mốc đúng hạn an toàn
        if (phase13 != null) {
            Milestone m2 = new Milestone(
                    project.getId(),
                    phase13,
                    "Cất nóc kết cấu sàn tầng 1",
                    LocalDate.of(2026, 11, 20),
                    "Hoàn thành đổ bê tông sàn tầng 1 và bảo dưỡng đạt cường độ R28."
            );
            milestoneRepository.save(m2);
        }
    }
}
