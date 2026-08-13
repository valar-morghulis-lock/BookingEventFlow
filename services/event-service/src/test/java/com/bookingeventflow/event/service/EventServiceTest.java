package com.bookingeventflow.event.service;

import com.bookingeventflow.event.domain.model.Event;
import com.bookingeventflow.event.domain.model.EventStatus;
import com.bookingeventflow.event.entity.EventEntity;
import com.bookingeventflow.event.exception.EventNotFoundException;
import com.bookingeventflow.event.mapper.EventMapper;
import com.bookingeventflow.event.presentation.request.CreateEventRequest;
import com.bookingeventflow.event.presentation.request.UpdateEventRequest;
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

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("EventService Unit Tests")
class EventServiceTest {

    @Mock
    private EventRepository eventRepository;

    @Mock
    private EventMapper eventMapper;

    @InjectMocks
    private EventService eventService;

    private UUID eventId;
    private Instant scheduledAt;

    private EventEntity entity;
    private EventResponse response;
    private Event domainEvent;

    @BeforeEach
    void setUp() {

        eventId = UUID.fromString(
                "512c5d0f-a416-4430-9265-4069b1637964"
        );

        scheduledAt = Instant.parse(
                "2026-12-20T19:00:00Z"
        );

        entity = persistedEntity(
                eventId,
                0L,
                "Rock Concert",
                "Live concert",
                scheduledAt,
                EventStatus.DRAFT
        );

        response = new EventResponse(
                eventId,
                0L,
                "Rock Concert",
                "Live concert",
                scheduledAt,
                EventStatus.DRAFT
        );

        domainEvent = mock(Event.class);
    }

    // =====================================================================
    // TEST FIXTURES
    // =====================================================================

    /**
     * Creates an EventEntity representing an entity that has already been
     * persisted by Hibernate.
     *
     * Unit tests do not run Hibernate, therefore:
     *
     * - @GeneratedValue does not generate the ID
     * - @Version does not initialize the version
     *
     * We explicitly initialize both values here.
     */
    private EventEntity persistedEntity(
            UUID id,
            Long version,
            String name,
            String description,
            Instant scheduledAt,
            EventStatus status
    ) {

        EventEntity entity = new EventEntity(
                name,
                description,
                scheduledAt,
                status
        );

        setField(
                entity,
                "id",
                id
        );

        setField(
                entity,
                "version",
                version
        );

        return entity;
    }

    /**
     * Creates an EventEntity representing a brand-new entity before
     * persistence.
     *
     * In real production code Hibernate would generate the ID and initialize
     * the @Version field during persistence.
     *
     * For service unit tests we generally don't need to reproduce that
     * behavior unless the test explicitly verifies the entity before/after
     * persistence.
     */
    private EventEntity newEntity(
            UUID id,
            String name,
            String description,
            Instant scheduledAt,
            EventStatus status
    ) {

        return persistedEntity(
                id,
                0L,
                name,
                description,
                scheduledAt,
                status
        );
    }

    /**
     * Test-only reflection helper.
     *
     * Production code should NOT use this.
     *
     * It is required here because EventEntity intentionally does not expose
     * public setters for persistence-managed fields such as ID and version.
     */
    private void setField(
            EventEntity entity,
            String fieldName,
            Object value
    ) {

        try {

            var field =
                    EventEntity.class.getDeclaredField(fieldName);

            field.setAccessible(true);

            field.set(
                    entity,
                    value
            );

        } catch (ReflectiveOperationException e) {

            throw new AssertionError(
                    "Could not initialize EventEntity field: "
                            + fieldName,
                    e
            );
        }
    }

    // =====================================================================
    // CREATE
    // =====================================================================

    @Nested
    @DisplayName("Create Event")
    class CreateTests {

