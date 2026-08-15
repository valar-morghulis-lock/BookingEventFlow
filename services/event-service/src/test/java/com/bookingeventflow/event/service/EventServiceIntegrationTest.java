package com.bookingeventflow.event.service;

import com.bookingeventflow.event.domain.model.EventStatus;
import com.bookingeventflow.event.entity.EventEntity;
import com.bookingeventflow.event.exception.EventNotFoundException;
import com.bookingeventflow.event.presentation.request.CreateEventRequest;
import com.bookingeventflow.event.presentation.request.UpdateEventRequest;
import com.bookingeventflow.event.presentation.response.EventPageResponse;
import com.bookingeventflow.event.presentation.response.EventResponse;
import com.bookingeventflow.event.repository.EventRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.transaction.support.TransactionTemplate;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.Instant;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
@Testcontainers
class EventServiceIntegrationTest {

    private static final int DEFAULT_LIMIT = 20;

    private static final Instant FIRST_SCHEDULED_AT =
            Instant.parse("2026-12-20T19:00:00Z");

    private static final Instant SECOND_SCHEDULED_AT =
            Instant.parse("2026-12-21T19:00:00Z");

    private static final Instant THIRD_SCHEDULED_AT =
            Instant.parse("2026-12-22T19:00:00Z");

    @Container
    static final PostgreSQLContainer<?> POSTGRES =
            new PostgreSQLContainer<>("postgres:17")
                    .withDatabaseName("event_service")
                    .withUsername("event_service")
                    .withPassword("event_service");

    @DynamicPropertySource
    static void configureDatabase(
            DynamicPropertyRegistry registry
    ) {

        registry.add(
                "spring.datasource.url",
                POSTGRES::getJdbcUrl
        );

        registry.add(
                "spring.datasource.username",
                POSTGRES::getUsername
        );

        registry.add(
                "spring.datasource.password",
                POSTGRES::getPassword
        );

        registry.add(
                "spring.datasource.driver-class-name",
                POSTGRES::getDriverClassName
        );

        registry.add(
                "spring.flyway.url",
                POSTGRES::getJdbcUrl
        );

        registry.add(
                "spring.flyway.user",
                POSTGRES::getUsername
        );

        registry.add(
                "spring.flyway.password",
                POSTGRES::getPassword
        );

        registry.add(
                "spring.jpa.show-sql",
                () -> "false"
        );

        registry.add(
                "spring.jpa.properties.hibernate.format_sql",
                () -> "true"
        );
    }

    @Autowired
    private EventService eventService;

    @Autowired
    private EventRepository eventRepository;

    @Autowired
    private TransactionTemplate transactionTemplate;

    @BeforeEach
    void setUp() {

        eventRepository.deleteAll();
        eventRepository.flush();
    }

    // =========================================================
    // CREATE
    // =========================================================

    @Test
    void create_shouldPersistEvent() {

        CreateEventRequest request =
                new CreateEventRequest(
                        "Rock Concert",
                        "Live concert",
                        FIRST_SCHEDULED_AT
                );

        EventResponse result =
                eventService.create(request);

        assertNotNull(result.id());

        assertEquals(
                0L,
                result.version()
        );

        assertEquals(
                "Rock Concert",
                result.name()
        );

        assertEquals(
                "Live concert",
                result.description()
        );

        assertEquals(
                EventStatus.DRAFT,
                result.status()
        );

        EventEntity persisted =
                eventRepository.findById(result.id())
                        .orElseThrow();

        assertEquals(
                result.id(),
                persisted.id()
        );

        assertEquals(
                0L,
                persisted.version()
        );

        assertEquals(
                EventStatus.DRAFT,
                persisted.getStatus()
        );
    }

    // =========================================================
    // GET BY ID
    // =========================================================

    @Test
    void getById_shouldReturnPersistedEvent() {

        EventResponse created =
                eventService.create(
                        new CreateEventRequest(
                                "Rock Concert",
                                "Live concert",
                                FIRST_SCHEDULED_AT
                        )
                );

        EventResponse result =
                eventService.getById(
                        created.id()
                );

        assertEquals(
                created.id(),
                result.id()
        );

        assertEquals(
                "Rock Concert",
                result.name()
        );

        assertEquals(
                EventStatus.DRAFT,
                result.status()
        );
    }

    @Test
    void getById_shouldThrowEventNotFoundException_whenMissing() {

        UUID id =
                UUID.randomUUID();

        assertThrows(
                EventNotFoundException.class,
                () -> eventService.getById(id)
        );
    }

