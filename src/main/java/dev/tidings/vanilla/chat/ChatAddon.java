package dev.tidings.vanilla.chat;

import java.io.IOException;
import java.nio.file.Path;
import java.time.Duration;
import java.time.Instant;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.mojang.authlib.GameProfile;

import dev.tidings.core.PlayerProfile;
import dev.tidings.core.addon.ActionResult;
import dev.tidings.core.addon.Addon;
import dev.tidings.core.addon.AddonContext;
import dev.tidings.core.addon.AddonError;
import dev.tidings.core.addon.Block;
import dev.tidings.core.addon.Icon;
import dev.tidings.core.addon.Link;
import dev.tidings.core.addon.Notification;
import dev.tidings.core.addon.Request;
import dev.tidings.core.addon.View;
import dev.tidings.forge.TidingsAddons;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.common.util.FakePlayer;
import net.minecraftforge.event.CommandEvent;
import net.minecraftforge.event.ServerChatEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.server.ServerStartedEvent;
import net.minecraftforge.event.server.ServerStoppedEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.server.ServerLifecycleHooks;

final class ChatAddon {
    static final String SEND = "send";

    private static final Logger LOGGER = LoggerFactory.getLogger(ChatAddon.class);

    private final AddonContext tidings;
    private final ChatLog log = new ChatLog();
    private final KnownNames names = new KnownNames();
    private final RateLimit rateLimit = new RateLimit(3, Duration.ofSeconds(2));

    private ChatAddon(AddonContext tidings) {
        this.tidings = tidings;
    }

    static void register(FMLJavaModLoadingContext context) {
        Holder holder = new Holder();
        AddonContext tidings =
                TidingsAddons.register(
                        context,
                        Addon.builder("Chat")
                                .icon(Icon.MESSAGE)
                                .view("chat", r -> holder.addon.view(r))
                                .action(SEND, r -> holder.addon.send(r)));
        holder.addon = new ChatAddon(tidings);
        MinecraftForge.EVENT_BUS.register(holder.addon);
    }

    private static final class Holder {
        ChatAddon addon;
    }

    @SubscribeEvent
    public void onServerStarted(ServerStartedEvent event) {
        try {
            names.load(Path.of("usercache.json"));
        } catch (IOException e) {
            LOGGER.warn("Can't read who joined before, so mentions only find who joins now", e);
        }
    }

    @SubscribeEvent
    public void onServerStopped(ServerStoppedEvent event) {
        log.clear();
        names.clear();
        rateLimit.clear();
    }

