# cURL Examples

Use `API_KEY` from your environment or `.env`. Default with Docker: `docker-api-key`.

## Health & Metrics

```bash
# Health
curl -s http://localhost:8080/actuator/health | jq

# Metrics (when authorized)
curl -s -H "X-API-Key: $API_KEY" http://localhost:8080/actuator/metrics | jq
```

## Tasks

### Create task

```bash
curl -s -X POST http://localhost:8080/api/v1/tasks \
  -H "X-API-Key: $API_KEY" \
  -H "Content-Type: application/json" \
  -d '{
    "title": "Implement feature X",
    "description": "Details here",
    "status": "TODO",
    "priority": "HIGH",
    "assignee": "alice",
    "tags": ["backend", "api"]
  }' | jq
```

### Create with idempotency key (duplicate request returns same task)

```bash
KEY=$(uuidgen)
curl -s -X POST http://localhost:8080/api/v1/tasks \
  -H "X-API-Key: $API_KEY" \
  -H "Idempotency-Key: $KEY" \
  -H "Content-Type: application/json" \
  -d '{"title": "Idempotent task"}' | jq

# Same key again -> same task id
curl -s -X POST http://localhost:8080/api/v1/tasks \
  -H "X-API-Key: $API_KEY" \
  -H "Idempotency-Key: $KEY" \
  -H "Content-Type: application/json" \
  -d '{"title": "Idempotent task"}' | jq
```

### Get by ID

```bash
curl -s -H "X-API-Key: $API_KEY" http://localhost:8080/api/v1/tasks/{id} | jq
```

### List with filters and pagination

```bash
# Default list
curl -s -H "X-API-Key: $API_KEY" "http://localhost:8080/api/v1/tasks?page=0&size=20" | jq

# Filter by status and assignee
curl -s -H "X-API-Key: $API_KEY" "http://localhost:8080/api/v1/tasks?status=TODO&assignee=alice" | jq

# Text search, due date range, tag
curl -s -H "X-API-Key: $API_KEY" "http://localhost:8080/api/v1/tasks?textSearch=bug&tag=urgent&dueBefore=2025-12-31T23:59:59Z" | jq

# Sort
curl -s -H "X-API-Key: $API_KEY" "http://localhost:8080/api/v1/tasks?sort=dueDate&direction=asc" | jq
```

### Update task

```bash
curl -s -X PUT http://localhost:8080/api/v1/tasks/{id} \
  -H "X-API-Key: $API_KEY" \
  -H "Content-Type: application/json" \
  -d '{"title":"Updated title","status":"IN_PROGRESS","priority":"URGENT"}' | jq
```

### Patch status

```bash
curl -s -X PATCH http://localhost:8080/api/v1/tasks/{id}/status \
  -H "X-API-Key: $API_KEY" \
  -H "Content-Type: application/json" \
  -d '{"status":"DONE"}' | jq
```

### Delete task

```bash
curl -s -X DELETE -H "X-API-Key: $API_KEY" http://localhost:8080/api/v1/tasks/{id} -w "%{http_code}"
```

### Get task events

```bash
curl -s -H "X-API-Key: $API_KEY" "http://localhost:8080/api/v1/tasks/{id}/events?page=0&size=20" | jq
```

### Bulk create

```bash
curl -s -X POST http://localhost:8080/api/v1/tasks/bulk \
  -H "X-API-Key: $API_KEY" \
  -H "Content-Type: application/json" \
  -d '[
    {"title": "Task 1", "status": "TODO"},
    {"title": "Task 2", "priority": "HIGH"}
  ]' | jq
```
