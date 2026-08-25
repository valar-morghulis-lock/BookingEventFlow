package com.bookingeventflow.reservation.repository;

import com.bookingeventflow.reservation.entity.ProcessedEventEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface ProcessedEventRepository extends JpaRepository<ProcessedEventEntity, UUID> {

    boolean existsById(UUID eventId);
}