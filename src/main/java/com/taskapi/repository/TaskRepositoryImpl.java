package com.taskapi.repository;

import com.taskapi.domain.Task;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.Query;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

public class TaskRepositoryImpl implements TaskRepositoryCustom {

    private static final Map<String, String> SORT_COLUMN_MAP = Map.of(
        "id", "t.id",
        "title", "t.title",
        "status", "t.status",
        "priority", "t.priority",
        "dueDate", "t.due_date",
        "assignee", "t.assignee",
        "createdAt", "t.created_at",
        "updatedAt", "t.updated_at"
    );

    @PersistenceContext
    private EntityManager entityManager;

    @Override
    public Page<Task> findAllWithFilters(
        String status,
        String priority,
        String assignee,
        String tag,
        Instant dueBefore,
        Instant dueAfter,
        String textSearch,
        Pageable pageable
    ) {
        StringBuilder sql = new StringBuilder("""
            SELECT t.id FROM tasks t
            LEFT JOIN task_tags tt ON t.id = tt.task_id
            WHERE (:status IS NULL OR t.status = CAST(:status AS text))
            AND (:priority IS NULL OR t.priority = CAST(:priority AS text))
            AND (:assignee IS NULL OR t.assignee = :assignee)
            AND (:tag IS NULL OR EXISTS (SELECT 1 FROM task_tags tt2 WHERE tt2.task_id = t.id AND tt2.tag = :tag))
            AND (:dueBefore IS NULL OR t.due_date <= :dueBefore)
            AND (:dueAfter IS NULL OR t.due_date >= :dueAfter)
            AND (:textSearch IS NULL OR :textSearch = '' OR t.search_vector @@ plainto_tsquery('english', :textSearch))
            """);
        sql.append(orderByClause(pageable.getSort()));
        Query query = entityManager.createNativeQuery(sql.toString());
        setParams(query, status, priority, assignee, tag, dueBefore, dueAfter, textSearch);
        query.setFirstResult((int) pageable.getOffset());
        query.setMaxResults(pageable.getPageSize());
        @SuppressWarnings("unchecked")
        List<Object> rows = query.getResultList();
        List<UUID> ids = rows.stream()
            .map(row -> row instanceof UUID ? (UUID) row : UUID.fromString(row.toString()))
            .toList();

        String countSql = """
            SELECT COUNT(DISTINCT t.id) FROM tasks t
            LEFT JOIN task_tags tt ON t.id = tt.task_id
            WHERE (:status IS NULL OR t.status = CAST(:status AS text))
            AND (:priority IS NULL OR t.priority = CAST(:priority AS text))
            AND (:assignee IS NULL OR t.assignee = :assignee)
            AND (:tag IS NULL OR EXISTS (SELECT 1 FROM task_tags tt2 WHERE tt2.task_id = t.id AND tt2.tag = :tag))
            AND (:dueBefore IS NULL OR t.due_date <= :dueBefore)
            AND (:dueAfter IS NULL OR t.due_date >= :dueAfter)
            AND (:textSearch IS NULL OR :textSearch = '' OR t.search_vector @@ plainto_tsquery('english', :textSearch))
            """;
        Query countQuery = entityManager.createNativeQuery(countSql);
        setParams(countQuery, status, priority, assignee, tag, dueBefore, dueAfter, textSearch);
        long total = ((Number) countQuery.getSingleResult()).longValue();

        if (ids.isEmpty()) {
            return new PageImpl<>(List.of(), pageable, total);
        }
        List<Task> tasks = entityManager.createQuery(
                "SELECT t FROM Task t LEFT JOIN FETCH t.tags WHERE t.id IN :ids", Task.class)
            .setParameter("ids", ids)
            .getResultList();
        // Preserve order of ids
        List<Task> ordered = ids.stream()
            .map(id -> tasks.stream().filter(t -> t.getId().equals(id)).findFirst().orElse(null))
            .filter(t -> t != null)
            .collect(Collectors.toList());
        return new PageImpl<>(ordered, pageable, total);
    }

    private String orderByClause(Sort sort) {
        if (sort.isUnsorted()) {
            return " ORDER BY t.created_at DESC";
        }
        String order = sort.stream()
            .map(o -> {
                String col = SORT_COLUMN_MAP.getOrDefault(o.getProperty(), "t.created_at");
                return col + " " + (o.isAscending() ? "ASC" : "DESC");
            })
            .collect(Collectors.joining(", "));
        return " ORDER BY " + order;
    }

    private void setParams(Query q, String status, String priority, String assignee, String tag,
                          Instant dueBefore, Instant dueAfter, String textSearch) {
        q.setParameter("status", status);
        q.setParameter("priority", priority);
        q.setParameter("assignee", assignee);
        q.setParameter("tag", tag);
        q.setParameter("dueBefore", dueBefore);
        q.setParameter("dueAfter", dueAfter);
        q.setParameter("textSearch", textSearch == null || textSearch.isBlank() ? null : textSearch);
    }
}
