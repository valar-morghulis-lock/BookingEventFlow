package com.bookingeventflow.event.controller;

import com.bookingeventflow.common.pagination.CursorDecodingException;
import com.bookingeventflow.event.domain.model.EventStatus;
import com.bookingeventflow.event.entity.EventEntity;
import com.bookingeventflow.event.exception.EventNotFoundException;
import com.bookingeventflow.event.exception.InvalidEventStateException;
import com.bookingeventflow.event.presentation.response.EventPageResponse;
import com.bookingeventflow.event.presentation.response.EventResponse;
import com.bookingeventflow.event.service.EventService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.MediaType;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(EventController.class)
class EventControllerTest {

    private static final String EVENTS_URL =
            "/api/v1/events";

    private static final UUID EVENT_ID =
            UUID.fromString(
                    "512c5d0f-a416-4430-9265-4069b1637964"
            );

    private static final String EVENT_URL =
            EVENTS_URL + "/" + EVENT_ID;

    private static final String VALID_REQUEST = """
            {
              "name": "Rock Concert",
              "description": "Live concert",
              "scheduledAt": "2026-12-20T19:00:00Z",
              "numberOfRows": 10
            }
            """;

    private static final String UPDATED_REQUEST = """
        {
            "name": "Rock Concert",
            "description": "Live concert 2x",
            "scheduledAt": "2026-12-20T19:00:00Z",
            "numberOfRows": 5
        }
        """;

    private static final String INVALID_REQUEST = """
            {
              "name": "",
              "description": "",
              "scheduledAt": "2020-01-01T00:00:00Z",
              "numberOfRows": 0
            }
            """;

    private static final Instant SCHEDULED_AT =
            Instant.parse("2026-12-20T19:00:00Z");

    private static final int DEFAULT_NUMBER_OF_ROWS = 10;

    private static final String NEXT_CURSOR =
            "next-cursor";

    private static final String PREVIOUS_CURSOR =
            "previous-cursor";

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private EventService eventService;

    // =========================================================
    // TEST FIXTURES
    // =========================================================

    private EventResponse response() {

        return response(
                0L,
                "Rock Concert",
                "Live concert",
                EventStatus.DRAFT
        );
    }

    private EventResponse response(
            long version,
            String name,
            String description,
            EventStatus status
    ) {

        return new EventResponse(
                EVENT_ID,
                version,
                name,
                description,
                SCHEDULED_AT,
                DEFAULT_NUMBER_OF_ROWS,
                status
        );
    }

    private EventPageResponse pageResponse() {

        return new EventPageResponse(
                List.of(response()),
                NEXT_CURSOR
        );
    }

    private EventPageResponse emptyPageResponse() {

        return new EventPageResponse(
                List.of(),
                null
        );
    }

    // =========================================================
    // CREATE
    // =========================================================

    @Test
    void create_shouldReturn201() throws Exception {

        when(eventService.create(any()))
                .thenReturn(response());

        mockMvc.perform(
                        post(EVENTS_URL)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(VALID_REQUEST)
                )
                .andExpect(status().isCreated())
                .andExpect(
                        jsonPath("$.id")
                                .value(EVENT_ID.toString())
                )
                .andExpect(
                        jsonPath("$.version")
                                .value(0)
                )
                .andExpect(
                        jsonPath("$.name")
                                .value("Rock Concert")
                )
                .andExpect(
                        jsonPath("$.description")
                                .value("Live concert")
                )
                .andExpect(
                        jsonPath("$.scheduledAt")
                                .value("2026-12-20T19:00:00Z")
                )
                .andExpect(
                        jsonPath("$.numberOfRows")
                                .value(DEFAULT_NUMBER_OF_ROWS)
                )
                .andExpect(
                        jsonPath("$.status")
                                .value("DRAFT")
                );

        verify(eventService).create(any());
    }

