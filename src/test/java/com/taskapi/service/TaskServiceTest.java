package com.taskapi.service;

import com.taskapi.domain.TaskPriority;
import com.taskapi.domain.TaskStatus;
import com.taskapi.dto.TaskRequest;
import com.taskapi.dto.TaskResponse;
import com.taskapi.dto.TaskStatusUpdateRequest;
import com.taskapi.repository.TaskRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.Page;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.transaction.annotation.Transactional;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@ActiveProfiles("test")
@Testcontainers
@Transactional
class TaskServiceTest {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine")
        .withDatabaseName("testdb")
        .withUsername("test")
        .withPassword("test");

    @DynamicPropertySource
    static void configure(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
    }

    @Autowired
    TaskService taskService;

    @Autowired
    TaskRepository taskRepository;

    @Test
    void create_and_getById() {
        TaskRequest req = TaskRequest.builder()
            .title("Test task")
            .description("Desc")
            .status(TaskStatus.TODO)
            .priority(TaskPriority.HIGH)
            .tags(List.of("a", "b"))
            .build();
        TaskResponse created = taskService.create(req, null);
        assertThat(created.getId()).isNotNull();
        assertThat(created.getTitle()).isEqualTo("Test task");
        assertThat(created.getTags()).containsExactlyInAnyOrder("a", "b");

        TaskResponse found = taskService.getById(created.getId());
        assertThat(found.getTitle()).isEqualTo(created.getTitle());
    }

    @Test
    void idempotency_same_key_returns_same_task() {
        TaskRequest req = TaskRequest.builder().title("Idem").build();
        TaskResponse first = taskService.create(req, "key-123");
        TaskResponse second = taskService.create(req, "key-123");
        assertThat(first.getId()).isEqualTo(second.getId());
    }

    @Test
    void updateStatus() {
        TaskResponse created = taskService.create(
            TaskRequest.builder().title("T").status(TaskStatus.TODO).build(), null);
        TaskResponse updated = taskService.updateStatus(created.getId(),
            new TaskStatusUpdateRequest(TaskStatus.IN_PROGRESS));
        assertThat(updated.getStatus()).isEqualTo(TaskStatus.IN_PROGRESS);
    }

    @Test
    void list_with_filters() {
        taskService.create(TaskRequest.builder().title("A").assignee("alice").build(), null);
        taskService.create(TaskRequest.builder().title("B").assignee("bob").build(), null);
        Page<TaskResponse> page = taskService.list("TODO", null, "alice", null, null, null, null, 0, 10, "createdAt", "desc");
        assertThat(page.getContent()).hasSize(1);
        assertThat(page.getContent().get(0).getAssignee()).isEqualTo("alice");
    }

    @Test
    void delete() {
        TaskResponse created = taskService.create(TaskRequest.builder().title("To delete").build(), null);
        taskService.delete(created.getId());
        assertThatThrownBy(() -> taskService.getById(created.getId()))
            .hasMessageContaining("404");
    }
}
