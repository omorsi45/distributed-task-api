package com.taskapi.repository;

import com.taskapi.domain.Task;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.time.Instant;
import java.util.Optional;

public interface TaskRepositoryCustom {

    Page<Task> findAllWithFilters(
        String status,
        String priority,
        String assignee,
        String tag,
        Instant dueBefore,
        Instant dueAfter,
        String textSearch,
        Pageable pageable
    );
}
