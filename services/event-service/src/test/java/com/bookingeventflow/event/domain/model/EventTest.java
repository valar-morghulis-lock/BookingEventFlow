package com.bookingeventflow.event.domain.model;

import com.bookingeventflow.common.event.DomainEvent;
import com.bookingeventflow.event.domain.event.EventCancelled;
import com.bookingeventflow.event.domain.event.EventCompleted;
import com.bookingeventflow.event.domain.event.EventCreated;
import com.bookingeventflow.event.domain.event.EventPublished;
import com.bookingeventflow.event.domain.valueobject.EventDescription;
import com.bookingeventflow.event.domain.valueobject.EventName;
import com.bookingeventflow.event.exception.InvalidEventStateException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DisplayName("Event Aggregate")
class EventTest {

    private static final EventName EVENT_NAME =
            EventName.of("Java Conference");

    private static final EventDescription EVENT_DESCRIPTION =
            EventDescription.of(
                    "A conference about Java and distributed systems."
            );

    private static final Instant SCHEDULED_AT =
            Instant.parse("2026-12-01T10:00:00Z");

    // =====================================================================
    // CREATION
    // =====================================================================

    @Nested
    @DisplayName("Creation")
    class CreationTests {

        @Test
        @DisplayName("Should create event in DRAFT state")
        void shouldCreateDraftEvent() {

            Event event = createEvent();

            assertNotNull(event.id());
            assertEquals(EventStatus.DRAFT, event.status());
            assertEquals(EVENT_NAME, event.name());
            assertEquals(EVENT_DESCRIPTION, event.description());
            assertEquals(SCHEDULED_AT, event.scheduledAt());
        }

        @Test
        @DisplayName("Should register EventCreated domain event")
        void shouldRegisterEventCreated() {

            Event event = createEvent();

            assertEquals(1, event.domainEvents().size());

            EventCreated created =
                    assertInstanceOf(
                            EventCreated.class,
                            event.domainEvents().get(0)
                    );

            assertDomainEventMetadata(
                    created.eventId(),
                    created.aggregateId(),
                    created.occurredAt(),
                    event.id()
            );
        }
    }

    // =====================================================================
    // PUBLISHING
    // =====================================================================

    @Nested
    @DisplayName("Publishing")
    class PublishingTests {

        @Test
        @DisplayName("Should publish draft event")
        void shouldPublishDraftEvent() {

            Event event = createEvent();

            event.clearDomainEvents();

            event.publish();

            assertEquals(
                    EventStatus.PUBLISHED,
                    event.status()
            );
        }

        @Test
        @DisplayName("Should register EventPublished domain event")
        void shouldRegisterEventPublished() {

            Event event = createEvent();

            event.clearDomainEvents();

            event.publish();

            assertEquals(
                    1,
                    event.domainEvents().size()
            );

            EventPublished published =
                    assertInstanceOf(
                            EventPublished.class,
                            event.domainEvents().get(0)
                    );

            assertDomainEventMetadata(
                    published.eventId(),
                    published.aggregateId(),
                    published.occurredAt(),
                    event.id()
            );
        }

        @Test
        @DisplayName("Should reject publishing an already published event")
        void shouldRejectPublishingAlreadyPublishedEvent() {

            Event event = createEvent();

            event.publish();
            event.clearDomainEvents();

            assertThrows(
                    InvalidEventStateException.class,
                    event::publish
            );

            assertTrue(
                    event.domainEvents().isEmpty()
            );

            assertEquals(
                    EventStatus.PUBLISHED,
                    event.status()
            );
        }
    }

    // =====================================================================
    // CANCELLATION
    // =====================================================================

    @Nested
    @DisplayName("Cancellation")
    class CancellationTests {

        @Test
        @DisplayName("Should cancel draft event")
        void shouldCancelDraftEvent() {

            Event event = createEvent();

            event.clearDomainEvents();

            event.cancel();

            assertEquals(
                    EventStatus.CANCELLED,
                    event.status()
            );
        }

        @Test
        @DisplayName("Should cancel published event")
        void shouldCancelPublishedEvent() {

            Event event = createEvent();

            event.publish();
            event.clearDomainEvents();

            event.cancel();

            assertEquals(
                    EventStatus.CANCELLED,
                    event.status()
            );
        }