    @Test
    void create_shouldReturn400_whenValidationFails()
            throws Exception {

        mockMvc.perform(
                        post(EVENTS_URL)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(INVALID_REQUEST)
                )
                .andExpect(status().isBadRequest())
                .andExpect(
                        jsonPath("$.status")
                                .value(400)
                )
                .andExpect(
                        jsonPath("$.error")
                                .value("Bad Request")
                )
                .andExpect(
                        jsonPath("$.message")
                                .value("Validation failed")
                )
                .andExpect(
                        jsonPath("$.path")
                                .value(EVENTS_URL)
                )
                .andExpect(
                        jsonPath("$.errors.name")
                                .isArray()
                )
                .andExpect(
                        jsonPath("$.errors.name[0]")
                                .value("Name must not be blank")
                )
                .andExpect(
                        jsonPath("$.errors.scheduledAt")
                                .isArray()
                )
                .andExpect(
                        jsonPath("$.errors.scheduledAt[0]")
                                .value(
                                        "Scheduled time must be in the future"
                                )
                );

        verify(
                eventService,
                never()
        ).create(any());
    }

    @Test
    void create_shouldReturn409_whenDataIntegrityViolationOccurs()
            throws Exception {

        when(eventService.create(any()))
                .thenThrow(
                        new DataIntegrityViolationException(
                                "Duplicate event"
                        )
                );

        mockMvc.perform(
                        post(EVENTS_URL)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(VALID_REQUEST)
                )
                .andExpect(status().isConflict())
                .andExpect(
                        jsonPath("$.status")
                                .value(409)
                )
                .andExpect(
                        jsonPath("$.error")
                                .value("Conflict")
                )
                .andExpect(
                        jsonPath("$.message")
                                .value(
                                        "The request could not be completed "
                                                + "because it violates a data constraint."
                                )
                )
                .andExpect(
                        jsonPath("$.errors")
                                .isEmpty()
                );

        verify(eventService).create(any());
    }

    // =========================================================
    // GET BY ID
    // =========================================================

    @Test
    void getById_shouldReturn200() throws Exception {

        when(eventService.getById(EVENT_ID))
                .thenReturn(response());

        mockMvc.perform(
                        get(EVENT_URL)
                )
                .andExpect(status().isOk())
                .andExpect(
                        jsonPath("$.id")
                                .value(EVENT_ID.toString())
                )
                .andExpect(
                        jsonPath("$.version")
                                .value(0)
                )
                .andExpect(
                        jsonPath("$.name")
                                .value("Rock Concert")
                )
                .andExpect(
                        jsonPath("$.description")
                                .value("Live concert")
                )
                .andExpect(
                        jsonPath("$.scheduledAt")
                                .value("2026-12-20T19:00:00Z")
                )
                .andExpect(
                        jsonPath("$.numberOfRows")
                                .value(DEFAULT_NUMBER_OF_ROWS)
                )
                .andExpect(
                        jsonPath("$.status")
                                .value("DRAFT")
                );

        verify(eventService).getById(EVENT_ID);
    }

    @Test
    void getById_shouldReturn404_whenEventDoesNotExist()
            throws Exception {

        when(eventService.getById(EVENT_ID))
                .thenThrow(
                        new EventNotFoundException(EVENT_ID)
                );

        mockMvc.perform(
                        get(EVENT_URL)
                )
                .andExpect(status().isNotFound())
                .andExpect(
                        jsonPath("$.status")
                                .value(404)
                )
                .andExpect(
                        jsonPath("$.error")
                                .value("Not Found")
                )
                .andExpect(
                        jsonPath("$.path")
                                .value(EVENT_URL)
                )
                .andExpect(
                        jsonPath("$.errors")
                                .isEmpty()
                );

        verify(eventService).getById(EVENT_ID);
    }

    @Test
    void getById_shouldReturn400_whenIdIsInvalid()
            throws Exception {

        mockMvc.perform(
                        get(EVENTS_URL + "/not-a-uuid")
                )
                .andExpect(status().isBadRequest());

        verify(
                eventService,
                never()
        ).getById(any());
    }

    @Test
    void getById_shouldReturn500_whenUnexpectedExceptionOccurs()
            throws Exception {

        when(eventService.getById(EVENT_ID))
                .thenThrow(
                        new RuntimeException(
                                "Database connection failed"
                        )
                );

        mockMvc.perform(
                        get(EVENT_URL)
                )
                .andExpect(status().isInternalServerError())
                .andExpect(
                        jsonPath("$.status")
                                .value(500)
                )
                .andExpect(
                        jsonPath("$.error")
                                .value("Internal Server Error")
                )
                .andExpect(
                        jsonPath("$.message")
                                .value(
                                        "An unexpected error occurred."
                                )
                )
                .andExpect(
                        jsonPath("$.path")
                                .value(EVENT_URL)
                )
                .andExpect(
                        jsonPath("$.errors")
                                .isEmpty()
                );

        verify(eventService).getById(EVENT_ID);
    }

