package com.ntdhtcct.domain.wbs;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.util.List;

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
                List.of(task)
        );

        JsonNode json = new ObjectMapper().valueToTree(response);

        assertThat(json.path("success").asBoolean()).isTrue();
        assertThat(json.path("summary").path("totalTasks").asInt()).isEqualTo(1);
        assertThat(json.path("summary").path("criticalTasksCount").asInt()).isEqualTo(1);
        assertThat(json.path("summary").path("projectDuration").asInt()).isEqualTo(15);
        assertThat(json.path("tasks")).hasSize(1);
        assertThat(json.path("tasks").get(0).path("isCritical").asBoolean()).isTrue();
    }
}
