package com.bookingeventflow.reservation.service;

import com.bookingeventflow.reservation.domain.model.SeatStatus;
import com.bookingeventflow.reservation.entity.ReservationSeatEntity;
import com.bookingeventflow.reservation.presentation.response.SeatResponse;
import com.bookingeventflow.reservation.repository.ReservationSeatRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
public class SeatQueryService {

    private final ReservationSeatRepository reservationSeatRepository;

    public SeatQueryService(ReservationSeatRepository reservationSeatRepository) {
        this.reservationSeatRepository = reservationSeatRepository;
    }

    @Transactional(readOnly = true)
    public List<SeatResponse> getSeatsForEvent(UUID eventId, SeatStatus status) {

        List<ReservationSeatEntity> seats = status == null
                ? reservationSeatRepository.findByEventIdOrderByRowNumberAscSeatNumberInRowAsc(eventId)
                : reservationSeatRepository.findByEventIdAndStatusOrderByRowNumberAscSeatNumberInRowAsc(eventId, status);

        return seats.stream()
                .map(this::toResponse)
                .toList();
    }

    private SeatResponse toResponse(ReservationSeatEntity seat) {
        return new SeatResponse(
                seat.getSeatNumber(),
                seat.getRowNumber(),
                seat.getSeatNumberInRow(),
                seat.getStatus()
        );
    }
}