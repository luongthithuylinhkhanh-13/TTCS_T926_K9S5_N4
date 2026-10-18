package com.ntdhtcct.domain.health;

import jakarta.mail.MessagingException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.mail.javamail.JavaMailSenderImpl;

import javax.sql.DataSource;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.SQLException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class SystemHealthServiceTest {

    @TempDir
    Path storageDirectory;

    private DataSource dataSource;
    private JavaMailSenderImpl mailSender;
    private Connection connection;
    private SystemHealthService healthService;

    @BeforeEach
    void setUp() throws SQLException, MessagingException {
        dataSource = mock(DataSource.class);
        mailSender = mock(JavaMailSenderImpl.class);
        connection = mock(Connection.class);
        when(dataSource.getConnection()).thenReturn(connection);
        when(connection.isValid(3)).thenReturn(true);
        mailSender.testConnection();
        healthService = new SystemHealthService(dataSource, mailSender, storageDirectory.toString());
    }

    @Test
    void reportsAllConfiguredServicesUp() {
        SystemHealthResponse response = healthService.checkHealth();

        assertThat(response.status()).isEqualTo("UP");
        assertThat(response.services())
                .containsEntry("application", new SystemHealthResponse.ServiceStatus("UP", "Ứng dụng đang hoạt động."))
                .containsEntry("database", new SystemHealthResponse.ServiceStatus("UP", "PostgreSQL đang kết nối."))
                .containsEntry("email", new SystemHealthResponse.ServiceStatus("UP", "SMTP đang kết nối."))
                .containsEntry("storage", new SystemHealthResponse.ServiceStatus("UP", "Thư mục lưu trữ đang sẵn sàng."));
    }

    @Test
    void reportsDownServicesWithoutFailingTheHealthEndpoint() throws SQLException, MessagingException {
        when(connection.isValid(3)).thenReturn(false);
        org.mockito.Mockito.doThrow(new MessagingException("SMTP unavailable"))
                .when(mailSender).testConnection();

        SystemHealthResponse response = healthService.checkHealth();

        assertThat(response.status()).isEqualTo("DOWN");
        assertThat(response.services().get("database").status()).isEqualTo("DOWN");
        assertThat(response.services().get("email").status()).isEqualTo("DOWN");
    }
}
