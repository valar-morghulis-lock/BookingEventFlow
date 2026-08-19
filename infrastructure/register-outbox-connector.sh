#!/usr/bin/env bash
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(cd "${SCRIPT_DIR}/.." && pwd)"

# Load the single project-level .env
set -a
source "${PROJECT_ROOT}/.env"
set +a

envsubst < "${SCRIPT_DIR}/outbox-connector.template.json" \
  > /tmp/outbox-connector.json

curl -X POST "http://localhost:${KAFKA_CONNECT_PORT}/connectors" \
  -H "Content-Type: application/json" \
  -d @/tmp/outbox-connector.json