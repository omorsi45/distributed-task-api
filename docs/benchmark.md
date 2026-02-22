# Caching and performance

## Why caching helps

- **GET by ID** is cached (Caffeine). Repeated reads for the same task hit the cache and avoid DB round-trips, reducing latency and load.
- **List** can be cached for common filter combinations (e.g. no filters, first page); we evict on any write so data stays consistent.

## How to observe cache hits

1. **Logs**: Set `logging.level.com.taskapi=DEBUG` and watch for cache-related logs if you add cache logging.
2. **Metrics**: Call `GET /actuator/metrics/cache.gets` and `cache.puts` (with API key). After warming up with repeated GET by ID, `cache.gets` will increase on cache hits.
3. **Manual test**: Create a task, then call `GET /api/v1/tasks/{id}` multiple times; the second and subsequent calls are served from cache (no DB query).

## Optional: simple load check with curl

```bash
# Create a task and capture id
TASK_ID=$(curl -s -X POST http://localhost:8080/api/v1/tasks \
  -H "X-API-Key: $API_KEY" -H "Content-Type: application/json" \
  -d '{"title":"Bench"}' | jq -r '.data.id')

# Repeated GETs (cache warms after first call)
for i in $(seq 1 20); do
  curl -s -o /dev/null -w "%{time_total}\n" -H "X-API-Key: $API_KEY" "http://localhost:8080/api/v1/tasks/$TASK_ID"
done
```

First request is typically slower (DB); subsequent ones are faster (cache).
