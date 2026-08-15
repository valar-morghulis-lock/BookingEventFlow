package com.bookingeventflow.event.service;

import com.bookingeventflow.common.pagination.CursorCodec;
import com.bookingeventflow.event.domain.model.Event;
import com.bookingeventflow.event.domain.model.EventStatus;
import com.bookingeventflow.event.domain.valueobject.EventDescription;
import com.bookingeventflow.event.domain.valueobject.EventName;
import com.bookingeventflow.event.entity.EventEntity;
import com.bookingeventflow.event.exception.EventNotFoundException;
import com.bookingeventflow.event.mapper.EventMapper;
import com.bookingeventflow.event.pagination.EventCursor;
import com.bookingeventflow.event.presentation.request.CreateEventRequest;
import com.bookingeventflow.event.presentation.request.UpdateEventRequest;
import com.bookingeventflow.event.presentation.response.EventPageResponse;
import com.bookingeventflow.event.presentation.response.EventResponse;
import com.bookingeventflow.event.repository.EventRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@Transactional
public class EventService {

    private static final Logger log =
            LoggerFactory.getLogger(EventService.class);

    private static final int MAX_PAGE_SIZE = 100;

    private final EventRepository eventRepository;
    private final EventMapper eventMapper;
    private final CursorCodec<EventCursor> cursorCodec;

    public EventService(
            EventRepository eventRepository,
            EventMapper eventMapper,
            CursorCodec<EventCursor> cursorCodec
    ) {
        this.eventRepository = eventRepository;
        this.eventMapper = eventMapper;
        this.cursorCodec = cursorCodec;
    }

    // =====================================================================
    // CREATE
    // =====================================================================

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

    // =====================================================================
    // GET BY ID
    // =====================================================================

    /**
     * Retrieves an event by its identifier.
     */
    @Transactional(readOnly = true)
    public EventResponse getById(UUID id) {

        return eventMapper.toResponse(
                findById(id)
        );
    }

    // =====================================================================
    // GET ALL
    // =====================================================================

    /**
     * Retrieves events using keyset pagination.
     *
     * <p>Events are ordered by scheduled time and event ID:</p>
     *
     * <pre>
     * ORDER BY scheduledAt ASC, id ASC
     * </pre>
     *
     * <p>The event ID acts as the deterministic tie-breaker when
     * multiple events have the same scheduled time.</p>
     *
     * <p>An optional status filter can be applied. When a status is
     * provided, the filter is applied consistently to both the first
     * page and subsequent cursor-based pages.</p>
     *
     * <p>The repository retrieves {@code limit + 1} records so that
     * the service can determine whether another page exists without
     * executing an additional COUNT query.</p>
     *
     * @param status optional event status filter
     * @param limit maximum number of events returned
     * @param after opaque cursor representing the last event of
     *              the previous page; may be {@code null}
     * @return paginated event response
     */
    @Transactional(readOnly = true)
    public EventPageResponse getAll(
            EventStatus status,
            int limit,
            String after
    ) {

        validatePageSize(limit);

        Pageable pageable =
                PageRequest.of(
                        0,
                        limit + 1
                );

        List<EventEntity> entities =
                loadPage(
                        status,
                        limit,
                        after,
                        pageable
                );

        boolean hasNext =
                entities.size() > limit;

        List<EventEntity> pageEntities =
                hasNext
                        ? entities.subList(0, limit)
                        : entities;

        List<EventResponse> items =
                pageEntities.stream()
                        .map(eventMapper::toResponse)
                        .toList();

        String nextCursor =
                hasNext
                        ? createNextCursor(pageEntities)
                        : null;

        log.debug(
                "Retrieved {} events with status={}, limit={}, after={}, hasNext={}",
                items.size(),
                status,
                limit,
                after,
                hasNext
        );

        return new EventPageResponse(
                items,
                nextCursor
        );
    }

    /**
     * Loads the appropriate keyset page.
     */
    private List<EventEntity> loadPage(
            EventStatus status,
            int limit,
            String after,
            Pageable pageable
    ) {

        if (after == null || after.isBlank()) {

            if (status == null) {
                return eventRepository.findFirstKeysetPage(
                        pageable
                );
            }

            return eventRepository.findFirstKeysetPageByStatus(
                    status,
                    pageable
            );
        }

        EventCursor cursor =
                cursorCodec.decode(after);

        if (status == null) {
            return eventRepository.findNextKeysetPage(
                    cursor.scheduledAt(),
                    cursor.eventId(),
                    pageable
            );
        }

        return eventRepository.findNextKeysetPageByStatus(
                status,
                cursor.scheduledAt(),
                cursor.eventId(),
                pageable
        );
    }

    /**
     * Creates a cursor from the last entity in the current page.
     */
    private String createNextCursor(
            List<EventEntity> pageEntities
    ) {

        EventEntity last =
                pageEntities.get(
                        pageEntities.size() - 1
                );

        EventCursor cursor =
                new EventCursor(
                        last.getScheduledAt(),
                        last.id()
                );

        return cursorCodec.encode(cursor);
    }

    /**
     * Validates the requested page size.
     */
    private void validatePageSize(int limit) {

        if (limit < 1 || limit > MAX_PAGE_SIZE) {

            throw new IllegalArgumentException(
                    "Page size must be between 1 and "
                            + MAX_PAGE_SIZE
            );
        }
    }

    // =====================================================================
    // UPDATE
    // =====================================================================

    /**
     * Updates event details.
     */
    public EventResponse update(
            UUID id,
            UpdateEventRequest request
    ) {

        EventEntity entity =
                findById(id);

        Long previousVersion =
                entity.version();

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

    // =====================================================================
    // PUBLISH
    // =====================================================================

    /**
     * Publishes an event.
     */
    public EventResponse publish(UUID id) {

        EventEntity entity =
                findById(id);

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

    // =====================================================================
    // CANCEL
    // =====================================================================

    /**
     * Cancels an event.
     */
    public EventResponse cancel(UUID id) {

        EventEntity entity =
                findById(id);

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

    // =====================================================================
    // COMPLETE
    // =====================================================================

    /**
     * Completes an event.
     */
    public EventResponse complete(UUID id) {

        EventEntity entity =
                findById(id);

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

    // =====================================================================
    // DELETE
    // =====================================================================

    /**
     * Deletes an event.
     */
    public void delete(UUID id) {

        EventEntity entity =
                findById(id);

        eventRepository.delete(entity);
    }

    // =====================================================================
    // INTERNAL
    // =====================================================================

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