    // =========================================================
    // GET ALL - KEYSET PAGINATION + STATUS FILTER
    // =========================================================

    @Test
    void getAll_shouldReturn200_withDefaultLimit()
            throws Exception {

        when(
                eventService.getAll(
                        null,
                        20,
                        null
                )
        ).thenReturn(pageResponse());

        mockMvc.perform(
                        get(EVENTS_URL)
                )
                .andExpect(status().isOk())
                .andExpect(
                        jsonPath("$.items")
                                .isArray()
                )
                .andExpect(
                        jsonPath("$.items.length()")
                                .value(1)
                )
                .andExpect(
                        jsonPath("$.items[0].id")
                                .value(EVENT_ID.toString())
                )
                .andExpect(
                        jsonPath("$.items[0].numberOfRows")
                                .value(DEFAULT_NUMBER_OF_ROWS)
                )
                .andExpect(
                        jsonPath("$.items[0].status")
                                .value("DRAFT")
                )
                .andExpect(
                        jsonPath("$.nextCursor")
                                .value(NEXT_CURSOR)
                );

        verify(
                eventService
        ).getAll(
                null,
                20,
                null
        );
    }

    @Test
    void getAll_shouldReturn200_withStatusFilter()
            throws Exception {

        EventResponse published =
                response(
                        1L,
                        "Rock Concert",
                        "Live concert",
                        EventStatus.PUBLISHED
                );

        EventPageResponse page =
                new EventPageResponse(
                        List.of(published),
                        NEXT_CURSOR
                );

        when(
                eventService.getAll(
                        EventStatus.PUBLISHED,
                        20,
                        null
                )
        ).thenReturn(page);

        mockMvc.perform(
                        get(EVENTS_URL)
                                .param(
                                        "status",
                                        "PUBLISHED"
                                )
                )
                .andExpect(status().isOk())
                .andExpect(
                        jsonPath("$.items.length()")
                                .value(1)
                )
                .andExpect(
                        jsonPath("$.items[0].numberOfRows")
                                .value(DEFAULT_NUMBER_OF_ROWS)
                )
                .andExpect(
                        jsonPath("$.items[0].status")
                                .value("PUBLISHED")
                )
                .andExpect(
                        jsonPath("$.nextCursor")
                                .value(NEXT_CURSOR)
                );

        verify(
                eventService
        ).getAll(
                EventStatus.PUBLISHED,
                20,
                null
        );
    }

    @Test
    void getAll_shouldReturn200_withStatusAndCustomLimit()
            throws Exception {

        when(
                eventService.getAll(
                        EventStatus.DRAFT,
                        50,
                        null
                )
        ).thenReturn(pageResponse());

        mockMvc.perform(
                        get(EVENTS_URL)
                                .param("status", "DRAFT")
                                .param("limit", "50")
                )
                .andExpect(status().isOk())
                .andExpect(
                        jsonPath("$.items.length()")
                                .value(1)
                );

        verify(
                eventService
        ).getAll(
                EventStatus.DRAFT,
                50,
                null
        );
    }

    @Test
    void getAll_shouldReturn200_withAfterCursor()
            throws Exception {

        EventPageResponse page =
                new EventPageResponse(
                        List.of(response()),
                        "next-cursor-2"
                );

        when(
                eventService.getAll(
                        null,
                        20,
                        PREVIOUS_CURSOR
                )
        ).thenReturn(page);

        mockMvc.perform(
                        get(EVENTS_URL)
                                .param(
                                        "after",
                                        PREVIOUS_CURSOR
                                )
                )
                .andExpect(status().isOk())
                .andExpect(
                        jsonPath("$.items.length()")
                                .value(1)
                )
                .andExpect(
                        jsonPath("$.items[0].numberOfRows")
                                .value(DEFAULT_NUMBER_OF_ROWS)
                )
                .andExpect(
                        jsonPath("$.nextCursor")
                                .value("next-cursor-2")
                );

        verify(
                eventService
        ).getAll(
                null,
                20,
                PREVIOUS_CURSOR
        );
    }

