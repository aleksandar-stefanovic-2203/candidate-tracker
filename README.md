# candidate-tracker
Web application for collecting and tracking job applications: candidates apply through a public form, staff review CVs, set statuses and leave notes. Angular + Spring Boot.

## Database (Docker + Flyway)

The repository currently contains the database model and project documentation; application source code is not yet included. Docker Compose starts PostgreSQL and runs the Flyway migrations against it.

Requirements: Docker with the Compose plugin.

```sh
docker compose up -d
docker compose logs flyway
```

The database is available on `localhost:5432` by default. Local development defaults are `candidate_tracker` for the database and user, with password `candidate_tracker_dev`. Set `POSTGRES_DB`, `POSTGRES_USER`, `POSTGRES_PASSWORD`, or `POSTGRES_PORT` in a `.env` file to override them; do not use the development password outside local development.

Flyway migrations live in `db/migration` and are applied automatically after PostgreSQL becomes healthy. To stop the services while retaining database data, run `docker compose down`. To also delete the local database volume, run `docker compose down -v`.
