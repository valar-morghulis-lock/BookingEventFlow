package com.bookingeventflow.event.outbox.repository;

import com.bookingeventflow.event.outbox.entity.OutboxEventEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

public interface OutboxEventRepository
        extends JpaRepository<OutboxEventEntity, UUID> {

    /**
     * Deletes outbox events created before the given cutoff.
     *
     * Used by the scheduled cleanup job to purge events that have
     * already been captured by Debezium's CDC connector, keeping
     * the outbox table from growing unbounded.
     *
     * @return the number of rows deleted
     */
    @Modifying
    @Transactional
    @Query(
            "DELETE FROM OutboxEventEntity e " +
                    "WHERE e.createdAt < :cutoff"
    )
    long deleteByCreatedAtBefore(Instant cutoff);
}