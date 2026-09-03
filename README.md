# BookingEventFlow

[![Java](https://img.shields.io/badge/Java-21-orange)](https://openjdk.org/)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.5-brightgreen)](https://spring.io/projects/spring-boot)
[![Status](https://img.shields.io/badge/status-active%20development-blue)]()
[![License](https://img.shields.io/badge/license-MIT-lightgrey)]()

A production-style event booking platform built with Java and Spring Boot. BookingEventFlow demonstrates the engineering challenges that show up in real distributed systems: keeping data consistent across services that each own their own database, handling concurrent requests without double-booking a seat, and making sure a message gets where it needs to go even when things fail along the way.

> **Status:** Active Development. Event Service, Reservation Service, and Customer Service are implemented and tested end to end. Payment, Booking, Ticket, and Notification Services are planned next.

---

## Table of Contents

- [Overview](#overview)
- [What's Implemented](#whats-implemented)
- [Architecture](#architecture)
  - [Current Architecture](#current-architecture)
  - [Target Architecture](#target-architecture)
  - [Data Ownership](#data-ownership)
  - [Architectural Principles](#architectural-principles)
- [Technology Stack](#technology-stack)
- [Project Structure](#project-structure)
- [Getting Started](#getting-started)
- [Running Tests](#running-tests)
- [Engineering Goals](#engineering-goals)
- [Roadmap](#roadmap)
- [Project Status](#project-status)
- [License](#license)

---

## Overview

BookingEventFlow is a reference project for an event booking platform, built to work through the problems that actually matter once a system moves beyond a single service and a single database:

| Concern | Description |
|---|---|
| Concurrent seat reservations | Prevent double-booking under real contention |
| Temporary holds | Reserve seats with an expiration and automatic release |
| Idempotency | Safely process retries and redelivered messages without duplicating work |
| Event-driven communication | Reliably propagate state between services using change data capture and Kafka |
| Authentication | Issue and verify JWTs across services without a shared secret |
| Rate limiting | Defend authentication endpoints against brute force and denial of service |
| Observability | Expose metrics for every meaningful business operation, not just HTTP status codes |
| Testing | Cover unit, integration, concurrency, and architectural boundaries with automated tests |

The project is built incrementally, and this README reflects what is actually working today, not just what is planned.

---

## What's Implemented

**Event Service** owns event creation, publishing, and lifecycle management, and publishes domain events through a transactional outbox.

**Reservation Service** consumes those events over Kafka (via Debezium change data capture) to build seat inventory, then handles the full reservation lifecycle: hold, confirm, release, and automatic expiry, with pessimistic locking to guarantee two customers can never hold the same seat at once.

**Customer Service** handles registration and login, issuing RSA-signed JWTs and exposing a public JWKS endpoint so other services can verify tokens independently, with no shared secret between them. It also enforces multi-tier rate limiting on its authentication endpoints, load-tested with k6 to confirm the service stays fast and stable under a simulated brute-force attack.

All three services are independently dockerized, run against their own PostgreSQL database, and are covered by unit tests, Testcontainers-backed integration tests, and ArchUnit rules that enforce layering boundaries.

---

# Architecture

BookingEventFlow follows a database-per-service microservice architecture. Each service owns its own persistence boundary and exposes well-defined APIs and messaging contracts. No service reaches directly into another service's database.

Services communicate through:

- Synchronous REST APIs where a request/response interaction makes sense.
- Asynchronous events for propagating state changes, using the transactional outbox pattern, Debezium, and Kafka.

---

## Current Architecture

```text
                         ┌──────────────────────┐
                         │      API / Client      │
                         └──────────┬────────────┘
                                    │
                     ┌──────────────┴──────────────┐
                     ▼                              ▼
          ┌──────────────────┐           ┌──────────────────────┐
          │  Event Service    │           │  Customer Service     │
          │  Spring Boot       │           │  Spring Boot           │
          └────────┬──────────┘           └──────────┬────────────┘
                   │                                 │
                   ▼                                 ▼
          ┌──────────────────┐           ┌──────────────────────┐
          │    Event DB        │           │    Customer DB         │
          │   PostgreSQL        │           │   PostgreSQL            │
          └────────┬──────────┘           └──────────────────────┘
                   │
                   │ Outbox table
                   ▼
          ┌──────────────────┐
          │     Debezium       │
          │  (CDC connector)    │
          └────────┬──────────┘
                   │
                   ▼
          ┌──────────────────┐
          │       Kafka         │
          └────────┬──────────┘
                   │
                   ▼
          ┌──────────────────────┐
          │  Reservation Service   │
          │  Spring Boot             │
          └──────────┬────────────┘
                     │
                     ▼
          ┌──────────────────────┐
          │  Reservation DB         │
          │   PostgreSQL             │
          └──────────────────────┘
```

Event Service publishes domain events (such as `EventPublished`) into an outbox table within its own transaction. Debezium captures those inserts via PostgreSQL's logical replication and forwards them to Kafka. Reservation Service consumes those events to build and manage seat inventory, entirely decoupled from Event Service at the database level.

Customer Service issues JWTs used to authenticate requests. Wiring Event Service and Reservation Service to validate those tokens as OAuth2 resource servers is in progress.

### Implemented Services

| Service | Responsibility | Status |
|---|---|---|
| **Event Service** | Event lifecycle: create, publish, cancel, complete | Implemented |
| **Reservation Service** | Seat inventory, holds, confirmations, and expiry | Implemented |
| **Customer Service** | Registration, authentication, JWT issuance | Implemented |
| Payment Service | Payment processing and payment state | Planned |
| Booking Service | Booking lifecycle and confirmation | Planned |
| Ticket Service | Ticket generation | Planned |
| Notification Service | Customer notifications | Planned |

---

## Target Architecture

```text
                         ┌──────────────────────┐
                         │      API / Client      │
                         └──────────┬────────────┘
                                    │
                                    ▼
                         ┌──────────────────────┐
                         │    Customer Service     │
                         │  Auth / JWT Issuance     │
                         └──────────┬────────────┘
                                    │
                                    ▼
                         ┌──────────────────────┐
                         │      Event Service      │
                         │ Management / Discovery   │
                         └──────────┬────────────┘
                                    │
                                    │ Outbox → Debezium → Kafka
                                    ▼
                         ┌──────────────────────┐
                         │  Reservation Service    │
                         │ Availability / Holds     │
                         │ Reservation TTL           │
                         └──────────┬────────────┘
                                    │
                                    │ Events
                                    ▼
                    ┌────────────────────────────────┐
                    │              Kafka               │
                    │                                  │
                    │ reservation.confirmed            │
                    │ reservation.expired              │
                    │ payment.completed                │
                    │ payment.failed                   │
                    │ booking.confirmed                 │
                    │ booking.cancelled                 │
                    │ ticket.generated                  │
                    └───────┬──────────┬───────────────┘
                            │          │
              ┌─────────────┘          └─────────────┐
              ▼                                      ▼
      ┌─────────────────┐                    ┌─────────────────┐
      │ Payment Service  │                    │ Booking Service   │
      │                   │                    │                   │
      │ Payment State      │                    │ Booking State      │
      └────────┬──────────┘                    └────────┬──────────┘
               │                                        │
               ▼                                        ▼
      ┌─────────────────┐                    ┌─────────────────┐
      │   Payment DB       │                    │   Booking DB        │
      └─────────────────┘                    └─────────────────┘
                            ┌─────────────────┐
                            │  Ticket Service    │
                            │                     │
                            │  Ticket Creation     │
                            └────────┬──────────┘
                                     │
                                     ▼
                            ┌─────────────────┐
                            │    Ticket DB         │
                            └────────┬──────────┘
                                     │
                                     │ Event
                                     ▼
                         ┌──────────────────────┐
                         │  Notification Service   │
                         │                          │
                         │  Email / SMS / Push       │
                         └──────────┬────────────┘
                                    │
                                    ▼
                            External Providers
```

Payment, Booking, Ticket, and Notification Services are not yet implemented. An API Gateway and a Saga-based coordination strategy for the multi-service booking workflow are also planned but not yet built.

---

## Data Ownership

```text
Event Service          → Event DB
Reservation Service     → Reservation DB
Customer Service         → Customer DB
Payment Service          → Payment DB
Booking Service          → Booking DB
Ticket Service            → Ticket DB
Notification Service      → Notification DB
```

No service queries or modifies another service's database directly. Cross-service state moves through APIs and asynchronous events, which keeps ownership explicit and prevents the system from turning into a distributed monolith with shared persistence.

---

## Architectural Principles

- Database per service
- Explicit service ownership
- No shared database access
- Synchronous APIs for request/response interactions
- Transactional outbox for reliable event publication
- Change data capture (Debezium) and Kafka for asynchronous integration
- Idempotent message processing
- Concurrency control at the reservation boundary, using pessimistic locking
- Asymmetric JWT signing, so only the issuing service can create tokens and every other service can verify them independently
- Rate limiting at the authentication boundary
- Automated testing at multiple levels, including architectural boundary enforcement
- Saga-based distributed workflow coordination (planned)
- Retries, timeouts, and circuit breakers for transient failures (planned)
- Observability across service boundaries (partially implemented via Micrometer and Prometheus, distributed tracing planned)

---

# Technology Stack

| Layer | Technologies |
|---|---|
| **Language** | Java 21 |
| **Framework** | Spring Boot 3.5 |
| **Web** | Spring Web |
| **Security** | Spring Security, OAuth2 Resource Server, JWT (RS256) |
| **Rate Limiting** | Bucket4j |
| **Persistence** | Spring Data JPA, Hibernate |
| **Database** | PostgreSQL |
| **Database Migration** | Flyway |
| **Messaging** | Apache Kafka, Kafka Connect |
| **Change Data Capture** | Debezium |
| **Scheduling / Distributed Locking** | ShedLock |
| **Build** | Maven |
| **Testing** | JUnit 5, Mockito, Spring Boot Test |
| **Architecture Testing** | ArchUnit |
| **Infrastructure Testing** | Testcontainers |
| **Load Testing** | k6 |
| **API Documentation** | springdoc-openapi (Swagger UI) |
| **Metrics** | Micrometer, Prometheus |
| **Containerization** | Docker |
| **Local Infrastructure** | Docker Compose |
| **Resilience** | Retry, timeout, circuit breaker (planned) |
| **Tracing** | Distributed tracing (planned) |

---

# Project Structure

```text
BookingEventFlow/
│
├── infrastructure/
│   ├── docker-compose.yml
│   ├── docker-compose.dev.yml
│   ├── docker-compose.full.yml
│   ├── platform/
│   │   ├── kafka/
│   │   └── services/
│   │       ├── event-service/
│   │       ├── reservation-service/
│   │       └── customer-service/
│   └── postgres/
│
├── libraries/
│   └── common-domain/
│
├── services/
│   ├── event-service/
│   ├── reservation-service/
│   └── customer-service/
│       ├── src/
│       │   ├── main/
│       │   │   ├── java/
│       │   │   └── resources/
│       │   │       ├── application.yml
│       │   │       ├── application-dev.yml
│       │   │       ├── application-docker.yml
│       │   │       └── db/migration/
│       │   └── test/
│       ├── Dockerfile
│       └── pom.xml
│
├── scripts/
│   └── benchmarks/
│
├── .env.example
├── .gitignore
├── pom.xml
└── README.md
```

The project is a multi-module Maven build. Each service stays independently organized, sharing only what lives in `common-domain`.

---

# Getting Started

## Prerequisites

- JDK 21+
- Maven
- Docker and Docker Compose
- OpenSSL (for generating the RSA key pair used by Customer Service)

Verify your setup:

```bash
java -version
mvn -version
docker --version
docker compose version
openssl version
```

## Environment Configuration

Copy the example environment file:

```bash
cp .env.example .env
```

On Windows PowerShell:

```powershell
Copy-Item .env.example .env
```

Fill in the required values. Each service has its own section in `.env.example`, covering its port, database credentials, and any service-specific configuration.

## Generating the JWT Signing Key

Customer Service signs tokens with an RSA key pair. Generate one before running it for the first time:

```bash
cd services/customer-service

openssl genpkey -algorithm RSA -out private.pem -pkeyopt rsa_keygen_bits:2048
openssl rsa -pubout -in private.pem -out src/main/resources/keys/public.pem
openssl pkcs8 -topk8 -inform PEM -outform PEM -nocrypt -in private.pem -out private_pkcs8.pem
```

Move `private_pkcs8.pem` outside the repository, then point `JWT_PRIVATE_KEY_PATH` in `.env` at its new location. The public key stays in the repository, since it is safe to share and is what other services use to verify tokens.

## Running the Full Stack with Docker

Bring up every service and its infrastructure together:

```bash
cd infrastructure
docker compose -f docker-compose.dev.yml up -d
docker compose -f docker-compose.dev.yml ps
```

Register the outbox connector so Event Service's changes reach Kafka:

```powershell
.\register-outbox-connector.ps1
```

```bash
./register-outbox-connector.sh
```

Tear the stack down when you're done:

```bash
docker compose -f docker-compose.dev.yml down
```

## Running a Service Locally

If you'd rather iterate on one service without rebuilding a container each time, run it directly with Maven. Bring up its database (and Kafka, if needed) via Docker first, then:

```bash
cd services/event-service
mvn spring-boot:run
```

Each service reads its configuration from `application-dev.yml` when `SPRING_PROFILES_ACTIVE=dev` is set.

## Build the Whole Project

From the repository root:

```bash
mvn clean install
```

---

# Running Tests

Run the full test suite from the repository root:

```bash
mvn clean test
```

Integration tests use Testcontainers, so they don't depend on PostgreSQL running on your machine. Each test spins up its own disposable, isolated database container.

The test suite includes:

- Unit tests for service logic
- Testcontainers-backed integration tests, including a concurrency test that proves only one of several simultaneous requests can hold the same seat
- Controller tests with `@WebMvcTest`
- ArchUnit tests that enforce package layering and naming conventions
- A k6 load-testing benchmark (`scripts/benchmarks/login-benchmark.js`) that verifies Customer Service's rate limiting holds under sustained concurrent load

---

# Engineering Goals

**Correctness before optimization.** A booking system that's fast but allows double-booking is fundamentally broken.

**Explicit domain boundaries.** Each service has a clearly defined responsibility and owns its own data.

**Safe concurrency.** The reservation boundary stays correct under real concurrent access, proven with automated tests, not just assumed.

**Idempotent operations.** Retries and redelivered messages should never duplicate a reservation or a business outcome.

**Independent verification.** Services trust each other's signed tokens without sharing secrets, and can be tested and deployed independently.

**Automated testing.** Business-critical behavior is covered by tests before new capabilities are layered on top.

**Production-oriented, not production-claiming.** The project demonstrates the practices a production system needs without claiming to be one yet.

---

# Roadmap

## Foundation
- [x] Multi-module project structure
- [x] Event Service
- [x] PostgreSQL persistence per service
- [x] Flyway migrations
- [x] REST APIs with global exception handling
- [x] Docker Compose local infrastructure, split by platform concern
- [x] Environment-based configuration

## Reservation
- [x] Reservation Service
- [x] Seat inventory built from consumed events
- [x] Seat-level concurrency control with pessimistic locking
- [x] Double-booking prevention, verified under concurrent load
- [x] Temporary holds with expiration
- [x] Automatic expiry via a distributed-lock-guarded scheduler
- [x] Reservation confirm and release flows
- [x] Idempotent event consumption

## Event-Driven Communication
- [x] Transactional outbox pattern
- [x] Debezium change data capture
- [x] Kafka as the event backbone
- [ ] Domain event versioning strategy
- [ ] Additional domain events beyond `EventPublished`

## Authentication
- [x] Customer Service
- [x] Registration and login
- [x] Asymmetric JWT signing (RS256) with a public JWKS endpoint
- [x] Protected profile endpoint
- [x] Multi-tier rate limiting against brute force and denial of service
- [x] Load-tested with k6
- [ ] Wire Event Service and Reservation Service as OAuth2 resource servers
- [ ] Move reservation ownership from a client-supplied field to the authenticated JWT claim
- [ ] Refresh tokens and token revocation
- [ ] Role-based authorization for administrative operations

## Payment
- [ ] Payment Service
- [ ] Payment state machine
- [ ] Payment idempotency
- [ ] Success and failure handling
- [ ] Retry handling for delayed payment responses

## Distributed Workflow
- [ ] Define the booking Saga
- [ ] Saga orchestration
- [ ] Compensation actions
- [ ] Partial workflow failure handling
- [ ] Duplicate event delivery handling beyond what's already covered

## Reliability
- [ ] Retry policies
- [ ] Timeout policies
- [ ] Circuit breakers
- [ ] Dead-letter handling

## Ticketing and Notifications
- [ ] Ticket Service
- [ ] Notification Service
- [ ] Email notifications
- [ ] Idempotent notification processing

## Observability
- [x] Application metrics via Micrometer and Prometheus
- [x] Per-operation dimensional metrics with outcome tagging
- [ ] Distributed tracing
- [ ] Correlation IDs across service boundaries
- [ ] Kafka consumer and producer metrics
- [ ] Centralized dashboards (Grafana)

## Platform
- [ ] API Gateway
- [ ] Centralized authentication enforcement at the gateway
- [ ] CI/CD pipeline
- [ ] Secrets management strategy for deployment

---

# Project Status

**Active Development**

Event Service, Reservation Service, and Customer Service are implemented, tested, and running together as a working event-driven system. The core engineering challenges the project set out to demonstrate, concurrency safety, event-driven consistency, and independent service authentication, are proven with real automated tests and load benchmarks, not just described.

Payment, Booking, Ticket, and Notification Services, along with Saga coordination, an API Gateway, and full observability, are the next phases of work.

This project is not production-ready. It's a working demonstration of the engineering decisions a real distributed booking system requires, built and verified incrementally.

---

# License

MIT
