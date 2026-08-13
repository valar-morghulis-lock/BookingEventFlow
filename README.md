# BookingEventFlow

[![Java](https://img.shields.io/badge/Java-21-orange)](https://openjdk.org/)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.5-brightgreen)](https://spring.io/projects/spring-boot)
[![Status](https://img.shields.io/badge/status-early%20development-yellow)]()
[![License](https://img.shields.io/badge/license-TBD-lightgrey)]()

A production-style event booking platform built with Java and Spring Boot, designed to demonstrate real-world microservice architecture, concurrency control, transactional consistency, resilience, and distributed workflow coordination.

> **Status:** Early Development 🚧 — Event Service implemented; remaining services are planned and under development.

---

## Table of Contents

- [Overview](#overview)
- [Architecture](#architecture)
  - [Current Architecture](#current-architecture)
  - [Target Architecture](#target-architecture)
  - [Booking Workflow](#booking-workflow)
  - [Data Ownership](#data-ownership)
  - [Architectural Principles](#architectural-principles)
- [Current Progress](#current-progress)
- [Technology Stack](#technology-stack)
- [Project Structure](#project-structure)
- [Getting Started](#getting-started)
- [Running Tests](#running-tests)
- [Engineering Goals](#engineering-goals)
- [Roadmap](#roadmap)
- [Project Status](#project-status)
- [Contributing](#contributing)
- [License](#license)

---

## Overview

BookingEventFlow is a production-oriented reference project for an event booking platform.

The project focuses not only on implementing business functionality, but also on the engineering challenges that arise when building distributed booking systems:

| Concern | Description |
|---|---|
| Concurrent seat reservations | Prevent double-booking under high contention |
| Temporary holds | Reserve seats temporarily with expiration and release |
| Idempotency | Safely process retries without duplicating business operations |
| Payment processing | Model payment lifecycle and state transitions |
| Asynchronous communication | Reliably communicate between services using events |
| Failure handling | Handle retries, timeouts, partial failures, and unavailable dependencies |
| Distributed workflow | Coordinate booking operations across multiple services |
| Observability | Provide tracing, metrics, and structured logging |
| Testing | Maintain unit, integration, concurrency, and eventually end-to-end coverage |

The project is intentionally being developed incrementally. Architectural capabilities such as Kafka messaging, distributed workflow coordination, transactional outbox, and observability will be introduced as the corresponding services and workflows are implemented.

---

# Architecture

BookingEventFlow follows a **database-per-service microservice architecture**.

Each service owns its own persistence boundary and exposes well-defined APIs and messaging contracts.

Services must **not directly access another service's database**.

Communication between services will happen through:

- Synchronous APIs where request/response semantics are appropriate.
- Asynchronous events for distributed workflows and state propagation.
- Kafka as the planned event backbone.

> **Important:** The architecture described below represents both the current implementation and the intended target architecture. Planned capabilities are explicitly marked as such.

---

## Current Architecture

At the current stage, only the **Event Service** is implemented.

The Event Service runs as an independent Spring Boot application and owns its PostgreSQL database.

There is currently no inter-service communication.

```text
                    ┌─────────────────┐
                    │   API / Client  │
                    └────────┬────────┘
                             │
                             ▼
                    ┌─────────────────┐
                    │  Event Service  │
                    │  Spring Boot    │
                    └────────┬────────┘
                             │
                             ▼
                    ┌─────────────────┐
                    │    Event DB     │
                    │   PostgreSQL    │
                    └─────────────────┘
```

### Currently Implemented

| Service               | Responsibility                                | Status         |
| ---------------------- | --------------------------------------------- | -------------- |
| **Event Service**      | Event management and discovery                | ✅ Implemented |
| Reservation Service    | Seat availability and temporary reservations  | 🚧 Planned     |
| Payment Service        | Payment processing and payment state          | 🚧 Planned     |
| Booking Service        | Booking lifecycle and confirmation            | 🚧 Planned     |
| Ticket Service         | Ticket generation                             | 🚧 Planned     |
| Notification Service   | Customer notifications                        | 🚧 Planned     |

---

## Target Architecture

The target architecture is a **database-per-service, event-driven microservice architecture**.

Kafka will provide asynchronous communication between services, while each service remains responsible for its own business state and persistence.

```text
                         ┌──────────────────────┐
                         │      API / Client     │
                         └──────────┬───────────┘
                                    │
                                    ▼
                         ┌──────────────────────┐
                         │    Event Service     │
                         │ Management / Discovery│
                         └──────────┬───────────┘
                                    │
                                    │ API
                                    ▼
                         ┌──────────────────────┐
                         │ Reservation Service  │
                         │ Availability / Holds │
                         │ Reservation TTL      │
                         └──────────┬───────────┘
                                    │
                                    │ Events
                                    ▼
                    ┌────────────────────────────────┐
                    │              Kafka              │
                    │                                │
                    │ reservation.created            │
                    │ reservation.expired            │
                    │ payment.completed              │
                    │ payment.failed                 │
                    │ booking.confirmed               │
                    │ booking.cancelled               │
                    │ ticket.generated                │
                    └───────┬──────────┬─────────────┘
                            │          │
              ┌─────────────┘          └─────────────┐
              ▼                                      ▼
      ┌─────────────────┐                    ┌─────────────────┐
      │ Payment Service │                    │ Booking Service │
      │                 │                    │                 │
      │ Payment State   │                    │ Booking State   │
      └────────┬────────┘                    └────────┬────────┘
               │                                      │
               ▼                                      ▼
      ┌─────────────────┐                    ┌─────────────────┐
      │   Payment DB    │                    │   Booking DB    │
      └─────────────────┘                    └─────────────────┘

                            ┌─────────────────┐
                            │ Ticket Service  │
                            │                 │
                            │ Ticket Creation │
                            └────────┬────────┘
                                     │
                                     ▼
                            ┌─────────────────┐
                            │    Ticket DB    │
                            └────────┬────────┘
                                     │
                                     │ Event
                                     ▼
                         ┌──────────────────────┐
                         │ Notification Service │
                         │                      │
                         │ Email / SMS / Push   │
                         └──────────┬───────────┘
                                    │
                                    ▼
                            External Providers
```

The target architecture is deliberately separated from the current implementation. Services such as Reservation, Payment, Booking, Ticket, and Notification are not yet implemented.

---

## Booking Workflow

The core engineering challenge of BookingEventFlow is not simply building several microservices.

The main objective is to demonstrate how to maintain **business correctness across service boundaries**, especially when concurrency, retries, failures, and asynchronous communication are involved.

The intended booking workflow is:

```text
Customer
   │
   │ Reserve Seats
   ▼
Reservation Service
   │
   ├── Validate event / seat availability
   │
   ├── Acquire concurrency control
   │
   ├── Create temporary reservation
   │
   └── Publish ReservationCreated
                │
                │ Kafka
                ▼
         Payment Service
                │
        ┌───────┴────────┐
        │                │
     SUCCESS           FAILURE
        │                │
        ▼                ▼
PaymentCompleted   PaymentFailed
        │                │
        └───────┬────────┘
                │
                ▼
        Booking Service
                │
        ┌───────┴────────┐
        │                │
      CONFIRM          CANCEL
        │                │
        ▼                ▼
BookingConfirmed   ReleaseReservation
        │
        ▼
   Ticket Service
        │
        ├── Generate Ticket
        │
        └── Publish TicketGenerated
                │
                ▼
      Notification Service
                │
                ▼
        Email / SMS / Push
```

The distributed workflow will eventually be coordinated through an explicit Saga strategy.

The current design is leaning toward **orchestration**, where a dedicated workflow coordinator owns the business sequencing and reacts to service outcomes.

> Saga coordination is part of the target architecture and has not yet been implemented.

---

## Data Ownership

Each service owns its own database.

```text
Event Service        → Event DB
Reservation Service  → Reservation DB
Payment Service      → Payment DB
Booking Service      → Booking DB
Ticket Service       → Ticket DB
Notification Service → Notification DB
```

No service should directly query or modify another service's database.

For example:

```text
❌ Booking Service → Reservation DB
❌ Payment Service → Booking DB
❌ Event Service → Reservation DB

✅ Booking Service → Booking DB
✅ Reservation Service → Reservation DB
✅ Event Service → Event DB
```

Cross-service state is communicated through APIs and asynchronous events.

This keeps service ownership explicit and prevents the system from becoming a distributed monolith with shared persistence.

---

## Architectural Principles

The project is being developed around the following principles:

* **Database per service**
* **Explicit service ownership**
* **No shared database access**
* **Synchronous APIs for request/response interactions**
* **Asynchronous messaging for distributed workflows**
* **Kafka for event-driven integration**
* **Transactional consistency within service boundaries**
* **Idempotent message processing**
* **Concurrency control at the reservation boundary**
* **Saga-based distributed workflow coordination**
* **Transactional Outbox for reliable event publication**
* **Retries and timeouts for transient failures**
* **Circuit breakers for unstable dependencies**
* **Observability across service boundaries**
* **Automated testing at multiple levels**

> Kafka, Saga, Transactional Outbox, resilience patterns, and distributed observability are **target capabilities** and will be marked as implemented only after the corresponding functionality is actually delivered.

---

# Current Progress

## Event Service ✅

The Event Service is currently the first completed service in the platform.

Implemented capabilities include:

* Create event
* Retrieve event
* Update event
* Delete event
* REST API
* Request/response DTO layer
* Bean validation
* Domain model / persistence entity separation
* Persistence mapper
* Custom JPA converters where required
* Global REST exception handling
* Standardized error responses
* PostgreSQL persistence
* Flyway database migrations
* Transactional service layer
* Concurrency-focused testing
* Integration testing
* Testcontainers-based database testing

The Event Service currently represents the foundation on which the remaining booking workflow services will be built.

---

## Infrastructure

Current local infrastructure includes:

* PostgreSQL
* Docker Compose
* Environment-based configuration
* Spring profiles
* `.env` for local configuration
* `.env.example` as the committed configuration template
* Testcontainers for isolated integration tests

The local `.env` file is intentionally excluded from Git.

---

# Technology Stack

| Layer                       | Technologies                                     |
| ---------------------------- | ------------------------------------------------- |
| **Language**                | Java 21                                          |
| **Framework**               | Spring Boot 3.5.x                                |
| **Web**                     | Spring Web                                       |
| **Persistence**             | Spring Data JPA / Hibernate                      |
| **Database**                | PostgreSQL                                       |
| **Database Migration**      | Flyway                                           |
| **Build**                   | Maven                                            |
| **Testing**                 | JUnit 5                                          |
| **Mocking**                 | Mockito                                          |
| **Integration Testing**     | Spring Boot Test                                 |
| **Infrastructure Testing**  | Testcontainers                                   |
| **Containerization**        | Docker                                           |
| **Local Infrastructure**    | Docker Compose                                   |
| **Messaging**               | Kafka — planned                                  |
| **Resilience**              | Retry / Timeout / Circuit Breaker — planned      |
| **Observability**           | Metrics / Tracing / Structured Logging — planned |

---

# Project Structure

```text
BookingEventFlow/
│
├── infrastructure/
│   └── docker-compose.yml
│
├── libraries/
│   └── common-domain/
│
├── services/
│   └── event-service/
│       ├── src/
│       │   ├── main/
│       │   │   ├── java/
│       │   │   └── resources/
│       │   │       ├── application.yml
│       │   │       ├── application-dev.yml
│       │   │       └── db/
│       │   │           └── migration/
│       │   │
│       │   └── test/
│       │
│       └── pom.xml
│
├── .env.example
├── .gitignore
├── pom.xml
└── README.md
```

The project is structured as a multi-module Maven project, allowing each service to remain independently organized while sharing only explicitly approved common libraries.

---

# Getting Started

## Prerequisites

Make sure the following tools are installed:

* JDK 21+
* Maven
* Docker
* Docker Compose

Verify the installations:

```bash
java -version
mvn -version
docker --version
docker compose version
```

---

## Environment Configuration

The project uses environment variables for local configuration.

Copy the example environment file:

```bash
cp .env.example .env
```

On Windows PowerShell:

```powershell
Copy-Item .env.example .env
```

Configure the required values in `.env`.

Example:

```env
SPRING_PROFILES_ACTIVE=dev

EVENT_SERVICE_PORT=8081

EVENT_DB_NAME=event_service
EVENT_DB_USERNAME=event_service
EVENT_DB_PASSWORD=event_service
EVENT_DB_PORT=5432

EVENT_DB_URL=jdbc:postgresql://event-db:5432/event_service
```

The `.env` file is intentionally ignored by Git.


---

## Start Local Infrastructure

Start PostgreSQL using Docker Compose:

```bash
docker compose -f infrastructure/docker-compose.yml up -d
```

Verify the infrastructure:

```bash
docker compose -f infrastructure/docker-compose.yml ps
```

The Event Service database should be available before starting the application.

---

## Stop Local Infrastructure

```bash
docker compose -f infrastructure/docker-compose.yml down
```

To remove the database volume as well:

```bash
docker compose -f infrastructure/docker-compose.yml down -v
```

> Removing the volume deletes the local PostgreSQL data.

---

## Build the Project

From the repository root:

```bash
mvn clean install
```

---

## Run the Event Service

From the repository root:

```bash
cd services/event-service
mvn spring-boot:run
```

The development profile is activated through:

```env
SPRING_PROFILES_ACTIVE=dev
```

The Event Service runs on the configured port:

```text
http://localhost:8081
```

---

# Running Tests

Run the complete test suite from the repository root:

```bash
mvn clean test
```

The integration tests use **Testcontainers** where appropriate.

This means integration tests do not depend on a PostgreSQL installation running directly on the developer's machine.

Instead, Testcontainers creates an isolated PostgreSQL container for the test execution.

This provides:

* Repeatable test environments
* Isolation from developer machines
* Consistent database versions
* Disposable test databases
* Reduced dependency on local infrastructure

The intended testing strategy will eventually include:

```text
Unit Tests
    │
    ▼
Integration Tests
    │
    ▼
Concurrency Tests
    │
    ▼
Contract Tests
    │
    ▼
End-to-End Tests
```

---

# Engineering Goals

BookingEventFlow is being developed around the following engineering goals:

### Correctness Before Optimization

Business correctness is prioritized over premature performance optimization.

A booking system that is fast but allows double-booking is fundamentally incorrect.

### Explicit Domain Boundaries

Each service should have a clearly defined responsibility and ownership boundary.

### Strong Transactional Guarantees

Operations that must be atomic within a service boundary should be protected by appropriate transactional semantics.

### Safe Concurrency

The reservation boundary must remain correct under concurrent access and high contention.

### Idempotent Distributed Operations

Retries must not result in duplicate reservations, payments, bookings, tickets, or notifications.

### Failure-Aware Communication

The system must assume that network calls, services, brokers, and external providers can fail.

### Independent Service Ownership

Each service owns its data and business state.

### Automated Testing

Business-critical behavior should be covered by automated tests before moving to higher-level distributed workflows.

### Observable Systems

The eventual distributed architecture should provide sufficient metrics, logs, and tracing to understand system behavior across service boundaries.

### Production-Oriented Design

The project aims to demonstrate production-oriented engineering practices without claiming to be production-ready at the current stage.

---

# Roadmap

## Foundation

* [x] Establish project structure
* [x] Implement initial Event Service
* [x] Introduce PostgreSQL persistence
* [x] Introduce Flyway migrations
* [x] Add REST API
* [x] Add global exception handling
* [x] Add integration tests
* [x] Add concurrency-focused tests
* [x] Introduce Docker-based local infrastructure
* [x] Introduce environment-based configuration

## Reservation

* [ ] Implement Reservation Service
* [ ] Introduce seat inventory
* [ ] Implement seat concurrency control
* [ ] Prevent double booking
* [ ] Implement temporary reservations
* [ ] Implement reservation expiration
* [ ] Implement reservation release
* [ ] Implement idempotent reservation operations

## Payment

* [ ] Implement Payment Service
* [ ] Define payment state machine
* [ ] Implement payment idempotency
* [ ] Handle payment success/failure
* [ ] Handle delayed payment responses
* [ ] Handle payment retry scenarios

## Distributed Workflow

* [ ] Introduce asynchronous messaging with Kafka
* [ ] Define domain events
* [ ] Define event versioning strategy
* [ ] Implement distributed booking workflow
* [ ] Define Saga coordination strategy
* [ ] Implement Saga orchestration
* [ ] Implement compensation actions
* [ ] Handle partial workflow failures
* [ ] Handle duplicate event delivery

## Reliability

* [ ] Introduce Transactional Outbox
* [ ] Introduce retry policies
* [ ] Introduce timeout policies
* [ ] Introduce circuit breakers
* [ ] Introduce dead-letter handling
* [ ] Introduce failure recovery strategies

## Ticketing & Notifications

* [ ] Implement Ticket Service
* [ ] Implement ticket generation
* [ ] Implement Notification Service
* [ ] Add email notifications
* [ ] Add SMS/push notification abstraction
* [ ] Make notification processing idempotent

## Observability

* [ ] Introduce structured logging
* [ ] Introduce application metrics
* [ ] Introduce distributed tracing
* [ ] Introduce correlation IDs
* [ ] Introduce Kafka consumer/producer metrics
* [ ] Define service health indicators

## Testing

* [ ] Expand unit test coverage
* [ ] Expand integration test coverage
* [ ] Add concurrency stress testing
* [ ] Add contract testing
* [ ] Add Kafka integration testing
* [ ] Add distributed workflow tests
* [ ] Add end-to-end testing
* [ ] Add failure-injection testing

## Production Architecture

* [ ] Define production deployment architecture
* [ ] Containerize application services
* [ ] Define environment-specific configuration
* [ ] Define secrets management strategy
* [ ] Define database deployment strategy
* [ ] Define Kafka deployment strategy
* [ ] Define monitoring and alerting strategy
* [ ] Define CI/CD pipeline

---

# Project Status

**Early Development 🚧**

BookingEventFlow is currently an actively developed production-style event booking platform.

The **Event Service is implemented** and provides the initial foundation for the rest of the platform.

The remaining services and distributed workflow are intentionally being implemented incrementally.

The primary engineering focus is on:

* Concurrency
* Double-booking prevention
* Transactional consistency
* Idempotency
* Temporary reservations
* Failure recovery
* Asynchronous communication
* Distributed workflow coordination
* Resilience
* Observability

The project should **not be considered production-ready** at its current stage.

The objective is to evolve the platform incrementally while demonstrating the engineering decisions required to build a reliable distributed booking system.

---

# License

MIT
