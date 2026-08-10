package com.bookingeventflow.event.domain.model;

import com.bookingeventflow.common.entity.AggregateRoot;
import com.bookingeventflow.event.domain.event.EventCancelled;
import com.bookingeventflow.event.domain.event.EventCompleted;
import com.bookingeventflow.event.domain.event.EventCreated;
import com.bookingeventflow.event.domain.event.EventPublished;
import com.bookingeventflow.event.domain.valueobject.EventDescription;
import com.bookingeventflow.event.domain.valueobject.EventName;

import java.time.Instant;
import java.util.Objects;

public class Event extends AggregateRoot {

    private EventName name;
    private EventDescription description;
    private Instant scheduledAt;
    private EventStatus status;

    /**
     * Required by persistence frameworks when the domain object
     * is reconstructed from persistence.
     */
    protected Event() {
    }

    private Event(
            EventName name,
            EventDescription description,
            Instant scheduledAt
    ) {
        this.name = requireName(name);
        this.description = description;
        this.scheduledAt = requireScheduledAt(scheduledAt);
        this.status = EventStatus.DRAFT;

        registerEvent(
                new EventCreated(
                        id(),
                        Instant.now()
                )
        );
    }

    /**
     * Creates a new event in DRAFT state.
     *
     * A new EventCreated domain event is registered as part
     * of aggregate creation.
     */
    public static Event create(
            EventName name,
            EventDescription description,
            Instant scheduledAt
    ) {
        return new Event(
                name,
                description,
                scheduledAt
        );
    }

    public EventName name() {
        return name;
    }

    public EventDescription description() {
        return description;
    }

    public Instant scheduledAt() {
        return scheduledAt;
    }

    public EventStatus status() {
        return status;
    }

    /**
     * Publishes the event.
     *
     * Only DRAFT events can be published.
     */
    public void publish() {

        ensureStatus(EventStatus.DRAFT);

        status = EventStatus.PUBLISHED;

        registerEvent(
                new EventPublished(
                        id(),
                        Instant.now()
                )
        );
    }

    /**
     * Cancels the event.
     *
     * DRAFT and PUBLISHED events may be cancelled.
     */
    public void cancel() {

        if (status == EventStatus.CANCELLED) {
            throw new IllegalStateException(
                    "Event is already cancelled"
            );
        }

        if (status == EventStatus.COMPLETED) {
            throw new IllegalStateException(
                    "Completed events cannot be cancelled"
            );
        }

        status = EventStatus.CANCELLED;

        registerEvent(
                new EventCancelled(
                        id(),
                        Instant.now()
                )
        );
    }

    /**
     * Marks the event as completed.
     *
     * Only PUBLISHED events can be completed.
     */
    public void complete() {

        ensureStatus(EventStatus.PUBLISHED);

        status = EventStatus.COMPLETED;

        registerEvent(
                new EventCompleted(
                        id(),
                        Instant.now()
                )
        );
    }

    /**
     * Updates the event details.
     *
     * CANCELLED and COMPLETED events cannot be modified.
     */
    public void updateDetails(
            EventName name,
            EventDescription description,
            Instant scheduledAt
    ) {

        ensureModifiable();

        this.name = requireName(name);
        this.description = description;
        this.scheduledAt = requireScheduledAt(scheduledAt);
    }

    private void ensureModifiable() {

        if (status == EventStatus.CANCELLED) {
            throw new IllegalStateException(
                    "Cancelled events cannot be modified"
            );
        }

        if (status == EventStatus.COMPLETED) {
            throw new IllegalStateException(
                    "Completed events cannot be modified"
            );
        }
    }

    private void ensureStatus(EventStatus expectedStatus) {

        if (status != expectedStatus) {
            throw new IllegalStateException(
                    "Event must be in " + expectedStatus
                            + " state but is currently " + status
            );
        }
    }

    private static EventName requireName(EventName name) {

        return Objects.requireNonNull(
                name,
                "Event name must not be null"
        );
    }

    private static Instant requireScheduledAt(Instant scheduledAt) {

        return Objects.requireNonNull(
                scheduledAt,
                "Event scheduledAt must not be null"
        );
    }
}
