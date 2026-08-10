package com.bookingeventflow.event.domain.repository;

import com.bookingeventflow.event.domain.model.Event;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface EventRepository {

    Event save(Event event);

    Optional<Event> findById(UUID eventId);

    List<Event> findAll();

    boolean existsById(UUID eventId);

    void delete(Event event);


}
