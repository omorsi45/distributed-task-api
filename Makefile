.PHONY: build run test docker-up docker-down clean

# Build the application
build:
	./gradlew bootJar

# Run locally (expects PostgreSQL and env or .env)
run:
	./gradlew bootRun

# Run tests
test:
	./gradlew test

# Run tests with coverage (if jacoco applied)
test-cov:
	./gradlew test jacocoTestReport

# Docker: build and run app + db
docker-up:
	docker compose up --build -d

# Docker: run in foreground
docker-up-fg:
	docker compose up --build

# Docker: stop
docker-down:
	docker compose down

# Full clean
clean:
	./gradlew clean
	rm -rf build

# Migrations (for local dev: run Flyway via Spring Boot or manually)
migrate:
	./gradlew bootRun --args='--spring.flyway.clean-disabled=false'

# Curl examples (requires API_KEY and running server)
curl-health:
	curl -s http://localhost:8080/actuator/health | jq .

curl-tasks:
	curl -s -H "X-API-Key: $${API_KEY:-docker-api-key}" http://localhost:8080/api/v1/tasks | jq .

curl-create:
	curl -s -X POST -H "X-API-Key: $${API_KEY:-docker-api-key}" -H "Content-Type: application/json" \
	  -d '{"title":"Make task","status":"TODO","priority":"MEDIUM"}' \
	  http://localhost:8080/api/v1/tasks | jq .
