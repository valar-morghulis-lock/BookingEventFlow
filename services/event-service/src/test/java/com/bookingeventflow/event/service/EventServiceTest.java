package com.bookingeventflow.event.service;

import com.bookingeventflow.common.pagination.CursorCodec;
import com.bookingeventflow.event.domain.model.Event;
import com.bookingeventflow.event.domain.model.EventStatus;
import com.bookingeventflow.event.entity.EventEntity;
import com.bookingeventflow.event.exception.EventNotFoundException;
import com.bookingeventflow.event.mapper.EventMapper;
import com.bookingeventflow.event.pagination.EventCursor;
import com.bookingeventflow.event.presentation.request.CreateEventRequest;
import com.bookingeventflow.event.presentation.request.UpdateEventRequest;
import com.bookingeventflow.event.presentation.response.EventPageResponse;
import com.bookingeventflow.event.presentation.response.EventResponse;
import com.bookingeventflow.event.repository.EventRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Pageable;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("EventService")
class EventServiceTest {

    private static final int DEFAULT_LIMIT = 20;
    private static final int MAX_PAGE_SIZE = 100;

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

    private static final String ENCODED_CURSOR =
            "encoded-cursor";

    @Mock
    private EventRepository eventRepository;

    @Mock
    private EventMapper eventMapper;

    @Mock
    private CursorCodec<EventCursor> cursorCodec;

    @Mock
    private Event domainEvent;

    @InjectMocks
    private EventService eventService;

    private EventEntity eventEntity;
    private EventResponse eventResponse;

    @BeforeEach
    void setUp() {

        eventEntity = event(
                EVENT_NAME,
                EVENT_DESCRIPTION,
                SCHEDULED_AT,
                EventStatus.DRAFT
        );

        eventResponse = response(eventEntity);
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
                            SCHEDULED_AT
                    );

            EventEntity newEntity =
                    mock(EventEntity.class);

            when(eventMapper.toNewEntity(any(Event.class)))
                    .thenReturn(newEntity);

            when(eventRepository.saveAndFlush(newEntity))
                    .thenReturn(eventEntity);

            when(eventMapper.toResponse(eventEntity))
                    .thenReturn(eventResponse);

            EventResponse result =
                    eventService.create(request);

            assertEquals(
                    eventResponse,
                    result
            );

            verify(eventMapper)
                    .toNewEntity(any(Event.class));

            verify(eventRepository)
                    .saveAndFlush(newEntity);

