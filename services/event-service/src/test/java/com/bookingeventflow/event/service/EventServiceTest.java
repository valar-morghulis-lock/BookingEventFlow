package com.bookingeventflow.event.service;

import com.bookingeventflow.common.pagination.CursorCodec;
import com.bookingeventflow.event.domain.model.Event;
import com.bookingeventflow.event.domain.model.EventStatus;
import com.bookingeventflow.event.entity.EventEntity;
import com.bookingeventflow.event.exception.EventNotFoundException;
import com.bookingeventflow.event.mapper.EventMapper;
import com.bookingeventflow.event.observability.metrics.EventMetrics;
import com.bookingeventflow.event.pagination.EventCursor;
import com.bookingeventflow.event.presentation.request.CreateEventRequest;
import com.bookingeventflow.event.presentation.request.UpdateEventRequest;
import com.bookingeventflow.event.presentation.response.EventPageResponse;
import com.bookingeventflow.event.presentation.response.EventResponse;
import com.bookingeventflow.event.repository.EventRepository;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Pageable;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/**
 * Metrics are exercised through a real {@link SimpleMeterRegistry} rather
 * than mocked, so assertions target the actual metric identity (name +
 * tags) that ships to production.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("EventService")
class EventServiceTest {

    private static final int DEFAULT_LIMIT = 20;
    private static final int MAX_PAGE_SIZE = 100;

    private static final int DEFAULT_NUMBER_OF_ROWS = 50;

    private static final Instant SCHEDULED_AT =
            Instant.parse("2026-12-20T19:00:00Z");

    private static final Instant SECOND_SCHEDULED_AT =
            Instant.parse("2026-12-21T19:00:00Z");

    private static final Instant THIRD_SCHEDULED_AT =
            Instant.parse("2026-12-22T19:00:00Z");

    private static final String EVENT_NAME =
            "Rock Concert";

    private static final String EVENT_DESCRIPTION =
            "Live concert";

    private static final String SECOND_EVENT_NAME =
            "Jazz Festival";

    private static final String SECOND_EVENT_DESCRIPTION =
            "Live jazz";

    private static final String THIRD_EVENT_NAME =
            "Classical Night";

    private static final String THIRD_EVENT_DESCRIPTION =
            "Classical music";

    @Mock
    private EventRepository eventRepository;

    @Mock
    private EventMapper eventMapper;

    @Mock
    private CursorCodec<EventCursor> cursorCodec;

    @Mock
    private Event domainEvent;

    private SimpleMeterRegistry meterRegistry;
    private EventMetrics eventMetrics;
    private EventService eventService;

    private EventEntity eventEntity;
    private EventResponse eventResponse;

    @BeforeEach
    void setUp() {

        meterRegistry =
                new SimpleMeterRegistry();

        eventMetrics =
                new EventMetrics(meterRegistry);

        eventService =
                new EventService(
                        eventRepository,
                        eventMapper,
                        cursorCodec,
                        eventMetrics
                );

        eventEntity =
                event(
                        EVENT_NAME,
                        EVENT_DESCRIPTION,
                        SCHEDULED_AT,
                        DEFAULT_NUMBER_OF_ROWS,
                        EventStatus.DRAFT
                );

        eventResponse =
                response(eventEntity);
    }

    // =====================================================================
    // CREATE
    // =====================================================================

    @Nested
    @DisplayName("create")
    class CreateTests {

        @Test
        @DisplayName("should create and return event")
        void shouldCreateAndReturnEvent() {

            CreateEventRequest request =
                    createRequest(
                            EVENT_NAME,
                            EVENT_DESCRIPTION,
                            SCHEDULED_AT,
                            DEFAULT_NUMBER_OF_ROWS
                    );

            EventEntity newEntity =
                    event(
                            EVENT_NAME,
                            EVENT_DESCRIPTION,
                            SCHEDULED_AT,
                            DEFAULT_NUMBER_OF_ROWS,
                            EventStatus.DRAFT
                    );

            EventEntity savedEntity =
                    event(
                            EVENT_NAME,
                            EVENT_DESCRIPTION,
                            SCHEDULED_AT,
                            DEFAULT_NUMBER_OF_ROWS,
                            EventStatus.DRAFT
                    );

            EventResponse savedResponse =
                    response(savedEntity);

            when(
                    eventMapper.toNewEntity(
                            any(Event.class)
                    )
            ).thenReturn(newEntity);

            when(
                    eventRepository.saveAndFlush(
                            newEntity
                    )
            ).thenReturn(savedEntity);

            when(
                    eventMapper.toResponse(
                            savedEntity
                    )
            ).thenReturn(savedResponse);

            EventResponse result =
                    eventService.create(request);

            assertEquals(
                    savedResponse,
                    result
            );

            verify(eventMapper)
                    .toNewEntity(any(Event.class));

            verify(eventRepository)
                    .saveAndFlush(newEntity);

            verify(eventMapper)
                    .toResponse(savedEntity);

            assertOperationCount(
                    "create",
                    "success",
                    1.0
            );
        }

        @Test
        @DisplayName("should persist mapped entity")
        void shouldPersistMappedEntity() {

            CreateEventRequest request =
                    createRequest(
                            "New Event",
                            "Description",
                            SCHEDULED_AT,
                            DEFAULT_NUMBER_OF_ROWS
                    );

            EventEntity newEntity =
                    event(
                            "New Event",
                            "Description",
                            SCHEDULED_AT,
                            DEFAULT_NUMBER_OF_ROWS,
                            EventStatus.DRAFT
                    );

            when(
                    eventMapper.toNewEntity(
                            any(Event.class)
                    )
            ).thenReturn(newEntity);

            when(
                    eventRepository.saveAndFlush(
                            newEntity
                    )
            ).thenReturn(newEntity);

            when(
                    eventMapper.toResponse(
                            newEntity
                    )
            ).thenReturn(
                    response(newEntity)
            );

            eventService.create(request);

            ArgumentCaptor<EventEntity> captor =
                    ArgumentCaptor.forClass(
                            EventEntity.class
                    );

            verify(eventRepository)
                    .saveAndFlush(
                            captor.capture()
                    );

            assertEquals(
                    newEntity,
                    captor.getValue()
            );
        }
    }

    // =====================================================================
    // GET BY ID
    // =====================================================================

    @Nested
    @DisplayName("getById")
    class GetByIdTests {

        @Test
        @DisplayName("should return event when found")
        void shouldReturnEventWhenFound() {

            givenEventExists();

            when(
                    eventMapper.toResponse(
                            eventEntity
                    )
            ).thenReturn(eventResponse);

            EventResponse result =
                    eventService.getById(
                            eventEntity.id()
                    );

            assertEquals(
                    eventResponse,
                    result
            );

            verify(eventRepository)
                    .findById(eventEntity.id());

            verify(eventMapper)
                    .toResponse(eventEntity);

            assertOperationCount(
                    "get",
                    "success",
                    1.0
            );
        }

        @Test
        @DisplayName("should throw when event does not exist")
        void shouldThrowWhenEventDoesNotExist() {

            givenEventDoesNotExist();

            assertThrows(
                    EventNotFoundException.class,
                    () -> eventService.getById(
                            eventEntity.id()
                    )
            );

            verify(eventRepository)
                    .findById(eventEntity.id());

            assertOperationCount(
                    "get",
                    "not_found",
                    1.0
            );

            verifyNoInteractions(eventMapper);
        }
    }

    // =====================================================================
    // GET ALL
    // =====================================================================

    @Nested
    @DisplayName("getAll")
    class GetAllTests {

        @Test
        @DisplayName("should return first page without status or cursor")
        void shouldReturnFirstPageWithoutStatusOrCursor() {

            EventEntity secondEntity =
                    event(
                            SECOND_EVENT_NAME,
                            SECOND_EVENT_DESCRIPTION,
                            SECOND_SCHEDULED_AT,
                            DEFAULT_NUMBER_OF_ROWS,
                            EventStatus.DRAFT
                    );

            EventResponse secondResponse =
                    response(secondEntity);

            givenFirstPage(
                    eventEntity,
                    secondEntity
            );

            givenResponse(
                    eventEntity,
                    eventResponse
            );

            givenResponse(
                    secondEntity,
                    secondResponse
            );

            EventPageResponse result =
                    eventService.getAll(
                            null,
                            DEFAULT_LIMIT,
                            null
                    );

            assertEquals(
                    2,
                    result.items().size()
            );

            assertEquals(
                    eventResponse,
                    result.items().get(0)
            );

            assertEquals(
                    secondResponse,
                    result.items().get(1)
            );

            assertNull(
                    result.nextCursor()
            );

            verify(eventRepository)
                    .findFirstKeysetPage(
                            any(Pageable.class)
                    );

            verify(eventMapper)
                    .toResponse(eventEntity);

            verify(eventMapper)
                    .toResponse(secondEntity);

            assertOperationCount(
                    "list",
                    "success",
                    1.0
            );

            assertPaginationCount(
                    "cursor_used",
                    0.0
            );

            assertPaginationCount(
                    "has_next",
                    0.0
            );

            verifyNoInteractions(cursorCodec);
        }

        @Test
        @DisplayName("should return first page filtered by status")
        void shouldReturnFirstPageFilteredByStatus() {

            EventStatus status =
                    EventStatus.DRAFT;

            givenFirstPageByStatus(
                    status,
                    eventEntity
            );

            givenResponse(
                    eventEntity,
                    eventResponse
            );

            EventPageResponse result =
                    eventService.getAll(
                            status,
                            DEFAULT_LIMIT,
                            null
                    );

            assertEquals(
                    1,
                    result.items().size()
            );

            assertEquals(
                    eventResponse,
                    result.items().get(0)
            );

            assertNull(
                    result.nextCursor()
            );

            verify(eventRepository)
                    .findFirstKeysetPageByStatus(
                            eq(status),
                            any(Pageable.class)
                    );

            verify(eventMapper)
                    .toResponse(eventEntity);

            assertOperationCount(
                    "list",
                    "success",
                    1.0
            );

            verifyNoInteractions(cursorCodec);
        }

        @Test
        @DisplayName("should return empty page when no events exist")
        void shouldReturnEmptyPageWhenNoEventsExist() {

            givenFirstPage();

            EventPageResponse result =
                    eventService.getAll(
                            null,
                            DEFAULT_LIMIT,
                            null
                    );

            assertTrue(
                    result.items().isEmpty()
            );

            assertNull(
                    result.nextCursor()
            );

            verify(eventRepository)
                    .findFirstKeysetPage(
                            any(Pageable.class)
                    );

            assertOperationCount(
                    "list",
                    "success",
                    1.0
            );

            verifyNoInteractions(
                    eventMapper,
                    cursorCodec
            );
        }

        @Test
        @DisplayName("should request limit plus one record")
        void shouldFetchLimitPlusOneRecord() {

            int limit = 2;

            EventEntity secondEntity =
                    event(
                            SECOND_EVENT_NAME,
                            SECOND_EVENT_DESCRIPTION,
                            SECOND_SCHEDULED_AT,
                            DEFAULT_NUMBER_OF_ROWS,
                            EventStatus.DRAFT
                    );

            EventEntity thirdEntity =
                    event(
                            THIRD_EVENT_NAME,
                            THIRD_EVENT_DESCRIPTION,
                            THIRD_SCHEDULED_AT,
                            DEFAULT_NUMBER_OF_ROWS,
                            EventStatus.DRAFT
                    );

            String nextCursor =
                    "encoded-cursor";

            givenFirstPage(
                    eventEntity,
                    secondEntity,
                    thirdEntity
            );

            givenResponse(
                    eventEntity,
                    eventResponse
            );

            givenResponse(
                    secondEntity,
                    response(secondEntity)
            );

            when(
                    cursorCodec.encode(
                            any(EventCursor.class)
                    )
            ).thenReturn(nextCursor);

            EventPageResponse result =
                    eventService.getAll(
                            null,
                            limit,
                            null
                    );

            assertEquals(
                    2,
                    result.items().size()
            );

            assertEquals(
                    eventResponse,
                    result.items().get(0)
            );

            assertEquals(
                    nextCursor,
                    result.nextCursor()
            );

            ArgumentCaptor<Pageable> pageableCaptor =
                    ArgumentCaptor.forClass(
                            Pageable.class
                    );

            verify(eventRepository)
                    .findFirstKeysetPage(
                            pageableCaptor.capture()
                    );

            assertEquals(
                    limit + 1,
                    pageableCaptor.getValue()
                            .getPageSize()
            );

            ArgumentCaptor<EventCursor> cursorCaptor =
                    ArgumentCaptor.forClass(
                            EventCursor.class
                    );

            verify(cursorCodec)
                    .encode(
                            cursorCaptor.capture()
                    );

            EventCursor encodedCursor =
                    cursorCaptor.getValue();

            assertEquals(
                    secondEntity.getScheduledAt(),
                    encodedCursor.scheduledAt()
            );

            assertEquals(
                    secondEntity.id(),
                    encodedCursor.eventId()
            );

            assertOperationCount(
                    "list",
                    "success",
                    1.0
            );

            assertPaginationCount(
                    "has_next",
                    1.0
            );

            assertPaginationCount(
                    "cursor_used",
                    0.0
            );
        }

        @Test
        @DisplayName("should use cursor without status filter")
        void shouldUseCursorWithoutStatusFilter() {

            int limit = 2;

            String after =
                    "encoded-cursor";

            EventCursor decodedCursor =
                    new EventCursor(
                            SCHEDULED_AT,
                            eventEntity.id()
                    );

            EventEntity secondEntity =
                    event(
                            SECOND_EVENT_NAME,
                            SECOND_EVENT_DESCRIPTION,
                            SECOND_SCHEDULED_AT,
                            DEFAULT_NUMBER_OF_ROWS,
                            EventStatus.DRAFT
                    );

            when(
                    cursorCodec.decode(after)
            ).thenReturn(decodedCursor);

            when(
                    eventRepository.findNextKeysetPage(
                            eq(SCHEDULED_AT),
                            eq(eventEntity.id()),
                            any(Pageable.class)
                    )
            ).thenReturn(
                    List.of(secondEntity)
            );

            givenResponse(
                    secondEntity,
                    response(secondEntity)
            );

            EventPageResponse result =
                    eventService.getAll(
                            null,
                            limit,
                            after
                    );

            assertEquals(
                    1,
                    result.items().size()
            );

            assertNull(
                    result.nextCursor()
            );

            verify(cursorCodec)
                    .decode(after);

            verify(eventRepository)
                    .findNextKeysetPage(
                            eq(SCHEDULED_AT),
                            eq(eventEntity.id()),
                            any(Pageable.class)
                    );

            verify(eventMapper)
                    .toResponse(secondEntity);

            assertOperationCount(
                    "list",
                    "success",
                    1.0
            );

            assertPaginationCount(
                    "cursor_used",
                    1.0
            );

            verify(
                    eventRepository,
                    never()
            ).findFirstKeysetPage(any());

            verify(
                    cursorCodec,
                    never()
            ).encode(any());
        }

        @Test
        @DisplayName("should use cursor with status filter")
        void shouldUseCursorWithStatusFilter() {

            int limit = 2;

            EventStatus status =
                    EventStatus.DRAFT;

            String after =
                    "encoded-cursor";

            EventCursor decodedCursor =
                    new EventCursor(
                            SCHEDULED_AT,
                            eventEntity.id()
                    );

            EventEntity secondEntity =
                    event(
                            SECOND_EVENT_NAME,
                            SECOND_EVENT_DESCRIPTION,
                            SECOND_SCHEDULED_AT,
                            DEFAULT_NUMBER_OF_ROWS,
                            EventStatus.DRAFT
                    );

            when(
                    cursorCodec.decode(after)
            ).thenReturn(decodedCursor);

            when(
                    eventRepository.findNextKeysetPageByStatus(
                            eq(status),
                            eq(SCHEDULED_AT),
                            eq(eventEntity.id()),
                            any(Pageable.class)
                    )
            ).thenReturn(
                    List.of(secondEntity)
            );

            givenResponse(
                    secondEntity,
                    response(secondEntity)
            );

            EventPageResponse result =
                    eventService.getAll(
                            status,
                            limit,
                            after
                    );

            assertEquals(
                    1,
                    result.items().size()
            );

            assertNull(
                    result.nextCursor()
            );

            verify(cursorCodec)
                    .decode(after);

            verify(eventRepository)
                    .findNextKeysetPageByStatus(
                            eq(status),
                            eq(SCHEDULED_AT),
                            eq(eventEntity.id()),
                            any(Pageable.class)
                    );

            verify(eventMapper)
                    .toResponse(secondEntity);

            assertOperationCount(
                    "list",
                    "success",
                    1.0
            );

            assertPaginationCount(
                    "cursor_used",
                    1.0
            );

            verify(
                    eventRepository,
                    never()
            ).findFirstKeysetPageByStatus(
                    any(),
                    any()
            );
        }

        @Test
        @DisplayName("should treat blank cursor as first page")
        void shouldTreatBlankCursorAsFirstPage() {

            givenFirstPage(eventEntity);

            givenResponse(
                    eventEntity,
                    eventResponse
            );

            EventPageResponse result =
                    eventService.getAll(
                            null,
                            DEFAULT_LIMIT,
                            "   "
                    );

            assertEquals(
                    1,
                    result.items().size()
            );

            assertEquals(
                    eventResponse,
                    result.items().get(0)
            );

            assertNull(
                    result.nextCursor()
            );

            verify(eventRepository)
                    .findFirstKeysetPage(
                            any(Pageable.class)
                    );

            assertPaginationCount(
                    "cursor_used",
                    0.0
            );

            verifyNoInteractions(cursorCodec);
        }

        @Test
        @DisplayName("should reject page size below minimum")
        void shouldRejectPageSizeBelowMinimum() {

            assertThrows(
                    IllegalArgumentException.class,
                    () -> eventService.getAll(
                            null,
                            0,
                            null
                    )
            );

            verifyNoInteractions(
                    eventRepository,
                    eventMapper,
                    cursorCodec
            );

            assertTrue(
                    meterRegistry.getMeters().isEmpty()
            );
        }

        @Test
        @DisplayName("should reject page size above maximum")
        void shouldRejectPageSizeAboveMaximum() {

            assertThrows(
                    IllegalArgumentException.class,
                    () -> eventService.getAll(
                            null,
                            MAX_PAGE_SIZE + 1,
                            null
                    )
            );

            verifyNoInteractions(
                    eventRepository,
                    eventMapper,
                    cursorCodec
            );

            assertTrue(
                    meterRegistry.getMeters().isEmpty()
            );
        }

        @Test
        @DisplayName("should accept minimum page size")
        void shouldAcceptMinimumPageSize() {

            givenFirstPage();

            EventPageResponse result =
                    eventService.getAll(
                            null,
                            1,
                            null
                    );

            assertTrue(
                    result.items().isEmpty()
            );

            verify(eventRepository)
                    .findFirstKeysetPage(
                            any(Pageable.class)
                    );

            assertOperationCount(
                    "list",
                    "success",
                    1.0
            );
        }

        @Test
        @DisplayName("should accept maximum page size")
        void shouldAcceptMaximumPageSize() {

            givenFirstPage();

            EventPageResponse result =
                    eventService.getAll(
                            null,
                            MAX_PAGE_SIZE,
                            null
                    );

            assertTrue(
                    result.items().isEmpty()
            );

            verify(eventRepository)
                    .findFirstKeysetPage(
                            any(Pageable.class)
                    );

            assertOperationCount(
                    "list",
                    "success",
                    1.0
            );
        }

        @Test
        @DisplayName("should propagate cursor decoding failure")
        void shouldPropagateCursorDecodingFailure() {

            String invalidCursor =
                    "invalid-cursor";

            IllegalArgumentException decodingException =
                    new IllegalArgumentException(
                            "Invalid cursor"
                    );

            when(
                    cursorCodec.decode(
                            invalidCursor
                    )
            ).thenThrow(decodingException);

            IllegalArgumentException thrown =
                    assertThrows(
                            IllegalArgumentException.class,
                            () -> eventService.getAll(
                                    null,
                                    DEFAULT_LIMIT,
                                    invalidCursor
                            )
                    );

            assertSame(
                    decodingException,
                    thrown
            );

            verify(cursorCodec)
                    .decode(invalidCursor);

            verifyNoInteractions(
                    eventRepository,
                    eventMapper
            );

            assertTrue(
                    meterRegistry.getMeters().isEmpty()
            );
        }
    }

    // =====================================================================
    // UPDATE
    // =====================================================================

    @Nested
    @DisplayName("update")
    class UpdateTests {

        @Test
        @DisplayName("should update existing event")
        void shouldUpdateExistingEvent() {

            Instant updatedScheduledAt =
                    Instant.parse(
                            "2026-12-21T19:00:00Z"
                    );

            int updatedNumberOfRows =
                    DEFAULT_NUMBER_OF_ROWS + 10;

            UpdateEventRequest request =
                    updateRequest(
                            "Rock Concert Updated",
                            "Updated description",
                            updatedScheduledAt,
                            updatedNumberOfRows
                    );

            EventEntity savedEntity =
                    event(
                            "Rock Concert Updated",
                            "Updated description",
                            updatedScheduledAt,
                            updatedNumberOfRows,
                            EventStatus.DRAFT
                    );

            EventResponse updatedResponse =
                    response(savedEntity);

            givenEventExists();
            givenDomainEvent();

            when(
                    eventRepository.saveAndFlush(
                            eventEntity
                    )
            ).thenReturn(savedEntity);

            when(
                    eventMapper.toResponse(
                            savedEntity
                    )
            ).thenReturn(updatedResponse);

            EventResponse result =
                    eventService.update(
                            eventEntity.id(),
                            request
                    );

            assertEquals(
                    updatedResponse,
                    result
            );

            verify(eventRepository)
                    .findById(eventEntity.id());

            verify(eventMapper)
                    .toDomain(eventEntity);

            verify(domainEvent)
                    .updateDetails(
                            any(),
                            any(),
                            eq(updatedScheduledAt),
                            eq(updatedNumberOfRows)
                    );

            verify(eventMapper)
                    .updateEntity(
                            domainEvent,
                            eventEntity
                    );

            verify(eventRepository)
                    .saveAndFlush(eventEntity);

            verify(eventMapper)
                    .toResponse(savedEntity);

            assertOperationCount(
                    "update",
                    "success",
                    1.0
            );
        }

        @Test
        @DisplayName("should throw when event does not exist")
        void shouldThrowWhenEventDoesNotExist() {

            UpdateEventRequest request =
                    updateRequest(
                            "Updated",
                            "Updated description",
                            SCHEDULED_AT,
                            DEFAULT_NUMBER_OF_ROWS
                    );

            givenEventDoesNotExist();

            assertThrows(
                    EventNotFoundException.class,
                    () -> eventService.update(
                            eventEntity.id(),
                            request
                    )
            );

            verify(eventRepository)
                    .findById(eventEntity.id());

            assertOperationCount(
                    "update",
                    "not_found",
                    1.0
            );

            verifyNoInteractions(eventMapper);

            verify(
                    eventRepository,
                    never()
            ).saveAndFlush(any());
        }
    }

    // =====================================================================
    // PUBLISH
    // =====================================================================

    @Nested
    @DisplayName("publish")
    class PublishTests {

        @Test
        @DisplayName("should publish event")
        void shouldPublishEvent() {

            EventEntity savedEntity =
                    event(
                            EVENT_NAME,
                            EVENT_DESCRIPTION,
                            SCHEDULED_AT,
                            DEFAULT_NUMBER_OF_ROWS,
                            EventStatus.PUBLISHED
                    );

            EventResponse publishedResponse =
                    response(savedEntity);

            givenEventExists();
            givenDomainEvent();

            when(
                    eventRepository.saveAndFlush(
                            eventEntity
                    )
            ).thenReturn(savedEntity);

            when(
                    eventMapper.toResponse(
                            savedEntity
                    )
            ).thenReturn(publishedResponse);

            EventResponse result =
                    eventService.publish(
                            eventEntity.id()
                    );

            assertEquals(
                    publishedResponse,
                    result
            );

            verify(eventRepository)
                    .findById(eventEntity.id());

            verify(eventMapper)
                    .toDomain(eventEntity);

            verify(domainEvent)
                    .publish();

            verify(eventMapper)
                    .updateEntity(
                            domainEvent,
                            eventEntity
                    );

            verify(eventRepository)
                    .saveAndFlush(eventEntity);

            verify(eventMapper)
                    .toResponse(savedEntity);

            assertOperationCount(
                    "publish",
                    "success",
                    1.0
            );
        }

        @Test
        @DisplayName("should throw when event does not exist")
        void shouldThrowWhenEventDoesNotExist() {

            givenEventDoesNotExist();

            assertThrows(
                    EventNotFoundException.class,
                    () -> eventService.publish(
                            eventEntity.id()
                    )
            );

            verify(eventRepository)
                    .findById(eventEntity.id());

            assertOperationCount(
                    "publish",
                    "not_found",
                    1.0
            );

            verifyNoInteractions(eventMapper);

            verify(
                    eventRepository,
                    never()
            ).saveAndFlush(any());
        }

        @Test
        @DisplayName("should propagate domain exception")
        void shouldPropagateDomainException() {

            givenEventExists();
            givenDomainEvent();

            doThrow(
                    new IllegalStateException(
                            "Event must be in DRAFT state"
                    )
            ).when(domainEvent)
                    .publish();

            assertThrows(
                    IllegalStateException.class,
                    () -> eventService.publish(
                            eventEntity.id()
                    )
            );

            verify(domainEvent)
                    .publish();

            verify(
                    eventMapper,
                    never()
            ).updateEntity(any(), any());

            verify(
                    eventRepository,
                    never()
            ).saveAndFlush(any());

            assertTrue(
                    meterRegistry.getMeters().isEmpty()
            );
        }
    }

    // =====================================================================
    // CANCEL
    // =====================================================================

    @Nested
    @DisplayName("cancel")
    class CancelTests {

        @Test
        @DisplayName("should cancel event")
        void shouldCancelEvent() {

            EventEntity savedEntity =
                    event(
                            EVENT_NAME,
                            EVENT_DESCRIPTION,
                            SCHEDULED_AT,
                            DEFAULT_NUMBER_OF_ROWS,
                            EventStatus.CANCELLED
                    );

            EventResponse cancelledResponse =
                    response(savedEntity);

            givenEventExists();
            givenDomainEvent();

            when(
                    eventRepository.saveAndFlush(
                            eventEntity
                    )
            ).thenReturn(savedEntity);

            when(
                    eventMapper.toResponse(
                            savedEntity
                    )
            ).thenReturn(cancelledResponse);

            EventResponse result =
                    eventService.cancel(
                            eventEntity.id()
                    );

            assertEquals(
                    cancelledResponse,
                    result
            );

            verify(eventRepository)
                    .findById(eventEntity.id());

            verify(eventMapper)
                    .toDomain(eventEntity);

            verify(domainEvent)
                    .cancel();

            verify(eventMapper)
                    .updateEntity(
                            domainEvent,
                            eventEntity
                    );

            verify(eventRepository)
                    .saveAndFlush(eventEntity);

            verify(eventMapper)
                    .toResponse(savedEntity);

            assertOperationCount(
                    "cancel",
                    "success",
                    1.0
            );
        }

        @Test
        @DisplayName("should throw when event does not exist")
        void shouldThrowWhenEventDoesNotExist() {

            givenEventDoesNotExist();

            assertThrows(
                    EventNotFoundException.class,
                    () -> eventService.cancel(
                            eventEntity.id()
                    )
            );

            verify(eventRepository)
                    .findById(eventEntity.id());

            assertOperationCount(
                    "cancel",
                    "not_found",
                    1.0
            );

            verifyNoInteractions(eventMapper);

            verify(
                    eventRepository,
                    never()
            ).saveAndFlush(any());
        }

        @Test
        @DisplayName("should propagate domain exception")
        void shouldPropagateDomainException() {

            givenEventExists();
            givenDomainEvent();

            doThrow(
                    new IllegalStateException(
                            "Event cannot be cancelled"
                    )
            ).when(domainEvent)
                    .cancel();

            assertThrows(
                    IllegalStateException.class,
                    () -> eventService.cancel(
                            eventEntity.id()
                    )
            );

            verify(domainEvent)
                    .cancel();

            verify(
                    eventMapper,
                    never()
            ).updateEntity(any(), any());

            verify(
                    eventRepository,
                    never()
            ).saveAndFlush(any());

            assertTrue(
                    meterRegistry.getMeters().isEmpty()
            );
        }
    }

    // =====================================================================
    // COMPLETE
    // =====================================================================

    @Nested
    @DisplayName("complete")
    class CompleteTests {

        @Test
        @DisplayName("should complete event")
        void shouldCompleteEvent() {

            EventEntity savedEntity =
                    event(
                            EVENT_NAME,
                            EVENT_DESCRIPTION,
                            SCHEDULED_AT,
                            DEFAULT_NUMBER_OF_ROWS,
                            EventStatus.COMPLETED
                    );

            EventResponse completedResponse =
                    response(savedEntity);

            givenEventExists();
            givenDomainEvent();

            when(
                    eventRepository.saveAndFlush(
                            eventEntity
                    )
            ).thenReturn(savedEntity);

            when(
                    eventMapper.toResponse(
                            savedEntity
                    )
            ).thenReturn(completedResponse);

            EventResponse result =
                    eventService.complete(
                            eventEntity.id()
                    );

            assertEquals(
                    completedResponse,
                    result
            );

            verify(eventRepository)
                    .findById(eventEntity.id());

            verify(eventMapper)
                    .toDomain(eventEntity);

            verify(domainEvent)
                    .complete();

            verify(eventMapper)
                    .updateEntity(
                            domainEvent,
                            eventEntity
                    );

            verify(eventRepository)
                    .saveAndFlush(eventEntity);

            verify(eventMapper)
                    .toResponse(savedEntity);

            assertOperationCount(
                    "complete",
                    "success",
                    1.0
            );
        }

        @Test
        @DisplayName("should throw when event does not exist")
        void shouldThrowWhenEventDoesNotExist() {

            givenEventDoesNotExist();

            assertThrows(
                    EventNotFoundException.class,
                    () -> eventService.complete(
                            eventEntity.id()
                    )
            );

            verify(eventRepository)
                    .findById(eventEntity.id());

            assertOperationCount(
                    "complete",
                    "not_found",
                    1.0
            );

            verifyNoInteractions(eventMapper);

            verify(
                    eventRepository,
                    never()
            ).saveAndFlush(any());
        }

        @Test
        @DisplayName("should propagate domain exception")
        void shouldPropagateDomainException() {

            givenEventExists();
            givenDomainEvent();

            doThrow(
                    new IllegalStateException(
                            "Event cannot be completed"
                    )
            ).when(domainEvent)
                    .complete();

            assertThrows(
                    IllegalStateException.class,
                    () -> eventService.complete(
                            eventEntity.id()
                    )
            );

            verify(domainEvent)
                    .complete();

            verify(
                    eventMapper,
                    never()
            ).updateEntity(any(), any());

            verify(
                    eventRepository,
                    never()
            ).saveAndFlush(any());

            assertTrue(
                    meterRegistry.getMeters().isEmpty()
            );
        }
    }

    // =====================================================================
    // DELETE
    // =====================================================================

    @Nested
    @DisplayName("delete")
    class DeleteTests {

        @Test
        @DisplayName("should delete existing event")
        void shouldDeleteExistingEvent() {

            givenEventExists();

            eventService.delete(
                    eventEntity.id()
            );

            verify(eventRepository)
                    .findById(eventEntity.id());

            verify(eventRepository)
                    .delete(eventEntity);

            assertOperationCount(
                    "delete",
                    "success",
                    1.0
            );

            verifyNoInteractions(eventMapper);
        }

        @Test
        @DisplayName("should throw when event does not exist")
        void shouldThrowWhenEventDoesNotExist() {

            givenEventDoesNotExist();

            assertThrows(
                    EventNotFoundException.class,
                    () -> eventService.delete(
                            eventEntity.id()
                    )
            );

            verify(eventRepository)
                    .findById(eventEntity.id());

            assertOperationCount(
                    "delete",
                    "not_found",
                    1.0
            );

            verify(
                    eventRepository,
                    never()
            ).delete(any());

            verifyNoInteractions(eventMapper);
        }
    }

    // =====================================================================
    // FIXTURES
    // =====================================================================

    private EventEntity event(
            String name,
            String description,
            Instant scheduledAt,
            int numberOfRows,
            EventStatus status
    ) {
        return new EventEntity(
                name,
                description,
                scheduledAt,
                numberOfRows,
                status
        );
    }

    private EventResponse response(
            EventEntity entity
    ) {
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

    private CreateEventRequest createRequest(
            String name,
            String description,
            Instant scheduledAt,
            int numberOfRows
    ) {
        return new CreateEventRequest(
                name,
                description,
                scheduledAt,
                numberOfRows
        );
    }

    private UpdateEventRequest updateRequest(
            String name,
            String description,
            Instant scheduledAt,
            int numberOfRows
    ) {
        return new UpdateEventRequest(
                name,
                description,
                scheduledAt,
                numberOfRows
        );
    }

    // =====================================================================
    // REPOSITORY / MAPPER HELPERS
    // =====================================================================

    private void givenEventExists() {

        when(
                eventRepository.findById(
                        eventEntity.id()
                )
        ).thenReturn(
                Optional.of(eventEntity)
        );
    }

    private void givenEventDoesNotExist() {

        when(
                eventRepository.findById(
                        eventEntity.id()
                )
        ).thenReturn(
                Optional.empty()
        );
    }

    private void givenFirstPage(
            EventEntity... entities
    ) {
        when(
                eventRepository.findFirstKeysetPage(
                        any(Pageable.class)
                )
        ).thenReturn(
                List.of(entities)
        );
    }

    private void givenFirstPageByStatus(
            EventStatus status,
            EventEntity... entities
    ) {
        when(
                eventRepository.findFirstKeysetPageByStatus(
                        eq(status),
                        any(Pageable.class)
                )
        ).thenReturn(
                List.of(entities)
        );
    }

    private void givenResponse(
            EventEntity entity,
            EventResponse response
    ) {
        when(
                eventMapper.toResponse(entity)
        ).thenReturn(response);
    }

    private void givenDomainEvent() {

        when(
                eventMapper.toDomain(
                        eventEntity
                )
        ).thenReturn(domainEvent);
    }

    // =====================================================================
    // METRIC ASSERTION HELPERS
    // =====================================================================

    private void assertOperationCount(
            String operation,
            String result,
            double expectedCount
    ) {
        assertEquals(
                expectedCount,
                meterRegistry.get("events.operations")
                        .tags(
                                "operation",
                                operation,
                                "result",
                                result
                        )
                        .counter()
                        .count()
        );
    }

    private void assertPaginationCount(
            String type,
            double expectedCount
    ) {

        if (expectedCount == 0.0) {

            boolean meterExists =
                    meterRegistry
                            .find("events.pagination")
                            .tags(
                                    "type",
                                    type
                            )
                            .counter() != null;

            if (!meterExists) {
                return;
            }
        }

        assertEquals(
                expectedCount,
                meterRegistry
                        .get("events.pagination")
                        .tags(
                                "type",
                                type
                        )
                        .counter()
                        .count()
        );
    }
}