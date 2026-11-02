package dev.tidings.vanilla.chat;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;
import java.util.UUID;

class KnownNamesTest {
    private static final UUID STEVE = UUID.fromString("8667ba71-b85a-4004-af54-457a9734eed7");
    private static final UUID ALEX = UUID.fromString("ec561538-f3fd-461d-aff5-086b22154bce");

    @TempDir Path dir;

    @Test
    void readsTheGamesUsercache() throws Exception {
        Path file = dir.resolve("usercache.json");
        Files.writeString(
                file,
                "[{\"name\":\"Steve\",\"uuid\":\"" + STEVE + "\",\"expiresOn\":\"x\"},"
                        + "{\"name\":\"broken\"},42,"
                        + "{\"name\":\"Alex\",\"uuid\":\"" + ALEX + "\"}]");
        KnownNames names = new KnownNames();
        names.load(file);
        assertEquals(Optional.of(STEVE), names.find("steve"));
        assertEquals(Optional.of(ALEX), names.find("ALEX"));
        assertTrue(names.find("broken").isEmpty());
    }

    @Test
    void noUsercacheIsFine() throws Exception {
        KnownNames names = new KnownNames();
        names.load(dir.resolve("usercache.json"));
        assertTrue(names.find("steve").isEmpty());
    }

    @Test
    void followsRenames() throws Exception {
        KnownNames names = new KnownNames();
        names.add("Steve", STEVE);
        names.add("Steve2", STEVE);
        assertTrue(names.find("steve").isEmpty());
        assertEquals(Optional.of(STEVE), names.find("steve2"));
        // The file is older than what logins said
        Path file = dir.resolve("usercache.json");
        Files.writeString(file, "[{\"name\":\"Steve2\",\"uuid\":\"" + ALEX + "\"}]");
        names.load(file);
        assertEquals(Optional.of(STEVE), names.find("steve2"));
    }
}