    @Test
    void getAll_shouldReturn200_withStatusAndAfterCursor()
            throws Exception {

        EventPageResponse page =
                new EventPageResponse(
                        List.of(
                                response(
                                        1L,
                                        "Rock Concert",
                                        "Live concert",
                                        EventStatus.PUBLISHED
                                )
                        ),
                        "next-cursor-2"
                );

        when(
                eventService.getAll(
                        EventStatus.PUBLISHED,
                        20,
                        PREVIOUS_CURSOR
                )
        ).thenReturn(page);

        mockMvc.perform(
                        get(EVENTS_URL)
                                .param(
                                        "status",
                                        "PUBLISHED"
                                )
                                .param(
                                        "after",
                                        PREVIOUS_CURSOR
                                )
                )
                .andExpect(status().isOk())
                .andExpect(
                        jsonPath("$.items.length()")
                                .value(1)
                )
                .andExpect(
                        jsonPath("$.items[0].numberOfRows")
                                .value(DEFAULT_NUMBER_OF_ROWS)
                )
                .andExpect(
                        jsonPath("$.items[0].status")
                                .value("PUBLISHED")
                )
                .andExpect(
                        jsonPath("$.nextCursor")
                                .value("next-cursor-2")
                );

        verify(
                eventService
        ).getAll(
                EventStatus.PUBLISHED,
                20,
                PREVIOUS_CURSOR
        );
    }

    @Test
    void getAll_shouldReturn200_withCustomLimitAndCursor()
            throws Exception {

        when(
                eventService.getAll(
                        null,
                        50,
                        PREVIOUS_CURSOR
                )
        ).thenReturn(pageResponse());

        mockMvc.perform(
                        get(EVENTS_URL)
                                .param("limit", "50")
                                .param(
                                        "after",
                                        PREVIOUS_CURSOR
                                )
                )
                .andExpect(status().isOk())
                .andExpect(
                        jsonPath("$.items.length()")
                                .value(1)
                );

        verify(
                eventService
        ).getAll(
                null,
                50,
                PREVIOUS_CURSOR
        );
    }

    @Test
    void getAll_shouldReturn200_withEmptyPage()
            throws Exception {

        when(
                eventService.getAll(
                        null,
                        20,
                        null
                )
        ).thenReturn(emptyPageResponse());

        mockMvc.perform(
                        get(EVENTS_URL)
                )
                .andExpect(status().isOk())
                .andExpect(
                        jsonPath("$.items.length()")
                                .value(0)
                )
                .andExpect(
                        jsonPath("$.nextCursor")
                                .doesNotExist()
                );

        verify(
                eventService
        ).getAll(
                null,
                20,
                null
        );
    }

    @Test
    void getAll_shouldReturn400_whenInvalidStatus()
            throws Exception {

        mockMvc.perform(
                        get(EVENTS_URL)
                                .param(
                                        "status",
                                        "INVALID"
                                )
                )
                .andExpect(status().isBadRequest());

        verify(
                eventService,
                never()
        ).getAll(
                any(),
                any(Integer.class),
                any()
        );
    }

    @Test
    void getAll_shouldReturn400_whenLimitIsZero()
            throws Exception {

        mockMvc.perform(
                        get(EVENTS_URL)
                                .param("limit", "0")
                )
                .andExpect(status().isBadRequest())
                .andExpect(
                        jsonPath("$.status")
                                .value(400)
                )
                .andExpect(
                        jsonPath("$.message")
                                .value("Validation failed")
                );

        verify(
                eventService,
                never()
        ).getAll(
                any(),
                any(Integer.class),
                any()
        );
    }

    @Test
    void getAll_shouldReturn400_whenLimitExceedsMaximum()
            throws Exception {

        mockMvc.perform(
                        get(EVENTS_URL)
                                .param("limit", "101")
                )
                .andExpect(status().isBadRequest())
                .andExpect(
                        jsonPath("$.status")
                                .value(400)
                )
                .andExpect(
                        jsonPath("$.message")
                                .value("Validation failed")
                );

        verify(
                eventService,
                never()
        ).getAll(
                any(),
                any(Integer.class),
                any()
        );
    }

