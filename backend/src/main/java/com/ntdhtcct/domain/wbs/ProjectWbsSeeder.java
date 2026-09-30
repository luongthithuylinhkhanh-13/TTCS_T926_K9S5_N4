package com.ntdhtcct.domain.wbs;

import com.ntdhtcct.domain.project.Project;
import com.ntdhtcct.domain.project.ProjectRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.UUID;

@Component
public class ProjectWbsSeeder implements CommandLineRunner {

    private final ProjectRepository projectRepository;
    private final WbsItemRepository wbsItemRepository;

    public ProjectWbsSeeder(
            ProjectRepository projectRepository,
            WbsItemRepository wbsItemRepository
    ) {
        this.projectRepository = projectRepository;
        this.wbsItemRepository = wbsItemRepository;
    }

    @Override
    public void run(String... args) {
        seedProjects();
        seedWbsDa001();
        seedWbsDa002();
    }

    // =========================================================
    // PROJECTS
    // =========================================================
    private void seedProjects() {

        // DA-001
        Project da001 = projectRepository.findByCode("DA-001")
                .orElseGet(() -> new Project(
                        "DA-001",
                        "Xây dựng Chung cư Green Tower"
                ));

        da001.setName("Xây dựng Chung cư Green Tower");
        da001.setStatus("in_progress");
        da001.setProgress(68);
        da001.setStartDate(LocalDate.parse("2026-09-01"));
        da001.setEndDate(LocalDate.parse("2027-06-30"));
        da001.setDescription(
                "Dự án xây dựng chung cư Green Tower"
        );

        projectRepository.save(da001);

        // DA-002
        Project da002 = projectRepository.findByCode("DA-002")
                .orElseGet(() -> new Project(
                        "DA-002",
                        "Khu đô thị Green City"
                ));

        da002.setName("Khu đô thị Green City");
        da002.setStatus("in_progress");
        da002.setProgress(35);
        da002.setStartDate(LocalDate.parse("2026-10-15"));
        da002.setEndDate(LocalDate.parse("2028-12-31"));
        da002.setDescription(
                "Dự án khu đô thị sinh thái cao cấp."
        );

        projectRepository.save(da002);

        // DA-003
        Project da003 = projectRepository.findByCode("DA-003")
                .orElseGet(() -> new Project(
                        "DA-003",
                        "Dự án DA-003"
                ));

        da003.setName("Dự án DA-003");
        da003.setStatus("not_started");
        da003.setProgress(0);
        da003.setStartDate(null);
        da003.setEndDate(null);

        projectRepository.save(da003);
    }

