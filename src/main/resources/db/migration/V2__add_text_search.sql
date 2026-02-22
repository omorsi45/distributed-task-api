-- Full-text search: tsvector on title + description for textSearch filter
-- Justification: tsvector + GIN gives efficient full-text search; pg_trgm used only if needed
ALTER TABLE tasks ADD COLUMN IF NOT EXISTS search_vector tsvector
    GENERATED ALWAYS AS (
        setweight(to_tsvector('english', coalesce(title, '')), 'A') ||
        setweight(to_tsvector('english', coalesce(description, '')), 'B')
    ) STORED;

CREATE INDEX idx_tasks_search_vector ON tasks USING GIN(search_vector);
