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

    private static final int NUMBER_OF_ROWS = 50;

    private static final int UPDATED_NUMBER_OF_ROWS = 60;

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
            assertEquals(
                    EventStatus.DRAFT,
                    event.status()
            );
            assertEquals(
                    EVENT_NAME,
                    event.name()
            );
            assertEquals(
                    EVENT_DESCRIPTION,
                    event.description()
            );
            assertEquals(
                    SCHEDULED_AT,
                    event.scheduledAt()
            );
            assertEquals(
                    NUMBER_OF_ROWS,
                    event.numberOfRows()
            );
        }

        @Test
        @DisplayName("Should calculate seating capacity from number of rows")
        void shouldCalculateSeatingCapacity() {

            Event event = createEvent();

            assertEquals(
                    NUMBER_OF_ROWS * Event.SEATS_PER_ROW,
                    event.capacity()
            );
        }

        @Test
        @DisplayName("Should register EventCreated domain event")
        void shouldRegisterEventCreated() {

            Event event = createEvent();

            assertEquals(
                    1,
                    event.domainEvents().size()
            );

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

            assertEquals(
                    NUMBER_OF_ROWS,
                    created.numberOfRows()
            );

            assertEquals(
                    Event.SEATS_PER_ROW,
                    created.seatsPerRow()
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
        @DisplayName("Should update draft event details")
        void shouldUpdateDraftEventDetails() {

            Event event = createEvent();

            EventName updatedName =
                    EventName.of("Updated Conference");

            EventDescription updatedDescription =
                    EventDescription.of(
                            "Updated conference description."
                    );

            Instant updatedScheduledAt =
                    Instant.parse(
                            "2026-12-02T10:00:00Z"
                    );

            event.updateDetails(
                    updatedName,
                    updatedDescription,
                    updatedScheduledAt,
                    UPDATED_NUMBER_OF_ROWS
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
                    UPDATED_NUMBER_OF_ROWS,
                    event.numberOfRows()
            );

            assertEquals(
                    UPDATED_NUMBER_OF_ROWS * Event.SEATS_PER_ROW,
                    event.capacity()
            );

            assertEquals(
                    EventStatus.DRAFT,
                    event.status()
            );
        }

        @Test
        @DisplayName("Should preserve event status when updating draft event")
        void shouldPreserveDraftStatusWhenUpdatingEvent() {

            Event event = createEvent();

            event.updateDetails(
                    EventName.of("Updated Conference"),
                    EventDescription.of("Updated description."),
                    Instant.parse(
                            "2026-12-02T10:00:00Z"
                    ),
                    UPDATED_NUMBER_OF_ROWS
            );

            assertEquals(
                    EventStatus.DRAFT,
                    event.status()
            );
        }

        @Test
        @DisplayName("Should reject updating published event")
        void shouldRejectUpdatingPublishedEvent() {

            Event event = createEvent();

            event.publish();

            EventName originalName =
                    event.name();

            EventDescription originalDescription =
                    event.description();

            Instant originalScheduledAt =
                    event.scheduledAt();

            int originalNumberOfRows =
                    event.numberOfRows();

            assertThrows(
                    InvalidEventStateException.class,
                    () -> event.updateDetails(
                            EventName.of("Updated Conference"),
                            EventDescription.of(
                                    "Updated description."
                            ),
                            Instant.parse(
                                    "2026-12-02T10:00:00Z"
                            ),
                            UPDATED_NUMBER_OF_ROWS
                    )
            );

            assertEquals(
                    originalName,
                    event.name()
            );

            assertEquals(
                    originalDescription,
                    event.description()
            );

            assertEquals(
                    originalScheduledAt,
                    event.scheduledAt()
            );

            assertEquals(
                    originalNumberOfRows,
                    event.numberOfRows()
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
                            SCHEDULED_AT,
                            UPDATED_NUMBER_OF_ROWS
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
                            SCHEDULED_AT,
                            UPDATED_NUMBER_OF_ROWS
                    )
            );
        }

        @Test
        @DisplayName("Should reject zero number of rows")
        void shouldRejectZeroNumberOfRows() {

            Event event = createEvent();

            assertThrows(
                    IllegalArgumentException.class,
                    () -> event.updateDetails(
                            EventName.of("Updated"),
                            EVENT_DESCRIPTION,
                            SCHEDULED_AT,
                            0
                    )
            );

            assertEquals(
                    NUMBER_OF_ROWS,
                    event.numberOfRows()
            );

            assertEquals(
                    EventStatus.DRAFT,
                    event.status()
            );
        }

        @Test
        @DisplayName("Should reject negative number of rows")
        void shouldRejectNegativeNumberOfRows() {

            Event event = createEvent();

            assertThrows(
                    IllegalArgumentException.class,
                    () -> event.updateDetails(
                            EventName.of("Updated"),
                            EVENT_DESCRIPTION,
                            SCHEDULED_AT,
                            -1
                    )
            );

            assertEquals(
                    NUMBER_OF_ROWS,
                    event.numberOfRows()
            );

            assertEquals(
                    EventStatus.DRAFT,
                    event.status()
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

            assertNotNull(
                    created.eventId()
            );

            assertNotNull(
                    published.eventId()
            );

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
                    ((EventCreated) events.get(0))
                            .aggregateId()
            );

            assertEquals(
                    event.id(),
                    ((EventPublished) events.get(1))
                            .aggregateId()
            );

            assertEquals(
                    event.id(),
                    ((EventCancelled) events.get(2))
                            .aggregateId()
            );
        }
    }

    // =====================================================================
    // RECONSTITUTION
    // =====================================================================

    @Nested
    @DisplayName("Reconstitution")
    class ReconstitutionTests {

        @Test
        @DisplayName("Should reconstitute event without registering domain events")
        void shouldReconstituteEventWithoutRegisteringDomainEvents() {

            UUID eventId =
                    UUID.randomUUID();

            Long version =
                    3L;

            Event event =
                    Event.reconstitute(
                            eventId,
                            version,
                            EVENT_NAME,
                            EVENT_DESCRIPTION,
                            SCHEDULED_AT,
                            NUMBER_OF_ROWS,
                            EventStatus.PUBLISHED
                    );

            assertEquals(
                    eventId,
                    event.id()
            );

            assertEquals(
                    version,
                    event.version()
            );

            assertEquals(
                    EVENT_NAME,
                    event.name()
            );

            assertEquals(
                    EVENT_DESCRIPTION,
                    event.description()
            );

            assertEquals(
                    SCHEDULED_AT,
                    event.scheduledAt()
            );

            assertEquals(
                    NUMBER_OF_ROWS,
                    event.numberOfRows()
            );

            assertEquals(
                    EventStatus.PUBLISHED,
                    event.status()
            );

            assertTrue(
                    event.domainEvents().isEmpty()
            );
        }

        @Test
        @DisplayName("Should preserve seating capacity when reconstituting event")
        void shouldPreserveSeatingCapacityWhenReconstitutingEvent() {

            Event event =
                    Event.reconstitute(
                            UUID.randomUUID(),
                            1L,
                            EVENT_NAME,
                            EVENT_DESCRIPTION,
                            SCHEDULED_AT,
                            NUMBER_OF_ROWS,
                            EventStatus.DRAFT
                    );

            assertEquals(
                    NUMBER_OF_ROWS * Event.SEATS_PER_ROW,
                    event.capacity()
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
                SCHEDULED_AT,
                NUMBER_OF_ROWS
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

        assertNotNull(
                eventId
        );

        assertEquals(
                expectedAggregateId,
                aggregateId
        );

        assertNotNull(
                occurredAt
        );
    }
}