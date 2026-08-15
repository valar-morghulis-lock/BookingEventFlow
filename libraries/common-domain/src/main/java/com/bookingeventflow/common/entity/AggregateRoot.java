package com.bookingeventflow.common.entity;

import com.bookingeventflow.common.event.DomainEvent;

import java.util.ArrayList;
import java.util.List;

public abstract class AggregateRoot extends AuditableEntity {

    private final List<DomainEvent> domainEvents = new ArrayList<>();

    protected void registerEvent(DomainEvent event) {
        domainEvents.add(event);
    }

    public List<DomainEvent> domainEvents() {
        return List.copyOf(domainEvents);
    }

    public void clearDomainEvents() {
        domainEvents.clear();
    }

}

