package com.bookingeventflow.reservation.repository;

import com.bookingeventflow.reservation.entity.ReservationLineItemEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface ReservationLineItemRepository extends JpaRepository<ReservationLineItemEntity, UUID> {

    List<ReservationLineItemEntity> findByReservationId(UUID reservationId);

    List<ReservationLineItemEntity> findBySeatIdOrderByCreatedAtDesc(UUID seatId);
}