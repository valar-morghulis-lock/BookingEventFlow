package com.bookingeventflow.event.domain.valueobject;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class EventNameTest {


    @Test
    void shouldCreateValidEventName() {

        EventName name = new EventName("Java Conference");

        assertEquals("Java Conference", name.value());
    }

    @Test
    void shouldTrimEventName() {

        EventName name = new EventName("  Java Conference  ");

        assertEquals("Java Conference", name.value());
    }

    @Test
    void shouldRejectNullEventName() {

        assertThrows(NullPointerException.class, () -> new EventName(null));
    }

    @Test
    void shouldRejectBlankEventName() {

        assertThrows(IllegalArgumentException.class, () -> new EventName("   "));
    }

    @Test
    void shouldRejectEventNameExceedingMaximumLength() {

        String name = "a".repeat(201);

        assertThrows(IllegalArgumentException.class, () -> new EventName(name));
    }


}
