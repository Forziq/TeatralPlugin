package dev.egorgrigorciuc.forzammilmod;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SeenGuideTest {

    @TempDir
    Path dir;

    @Test
    void missingFileMeansNobodySeen() throws Exception {
        SeenGuide g = new SeenGuide(dir.resolve("seen-guide.yml"));
        g.load();
        assertFalse(g.hasSeen(UUID.randomUUID()));
    }

    @Test
    void markIsRememberedAcrossSaveAndLoad() throws Exception {
        Path f = dir.resolve("data/seen-guide.yml");
        UUID a = UUID.randomUUID();
        UUID b = UUID.randomUUID();
        SeenGuide g = new SeenGuide(f);
        assertTrue(g.markSeen(a));
        assertFalse(g.markSeen(a));
        g.save();
        SeenGuide again = new SeenGuide(f);
        again.load();
        assertTrue(again.hasSeen(a));
        assertFalse(again.hasSeen(b));
        assertEquals(1, again.all().size());
        assertFalse(Files.exists(dir.resolve("data/seen-guide.yml.tmp")));
    }

    @Test
    void junkLinesAreSkipped() throws Exception {
        UUID a = UUID.randomUUID();
        Path f = dir.resolve("seen-guide.yml");
        Files.writeString(f, "# comment\n\nnot a uuid\n- " + a + "\n- \"" + UUID.nameUUIDFromBytes(new byte[] {1}) + "\"\n");
        SeenGuide g = new SeenGuide(f);
        g.load();
        assertTrue(g.hasSeen(a));
        assertEquals(2, g.all().size());
    }
}