    @SubscribeEvent
    public void onPlayerLoggedIn(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer player && !(player instanceof FakePlayer)) {
            GameProfile profile = player.getGameProfile();
            names.add(profile.getName(), profile.getId());
        }
    }

    // After other mods which may cancel or change the message
    @SubscribeEvent(priority = EventPriority.LOWEST)
    public void onChat(ServerChatEvent event) {
        ServerPlayer player = event.getPlayer();
        if (player instanceof FakePlayer) return;
        String text = event.getMessage().getString();
        if (text.isBlank()) return;
        UUID id = player.getUUID();
        String name = player.getGameProfile().getName();
        log.add(new ChatLog.Message(Instant.now(), id, name, text, false));
        notifyMentioned(player.server, id, name, text);
    }

    private View view(Request request) {
        return ChatView.view(tidings, log, request);
    }

    private ActionResult send(Request request) {
        MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
        if (server == null) throw new AddonError("The server is starting");
        PlayerProfile player = request.player();
        if (server.getPlayerList().getBans().isBanned(new GameProfile(player.id(), player.name()))) {
            throw new AddonError("You're banned from this server");
        }
        String text = ChatText.clean(request.value(Block.Chat.VALUE));
        if (text == null) {
            throw new AddonError(
                    "Write a message of at most " + ChatText.MAX_LENGTH + " characters");
        }
        if (!rateLimit.allow(player.id(), Instant.now())) {
            throw new AddonError("You're sending messages too fast: wait a few seconds");
        }
        if (text.startsWith("/")) {
            whisperFromPhone(server, player, text);
            return ActionResult.done();
        }

        server.getPlayerList()
                .broadcastSystemMessage(
                        fromPhone(Component.translatable("chat.type.text", player.name(), text)),
                        false);

        log.add(new ChatLog.Message(Instant.now(), player.id(), player.name(), text, true));
        notifyMentioned(server, player.id(), player.name(), text);
        return ActionResult.done();
    }

    // The only command the app takes
    private void whisperFromPhone(MinecraftServer server, PlayerProfile player, String command) {
        ChatText.Whisper whisper = ChatText.whisper(command);
        if (whisper == null) {
            throw new AddonError("From the app, the only command is /msg <player> <message>");
        }
        UUID to =
                names.find(whisper.target())
                        .orElseThrow(
                                () ->
                                        new AddonError(
                                                "No player called "
                                                        + whisper.target()
                                                        + " has played here"));
        LOGGER.info("{} whispered from their phone: {}", player.name(), command);
        whisper(server, player.id(), player.name(), to, nameOf(server, to, whisper.target()),
                whisper.text(), true);
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public void onCommand(CommandEvent event) {
        CommandSourceStack source = event.getParseResults().getContext().getSource();
        if (!(source.getEntity() instanceof ServerPlayer player) || player instanceof FakePlayer) {
            return;
        }
        ChatText.Whisper whisper = ChatText.whisper(event.getParseResults().getReader().getString());
        if (whisper == null) return;
        String text = ChatText.clean(whisper.text());
        if (text == null) return;
        MinecraftServer server = player.server;
        String fromName = player.getGameProfile().getName();
        ServerPlayer online = server.getPlayerList().getPlayerByName(whisper.target());
        if (online != null) {
            log.add(
                    new ChatLog.Message(
                            Instant.now(),
                            player.getUUID(),
                            fromName,
                            text,
                            false,
                            online.getUUID(),
                            online.getGameProfile().getName()));
            return;
        }
        Optional<UUID> to = names.find(whisper.target());
        if (to.isEmpty()) return;
        event.setCanceled(true);
        whisper(
                server,
                player.getUUID(),
                fromName,
                to.get(),
                nameOf(server, to.get(), whisper.target()),
                text,
                false);
    }

    private void whisper(
            MinecraftServer server,
            UUID from,
            String fromName,
            UUID to,
            String toName,
            String text,
            boolean phone) {
        ServerPlayer recipient = server.getPlayerList().getPlayer(to);
        if (recipient != null) {
            Component incoming =
                    Component.translatable("commands.message.display.incoming", fromName, text)
                            .withStyle(ChatFormatting.GRAY, ChatFormatting.ITALIC);
            recipient.sendSystemMessage(phone ? fromPhone(incoming) : incoming);
        }
        ServerPlayer sender = server.getPlayerList().getPlayer(from);
        if (sender != null) {
            MutableComponent echo =
                    Component.translatable("commands.message.display.outgoing", toName, text)
                            .withStyle(ChatFormatting.GRAY, ChatFormatting.ITALIC);
            if (recipient == null) {
                echo.append(Component.literal(" (to their phone)").withStyle(ChatFormatting.DARK_GRAY));
            }
            sender.sendSystemMessage(echo);
        }
        log.add(new ChatLog.Message(Instant.now(), from, fromName, text, phone, to, toName));
        if (recipient == null && !to.equals(from)) {
            tidings.notify(
                    Notification.of(shorten(fromName + " whispers to you: " + text))
                            .about(ChatView.profile(tidings, from, fromName))
                            .link(Link.to("chat"))
                            .to(Set.of(to)));
        }
    }

    /** {@code message} after a grey "[Phone]" to tell it was sent from the app. */
    private static Component fromPhone(Component message) {
        return Component.empty()
                .append(Component.literal("[Phone] ").withStyle(ChatFormatting.GRAY))
                .append(message);
    }

    private String nameOf(MinecraftServer server, UUID id, String typed) {
        ServerPlayer online = server.getPlayerList().getPlayer(id);
        if (online != null) return online.getGameProfile().getName();
        return tidings.player(id).map(PlayerProfile::name).orElse(typed);
    }

    private static String shorten(String text) {
        if (text.length() <= Notification.MAX_TEXT_LENGTH) return text;
        return text.substring(0, Notification.MAX_TEXT_LENGTH - 1) + "…";
    }

    private void notifyMentioned(MinecraftServer server, UUID sender, String name, String text) {
        Set<String> mentioned = ChatText.mentions(text);
        if (mentioned.isEmpty()) return;
        Set<UUID> recipients = new HashSet<>();
        for (String mention : mentioned) {
            names.find(mention)
                    .filter(id -> !id.equals(sender))
                    .filter(id -> server.getPlayerList().getPlayer(id) == null)
                    .ifPresent(recipients::add);
        }
        if (recipients.isEmpty()) return;
        tidings.notify(
                Notification.of(shorten(name + ": " + text))
                        .about(ChatView.profile(tidings, sender, name))
                        .link(Link.to("chat"))
                        .to(recipients));
    }
}
