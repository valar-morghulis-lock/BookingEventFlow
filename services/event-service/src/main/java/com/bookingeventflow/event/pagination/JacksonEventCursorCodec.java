package com.bookingeventflow.event.pagination;

import com.bookingeventflow.common.pagination.CursorCodec;
import com.bookingeventflow.common.pagination.CursorDecodingException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

public class JacksonEventCursorCodec
        implements CursorCodec<EventCursor> {

    private final ObjectMapper objectMapper;

    public JacksonEventCursorCodec(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    public String encode(EventCursor cursor) {

        try {
            String json = objectMapper.writeValueAsString(cursor);

            return Base64.getUrlEncoder()
                    .withoutPadding()
                    .encodeToString(
                            json.getBytes(StandardCharsets.UTF_8)
                    );

        } catch (JsonProcessingException e) {
            throw new IllegalStateException(
                    "Failed to encode event cursor",
                    e
            );
        }
    }

    @Override
    public EventCursor decode(String value) {

        try {
            byte[] decoded =
                    Base64.getUrlDecoder()
                            .decode(value);

            String json =
                    new String(
                            decoded,
                            StandardCharsets.UTF_8
                    );

            return objectMapper.readValue(
                    json,
                    EventCursor.class
            );

        } catch (IllegalArgumentException |
                 JsonProcessingException e) {

            throw new CursorDecodingException(
                    "Invalid event cursor",
                    e
            );
        }
    }
}