package com.bookingeventflow.event.controller;

import com.bookingeventflow.event.domain.model.EventStatus;
import com.bookingeventflow.event.exception.ErrorResponse;
import com.bookingeventflow.event.presentation.request.CreateEventRequest;
import com.bookingeventflow.event.presentation.request.UpdateEventRequest;
import com.bookingeventflow.event.presentation.response.EventPageResponse;
import com.bookingeventflow.event.presentation.response.EventResponse;
import com.bookingeventflow.event.service.EventService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/events")
@Validated
@Tag(
        name = "Events",
        description = "Event management and discovery operations"
)
public class EventController {

    private final EventService eventService;

    public EventController(EventService eventService) {
        this.eventService = eventService;
    }

    // =========================================================
    // CREATE
    // =========================================================

    @Operation(
            summary = "Create an event",
            description = "Creates a new event."
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "201",
                    description = "Event created successfully",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(
                                    implementation = EventResponse.class
                            )
                    )
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Invalid request",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(
                                    implementation = ErrorResponse.class
                            )
                    )
            )
    })
    @PostMapping
    public ResponseEntity<EventResponse> create(
            @Valid @RequestBody CreateEventRequest request
    ) {

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(eventService.create(request));
    }

    // =========================================================
    // GET BY ID
    // =========================================================

    @Operation(
            summary = "Get an event",
            description = "Retrieves an event by its unique identifier."
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Event retrieved successfully",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(
                                    implementation = EventResponse.class
                            )
                    )
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "Event not found",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(
                                    implementation = ErrorResponse.class
                            )
                    )
            )
    })
    @GetMapping("/{id}")
    public ResponseEntity<EventResponse> getById(
            @Parameter(
                    description = "Unique identifier of the event",
                    required = true
            )
            @PathVariable UUID id
    ) {

        return ResponseEntity.ok(
                eventService.getById(id)
        );
    }

    // =========================================================
    // GET ALL - KEYSET PAGINATION
    // =========================================================

    @Operation(
            summary = "Get events",
            description = """
                    Retrieves events using keyset (cursor-based) pagination.

                    Results are ordered by scheduledAt ascending and event ID
                    ascending.

                    An optional status filter can be used to retrieve only
                    events with the requested lifecycle status.

                    The status filter is applied consistently across all
                    pages.

                    The first request does not provide an 'after' cursor.
                    Subsequent requests use the nextCursor returned by the
                    previous response.
                    """
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Events retrieved successfully",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(
                                    implementation = EventPageResponse.class
                            )
                    )
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Invalid pagination or status parameters",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(
                                    implementation = ErrorResponse.class
                            )
                    )
            )
    })
    @GetMapping
    public ResponseEntity<EventPageResponse> getAll(

            @Parameter(
                    description = """
                            Optional event status filter.
                            If omitted, events of all statuses are returned.
                            """,
                    required = false,
                    example = "PUBLISHED"
            )
            @RequestParam(required = false)
            EventStatus status,

            @Parameter(
                    description = "Maximum number of events to return. "
                            + "Must be between 1 and 100.",
                    required = false,
                    example = "20"
            )
            @RequestParam(defaultValue = "20")
            @Min(
                    value = 1,
                    message = "Limit must be at least 1"
            )
            @Max(
                    value = 100,
                    message = "Limit must not exceed 100"
            )
            int limit,

            @Parameter(
                    description = """
                            Cursor returned by the previous request.
                            Omit this parameter when requesting the first page.
                            """,
                    required = false
            )
            @RequestParam(required = false)
            String after
    ) {

        return ResponseEntity.ok(
                eventService.getAll(
                        status,
                        limit,
                        after
                )
        );
    }

    // =========================================================
    // UPDATE
    // =========================================================

    @Operation(
            summary = "Update an event",
            description = "Updates an existing event."
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Event updated successfully",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(
                                    implementation = EventResponse.class
                            )
                    )
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Invalid request",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(
                                    implementation = ErrorResponse.class
                            )
                    )
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "Event not found",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(
                                    implementation = ErrorResponse.class
                            )
                    )
            )
    })
    @PutMapping("/{id}")
    public ResponseEntity<EventResponse> update(
            @Parameter(
                    description = "Unique identifier of the event",
                    required = true
            )
            @PathVariable UUID id,

            @Valid @RequestBody UpdateEventRequest request
    ) {

        return ResponseEntity.ok(
                eventService.update(
                        id,
                        request
                )
        );
    }

    // =========================================================
    // PUBLISH
    // =========================================================

    @Operation(
            summary = "Publish an event",
            description = "Publishes an existing event."
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Event published successfully",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(
                                    implementation = EventResponse.class
                            )
                    )
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "Event not found",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(
                                    implementation = ErrorResponse.class
                            )
                    )
            )
    })
    @PostMapping("/{id}/publish")
    public ResponseEntity<EventResponse> publish(
            @PathVariable UUID id
    ) {

        return ResponseEntity.ok(
                eventService.publish(id)
        );
    }

    // =========================================================
    // CANCEL
    // =========================================================

    @Operation(
            summary = "Cancel an event",
            description = "Cancels an existing event."
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Event cancelled successfully",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(
                                    implementation = EventResponse.class
                            )
                    )
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "Event not found",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(
                                    implementation = ErrorResponse.class
                            )
                    )
            )
    })
    @PostMapping("/{id}/cancel")
    public ResponseEntity<EventResponse> cancel(
            @PathVariable UUID id
    ) {

        return ResponseEntity.ok(
                eventService.cancel(id)
        );
    }

    // =========================================================
    // COMPLETE
    // =========================================================

    @Operation(
            summary = "Complete an event",
            description = "Completes an existing event."
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Event completed successfully",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(
                                    implementation = EventResponse.class
                            )
                    )
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "Event not found",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(
                                    implementation = ErrorResponse.class
                            )
                    )
            )
    })
    @PostMapping("/{id}/complete")
    public ResponseEntity<EventResponse> complete(
            @PathVariable UUID id
    ) {

        return ResponseEntity.ok(
                eventService.complete(id)
        );
    }

    // =========================================================
    // DELETE
    // =========================================================

    @Operation(
            summary = "Delete an event",
            description = "Deletes an existing event."
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "204",
                    description = "Event deleted successfully"
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "Event not found",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(
                                    implementation = ErrorResponse.class
                            )
                    )
            )
    })
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(
            @PathVariable UUID id
    ) {

        eventService.delete(id);

        return ResponseEntity.noContent().build();
    }
}