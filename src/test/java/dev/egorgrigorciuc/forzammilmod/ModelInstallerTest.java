package dev.egorgrigorciuc.forzammilmod;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.jar.JarEntry;
import java.util.jar.JarOutputStream;
import java.util.logging.Logger;

import static org.junit.jupiter.api.Assertions.*;

class ModelInstallerTest {

    @TempDir
    Path dir;

    private Path jar(Map<String, String> entries) throws IOException {
        Path jar = dir.resolve("plugin.jar");
        try (JarOutputStream out = new JarOutputStream(Files.newOutputStream(jar))) {
            out.putNextEntry(new JarEntry("models/"));
            out.putNextEntry(new JarEntry("models/players/"));
            for (var e : entries.entrySet()) {
                out.putNextEntry(new JarEntry(e.getKey()));
                out.write(e.getValue().getBytes(StandardCharsets.UTF_8));
            }
        }
        return jar;
    }

    @Test
    void installsThenSkipsThenRepairs() throws IOException {
        Path jar = jar(Map.of(
                "models/players/forzammil_player.bbmodel", "{\"player\":1}",
                "models/tank.bbmodel", "{\"tank\":1}",
                "plugin.yml", "name: x"));
        Path bm = dir.resolve("BetterModel");
        var installer = new ModelInstaller(jar.toFile(), bm, Logger.getLogger("test"));

        assertEquals(2, installer.install().size());
        assertEquals("{\"player\":1}", Files.readString(bm.resolve("players/forzammil_player.bbmodel")));
        assertEquals("{\"tank\":1}", Files.readString(bm.resolve("models/tank.bbmodel")));
        assertFalse(Files.exists(bm.resolve("plugin.yml")));

        assertTrue(installer.install().isEmpty(), "second run should change nothing");

        Files.writeString(bm.resolve("models/tank.bbmodel"), "edited by hand");
        var again = installer.install();
        assertEquals(1, again.size());
        assertEquals(ModelInstallPlan.Reason.CHANGED, again.get(0).reason());
        assertEquals("{\"tank\":1}", Files.readString(bm.resolve("models/tank.bbmodel")));
        try (var files = Files.list(bm.resolve("models"))) {
            assertEquals(1, files.count(), "no temp files left behind");
        }
    }

    @Test
    void missingJarChangesNothing() {
        var installer = new ModelInstaller(dir.resolve("nope.jar").toFile(), dir.resolve("BetterModel"),
                Logger.getLogger("test"));
        assertTrue(installer.install().isEmpty());
    }
}
