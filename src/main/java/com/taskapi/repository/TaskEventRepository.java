package com.taskapi.repository;

import com.taskapi.domain.TaskEvent;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface TaskEventRepository extends JpaRepository<TaskEvent, UUID> {

    Page<TaskEvent> findByTaskIdOrderByCreatedAtDesc(UUID taskId, Pageable pageable);
}
