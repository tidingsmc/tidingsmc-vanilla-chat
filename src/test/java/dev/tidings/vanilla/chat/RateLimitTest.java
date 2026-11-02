package dev.tidings.vanilla.chat;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

class RateLimitTest {
    private final RateLimit limit = new RateLimit(3, Duration.ofSeconds(2));
    private final UUID steve = UUID.randomUUID();
    private final Instant start = Instant.parse("2026-11-02T12:00:00Z");

    @Test
    void allowsABurstThenOneEveryPeriod() {
        assertTrue(limit.allow(steve, start));
        assertTrue(limit.allow(steve, start));
        assertTrue(limit.allow(steve, start));
        assertFalse(limit.allow(steve, start));
        assertFalse(limit.allow(steve, start.plusMillis(1999)));
        assertTrue(limit.allow(steve, start.plusSeconds(2)));
        assertFalse(limit.allow(steve, start.plusSeconds(2)));
    }

    @Test
    void refusedMessagesDontCount() {
        for (int i = 0; i < 10; i++) limit.allow(steve, start);
        assertTrue(limit.allow(steve, start.plusSeconds(2)));
    }

    @Test
    void fillsUpAgainWhenQuiet() {
        for (int i = 0; i < 3; i++) limit.allow(steve, start);
        Instant later = start.plusSeconds(60);
        assertTrue(limit.allow(steve, later));
        assertTrue(limit.allow(steve, later));
        assertTrue(limit.allow(steve, later));
        assertFalse(limit.allow(steve, later));
    }

    @Test
    void eachPlayerHasTheirOwn() {
        for (int i = 0; i < 3; i++) limit.allow(steve, start);
        assertTrue(limit.allow(UUID.randomUUID(), start));
    }
}
