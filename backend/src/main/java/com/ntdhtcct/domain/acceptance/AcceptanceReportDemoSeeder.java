package com.ntdhtcct.domain.acceptance;

import com.ntdhtcct.domain.project.Project;
import com.ntdhtcct.domain.project.ProjectRepository;
import com.ntdhtcct.domain.wbs.WbsItem;
import com.ntdhtcct.domain.wbs.WbsItemRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDate;

@Component
@Order(8)
public class AcceptanceReportDemoSeeder implements CommandLineRunner {

    private final ProjectRepository projectRepository;
    private final AcceptanceReportRepository reportRepository;
    private final WbsItemRepository wbsItemRepository;

    public AcceptanceReportDemoSeeder(
            ProjectRepository projectRepository,
            AcceptanceReportRepository reportRepository,
            WbsItemRepository wbsItemRepository
    ) {
        this.projectRepository = projectRepository;
        this.reportRepository = reportRepository;
        this.wbsItemRepository = wbsItemRepository;
    }

    @Override
    public void run(String... args) {
        Project project = projectRepository.findByCode("DA-001").orElse(null);
        if (project == null) {
            return;
        }

        if (reportRepository.countByProjectId(project.getId()) > 0) {
            return;
        }

        WbsItem pileItem = wbsItemRepository.findByProjectIdAndWbsCode(project.getId(), "1.2.5").orElse(null);
        WbsItem excavateItem = wbsItemRepository.findByProjectIdAndWbsCode(project.getId(), "1.2.1").orElse(null);
        WbsItem rebarItem = wbsItemRepository.findByProjectIdAndWbsCode(project.getId(), "1.2.2").orElse(null);

        // Đợt 1: Đã duyệt (APPROVED) - Có hạng mục Ép cọc bê tông D400
        AcceptanceReport report1 = new AcceptanceReport(
                project.getId(),
                "NT-001/GTC",
                "Nghiệm thu khối lượng phần cọc và đất móng đợt 1",
                1,
                LocalDate.of(2026, 9, 25),
                "APPROVED",
                null,
                "Nghiệm thu hoàn thành 100% công tác đào đất và ép cọc ly tâm D400."
        );
        report1.setLocation("Công trường dự án Chung cư Green Tower - Khu móng tháp A");
        report1.setContractorRepresentativeName("Trần Đức Long - Chỉ huy phó");
        report1.setSupervisorRepresentativeName("Phạm Quốc Huy - TVGS Trưởng");
        report1.setOwnerRepresentativeName("Đặng Hữu Chinh - Đại diện CĐT");

        if (pileItem != null) {
            AcceptanceItem item1 = new AcceptanceItem(
                    pileItem.getId(),
                    null,
                    "Ép cọc bê tông cốt thép D400",
                    "md",
                    BigDecimal.valueOf(1500.0),
                    BigDecimal.valueOf(1500.0),
                    BigDecimal.valueOf(380000.0),
                    BigDecimal.valueOf(1500.0),
                    "Đạt cao độ và tải trọng ép thiết kế Pmax"
            );
            report1.addItem(item1);
        }

        if (excavateItem != null) {
            AcceptanceItem item2 = new AcceptanceItem(
                    excavateItem.getId(),
                    null,
                    "Đào đất móng",
                    "m3",
                    BigDecimal.valueOf(3500.0),
                    BigDecimal.valueOf(3500.0),
                    BigDecimal.valueOf(85000.0),
                    BigDecimal.valueOf(3500.0),
                    "Đạt cao độ đáy móng thiết kế"
            );
            report1.addItem(item2);
        }

        report1.calculateTotalAmount();
        reportRepository.save(report1);

        // Đợt 2: Đang chờ duyệt (SUBMITTED)
        if (rebarItem != null) {
            AcceptanceReport report2 = new AcceptanceReport(
                    project.getId(),
                    "NT-002/GTC",
                    "Nghiệm thu khối lượng gia công cốt thép móng đợt 2",
                    2,
                    LocalDate.of(2026, 10, 5),
                    "SUBMITTED",
                    null,
                    "Hồ sơ nghiệm thu thép đài móng đợt 2 gửi TVGS và CĐT xem xét phê duyệt."
            );
            report2.setLocation("Khu vực đài móng trục 1 - 5");
            report2.setContractorRepresentativeName("Trần Đức Long - Chỉ huy phó");
            report2.setSupervisorRepresentativeName("Phạm Quốc Huy - TVGS Trưởng");

            AcceptanceItem item3 = new AcceptanceItem(
                    rebarItem.getId(),
                    null,
                    "Gia công cốt thép móng",
                    "tấn",
                    BigDecimal.valueOf(45.0),
                    BigDecimal.valueOf(30.0),
                    BigDecimal.valueOf(18500000.0),
                    BigDecimal.valueOf(30.0),
                    "Nghiệm thu khối lượng thép đài móng đợt 1 hoàn thành 30 tấn"
            );
            report2.addItem(item3);
            report2.calculateTotalAmount();
            reportRepository.save(report2);
        }
    }
}
