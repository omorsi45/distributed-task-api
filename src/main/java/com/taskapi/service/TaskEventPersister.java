package com.taskapi.service;

import com.taskapi.domain.TaskEvent;
import com.taskapi.domain.TaskEventType;
import com.taskapi.event.TaskDomainEvent;
import com.taskapi.repository.TaskEventRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
@RequiredArgsConstructor
@Slf4j
public class TaskEventPersister {

    private final TaskEventRepository taskEventRepository;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void onTaskDomainEvent(TaskDomainEvent event) {
        TaskEvent entity = TaskEvent.builder()
            .taskId(event.getTask().getId())
            .type(event.getType())
            .payload(event.getPayload())
            .build();
        taskEventRepository.save(entity);
        log.debug("Persisted task event: taskId={}, type={}", entity.getTaskId(), entity.getType());
    }
}
