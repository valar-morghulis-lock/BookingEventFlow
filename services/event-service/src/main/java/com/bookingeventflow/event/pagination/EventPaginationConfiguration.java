package com.bookingeventflow.event.pagination;

import com.bookingeventflow.common.pagination.CursorCodec;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class EventPaginationConfiguration {

    @Bean
    public CursorCodec<EventCursor> eventCursorCodec(
            ObjectMapper objectMapper
    ) {
        return new JacksonEventCursorCodec(objectMapper);
    }
}