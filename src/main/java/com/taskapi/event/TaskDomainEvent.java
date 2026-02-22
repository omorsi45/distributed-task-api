package com.taskapi.event;

import com.taskapi.domain.Task;
import com.taskapi.domain.TaskEventType;
import lombok.Getter;
import org.springframework.context.ApplicationEvent;

@Getter
public class TaskDomainEvent extends ApplicationEvent {

    private final Task task;
    private final TaskEventType type;
    private final String payload;

    public TaskDomainEvent(Object source, Task task, TaskEventType type, String payload) {
        super(source);
        this.task = task;
        this.type = type;
        this.payload = payload != null ? payload : "{}";
    }
}
