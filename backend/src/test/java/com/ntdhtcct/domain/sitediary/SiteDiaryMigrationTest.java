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
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class SiteDiaryMigrationTest {

    @Test
    void addsDailyResourceAndWorkingConditionFieldsWithoutLosingExistingDiaries() throws Exception {
        try (var connection = DriverManager.getConnection(
                "jdbc:h2:mem:site_diary_v17;MODE=PostgreSQL;DB_CLOSE_DELAY=-1",
                "sa",
                "")) {
            UUID existingDiaryId = UUID.randomUUID();
            try (Statement statement = connection.createStatement()) {
                statement.execute("CREATE TABLE site_diaries (id UUID PRIMARY KEY)");
            }
            try (PreparedStatement insert = connection.prepareStatement(
                    "INSERT INTO site_diaries (id) VALUES (?)")) {
                insert.setObject(1, existingDiaryId);
                insert.executeUpdate();
            }

            Database database = DatabaseFactory.getInstance()
                    .findCorrectDatabaseImplementation(new JdbcConnection(connection));
            Liquibase liquibase = new Liquibase(
                    "db/migration/v17/V17_0__extend_site_diary_daily_conditions.xml",
                    new ClassLoaderResourceAccessor(),
                    database
            );
            liquibase.update(new Contexts(), new LabelExpression());

            try (PreparedStatement query = connection.prepareStatement(
                    "SELECT engineer_count, crew_count, crew_details, working_conditions " +
                            "FROM site_diaries WHERE id = ?")) {
                query.setObject(1, existingDiaryId);
                try (ResultSet result = query.executeQuery()) {
                    assertThat(result.next()).isTrue();
                    assertThat(result.getObject("engineer_count")).isNull();
                    assertThat(result.getObject("crew_count")).isNull();
                    assertThat(result.getString("crew_details")).isNull();
                    assertThat(result.getString("working_conditions")).isNull();
                }
            }
        }
    }
}
