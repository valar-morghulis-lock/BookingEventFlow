# BookingEventFlow

[![Java](https://img.shields.io/badge/Java-21-orange)](https://openjdk.org/)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.x-brightgreen)](https://spring.io/projects/spring-boot)
[![Status](https://img.shields.io/badge/status-early%20development-yellow)]()
[![License](https://img.shields.io/badge/license-TBD-lightgrey)]()

A production-style event booking platform built with Java and Spring Boot, designed to demonstrate real-world microservice architecture: concurrency control, transactional consistency, resilience, and distributed workflow coordination.

> **Status:** Early Development 🚧 — Event Service implemented; remaining services in design/build.

## Table of Contents

- [Overview](#overview)
- [Architecture](#architecture)
- [Current Progress](#current-progress)
- [Technology Stack](#technology-stack)
- [Project Structure](#project-structure)
- [Getting Started](#getting-started)
- [Running Tests](#running-tests)
- [Engineering Goals](#engineering-goals)
- [Roadmap](#roadmap)
- [Contributing](#contributing)
- [License](#license)

## Overview

BookingEventFlow is an event booking platform built as a production-oriented reference project. It focuses not just on business functionality, but on the hard engineering problems inherent to distributed booking systems:

| Concern | Description |
|---|---|
| Concurrent seat reservations | Prevent double-booking under high contention |
| Temporary holds | Reservation TTLs with expiration/release |
| Idempotency | Safe retries for reservation, payment, and booking operations |
| Payment processing | State machine for payment lifecycle |
| Async communication | Reliable messaging between services |
| Failure handling | Retries, timeouts, circuit breaking |
| Distributed workflow | Saga-based coordination across services |
| Observability | Tracing, metrics, structured logging |
| Testing | Unit, integration, and concurrency test coverage |

## Architecture

Independently deployable microservices, each owning its own persistence boundary and exposing well-defined APIs/messaging contracts. Services do **not** share databases -> communication happens through APIs and asynchronous events only.

| Service | Responsibility | Status |
|---|---|---|
| **Event Service** | Event management and discovery | ✅ Implemented |
| **Reservation Service** | Seat availability, temporary holds, TTL | 🚧 Planned |
| **Payment Service** | Payment processing and state | 🚧 Planned |
| **Booking Service** | Booking lifecycle and confirmation | 🚧 Planned |
| **Ticket Service** | Ticket generation | 🚧 Planned |
| **Notification Service** | Customer notifications (email/SMS/push) | 🚧 Planned |

### Current Architecture

Only the Event Service is implemented today. It runs as a standalone Spring Boot service backed by its own PostgreSQL database, there is no inter-service communication yet.

```text
┌─────────────┐
│ API / Client│
└──────┬──────┘
       ▼
┌──────────────┐
│ Event Service│
└──────┬───────┘
       ▼
┌──────────────┐
│  Event DB    │
│  PostgreSQL  │
└──────────────┘
```

### Target Architecture

BookingEventFlow is designed as a **database-per-service, event-driven microservice architecture**, coordinated through Kafka. This reflects where the project is heading, not what's built yet.

```text
                              ┌──────────────────────┐
                              │      API / Client     │
                              └──────────┬────────────┘
                                         ▼
                              ┌──────────────────────┐
                              │    Event Service      │
                              │  Management/Discovery │
                              └──────────┬────────────┘
                                         ▼
                              ┌──────────────────────┐
                              │ Reservation Service   │
                              │  Availability / Holds │
                              │  Reservation TTL      │
                              └──────────┬────────────┘
                                         │ events
                                         ▼
                    ┌────────────────────────────────────────┐
                    │                  Kafka                  │
                    │  reservation.created / .expired         │
                    │  payment.completed / .failed            │
                    │  booking.confirmed / ticket.generated   │
                    └───────┬──────────┬──────────┬───────────┘
                            ▼          ▼          ▼
                    ┌────────────┐ ┌────────────┐ ┌──────────────┐
                    │  Payment   │ │  Booking   │ │    Ticket    │
                    │  Service   │ │  Service   │ │   Service    │
                    └─────┬──────┘ └─────┬──────┘ └──────┬───────┘
                          ▼               ▼               ▼
                    ┌──────────┐    ┌──────────┐    ┌──────────┐
                    │Payment DB│    │Booking DB│    │Ticket DB │
                    └──────────┘    └──────────┘    └──────────┘
                                           │
                                           │ booking/ticket events
                                           ▼
                                  ┌─────────────────────┐
                                  │ Notification Service │
                                  │  Email / SMS / Push  │
                                  └──────────┬────────────┘
                                             ▼
                                    External Providers
```

Each service in the diagram above owns its own database (Event DB, Reservation DB, Payment DB, Booking DB, Ticket DB, Notification DB) -> no shared schemas.

### Booking Workflow

The core of BookingEventFlow isn't "microservices + Kafka" — it's the **correctness of the booking workflow** across service boundaries:

```text
Customer
   │
   ▼
Reserve Seats ──> Reservation Service
                     ├── validate availability
                     ├── acquire concurrency control
                     ├── create temporary reservation
                     └── publish ReservationCreated
                              │
                              ▼ Kafka
                       Payment Service
                        ├── SUCCESS → PaymentCompleted
                        └── FAILURE → PaymentFailed
                              │
                              ▼ Kafka
                       Booking Service
                        ├── CONFIRM → BookingConfirmed
                        └── CANCEL  → ReleaseReservation
                              │
                              ▼ Kafka
                    ┌─────────┴─────────┐
                    ▼                   ▼
             Ticket Service     Notification Service
```

A distributed workflow coordinator (see [Saga coordination](#roadmap)) will own this sequencing —> this is the primary engineering story the project demonstrates, not incidental plumbing.

### Data Ownership

Each microservice owns its own persistence boundary. Services must not directly access another service's database.

```text
Event Service        → Event DB
Reservation Service  → Reservation DB
Payment Service      → Payment DB
Booking Service      → Booking DB
Ticket Service       → Ticket DB
Notification Service → Notification DB
```

Communication between services occurs through APIs and asynchronous events, never shared database access.

### Architectural Principles

- Database per service
- No shared database access
- Synchronous APIs for request/response interactions
- Asynchronous messaging for distributed workflows
- Event-driven integration through Kafka
- Transactional consistency within service boundaries
- Idempotent message processing
- Concurrency control at the reservation boundary
- Saga-based distributed workflow coordination
- Transactional Outbox for reliable event publication
- Resilience through retries, timeouts, and circuit breakers
- Observability across service boundaries

> Kafka, Saga, and Outbox are **target architecture** — labeled as planned capabilities until implemented (tracked in [Roadmap](#roadmap)).

## Current Progress

### Event Service ✅

- CRUD (create, retrieve, update, delete)
- Request/response DTO layer
- Domain model / persistence entity separation
- Global REST exception handling
- Bean validation
- PostgreSQL persistence via Flyway migrations
- Integration and concurrency testing with Testcontainers

### Infrastructure

- PostgreSQL via Docker Compose
- Environment-based configuration (Spring profiles)
- Testcontainers for isolated integration tests
- `.env`-based local secrets, excluded from Git

## Technology Stack

| Layer | Technologies |
|---|---|
| **Language / Framework** | Java 21, Spring Boot, Spring Web, Spring Data JPA, Hibernate |
| **Persistence** | PostgreSQL, Flyway |
| **Build** | Maven |
| **Testing** | JUnit 5, Spring Boot Test, Mockito, Testcontainers |
| **Infrastructure** | Docker, Docker Compose |

## Project Structure

```text
BookingEventFlow/
├── infrastructure/
│   └── docker-compose.yml
│
├── libraries/
│   └── common-domain/
│
├── services/
│   └── event-service/
│
├── .env.example
├── pom.xml
└── README.md
```

## Getting Started

### Prerequisites

- JDK 21+
- Maven
- Docker & Docker Compose

### Environment Configuration

```bash
cp .env.example .env
```

Configure the required values in `.env`. This file is excluded from Git —> Kindly supply your own configuration accordingly.

### Start Local Infrastructure

```bash
docker compose -f infrastructure/docker-compose.yml up -d
```

Verify containers are running:

```bash
docker compose -f infrastructure/docker-compose.yml ps
```

Stop infrastructure:

```bash
docker compose -f infrastructure/docker-compose.yml down
```

### Build & Run

```bash
mvn clean install
cd services/event-service
mvn spring-boot:run
```

## Running Tests

```bash
mvn clean test
```

Integration and concurrency tests use **Testcontainers**, so they don't depend on a local PostgreSQL installation — each run spins up an isolated, disposable database container.

## Engineering Goals

- Correctness before optimization
- Explicit domain boundaries
- Strong transactional guarantees
- Idempotent distributed operations
- Safe concurrency under contention
- Failure-aware communication
- Independent service ownership
- Automated testing at every layer
- Observable systems by design
- Production-oriented architecture

The goal isn't just a working booking app — it's a demonstration of how to design a distributed booking system around correctness, concurrency, resilience, and failure handling.

## Roadmap

- [x] Establish project structure
- [x] Implement initial Event Service
- [x] Introduce PostgreSQL persistence
- [x] Introduce Flyway migrations
- [x] Add REST API
- [x] Add global exception handling
- [x] Add integration and concurrency tests
- [x] Introduce Docker-based local infrastructure
- [x] Introduce environment-based configuration
- [ ] Implement Reservation Service
- [ ] Implement seat inventory and concurrency control
- [ ] Implement reservation expiration
- [ ] Implement Payment Service
- [ ] Introduce asynchronous messaging (Kafka)
- [ ] Implement distributed booking workflow
- [ ] Define and implement Saga coordination strategy (leaning orchestration — explicit business sequencing)
- [ ] Introduce resilience patterns (retry, circuit breaker, outbox)
- [ ] Introduce observability (tracing, metrics, structured logs)
- [ ] Add end-to-end testing
- [ ] Define production deployment architecture


## Project Status

**Early Development 🚧**

BookingEventFlow is currently a production-style event booking platform under active, incremental development, with particular emphasis on distributed-systems challenges: concurrency and double-booking prevention, transactional consistency, idempotency, temporary reservations, failure recovery, asynchronous communication, distributed workflow coordination, and observability.

The system should not be considered production-ready. The current implementation is the foundation for the broader architecture and upcoming booking workflow.

## License

TBD
