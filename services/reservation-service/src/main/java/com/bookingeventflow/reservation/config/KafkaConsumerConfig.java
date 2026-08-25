package com.bookingeventflow.reservation.config;

import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.listener.DefaultErrorHandler;
import org.springframework.kafka.listener.ConsumerRecordRecoverer;
import org.springframework.util.backoff.FixedBackOff;

@Configuration
public class KafkaConsumerConfig {

    /**
     * Retries a failed message up to 3 times with a 2s delay, then
     * logs and gives up (moves past the poison message) rather than
     * blocking the partition indefinitely.
     */
    @Bean
    public DefaultErrorHandler errorHandler() {

        ConsumerRecordRecoverer recoverer = (record, exception) ->
                org.slf4j.LoggerFactory
                        .getLogger(KafkaConsumerConfig.class)
                        .error(
                                "Giving up on record after retries: topic={}, partition={}, offset={}",
                                record.topic(),
                                record.partition(),
                                record.offset(),
                                exception
                        );

        return new DefaultErrorHandler(
                recoverer,
                new FixedBackOff(2000L, 3)
        );
    }
}