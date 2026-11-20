# Team-Project-Resource-Management-System-
Team Project Management System is a web-based application designed to help development teams organize and manage their projects. Users can create teams and projects, assign members, and structure project work using Epics, Stories, Tasks, and Bugs. Work items can have priorities, deadlines, estimated working hours, statuses, and assigned team members. The system supports Sprint planning, allowing teams to select and organize work for a specific development period, while a Kanban board provides a clear overview of the current progress. The application also includes basic workload tracking, which uses estimated working hours to show how much work is currently assigned to each team member. The main goal of the system is to provide a simple and structured environment for planning, distributing, and tracking work within a development team.

## Current state

Implemented:

- JWT authentication with refresh token rotation, logout and password change
- Organizations with invite codes, roles (`OWNER` / `ADMIN` / `USER`) and ownership transfer
- Projects with statuses and project members with roles (`ADMIN` / `EXTENDED` / `USER`)
- Issues: Epic, Story, Task and Bug with a status workflow, assignees, epic links, search, filters and sorting
- Comments and issue history
- Kanban board and list views in the web client

Planned: deadlines, estimated hours and time tracking, sprints, workload tracking and notifications.

The full list of API endpoints and access rules is in [`docs/api-methods.md`](docs/api-methods.md).

## Tech stack

- **Backend:** Java 25, Spring Boot 4, Spring Security (stateless JWT), Spring Data JPA, Flyway, PostgreSQL 17
- **Frontend:** React 19, TypeScript, Vite
- **Tests:** JUnit 5, MockMvc, Testcontainers

## Repository structure

```text
backend/    Spring Boot application (com.example.tasktracker)
frontend/   React web client
docs/       API reference, ER diagram, user flows
scripts/    development helpers (demo data seeding)
docker-compose.yml   local PostgreSQL
```

## Running locally

Requirements: Java 25, Node.js 24, Docker.

1. Copy `.env.example` to `.env` in the repository root and set your own `JWT_SECRET` (for example `openssl rand -base64 48`).
2. Start PostgreSQL:
   ```bash
   docker compose up -d
   ```
3. Start the backend on port 8080 (Flyway creates the schema on startup):
   ```bash
   cd backend
   ./mvnw spring-boot:run
   ```
4. Start the frontend on port 5173 (requests to `/api` are proxied to the backend):
   ```bash
   cd frontend
   npm install
   npm run dev
   ```
5. Optionally fill the database with demo users, an organization, projects and issues:
   ```bash
   ./scripts/seed-dev-data.sh
   ```
   All demo accounts share the password from `SEED_PASSWORD` (the default is in the script).

## Tests

```bash
cd backend
./mvnw test
```

Integration tests start PostgreSQL through Testcontainers, so Docker must be running.

# Work Conventions
The repository includes a [`work_conventions.md`](work_conventions.md) file that defines the main development workflow and team rules.

It covers branch usage, branch naming, commit message conventions, pull request requirements, and testing expectations. All contributors should review these conventions before starting development.
