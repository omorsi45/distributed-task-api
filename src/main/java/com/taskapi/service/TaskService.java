package com.taskapi.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.taskapi.domain.Task;
import com.taskapi.domain.TaskEventType;
import com.taskapi.domain.TaskPriority;
import com.taskapi.domain.TaskStatus;
import com.taskapi.dto.*;
import com.taskapi.mapper.TaskMapper;
import com.taskapi.repository.TaskEventRepository;
import com.taskapi.repository.TaskRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class TaskService {

    private static final String TASK_CACHE = "tasks";
    private static final String TASK_LIST_CACHE = "taskList";

    private final TaskRepository taskRepository;
    private final TaskEventRepository taskEventRepository;
    private final TaskMapper taskMapper;
    private final TaskEventPublisher taskEventPublisher;
    private final IdempotencyService idempotencyService;
    private final ObjectMapper objectMapper;

    @Transactional(readOnly = true)
    @Cacheable(value = TASK_CACHE, key = "#id.toString()")
    public TaskResponse getById(UUID id) {
        Task task = taskRepository.findByIdWithTags(id)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Task not found: " + id));
        return taskMapper.toResponse(task);
    }

    @Transactional
    @CacheEvict(value = { TASK_CACHE, TASK_LIST_CACHE }, allEntries = true)
    public TaskResponse create(TaskRequest request, String idempotencyKey) {
        if (idempotencyKey != null && !idempotencyKey.isBlank()) {
            var existing = idempotencyService.findExistingResponseTaskId(idempotencyKey);
            if (existing.isPresent()) {
                log.info("Idempotent create: returning existing task {}", existing.get());
                return getById(existing.get());
            }
        }

        Task task = taskMapper.toEntity(request);
        task.setStatus(request.getStatus() != null ? request.getStatus() : TaskStatus.TODO);
        task.setPriority(request.getPriority() != null ? request.getPriority() : TaskPriority.MEDIUM);
        task.setTags(request.getTags() != null ? new ArrayList<>(request.getTags()) : new ArrayList<>());
        task = taskRepository.save(task);
        taskEventPublisher.publish(task, TaskEventType.CREATED, toPayload(task));

        if (idempotencyKey != null && !idempotencyKey.isBlank()) {
            idempotencyService.storeKey(idempotencyKey, task.getId());
        }
        return taskMapper.toResponse(task);
    }

    @Transactional
    @CacheEvict(value = { TASK_CACHE, TASK_LIST_CACHE }, allEntries = true)
    public TaskResponse update(UUID id, TaskRequest request) {
        Task task = taskRepository.findByIdWithTags(id)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Task not found: " + id));
        taskMapper.updateEntity(request, task);
        if (request.getStatus() != null) task.setStatus(request.getStatus());
        if (request.getPriority() != null) task.setPriority(request.getPriority());
        if (request.getTags() != null) task.setTags(new ArrayList<>(request.getTags()));
        try {
            task = taskRepository.save(task);
        } catch (jakarta.persistence.OptimisticLockException e) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Task was modified by another request");
        }
        taskEventPublisher.publish(task, TaskEventType.UPDATED, toPayload(task));
        return taskMapper.toResponse(task);
    }

    @Transactional
    @CacheEvict(value = { TASK_CACHE, TASK_LIST_CACHE }, allEntries = true)
    public TaskResponse updateStatus(UUID id, TaskStatusUpdateRequest request) {
        Task task = taskRepository.findByIdWithTags(id)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Task not found: " + id));
        TaskStatus oldStatus = task.getStatus();
        task.setStatus(request.getStatus());
        try {
            task = taskRepository.save(task);
        } catch (jakarta.persistence.OptimisticLockException e) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Task was modified by another request");
        }
        taskEventPublisher.publish(task, TaskEventType.STATUS_CHANGED,
            "{\"oldStatus\":\"" + oldStatus + "\",\"newStatus\":\"" + task.getStatus() + "\"}");
        return taskMapper.toResponse(task);
    }

    @Transactional
    @CacheEvict(value = { TASK_CACHE, TASK_LIST_CACHE }, allEntries = true)
    public void delete(UUID id) {
        Task task = taskRepository.findById(id)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Task not found: " + id));
        taskEventPublisher.publish(task, TaskEventType.DELETED, toPayload(task));
        taskRepository.delete(task);
    }

    @Transactional(readOnly = true)
    public Page<TaskResponse> list(String status, String priority, String assignee, String tag,
                                   Instant dueBefore, Instant dueAfter, String textSearch,
                                   int page, int size, String sortField, String sortDir) {
        Sort sort = sortField != null && !sortField.isBlank()
            ? ("desc".equalsIgnoreCase(sortDir) ? Sort.by(sortField).descending() : Sort.by(sortField).ascending())
            : Sort.by("createdAt").descending();
        Pageable pageable = PageRequest.of(page, size, sort);
        Page<Task> tasks = taskRepository.findAllWithFilters(
            status, priority, assignee, tag, dueBefore, dueAfter, textSearch, pageable);
        return tasks.map(taskMapper::toResponse);
    }

    /** Cached list for common filter combo (no filters, first page). */
    @Transactional(readOnly = true)
    @Cacheable(value = TASK_LIST_CACHE, key = "'default'")
    public Page<TaskResponse> listCachedDefault(int page, int size) {
        return list(null, null, null, null, null, null, null, page, size, "createdAt", "desc");
    }

    @Transactional(readOnly = true)
    public Page<TaskEventResponse> getEvents(UUID taskId, int page, int size) {
        if (!taskRepository.existsById(taskId)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Task not found: " + taskId);
        }
        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        return taskEventRepository.findByTaskIdOrderByCreatedAtDesc(taskId, pageable)
            .map(taskMapper::toEventResponse);
    }

    @Transactional
    @CacheEvict(value = { TASK_CACHE, TASK_LIST_CACHE }, allEntries = true)
    public List<TaskResponse> bulkCreate(List<TaskRequest> requests) {
        List<TaskResponse> result = new ArrayList<>();
        for (TaskRequest req : requests) {
            Task task = taskMapper.toEntity(req);
            task.setStatus(req.getStatus() != null ? req.getStatus() : TaskStatus.TODO);
            task.setPriority(req.getPriority() != null ? req.getPriority() : TaskPriority.MEDIUM);
            task.setTags(req.getTags() != null ? new ArrayList<>(req.getTags()) : new ArrayList<>());
            task = taskRepository.save(task);
            taskEventPublisher.publish(task, TaskEventType.CREATED, toPayload(task));
            result.add(taskMapper.toResponse(task));
        }
        return result;
    }

    private String toPayload(Task task) {
        try {
            return objectMapper.writeValueAsString(taskMapper.toResponse(task));
        } catch (JsonProcessingException e) {
            return "{}";
        }
    }
}