    // =========================================================
    // DA-001
    // =========================================================
    private void seedWbsDa001() {

        Project project = projectRepository.findByCode("DA-001")
                .orElseThrow(() ->
                        new RuntimeException("Không tìm thấy DA-001")
                );

        if (!wbsItemRepository
                .findByProjectIdOrderByWbsCodeAsc(project.getId())
                .isEmpty()) {
            return;
        }

        WbsItem root = createItem(
                project.getId(), null,
                "1.0",
                "Xây dựng tòa nhà văn phòng A",
                "project",
                null, null, null,
                "in_progress", 68,
                "2026-09-01",
                "2027-06-30",
                "Dự án đầu tư xây dựng tòa nhà văn phòng tiêu chuẩn 15 tầng.",
                null
        );

        // 1.1
        WbsItem phase11 = createItem(
                project.getId(), root.getId(),
                "1.1",
                "Công tác chuẩn bị",
                "phase",
                null, null, null,
                "completed", 100,
                "2026-09-01",
                "2026-09-07",
                "Chuẩn bị mặt bằng và tổ chức thi công công trình.",
                null
        );

        createItem(
                project.getId(), phase11.getId(),
                "1.1.1",
                "Chuẩn bị mặt bằng",
                "task",
                "NV001", "Nguyễn Văn Minh", "VM",
                "completed", 100,
                "2026-09-01", "2026-09-03",
                "Dọn dẹp và chuẩn bị mặt bằng phục vụ thi công.",
                "site_preparation.png"
        );

        createItem(
                project.getId(), phase11.getId(),
                "1.1.2",
                "Định vị công trình",
                "task",
                "NV002", "Trần Quốc Nam", "QN",
                "completed", 100,
                "2026-09-01", "2026-09-04",
                "Xác định tọa độ và mốc móng công trình.",
                "site_location.png"
        );

        createItem(
                project.getId(), phase11.getId(),
                "1.1.3",
                "Tập kết vật tư",
                "task",
                "NV003", "Lê Hoàng Anh", "HA",
                "completed", 100,
                "2026-09-02", "2026-09-07",
                "Vận chuyển cát, đá, xi măng, cốp pha về công trường.",
                "material_delivery.png"
        );

        // 1.2
        WbsItem phase12 = createItem(
                project.getId(), root.getId(),
                "1.2",
                "Phần móng",
                "phase",
                null, null, null,
                "in_progress", 54,
                "2026-09-08",
                "2026-10-02",
                "Thi công cọc móng và bê tông móng công trình.",
                null
        );

        createItem(
                project.getId(), phase12.getId(),
                "1.2.1",
                "Đào đất móng",
                "task",
                "NV001", "Nguyễn Văn Minh", "VM",
                "completed", 100,
                "2026-09-08", "2026-09-12",
                "Đào hố móng bằng máy đào theo bản vẽ kỹ thuật.",
                "excavation.png"
        );

        createItem(
                project.getId(), phase12.getId(),
                "1.2.2",
                "Gia công cốt thép móng",
                "task",
                "NV004", "Trần Đức Long", "TL",
                "in_progress", 65,
                "2026-09-13", "2026-09-25",
                "Gia công và lắp đặt cốt thép phần móng theo hồ sơ thiết kế được phê duyệt.",
                "rebar.png"
        );

        createItem(
                project.getId(), phase12.getId(),
                "1.2.3",
                "Lắp dựng cốp pha móng",
                "task",
                "NV005", "Phạm Quốc Huy", "QH",
                "in_progress", 50,
                "2026-09-18", "2026-09-27",
                "Lắp đặt cốp pha phủ phim định hình khung móng.",
                "formwork.png"
        );

        createItem(
                project.getId(), phase12.getId(),
                "1.2.4",
                "Đổ bê tông móng",
                "task",
                "NV006", "Nguyễn Hoàng Nam", "HN",
                "not_started", 0,
                "2026-09-28", "2026-10-02",
                "Đổ bê tông tươi thương phẩm mác 300 cho móng.",
                "concrete_pour.png"
        );

        // 1.3
        WbsItem phase13 = createItem(
                project.getId(), root.getId(),
                "1.3",
                "Phần thân",
                "phase",
                null, null, null,
                "in_progress", 10,
                "2026-10-03",
                "2026-10-28",
                "Thi công kết cấu bê tông cốt thép phần thân.",
                null
        );

        createItem(
                project.getId(), phase13.getId(),
                "1.3.1",
                "Thi công cột tầng 1",
                "task",
                "NV004", "Trần Đức Long", "TL",
                "in_progress", 40,
                "2026-10-03", "2026-10-08",
                "Gia công lắp dựng thép và cốp pha cột tầng 1.",
                "column_construction.png"
        );

        createItem(
                project.getId(), phase13.getId(),
                "1.3.2",
                "Thi công dầm tầng 1",
                "task",
                "NV005", "Phạm Quốc Huy", "QH",
                "not_started", 0,
                "2026-10-09", "2026-10-14",
                "Gia công cốt thép và cốp pha dầm tầng 1.",
                "beam_construction.png"
        );

        createItem(
                project.getId(), phase13.getId(),
                "1.3.3",
                "Thi công sàn tầng 1",
                "task",
                "NV001", "Nguyễn Văn Minh", "VM",
                "not_started", 0,
                "2026-10-15", "2026-10-20",
                "Lắp dựng thép sàn và đổ bê tông sàn tầng 1.",
                "floor_construction.png"
        );

        createItem(
                project.getId(), phase13.getId(),
                "1.3.4",
                "Xây tường tầng 1",
                "task",
                "NV003", "Lê Hoàng Anh", "HA",
                "not_started", 0,
                "2026-10-21", "2026-10-28",
                "Xây bao che và tường ngăn tầng 1.",
                "wall_construction.png"
        );

        // 1.4
        WbsItem phase14 = createItem(
                project.getId(), root.getId(),
                "1.4",
                "Hoàn thiện",
                "phase",
                null, null, null,
                "not_started", 0,
                "2026-10-29",
                "2026-12-10",
                "Công tác trát, sơn, cửa và ME hoàn thiện.",
                null
        );

        createItem(
                project.getId(), phase14.getId(),
                "1.4.1",
                "Trát tường",
                "task",
                "NV002", "Trần Quốc Nam", "QN",
                "not_started", 0,
                "2026-10-29", "2026-11-05",
                "Trát tường trong và ngoài nhà.",
                "plastering.png"
        );

        createItem(
                project.getId(), phase14.getId(),
                "1.4.2",
                "Sơn tường",
                "task",
                "NV003", "Lê Hoàng Anh", "HA",
                "not_started", 0,
                "2026-11-06", "2026-11-15",
                "Sơn lót và 2 lớp sơn phủ hoàn thiện.",
                "painting.png"
        );

        createItem(
                project.getId(), phase14.getId(),
                "1.4.3",
                "Lắp đặt cửa",
                "task",
                "NV005", "Phạm Quốc Huy", "QH",
                "not_started", 0,
                "2026-11-16", "2026-11-25",
                "Lắp đặt khung nhôm kính và cửa gỗ.",
                "door_installation.png"
        );

        createItem(
                project.getId(), phase14.getId(),
                "1.4.4",
                "Lắp đặt điện nước",
                "task",
                "NV006", "Nguyễn Hoàng Nam", "HN",
                "not_started", 0,
                "2026-11-26", "2026-12-10",
                "Lắp thiết bị điện, vệ sinh và chạy thử nghiệm.",
                "mep_installation.png"
        );
    }

