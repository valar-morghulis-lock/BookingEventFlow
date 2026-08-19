package com.bookingeventflow.event.outbox.service;

import com.bookingeventflow.common.event.DomainEvent;
import com.bookingeventflow.event.outbox.entity.OutboxEventEntity;
import com.bookingeventflow.event.outbox.mapper.OutboxEventMapper;
import com.bookingeventflow.event.outbox.repository.OutboxEventRepository;
import org.springframework.stereotype.Service;

import java.util.Collection;

@Service
public class OutboxService {

    private final OutboxEventRepository outboxEventRepository;
    private final OutboxEventMapper outboxEventMapper;

    public OutboxService(OutboxEventRepository outboxEventRepository, OutboxEventMapper outboxEventMapper) {
        this.outboxEventRepository = outboxEventRepository;
        this.outboxEventMapper = outboxEventMapper;
    }

    public void store(DomainEvent domainEvent) {

        OutboxEventEntity outboxEvent = outboxEventMapper.toEntity(domainEvent);

        outboxEventRepository.save(outboxEvent);
    }

    public void storeAll(
            Collection<? extends DomainEvent> domainEvents
    ) {
        domainEvents.stream()
                .map(outboxEventMapper::toEntity)
                .forEach(outboxEventRepository::save);
    }
}