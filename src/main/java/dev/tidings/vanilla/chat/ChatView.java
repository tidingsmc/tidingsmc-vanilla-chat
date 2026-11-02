package dev.tidings.vanilla.chat;

import dev.tidings.core.PlayerProfile;
import dev.tidings.core.addon.AddonContext;
import dev.tidings.core.addon.Block;
import dev.tidings.core.addon.Request;
import dev.tidings.core.addon.View;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

final class ChatView {
    static final Duration REFRESH = Duration.ofSeconds(3);

    private ChatView() {}

    static View view(AddonContext tidings, ChatLog log, Request request) {
        UUID me = request.player().id();
        List<Block.ChatLine> lines = new ArrayList<>();
        for (ChatLog.Message message : log.oldestFirst()) {
            boolean mine = message.sender().equals(me);
            String tag;
            if (message.isWhisper()) {
                if (!mine && !message.to().equals(me)) continue;
                tag = mine ? "To " + message.toName() : "Whisper";
                if (message.phone()) tag = "Phone] [" + tag;
            } else {
                tag = message.phone() ? "Phone" : null;
            }
            Block.ChatLine line =
                    Block.ChatLine.of(
                                    profile(tidings, message.sender(), message.name()),
                                    message.text())
                            .time(message.time())
                            .tag(tag);
            lines.add(message.isWhisper() ? line.whispered() : line);
        }

        Block chat =
                Block.chat(lines, ChatAddon.SEND)
                        .placeholder("Type here")
                        .maxLength(ChatText.MAX_LENGTH);
        return View.of(null, List.of(chat)).refreshEvery(REFRESH);
    }

    static PlayerProfile profile(AddonContext tidings, UUID id, String name) {
        return tidings.player(id).orElse(new PlayerProfile(id, name, null));
    }
}
