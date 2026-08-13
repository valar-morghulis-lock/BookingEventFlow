package com.bookingeventflow.event.service;

import com.bookingeventflow.event.entity.EventEntity;
import com.bookingeventflow.event.repository.EventRepository;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Component
class EventConcurrencyTestHelper {

    private final EventRepository eventRepository;

    EventConcurrencyTestHelper(
            EventRepository eventRepository
    ) {
        this.eventRepository = eventRepository;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public EventEntity load(UUID id) {

        return eventRepository.findById(id)
                .orElseThrow();
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void update(
            EventEntity entity,
            String name
    ) {
        entity.setName(name);

        eventRepository.saveAndFlush(entity);
    }
}