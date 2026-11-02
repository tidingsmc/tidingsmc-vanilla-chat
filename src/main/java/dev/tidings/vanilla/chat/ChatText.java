package dev.tidings.vanilla.chat;

import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

final class ChatText {
    static final int MAX_LENGTH = 256;

    /** "@" then a Minecraft name, not inside a word or an email address */
    private static final Pattern MENTION = Pattern.compile("(?<![\\w@])@(\\w{3,16})(?!\\w)");

    /** "/msg", "/tell" or "/w", with or without "/" or "minecraft:", then a name and a message */
    private static final Pattern WHISPER =
            Pattern.compile(
                    "^/?(?:minecraft:)?(?:msg|tell|w)\\s+(\\S+)\\s+(.*\\S)\\s*$",
                    Pattern.CASE_INSENSITIVE | Pattern.DOTALL);

    record Whisper(String target, String text) {}

    private ChatText() {}

    /** A /msg command, or null if {@code command} isn't one. */
    static Whisper whisper(String command) {
        Matcher matcher = WHISPER.matcher(command);
        return matcher.matches() ? new Whisper(matcher.group(1), matcher.group(2)) : null;
    }

    /**
     * What a player typed in the app, as it may go into the game's chat: no formatting codes or
     * control characters, which the game's own chat box refuses too, and spaces trimmed.
     *
     * @return null if nothing's left or it's longer than {@link #MAX_LENGTH}
     */
    static String clean(String text) {
        if (text == null) return null;
        StringBuilder out = new StringBuilder(text.length());
        boolean code = false;
        for (int c : text.codePoints().toArray()) {
            if (code) { // the character after "§", such as "c" for red
                code = false;
            } else if (c == '§') {
                code = true;
            } else if (Character.isISOControl(c) || Character.getType(c) == Character.FORMAT) {
                // Newlines and tabs as spaces and other invisible characters dropped
                if (Character.isWhitespace(c)) out.append(' ');
            } else {
                out.appendCodePoint(c);
            }
        }
        String cleaned = out.toString().strip();
        if (cleaned.isEmpty() || cleaned.length() > MAX_LENGTH) return null;
        return cleaned;
    }

    static Set<String> mentions(String text) {
        Set<String> names = new LinkedHashSet<>();
        Matcher matcher = MENTION.matcher(text);
        while (matcher.find()) names.add(matcher.group(1).toLowerCase(Locale.ROOT));
        return names;
    }
}
