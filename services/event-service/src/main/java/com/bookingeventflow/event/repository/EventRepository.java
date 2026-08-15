package com.bookingeventflow.event.repository;

import com.bookingeventflow.event.domain.model.EventStatus;
import com.bookingeventflow.event.entity.EventEntity;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public interface EventRepository extends JpaRepository<EventEntity, UUID> {

    @Query("""
            SELECT e
            FROM EventEntity e
            ORDER BY e.scheduledAt ASC, e.id ASC
            """)
    List<EventEntity> findFirstKeysetPage(
            Pageable pageable
    );

    @Query("""
            SELECT e
            FROM EventEntity e
            WHERE e.status = :status
            ORDER BY e.scheduledAt ASC, e.id ASC
            """)
    List<EventEntity> findFirstKeysetPageByStatus(
            @Param("status") EventStatus status,
            Pageable pageable
    );

    @Query("""
            SELECT e
            FROM EventEntity e
            WHERE
                e.scheduledAt > :scheduledAt
                OR (
                    e.scheduledAt = :scheduledAt
                    AND e.id > :eventId
                )
            ORDER BY e.scheduledAt ASC, e.id ASC
            """)
    List<EventEntity> findNextKeysetPage(
            @Param("scheduledAt") Instant scheduledAt,
            @Param("eventId") UUID eventId,
            Pageable pageable
    );

    @Query("""
            SELECT e
            FROM EventEntity e
            WHERE
                e.status = :status
                AND (
                    e.scheduledAt > :scheduledAt
                    OR (
                        e.scheduledAt = :scheduledAt
                        AND e.id > :eventId
                    )
                )
            ORDER BY e.scheduledAt ASC, e.id ASC
            """)
    List<EventEntity> findNextKeysetPageByStatus(
            @Param("status") EventStatus status,
            @Param("scheduledAt") Instant scheduledAt,
            @Param("eventId") UUID eventId,
            Pageable pageable
    );
}