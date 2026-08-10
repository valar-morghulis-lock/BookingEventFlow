package com.bookingeventflow.event.domain.valueobject;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class EventDescriptionTest {

    @Test
    void shouldCreateValidDescription() {

        EventDescription description =
                new EventDescription("A Java conference.");

        assertEquals(
                "A Java conference.",
                description.value()
        );

        assertTrue(description.isPresent());
    }

    @Test
    void shouldTrimDescription() {

        EventDescription description =
                new EventDescription("  A Java conference.  ");

        assertEquals(
                "A Java conference.",
                description.value()
        );
    }

    @Test
    void shouldAllowNullDescription() {

        EventDescription description =
                new EventDescription(null);

        assertNull(description.value());
        assertFalse(description.isPresent());
    }

    @Test
    void shouldConvertBlankDescriptionToNull() {

        EventDescription description =
                new EventDescription("   ");

        assertNull(description.value());
        assertFalse(description.isPresent());
    }

    @Test
    void shouldRejectDescriptionExceedingMaximumLength() {

        String description = "a".repeat(2001);

        assertThrows(
                IllegalArgumentException.class,
                () -> new EventDescription(description)
        );
    }


}
