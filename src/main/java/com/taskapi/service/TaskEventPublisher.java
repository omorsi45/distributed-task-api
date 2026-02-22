package com.taskapi.service;

import com.taskapi.domain.Task;
import com.taskapi.domain.TaskEventType;
import com.taskapi.event.TaskDomainEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class TaskEventPublisher {

    private final ApplicationEventPublisher applicationEventPublisher;

    @Async
    public void publish(Task task, TaskEventType type, String payload) {
        TaskDomainEvent event = new TaskDomainEvent(this, task, type, payload);
        applicationEventPublisher.publishEvent(event);
        log.debug("Published task event: taskId={}, type={}", task.getId(), type);
    }
}
