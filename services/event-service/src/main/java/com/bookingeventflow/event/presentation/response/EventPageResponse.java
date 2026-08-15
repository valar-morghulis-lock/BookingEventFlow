package com.bookingeventflow.event.presentation.response;

import java.util.List;

public record EventPageResponse(
        List<EventResponse> items,
        String nextCursor
) {
}