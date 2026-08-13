package com.bookingeventflow.event.service;

import com.bookingeventflow.event.domain.model.Event;
import com.bookingeventflow.event.domain.valueobject.EventDescription;
import com.bookingeventflow.event.domain.valueobject.EventName;
import com.bookingeventflow.event.entity.EventEntity;
import com.bookingeventflow.event.exception.EventNotFoundException;
import com.bookingeventflow.event.mapper.EventMapper;
import com.bookingeventflow.event.presentation.request.CreateEventRequest;
import com.bookingeventflow.event.presentation.request.UpdateEventRequest;
import com.bookingeventflow.event.presentation.response.EventResponse;
import com.bookingeventflow.event.repository.EventRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@Transactional
public class EventService {

    private static final Logger log =
            LoggerFactory.getLogger(EventService.class);

    private final EventRepository eventRepository;
    private final EventMapper eventMapper;

    public EventService(
            EventRepository eventRepository,
            EventMapper eventMapper
    ) {
        this.eventRepository = eventRepository;
        this.eventMapper = eventMapper;
    }

    /**
     * Creates a new event.
     */
    public EventResponse create(
            CreateEventRequest request
    ) {

        Event event = Event.create(
                EventName.of(request.name()),
                EventDescription.of(request.description()),
                request.scheduledAt()
        );

        EventEntity entity =
                eventMapper.toNewEntity(event);

        EventEntity saved =
                eventRepository.saveAndFlush(entity);

        log.debug(
                "Created event {} with version {}",
                saved.id(),
                saved.version()
        );

        return eventMapper.toResponse(saved);
    }

    /**
     * Retrieves an event by its identifier.
     */
    @Transactional(readOnly = true)
    public EventResponse getById(UUID id) {

        return eventMapper.toResponse(
                findById(id)
        );
    }

    /**
     * Retrieves all events.
     */
    @Transactional(readOnly = true)
    public List<EventResponse> getAll() {

        return eventRepository.findAll()
                .stream()
                .map(eventMapper::toResponse)
                .toList();
    }

    /**
     * Updates event details.
     */
    public EventResponse update(
            UUID id,
            UpdateEventRequest request
    ) {

        EventEntity entity = findById(id);

        Long previousVersion = entity.version();

        Event event =
                eventMapper.toDomain(entity);

        event.updateDetails(
                EventName.of(request.name()),
                EventDescription.of(request.description()),
                request.scheduledAt()
        );

        eventMapper.updateEntity(
                event,
                entity
        );

        EventEntity saved =
                eventRepository.saveAndFlush(entity);

        log.debug(
                "Updated event {} from version {} to {}",
                id,
                previousVersion,
                saved.version()
        );

        return eventMapper.toResponse(saved);
    }

    /**
     * Publishes an event.
     */
    public EventResponse publish(UUID id) {

        EventEntity entity = findById(id);

        Event event =
                eventMapper.toDomain(entity);

        event.publish();

        eventMapper.updateEntity(
                event,
                entity
        );

        EventEntity saved =
                eventRepository.saveAndFlush(entity);

        log.debug(
                "Published event {} with version {}",
                id,
                saved.version()
        );

        return eventMapper.toResponse(saved);
    }

    /**
     * Cancels an event.
     */
    public EventResponse cancel(UUID id) {

        EventEntity entity = findById(id);

        Event event =
                eventMapper.toDomain(entity);

        event.cancel();

        eventMapper.updateEntity(
                event,
                entity
        );

        EventEntity saved =
                eventRepository.saveAndFlush(entity);

        log.debug(
                "Cancelled event {} with version {}",
                id,
                saved.version()
        );

        return eventMapper.toResponse(saved);
    }

    /**
     * Completes an event.
     */
    public EventResponse complete(UUID id) {

        EventEntity entity = findById(id);

        Event event =
                eventMapper.toDomain(entity);

        event.complete();

        eventMapper.updateEntity(
                event,
                entity
        );

        EventEntity saved =
                eventRepository.saveAndFlush(entity);

        log.debug(
                "Completed event {} with version {}",
                id,
                saved.version()
        );

        return eventMapper.toResponse(saved);
    }

    /**
     * Deletes an event.
     */
    public void delete(UUID id) {

        EventEntity entity = findById(id);

        eventRepository.delete(entity);
    }

    /**
     * Loads an event or throws EventNotFoundException.
     */
    private EventEntity findById(UUID id) {

        return eventRepository.findById(id)
                .orElseThrow(
                        () -> new EventNotFoundException(id)
                );
    }
}