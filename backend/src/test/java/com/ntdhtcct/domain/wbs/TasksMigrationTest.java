package com.ntdhtcct.domain.wbs;

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
import java.sql.SQLException;
import java.sql.Statement;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

class TasksMigrationTest {

    @Test
    void createsTasksLinkedToWbsCategoryAndRejectsNonPositiveDuration() throws Exception {
        try (var connection = DriverManager.getConnection(
                "jdbc:h2:mem:tasks_migration;MODE=PostgreSQL;DB_CLOSE_DELAY=-1",
                "sa",
                "")) {
            try (Statement statement = connection.createStatement()) {
                statement.execute("CREATE TABLE wbs_items (id UUID PRIMARY KEY)");
            }

            Database database = DatabaseFactory.getInstance()
                    .findCorrectDatabaseImplementation(new JdbcConnection(connection));
            Liquibase liquibase = new Liquibase(
                    "db/migration/v13/V13_0__create_tasks_table.xml",
                    new ClassLoaderResourceAccessor(),
                    database
            );

            liquibase.update(new Contexts(), new LabelExpression());

            UUID categoryId = UUID.randomUUID();
            try (PreparedStatement insertCategory = connection.prepareStatement(
                    "INSERT INTO wbs_items (id) VALUES (?)")) {
                insertCategory.setObject(1, categoryId);
                insertCategory.executeUpdate();
            }

            try (PreparedStatement insertTask = connection.prepareStatement(
                    "INSERT INTO tasks (id, category_id, name, duration) VALUES (?, ?, ?, ?)")) {
                insertTask.setObject(1, UUID.randomUUID());
                insertTask.setObject(2, categoryId);
                insertTask.setString(3, "Test task");
                insertTask.setInt(4, 3);
                assertThat(insertTask.executeUpdate()).isEqualTo(1);
            }

            try (PreparedStatement insertInvalidTask = connection.prepareStatement(
                    "INSERT INTO tasks (id, category_id, name, duration) VALUES (?, ?, ?, ?)")) {
                insertInvalidTask.setObject(1, UUID.randomUUID());
                insertInvalidTask.setObject(2, categoryId);
                insertInvalidTask.setString(3, "Invalid task");
                insertInvalidTask.setInt(4, 0);
                assertThrows(SQLException.class, insertInvalidTask::executeUpdate);
            }

            liquibase.close();
        }
    }
}