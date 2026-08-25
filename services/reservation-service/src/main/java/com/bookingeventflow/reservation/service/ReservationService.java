package com.bookingeventflow.reservation.service;

import com.bookingeventflow.reservation.domain.model.ReservationStatus;
import com.bookingeventflow.reservation.domain.model.SeatStatus;
import com.bookingeventflow.reservation.entity.ReservationEntity;
import com.bookingeventflow.reservation.entity.ReservationLineItemEntity;
import com.bookingeventflow.reservation.entity.ReservationSeatEntity;
import com.bookingeventflow.reservation.exception.InvalidReservationStateException;
import com.bookingeventflow.reservation.exception.ReservationNotFoundException;
import com.bookingeventflow.reservation.exception.SeatNotFoundException;
import com.bookingeventflow.reservation.exception.SeatUnavailableException;
import com.bookingeventflow.reservation.observability.metrics.ReservationMetrics;
import com.bookingeventflow.reservation.presentation.request.CreateReservationRequest;
import com.bookingeventflow.reservation.presentation.response.ReservationResponse;
import com.bookingeventflow.reservation.repository.ReservationLineItemRepository;
import com.bookingeventflow.reservation.repository.ReservationRepository;
import com.bookingeventflow.reservation.repository.ReservationSeatRepository;
import io.micrometer.core.annotation.Timed;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class ReservationService {

    private static final Logger log =
            LoggerFactory.getLogger(ReservationService.class);

    private static final String OP_CREATE = "create_reservation";
    private static final String OP_CONFIRM = "confirm_reservation";
    private static final String OP_RELEASE = "release_reservation";
    private static final String OP_EXPIRE = "expire_reservation";
    private static final String OP_GET = "get_reservation";

    private final ReservationRepository reservationRepository;
    private final ReservationLineItemRepository reservationLineItemRepository;
    private final ReservationSeatRepository reservationSeatRepository;
    private final ReservationMetrics reservationMetrics;
    private final Duration holdDuration;

    public ReservationService(
            ReservationRepository reservationRepository,
            ReservationLineItemRepository reservationLineItemRepository,
            ReservationSeatRepository reservationSeatRepository,
            ReservationMetrics reservationMetrics,
            @Value("${reservation.hold-duration-minutes:10}") long holdDurationMinutes
    ) {
        this.reservationRepository = reservationRepository;
        this.reservationLineItemRepository = reservationLineItemRepository;
        this.reservationSeatRepository = reservationSeatRepository;
        this.reservationMetrics = reservationMetrics;
        this.holdDuration = Duration.ofMinutes(holdDurationMinutes);
    }

    @Timed(
            value = "reservation.create.duration",
            description = "Reservation creation (seat hold) duration"
    )
    @Transactional
    public ReservationResponse createReservation(
            UUID eventId,
            CreateReservationRequest request
    ) {

        List<String> requestedSeatNumbers = request.seatNumbers();

        List<ReservationSeatEntity> lockedSeats =
                reservationSeatRepository.lockSeatsForUpdate(
                        eventId,
                        requestedSeatNumbers
                );

        validateAllSeatsExist(requestedSeatNumbers, lockedSeats);
        validateAllSeatsAvailable(lockedSeats);

        Instant expiresAt = Instant.now().plus(holdDuration);

        ReservationEntity reservation =
                reservationRepository.save(
                        new ReservationEntity(
                                eventId,
                                request.customerId(),
                                expiresAt
                        )
                );

        List<ReservationLineItemEntity> lineItems =
                new ArrayList<>(lockedSeats.size());

        for (ReservationSeatEntity seat : lockedSeats) {

            seat.setStatus(SeatStatus.HELD);

            lineItems.add(
                    new ReservationLineItemEntity(
                            reservation.id(),
                            seat.id(),
                            eventId,
                            seat.getSeatNumber()
                    )
            );
        }

        reservationSeatRepository.saveAll(lockedSeats);
        reservationLineItemRepository.saveAll(lineItems);

        reservationMetrics.recordOperation(OP_CREATE, ReservationMetrics.Result.SUCCESS);

        log.info(
                "Created reservation {} with {} seats for customer {}",
                reservation.id(),
                lineItems.size(),
                request.customerId()
        );

        return toResponse(reservation, lockedSeats);
    }

    @Timed(
            value = "reservation.confirm.duration",
            description = "Reservation confirmation duration"
    )
    @Transactional
    public ReservationResponse confirmReservation(UUID reservationId) {

        ReservationEntity reservation = findReservationOrThrow(reservationId, OP_CONFIRM);

        if (reservation.getStatus() != ReservationStatus.PENDING) {

            reservationMetrics.recordOperation(OP_CONFIRM, ReservationMetrics.Result.INVALID_STATE);

            throw new InvalidReservationStateException(
                    reservationId,
                    reservation.getStatus()
            );
        }

        List<ReservationSeatEntity> seats = loadSeatsForReservation(reservationId);

        seats.forEach(seat -> seat.setStatus(SeatStatus.BOOKED));
        reservationSeatRepository.saveAll(seats);

        reservation.setStatus(ReservationStatus.CONFIRMED);
        reservation.setExpiresAt(null);
        reservationRepository.save(reservation);

        reservationMetrics.recordOperation(OP_CONFIRM, ReservationMetrics.Result.SUCCESS);

        log.info("Confirmed reservation {}", reservationId);

        return toResponse(reservation, seats);
    }

    @Timed(
            value = "reservation.release.duration",
            description = "Reservation release duration"
    )
    @Transactional
    public ReservationResponse releaseReservation(UUID reservationId) {

        ReservationEntity reservation = findReservationOrThrow(reservationId, OP_RELEASE);

        if (reservation.getStatus() != ReservationStatus.PENDING) {

            reservationMetrics.recordOperation(OP_RELEASE, ReservationMetrics.Result.INVALID_STATE);

            throw new InvalidReservationStateException(
                    reservationId,
                    reservation.getStatus()
            );
        }

        List<ReservationSeatEntity> seats = loadSeatsForReservation(reservationId);

        seats.forEach(seat -> seat.setStatus(SeatStatus.AVAILABLE));
        reservationSeatRepository.saveAll(seats);

        reservation.setStatus(ReservationStatus.RELEASED);
        reservation.setExpiresAt(null);
        reservationRepository.save(reservation);

        reservationMetrics.recordOperation(OP_RELEASE, ReservationMetrics.Result.SUCCESS);

        log.info("Released reservation {}", reservationId);

        return toResponse(reservation, seats);
    }

    /**
     * Expires a single PENDING reservation past its hold window,
     * releasing its seats back to AVAILABLE. Silently no-ops if the
     * reservation no longer exists or is no longer PENDING (e.g. it
     * was confirmed/released concurrently with the sweep).
     */
    @Timed(
            value = "reservation.expire.duration",
            description = "Reservation expiry duration"
    )
    @Transactional
    public void expireReservation(UUID reservationId) {

        ReservationEntity reservation =
                reservationRepository.findById(reservationId).orElse(null);

        if (reservation == null || reservation.getStatus() != ReservationStatus.PENDING) {
            return;
        }

        List<ReservationSeatEntity> seats = loadSeatsForReservation(reservationId);

        seats.forEach(seat -> seat.setStatus(SeatStatus.AVAILABLE));
        reservationSeatRepository.saveAll(seats);

        reservation.setStatus(ReservationStatus.EXPIRED);
        reservationRepository.save(reservation);

        reservationMetrics.recordOperation(OP_EXPIRE, ReservationMetrics.Result.SUCCESS);

        log.info("Expired reservation {}", reservationId);
    }

    private ReservationEntity findReservationOrThrow(UUID reservationId, String operation) {

        return reservationRepository.findById(reservationId)
                .orElseThrow(() -> {

                    reservationMetrics.recordOperation(operation, ReservationMetrics.Result.NOT_FOUND);

                    return new ReservationNotFoundException(reservationId);
                });
    }

    private List<ReservationSeatEntity> loadSeatsForReservation(UUID reservationId) {

        List<UUID> seatIds =
                reservationLineItemRepository.findByReservationId(reservationId)
                        .stream()
                        .map(ReservationLineItemEntity::getSeatId)
                        .toList();

        return reservationSeatRepository.findAllById(seatIds);
    }

    private void validateAllSeatsExist(
            List<String> requested,
            List<ReservationSeatEntity> found
    ) {

        if (found.size() == requested.size()) {
            return;
        }

        Set<String> foundNumbers =
                found.stream()
                        .map(ReservationSeatEntity::getSeatNumber)
                        .collect(Collectors.toSet());

        List<String> missing =
                requested.stream()
                        .filter(number -> !foundNumbers.contains(number))
                        .toList();

        reservationMetrics.recordOperation(OP_CREATE, ReservationMetrics.Result.NOT_FOUND);

        throw new SeatNotFoundException(missing);
    }

    private void validateAllSeatsAvailable(List<ReservationSeatEntity> seats) {

        List<String> unavailable =
                seats.stream()
                        .filter(seat -> seat.getStatus() != SeatStatus.AVAILABLE)
                        .map(ReservationSeatEntity::getSeatNumber)
                        .toList();

        if (unavailable.isEmpty()) {
            return;
        }

        reservationMetrics.recordOperation(OP_CREATE, ReservationMetrics.Result.SEAT_UNAVAILABLE);

        throw new SeatUnavailableException(unavailable);
    }

    private ReservationResponse toResponse(
            ReservationEntity reservation,
            List<ReservationSeatEntity> seats
    ) {

        List<String> seatNumbers =
                seats.stream()
                        .map(ReservationSeatEntity::getSeatNumber)
                        .sorted()
                        .toList();

        return new ReservationResponse(
                reservation.id(),
                reservation.getEventId(),
                reservation.getStatus(),
                reservation.getExpiresAt(),
                seatNumbers
        );
    }


    @Timed(
            value = "reservation.get.duration",
            description = "Reservation retrieval duration"
    )
    @Transactional(readOnly = true)
    public ReservationResponse getReservation(UUID reservationId) {

        ReservationEntity reservation = findReservationOrThrow(reservationId, OP_GET);

        List<ReservationSeatEntity> seats = loadSeatsForReservation(reservationId);

        return toResponse(reservation, seats);
    }
}