    @Test
    void getAll_shouldReturn400_whenLimitIsNotANumber()
            throws Exception {

        mockMvc.perform(
                        get(EVENTS_URL)
                                .param("limit", "abc")
                )
                .andExpect(status().isBadRequest());

        verify(
                eventService,
                never()
        ).getAll(
                any(),
                any(Integer.class),
                any()
        );
    }

    @Test
    void getAll_shouldReturn500_whenServiceFails()
            throws Exception {

        when(
                eventService.getAll(
                        null,
                        20,
                        null
                )
        ).thenThrow(
                new RuntimeException(
                        "Database failure"
                )
        );

        mockMvc.perform(
                        get(EVENTS_URL)
                )
                .andExpect(status().isInternalServerError())
                .andExpect(
                        jsonPath("$.status")
                                .value(500)
                )
                .andExpect(
                        jsonPath("$.error")
                                .value("Internal Server Error")
                )
                .andExpect(
                        jsonPath("$.message")
                                .value(
                                        "An unexpected error occurred."
                                )
                );

        verify(
                eventService
        ).getAll(
                null,
                20,
                null
        );
    }

    @Test
    void getAll_shouldReturn200_whenCursorPointsToLastPage()
            throws Exception {

        when(
                eventService.getAll(
                        null,
                        20,
                        PREVIOUS_CURSOR
                )
        ).thenReturn(emptyPageResponse());

        mockMvc.perform(
                        get(EVENTS_URL)
                                .param(
                                        "after",
                                        PREVIOUS_CURSOR
                                )
                )
                .andExpect(status().isOk())
                .andExpect(
                        jsonPath("$.items.length()")
                                .value(0)
                )
                .andExpect(
                        jsonPath("$.nextCursor")
                                .doesNotExist()
                );

        verify(
                eventService
        ).getAll(
                null,
                20,
                PREVIOUS_CURSOR
        );
    }

    @Test
    void getAll_shouldReturn400_whenCursorIsInvalid()
            throws Exception {

        String invalidCursor = "garbage";

        when(
                eventService.getAll(
                        null,
                        20,
                        invalidCursor
                )
        ).thenThrow(
                new CursorDecodingException(
                        "Invalid pagination cursor."
                )
        );

        mockMvc.perform(
                        get(EVENTS_URL)
                                .param(
                                        "after",
                                        invalidCursor
                                )
                )
                .andExpect(status().isBadRequest())
                .andExpect(
                        jsonPath("$.status")
                                .value(400)
                )
                .andExpect(
                        jsonPath("$.error")
                                .value("Bad Request")
                )
                .andExpect(
                        jsonPath("$.message")
                                .value(
                                        "Invalid pagination cursor."
                                )
                );

        verify(
                eventService
        ).getAll(
                null,
                20,
                invalidCursor
        );
    }

    // =========================================================
    // UPDATE
    // =========================================================

    @Test
    void update_shouldReturn200() throws Exception {

        EventResponse updated =
                response(
                        1L,
                        "Rock Concert",
                        "Live concert 2x",
                        EventStatus.DRAFT
                );

        when(
                eventService.update(
                        eq(EVENT_ID),
                        any()
                )
        ).thenReturn(updated);

        mockMvc.perform(
                        put(EVENT_URL)
                                .contentType(
                                        MediaType.APPLICATION_JSON
                                )
                                .content(UPDATED_REQUEST)
                )
                .andExpect(status().isOk())
                .andExpect(
                        jsonPath("$.id")
                                .value(EVENT_ID.toString())
                )
                .andExpect(
                        jsonPath("$.version")
                                .value(1)
                )
                .andExpect(
                        jsonPath("$.description")
                                .value("Live concert 2x")
                )
                .andExpect(
                        jsonPath("$.scheduledAt")
                                .value("2026-12-20T19:00:00Z")
                )
                .andExpect(
                        jsonPath("$.numberOfRows")
                                .value(DEFAULT_NUMBER_OF_ROWS)
                )
                .andExpect(
                        jsonPath("$.status")
                                .value("DRAFT")
                );

        verify(
                eventService
        ).update(
                eq(EVENT_ID),
                any()
        );
    }

    @Test
    void update_shouldReturn400_whenValidationFails()
            throws Exception {

        mockMvc.perform(
                        put(EVENT_URL)
                                .contentType(
                                        MediaType.APPLICATION_JSON
                                )
                                .content(INVALID_REQUEST)
                )
                .andExpect(status().isBadRequest())
                .andExpect(
                        jsonPath("$.status")
                                .value(400)
                )
                .andExpect(
                        jsonPath("$.message")
                                .value("Validation failed")
                );

        verify(
                eventService,
                never()
        ).update(
                eq(EVENT_ID),
                any()
        );
    }

