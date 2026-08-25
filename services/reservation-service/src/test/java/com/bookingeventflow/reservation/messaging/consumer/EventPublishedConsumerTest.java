package com.bookingeventflow.reservation.messaging.consumer;

import com.bookingeventflow.reservation.messaging.dto.EventPublishedMessage;
import com.bookingeventflow.reservation.service.SeatCreationService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.UUID;

import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class EventPublishedConsumerTest {

    @Mock
    private SeatCreationService seatCreationService;

    @Test
    void shouldDelegateToSeatCreationService() {

        EventPublishedConsumer consumer =
                new EventPublishedConsumer(seatCreationService);

        EventPublishedMessage message = new EventPublishedMessage(
                UUID.randomUUID(),
                UUID.randomUUID(),
                Instant.now(),
                2,
                10
        );

        consumer.onEventPublished(message);

        verify(seatCreationService).handle(message);
    }
}