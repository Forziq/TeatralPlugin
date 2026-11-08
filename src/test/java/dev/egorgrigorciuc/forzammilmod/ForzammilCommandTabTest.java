package dev.egorgrigorciuc.forzammilmod;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ForzammilCommandTabTest {

    // onTabComplete never touches plugin/sender/command, so nulls are safe here.
    private final ForzammilCommand cmd = new ForzammilCommand(null);

    private List<String> tab(String... args) {
        return cmd.onTabComplete(null, null, "forzammilmod", args);
    }

    @Test
    void emptyPrefixListsAllSubcommands() {
        assertEquals(List.of("music", "pack", "update", "version"), tab(""));
    }

    @Test
    void prefixMatches() {
        assertEquals(List.of("version"), tab("ver"));
    }

    @Test
    void prefixIsCaseInsensitive() {
        assertEquals(List.of("version"), tab("VER"));
    }

    @Test
    void nonMatchingPrefixIsEmpty() {
        assertTrue(tab("xyz").isEmpty());
    }

    @Test
    void secondArgumentGivesNoCompletions() {
        assertTrue(tab("version", "").isEmpty());
    }

    @Test
    void noArgsGivesNoCompletions() {
        assertTrue(tab().isEmpty());
    }

    @Test
    void musicSubcommands() {
        assertEquals(List.of("play", "stop", "shuffle", "loop", "list"), tab("music", ""));
        assertEquals(List.of("shuffle"), tab("music", "sh"));
    }

    @Test
    void musicTrackArgWithoutPluginIsEmpty() {
        assertTrue(tab("music", "play", "").isEmpty());
    }
}
