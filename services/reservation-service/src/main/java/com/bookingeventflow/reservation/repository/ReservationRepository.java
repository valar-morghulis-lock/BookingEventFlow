package com.bookingeventflow.reservation.repository;

import com.bookingeventflow.reservation.domain.model.ReservationStatus;
import com.bookingeventflow.reservation.entity.ReservationEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public interface ReservationRepository extends JpaRepository<ReservationEntity, UUID> {

    List<ReservationEntity> findByStatusAndExpiresAtBefore(
            ReservationStatus status,
            Instant cutoff
    );
}