package com.bookingeventflow.event.controller;

import com.bookingeventflow.event.presentation.request.CreateEventRequest;
import com.bookingeventflow.event.presentation.request.UpdateEventRequest;
import com.bookingeventflow.event.presentation.response.EventResponse;
import com.bookingeventflow.event.service.EventService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/events")
public class EventController {

    private final EventService eventService;

    public EventController(EventService eventService) {
        this.eventService = eventService;
    }

    @PostMapping
    public ResponseEntity<EventResponse> create(
            @Valid @RequestBody CreateEventRequest request
    ) {
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(eventService.create(request));
    }

    @GetMapping("/{id}")
    public ResponseEntity<EventResponse> getById(
            @PathVariable UUID id
    ) {
        return ResponseEntity.ok(
                eventService.getById(id)
        );
    }

    @GetMapping
    public ResponseEntity<List<EventResponse>> getAll() {
        return ResponseEntity.ok(
                eventService.getAll()
        );
    }

    @PutMapping("/{id}")
    public ResponseEntity<EventResponse> update(
            @PathVariable UUID id,
            @Valid @RequestBody UpdateEventRequest request
    ) {
        return ResponseEntity.ok(
                eventService.update(id, request)
        );
    }

    @PostMapping("/{id}/publish")
    public ResponseEntity<EventResponse> publish(
            @PathVariable UUID id
    ) {
        return ResponseEntity.ok(
                eventService.publish(id)
        );
    }

    @PostMapping("/{id}/cancel")
    public ResponseEntity<EventResponse> cancel(
            @PathVariable UUID id
    ) {
        return ResponseEntity.ok(
                eventService.cancel(id)
        );
    }

    @PostMapping("/{id}/complete")
    public ResponseEntity<EventResponse> complete(
            @PathVariable UUID id
    ) {
        return ResponseEntity.ok(
                eventService.complete(id)
        );
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(
            @PathVariable UUID id
    ) {
        eventService.delete(id);

        return ResponseEntity.noContent().build();
    }
}