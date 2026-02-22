package com.taskapi.dto;

import com.taskapi.domain.TaskPriority;
import com.taskapi.domain.TaskStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TaskRequest {

    @NotBlank(message = "Title is required")
    @Size(min = 1, max = 200)
    private String title;

    @Size(max = 5000)
    private String description;

    private TaskStatus status;

    private TaskPriority priority;

    private Instant dueDate;

    @Size(max = 255)
    private String assignee;

    private List<@Size(max = 255) String> tags;
}
