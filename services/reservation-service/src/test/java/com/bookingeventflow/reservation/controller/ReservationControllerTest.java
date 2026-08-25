package com.bookingeventflow.reservation.controller;

import com.bookingeventflow.reservation.domain.model.ReservationStatus;
import com.bookingeventflow.reservation.exception.GlobalExceptionHandler;
import com.bookingeventflow.reservation.exception.SeatUnavailableException;
import com.bookingeventflow.reservation.presentation.response.ReservationResponse;
import com.bookingeventflow.reservation.service.ReservationService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(ReservationController.class)
@org.springframework.context.annotation.Import(GlobalExceptionHandler.class)
class ReservationControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private ReservationService reservationService;

    private static final UUID EVENT_ID = UUID.randomUUID();

    @Test
    void create_shouldReturn201_onSuccess() throws Exception {

        UUID reservationId = UUID.randomUUID();

        when(reservationService.createReservation(eq(EVENT_ID), any()))
                .thenReturn(new ReservationResponse(
                        reservationId,
                        EVENT_ID,
                        ReservationStatus.PENDING,
                        Instant.now(),
                        List.of("R001-S01")
                ));

        mockMvc.perform(
                        post("/api/v1/events/" + EVENT_ID + "/reservations")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                                        {"customerId": "%s", "seatNumbers": ["R001-S01"]}
                                        """.formatted(UUID.randomUUID()))
                )
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("PENDING"));
    }

    @Test
    void create_shouldReturn400_whenSeatNumbersEmpty() throws Exception {

        mockMvc.perform(
                        post("/api/v1/events/" + EVENT_ID + "/reservations")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                                        {"customerId": "%s", "seatNumbers": []}
                                        """.formatted(UUID.randomUUID()))
                )
                .andExpect(status().isBadRequest());
    }

    @Test
    void create_shouldReturn409_whenSeatUnavailable() throws Exception {

        when(reservationService.createReservation(eq(EVENT_ID), any()))
                .thenThrow(new SeatUnavailableException(List.of("R001-S01")));

        mockMvc.perform(
                        post("/api/v1/events/" + EVENT_ID + "/reservations")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                                        {"customerId": "%s", "seatNumbers": ["R001-S01"]}
                                        """.formatted(UUID.randomUUID()))
                )
                .andExpect(status().isConflict());
    }
}