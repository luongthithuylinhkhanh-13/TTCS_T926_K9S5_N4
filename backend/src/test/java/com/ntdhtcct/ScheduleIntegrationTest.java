package com.ntdhtcct;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ntdhtcct.dto.CreateDependencyRequest;
import com.ntdhtcct.dto.CreateTaskRequest;
import com.ntdhtcct.entity.*;
import com.ntdhtcct.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Epic E-04: Tiến độ và đường găng
 * Story S-08: Tính thời điểm khởi sớm và kết thúc sớm (NTDHTCT-143)
 * Integration Test kiểm thử tích hợp toàn bộ API quản lý công việc, quan hệ phụ thuộc và tính toán Forward Pass.
 *
 * Kiểm tra:
 * - T-18 (NTDHTCT-144): Tạo công việc, tạo 4 loại quan hệ phụ thuộc (FS, SS, FF, SF) và độ trễ.
 * - T-19 (NTDHTCT-145): Kích hoạt API tính toán ES/EF, lưu DB, kiểm tra RBAC và phát hiện lỗi chu trình.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
public class ScheduleIntegrationTest {

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
    private TaskRepository taskRepository;

    @Autowired
    private TaskDependencyRepository taskDependencyRepository;

    private User managerUser;
    private User workerUser;
    private Project project;

    @BeforeEach
    void setUp() {
        taskDependencyRepository.deleteAll();
        taskRepository.deleteAll();
        projectMemberRepository.deleteAll();
        projectRepository.deleteAll();
        userRepository.deleteAll();
        roleRepository.deleteAll();

        // Tạo roles
        Role roleManager = roleRepository.save(new Role("PROJECT_MANAGER", "Chỉ huy trưởng", "Quản lý dự án"));
        Role roleWorker = roleRepository.save(new Role("WORKER", "Đội thi công", "Công nhân hiện trường"));

        // Tạo users
        managerUser = userRepository.save(new User("manager", "manager@test.com", "Nguyễn Chỉ Huy"));
        workerUser = userRepository.save(new User("worker", "worker@test.com", "Trần Công Nhân"));

        // Tạo dự án
        project = projectRepository.save(new Project("DA-001", "Dự án Cầu Sông Hồng", "Xây dựng hạ tầng cầu đường"));

        // Gán thành viên
        projectMemberRepository.save(new ProjectMember(project, managerUser, roleManager));
        projectMemberRepository.save(new ProjectMember(project, workerUser, roleWorker));
    }