        @Test
        @DisplayName("Should create and return a new event")
        void create_shouldCreateAndReturnEvent() {

            CreateEventRequest request =
                    new CreateEventRequest(
                            "Rock Concert",
                            "Live concert",
                            scheduledAt
                    );

            EventEntity newEntity =
                    newEntity(
                            eventId,
                            "Rock Concert",
                            "Live concert",
                            scheduledAt,
                            EventStatus.DRAFT
                    );

            EventEntity savedEntity =
                    persistedEntity(
                            eventId,
                            0L,
                            "Rock Concert",
                            "Live concert",
                            scheduledAt,
                            EventStatus.DRAFT
                    );

            when(eventMapper.toNewEntity(any(Event.class)))
                    .thenReturn(newEntity);

            when(eventRepository.saveAndFlush(newEntity))
                    .thenReturn(savedEntity);

            when(eventMapper.toResponse(savedEntity))
                    .thenReturn(response);

            EventResponse result =
                    eventService.create(request);

            assertEquals(
                    response,
                    result
            );

            assertEquals(
                    eventId,
                    result.id()
            );

            verify(eventMapper)
                    .toNewEntity(any(Event.class));

            verify(eventRepository)
                    .saveAndFlush(newEntity);

            verify(eventMapper)
                    .toResponse(savedEntity);

            verifyNoMoreInteractions(
                    eventRepository,
                    eventMapper
            );
        }

        @Test
        @DisplayName("Should create entity with version zero")
        void create_shouldCreateEntityWithVersionZero() {

            CreateEventRequest request =
                    new CreateEventRequest(
                            "New Event",
                            "Description",
                            scheduledAt
                    );

            EventEntity newEntity =
                    newEntity(
                            eventId,
                            "New Event",
                            "Description",
                            scheduledAt,
                            EventStatus.DRAFT
                    );

            when(eventMapper.toNewEntity(any(Event.class)))
                    .thenReturn(newEntity);

            when(eventRepository.saveAndFlush(newEntity))
                    .thenReturn(newEntity);

            when(eventMapper.toResponse(newEntity))
                    .thenReturn(response);

            eventService.create(request);

            ArgumentCaptor<EventEntity> captor =
                    ArgumentCaptor.forClass(
                            EventEntity.class
                    );

            verify(eventRepository)
                    .saveAndFlush(captor.capture());

            EventEntity captured =
                    captor.getValue();

            assertEquals(
                    eventId,
                    captured.id()
            );

            assertEquals(
                    0L,
                    captured.version()
            );

            assertEquals(
                    "New Event",
                    captured.getName()
            );

            assertEquals(
                    "Description",
                    captured.getDescription()
            );

            assertEquals(
                    EventStatus.DRAFT,
                    captured.getStatus()
            );
        }
    }

    // =====================================================================
    // GET BY ID
    // =====================================================================

    @Nested
    @DisplayName("Get Event By ID")
    class GetByIdTests {

        @Test
        @DisplayName("Should return event when found")
        void getById_shouldReturnEvent() {

            when(eventRepository.findById(eventId))
                    .thenReturn(
                            Optional.of(entity)
                    );

            when(eventMapper.toResponse(entity))
                    .thenReturn(response);

            EventResponse result =
                    eventService.getById(eventId);

            assertEquals(
                    response,
                    result
            );

            assertEquals(
                    eventId,
                    result.id()
            );

            verify(eventRepository)
                    .findById(eventId);

            verify(eventMapper)
                    .toResponse(entity);
        }

        @Test
        @DisplayName("Should throw when event does not exist")
        void getById_shouldThrowWhenMissing() {

            when(eventRepository.findById(eventId))
                    .thenReturn(Optional.empty());

            assertThrows(
                    EventNotFoundException.class,
                    () -> eventService.getById(eventId)
            );

            verify(eventRepository)
                    .findById(eventId);

            verifyNoInteractions(
                    eventMapper
            );
        }
    }

    // =====================================================================
    // GET ALL
    // =====================================================================

    @Nested
    @DisplayName("Get All Events")
    class GetAllTests {

