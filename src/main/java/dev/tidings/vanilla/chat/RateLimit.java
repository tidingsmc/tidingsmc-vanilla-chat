package dev.tidings.vanilla.chat;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

final class RateLimit {
    private final int burst;
    private final Duration every;
    private final Map<UUID, Instant> full = new ConcurrentHashMap<>();

    RateLimit(int burst, Duration every) {
        this.burst = burst;
        this.every = every;
    }

    boolean allow(UUID player, Instant now) {
        boolean[] allowed = {false};
        full.compute(
                player,
                (id, until) -> {
                    Instant start = until == null || until.isBefore(now) ? now : until;
                    Instant next = start.plus(every);
                    if (Duration.between(now, next).compareTo(every.multipliedBy(burst)) > 0) {
                        return until;
                    }
                    allowed[0] = true;
                    return next;
                });
        return allowed[0];
    }

    void clear() {
        full.clear();
    }
}
