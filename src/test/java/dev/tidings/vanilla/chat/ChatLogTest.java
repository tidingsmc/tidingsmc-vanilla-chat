package dev.tidings.vanilla.chat;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.UUID;

class ChatLogTest {
    @Test
    void keepsTheLastMessages() {
        ChatLog log = new ChatLog();
        UUID id = UUID.randomUUID();
        for (int i = 0; i < ChatLog.SIZE + 5; i++) {
            log.add(new ChatLog.Message(Instant.EPOCH, id, "Steve", "#" + i, false));
        }
        var messages = log.oldestFirst();
        assertEquals(ChatLog.SIZE, messages.size());
        assertEquals("#5", messages.get(0).text());
        assertEquals("#" + (ChatLog.SIZE + 4), messages.get(ChatLog.SIZE - 1).text());
    }
}
