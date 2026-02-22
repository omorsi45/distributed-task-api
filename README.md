# Distributed Task Management API

Production-ready REST API to manage tasks across distributed services. Built with **Java 21**, **Spring Boot 3**, **PostgreSQL**, and **Docker**. Includes caching, optimized DB schema with indexes, idempotency, optimistic locking, API key auth, and rate limiting.

## Stack

- Java 21, Spring Boot 3.2
- PostgreSQL 16, Flyway migrations
- Spring Data JPA, MapStruct
- Caffeine cache (Redis-swappable)
- Docker & docker-compose
- OpenAPI/Swagger, Actuator (health, metrics)
- Testcontainers for integration tests

## Quick Start

### Run with Docker (recommended)

```bash
docker compose up --build
```

- API: http://localhost:8080  
- Swagger UI: http://localhost:8080/swagger-ui.html  
- Health: http://localhost:8080/actuator/health  

Default API key: `docker-api-key` (set `API_KEY` in env to override).

### Run locally

1. **Prerequisites**: Java 21, PostgreSQL 16 (or use Docker for DB only: `docker compose up db`).
2. **Database**: Create DB and user, or use defaults:
   - DB: `taskdb`, user: `taskuser`, password: `taskpass`.
3. **Environment**: Copy `.env.example` to `.env` and set `API_KEY`, DB URL, etc.
4. **Build**: `./gradlew bootJar` (or `gradle wrapper` then `./gradlew bootJar` if wrapper is missing).
5. **Run**: `./gradlew bootRun` or `java -jar build/libs/distributed-task-api-1.0.0.jar`.

### Makefile

- `make build` – build JAR  
- `make run` – run app (expects DB + env)  
- `make test` – run tests (Testcontainers)  
- `make docker-up` – start app + DB in background  
- `make docker-down` – stop containers  

On Windows use `gradlew.bat` or run the equivalent Gradle commands.

## API Overview

| Method | Endpoint | Description |
|--------|----------|-------------|
| POST | `/api/v1/tasks` | Create task (optional `Idempotency-Key` header) |
| GET | `/api/v1/tasks/{id}` | Get task by ID |
| PUT | `/api/v1/tasks/{id}` | Full update |
| PATCH | `/api/v1/tasks/{id}/status` | Update status only |
| DELETE | `/api/v1/tasks/{id}` | Delete task |
| GET | `/api/v1/tasks` | List with filters, pagination, sort |
| GET | `/api/v1/tasks/{id}/events` | Task event history |
| POST | `/api/v1/tasks/bulk` | Create multiple tasks |

**Auth**: Send `X-API-Key: <your-key>` on every request (except health/docs).  
**Response format**: `{ "data": ..., "meta": ... }` or `{ "error": { "code", "message" } }`.

List query params: `status`, `priority`, `assignee`, `tag`, `dueBefore`, `dueAfter`, `textSearch`, `page`, `size`, `sort`, `direction`.

See [docs/curl.md](docs/curl.md) and [postman_collection.json](postman_collection.json) for examples.

## Caching

- **Where**: `TaskService.getById()` and list for common filter combos (cache name `tasks` and `taskList`).
- **Provider**: Caffeine in-memory by default (5 min TTL, max 1000 entries). Set `CACHE_PROVIDER=redis` and configure Redis to use Redis (add `spring-boot-starter-data-redis` and a `RedisCacheManager` bean).
- **Invalidation**: On create, update, status change, and delete we evict all entries for `tasks` and `taskList` (`@CacheEvict(allEntries = true)`).
- **Proving cache hits**: Enable debug logs for `com.taskapi` and watch for cache stats, or call `GET /actuator/metrics/cache.gets` (and `cache.puts`) after enabling metrics.

## Database & Indexes

- **Schema**: UUID primary keys, `task_events.payload` as JSONB, tags in join table `task_tags` (flexible indexing and “filter by tag”).
- **Indexes** (see `db/migration/V1__*` and `V2__*`):
  - `idx_tasks_status`, `idx_tasks_priority`, `idx_tasks_assignee`, `idx_tasks_due_date`, `idx_tasks_created_at` – for list filters and sort.
  - `idx_task_tags_tag`, `idx_task_tags_task_id` – for filter by tag and joins.
  - `idx_task_events_task_id`, `idx_task_events_created_at` – for event history.
  - `idx_tasks_search_vector` (GIN on `search_vector`) – full-text search on title/description (tsvector).
- **EXPLAIN**: For the list endpoint query, Postgres uses these indexes for filter + sort, e.g. `Index Scan using idx_tasks_status` when `status` is provided; GIN index for `textSearch`.

## Idempotency

- Send `Idempotency-Key: <unique-key>` on **POST /api/v1/tasks**.
- Key is hashed (SHA-256) and stored with the created task ID and TTL (default 24h).
- Duplicate request with same key returns the same task (same ID) and does not create a second task.

## Optimistic Locking & 409

- Each task has a `version` field. On PUT or PATCH, if the task was updated by another request, the save throws and the API returns **409 Conflict** with message “Task was modified by another request”.

## Observability

- **Health**: `GET /actuator/health` (DB and app).
- **Metrics**: `GET /actuator/metrics` (with API key).
- **Logs**: Structured; level `com.taskapi: DEBUG` for app logs.

## Tests

- **Unit**: Service and repository logic.
- **Integration**: Full API with **Testcontainers** (PostgreSQL).

Run: `./gradlew test`.

## License

MIT.
