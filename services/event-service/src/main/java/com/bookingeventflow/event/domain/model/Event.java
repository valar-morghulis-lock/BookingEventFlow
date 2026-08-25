package com.bookingeventflow.event.domain.model;

import com.bookingeventflow.common.entity.AggregateRoot;
import com.bookingeventflow.event.domain.event.EventCancelled;
import com.bookingeventflow.event.domain.event.EventCompleted;
import com.bookingeventflow.event.domain.event.EventCreated;
import com.bookingeventflow.event.domain.event.EventPublished;
import com.bookingeventflow.event.domain.valueobject.EventDescription;
import com.bookingeventflow.event.domain.valueobject.EventName;
import com.bookingeventflow.event.exception.InvalidEventStateException;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

public class Event extends AggregateRoot {

    public static final int SEATS_PER_ROW = 10;

    private EventName name;
    private EventDescription description;
    private Instant scheduledAt;
    private int numberOfRows;
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
            Instant scheduledAt,
            int numberOfRows
    ) {
        this.name = requireName(name);
        this.description = description;
        this.scheduledAt = requireScheduledAt(scheduledAt);
        this.numberOfRows = requireNumberOfRows(numberOfRows);
        this.status = EventStatus.DRAFT;

        registerEvent(
                new EventCreated(
                        id(),
                        Instant.now(),
                        numberOfRows,
                        SEATS_PER_ROW
                )
        );
    }

    /**
     * Creates a new event in DRAFT state.
     */
    public static Event create(
            EventName name,
            EventDescription description,
            Instant scheduledAt,
            int numberOfRows
    ) {
        return new Event(
                name,
                description,
                scheduledAt,
                numberOfRows
        );
    }

    /**
     * Reconstitutes an event from its persisted state.
     *
     * No domain event is registered during reconstitution.
     */
    public static Event reconstitute(
            UUID id,
            Long version,
            EventName name,
            EventDescription description,
            Instant scheduledAt,
            int numberOfRows,
            EventStatus status
    ) {
        Objects.requireNonNull(
                id,
                "Event id must not be null"
        );

        Objects.requireNonNull(
                version,
                "Event version must not be null"
        );

        Objects.requireNonNull(
                name,
                "Event name must not be null"
        );

        Objects.requireNonNull(
                scheduledAt,
                "Event scheduledAt must not be null"
        );

        Objects.requireNonNull(
                status,
                "Event status must not be null"
        );

        Event event = new Event();

        event.reconstituteIdentity(
                id,
                version
        );

        event.name = name;
        event.description = description;
        event.scheduledAt = scheduledAt;
        event.numberOfRows = requireNumberOfRows(numberOfRows);
        event.status = status;

        return event;
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

    public int numberOfRows() {
        return numberOfRows;
    }

    /**
     * Returns the total seating capacity for this event.
     *
     * Each row contains exactly {@link #SEATS_PER_ROW} seats.
     */
    public int capacity() {
        return numberOfRows * SEATS_PER_ROW;
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
                        Instant.now(),
                        numberOfRows,
                        SEATS_PER_ROW
                )
        );
    }

    /**
     * Cancels the event.
     *
     * DRAFT and PUBLISHED events may be canceled.
     */
    public void cancel() {

        ensureCancellable();

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
     * Updates the event details while it is still in DRAFT state.
     *
     * The seating configuration may be changed only before publication.
     * Once published, the reservation service may already own the
     * corresponding seat inventory.
     */
    public void updateDetails(
            EventName name,
            EventDescription description,
            Instant scheduledAt,
            int numberOfRows
    ) {
        ensureModifiable();

        this.name = requireName(name);
        this.description = description;
        this.scheduledAt = requireScheduledAt(scheduledAt);
        this.numberOfRows = requireNumberOfRows(numberOfRows);
    }

    private void ensureModifiable() {

        if (status != EventStatus.DRAFT) {

            throw new InvalidEventStateException(
                    "Event in " + status +
                            " state cannot be modified"
            );
        }
    }

    private void ensureCancellable() {

        if (status != EventStatus.DRAFT &&
                status != EventStatus.PUBLISHED) {

            throw new InvalidEventStateException(
                    "Event in " + status +
                            " state cannot be cancelled"
            );
        }
    }

    private void ensureStatus(
            EventStatus expectedStatus
    ) {

        if (status != expectedStatus) {

            throw new InvalidEventStateException(
                    "Event must be in " + expectedStatus +
                            " state but is currently " + status
            );
        }
    }

    private static EventName requireName(
            EventName name
    ) {

        return Objects.requireNonNull(
                name,
                "Event name must not be null"
        );
    }

    private static Instant requireScheduledAt(
            Instant scheduledAt
    ) {

        return Objects.requireNonNull(
                scheduledAt,
                "Event scheduledAt must not be null"
        );
    }

    private static int requireNumberOfRows(
            int numberOfRows
    ) {

        if (numberOfRows <= 0) {

            throw new IllegalArgumentException(
                    "Event number of rows must be greater than zero"
            );
        }

        return numberOfRows;
    }
}