            verify(eventMapper)
                    .toResponse(eventEntity);
        }

        @Test
        @DisplayName("should persist mapped entity")
        void shouldPersistMappedEntity() {

            CreateEventRequest request =
                    createRequest(
                            "New Event",
                            "Description",
                            SCHEDULED_AT
                    );

            EventEntity newEntity =
                    mock(EventEntity.class);

            when(eventMapper.toNewEntity(any(Event.class)))
                    .thenReturn(newEntity);

            when(eventRepository.saveAndFlush(newEntity))
                    .thenReturn(newEntity);

            when(eventMapper.toResponse(newEntity))
                    .thenReturn(eventResponse);

            eventService.create(request);

            ArgumentCaptor<EventEntity> captor =
                    ArgumentCaptor.forClass(EventEntity.class);

            verify(eventRepository)
                    .saveAndFlush(captor.capture());

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

            givenResponse(
                    eventEntity,
                    eventResponse
            );

            EventResponse result =
                    eventService.getById(eventEntity.id());

            assertEquals(
                    eventResponse,
                    result
            );

            assertEquals(
                    eventEntity.id(),
                    result.id()
            );

            verify(eventRepository)
                    .findById(eventEntity.id());

            verify(eventMapper)
                    .toResponse(eventEntity);
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
        @DisplayName(
                "should return first page without status filter"
        )
        void shouldReturnFirstPageWithoutStatusFilter() {

            EventEntity secondEntity =
                    event(
                            SECOND_EVENT_NAME,
                            SECOND_EVENT_DESCRIPTION,
                            SECOND_SCHEDULED_AT,
                            EventStatus.PUBLISHED
                    );

            EventResponse secondResponse =
                    response(secondEntity);

            givenFirstPageWithoutStatus(
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

            assertNotNull(result);

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

            verify(
                    eventRepository,
                    never()
            ).findFirstKeysetPageByStatus(
                    any(),
                    any()
            );

            verify(
                    eventRepository,
                    never()
            ).findNextKeysetPage(
                    any(),
                    any(),
                    any()
            );

            verify(
                    eventRepository,
                    never()
            ).findNextKeysetPageByStatus(
                    any(),
                    any(),
                    any(),
                    any()
            );

            verify(eventMapper)
                    .toResponse(eventEntity);

            verify(eventMapper)
                    .toResponse(secondEntity);

            verifyNoInteractions(cursorCodec);
        }

        @Test
        @DisplayName(
                "should return first page filtered by status"
        )
        void shouldReturnFirstPageFilteredByStatus() {

            givenFirstPageByStatus(
                    EventStatus.DRAFT,
                    eventEntity
            );

            givenResponse(
                    eventEntity,
                    eventResponse
            );

            EventPageResponse result =
                    eventService.getAll(
                            EventStatus.DRAFT,
                            DEFAULT_LIMIT,
                            null
                    );

            assertNotNull(result);

            assertEquals(
                    1,
                    result.items().size()
            );

            assertEquals(
                    eventResponse,
                    result.items().get(0)
            );

            assertEquals(
                    EventStatus.DRAFT,
                    result.items().get(0).status()
            );

            assertNull(
                    result.nextCursor()
            );

            verify(eventRepository)
                    .findFirstKeysetPageByStatus(
                            eq(EventStatus.DRAFT),
                            any(Pageable.class)
                    );

            verify(
                    eventRepository,
                    never()
            ).findFirstKeysetPage(
                    any(Pageable.class)
            );

            verify(
                    eventRepository,
                    never()
            ).findNextKeysetPage(
                    any(),
                    any(),
                    any()
            );

            verify(
                    eventRepository,
                    never()
            ).findNextKeysetPageByStatus(
                    any(),
                    any(),
                    any(),
                    any()
            );

            verify(eventMapper)
                    .toResponse(eventEntity);

            verifyNoInteractions(cursorCodec);
        }

        @Test
        @DisplayName(
                "should return empty page when no events exist"
        )
        void shouldReturnEmptyPageWhenNoEventsExist() {

            givenFirstPageByStatus(
                    EventStatus.PUBLISHED
            );

            EventPageResponse result =
                    eventService.getAll(
                            EventStatus.PUBLISHED,
                            DEFAULT_LIMIT,
                            null
                    );

            assertNotNull(result);

            assertNotNull(result.items());

            assertTrue(
                    result.items().isEmpty()
            );

            assertNull(
                    result.nextCursor()
            );

            verify(eventRepository)
                    .findFirstKeysetPageByStatus(
                            eq(EventStatus.PUBLISHED),
                            any(Pageable.class)
                    );

            verifyNoInteractions(
                    eventMapper,
                    cursorCodec
            );
        }

        @Test
        @DisplayName(
                "should fetch limit plus one record without status"
        )
        void shouldFetchLimitPlusOneRecordWithoutStatus() {

            int limit = 2;

            EventEntity secondEntity =
                    event(
                            SECOND_EVENT_NAME,
                            SECOND_EVENT_DESCRIPTION,
                            SECOND_SCHEDULED_AT,
                            EventStatus.PUBLISHED
                    );

            EventEntity thirdEntity =
                    event(
                            THIRD_EVENT_NAME,
                            THIRD_EVENT_DESCRIPTION,
                            THIRD_SCHEDULED_AT,
                            EventStatus.DRAFT
                    );

            EventResponse secondResponse =
                    response(secondEntity);

            givenFirstPageWithoutStatus(
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
                    secondResponse
            );

            when(cursorCodec.encode(any(EventCursor.class)))
                    .thenReturn(ENCODED_CURSOR);

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
                    secondResponse,
                    result.items().get(1)
            );

            assertEquals(
                    ENCODED_CURSOR,
                    result.nextCursor()
            );

            ArgumentCaptor<Pageable> pageableCaptor =
                    ArgumentCaptor.forClass(Pageable.class);

            verify(eventRepository)
                    .findFirstKeysetPage(
                            pageableCaptor.capture()
                    );

            assertEquals(
                    limit + 1,
                    pageableCaptor
                            .getValue()
                            .getPageSize()
            );

            ArgumentCaptor<EventCursor> cursorCaptor =
                    ArgumentCaptor.forClass(EventCursor.class);

            verify(cursorCodec)
                    .encode(cursorCaptor.capture());

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

            assertTrue(
                    result.items()
                            .stream()
                            .noneMatch(
                                    item -> item.id()
                                            .equals(thirdEntity.id())
                            )
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
        @DisplayName(
                "should fetch limit plus one record with status"
        )
        void shouldFetchLimitPlusOneRecordWithStatus() {

            int limit = 2;

            EventEntity secondEntity =
                    event(
                            SECOND_EVENT_NAME,
                            SECOND_EVENT_DESCRIPTION,
                            SECOND_SCHEDULED_AT,
                            EventStatus.DRAFT
                    );

            EventEntity thirdEntity =
                    event(
                            THIRD_EVENT_NAME,
                            THIRD_EVENT_DESCRIPTION,
                            THIRD_SCHEDULED_AT,
                            EventStatus.DRAFT
                    );

            EventResponse secondResponse =
                    response(secondEntity);

            givenFirstPageByStatus(
                    EventStatus.DRAFT,
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
                    secondResponse
            );

            when(cursorCodec.encode(any(EventCursor.class)))
                    .thenReturn(ENCODED_CURSOR);

            EventPageResponse result =
                    eventService.getAll(
                            EventStatus.DRAFT,
                            limit,
                            null
                    );

            assertEquals(
                    2,
                    result.items().size()
            );

            assertEquals(
                    ENCODED_CURSOR,
                    result.nextCursor()
            );

            ArgumentCaptor<Pageable> pageableCaptor =
                    ArgumentCaptor.forClass(Pageable.class);

            verify(eventRepository)
                    .findFirstKeysetPageByStatus(
                            eq(EventStatus.DRAFT),
                            pageableCaptor.capture()
                    );

            assertEquals(
                    limit + 1,
                    pageableCaptor
                            .getValue()
                            .getPageSize()
            );

            ArgumentCaptor<EventCursor> cursorCaptor =
                    ArgumentCaptor.forClass(EventCursor.class);

            verify(cursorCodec)
                    .encode(cursorCaptor.capture());

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
        }

        @Test
        @DisplayName(
                "should use cursor without status filter"
        )
        void shouldUseCursorWithoutStatusFilter() {

            int limit = 2;

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
                            EventStatus.PUBLISHED
                    );

            EventResponse secondResponse =
                    response(secondEntity);

            when(cursorCodec.decode(ENCODED_CURSOR))
                    .thenReturn(decodedCursor);

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
                    secondResponse
            );

            EventPageResponse result =
                    eventService.getAll(
                            null,
                            limit,
                            ENCODED_CURSOR
                    );

            assertEquals(
                    1,
                    result.items().size()
            );

            assertEquals(
                    secondResponse,
                    result.items().get(0)
            );

            assertNull(
                    result.nextCursor()
            );

            verify(cursorCodec)
                    .decode(ENCODED_CURSOR);

            verify(eventRepository)
                    .findNextKeysetPage(
                            eq(SCHEDULED_AT),
                            eq(eventEntity.id()),
                            any(Pageable.class)
                    );

            verify(
                    eventRepository,
                    never()
            ).findFirstKeysetPage(
                    any(Pageable.class)
            );

            verify(
                    eventRepository,
                    never()
            ).findFirstKeysetPageByStatus(
                    any(),
                    any()
            );

            verify(
                    eventRepository,
                    never()
            ).findNextKeysetPageByStatus(
                    any(),
                    any(),
                    any(),
                    any()
            );

            verify(eventMapper)
                    .toResponse(secondEntity);

            verify(
                    cursorCodec,
                    never()
            ).encode(any());
        }

        @Test
        @DisplayName(
                "should use cursor and status filter"
        )
        void shouldUseCursorWithStatusFilter() {

            int limit = 2;

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
                            EventStatus.DRAFT
                    );

            EventResponse secondResponse =
                    response(secondEntity);

            when(cursorCodec.decode(ENCODED_CURSOR))
                    .thenReturn(decodedCursor);

            when(
                    eventRepository.findNextKeysetPageByStatus(
                            eq(EventStatus.DRAFT),
                            eq(SCHEDULED_AT),
                            eq(eventEntity.id()),
                            any(Pageable.class)
                    )
            ).thenReturn(
                    List.of(secondEntity)
            );

            givenResponse(
                    secondEntity,
                    secondResponse
            );

            EventPageResponse result =
                    eventService.getAll(
                            EventStatus.DRAFT,
                            limit,
                            ENCODED_CURSOR
                    );

            assertEquals(
                    1,
                    result.items().size()
            );

            assertEquals(
                    secondResponse,
                    result.items().get(0)
            );

            assertNull(
                    result.nextCursor()
            );

            verify(cursorCodec)
                    .decode(ENCODED_CURSOR);

            verify(eventRepository)
                    .findNextKeysetPageByStatus(
                            eq(EventStatus.DRAFT),
                            eq(SCHEDULED_AT),
                            eq(eventEntity.id()),
                            any(Pageable.class)
                    );

            verify(
                    eventRepository,
                    never()
            ).findNextKeysetPage(
                    any(),
                    any(),
                    any()
            );

            verify(
                    eventRepository,
                    never()
            ).findFirstKeysetPage(
                    any(Pageable.class)
            );

            verify(
                    eventRepository,
                    never()
            ).findFirstKeysetPageByStatus(
                    any(),
                    any()
            );

            verify(eventMapper)
                    .toResponse(secondEntity);

            verify(
                    cursorCodec,
                    never()
            ).encode(any());
        }

        @Test
        @DisplayName(
                "should treat blank cursor as first page without status"
        )
        void shouldTreatBlankCursorAsFirstPageWithoutStatus() {

            givenFirstPageWithoutStatus(
                    eventEntity
            );

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

            verify(
                    eventRepository,
                    never()
            ).findFirstKeysetPageByStatus(
                    any(),
                    any()
            );

            verify(
                    eventRepository,
                    never()
            ).findNextKeysetPage(
                    any(),
                    any(),
                    any()
            );

            verify(
                    eventRepository,
                    never()
            ).findNextKeysetPageByStatus(
                    any(),
                    any(),
                    any(),
                    any()
            );

            verifyNoInteractions(cursorCodec);
        }

        @Test
        @DisplayName(
                "should treat blank cursor as first page with status"
        )
        void shouldTreatBlankCursorAsFirstPageWithStatus() {

            givenFirstPageByStatus(
                    EventStatus.DRAFT,
                    eventEntity
            );

            givenResponse(
                    eventEntity,
                    eventResponse
            );

            EventPageResponse result =
                    eventService.getAll(
                            EventStatus.DRAFT,
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
                    .findFirstKeysetPageByStatus(
                            eq(EventStatus.DRAFT),
                            any(Pageable.class)
                    );

            verify(
                    eventRepository,
                    never()
            ).findFirstKeysetPage(
                    any(Pageable.class)
            );

            verify(
                    eventRepository,
                    never()
            ).findNextKeysetPage(
                    any(),
                    any(),
                    any()
            );

            verify(
                    eventRepository,
                    never()
            ).findNextKeysetPageByStatus(
                    any(),
                    any(),
                    any(),
                    any()
            );

            verifyNoInteractions(cursorCodec);
        }

        @Test
        @DisplayName("should reject page size below minimum")
        void shouldRejectPageSizeBelowMinimum() {

            assertThrows(
                    IllegalArgumentException.class,
                    () -> eventService.getAll(
                            EventStatus.DRAFT,
                            0,
                            null
                    )
            );

            verifyNoInteractions(
                    eventRepository,
                    eventMapper,
                    cursorCodec
            );
        }

        @Test
        @DisplayName("should reject page size above maximum")
        void shouldRejectPageSizeAboveMaximum() {

            assertThrows(
                    IllegalArgumentException.class,
                    () -> eventService.getAll(
                            EventStatus.DRAFT,
                            MAX_PAGE_SIZE + 1,
                            null
                    )
            );

            verifyNoInteractions(
                    eventRepository,
                    eventMapper,
                    cursorCodec
            );
        }

        @Test
        @DisplayName("should accept minimum page size")
        void shouldAcceptMinimumPageSize() {

            givenFirstPageByStatus(
                    EventStatus.DRAFT
            );

            EventPageResponse result =
                    eventService.getAll(
                            EventStatus.DRAFT,
                            1,
                            null
                    );

            assertNotNull(result);

            verify(eventRepository)
                    .findFirstKeysetPageByStatus(
                            eq(EventStatus.DRAFT),
                            any(Pageable.class)
                    );
        }

        @Test
        @DisplayName("should accept maximum page size")
        void shouldAcceptMaximumPageSize() {

            givenFirstPageByStatus(
                    EventStatus.DRAFT
            );

            EventPageResponse result =
                    eventService.getAll(
                            EventStatus.DRAFT,
                            MAX_PAGE_SIZE,
                            null
                    );

            assertNotNull(result);

            verify(eventRepository)
                    .findFirstKeysetPageByStatus(
                            eq(EventStatus.DRAFT),
                            any(Pageable.class)
                    );
        }

        @Test
        @DisplayName("should propagate cursor decoding failure")
        void shouldPropagateCursorDecodingFailure() {

            when(cursorCodec.decode(ENCODED_CURSOR))
                    .thenThrow(
                            new IllegalArgumentException(
                                    "Invalid cursor"
                            )
                    );

            assertThrows(
                    IllegalArgumentException.class,
                    () -> eventService.getAll(
                            EventStatus.DRAFT,
                            DEFAULT_LIMIT,
                            ENCODED_CURSOR
                    )
            );

            verify(cursorCodec)
                    .decode(ENCODED_CURSOR);

            verify(
                    eventRepository,
                    never()
            ).findFirstKeysetPage(
                    any(Pageable.class)
            );

            verify(
                    eventRepository,
                    never()
            ).findFirstKeysetPageByStatus(
                    any(),
                    any()
            );

            verify(
                    eventRepository,
                    never()
            ).findNextKeysetPage(
                    any(),
                    any(),
                    any()
            );

            verify(
                    eventRepository,
                    never()
            ).findNextKeysetPageByStatus(
                    any(),
                    any(),
                    any(),
                    any()
            );

            verifyNoInteractions(eventMapper);
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

            UpdateEventRequest request =
                    new UpdateEventRequest(
                            "Rock Concert Updated",
                            "Updated description",
                            updatedScheduledAt
                    );

            EventEntity savedEntity =
                    mock(EventEntity.class);

            EventResponse updatedResponse =
                    response(
                            eventEntity.id(),
                            1L,
                            "Rock Concert Updated",
                            "Updated description",
                            updatedScheduledAt,
                            EventStatus.DRAFT
                    );

            givenEventExists();

            when(eventMapper.toDomain(eventEntity))
                    .thenReturn(domainEvent);

            when(
                    eventRepository.saveAndFlush(
                            eventEntity
                    )
            ).thenReturn(savedEntity);

            when(eventMapper.toResponse(savedEntity))
                    .thenReturn(updatedResponse);

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
                            eq(updatedScheduledAt)
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
        }

        @Test
        @DisplayName("should throw when event does not exist")
        void shouldThrowWhenEventDoesNotExist() {

            UpdateEventRequest request =
                    createUpdateRequest(
                            "Updated",
                            "Updated description",
                            SCHEDULED_AT
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
                    mock(EventEntity.class);

            EventResponse publishedResponse =
                    response(
                            eventEntity.id(),
                            1L,
                            EVENT_NAME,
                            EVENT_DESCRIPTION,
                            SCHEDULED_AT,
                            EventStatus.PUBLISHED
                    );

            givenEventExists();
            givenDomainEvent();

            when(
                    eventRepository.saveAndFlush(
                            eventEntity
                    )
            ).thenReturn(savedEntity);

            when(eventMapper.toResponse(savedEntity))
                    .thenReturn(publishedResponse);

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
                    new RuntimeException(
                            "Invalid event state"
                    )
            ).when(domainEvent).publish();

            assertThrows(
                    RuntimeException.class,
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
                    mock(EventEntity.class);

            EventResponse cancelledResponse =
                    response(
                            eventEntity.id(),
                            1L,
                            EVENT_NAME,
                            EVENT_DESCRIPTION,
                            SCHEDULED_AT,
                            EventStatus.CANCELLED
                    );

            givenEventExists();
            givenDomainEvent();

            when(
                    eventRepository.saveAndFlush(
                            eventEntity
                    )
            ).thenReturn(savedEntity);

            when(eventMapper.toResponse(savedEntity))
                    .thenReturn(cancelledResponse);

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
                    new RuntimeException(
                            "Invalid event state"
                    )
            ).when(domainEvent).cancel();

            assertThrows(
                    RuntimeException.class,
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
                    mock(EventEntity.class);

            EventResponse completedResponse =
                    response(
                            eventEntity.id(),
                            2L,
                            EVENT_NAME,
                            EVENT_DESCRIPTION,
                            SCHEDULED_AT,
                            EventStatus.COMPLETED
                    );

            givenEventExists();
            givenDomainEvent();

            when(
                    eventRepository.saveAndFlush(
                            eventEntity
                    )
            ).thenReturn(savedEntity);

            when(eventMapper.toResponse(savedEntity))
                    .thenReturn(completedResponse);

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
                    new RuntimeException(
                            "Invalid event state"
                    )
            ).when(domainEvent).complete();

            assertThrows(
                    RuntimeException.class,
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

            verify(
                    eventRepository,
                    never()
            ).delete(any());

            verifyNoInteractions(eventMapper);
        }
    }

    // =====================================================================
    // FIXTURE HELPERS
    // =====================================================================

    private EventEntity event(
            String name,
            String description,
            Instant scheduledAt,
            EventStatus status
    ) {
        return new EventEntity(
                name,
                description,
                scheduledAt,
                status
        );
    }

    private EventResponse response(
            EventEntity entity
    ) {
        return response(
                entity.id(),
                entity.version(),
                entity.getName(),
                entity.getDescription(),
                entity.getScheduledAt(),
                entity.getStatus()
        );
    }

    private EventResponse response(
            UUID id,
            Long version,
            String name,
            String description,
            Instant scheduledAt,
            EventStatus status
    ) {
        return new EventResponse(
                id,
                version,
                name,
                description,
                scheduledAt,
                status
        );
    }

    private CreateEventRequest createRequest(
            String name,
            String description,
            Instant scheduledAt
    ) {
        return new CreateEventRequest(
                name,
                description,
                scheduledAt
        );
    }

    private UpdateEventRequest createUpdateRequest(
            String name,
            String description,
            Instant scheduledAt
    ) {
        return new UpdateEventRequest(
                name,
                description,
                scheduledAt
        );
    }

    // =====================================================================
    // MOCK HELPERS
    // =====================================================================

    private void givenEventExists() {

        when(eventRepository.findById(eventEntity.id()))
                .thenReturn(
                        Optional.of(eventEntity)
                );
    }

    private void givenEventDoesNotExist() {

        when(eventRepository.findById(eventEntity.id()))
                .thenReturn(
                        Optional.empty()
                );
    }

    private void givenDomainEvent() {

        when(eventMapper.toDomain(eventEntity))
                .thenReturn(domainEvent);
    }

    private void givenResponse(
            EventEntity entity,
            EventResponse response
    ) {

        when(eventMapper.toResponse(entity))
                .thenReturn(response);
    }

    private void givenFirstPageWithoutStatus(
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
}