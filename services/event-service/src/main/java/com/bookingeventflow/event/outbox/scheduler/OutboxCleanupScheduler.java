package com.bookingeventflow.event.outbox.scheduler;

import com.bookingeventflow.event.outbox.repository.OutboxEventRepository;
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;

@Component
public class OutboxCleanupScheduler {

    private static final Logger log =
            LoggerFactory.getLogger(OutboxCleanupScheduler.class);

    private static final Duration RETENTION = Duration.ofDays(7);

    private final OutboxEventRepository outboxEventRepository;

    public OutboxCleanupScheduler(
            OutboxEventRepository outboxEventRepository
    ) {
        this.outboxEventRepository = outboxEventRepository;
    }

    @Scheduled(cron = "0 0 3 * * *")
    @SchedulerLock(
            name = "outbox-cleanup",
            lockAtLeastFor = "PT1M",
            lockAtMostFor = "PT10M"
    )
    public void purgeProcessedOutboxEvents() {

        Instant cutoff =
                Instant.now().minus(RETENTION);

        long deleted =
                outboxEventRepository.deleteByCreatedAtBefore(cutoff);

        log.info(
                "Purged {} outbox events older than {}",
                deleted,
                cutoff
        );
    }
}