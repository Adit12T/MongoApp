# MongoApp

[![CI](https://github.com/Adit12T/MongoApp/actions/workflows/ci.yml/badge.svg)](https://github.com/Adit12T/MongoApp/actions/workflows/ci.yml)
[![CodeQL](https://github.com/Adit12T/MongoApp/actions/workflows/codeql.yml/badge.svg)](https://github.com/Adit12T/MongoApp/actions/workflows/codeql.yml)

A **reactive** user-profile service built with **Java 21, Spring Boot 3.5, Spring WebFlux and
reactive MongoDB**. Every request is handled without blocking a thread, from the HTTP layer down
to the database driver.

I first wrote this in 2018 as a small blocking CRUD app. Version 2 keeps its operations (create,
get, list, update age, delete, delete all) and rebuilds it as a non-blocking service.

## Features

- **Non-blocking end to end:** WebFlux on Netty with the reactive MongoDB driver.
- **Live updates:** `GET /api/v1/users/events` streams create, update and delete events to every
  connected client as Server-Sent Events, with a keep-alive comment every 15 seconds.
- **Aggregation:** `GET /api/v1/users/stats` returns count, average, minimum and maximum age,
  computed inside MongoDB.
- **Paging and filtering:** list users by age range, paged and sorted by name.
- **Role-based access:** `USER` can read and change single users; only `ADMIN` can delete every user.
- **Safe input:** Bean Validation on every field, including a name pattern that rejects MongoDB
  operator injection such as `{$gt: ''}`.
- **Integrity:** a unique index on name (`409 Conflict` on duplicates), optimistic locking, and
  `createdAt` / `updatedAt` audit fields.
- **Credentials from the environment:** the app refuses to start without passwords of at least 12 characters.

## API

| Method | Path | Role | Purpose |
|---|---|---|---|
| `POST` | `/api/v1/users` | USER | Create `{ "name": "...", "age": 30 }` |
| `GET` | `/api/v1/users?minAge=&maxAge=&page=&size=` | USER | List |
| `GET` | `/api/v1/users/{name}` | USER | Get one |
| `PATCH` | `/api/v1/users/{name}` | USER | Change age `{ "age": 31 }` |
| `DELETE` | `/api/v1/users/{name}` | USER | Delete one |
| `DELETE` | `/api/v1/users` | ADMIN | Delete everyone |
| `GET` | `/api/v1/users/stats` | USER | Age statistics |
| `GET` | `/api/v1/users/events` | USER | Live change stream (SSE) |

Interactive docs: `http://localhost:8080/swagger-ui.html`.

## Run it

```bash
docker compose up --build
```

Then, in two terminals:

```bash
curl -N -u user:change-me-user-password localhost:8080/api/v1/users/events
```

```bash
curl -u user:change-me-user-password -X POST localhost:8080/api/v1/users \
  -H 'Content-Type: application/json' -d '{"name":"Asha","age":31}'
```

The first terminal shows the `CREATED` event as soon as the second command runs.

## Configuration

| Variable | Required | Meaning |
|---|---|---|
| `MONGODB_URI` | no (defaults to local MongoDB) | Database connection |
| `APP_USER_PASSWORD` | yes, 12+ characters | Password for `user` |
| `APP_ADMIN_PASSWORD` | yes, 12+ characters | Password for `admin` |

## Tests

```bash
mvn verify
```

Needs Docker. Integration tests run against a real MongoDB container and cover authentication,
roles, validation, duplicates, age filtering, the aggregation and the event stream.

## License

Copyright © 2026 Aditya Tripathi. All rights reserved. The code is published for viewing only; see
[LICENSE](LICENSE). Security reports: see [SECURITY.md](SECURITY.md).
