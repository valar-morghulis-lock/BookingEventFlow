package com.bookingeventflow.event.outbox.mapper;

import com.bookingeventflow.common.event.DomainEvent;
import com.bookingeventflow.event.outbox.entity.OutboxEventEntity;

public interface OutboxEventMapper {

    OutboxEventEntity toEntity(
            DomainEvent domainEvent
    );
}