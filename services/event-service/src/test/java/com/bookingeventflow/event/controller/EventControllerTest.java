package com.bookingeventflow.event.controller;

import com.bookingeventflow.event.domain.model.EventStatus;
import com.bookingeventflow.event.entity.EventEntity;
import com.bookingeventflow.event.exception.EventNotFoundException;
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

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private EventService eventService;

    private final UUID eventId = UUID.fromString("512c5d0f-a416-4430-9265-4069b1637964");

    private EventResponse response() {
        return new EventResponse(eventId, 0L, "Rock Concert", "Live concert", Instant.parse("2026-12-20T19:00:00Z"), EventStatus.DRAFT);
    }

    // =========================================================
    // CREATE
    // =========================================================

    @Test
    void create_shouldReturn201() throws Exception {

        when(eventService.create(any())).thenReturn(response());

        mockMvc.perform(post("/api/v1/events").contentType(MediaType.APPLICATION_JSON).content("""
                {
                  "name": "Rock Concert",
                  "description": "Live concert",
                  "scheduledAt": "2026-12-20T19:00:00Z"
                }
                """)).andExpect(status().isCreated()).andExpect(jsonPath("$.id").value(eventId.toString())).andExpect(jsonPath("$.version").value(0)).andExpect(jsonPath("$.name").value("Rock Concert")).andExpect(jsonPath("$.description").value("Live concert")).andExpect(jsonPath("$.status").value("DRAFT"));

        verify(eventService).create(any());
    }

    @Test
    void create_shouldReturn400_whenValidationFails() throws Exception {

        mockMvc.perform(post("/api/v1/events").contentType(MediaType.APPLICATION_JSON).content("""
                        {
                          "name": "",
                          "description": "",
                          "scheduledAt": "2020-01-01T00:00:00Z"
                        }
                        """)).andExpect(status().isBadRequest()).andExpect(jsonPath("$.status").value(400)).andExpect(jsonPath("$.error").value("Bad Request")).andExpect(jsonPath("$.message").value("Validation failed")).andExpect(jsonPath("$.path").value("/api/v1/events"))

                // Map<String, List<String>>
                .andExpect(jsonPath("$.errors.name").isArray()).andExpect(jsonPath("$.errors.name[0]").value("Name must not be blank"))

                .andExpect(jsonPath("$.errors.scheduledAt").isArray()).andExpect(jsonPath("$.errors.scheduledAt[0]").value("Scheduled time must be in the future"));

        verify(eventService, never()).create(any());
    }

    // =========================================================
    // GET BY ID
    // =========================================================

    @Test
    void getById_shouldReturn200() throws Exception {

        when(eventService.getById(eventId)).thenReturn(response());

        mockMvc.perform(get("/api/v1/events/{id}", eventId)).andExpect(status().isOk()).andExpect(jsonPath("$.id").value(eventId.toString())).andExpect(jsonPath("$.version").value(0)).andExpect(jsonPath("$.name").value("Rock Concert")).andExpect(jsonPath("$.status").value("DRAFT"));

        verify(eventService).getById(eventId);
    }

    @Test
    void getById_shouldReturn404_whenMissing() throws Exception {

        when(eventService.getById(eventId)).thenThrow(new EventNotFoundException(eventId));

        mockMvc.perform(get("/api/v1/events/{id}", eventId)).andExpect(status().isNotFound()).andExpect(jsonPath("$.status").value(404)).andExpect(jsonPath("$.error").value("Not Found")).andExpect(jsonPath("$.path").value("/api/v1/events/" + eventId))

                // Empty error map
                .andExpect(jsonPath("$.errors").isEmpty());

        verify(eventService).getById(eventId);
    }

    @Test
    void getById_shouldReturn400_whenIdIsInvalid() throws Exception {

        mockMvc.perform(get("/api/v1/events/{id}", "not-a-uuid")).andExpect(status().isBadRequest());

        verify(eventService, never()).getById(any());
    }

    // =========================================================
    // GET ALL
    // =========================================================

    @Test
    void getAll_shouldReturn200() throws Exception {

        when(eventService.getAll()).thenReturn(List.of(response()));

        mockMvc.perform(get("/api/v1/events")).andExpect(status().isOk()).andExpect(jsonPath("$").isArray()).andExpect(jsonPath("$.length()").value(1)).andExpect(jsonPath("$[0].id").value(eventId.toString())).andExpect(jsonPath("$[0].name").value("Rock Concert")).andExpect(jsonPath("$[0].status").value("DRAFT"));

        verify(eventService).getAll();
    }

    @Test
    void getAll_shouldReturnEmptyList_whenNoEventsExist() throws Exception {

        when(eventService.getAll()).thenReturn(List.of());

        mockMvc.perform(get("/api/v1/events")).andExpect(status().isOk()).andExpect(jsonPath("$").isArray()).andExpect(jsonPath("$.length()").value(0));

        verify(eventService).getAll();
    }

    // =========================================================
    // UPDATE
    // =========================================================

    @Test
    void update_shouldReturn200() throws Exception {

        EventResponse updated = new EventResponse(eventId, 1L, "Rock Concert", "Live concert 2x", Instant.parse("2026-12-20T19:00:00Z"), EventStatus.DRAFT);

        when(eventService.update(eq(eventId), any())).thenReturn(updated);

        mockMvc.perform(put("/api/v1/events/{id}", eventId).contentType(MediaType.APPLICATION_JSON).content("""
                {
                  "name": "Rock Concert",
                  "description": "Live concert 2x",
                  "scheduledAt": "2026-12-20T19:00:00Z"
                }
                """)).andExpect(status().isOk()).andExpect(jsonPath("$.id").value(eventId.toString())).andExpect(jsonPath("$.version").value(1)).andExpect(jsonPath("$.description").value("Live concert 2x")).andExpect(jsonPath("$.status").value("DRAFT"));

        verify(eventService).update(eq(eventId), any());
    }

    @Test
    void update_shouldReturn400_whenValidationFails() throws Exception {

        mockMvc.perform(put("/api/v1/events/{id}", eventId).contentType(MediaType.APPLICATION_JSON).content("""
                        {
                          "name": "",
                          "description": "",
                          "scheduledAt": "2020-01-01T00:00:00Z"
                        }
                        """)).andExpect(status().isBadRequest()).andExpect(jsonPath("$.status").value(400)).andExpect(jsonPath("$.message").value("Validation failed"))

                .andExpect(jsonPath("$.errors.name").isArray()).andExpect(jsonPath("$.errors.name[0]").value("Name must not be blank"))

                .andExpect(jsonPath("$.errors.scheduledAt").isArray()).andExpect(jsonPath("$.errors.scheduledAt[0]").value("Scheduled time must be in the future"));

        verify(eventService, never()).update(eq(eventId), any());
    }

    @Test
    void update_shouldReturn404_whenEventDoesNotExist() throws Exception {

        when(eventService.update(eq(eventId), any())).thenThrow(new EventNotFoundException(eventId));

        mockMvc.perform(put("/api/v1/events/{id}", eventId).contentType(MediaType.APPLICATION_JSON).content("""
                {
                  "name": "Rock Concert",
                  "description": "Live concert",
                  "scheduledAt": "2026-12-20T19:00:00Z"
                }
                """)).andExpect(status().isNotFound()).andExpect(jsonPath("$.status").value(404)).andExpect(jsonPath("$.error").value("Not Found")).andExpect(jsonPath("$.errors").isEmpty());

        verify(eventService).update(eq(eventId), any());
    }

    @Test
    void update_shouldReturn409_whenOptimisticLockingFails() throws Exception {

        when(eventService.update(eq(eventId), any())).thenThrow(new ObjectOptimisticLockingFailureException(EventEntity.class, eventId));

        mockMvc.perform(put("/api/v1/events/{id}", eventId).contentType(MediaType.APPLICATION_JSON).content("""
                {
                  "name": "Rock Concert",
                  "description": "Updated concert",
                  "scheduledAt": "2026-12-20T19:00:00Z"
                }
                """)).andExpect(status().isConflict()).andExpect(jsonPath("$.status").value(409)).andExpect(jsonPath("$.error").value("Conflict")).andExpect(jsonPath("$.message").value("The event was modified by another request. " + "Please reload the event and try again.")).andExpect(jsonPath("$.errors").isEmpty());

        verify(eventService).update(eq(eventId), any());
    }

    // =========================================================
    // PUBLISH
    // =========================================================

    @Test
    void publish_shouldReturn200() throws Exception {

        EventResponse published = new EventResponse(eventId, 1L, "Rock Concert", "Live concert", Instant.parse("2026-12-20T19:00:00Z"), EventStatus.PUBLISHED);

        when(eventService.publish(eventId)).thenReturn(published);

        mockMvc.perform(post("/api/v1/events/{id}/publish", eventId)).andExpect(status().isOk()).andExpect(jsonPath("$.id").value(eventId.toString())).andExpect(jsonPath("$.version").value(1)).andExpect(jsonPath("$.status").value("PUBLISHED"));

        verify(eventService).publish(eventId);
    }

    @Test
    void publish_shouldReturn404_whenEventDoesNotExist() throws Exception {

        when(eventService.publish(eventId)).thenThrow(new EventNotFoundException(eventId));

        mockMvc.perform(post("/api/v1/events/{id}/publish", eventId)).andExpect(status().isNotFound()).andExpect(jsonPath("$.status").value(404)).andExpect(jsonPath("$.error").value("Not Found")).andExpect(jsonPath("$.errors").isEmpty());

        verify(eventService).publish(eventId);
    }

    @Test
    void publish_shouldReturn409_whenEventStateIsInvalid() throws Exception {

        when(eventService.publish(eventId)).thenThrow(new IllegalStateException("Only draft events can be published"));

        mockMvc.perform(post("/api/v1/events/{id}/publish", eventId)).andExpect(status().isConflict()).andExpect(jsonPath("$.status").value(409)).andExpect(jsonPath("$.error").value("Conflict")).andExpect(jsonPath("$.message").value("Only draft events can be published")).andExpect(jsonPath("$.errors").isEmpty());

        verify(eventService).publish(eventId);
    }

    // =========================================================
    // CANCEL
    // =========================================================

    @Test
    void cancel_shouldReturn200() throws Exception {

        EventResponse cancelled = new EventResponse(eventId, 1L, "Rock Concert", "Live concert", Instant.parse("2026-12-20T19:00:00Z"), EventStatus.CANCELLED);

        when(eventService.cancel(eventId)).thenReturn(cancelled);

        mockMvc.perform(post("/api/v1/events/{id}/cancel", eventId)).andExpect(status().isOk()).andExpect(jsonPath("$.id").value(eventId.toString())).andExpect(jsonPath("$.version").value(1)).andExpect(jsonPath("$.status").value("CANCELLED"));

        verify(eventService).cancel(eventId);
    }

    @Test
    void cancel_shouldReturn404_whenEventDoesNotExist() throws Exception {

        when(eventService.cancel(eventId)).thenThrow(new EventNotFoundException(eventId));

        mockMvc.perform(post("/api/v1/events/{id}/cancel", eventId)).andExpect(status().isNotFound()).andExpect(jsonPath("$.status").value(404)).andExpect(jsonPath("$.error").value("Not Found")).andExpect(jsonPath("$.errors").isEmpty());

        verify(eventService).cancel(eventId);
    }

    @Test
    void cancel_shouldReturn409_whenEventStateIsInvalid() throws Exception {

        when(eventService.cancel(eventId)).thenThrow(new IllegalStateException("Event cannot be cancelled"));

        mockMvc.perform(post("/api/v1/events/{id}/cancel", eventId)).andExpect(status().isConflict()).andExpect(jsonPath("$.status").value(409)).andExpect(jsonPath("$.error").value("Conflict")).andExpect(jsonPath("$.message").value("Event cannot be cancelled")).andExpect(jsonPath("$.errors").isEmpty());

        verify(eventService).cancel(eventId);
    }

    // =========================================================
    // COMPLETE
    // =========================================================

    @Test
    void complete_shouldReturn200() throws Exception {

        EventResponse completed = new EventResponse(eventId, 2L, "Rock Concert", "Live concert", Instant.parse("2026-12-20T19:00:00Z"), EventStatus.COMPLETED);

        when(eventService.complete(eventId)).thenReturn(completed);

        mockMvc.perform(post("/api/v1/events/{id}/complete", eventId)).andExpect(status().isOk()).andExpect(jsonPath("$.id").value(eventId.toString())).andExpect(jsonPath("$.version").value(2)).andExpect(jsonPath("$.status").value("COMPLETED"));

        verify(eventService).complete(eventId);
    }

    @Test
    void complete_shouldReturn404_whenEventDoesNotExist() throws Exception {

        when(eventService.complete(eventId)).thenThrow(new EventNotFoundException(eventId));

        mockMvc.perform(post("/api/v1/events/{id}/complete", eventId)).andExpect(status().isNotFound()).andExpect(jsonPath("$.status").value(404)).andExpect(jsonPath("$.error").value("Not Found")).andExpect(jsonPath("$.errors").isEmpty());

        verify(eventService).complete(eventId);
    }

    @Test
    void complete_shouldReturn409_whenEventStateIsInvalid() throws Exception {

        when(eventService.complete(eventId)).thenThrow(new IllegalStateException("Event cannot be completed"));

        mockMvc.perform(post("/api/v1/events/{id}/complete", eventId)).andExpect(status().isConflict()).andExpect(jsonPath("$.status").value(409)).andExpect(jsonPath("$.error").value("Conflict")).andExpect(jsonPath("$.message").value("Event cannot be completed")).andExpect(jsonPath("$.errors").isEmpty());

        verify(eventService).complete(eventId);
    }

    // =========================================================
    // DELETE
    // =========================================================

    @Test
    void delete_shouldReturn204() throws Exception {

        mockMvc.perform(delete("/api/v1/events/{id}", eventId)).andExpect(status().isNoContent());

        verify(eventService).delete(eventId);
    }

    @Test
    void delete_shouldReturn404_whenEventDoesNotExist() throws Exception {

        doThrow(new EventNotFoundException(eventId)).when(eventService).delete(eventId);

        mockMvc.perform(delete("/api/v1/events/{id}", eventId)).andExpect(status().isNotFound()).andExpect(jsonPath("$.status").value(404)).andExpect(jsonPath("$.error").value("Not Found")).andExpect(jsonPath("$.errors").isEmpty());

        verify(eventService).delete(eventId);
    }

    // =========================================================
    // DATA INTEGRITY
    // =========================================================

    @Test
    void create_shouldReturn409_whenDataIntegrityViolationOccurs() throws Exception {

        when(eventService.create(any())).thenThrow(new DataIntegrityViolationException("Duplicate event"));

        mockMvc.perform(post("/api/v1/events").contentType(MediaType.APPLICATION_JSON).content("""
                {
                  "name": "Rock Concert",
                  "description": "Live concert",
                  "scheduledAt": "2026-12-20T19:00:00Z"
                }
                """)).andExpect(status().isConflict()).andExpect(jsonPath("$.status").value(409)).andExpect(jsonPath("$.error").value("Conflict")).andExpect(jsonPath("$.message").value("The request could not be completed " + "because it violates a data constraint.")).andExpect(jsonPath("$.errors").isEmpty());

        verify(eventService).create(any());
    }

    // =========================================================
    // UNEXPECTED EXCEPTION
    // =========================================================

    @Test
    void getById_shouldReturn500_whenUnexpectedExceptionOccurs() throws Exception {

        when(eventService.getById(eventId)).thenThrow(new RuntimeException("Database connection failed"));

        mockMvc.perform(get("/api/v1/events/{id}", eventId)).andExpect(status().isInternalServerError()).andExpect(jsonPath("$.status").value(500)).andExpect(jsonPath("$.error").value("Internal Server Error")).andExpect(jsonPath("$.message").value("An unexpected error occurred.")).andExpect(jsonPath("$.path").value("/api/v1/events/" + eventId)).andExpect(jsonPath("$.errors").isEmpty());

        verify(eventService).getById(eventId);
    }
}