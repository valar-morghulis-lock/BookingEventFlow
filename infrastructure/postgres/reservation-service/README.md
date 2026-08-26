# Postgres init scripts — reservation-service

Files placed here run automatically on **first initialization** of the
reservation-service-db container (empty data volume only — will not
re-run against an existing volume).

Use for: extensions, seed data, or one-time setup SQL specific to this
service's database.

Naming convention: numbered prefix for run order, e.g.
`01-extensions.sql`, `02-seed-data.sql`.