    // =========================================================
    // DA-002
    // =========================================================
    private void seedWbsDa002() {

        Project project = projectRepository.findByCode("DA-002")
                .orElseThrow(() ->
                        new RuntimeException("Không tìm thấy DA-002")
                );

        if (!wbsItemRepository
                .findByProjectIdOrderByWbsCodeAsc(project.getId())
                .isEmpty()) {
            return;
        }

        WbsItem root = createItem(
                project.getId(), null,
                "1.0",
                "Khu đô thị Green City",
                "project",
                null, null, null,
                "in_progress", 35,
                "2026-10-15",
                "2028-12-31",
                "Dự án khu đô thị sinh thái cao cấp.",
                null
        );

        // 1.1
        WbsItem phase21 = createItem(
                project.getId(), root.getId(),
                "1.1",
                "Giai đoạn 1 - San lấp & Hạ tầng",
                "phase",
                null, null, null,
                "in_progress", 62,
                "2026-10-15",
                "2027-04-30",
                "San lấp mặt bằng toàn bộ 50ha.",
                null
        );

        createItem(
                project.getId(), phase21.getId(),
                "1.1.1",
                "San lấp mặt bằng",
                "task",
                "NV001", "Nguyễn Văn Minh", "VM",
                "in_progress", 80,
                "2026-10-15",
                "2026-12-30",
                "Thi công san lấp đạt cao độ thiết kế.",
                null
        );

        createItem(
                project.getId(), phase21.getId(),
                "1.1.2",
                "Đào hệ thống thoát nước",
                "task",
                "NV002", "Trần Quốc Nam", "QN",
                "in_progress", 45,
                "2027-01-01",
                "2027-04-30",
                "Lắp đặt cống bê tông thoát nước mưa và nước thải.",
                null
        );

        // 1.2
        WbsItem phase22 = createItem(
                project.getId(), root.getId(),
                "1.2",
                "Giai đoạn 2 - Xây dựng thô",
                "phase",
                null, null, null,
                "not_started", 0,
                "2027-05-01",
                "2028-12-31",
                "Thi công đường giao thông và chiếu sáng.",
                null
        );

        createItem(
                project.getId(), phase22.getId(),
                "1.2.1",
                "Thi công đường nội bộ",
                "task",
                "NV003", "Lê Hoàng Anh", "HA",
                "not_started", 0,
                "2027-05-01",
                "2027-10-30",
                "Rải thảm nhựa đường nội bộ 15m.",
                null
        );

        createItem(
                project.getId(), phase22.getId(),
                "1.2.2",
                "Lắp đặt hệ thống chiếu sáng",
                "task",
                "NV005", "Phạm Quốc Huy", "QH",
                "not_started", 0,
                "2027-11-01",
                "2028-03-30",
                "Trồng cột đèn và đấu nối tủ điện chiếu sáng.",
                null
        );
    }

    // =========================================================
    // HELPER
    // =========================================================
    private WbsItem createItem(
            UUID projectId,
            UUID parentId,
            String wbsCode,
            String name,
            String type,
            String assigneeId,
            String assigneeName,
            String assigneeInitials,
            String status,
            int progress,
            String startDate,
            String endDate,
            String description,
            String image
    ) {
        WbsItem item = new WbsItem();

        item.setProjectId(projectId);
        item.setParentId(parentId);
        item.setWbsCode(wbsCode);
        item.setName(name);
        item.setType(type);

        item.setAssigneeId(assigneeId);
        item.setAssigneeName(assigneeName);
        item.setAssigneeInitials(assigneeInitials);

        item.setStatus(status);
        item.setProgress(progress);

        if (startDate != null) {
            item.setStartDate(
                    LocalDate.parse(startDate)
            );
        }

        if (endDate != null) {
            item.setEndDate(
                    LocalDate.parse(endDate)
            );
        }

        item.setDescription(description);
        item.setImage(image);

        return wbsItemRepository.save(item);
    }
}