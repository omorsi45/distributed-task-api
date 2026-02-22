package com.taskapi.dto;

import com.taskapi.domain.TaskEventType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TaskEventResponse {

    private UUID id;
    private UUID taskId;
    private TaskEventType type;
    private String payload;
    private Instant createdAt;
}
