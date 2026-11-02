package dev.tidings.vanilla.chat;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;

class ChatTextTest {
    @Test
    void cleansWhatGoesIntoTheGame() {
        assertEquals("hi there", ChatText.clean("  hi there \n"));
        assertEquals("red text", ChatText.clean("§cred §ltext"));
        assertEquals("two lines", ChatText.clean("two\nlines"));
        assertEquals("nojoin", ChatText.clean("no​join\u0007"));
        assertEquals("ça va ? 🙂", ChatText.clean("ça va ? 🙂"));
    }

    @Test
    void refusesEmptyOrLongMessages() {
        assertNull(ChatText.clean(null));
        assertNull(ChatText.clean(""));
        assertNull(ChatText.clean(" \n\t "));
        assertNull(ChatText.clean("§§"));
        assertEquals(256, ChatText.clean("a".repeat(256)).length());
        assertNull(ChatText.clean("a".repeat(257)));
    }

    @Test
    void findsMentions() {
        assertEquals(
                List.of("steve", "alex_2"),
                List.copyOf(ChatText.mentions("@Steve and @alex_2, and @steve again")));
        assertEquals(Set.of(), ChatText.mentions("mail me@example.com, @ab is too short"));
        assertEquals(Set.of(), ChatText.mentions("@@Steve"));
        assertEquals(Set.of("steve"), ChatText.mentions("(@Steve)"));
    }

    @Test
    void readsWhispers() {
        assertEquals(new ChatText.Whisper("Steve", "hi there"), ChatText.whisper("/msg Steve hi there"));
        assertEquals(new ChatText.Whisper("Steve", "hi"), ChatText.whisper("/TELL Steve   hi  "));
        assertEquals(new ChatText.Whisper("Steve", "hi"), ChatText.whisper("/w Steve hi"));
        assertEquals(new ChatText.Whisper("Steve", "hi"), ChatText.whisper("minecraft:msg Steve hi"));
        assertEquals(new ChatText.Whisper("@a", "hi"), ChatText.whisper("msg @a hi"));
    }

    @Test
    void otherCommandsArentWhispers() {
        for (String command :
                List.of("/msg Steve", "/msg", "/msgSteve hi", "/me waves", "/op Steve", "msg")) {
            assertNull(ChatText.whisper(command), command);
        }
    }
}
