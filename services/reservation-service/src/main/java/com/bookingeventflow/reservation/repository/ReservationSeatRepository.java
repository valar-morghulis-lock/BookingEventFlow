package com.bookingeventflow.reservation.repository;

import com.bookingeventflow.reservation.domain.model.SeatStatus;
import com.bookingeventflow.reservation.entity.ReservationSeatEntity;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

public interface ReservationSeatRepository
        extends JpaRepository<ReservationSeatEntity, UUID> {
    long countByEventId(UUID eventId);

    boolean existsByEventId(UUID eventId);

    List<ReservationSeatEntity> findByEventIdOrderByRowNumberAscSeatNumberInRowAsc(UUID eventId);

    List<ReservationSeatEntity> findByEventIdAndStatusOrderByRowNumberAscSeatNumberInRowAsc(
            UUID eventId,
            SeatStatus status
    );

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query(
            "SELECT s FROM ReservationSeatEntity s " +
                    "WHERE s.eventId = :eventId " +
                    "AND s.seatNumber IN :seatNumbers"
    )
    List<ReservationSeatEntity> lockSeatsForUpdate(
            @Param("eventId") UUID eventId,
            @Param("seatNumbers") List<String> seatNumbers
    );

}