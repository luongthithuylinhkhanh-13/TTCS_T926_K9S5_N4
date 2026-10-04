package com.ntdhtcct.domain.wbs;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class ProjectScheduleResponseTest {

    @Test
    void serializesTheScheduleApiContract() {
        WbsItem task = new WbsItem();
        task.setType("task");
        task.setWbsCode("1.4.4");
        task.setName("Install electrical and plumbing");
        task.setDuration(15);
        task.setEs(0);
        task.setEf(15);
        task.setLs(0);
        task.setLf(15);
        task.setSlack(0);
        task.setCritical(true);

        ProjectScheduleResponse response = new ProjectScheduleResponse(
                true,
                new ProjectScheduleResponse.Summary(1, 1, 15, "1", true, 0),
                List.of(task),
                OffsetDateTime.parse("2026-10-04T12:00:00Z"),
                List.of(new ProjectScheduleResponse.BaselineTask(
                        UUID.randomUUID(),
                        "1.4.4",
                        "Install electrical and plumbing",
                        LocalDate.parse("2026-10-01"),
                        LocalDate.parse("2026-10-15"),
                        15
                ))
        );

        JsonNode json = new ObjectMapper()
                .registerModule(new JavaTimeModule())
                .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
                .valueToTree(response);

        assertThat(json.path("success").asBoolean()).isTrue();
        assertThat(json.path("summary").path("totalTasks").asInt()).isEqualTo(1);
        assertThat(json.path("summary").path("criticalTasksCount").asInt()).isEqualTo(1);
        assertThat(json.path("summary").path("projectDuration").asInt()).isEqualTo(15);
        assertThat(json.path("tasks")).hasSize(1);
        assertThat(json.path("tasks").get(0).path("isCritical").asBoolean()).isTrue();
        assertThat(json.path("baselineCapturedAt").asText()).isEqualTo("2026-10-04T12:00:00Z");
        assertThat(json.path("baselineTasks").get(0).path("startDate").asText())
                .isEqualTo("2026-10-01");
    }
}
