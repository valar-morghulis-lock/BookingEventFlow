package com.bookingeventflow.reservation.messaging.consumer;

import com.bookingeventflow.reservation.messaging.dto.EventPublishedMessage;
import com.bookingeventflow.reservation.service.SeatCreationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
public class EventPublishedConsumer {

    private static final Logger log =
            LoggerFactory.getLogger(EventPublishedConsumer.class);

    private final SeatCreationService seatCreationService;

    public EventPublishedConsumer(SeatCreationService seatCreationService) {
        this.seatCreationService = seatCreationService;
    }

    @KafkaListener(
            topics = "${reservation.kafka.event-published-topic:booking-eventflow.event-service.events}",
            groupId = "${spring.kafka.consumer.group-id}"
    )
    public void onEventPublished(EventPublishedMessage message) {

        log.debug(
                "Received EVENT_PUBLISHED for aggregateId={}",
                message.aggregateId()
        );

        seatCreationService.handle(message);
    }
}