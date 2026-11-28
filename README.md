# Smart Task Manager

[![CI](https://github.com/ErdoganHuseyin/smart-task-manager/actions/workflows/ci.yml/badge.svg)](https://github.com/ErdoganHuseyin/smart-task-manager/actions/workflows/ci.yml)

An AI-powered task management REST API built with **Java 21** and **Spring Boot 4**.

Users can manage their tasks through a secure REST API, and create tasks from plain
natural language (e.g. *"Meeting with Ali tomorrow at 3pm, urgent"*), which an LLM turns
into a structured task with a title, due date and priority.

> 🚧 Work in progress. This project is being built step by step; see the roadmap below.

## Tech stack

| Area          | Technology                                 |
|---------------|--------------------------------------------|
| Language      | Java 21                                    |
| Framework     | Spring Boot 4 (Spring MVC)                 |
| Database      | PostgreSQL, Spring Data JPA, Flyway        |
| Security      | Spring Security, JWT                       |
| AI            | Spring AI + Anthropic Claude               |
| Testing       | JUnit 5, MockMvc, Testcontainers           |
| DevOps        | Docker Compose, GitHub Actions             |

## Getting started

Requirements: **JDK 21** and **Docker**. Maven is not required; the project ships with the Maven Wrapper.

```bash
# Start PostgreSQL locally
docker compose up -d

# Run the tests (Testcontainers starts its own PostgreSQL)
./mvnw test

# Start the application on http://localhost:8080
./mvnw spring-boot:run
```

On Windows, use `mvnw.cmd` instead of `./mvnw`.

### Run everything in Docker

No JDK needed, only Docker. This builds the app image and starts it together with PostgreSQL:

```bash
docker compose --profile app up --build
```

To enable the AI endpoints, put `ANTHROPIC_API_KEY=...` in a `.env` file next to `compose.yaml`
(it is git-ignored) or export it in your shell first. Without a key, the AI endpoints answer 503.

- API docs (Swagger UI): http://localhost:8080/swagger-ui.html
- Health check: http://localhost:8080/actuator/health

Database settings can be overridden with the `DB_URL`, `DB_USERNAME` and `DB_PASSWORD` environment variables.

## API

| Method | Endpoint             | Description                                      |
|--------|----------------------|--------------------------------------------------|
| GET    | `/api/tasks`         | List tasks (paged, optional `?status=TODO`)      |
| GET    | `/api/tasks/{id}`    | Get a task                                       |
| POST   | `/api/tasks`         | Create a task                                    |
| PUT    | `/api/tasks/{id}`    | Update a task                                    |
| DELETE | `/api/tasks/{id}`    | Delete a task                                    |

Example:

```bash
curl -X POST http://localhost:8080/api/tasks \
  -H "Content-Type: application/json" \
  -d '{"title": "Prepare for interview", "priority": "HIGH", "dueDate": "2026-12-01T09:00:00Z"}'
```

Errors use the standard [problem details](https://www.rfc-editor.org/rfc/rfc9457) format:

```json
{"status": 404, "title": "Not Found", "detail": "Task not found: 99", "instance": "/api/tasks/99"}
```

## Project structure

```
src/main/java/.../taskmanager
├── task/                  # Task feature: entity, repository, service, controller
│   └── dto/               # Request/response records (the API contract)
└── common/                # Cross-cutting: error handling, web config
src/main/resources/db/migration   # Flyway SQL migrations (database schema history)
```

## Roadmap

- [x] Project skeleton, health check, first test
- [x] Task CRUD API with PostgreSQL
- [ ] User registration and login with JWT
- [ ] AI: create tasks from natural language
- [ ] AI: weekly task summary
- [ ] Docker Compose and GitHub Actions CI
- [ ] Deployment
