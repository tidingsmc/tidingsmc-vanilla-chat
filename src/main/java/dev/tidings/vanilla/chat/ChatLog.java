package dev.tidings.vanilla.chat;

import java.time.Instant;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.UUID;

final class ChatLog {
    /** How many messages it keeps */
    static final int SIZE = 50;

    /**
     * @param phone whether it was sent from the app rather than from the game
     * @param to who it was whispered to with /msg, or null for everyone
     * @param toName their name, or null
     */
    record Message(
            Instant time,
            UUID sender,
            String name,
            String text,
            boolean phone,
            UUID to,
            String toName) {
        /** To everyone */
        Message(Instant time, UUID sender, String name, String text, boolean phone) {
            this(time, sender, name, text, phone, null, null);
        }

        boolean isWhisper() {
            return to != null;
        }
    }

    private final Deque<Message> messages = new ArrayDeque<>();

    synchronized void add(Message message) {
        if (messages.size() == SIZE) messages.removeFirst();
        messages.addLast(message);
    }

    synchronized List<Message> oldestFirst() {
        return new ArrayList<>(messages);
    }

    synchronized void clear() {
        messages.clear();
    }
}