        @Test
        @DisplayName("Should return all events")
        void getAll_shouldReturnAllEvents() {

            UUID secondEventId =
                    UUID.randomUUID();

            EventEntity secondEntity =
                    persistedEntity(
                            secondEventId,
                            0L,
                            "Jazz Festival",
                            "Live jazz",
                            scheduledAt,
                            EventStatus.DRAFT
                    );

            EventResponse secondResponse =
                    new EventResponse(
                            secondEventId,
                            0L,
                            "Jazz Festival",
                            "Live jazz",
                            scheduledAt,
                            EventStatus.DRAFT
                    );

            when(eventRepository.findAll())
                    .thenReturn(
                            List.of(
                                    entity,
                                    secondEntity
                            )
                    );

            when(eventMapper.toResponse(entity))
                    .thenReturn(response);

            when(eventMapper.toResponse(secondEntity))
                    .thenReturn(secondResponse);

            List<EventResponse> result =
                    eventService.getAll();

            assertEquals(
                    2,
                    result.size()
            );

            assertEquals(
                    response,
                    result.get(0)
            );

            assertEquals(
                    secondResponse,
                    result.get(1)
            );

            assertEquals(
                    eventId,
                    result.get(0).id()
            );

            assertEquals(
                    secondEventId,
                    result.get(1).id()
            );

            verify(eventRepository)
                    .findAll();

            verify(
                    eventMapper,
                    times(2)
            ).toResponse(
                    any(EventEntity.class)
            );
        }

        @Test
        @DisplayName("Should return empty list when no events exist")
        void getAll_shouldReturnEmptyList() {

            when(eventRepository.findAll())
                    .thenReturn(
                            List.of()
                    );

            List<EventResponse> result =
                    eventService.getAll();

            assertNotNull(result);

            assertTrue(
                    result.isEmpty()
            );

            verify(eventRepository)
                    .findAll();

            verifyNoInteractions(
                    eventMapper
            );
        }
    }

    // =====================================================================
    // UPDATE
    // =====================================================================

    @Nested
    @DisplayName("Update Event")
    class UpdateTests {

        @Test
        @DisplayName("Should update existing event")
        void update_shouldUpdateExistingEvent() {

            UpdateEventRequest request =
                    new UpdateEventRequest(
                            "Rock Concert Updated",
                            "Updated description",
                            Instant.parse(
                                    "2026-12-21T19:00:00Z"
                            )
                    );

            EventEntity savedEntity =
                    persistedEntity(
                            eventId,
                            1L,
                            "Rock Concert Updated",
                            "Updated description",
                            request.scheduledAt(),
                            EventStatus.DRAFT
                    );

            EventResponse updatedResponse =
                    new EventResponse(
                            eventId,
                            1L,
                            "Rock Concert Updated",
                            "Updated description",
                            request.scheduledAt(),
                            EventStatus.DRAFT
                    );

            when(eventRepository.findById(eventId))
                    .thenReturn(
                            Optional.of(entity)
                    );

            when(eventMapper.toDomain(entity))
                    .thenReturn(domainEvent);

            when(eventRepository.saveAndFlush(entity))
                    .thenReturn(savedEntity);

            when(eventMapper.toResponse(savedEntity))
                    .thenReturn(updatedResponse);

            EventResponse result =
                    eventService.update(
                            eventId,
                            request
                    );

            assertEquals(
                    updatedResponse,
                    result
            );

            assertEquals(
                    eventId,
                    result.id()
            );

            verify(eventRepository)
                    .findById(eventId);

            verify(eventMapper)
                    .toDomain(entity);

            verify(domainEvent)
                    .updateDetails(
                            any(),
                            any(),
                            eq(request.scheduledAt())
                    );

            verify(eventMapper)
                    .updateEntity(
                            domainEvent,
                            entity
                    );

            verify(eventRepository)
                    .saveAndFlush(entity);

            verify(eventMapper)
                    .toResponse(savedEntity);
        }

