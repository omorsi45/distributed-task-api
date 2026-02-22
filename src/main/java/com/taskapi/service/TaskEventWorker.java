package com.taskapi.service;

import com.taskapi.event.TaskDomainEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * Simulated "Worker service" component: consumes task events asynchronously.
 * In a distributed setup this could be a separate service subscribing to a message queue.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class TaskEventWorker {

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onTaskDomainEvent(TaskDomainEvent event) {
        log.info("Worker consumed event: taskId={}, type={}", event.getTask().getId(), event.getType());
        // Simulate downstream processing (e.g. notifications, analytics, sync to search index)
    }
}