        @Test
        @DisplayName("Should register EventCancelled domain event")
        void shouldRegisterEventCancelled() {

            Event event = createEvent();

            event.clearDomainEvents();

            event.cancel();

            assertEquals(
                    1,
                    event.domainEvents().size()
            );

            EventCancelled cancelled =
                    assertInstanceOf(
                            EventCancelled.class,
                            event.domainEvents().get(0)
                    );

            assertDomainEventMetadata(
                    cancelled.eventId(),
                    cancelled.aggregateId(),
                    cancelled.occurredAt(),
                    event.id()
            );
        }

        @Test
        @DisplayName("Should reject cancelling completed event")
        void shouldRejectCancellingCompletedEvent() {

            Event event = createEvent();

            event.publish();
            event.complete();

            event.clearDomainEvents();

            assertThrows(
                    InvalidEventStateException.class,
                    event::cancel
            );

            assertTrue(
                    event.domainEvents().isEmpty()
            );

            assertEquals(
                    EventStatus.COMPLETED,
                    event.status()
            );
        }

        @Test
        @DisplayName("Should reject cancelling already cancelled event")
        void shouldRejectCancellingAlreadyCancelledEvent() {

            Event event = createEvent();

            event.cancel();
            event.clearDomainEvents();

            assertThrows(
                    InvalidEventStateException.class,
                    event::cancel
            );

            assertTrue(
                    event.domainEvents().isEmpty()
            );

            assertEquals(
                    EventStatus.CANCELLED,
                    event.status()
            );
        }
    }

    // =====================================================================
    // COMPLETION
    // =====================================================================

    @Nested
    @DisplayName("Completion")
    class CompletionTests {

        @Test
        @DisplayName("Should complete published event")
        void shouldCompletePublishedEvent() {

            Event event = createEvent();

            event.publish();
            event.clearDomainEvents();

            event.complete();

            assertEquals(
                    EventStatus.COMPLETED,
                    event.status()
            );
        }

        @Test
        @DisplayName("Should register EventCompleted domain event")
        void shouldRegisterEventCompleted() {

            Event event = createEvent();

            event.publish();
            event.clearDomainEvents();

            event.complete();

            assertEquals(
                    1,
                    event.domainEvents().size()
            );

            EventCompleted completed =
                    assertInstanceOf(
                            EventCompleted.class,
                            event.domainEvents().get(0)
                    );

            assertDomainEventMetadata(
                    completed.eventId(),
                    completed.aggregateId(),
                    completed.occurredAt(),
                    event.id()
            );
        }

        @Test
        @DisplayName("Should reject completing draft event")
        void shouldRejectCompletingDraftEvent() {

            Event event = createEvent();

            event.clearDomainEvents();

            assertThrows(
                    InvalidEventStateException.class,
                    event::complete
            );

            assertTrue(
                    event.domainEvents().isEmpty()
            );

            assertEquals(
                    EventStatus.DRAFT,
                    event.status()
            );
        }

        @Test
        @DisplayName("Should reject completing cancelled event")
        void shouldRejectCompletingCancelledEvent() {

            Event event = createEvent();

            event.cancel();
            event.clearDomainEvents();

            assertThrows(
                    InvalidEventStateException.class,
                    event::complete
            );

            assertTrue(
                    event.domainEvents().isEmpty()
            );

            assertEquals(
                    EventStatus.CANCELLED,
                    event.status()
            );
        }

        @Test
        @DisplayName("Should reject completing already completed event")
        void shouldRejectCompletingAlreadyCompletedEvent() {

            Event event = createEvent();

            event.publish();
            event.complete();

            event.clearDomainEvents();

            assertThrows(
                    InvalidEventStateException.class,
                    event::complete
            );

            assertTrue(
                    event.domainEvents().isEmpty()
            );

            assertEquals(
                    EventStatus.COMPLETED,
                    event.status()
            );
        }
    }

    // =====================================================================
    // UPDATE
    // =====================================================================

    @Nested
    @DisplayName("Update Details")
    class UpdateTests {

        @Test
        @DisplayName("Should update draft event")
        void shouldUpdateDraftEvent() {

            Event event = createEvent();

            EventName updatedName =
                    EventName.of("Updated Conference");

            EventDescription updatedDescription =
                    EventDescription.of(
                            "Updated conference description."
                    );

            Instant updatedScheduledAt =
                    Instant.parse("2026-12-02T10:00:00Z");

            event.updateDetails(
                    updatedName,
                    updatedDescription,
                    updatedScheduledAt
            );

            assertEquals(
                    updatedName,
                    event.name()
            );

            assertEquals(
                    updatedDescription,
                    event.description()
            );

            assertEquals(
                    updatedScheduledAt,
                    event.scheduledAt()
            );

            assertEquals(
                    EventStatus.DRAFT,
                    event.status()
            );
        }