        @Test
        @DisplayName("Should throw when event does not exist")
        void update_shouldThrowWhenMissing() {

            UpdateEventRequest request =
                    new UpdateEventRequest(
                            "Updated",
                            "Updated description",
                            scheduledAt
                    );

            when(eventRepository.findById(eventId))
                    .thenReturn(
                            Optional.empty()
                    );

            assertThrows(
                    EventNotFoundException.class,
                    () -> eventService.update(
                            eventId,
                            request
                    )
            );

            verify(eventRepository)
                    .findById(eventId);

            verifyNoInteractions(
                    eventMapper
            );

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
    @DisplayName("Publish Event")
    class PublishTests {

        @Test
        @DisplayName("Should publish event")
        void publish_shouldPublishEvent() {

            EventEntity savedEntity =
                    persistedEntity(
                            eventId,
                            1L,
                            "Rock Concert",
                            "Live concert",
                            scheduledAt,
                            EventStatus.PUBLISHED
                    );

            EventResponse publishedResponse =
                    new EventResponse(
                            eventId,
                            1L,
                            "Rock Concert",
                            "Live concert",
                            scheduledAt,
                            EventStatus.PUBLISHED
                    );

            when(eventRepository.findById(eventId))
                    .thenReturn(
                            Optional.of(entity)
                    );

            when(eventMapper.toDomain(entity))
                    .thenReturn(domainEvent);

            when(eventRepository.saveAndFlush(entity))
                    .thenReturn(savedEntity);

            when(eventMapper.toResponse(savedEntity))
                    .thenReturn(publishedResponse);

            EventResponse result =
                    eventService.publish(eventId);

            assertEquals(
                    publishedResponse,
                    result
            );

            verify(domainEvent)
                    .publish();

            verify(eventMapper)
                    .updateEntity(
                            domainEvent,
                            entity
                    );

            verify(eventRepository)
                    .saveAndFlush(entity);

            verify(eventMapper)
                    .toResponse(savedEntity);
        }

        @Test
        @DisplayName("Should throw when event does not exist")
        void publish_shouldThrowWhenMissing() {

            when(eventRepository.findById(eventId))
                    .thenReturn(
                            Optional.empty()
                    );

            assertThrows(
                    EventNotFoundException.class,
                    () -> eventService.publish(eventId)
            );

            verify(eventRepository)
                    .findById(eventId);

            verifyNoInteractions(
                    eventMapper
            );

            verify(
                    eventRepository,
                    never()
            ).saveAndFlush(any());
        }

        @Test
        @DisplayName("Should propagate domain exception")
        void publish_shouldPropagateDomainException() {

            when(eventRepository.findById(eventId))
                    .thenReturn(
                            Optional.of(entity)
                    );

            when(eventMapper.toDomain(entity))
                    .thenReturn(domainEvent);

            doThrow(
                    new IllegalStateException(
                            "Event must be in DRAFT state"
                    )
            ).when(domainEvent)
                    .publish();

            assertThrows(
                    IllegalStateException.class,
                    () -> eventService.publish(eventId)
            );

            verify(domainEvent)
                    .publish();

            verify(
                    eventMapper,
                    never()
            ).updateEntity(
                    any(),
                    any()
            );

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
    @DisplayName("Cancel Event")
    class CancelTests {

        @Test
        @DisplayName("Should cancel event")
        void cancel_shouldCancelEvent() {

            EventEntity savedEntity =
                    persistedEntity(
                            eventId,
                            1L,
                            "Rock Concert",
                            "Live concert",
                            scheduledAt,
                            EventStatus.CANCELLED
                    );

            EventResponse cancelledResponse =
                    new EventResponse(
                            eventId,
                            1L,
                            "Rock Concert",
                            "Live concert",
                            scheduledAt,
                            EventStatus.CANCELLED
                    );

            when(eventRepository.findById(eventId))
                    .thenReturn(
                            Optional.of(entity)
                    );

            when(eventMapper.toDomain(entity))
                    .thenReturn(domainEvent);

            when(eventRepository.saveAndFlush(entity))
                    .thenReturn(savedEntity);

            when(eventMapper.toResponse(savedEntity))
                    .thenReturn(cancelledResponse);

            EventResponse result =
                    eventService.cancel(eventId);

            assertEquals(
                    cancelledResponse,
                    result
            );

            verify(domainEvent)
                    .cancel();

            verify(eventMapper)
                    .updateEntity(
                            domainEvent,
                            entity
                    );

            verify(eventRepository)
                    .saveAndFlush(entity);

            verify(eventMapper)
                    .toResponse(savedEntity);
        }

        @Test
        @DisplayName("Should throw when event does not exist")
        void cancel_shouldThrowWhenMissing() {

            when(eventRepository.findById(eventId))
                    .thenReturn(
                            Optional.empty()
                    );

            assertThrows(
                    EventNotFoundException.class,
                    () -> eventService.cancel(eventId)
            );

            verify(eventRepository)
                    .findById(eventId);

            verifyNoInteractions(
                    eventMapper
            );

            verify(
                    eventRepository,
                    never()
            ).saveAndFlush(any());
        }

        @Test
        @DisplayName("Should propagate domain exception")
        void cancel_shouldPropagateDomainException() {

            when(eventRepository.findById(eventId))
                    .thenReturn(
                            Optional.of(entity)
                    );

            when(eventMapper.toDomain(entity))
                    .thenReturn(domainEvent);

            doThrow(
                    new IllegalStateException(
                            "Event cannot be cancelled"
                    )
            ).when(domainEvent)
                    .cancel();

            assertThrows(
                    IllegalStateException.class,
                    () -> eventService.cancel(eventId)
            );

            verify(domainEvent)
                    .cancel();

            verify(
                    eventMapper,
                    never()
            ).updateEntity(
                    any(),
                    any()
            );

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
    @DisplayName("Complete Event")
    class CompleteTests {

        @Test
        @DisplayName("Should complete event")
        void complete_shouldCompleteEvent() {

            EventEntity savedEntity =
                    persistedEntity(
                            eventId,
                            2L,
                            "Rock Concert",
                            "Live concert",
                            scheduledAt,
                            EventStatus.COMPLETED
                    );

            EventResponse completedResponse =
                    new EventResponse(
                            eventId,
                            2L,
                            "Rock Concert",
                            "Live concert",
                            scheduledAt,
                            EventStatus.COMPLETED
                    );

            when(eventRepository.findById(eventId))
                    .thenReturn(
                            Optional.of(entity)
                    );

            when(eventMapper.toDomain(entity))
                    .thenReturn(domainEvent);

            when(eventRepository.saveAndFlush(entity))
                    .thenReturn(savedEntity);

            when(eventMapper.toResponse(savedEntity))
                    .thenReturn(completedResponse);

            EventResponse result =
                    eventService.complete(eventId);

            assertEquals(
                    completedResponse,
                    result
            );

            verify(domainEvent)
                    .complete();

            verify(eventMapper)
                    .updateEntity(
                            domainEvent,
                            entity
                    );

            verify(eventRepository)
                    .saveAndFlush(entity);

            verify(eventMapper)
                    .toResponse(savedEntity);
        }

        @Test
        @DisplayName("Should throw when event does not exist")
        void complete_shouldThrowWhenMissing() {

            when(eventRepository.findById(eventId))
                    .thenReturn(
                            Optional.empty()
                    );

            assertThrows(
                    EventNotFoundException.class,
                    () -> eventService.complete(eventId)
            );

            verify(eventRepository)
                    .findById(eventId);

            verifyNoInteractions(
                    eventMapper
            );

            verify(
                    eventRepository,
                    never()
            ).saveAndFlush(any());
        }

        @Test
        @DisplayName("Should propagate domain exception")
        void complete_shouldPropagateDomainException() {

            when(eventRepository.findById(eventId))
                    .thenReturn(
                            Optional.of(entity)
                    );

            when(eventMapper.toDomain(entity))
                    .thenReturn(domainEvent);

            doThrow(
                    new IllegalStateException(
                            "Event cannot be completed"
                    )
            ).when(domainEvent)
                    .complete();

            assertThrows(
                    IllegalStateException.class,
                    () -> eventService.complete(eventId)
            );

            verify(domainEvent)
                    .complete();

            verify(
                    eventMapper,
                    never()
            ).updateEntity(
                    any(),
                    any()
            );

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
    @DisplayName("Delete Event")
    class DeleteTests {

        @Test
        @DisplayName("Should delete existing event")
        void delete_shouldDeleteExistingEvent() {

            when(eventRepository.findById(eventId))
                    .thenReturn(
                            Optional.of(entity)
                    );

            eventService.delete(eventId);

            verify(eventRepository)
                    .findById(eventId);

            verify(eventRepository)
                    .delete(entity);
        }

        @Test
        @DisplayName("Should throw when event does not exist")
        void delete_shouldThrowWhenMissing() {

            when(eventRepository.findById(eventId))
                    .thenReturn(
                            Optional.empty()
                    );

            assertThrows(
                    EventNotFoundException.class,
                    () -> eventService.delete(eventId)
            );

            verify(eventRepository)
                    .findById(eventId);

            verify(
                    eventRepository,
                    never()
            ).delete(any());
        }
    }
}