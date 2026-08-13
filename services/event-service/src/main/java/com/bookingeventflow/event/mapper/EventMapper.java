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
     * Creates a brand-new entity.
     *
     * ID:
     *   Not assigned here.
     *   Hibernate generates it using @GeneratedValue.
     *
     * Version:
     *   Not assigned here.
     *   Hibernate manages it using @Version.
     */
    default EventEntity toNewEntity(Event event) {

        if (event == null) {
            return null;
        }

        return new EventEntity(
                event.name().value(),
                event.description() != null
                        ? event.description().value()
                        : null,
                event.scheduledAt(),
                event.status()
        );
    }

    /**
     * Updates an existing entity.
     *
     * ID and version are deliberately untouched.
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
                event.description() != null
                        ? event.description().value()
                        : null
        );

        entity.setScheduledAt(
                event.scheduledAt()
        );

        entity.setStatus(
                event.status()
        );
    }

    /**
     * Reconstructs the domain object from a persisted entity.
     *
     * ID and version must be preserved because they are
     * part of the persisted aggregate state.
     */
    default Event toDomain(EventEntity entity) {

        if (entity == null) {
            return null;
        }

        return Event.reconstitute(
                entity.id(),
                entity.version(),
                EventName.of(entity.getName()),
                entity.getDescription() != null
                        ? EventDescription.of(
                        entity.getDescription()
                )
                        : null,
                entity.getScheduledAt(),
                entity.getStatus()
        );
    }

    /**
     * Converts the persisted entity into the API response.
     *
     * The generated ID and current optimistic-lock version
     * are exposed in the response.
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
                entity.getStatus()
        );
    }
}