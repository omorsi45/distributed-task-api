-- Enable extensions for text search (trigram) and UUID
CREATE EXTENSION IF NOT EXISTS pg_trgm;

-- Tasks table: UUID PK, version for optimistic locking
CREATE TABLE tasks (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    title VARCHAR(200) NOT NULL,
    description VARCHAR(5000),
    status VARCHAR(20) NOT NULL,
    priority VARCHAR(20) NOT NULL,
    due_date TIMESTAMPTZ,
    assignee VARCHAR(255),
    version BIGINT NOT NULL DEFAULT 0,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT chk_task_title_length CHECK (char_length(trim(title)) >= 1)
);

-- Tags: separate join table for flexible indexing and "filter by tag"
-- (GIN on array would work too; join table allows index on tag + task_id for list filters)
CREATE TABLE task_tags (
    task_id UUID NOT NULL REFERENCES tasks(id) ON DELETE CASCADE,
    tag VARCHAR(255) NOT NULL,
    PRIMARY KEY (task_id, tag)
);

-- Task events: payload as JSONB for flexible event history
CREATE TABLE task_events (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    task_id UUID NOT NULL,
    type VARCHAR(30) NOT NULL,
    payload JSONB,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

-- Idempotency keys: store hash of key + TTL for duplicate POST detection
CREATE TABLE idempotency_keys (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    key_hash VARCHAR(64) NOT NULL,
    response_task_id UUID,
    expires_at TIMESTAMPTZ NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    UNIQUE (key_hash)
);

-- Indexes for list endpoint filters (status, priority, assignee, due_date, tag, text search)
CREATE INDEX idx_tasks_status ON tasks(status);
CREATE INDEX idx_tasks_priority ON tasks(priority);
CREATE INDEX idx_tasks_assignee ON tasks(assignee);
CREATE INDEX idx_tasks_due_date ON tasks(due_date);
CREATE INDEX idx_tasks_created_at ON tasks(created_at);

-- Join table: index on tag for "filter by tag" and on task_id for lookups
CREATE INDEX idx_task_tags_tag ON task_tags(tag);
CREATE INDEX idx_task_tags_task_id ON task_tags(task_id);

-- Task events: by task_id for GET /tasks/{id}/events
CREATE INDEX idx_task_events_task_id ON task_events(task_id);
CREATE INDEX idx_task_events_created_at ON task_events(created_at);

-- Idempotency: expire old keys (for cleanup jobs)
CREATE INDEX idx_idempotency_expires ON idempotency_keys(expires_at);
