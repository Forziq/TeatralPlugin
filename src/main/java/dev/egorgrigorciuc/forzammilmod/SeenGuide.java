package dev.egorgrigorciuc.forzammilmod;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * Remembers who has already been shown the first-join guide. A tiny YAML list of UUIDs ("- uuid" per line) that
 * is read once at startup and written whole. No Bukkit types. Safe to call from any thread.
 */
final class SeenGuide {

    private final Path file;
    private final Set<UUID> seen = new LinkedHashSet<>();

    SeenGuide(Path file) {
        this.file = file;
    }

    /** Reads the file if it exists. Lines that are not UUIDs are skipped. */
    synchronized void load() throws IOException {
        seen.clear();
        if (!Files.exists(file)) return;
        for (String line : Files.readAllLines(file, StandardCharsets.UTF_8)) {
            String s = line.trim();
            if (s.startsWith("-")) s = s.substring(1).trim();
            if (s.startsWith("\"") && s.endsWith("\"") && s.length() >= 2) s = s.substring(1, s.length() - 1);
            try {
                seen.add(UUID.fromString(s));
            } catch (IllegalArgumentException ignored) {
                // comment, blank or junk line
            }
        }
    }

    synchronized boolean hasSeen(UUID id) {
        return seen.contains(id);
    }

    /** Returns true if this is the first time the player is marked. */
    synchronized boolean markSeen(UUID id) {
        return seen.add(id);
    }

    /** Writes the whole list through a temp file, so a crash never leaves half a file. */
    synchronized void save() throws IOException {
        List<String> lines = new ArrayList<>();
        lines.add("# Players who have been shown the first-join guide. Delete a line to show it to them again.");
        for (UUID id : seen) lines.add("- " + id);
        Path parent = file.toAbsolutePath().getParent();
        if (parent != null) Files.createDirectories(parent);
        Path tmp = file.resolveSibling(file.getFileName() + ".tmp");
        Files.write(tmp, lines, StandardCharsets.UTF_8);
        Files.move(tmp, file, StandardCopyOption.REPLACE_EXISTING);
    }

    synchronized Set<UUID> all() {
        return Collections.unmodifiableSet(new LinkedHashSet<>(seen));
    }
}