    // =========================================================
    // GET ALL - KEYSET PAGINATION
    // =========================================================

    @Test
    void getAll_shouldReturnEvents_withoutStatusFilter() {

        eventService.create(
                new CreateEventRequest(
                        "Rock Concert",
                        "Live concert",
                        FIRST_SCHEDULED_AT
                )
        );

        eventService.create(
                new CreateEventRequest(
                        "Jazz Festival",
                        "Live jazz",
                        SECOND_SCHEDULED_AT
                )
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
                "Rock Concert",
                result.items().get(0).name()
        );

        assertEquals(
                "Jazz Festival",
                result.items().get(1).name()
        );

        assertNull(
                result.nextCursor()
        );
    }

    @Test
    void getAll_shouldReturnEmptyPage_whenNoEventsExist() {

        EventPageResponse result =
                eventService.getAll(
                        null,
                        DEFAULT_LIMIT,
                        null
                );

        assertNotNull(result);

        assertNotNull(
                result.items()
        );

        assertTrue(
                result.items().isEmpty()
        );

        assertNull(
                result.nextCursor()
        );
    }

    // =========================================================
    // STATUS FILTERING
    // =========================================================

    @Test
    void getAll_shouldReturnOnlyDraftEvents_whenFilteringByDraft() {

        EventResponse draft =
                eventService.create(
                        new CreateEventRequest(
                                "Draft Event",
                                "Draft",
                                FIRST_SCHEDULED_AT
                        )
                );

        EventResponse published =
                eventService.create(
                        new CreateEventRequest(
                                "Published Event",
                                "Published",
                                SECOND_SCHEDULED_AT
                        )
                );

        eventService.publish(
                published.id()
        );

        EventPageResponse result =
                eventService.getAll(
                        EventStatus.DRAFT,
                        DEFAULT_LIMIT,
                        null
                );

        assertEquals(
                1,
                result.items().size()
        );

        assertEquals(
                draft.id(),
                result.items().get(0).id()
        );

        assertEquals(
                EventStatus.DRAFT,
                result.items().get(0).status()
        );
    }

    @Test
    void getAll_shouldReturnOnlyPublishedEvents_whenFilteringByPublished() {

        eventService.create(
                new CreateEventRequest(
                        "Draft Event",
                        "Draft",
                        FIRST_SCHEDULED_AT
                )
        );

        EventResponse published =
                eventService.create(
                        new CreateEventRequest(
                                "Published Event",
                                "Published",
                                SECOND_SCHEDULED_AT
                        )
                );

        eventService.publish(
                published.id()
        );

        EventPageResponse result =
                eventService.getAll(
                        EventStatus.PUBLISHED,
                        DEFAULT_LIMIT,
                        null
                );

        assertEquals(
                1,
                result.items().size()
        );

        assertEquals(
                published.id(),
                result.items().get(0).id()
        );

        assertEquals(
                EventStatus.PUBLISHED,
                result.items().get(0).status()
        );
    }

    @Test
    void getAll_shouldReturnOnlyCancelledEvents_whenFilteringByCancelled() {

        EventResponse cancelled =
                eventService.create(
                        new CreateEventRequest(
                                "Cancelled Event",
                                "Cancelled",
                                FIRST_SCHEDULED_AT
                        )
                );

        eventService.cancel(
                cancelled.id()
        );

        eventService.create(
                new CreateEventRequest(
                        "Draft Event",
                        "Draft",
                        SECOND_SCHEDULED_AT
                )
        );

        EventPageResponse result =
                eventService.getAll(
                        EventStatus.CANCELLED,
                        DEFAULT_LIMIT,
                        null
                );

        assertEquals(
                1,
                result.items().size()
        );

        assertEquals(
                cancelled.id(),
                result.items().get(0).id()
        );

        assertEquals(
                EventStatus.CANCELLED,
                result.items().get(0).status()
        );
    }

    @Test
    void getAll_shouldReturnOnlyCompletedEvents_whenFilteringByCompleted() {

        EventResponse completed =
                eventService.create(
                        new CreateEventRequest(
                                "Completed Event",
                                "Completed",
                                FIRST_SCHEDULED_AT
                        )
                );

        eventService.publish(
                completed.id()
        );

        eventService.complete(
                completed.id()
        );

        EventPageResponse result =
                eventService.getAll(
                        EventStatus.COMPLETED,
                        DEFAULT_LIMIT,
                        null
                );

        assertEquals(
                1,
                result.items().size()
        );

        assertEquals(
                completed.id(),
                result.items().get(0).id()
        );

        assertEquals(
                EventStatus.COMPLETED,
                result.items().get(0).status()
        );
    }

