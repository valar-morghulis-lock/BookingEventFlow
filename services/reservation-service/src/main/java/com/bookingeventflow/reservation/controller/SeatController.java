package com.bookingeventflow.reservation.controller;

import com.bookingeventflow.reservation.domain.model.SeatStatus;
import com.bookingeventflow.reservation.presentation.response.SeatResponse;
import com.bookingeventflow.reservation.service.SeatQueryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/events/{eventId}/seats")
@Tag(name = "Seats", description = "Seat inventory queries for an event.")
public class SeatController {

    private final SeatQueryService seatQueryService;

    public SeatController(SeatQueryService seatQueryService) {
        this.seatQueryService = seatQueryService;
    }

    @Operation(
            summary = "List seats for an event",
            description = "Returns all seats for the event, optionally filtered by status."
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Seats returned",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            array = @ArraySchema(schema = @Schema(implementation = SeatResponse.class))
                    )
            )
    })
    @GetMapping
    public ResponseEntity<List<SeatResponse>> getSeats(
            @PathVariable UUID eventId,
            @Parameter(description = "Optional status filter, e.g. AVAILABLE, HELD, BOOKED")
            @RequestParam(required = false) SeatStatus status
    ) {
        return ResponseEntity.ok(seatQueryService.getSeatsForEvent(eventId, status));
    }
}