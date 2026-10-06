package com.ntdhtcct;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

/**
 * Kiểm tra Spring Boot context load thành công.
 * Yêu cầu PostgreSQL đang chạy và biến môi trường DB_* đã được cấu hình.
 * Disabled by default vì CI/CD không có PostgreSQL.
 */
@SpringBootTest
@ActiveProfiles("test")
@Disabled("Requires a running PostgreSQL instance")
class ApplicationTest {

    @Test
    void contextLoads() {
        // Xác nhận Spring context khởi động không lỗi
    }
}
