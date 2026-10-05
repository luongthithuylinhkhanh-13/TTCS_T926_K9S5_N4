package com.ntdhtcct.domain.milestone;

import liquibase.Contexts;
import liquibase.LabelExpression;
import liquibase.Liquibase;
import liquibase.database.Database;
import liquibase.database.DatabaseFactory;
import liquibase.database.jvm.JdbcConnection;
import liquibase.resource.ClassLoaderResourceAccessor;
import org.junit.jupiter.api.Test;

import java.sql.Date;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.time.LocalDate;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class MilestonesMigrationTest {

    @Test
    void createsMilestonesTableLinkedToProjectsAndCategories() throws Exception {
        try (var connection = DriverManager.getConnection(
                "jdbc:h2:mem:milestones_migration;MODE=PostgreSQL;DB_CLOSE_DELAY=-1",
                "sa",
                "")) {
            try (Statement statement = connection.createStatement()) {
                statement.execute("CREATE TABLE projects (id UUID PRIMARY KEY)");
                statement.execute("CREATE TABLE wbs_items (id UUID PRIMARY KEY)");
            }

            Database database = DatabaseFactory.getInstance()
                    .findCorrectDatabaseImplementation(new JdbcConnection(connection));
            Liquibase liquibase = new Liquibase(
                    "db/migration/v15/V15_0__create_milestones_table.xml",
                    new ClassLoaderResourceAccessor(),
                    database
            );

            liquibase.update(new Contexts(), new LabelExpression());

            UUID projectId = UUID.randomUUID();
            UUID categoryId = UUID.randomUUID();

            try (PreparedStatement insertProject = connection.prepareStatement(
                    "INSERT INTO projects (id) VALUES (?)")) {
                insertProject.setObject(1, projectId);
                insertProject.executeUpdate();
            }

            try (PreparedStatement insertCategory = connection.prepareStatement(
                    "INSERT INTO wbs_items (id) VALUES (?)")) {
                insertCategory.setObject(1, categoryId);
                insertCategory.executeUpdate();
            }

            UUID milestoneId = UUID.randomUUID();
            LocalDate targetDate = LocalDate.of(2026, 6, 30);

            try (PreparedStatement insertMilestone = connection.prepareStatement(
                    "INSERT INTO milestones (id, project_id, category_id, name, target_date, description) " +
                            "VALUES (?, ?, ?, ?, ?, ?)")) {
                insertMilestone.setObject(1, milestoneId);
                insertMilestone.setObject(2, projectId);
                insertMilestone.setObject(3, categoryId);
                insertMilestone.setString(4, "Hoàn thành phần ngầm");
                insertMilestone.setDate(5, Date.valueOf(targetDate));
                insertMilestone.setString(6, "Bao gồm ép cọc và đổ đài móng");
                assertThat(insertMilestone.executeUpdate()).isEqualTo(1);
            }

            try (PreparedStatement query = connection.prepareStatement(
                    "SELECT name, target_date FROM milestones WHERE id = ?")) {
                query.setObject(1, milestoneId);
                try (ResultSet rs = query.executeQuery()) {
                    assertThat(rs.next()).isTrue();
                    assertThat(rs.getString("name")).isEqualTo("Hoàn thành phần ngầm");
                    assertThat(rs.getDate("target_date").toLocalDate()).isEqualTo(targetDate);
                }
            }
        }
    }
}
