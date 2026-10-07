package com.ntdhtcct.domain.milestone;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ntdhtcct.domain.auth.AuthTokenService;
import com.ntdhtcct.domain.milestone.dto.CreateMilestoneRequest;
import com.ntdhtcct.domain.project.Project;
import com.ntdhtcct.domain.project.ProjectRepository;
import com.ntdhtcct.domain.wbs.TaskRepository;
import com.ntdhtcct.domain.wbs.WbsItem;
import com.ntdhtcct.domain.wbs.WbsItemRepository;
import com.ntdhtcct.entity.ProjectMember;
import com.ntdhtcct.entity.Role;
import com.ntdhtcct.entity.User;
import com.ntdhtcct.repository.ProjectMemberRepository;
import com.ntdhtcct.repository.RoleRepository;
import com.ntdhtcct.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;
import java.util.Set;
import java.util.UUID;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
public class MilestoneIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private ProjectRepository projectRepository;

    @Autowired
    private ProjectMemberRepository projectMemberRepository;

    @Autowired
    private WbsItemRepository wbsItemRepository;

    @Autowired
    private TaskRepository taskRepository;

    @Autowired
    private MilestoneRepository milestoneRepository;

    @Autowired
    private AuthTokenService authTokenService;

    private User managerUser;
    private Project project;
    private WbsItem category;

    @BeforeEach
    void setUp() {
        milestoneRepository.deleteAll();
        taskRepository.deleteAll();
        wbsItemRepository.deleteAll();
        projectMemberRepository.deleteAll();
        projectRepository.deleteAll();
        userRepository.deleteAll();
        roleRepository.deleteAll();

        Role roleManager = roleRepository.save(new Role("PROJECT_MANAGER"));
        managerUser = userRepository.save(new User("manager@congtrinh.vn", "Password123", "Trần Quản Lý"));

        project = new Project("DA-MS-01", "Dự án Khách Sạn Biển");
        project.setStartDate(LocalDate.of(2026, 3, 1));
        project = projectRepository.save(project);

        projectMemberRepository.save(new ProjectMember(project, managerUser, roleManager));

        category = new WbsItem();
        category.setProjectId(project.getId());
        category.setWbsCode("1.0");
        category.setName("Hạng mục Phần Thô");
        category.setType("category");
        category = wbsItemRepository.save(category);
    }

    @Test
    @DisplayName("T-43: Tạo mốc tiến độ thành công và trả về 201 Created")
    void testCreateMilestone_Success() throws Exception {
        LocalDate targetDate = LocalDate.of(2026, 3, 20);
        CreateMilestoneRequest request = new CreateMilestoneRequest(
                category.getId(),
                "Mốc cất nóc phần thô",
                targetDate,
                "Hoàn thành toàn bộ kết cấu dầm sàn"
        );

        mockMvc.perform(post("/api/projects/" + project.getId() + "/milestones")
                        .header("Authorization", bearerToken(managerUser))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name", is("Mốc cất nóc phần thô")))
                .andExpect(jsonPath("$.targetDate", is("2026-03-20")))
                .andExpect(jsonPath("$.categoryId", is(category.getId().toString())));
    }

    @Test
    @DisplayName("T-44 & T-45: Cảnh báo khi tiến độ/mốc bị vượt và hiển thị chuỗi việc gây chậm")
    void testGetMilestoneWarnings_WithOverrunAndDelayChain() throws Exception {
        // Tạo chuỗi công việc trong hạng mục:
        // Task 1: 1.0.1 Thi công cột (duration 10, ES=0, EF=10)
        // Task 2: 1.0.2 Đổ sàn tầng 1 (duration 15, ES=10, EF=25, phụ thuộc Task 1)
        // Dự án bắt đầu: 2026-03-01 -> Ngày hoàn thành sớm của Task 2: 2026-03-01 + 24 ngày = 2026-03-25
        WbsItem task1 = new WbsItem();
        task1.setProjectId(project.getId());
        task1.setParentId(category.getId());
        task1.setWbsCode("1.0.1");
        task1.setName("Thi công cột");
        task1.setType("task");
        task1.setDuration(10);
        task1.setEs(0);
        task1.setEf(10);
        task1 = wbsItemRepository.save(task1);

        WbsItem task2 = new WbsItem();
        task2.setProjectId(project.getId());
        task2.setParentId(category.getId());
        task2.setWbsCode("1.0.2");
        task2.setName("Đổ sàn tầng 1");
        task2.setType("task");
        task2.setDuration(15);
        task2.setEs(10);
        task2.setEf(25);
        task2.setPredecessorIds(Set.of(task1.getId()));
        task2 = wbsItemRepository.save(task2);

        // Mốc đặt deadline ngày 2026-03-20 (nhưng task 2 hoàn tất ngày 2026-03-25 -> Vượt 5 ngày)
        Milestone milestone = new Milestone(
                project.getId(),
                category,
                "Mốc hoàn thành sàn tầng 1",
                LocalDate.of(2026, 3, 20),
                "Yêu cầu xong trước 20/03"
        );
        milestoneRepository.save(milestone);

        mockMvc.perform(get("/api/projects/" + project.getId() + "/milestones/warnings")
                        .header("Authorization", bearerToken(managerUser)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].milestoneName", is("Mốc hoàn thành sàn tầng 1")))
                .andExpect(jsonPath("$[0].isOverrun", is(true)))
                .andExpect(jsonPath("$[0].overrunDays", is(5)))
                .andExpect(jsonPath("$[0].status", is("OVERRUN")))
                .andExpect(jsonPath("$[0].lastTaskName", is("Đổ sàn tầng 1")))
                .andExpect(jsonPath("$[0].delayChain", hasSize(2)))
                .andExpect(jsonPath("$[0].delayChain[0].name", is("Thi công cột")))
                .andExpect(jsonPath("$[0].delayChain[1].name", is("Đổ sàn tầng 1")));
    }

    private String bearerToken(User user) {
        return "Bearer " + authTokenService.createToken(user.getId()).getToken();
    }
}
