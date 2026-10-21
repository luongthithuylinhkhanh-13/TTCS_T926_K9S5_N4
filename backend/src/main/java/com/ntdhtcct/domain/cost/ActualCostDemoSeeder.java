package com.ntdhtcct.domain.cost;

import com.ntdhtcct.domain.project.Project;
import com.ntdhtcct.domain.project.ProjectRepository;
import com.ntdhtcct.domain.wbs.WbsItem;
import com.ntdhtcct.domain.wbs.WbsItemRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.time.LocalDate;

@Component
@Order(6)
public class ActualCostDemoSeeder implements CommandLineRunner {

    private final ProjectRepository projectRepository;
    private final ActualCostRepository actualCostRepository;
    private final WbsItemRepository wbsItemRepository;

    public ActualCostDemoSeeder(
            ProjectRepository projectRepository,
            ActualCostRepository actualCostRepository,
            WbsItemRepository wbsItemRepository
    ) {
        this.projectRepository = projectRepository;
        this.actualCostRepository = actualCostRepository;
        this.wbsItemRepository = wbsItemRepository;
    }

    @Override
    public void run(String... args) {
        Project project = projectRepository.findByCode("DA-001").orElse(null);
        if (project == null) {
            return;
        }

        if (!actualCostRepository.findByProjectIdOrderByIncurredDateDescCreatedAtDesc(project.getId()).isEmpty()) {
            return;
        }

        WbsItem pileItem = wbsItemRepository.findByProjectIdAndWbsCode(project.getId(), "1.2.5").orElse(null);
        WbsItem concreteItem = wbsItemRepository.findByProjectIdAndWbsCode(project.getId(), "1.2.4").orElse(null);
        WbsItem rebarItem = wbsItemRepository.findByProjectIdAndWbsCode(project.getId(), "1.2.2").orElse(null);

        // 1. Chi phí vật tư cọc bê tông D400
        ActualCost cost1 = new ActualCost(
                project.getId(),
                pileItem,
                null,
                "Vật tư cọc bê tông cốt thép đúc sẵn D400",
                285_000_000.0,
                "MATERIAL",
                LocalDate.of(2026, 9, 15),
                "Thanh toán tạm ứng đợt 1 nhà cung cấp cọc bê tông An Phát",
                "Đặng Hữu Chinh"
        );
        actualCostRepository.save(cost1);

        // 2. Chi phí máy ép cọc và nhân công ca ép
        ActualCost cost2 = new ActualCost(
                project.getId(),
                pileItem,
                null,
                "Thuê máy ép cọc robot và thợ vận hành ca 1",
                48_000_000.0,
                "EQUIPMENT",
                LocalDate.of(2026, 9, 18),
                "Chi phí thuê máy ép tải và thợ vận hành ép cọc đại trà",
                "Trần Quốc Nam"
        );
        actualCostRepository.save(cost2);

        // 3. Chi phí bê tông tươi móng
        ActualCost cost3 = new ActualCost(
                project.getId(),
                concreteItem,
                null,
                "Bê tông thương phẩm mác 300 đổ móng đợt 1",
                125_000_000.0,
                "MATERIAL",
                LocalDate.of(2026, 9, 28),
                "100m3 bê tông tươi trạm trộn Việt Đức",
                "Nguyễn Hoàng Nam"
        );
        actualCostRepository.save(cost3);

        // 4. Chi phí nhân công cốt thép
        ActualCost cost4 = new ActualCost(
                project.getId(),
                rebarItem,
                null,
                "Nhân công gia công và lắp đặt cốt thép móng",
                36_000_000.0,
                "LABOR",
                LocalDate.of(2026, 9, 22),
                "Tổ đội cốp pha thép Long Đạt nghiệm thu thanh toán đợt 1",
                "Trần Đức Long"
        );
        actualCostRepository.save(cost4);
    }
}