    @Test
    @DisplayName("T-19: Tạo công việc mới thành công qua REST API -> HTTP 201")
    void testCreateTaskSuccess() throws Exception {
        CreateTaskRequest request = new CreateTaskRequest("CV-01", "Đào hố móng", 5.0, "Đào đất móng trụ cầu");

        mockMvc.perform(post("/api/projects/" + project.getId() + "/tasks")
                        .header("X-User-Id", managerUser.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data.code", is("CV-01")))
                .andExpect(jsonPath("$.data.duration", is(5.0)));
    }

    @Test
    @DisplayName("T-18: Tạo quan hệ phụ thuộc giữa hai công việc thành công -> HTTP 201")
    void testCreateDependencySuccess() throws Exception {
        Task taskA = taskRepository.save(new Task(project, "A", "Gia công cốt thép", 4.0));
        Task taskB = taskRepository.save(new Task(project, "B", "Đổ bê tông trụ", 6.0));

        CreateDependencyRequest req = new CreateDependencyRequest(taskA.getId(), taskB.getId(), DependencyType.FS, 1.0);

        mockMvc.perform(post("/api/projects/" + project.getId() + "/dependencies")
                        .header("X-User-Id", managerUser.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data.type", is("FS")))
                .andExpect(jsonPath("$.data.lag", is(1.0)))
                .andExpect(jsonPath("$.data.predecessorCode", is("A")))
                .andExpect(jsonPath("$.data.successorCode", is("B")));
    }

    @Test
    @DisplayName("T-19: Kích hoạt Forward Pass tính toán ES và EF, lưu kết quả vào Database -> HTTP 200")
    void testCalculateForwardPassAndPersist() throws Exception {
        // Thiết lập mạng công việc:
        // A (dur=3) -> B (dur=4, FS với A, lag=0) -> C (dur=2, FS với B, lag=1)
        Task taskA = taskRepository.save(new Task(project, "A", "Công việc A", 3.0));
        Task taskB = taskRepository.save(new Task(project, "B", "Công việc B", 4.0));
        Task taskC = taskRepository.save(new Task(project, "C", "Công việc C", 2.0));

        taskDependencyRepository.save(new TaskDependency(project, taskA, taskB, DependencyType.FS, 0.0));
        taskDependencyRepository.save(new TaskDependency(project, taskB, taskC, DependencyType.FS, 1.0));

        mockMvc.perform(post("/api/projects/" + project.getId() + "/schedule/forward-pass")
                        .header("X-User-Id", managerUser.getId())
                        .param("saveToDb", "true"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data.totalTasks", is(3)))
                // A: ES=0, EF=3
                // B: ES=3, EF=7
                // C: ES=7+1=8, EF=8+2=10
                .andExpect(jsonPath("$.data.projectDuration", is(10.0)));

        // Kiểm tra dữ liệu trong DB đã được cập nhật
        Task updatedA = taskRepository.findById(taskA.getId()).orElseThrow();
        assertEquals(0.0, updatedA.getEarlyStart());
        assertEquals(3.0, updatedA.getEarlyFinish());

        Task updatedB = taskRepository.findById(taskB.getId()).orElseThrow();
        assertEquals(3.0, updatedB.getEarlyStart());
        assertEquals(7.0, updatedB.getEarlyFinish());

        Task updatedC = taskRepository.findById(taskC.getId()).orElseThrow();
        assertEquals(8.0, updatedC.getEarlyStart());
        assertEquals(10.0, updatedC.getEarlyFinish());
    }

    @Test
    @DisplayName("T-19: Truy vấn kết quả tiến độ (GET) thành công sau khi đã tính toán")
    void testGetCalculatedSchedule() throws Exception {
        Task taskA = new Task(project, "A", "Công việc A", 5.0);
        taskA.setEarlyStart(0.0);
        taskA.setEarlyFinish(5.0);
        taskRepository.save(taskA);

        mockMvc.perform(get("/api/projects/" + project.getId() + "/schedule/forward-pass")
                        .header("X-User-Id", managerUser.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data.projectDuration", is(5.0)))
                .andExpect(jsonPath("$.data.schedule[0].code", is("A")))
                .andExpect(jsonPath("$.data.schedule[0].earlyStart", is(0.0)))
                .andExpect(jsonPath("$.data.schedule[0].earlyFinish", is(5.0)));
    }

    @Test
    @DisplayName("T-19: Phát hiện mạng có chu trình kín A -> B -> A trả về HTTP 400 Bad Request")
    void testForwardPassWithCycleReturnsBadRequest() throws Exception {
        Task taskA = taskRepository.save(new Task(project, "A", "Công việc A", 3.0));
        Task taskB = taskRepository.save(new Task(project, "B", "Công việc B", 4.0));

        // Chu trình: A -> B và B -> A
        taskDependencyRepository.save(new TaskDependency(project, taskA, taskB, DependencyType.FS, 0.0));
        taskDependencyRepository.save(new TaskDependency(project, taskB, taskA, DependencyType.FS, 0.0));

        mockMvc.perform(post("/api/projects/" + project.getId() + "/schedule/forward-pass")
                        .header("X-User-Id", managerUser.getId()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code", is("SCHEDULE_CYCLE_DETECTED")));
    }

    @Test
    @DisplayName("T-19 & T-04: WORKER không có quyền tính toán tiến độ -> HTTP 403 Forbidden")
    void testWorkerCannotCalculateSchedule() throws Exception {
        mockMvc.perform(post("/api/projects/" + project.getId() + "/schedule/forward-pass")
                        .header("X-User-Id", workerUser.getId()))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code", is("INSUFFICIENT_PROJECT_ROLE")));
    }
}