    @Test
    void getAll_shouldReturnEmptyPage_whenStatusHasNoEvents() {

        eventService.create(
                new CreateEventRequest(
                        "Draft Event",
                        "Draft",
                        FIRST_SCHEDULED_AT
                )
        );

        EventPageResponse result =
                eventService.getAll(
                        EventStatus.PUBLISHED,
                        DEFAULT_LIMIT,
                        null
                );

        assertNotNull(result);

        assertTrue(
                result.items().isEmpty()
        );

        assertNull(
                result.nextCursor()
        );
    }

    // =========================================================
    // STATUS FILTER + PAGINATION
    // =========================================================

    @Test
    void getAll_shouldPaginateWithinSelectedStatus() {

        EventResponse first =
                eventService.create(
                        new CreateEventRequest(
                                "Published Event 1",
                                "Published",
                                FIRST_SCHEDULED_AT
                        )
                );

        EventResponse second =
                eventService.create(
                        new CreateEventRequest(
                                "Published Event 2",
                                "Published",
                                SECOND_SCHEDULED_AT
                        )
                );

        EventResponse third =
                eventService.create(
                        new CreateEventRequest(
                                "Published Event 3",
                                "Published",
                                THIRD_SCHEDULED_AT
                        )
                );

        eventService.publish(first.id());
        eventService.publish(second.id());
        eventService.publish(third.id());

        EventPageResponse firstPage =
                eventService.getAll(
                        EventStatus.PUBLISHED,
                        2,
                        null
                );

        assertEquals(
                2,
                firstPage.items().size()
        );

        assertEquals(
                first.id(),
                firstPage.items().get(0).id()
        );

        assertEquals(
                second.id(),
                firstPage.items().get(1).id()
        );

        assertNotNull(
                firstPage.nextCursor()
        );

        EventPageResponse secondPage =
                eventService.getAll(
                        EventStatus.PUBLISHED,
                        2,
                        firstPage.nextCursor()
                );

        assertEquals(
                1,
                secondPage.items().size()
        );

        assertEquals(
                third.id(),
                secondPage.items().get(0).id()
        );

        assertNull(
                secondPage.nextCursor()
        );
    }

    // =========================================================
    // UPDATE
    // =========================================================

    @Test
    void update_shouldPersistChangesAndIncrementVersion() {

        EventResponse created =
                eventService.create(
                        new CreateEventRequest(
                                "Rock Concert",
                                "Live concert",
                                FIRST_SCHEDULED_AT
                        )
                );

        assertEquals(
                0L,
                created.version()
        );

        UpdateEventRequest request =
                new UpdateEventRequest(
                        "Rock Concert Updated",
                        "Updated description",
                        SECOND_SCHEDULED_AT
                );

        EventResponse updated =
                eventService.update(
                        created.id(),
                        request
                );

        assertEquals(
                created.id(),
                updated.id()
        );

        assertEquals(
                1L,
                updated.version()
        );

        assertEquals(
                "Rock Concert Updated",
                updated.name()
        );

        assertEquals(
                "Updated description",
                updated.description()
        );

        assertEquals(
                SECOND_SCHEDULED_AT,
                updated.scheduledAt()
        );

        assertEquals(
                EventStatus.DRAFT,
                updated.status()
        );
    }

    @Test
    void update_shouldThrowEventNotFoundException_whenMissing() {

        UUID id =
                UUID.randomUUID();

        UpdateEventRequest request =
                new UpdateEventRequest(
                        "Updated",
                        "Updated description",
                        FIRST_SCHEDULED_AT
                );

        assertThrows(
                EventNotFoundException.class,
                () -> eventService.update(
                        id,
                        request
                )
        );
    }

    // =========================================================
    // PUBLISH
    // =========================================================

    @Test
    void publish_shouldChangeStatusAndIncrementVersion() {

        EventResponse created =
                eventService.create(
                        new CreateEventRequest(
                                "Rock Concert",
                                "Live concert",
                                FIRST_SCHEDULED_AT
                        )
                );

        EventResponse published =
                eventService.publish(
                        created.id()
                );

        assertEquals(
                EventStatus.PUBLISHED,
                published.status()
        );

        assertEquals(
                1L,
                published.version()
        );

        EventEntity persisted =
                eventRepository.findById(
                        created.id()
                ).orElseThrow();

        assertEquals(
                EventStatus.PUBLISHED,
                persisted.getStatus()
        );
    }

