package com.bookingeventflow.reservation.repository;

import com.bookingeventflow.reservation.entity.ReservationInventoryEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface ReservationInventoryRepository
        extends JpaRepository<ReservationInventoryEntity, UUID> {

    boolean existsByEventId(UUID eventId);
    long countByEventId(UUID eventId);


}