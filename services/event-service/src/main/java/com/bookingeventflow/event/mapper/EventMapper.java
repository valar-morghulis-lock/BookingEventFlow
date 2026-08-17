package com.bookingeventflow.event.mapper;

import com.bookingeventflow.event.domain.model.Event;
import com.bookingeventflow.event.domain.valueobject.EventDescription;
import com.bookingeventflow.event.domain.valueobject.EventName;
import com.bookingeventflow.event.entity.EventEntity;
import com.bookingeventflow.event.presentation.response.EventResponse;
import org.mapstruct.Mapper;
import org.mapstruct.MappingConstants;
import org.mapstruct.MappingTarget;

@Mapper(
        componentModel = MappingConstants.ComponentModel.SPRING
)
public interface EventMapper {

    /**
     * Maps a newly created domain event to a new persistence entity.
     *
     * The entity owns persistence concerns such as:
     * - ID generation
     * - optimistic locking version
     * - auditing fields
     */
    default EventEntity toNewEntity(Event event) {

        if (event == null) {
            return null;
        }

        return new EventEntity(
                event.name().value(),
                descriptionValue(event),
                event.scheduledAt(),
                event.numberOfRows(),
                event.status()
        );
    }

    /**
     * Updates the persistence representation of an existing event.
     *
     * Identity, version and auditing fields are intentionally not
     * modified here.
     *
     * The domain aggregate is responsible for deciding whether
     * these changes are allowed.
     */
    default void updateEntity(
            Event event,
            @MappingTarget EventEntity entity
    ) {

        if (event == null || entity == null) {
            return;
        }

        entity.setName(
                event.name().value()
        );

        entity.setDescription(
                descriptionValue(event)
        );

        entity.setScheduledAt(
                event.scheduledAt()
        );

        entity.setNumberOfRows(
                event.numberOfRows()
        );

        entity.setStatus(
                event.status()
        );
    }

    /**
     * Reconstitutes the domain aggregate from persistence state.
     *
     * No domain events are generated during reconstitution.
     */
    default Event toDomain(EventEntity entity) {

        if (entity == null) {
            return null;
        }

        return Event.reconstitute(
                entity.id(),
                entity.version(),
                EventName.of(entity.getName()),
                description(entity),
                entity.getScheduledAt(),
                entity.getNumberOfRows(),
                entity.getStatus()
        );
    }

    /**
     * Maps persistence state to the API representation.
     */
    default EventResponse toResponse(EventEntity entity) {

        if (entity == null) {
            return null;
        }

        return new EventResponse(
                entity.id(),
                entity.version(),
                entity.getName(),
                entity.getDescription(),
                entity.getScheduledAt(),
                entity.getNumberOfRows(),
                entity.getStatus()
        );
    }

    private static String descriptionValue(Event event) {

        EventDescription description =
                event.description();

        return description != null
                ? description.value()
                : null;
    }

    private static EventDescription description(
            EventEntity entity
    ) {

        String description =
                entity.getDescription();

        return description != null
                ? EventDescription.of(description)
                : null;
    }
}