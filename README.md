# Candidate Tracker

Candidate Tracker is a web application for managing job applications from submission through review. Candidates apply for positions and submit their CVs through a public form; staff can review applications, update their statuses, and leave notes. The intended stack is Angular, Spring Boot, and PostgreSQL.

## API

### GET

| Method | Path | Query params | Access | Description |
|---|---|---|---|---|
| GET | `/api/positions` | Optional `active` boolean: `true` for active positions, `false` for inactive. If omitted, returns all positions. | public | List positions |
| GET | `/api/applications` | — | public | List all applications |

### POST

| Method | Path | Request body | Access | Description |
|---|---|---|---|---|
| POST | `/api/applications` | `multipart/form-data` with an `application` part containing application details and a `cv` PDF file | public | Submit or update an application by email |

The `application` part uses `name`, `email`, `phone_number`, `recommendation`, and `consent_given_at`. `job_position_id` is optional; omit it or submit `null`/an empty value if no position is selected. `message` is also optional.

The applications endpoint returns an array of application records, including candidate contact details, application status, and job position (which may be `null`). These endpoints are currently public.

## Run with Docker Compose

Docker Compose starts PostgreSQL, applies the Flyway migrations, and builds and starts the Spring Boot API and Angular frontend. Requirements: Docker with the Compose plugin.

```powershell
docker compose up --build -d
docker compose ps
docker compose logs api frontend
```

The frontend is available at `http://localhost:4200`, the API at `http://localhost:8080`, and PostgreSQL at `localhost:5432` by default. The frontend's Nginx server forwards `/api` requests to the API container. Set `FRONTEND_PORT`, `API_PORT`, `POSTGRES_DB`, `POSTGRES_USER`, `POSTGRES_PASSWORD`, or `POSTGRES_PORT` in a `.env` file to override the ports and database settings; do not use the development password outside local development.

Flyway migrations live in `backend/src/main/resources/db/migration` and are applied by the API at startup after PostgreSQL becomes healthy. To stop the services while retaining database data, run `docker compose down`. To also delete the local database volume, run `docker compose down -v`.
