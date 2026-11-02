package dev.tidings.vanilla.chat;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonParser;

import java.io.IOException;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

final class KnownNames {
    private final Map<String, UUID> byName = new ConcurrentHashMap<>();

    void load(Path usercache) throws IOException {
        JsonElement json;
        try (Reader reader = Files.newBufferedReader(usercache, StandardCharsets.UTF_8)) {
            json = JsonParser.parseReader(reader);
        } catch (NoSuchFileException e) {
            return;
        } catch (JsonParseException e) {
            throw new IOException("Can't read " + usercache, e);
        }
        if (!(json instanceof JsonArray entries)) return;
        for (JsonElement entry : entries) {
            if (!(entry instanceof JsonObject object)) continue;
            try {
                String name = object.get("name").getAsString();
                UUID id = UUID.fromString(object.get("uuid").getAsString());
                byName.putIfAbsent(name.toLowerCase(Locale.ROOT), id);
            } catch (RuntimeException e) {
                // Skip
            }
        }
    }

    void add(String name, UUID id) {
        byName.values().remove(id); // they may have changed name
        byName.put(name.toLowerCase(Locale.ROOT), id);
    }

    Optional<UUID> find(String name) {
        return Optional.ofNullable(byName.get(name.toLowerCase(Locale.ROOT)));
    }

    void clear() {
        byName.clear();
    }
}
