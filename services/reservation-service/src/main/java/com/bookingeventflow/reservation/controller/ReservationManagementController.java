package com.bookingeventflow.reservation.controller;

import com.bookingeventflow.reservation.exception.ErrorResponse;
import com.bookingeventflow.reservation.presentation.response.ReservationResponse;
import com.bookingeventflow.reservation.service.ReservationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/reservations")
@Tag(name = "Reservations", description = "Reservation lifecycle operations.")
public class ReservationManagementController {

    private final ReservationService reservationService;

    public ReservationManagementController(ReservationService reservationService) {
        this.reservationService = reservationService;
    }

    @Operation(summary = "Confirm a pending reservation")
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Reservation confirmed",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = ReservationResponse.class)
                    )
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "Reservation not found",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = ErrorResponse.class)
                    )
            ),
            @ApiResponse(
                    responseCode = "409",
                    description = "Reservation is not in a confirmable state",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = ErrorResponse.class)
                    )
            )
    })
    @PostMapping("/{reservationId}/confirm")
    public ResponseEntity<ReservationResponse> confirm(@PathVariable UUID reservationId) {
        return ResponseEntity.ok(reservationService.confirmReservation(reservationId));
    }

    @Operation(summary = "Release a pending reservation's held seats")
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Reservation released",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = ReservationResponse.class)
                    )
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "Reservation not found",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = ErrorResponse.class)
                    )
            ),
            @ApiResponse(
                    responseCode = "409",
                    description = "Reservation is not in a releasable state",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = ErrorResponse.class)
                    )
            )
    })
    @PostMapping("/{reservationId}/release")
    public ResponseEntity<ReservationResponse> release(@PathVariable UUID reservationId) {
        return ResponseEntity.ok(reservationService.releaseReservation(reservationId));
    }


    @Operation(summary = "Get a reservation by id")
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Reservation found",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = ReservationResponse.class)
                    )
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "Reservation not found",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = ErrorResponse.class)
                    )
            )
    })
    @GetMapping("/{reservationId}")
    public ResponseEntity<ReservationResponse> get(@PathVariable UUID reservationId) {
        return ResponseEntity.ok(reservationService.getReservation(reservationId));
    }
}