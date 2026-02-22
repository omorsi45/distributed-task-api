package com.taskapi.dto;

import com.taskapi.domain.TaskPriority;
import com.taskapi.domain.TaskStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TaskResponse {

    private UUID id;
    private String title;
    private String description;
    private TaskStatus status;
    private TaskPriority priority;
    private Instant dueDate;
    private String assignee;
    private List<String> tags;
    private Long version;
    private Instant createdAt;
    private Instant updatedAt;
}
