package com.ntdhtcct;

import com.ntdhtcct.domain.wbs.Task;
import com.ntdhtcct.domain.wbs.TaskRepository;
import com.ntdhtcct.domain.wbs.WbsItemRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Kiểm tra Spring Boot context load thành công.
 * Yêu cầu PostgreSQL đang chạy và biến môi trường DB_* đã được cấu hình.
 * Disabled by default vì CI/CD không có PostgreSQL.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Disabled("Requires a running PostgreSQL instance")
class ApplicationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private TaskRepository taskRepository;

    @Autowired
    private WbsItemRepository wbsItemRepository;

    @Test
    void contextLoads() {
        // Xác nhận Spring context khởi động không lỗi
    }

    @Test
    void unknownPathReturnsNotFound() throws Exception {
        mockMvc.perform(get("/"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("RESOURCE_NOT_FOUND"));
    }

    @Test
    void taskPersistsWithCategoryAndDuration() {
        var category = wbsItemRepository.findAll().get(0);

        Task saved = taskRepository.saveAndFlush(
                new Task(category, "Công việc kiểm thử", 3)
        );

        assertThat(saved.getId()).isNotNull();
        assertThat(taskRepository.findByCategoryId(category.getId()))
                .extracting(Task::getName)
                .contains("Công việc kiểm thử");
    }
}
