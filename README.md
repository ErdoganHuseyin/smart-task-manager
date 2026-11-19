# Smart Task Manager

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

Requirements: **JDK 21**. Maven is not required; the project ships with the Maven Wrapper.

```bash
# Run the tests
./mvnw test

# Start the application on http://localhost:8080
./mvnw spring-boot:run

# Check that it is up
curl http://localhost:8080/actuator/health
# {"status":"UP"}
```

On Windows, use `mvnw.cmd` instead of `./mvnw`.

## Roadmap

- [x] Project skeleton, health check, first test
- [ ] Task CRUD API with PostgreSQL
- [ ] User registration and login with JWT
- [ ] AI: create tasks from natural language
- [ ] AI: weekly task summary
- [ ] Docker Compose and GitHub Actions CI
- [ ] Deployment
