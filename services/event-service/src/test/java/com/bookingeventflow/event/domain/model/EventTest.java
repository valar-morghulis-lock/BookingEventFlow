package com.bookingeventflow.event.domain.model;

import com.bookingeventflow.common.event.DomainEvent;
import com.bookingeventflow.event.domain.event.EventCancelled;
import com.bookingeventflow.event.domain.event.EventCompleted;
import com.bookingeventflow.event.domain.event.EventCreated;
import com.bookingeventflow.event.domain.event.EventPublished;
import com.bookingeventflow.event.domain.valueobject.EventDescription;
import com.bookingeventflow.event.domain.valueobject.EventName;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class EventTest {

    private static final EventName NAME =
            EventName.of("Java Conference");

    private static final EventDescription DESCRIPTION =
            EventDescription.of(
                    "A conference about Java and distributed systems."
            );

    private static final Instant SCHEDULED_AT =
            Instant.parse("2026-12-01T10:00:00Z");

    @Test
    void shouldCreateEventAndRegisterEventCreated() {

        Event event = Event.create(
                NAME,
                DESCRIPTION,
                SCHEDULED_AT
        );

        assertNotNull(event.id());
        assertEquals(EventStatus.DRAFT, event.status());

        List<DomainEvent> events = event.domainEvents();

        assertEquals(1, events.size());
        assertInstanceOf(EventCreated.class, events.get(0));

        EventCreated created =
                (EventCreated) events.get(0);

        assertEquals(event.id(), created.eventId());
        assertNotNull(created.occurredAt());
    }

    @Test
    void shouldPublishDraftEventAndRegisterEventPublished() {

        Event event = Event.create(
                NAME,
                DESCRIPTION,
                SCHEDULED_AT
        );

        event.clearDomainEvents();

        event.publish();

        assertEquals(
                EventStatus.PUBLISHED,
                event.status()
        );

        assertEquals(
                1,
                event.domainEvents().size()
        );

        EventPublished published =
                assertInstanceOf(
                        EventPublished.class,
                        event.domainEvents().get(0)
                );

        assertEquals(
                event.id(),
                published.eventId()
        );

        assertNotNull(
                published.occurredAt()
        );
    }

    @Test
    void shouldCancelDraftEventAndRegisterEventCancelled() {

        Event event = Event.create(
                NAME,
                DESCRIPTION,
                SCHEDULED_AT
        );

        event.clearDomainEvents();

        event.cancel();

        assertEquals(
                EventStatus.CANCELLED,
                event.status()
        );

        assertEquals(
                1,
                event.domainEvents().size()
        );

        EventCancelled cancelled =
                assertInstanceOf(
                        EventCancelled.class,
                        event.domainEvents().get(0)
                );

        assertEquals(
                event.id(),
                cancelled.eventId()
        );

        assertNotNull(
                cancelled.occurredAt()
        );
    }

    @Test
    void shouldCancelPublishedEventAndRegisterEventCancelled() {

        Event event = Event.create(
                NAME,
                DESCRIPTION,
                SCHEDULED_AT
        );

        event.publish();

        event.clearDomainEvents();

        event.cancel();

        assertEquals(
                EventStatus.CANCELLED,
                event.status()
        );

        assertEquals(
                1,
                event.domainEvents().size()
        );

        EventCancelled cancelled =
                assertInstanceOf(
                        EventCancelled.class,
                        event.domainEvents().get(0)
                );

        assertEquals(
                event.id(),
                cancelled.eventId()
        );
    }

    @Test
    void shouldCompletePublishedEventAndRegisterEventCompleted() {

        Event event = Event.create(
                NAME,
                DESCRIPTION,
                SCHEDULED_AT
        );

        event.publish();

        event.clearDomainEvents();

        event.complete();

        assertEquals(
                EventStatus.COMPLETED,
                event.status()
        );

        assertEquals(
                1,
                event.domainEvents().size()
        );

        EventCompleted completed =
                assertInstanceOf(
                        EventCompleted.class,
                        event.domainEvents().get(0)
                );

        assertEquals(
                event.id(),
                completed.eventId()
        );

        assertNotNull(
                completed.occurredAt()
        );
    }

    @Test
    void shouldNotRegisterEventWhenPublishingFails() {

        Event event = Event.create(
                NAME,
                DESCRIPTION,
                SCHEDULED_AT
        );

        event.publish();

        event.clearDomainEvents();

        assertThrows(
                IllegalStateException.class,
                event::publish
        );

        assertTrue(
                event.domainEvents().isEmpty()
        );
    }

    @Test
    void shouldNotRegisterEventWhenCompletingDraftFails() {

        Event event = Event.create(
                NAME,
                DESCRIPTION,
                SCHEDULED_AT
        );

        event.clearDomainEvents();

        assertThrows(
                IllegalStateException.class,
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
    void shouldNotRegisterEventWhenCompletingCancelledEventFails() {

        Event event = Event.create(
                NAME,
                DESCRIPTION,
                SCHEDULED_AT
        );

        event.cancel();

        event.clearDomainEvents();

        assertThrows(
                IllegalStateException.class,
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
    void shouldNotRegisterEventWhenCancellingCompletedEventFails() {

        Event event = Event.create(
                NAME,
                DESCRIPTION,
                SCHEDULED_AT
        );

        event.publish();
        event.complete();

        event.clearDomainEvents();

        assertThrows(
                IllegalStateException.class,
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
    void shouldClearDomainEvents() {

        Event event = Event.create(
                NAME,
                DESCRIPTION,
                SCHEDULED_AT
        );

        assertFalse(
                event.domainEvents().isEmpty()
        );

        event.clearDomainEvents();

        assertTrue(
                event.domainEvents().isEmpty()
        );
    }

    @Test
    void domainEventsShouldBeReadOnly() {

        Event event = Event.create(
                NAME,
                DESCRIPTION,
                SCHEDULED_AT
        );

        assertThrows(
                UnsupportedOperationException.class,
                () -> event.domainEvents().clear()
        );
    }
}