    @Test
    void update_shouldReturn404_whenEventDoesNotExist()
            throws Exception {

        when(
                eventService.update(
                        eq(EVENT_ID),
                        any()
                )
        ).thenThrow(
                new EventNotFoundException(EVENT_ID)
        );

        mockMvc.perform(
                        put(EVENT_URL)
                                .contentType(
                                        MediaType.APPLICATION_JSON
                                )
                                .content(VALID_REQUEST)
                )
                .andExpect(status().isNotFound())
                .andExpect(
                        jsonPath("$.status")
                                .value(404)
                )
                .andExpect(
                        jsonPath("$.error")
                                .value("Not Found")
                );

        verify(
                eventService
        ).update(
                eq(EVENT_ID),
                any()
        );
    }

    @Test
    void update_shouldReturn409_whenEventStateIsInvalid()
            throws Exception {

        String message =
                "Completed events cannot be modified";

        when(
                eventService.update(
                        eq(EVENT_ID),
                        any()
                )
        ).thenThrow(
                new InvalidEventStateException(message)
        );

        mockMvc.perform(
                        put(EVENT_URL)
                                .contentType(
                                        MediaType.APPLICATION_JSON
                                )
                                .content(VALID_REQUEST)
                )
                .andExpect(status().isConflict())
                .andExpect(
                        jsonPath("$.status")
                                .value(409)
                )
                .andExpect(
                        jsonPath("$.message")
                                .value(message)
                );

        verify(
                eventService
        ).update(
                eq(EVENT_ID),
                any()
        );
    }

    @Test
    void update_shouldReturn409_whenOptimisticLockingFails()
            throws Exception {

        when(
                eventService.update(
                        eq(EVENT_ID),
                        any()
                )
        ).thenThrow(
                new ObjectOptimisticLockingFailureException(
                        EventEntity.class,
                        EVENT_ID
                )
        );

        mockMvc.perform(
                        put(EVENT_URL)
                                .contentType(
                                        MediaType.APPLICATION_JSON
                                )
                                .content(VALID_REQUEST)
                )
                .andExpect(status().isConflict())
                .andExpect(
                        jsonPath("$.status")
                                .value(409)
                )
                .andExpect(
                        jsonPath("$.message")
                                .value(
                                        "The event was modified by another "
                                                + "request. Please reload the "
                                                + "event and try again."
                                )
                );

        verify(
                eventService
        ).update(
                eq(EVENT_ID),
                any()
        );
    }

    // =========================================================
    // PUBLISH
    // =========================================================

    @Test
    void publish_shouldReturn200() throws Exception {

        EventResponse published =
                response(
                        1L,
                        "Rock Concert",
                        "Live concert",
                        EventStatus.PUBLISHED
                );

        when(eventService.publish(EVENT_ID))
                .thenReturn(published);

        mockMvc.perform(
                        post(EVENT_URL + "/publish")
                )
                .andExpect(status().isOk())
                .andExpect(
                        jsonPath("$.numberOfRows")
                                .value(DEFAULT_NUMBER_OF_ROWS)
                )
                .andExpect(
                        jsonPath("$.status")
                                .value("PUBLISHED")
                );

        verify(eventService).publish(EVENT_ID);
    }

    @Test
    void publish_shouldReturn404_whenEventDoesNotExist()
            throws Exception {

        when(eventService.publish(EVENT_ID))
                .thenThrow(
                        new EventNotFoundException(EVENT_ID)
                );

        mockMvc.perform(
                        post(EVENT_URL + "/publish")
                )
                .andExpect(status().isNotFound());

        verify(eventService).publish(EVENT_ID);
    }

    @Test
    void publish_shouldReturn409_whenEventStateIsInvalid()
            throws Exception {

        String message =
                "Event must be in DRAFT state but is currently PUBLISHED";

        when(eventService.publish(EVENT_ID))
                .thenThrow(
                        new InvalidEventStateException(message)
                );

        mockMvc.perform(
                        post(EVENT_URL + "/publish")
                )
                .andExpect(status().isConflict())
                .andExpect(
                        jsonPath("$.message")
                                .value(message)
                );

        verify(eventService).publish(EVENT_ID);
    }

