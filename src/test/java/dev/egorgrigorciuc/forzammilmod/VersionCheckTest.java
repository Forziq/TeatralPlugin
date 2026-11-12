package dev.egorgrigorciuc.forzammilmod;

import org.junit.jupiter.api.Test;

import static dev.egorgrigorciuc.forzammilmod.VersionCheck.Status.*;
import static org.junit.jupiter.api.Assertions.assertEquals;

class VersionCheckTest {

    @Test
    void packMatches() {
        assertEquals(OK, VersionCheck.pack("0.8.0", "0.8.0"));
        assertEquals(OK, VersionCheck.pack("0.8.0-SNAPSHOT", "v0.8.0"));
    }

    @Test
    void packMismatch() {
        assertEquals(MISMATCH, VersionCheck.pack("0.8.0", "0.7.0"));
    }

    @Test
    void packUnknown() {
        assertEquals(UNKNOWN, VersionCheck.pack("0.8.0", ""));
        assertEquals(UNKNOWN, VersionCheck.pack("0.8.0", null));
    }

    @Test
    void betterModelIgnoresSuffix() {
        assertEquals(OK, VersionCheck.betterModel("2.2.0", "2.2.0"));
        assertEquals(OK, VersionCheck.betterModel("2.2.0", "2.2.0-fmod+patched"));
    }

    @Test
    void betterModelMismatchAndMissing() {
        assertEquals(MISMATCH, VersionCheck.betterModel("2.2.0", "3.0.0"));
        assertEquals(MISMATCH, VersionCheck.betterModel("2.2.0", "2.2.1"));
        assertEquals(MISMATCH, VersionCheck.betterModel("2.2.0", null));
        assertEquals(MISMATCH, VersionCheck.betterModel("2.2.0", ""));
        assertEquals(UNKNOWN, VersionCheck.betterModel("", "2.2.0"));
    }

    @Test
    void urlParsing() {
        assertEquals("0.7.0", VersionCheck.packVersionFromUrl("https://a.github.io/R/pack/forzammilmod-pack-0.7.0.zip"));
        assertEquals("", VersionCheck.packVersionFromUrl("https://x.test/pack.zip"));
        assertEquals("", VersionCheck.packVersionFromUrl(null));
    }

    @Test
    void docsRow() {
        String md = "| Thing | Version | Notes |\n|---|---|---|\n| Spigot | 1.21.4 | x |\n"
                + "| BetterModel | 2.2.0 (patched) | uses 3.6.0 elsewhere |\n";
        assertEquals("2.2.0", VersionCheck.pinnedFromDocs(md));
        assertEquals("", VersionCheck.pinnedFromDocs("| Spigot | 1.21.4 |"));
        assertEquals("", VersionCheck.pinnedFromDocs(null));
    }
}