    // =========================================================
    // CANCEL
    // =========================================================

    @Test
    void cancel_shouldChangeStatusAndIncrementVersion() {

        EventResponse created =
                eventService.create(
                        new CreateEventRequest(
                                "Rock Concert",
                                "Live concert",
                                FIRST_SCHEDULED_AT
                        )
                );

        EventResponse cancelled =
                eventService.cancel(
                        created.id()
                );

        assertEquals(
                EventStatus.CANCELLED,
                cancelled.status()
        );

        assertEquals(
                1L,
                cancelled.version()
        );

        EventEntity persisted =
                eventRepository.findById(
                        created.id()
                ).orElseThrow();

        assertEquals(
                EventStatus.CANCELLED,
                persisted.getStatus()
        );
    }

    // =========================================================
    // COMPLETE
    // =========================================================

    @Test
    void complete_shouldChangeStatusAndIncrementVersion() {

        EventResponse created =
                eventService.create(
                        new CreateEventRequest(
                                "Rock Concert",
                                "Live concert",
                                FIRST_SCHEDULED_AT
                        )
                );

        EventResponse published =
                eventService.publish(
                        created.id()
                );

        assertEquals(
                EventStatus.PUBLISHED,
                published.status()
        );

        EventResponse completed =
                eventService.complete(
                        created.id()
                );

        assertEquals(
                EventStatus.COMPLETED,
                completed.status()
        );

        assertEquals(
                2L,
                completed.version()
        );

        EventEntity persisted =
                eventRepository.findById(
                        created.id()
                ).orElseThrow();

        assertEquals(
                EventStatus.COMPLETED,
                persisted.getStatus()
        );
    }

    // =========================================================
    // DELETE
    // =========================================================

    @Test
    void delete_shouldRemoveEvent() {

        EventResponse created =
                eventService.create(
                        new CreateEventRequest(
                                "Rock Concert",
                                "Live concert",
                                FIRST_SCHEDULED_AT
                        )
                );

        UUID id =
                created.id();

        assertTrue(
                eventRepository.existsById(id)
        );

        eventService.delete(id);

        assertFalse(
                eventRepository.existsById(id)
        );
    }

    @Test
    void delete_shouldThrowEventNotFoundException_whenMissing() {

        UUID id =
                UUID.randomUUID();

        assertThrows(
                EventNotFoundException.class,
                () -> eventService.delete(id)
        );
    }

    // =========================================================
    // OPTIMISTIC LOCKING
    // =========================================================

    @Test
    void concurrentUpdates_shouldFailWithOptimisticLockingException() {

        EventResponse created =
                eventService.create(
                        new CreateEventRequest(
                                "Rock Concert",
                                "Live concert",
                                FIRST_SCHEDULED_AT
                        )
                );

        UUID id =
                created.id();

        EventEntity first =
                transactionTemplate.execute(
                        status ->
                                eventRepository
                                        .findById(id)
                                        .orElseThrow()
                );

        EventEntity second =
                transactionTemplate.execute(
                        status ->
                                eventRepository
                                        .findById(id)
                                        .orElseThrow()
                );

        assertNotNull(first);
        assertNotNull(second);

        assertNotSame(
                first,
                second
        );

        assertEquals(
                0L,
                first.version()
        );

        assertEquals(
                0L,
                second.version()
        );

        transactionTemplate.executeWithoutResult(
                status -> {

                    EventEntity current =
                            eventRepository
                                    .findById(id)
                                    .orElseThrow();

                    current.setName(
                            "Updated by transaction 1"
                    );

                    eventRepository.saveAndFlush(
                            current
                    );

                    assertEquals(
                            1L,
                            current.version()
                    );
                }
        );

        assertEquals(
                0L,
                second.version()
        );

        second.setName(
                "Updated by transaction 2"
        );

        assertThrows(
                OptimisticLockingFailureException.class,
                () ->
                        transactionTemplate.executeWithoutResult(
                                status ->
                                        eventRepository
                                                .saveAndFlush(second)
                        )
        );

        EventEntity persisted =
                eventRepository.findById(id)
                        .orElseThrow();

        assertEquals(
                "Updated by transaction 1",
                persisted.getName()
        );

        assertEquals(
                1L,
                persisted.version()
        );
    }
}