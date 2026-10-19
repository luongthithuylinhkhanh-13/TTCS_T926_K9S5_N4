package com.ntdhtcct.domain.sitediary;

import liquibase.Contexts;
import liquibase.LabelExpression;
import liquibase.Liquibase;
import liquibase.database.Database;
import liquibase.database.DatabaseFactory;
import liquibase.database.jvm.JdbcConnection;
import liquibase.resource.ClassLoaderResourceAccessor;
import org.junit.jupiter.api.Test;

import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.time.LocalDate;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class SiteDiaryLockMigrationTest {

    @Test
    void createsSiteDiaryLocksTableSuccessfully() throws Exception {
        try (var connection = DriverManager.getConnection(
                "jdbc:h2:mem:site_diary_v20;MODE=PostgreSQL;DB_CLOSE_DELAY=-1",
                "sa",
                "")) {

            // Tạo bảng cha projects và users trước để thỏa mãn foreign key
            try (Statement statement = connection.createStatement()) {
                statement.execute("CREATE TABLE projects (id UUID PRIMARY KEY)");
                statement.execute("CREATE TABLE users (id UUID PRIMARY KEY)");
            }

            Database database = DatabaseFactory.getInstance()
                    .findCorrectDatabaseImplementation(new JdbcConnection(connection));
            Liquibase liquibase = new Liquibase(
                    "db/migration/v20/V20_0__create_site_diary_locks_table.xml",
                    new ClassLoaderResourceAccessor(),
                    database
            );
            liquibase.update(new Contexts(), new LabelExpression());

            UUID projectId = UUID.randomUUID();
            UUID userId = UUID.randomUUID();
            try (PreparedStatement insertProject = connection.prepareStatement("INSERT INTO projects (id) VALUES (?)");
                 PreparedStatement insertUser = connection.prepareStatement("INSERT INTO users (id) VALUES (?)")) {
                insertProject.setObject(1, projectId);
                insertProject.executeUpdate();
                insertUser.setObject(1, userId);
                insertUser.executeUpdate();
            }

            UUID lockId = UUID.randomUUID();
            LocalDate lockDate = LocalDate.now();

            try (PreparedStatement insertLock = connection.prepareStatement(
                    "INSERT INTO site_diary_locks (id, project_id, lock_date, status, locked_by, lock_reason) " +
                            "VALUES (?, ?, ?, 'LOCKED', ?, 'Khóa sổ kiểm toán')")) {
                insertLock.setObject(1, lockId);
                insertLock.setObject(2, projectId);
                insertLock.setObject(3, lockDate);
                insertLock.setObject(4, userId);
                int rows = insertLock.executeUpdate();
                assertThat(rows).isEqualTo(1);
            }

            try (PreparedStatement query = connection.prepareStatement(
                    "SELECT status, lock_reason FROM site_diary_locks WHERE id = ?")) {
                query.setObject(1, lockId);
                try (ResultSet rs = query.executeQuery()) {
                    assertThat(rs.next()).isTrue();
                    assertThat(rs.getString("status")).isEqualTo("LOCKED");
                    assertThat(rs.getString("lock_reason")).isEqualTo("Khóa sổ kiểm toán");
                }
            }
        }
    }
}