        @Test
        @DisplayName("Should update published event")
        void shouldUpdatePublishedEvent() {

            Event event = createEvent();

            event.publish();

            EventName updatedName =
                    EventName.of("Updated Conference");

            EventDescription updatedDescription =
                    EventDescription.of(
                            "Updated description."
                    );

            Instant updatedScheduledAt =
                    Instant.parse("2026-12-02T10:00:00Z");

            event.updateDetails(
                    updatedName,
                    updatedDescription,
                    updatedScheduledAt
            );

            assertEquals(
                    updatedName,
                    event.name()
            );

            assertEquals(
                    updatedDescription,
                    event.description()
            );

            assertEquals(
                    updatedScheduledAt,
                    event.scheduledAt()
            );

            assertEquals(
                    EventStatus.PUBLISHED,
                    event.status()
            );
        }

        @Test
        @DisplayName("Should reject updating cancelled event")
        void shouldRejectUpdatingCancelledEvent() {

            Event event = createEvent();

            event.cancel();

            assertThrows(
                    InvalidEventStateException.class,
                    () -> event.updateDetails(
                            EventName.of("Updated"),
                            EVENT_DESCRIPTION,
                            SCHEDULED_AT
                    )
            );
        }

        @Test
        @DisplayName("Should reject updating completed event")
        void shouldRejectUpdatingCompletedEvent() {

            Event event = createEvent();

            event.publish();
            event.complete();

            assertThrows(
                    InvalidEventStateException.class,
                    () -> event.updateDetails(
                            EventName.of("Updated"),
                            EVENT_DESCRIPTION,
                            SCHEDULED_AT
                    )
            );
        }
    }

    // =====================================================================
    // DOMAIN EVENTS
    // =====================================================================

    @Nested
    @DisplayName("Domain Events")
    class DomainEventTests {

        @Test
        @DisplayName("Should clear domain events")
        void shouldClearDomainEvents() {

            Event event = createEvent();

            assertFalse(
                    event.domainEvents().isEmpty()
            );

            event.clearDomainEvents();

            assertTrue(
                    event.domainEvents().isEmpty()
            );
        }

        @Test
        @DisplayName("Should expose domain events as read-only collection")
        void shouldExposeDomainEventsAsReadOnlyCollection() {

            Event event = createEvent();

            assertThrows(
                    UnsupportedOperationException.class,
                    () -> event.domainEvents().clear()
            );
        }

        @Test
        @DisplayName("Should assign unique IDs to domain events")
        void shouldAssignUniqueIdsToDomainEvents() {

            Event event = createEvent();

            EventCreated created =
                    assertInstanceOf(
                            EventCreated.class,
                            event.domainEvents().get(0)
                    );

            event.publish();

            EventPublished published =
                    assertInstanceOf(
                            EventPublished.class,
                            event.domainEvents().get(1)
                    );

            assertNotNull(created.eventId());
            assertNotNull(published.eventId());

            assertNotEquals(
                    created.eventId(),
                    published.eventId()
            );
        }

        @Test
        @DisplayName("Should associate all domain events with the aggregate")
        void shouldAssociateAllDomainEventsWithAggregate() {

            Event event = createEvent();

            event.publish();
            event.cancel();

            List<DomainEvent> events =
                    event.domainEvents();

            assertEquals(
                    3,
                    events.size()
            );

            assertEquals(
                    event.id(),
                    ((EventCreated) events.get(0)).aggregateId()
            );

            assertEquals(
                    event.id(),
                    ((EventPublished) events.get(1)).aggregateId()
            );

            assertEquals(
                    event.id(),
                    ((EventCancelled) events.get(2)).aggregateId()
            );
        }
    }

    // =====================================================================
    // FIXTURES
    // =====================================================================

    private Event createEvent() {

        return Event.create(
                EVENT_NAME,
                EVENT_DESCRIPTION,
                SCHEDULED_AT
        );
    }

    // =====================================================================
    // ASSERTIONS
    // =====================================================================

    /**
     * Verifies the common contract of the custom domain events.
     *
     * eventId -> unique identity of this domain event
     * aggregateId -> identity of the aggregate that produced the event
     * occurredAt -> timestamp when the event occurred
     */
    private void assertDomainEventMetadata(
            UUID eventId,
            UUID aggregateId,
            Instant occurredAt,
            UUID expectedAggregateId
    ) {

        assertNotNull(eventId);
        assertEquals(expectedAggregateId, aggregateId);
        assertNotNull(occurredAt);
    }
}