    // =========================================================
    // CANCEL
    // =========================================================

    @Test
    void cancel_shouldReturn200() throws Exception {

        EventResponse cancelled =
                response(
                        1L,
                        "Rock Concert",
                        "Live concert",
                        EventStatus.CANCELLED
                );

        when(eventService.cancel(EVENT_ID))
                .thenReturn(cancelled);

        mockMvc.perform(
                        post(EVENT_URL + "/cancel")
                )
                .andExpect(status().isOk())
                .andExpect(
                        jsonPath("$.numberOfRows")
                                .value(DEFAULT_NUMBER_OF_ROWS)
                )
                .andExpect(
                        jsonPath("$.status")
                                .value("CANCELLED")
                );

        verify(eventService).cancel(EVENT_ID);
    }

    @Test
    void cancel_shouldReturn404_whenEventDoesNotExist()
            throws Exception {

        when(eventService.cancel(EVENT_ID))
                .thenThrow(
                        new EventNotFoundException(EVENT_ID)
                );

        mockMvc.perform(
                        post(EVENT_URL + "/cancel")
                )
                .andExpect(status().isNotFound());

        verify(eventService).cancel(EVENT_ID);
    }

    @Test
    void cancel_shouldReturn409_whenEventStateIsInvalid()
            throws Exception {

        String message =
                "Completed events cannot be cancelled";

        when(eventService.cancel(EVENT_ID))
                .thenThrow(
                        new InvalidEventStateException(message)
                );

        mockMvc.perform(
                        post(EVENT_URL + "/cancel")
                )
                .andExpect(status().isConflict())
                .andExpect(
                        jsonPath("$.message")
                                .value(message)
                );

        verify(eventService).cancel(EVENT_ID);
    }

    // =========================================================
    // COMPLETE
    // =========================================================

    @Test
    void complete_shouldReturn200() throws Exception {

        EventResponse completed =
                response(
                        2L,
                        "Rock Concert",
                        "Live concert",
                        EventStatus.COMPLETED
                );

        when(eventService.complete(EVENT_ID))
                .thenReturn(completed);

        mockMvc.perform(
                        post(EVENT_URL + "/complete")
                )
                .andExpect(status().isOk())
                .andExpect(
                        jsonPath("$.numberOfRows")
                                .value(DEFAULT_NUMBER_OF_ROWS)
                )
                .andExpect(
                        jsonPath("$.status")
                                .value("COMPLETED")
                );

        verify(eventService).complete(EVENT_ID);
    }

    @Test
    void complete_shouldReturn404_whenEventDoesNotExist()
            throws Exception {

        when(eventService.complete(EVENT_ID))
                .thenThrow(
                        new EventNotFoundException(EVENT_ID)
                );

        mockMvc.perform(
                        post(EVENT_URL + "/complete")
                )
                .andExpect(status().isNotFound());

        verify(eventService).complete(EVENT_ID);
    }

    @Test
    void complete_shouldReturn409_whenEventStateIsInvalid()
            throws Exception {

        String message =
                "Event must be in PUBLISHED state but is currently DRAFT";

        when(eventService.complete(EVENT_ID))
                .thenThrow(
                        new InvalidEventStateException(message)
                );

        mockMvc.perform(
                        post(EVENT_URL + "/complete")
                )
                .andExpect(status().isConflict())
                .andExpect(
                        jsonPath("$.message")
                                .value(message)
                );

        verify(eventService).complete(EVENT_ID);
    }

    // =========================================================
    // DELETE
    // =========================================================

    @Test
    void delete_shouldReturn204() throws Exception {

        mockMvc.perform(
                        delete(EVENT_URL)
                )
                .andExpect(status().isNoContent());

        verify(eventService).delete(EVENT_ID);
    }

    @Test
    void delete_shouldReturn404_whenEventDoesNotExist()
            throws Exception {

        doThrow(
                new EventNotFoundException(EVENT_ID)
        )
                .when(eventService)
                .delete(EVENT_ID);

        mockMvc.perform(
                        delete(EVENT_URL)
                )
                .andExpect(status().isNotFound());

        verify(eventService).delete(EVENT_ID);
    }
}