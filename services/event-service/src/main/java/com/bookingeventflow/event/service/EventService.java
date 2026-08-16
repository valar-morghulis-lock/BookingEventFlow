package com.bookingeventflow.event.service;

import com.bookingeventflow.common.pagination.CursorCodec;
import com.bookingeventflow.event.domain.model.Event;
import com.bookingeventflow.event.domain.model.EventStatus;
import com.bookingeventflow.event.domain.valueobject.EventDescription;
import com.bookingeventflow.event.domain.valueobject.EventName;
import com.bookingeventflow.event.entity.EventEntity;
import com.bookingeventflow.event.exception.EventNotFoundException;
import com.bookingeventflow.event.mapper.EventMapper;
import com.bookingeventflow.event.observability.metrics.EventMetrics;
import com.bookingeventflow.event.observability.metrics.EventMetrics.Result;
import com.bookingeventflow.event.pagination.EventCursor;
import com.bookingeventflow.event.presentation.request.CreateEventRequest;
import com.bookingeventflow.event.presentation.request.UpdateEventRequest;
import com.bookingeventflow.event.presentation.response.EventPageResponse;
import com.bookingeventflow.event.presentation.response.EventResponse;
import com.bookingeventflow.event.repository.EventRepository;
import io.micrometer.core.annotation.Timed;
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

    private static final String OP_CREATE = "create";
    private static final String OP_GET = "get";
    private static final String OP_LIST = "list";
    private static final String OP_UPDATE = "update";
    private static final String OP_PUBLISH = "publish";
    private static final String OP_CANCEL = "cancel";
    private static final String OP_COMPLETE = "complete";
    private static final String OP_DELETE = "delete";

    private final EventRepository eventRepository;
    private final EventMapper eventMapper;
    private final CursorCodec<EventCursor> cursorCodec;
    private final EventMetrics eventMetrics;

    public EventService(
            EventRepository eventRepository,
            EventMapper eventMapper,
            CursorCodec<EventCursor> cursorCodec,
            EventMetrics eventMetrics
    ) {
        this.eventRepository = eventRepository;
        this.eventMapper = eventMapper;
        this.cursorCodec = cursorCodec;
        this.eventMetrics = eventMetrics;
    }

    // =====================================================================
    // CREATE
    // =====================================================================

    @Timed(
            value = "events.create.duration",
            description = "Event creation duration"
    )
    public EventResponse create(CreateEventRequest request) {

        Event event = Event.create(
                EventName.of(request.name()),
                EventDescription.of(request.description()),
                request.scheduledAt()
        );

        EventEntity saved = eventRepository.saveAndFlush(
                eventMapper.toNewEntity(event)
        );

        eventMetrics.recordOperation(OP_CREATE, Result.SUCCESS);

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

    @Transactional(readOnly = true)
    @Timed(
            value = "events.get_by_id.duration",
            description = "Event retrieval by ID duration"
    )
    public EventResponse getById(UUID id) {

        EventResponse response =
                eventMapper.toResponse(findById(id, OP_GET));

        eventMetrics.recordOperation(OP_GET, Result.SUCCESS);

        return response;
    }

    // =====================================================================
    // GET ALL
    // =====================================================================

    @Transactional(readOnly = true)
    @Timed(
            value = "events.list.duration",
            description = "Event listing duration"
    )
    public EventPageResponse getAll(
            EventStatus status,
            int limit,
            String after
    ) {

        validatePageSize(limit);

        Pageable pageable = PageRequest.of(0, limit + 1);

        List<EventEntity> entities =
                loadPage(status, after, pageable);

        boolean hasNext = entities.size() > limit;

        List<EventEntity> pageEntities = hasNext
                ? entities.subList(0, limit)
                : entities;

        List<EventResponse> items = pageEntities.stream()
                .map(eventMapper::toResponse)
                .toList();

        String nextCursor = hasNext
                ? createNextCursor(pageEntities)
                : null;

        eventMetrics.recordOperation(OP_LIST, Result.SUCCESS);

        if (hasCursor(after)) {
            eventMetrics.recordCursorUsed();
        }

        if (hasNext) {
            eventMetrics.recordPageWithNext();
        }

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

    // =====================================================================
    // UPDATE
    // =====================================================================

    @Timed(
            value = "events.update.duration",
            description = "Event update duration"
    )
    public EventResponse update(
            UUID id,
            UpdateEventRequest request
    ) {

        EventEntity entity = findById(id, OP_UPDATE);

        Long previousVersion = entity.version();

        Event event = eventMapper.toDomain(entity);

        event.updateDetails(
                EventName.of(request.name()),
                EventDescription.of(request.description()),
                request.scheduledAt()
        );

        eventMapper.updateEntity(event, entity);

        EventEntity saved = eventRepository.saveAndFlush(entity);

        eventMetrics.recordOperation(OP_UPDATE, Result.SUCCESS);

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

    @Timed(
            value = "events.publish.duration",
            description = "Event publication duration"
    )
    public EventResponse publish(UUID id) {

        EventEntity entity = findById(id, OP_PUBLISH);

        Event event = eventMapper.toDomain(entity);

        event.publish();

        eventMapper.updateEntity(event, entity);

        EventEntity saved = eventRepository.saveAndFlush(entity);

        eventMetrics.recordOperation(OP_PUBLISH, Result.SUCCESS);

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

    @Timed(
            value = "events.cancel.duration",
            description = "Event cancellation duration"
    )
    public EventResponse cancel(UUID id) {

        EventEntity entity = findById(id, OP_CANCEL);

        Event event = eventMapper.toDomain(entity);

        event.cancel();

        eventMapper.updateEntity(event, entity);

        EventEntity saved = eventRepository.saveAndFlush(entity);

        eventMetrics.recordOperation(OP_CANCEL, Result.SUCCESS);

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

    @Timed(
            value = "events.complete.duration",
            description = "Event completion duration"
    )
    public EventResponse complete(UUID id) {

        EventEntity entity = findById(id, OP_COMPLETE);

        Event event = eventMapper.toDomain(entity);

        event.complete();

        eventMapper.updateEntity(event, entity);

        EventEntity saved = eventRepository.saveAndFlush(entity);

        eventMetrics.recordOperation(OP_COMPLETE, Result.SUCCESS);

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

    @Timed(
            value = "events.delete.duration",
            description = "Event deletion duration"
    )
    public void delete(UUID id) {

        EventEntity entity = findById(id, OP_DELETE);

        eventRepository.delete(entity);

        eventMetrics.recordOperation(OP_DELETE, Result.SUCCESS);

        log.debug("Deleted event {}", id);
    }

    // =====================================================================
    // INTERNAL
    // =====================================================================

    private List<EventEntity> loadPage(
            EventStatus status,
            String after,
            Pageable pageable
    ) {

        if (!hasCursor(after)) {
            return status == null
                    ? eventRepository.findFirstKeysetPage(pageable)
                    : eventRepository.findFirstKeysetPageByStatus(
                    status,
                    pageable
            );
        }

        EventCursor cursor = cursorCodec.decode(after);

        return status == null
                ? eventRepository.findNextKeysetPage(
                cursor.scheduledAt(),
                cursor.eventId(),
                pageable
        )
                : eventRepository.findNextKeysetPageByStatus(
                status,
                cursor.scheduledAt(),
                cursor.eventId(),
                pageable
        );
    }

    private String createNextCursor(
            List<EventEntity> pageEntities
    ) {

        EventEntity lastEntity =
                pageEntities.get(pageEntities.size() - 1);

        return cursorCodec.encode(
                new EventCursor(
                        lastEntity.getScheduledAt(),
                        lastEntity.id()
                )
        );
    }

    /**
     * Looks up an entity by id, tagging the not_found metric with the
     * operation that triggered the lookup (get/update/publish/cancel/
     * complete/delete) so failure rates are attributable per operation
     * rather than lumped into a single generic counter.
     */
    private EventEntity findById(UUID id, String operation) {

        return eventRepository.findById(id)
                .orElseThrow(() -> {
                    eventMetrics.recordOperation(operation, Result.NOT_FOUND);

                    return new EventNotFoundException(id);
                });
    }

    private boolean hasCursor(String after) {
        return after != null && !after.isBlank();
    }

    private void validatePageSize(int limit) {

        if (limit < 1 || limit > MAX_PAGE_SIZE) {
            throw new IllegalArgumentException(
                    "Page size must be between 1 and " + MAX_PAGE_SIZE
            );
        }
    }
}