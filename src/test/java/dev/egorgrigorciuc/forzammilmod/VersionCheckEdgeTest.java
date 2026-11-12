package dev.egorgrigorciuc.forzammilmod;

import org.junit.jupiter.api.Test;

import static dev.egorgrigorciuc.forzammilmod.VersionCheck.Status.*;
import static org.junit.jupiter.api.Assertions.assertEquals;

class VersionCheckEdgeTest {

    @Test
    void packBlankAndNullPlugin() {
        assertEquals(UNKNOWN, VersionCheck.pack(null, "1.0.0"));
        assertEquals(UNKNOWN, VersionCheck.pack("  ", "1.0.0"));
        assertEquals(UNKNOWN, VersionCheck.pack("1.0.0", "   "));
        assertEquals(UNKNOWN, VersionCheck.pack(null, null));
    }

    @Test
    void packSuffixes() {
        assertEquals(OK, VersionCheck.pack("2.2.0-SNAPSHOT", "2.2.0"));
        assertEquals(OK, VersionCheck.pack("v2.2.0", "V2.2.0+build5"));
        assertEquals(MISMATCH, VersionCheck.pack("2.2.0-SNAPSHOT", "2.2.1"));
        assertEquals(OK, VersionCheck.pack("2.2", "2.2.0"));
    }

    @Test
    void betterModelEdges() {
        assertEquals(UNKNOWN, VersionCheck.betterModel(null, "2.2.0"));
        assertEquals(UNKNOWN, VersionCheck.betterModel("  ", null));
        assertEquals(MISMATCH, VersionCheck.betterModel("2.2.0", "   "));
        assertEquals(OK, VersionCheck.betterModel("2.2.0", "2.2.0-SNAPSHOT"));
        assertEquals(OK, VersionCheck.betterModel("v2.2.0", "2.2.0"));
    }

    @Test
    void urlEdges() {
        assertEquals("1.2.3", VersionCheck.packVersionFromUrl("https://x.test/forzammilmod-pack-1.2.3.zip?raw=1"));
        assertEquals("", VersionCheck.packVersionFromUrl("https://x.test/"));
        assertEquals("", VersionCheck.packVersionFromUrl(""));
        assertEquals("", VersionCheck.packVersionFromUrl("https://x.test/forzammilmod-pack-1.2.zip"));
        assertEquals("", VersionCheck.packVersionFromUrl("https://x.test/forzammilmod-pack-1.2.3.tar"));
        assertEquals("2.0.0", VersionCheck.packVersionFromUrl("https://x.test/forzammilmod-pack-1.0.0.zip/forzammilmod-pack-2.0.0.zip"));
    }

    @Test
    void docsEdges() {
        assertEquals("", VersionCheck.pinnedFromDocs(""));
        assertEquals("", VersionCheck.pinnedFromDocs("| BetterModel | latest |"));
        assertEquals("", VersionCheck.pinnedFromDocs("| BetterModel |"));
        assertEquals("", VersionCheck.pinnedFromDocs("| BetterModel Extra | 2.2.0 |"));
        assertEquals("2.2.0", VersionCheck.pinnedFromDocs("| bettermodel | 2.2.0"));
        assertEquals("2.2.0", VersionCheck.pinnedFromDocs("| Spigot | 1.21.4 |\r\n  | BetterModel | v2.2.0 (patched) |\r\n"));
        assertEquals("2.2.0", VersionCheck.pinnedFromDocs("BetterModel 9.9.9\n| BetterModel | 2.2.0 |"));
        assertEquals("2.2.0", VersionCheck.pinnedFromDocs("| BetterModel | 2.2.0 |\n| BetterModel | 3.0.0 |"));
    }
}
