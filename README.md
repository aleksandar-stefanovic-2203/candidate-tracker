# Candidate Tracker

Candidate Tracker is a web application for managing job applications from submission through review. Candidates apply for positions and submit their CVs through a public form; staff can review applications, update their statuses, and leave notes. The intended stack is Angular, Spring Boot, and PostgreSQL.

## Database (Docker + Flyway)

The repository currently contains the database model and project documentation; application source code is not yet included. Docker Compose starts PostgreSQL and runs the Flyway migrations against it.

Requirements: Docker with the Compose plugin.

```sh
docker compose up -d
docker compose logs flyway
```

The database is available on `localhost:5432` by default. Local development defaults are `candidate_tracker` for the database and user, with password `candidate_tracker_dev`. Set `POSTGRES_DB`, `POSTGRES_USER`, `POSTGRES_PASSWORD`, or `POSTGRES_PORT` in a `.env` file to override them; do not use the development password outside local development.

Flyway migrations live in `db/migration` and are applied automatically after PostgreSQL becomes healthy. To stop the services while retaining database data, run `docker compose down`. To also delete the local database volume, run `docker compose down -v`.
