package com.ntdhtcct.domain.health;

import jakarta.mail.MessagingException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.datasource.DataSourceUtils;
import org.springframework.mail.javamail.JavaMailSenderImpl;
import org.springframework.stereotype.Service;

import javax.sql.DataSource;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.SQLException;
import java.time.OffsetDateTime;
import java.util.LinkedHashMap;
import java.util.Map;

@Service
public class SystemHealthService {

    private static final Logger log = LoggerFactory.getLogger(SystemHealthService.class);

    private final DataSource dataSource;
    private final JavaMailSenderImpl mailSender;
    private final Path storageDirectory;

    public SystemHealthService(
            DataSource dataSource,
            JavaMailSenderImpl mailSender,
            @Value("${app.storage.dir:uploads/diary}") String storageDirectory
    ) {
        this.dataSource = dataSource;
        this.mailSender = mailSender;
        this.storageDirectory = Path.of(storageDirectory).toAbsolutePath().normalize();
    }

    public SystemHealthResponse checkHealth() {
        Map<String, SystemHealthResponse.ServiceStatus> services = new LinkedHashMap<>();
        services.put("application", up("Ứng dụng đang hoạt động."));
        services.put("database", checkDatabase());
        services.put("email", checkEmail());
        services.put("storage", checkStorage());

        String overallStatus = services.values().stream()
                .allMatch(service -> "UP".equals(service.status()))
                ? "UP"
                : "DOWN";

        return new SystemHealthResponse(overallStatus, OffsetDateTime.now(), services);
    }

    private SystemHealthResponse.ServiceStatus checkDatabase() {
        Connection connection = null;
        try {
            connection = DataSourceUtils.getConnection(dataSource);
            if (connection.isValid(3)) {
                return up("PostgreSQL đang kết nối.");
            }
            return down("Không thể xác nhận kết nối PostgreSQL.");
        } catch (SQLException | RuntimeException exception) {
            log.warn("Database health check failed: {}", exception.getMessage());
            return down("Không kết nối được PostgreSQL.");
        } finally {
            if (connection != null) {
                DataSourceUtils.releaseConnection(connection, dataSource);
            }
        }
    }

    private SystemHealthResponse.ServiceStatus checkEmail() {
        try {
            mailSender.testConnection();
            return up("SMTP đang kết nối.");
        } catch (MessagingException | RuntimeException exception) {
            log.warn("SMTP health check failed: {}", exception.getMessage());
            return down("Không kết nối được SMTP.");
        }
    }

    private SystemHealthResponse.ServiceStatus checkStorage() {
        try {
            if (Files.isDirectory(storageDirectory) && Files.isWritable(storageDirectory)) {
                return up("Thư mục lưu trữ đang sẵn sàng.");
            }
            return down("Thư mục lưu trữ không tồn tại hoặc không có quyền ghi.");
        } catch (SecurityException exception) {
            log.warn("Storage health check failed: {}", exception.getMessage());
            return down("Không thể truy cập thư mục lưu trữ.");
        }
    }

    private SystemHealthResponse.ServiceStatus up(String message) {
        return new SystemHealthResponse.ServiceStatus("UP", message);
    }

    private SystemHealthResponse.ServiceStatus down(String message) {
        return new SystemHealthResponse.ServiceStatus("DOWN", message);
    }
}
