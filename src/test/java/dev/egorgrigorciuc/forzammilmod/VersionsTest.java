package dev.egorgrigorciuc.forzammilmod;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class VersionsTest {
    @Test
    void equalAndPrefix() {
        assertEquals(0, Versions.compare("v0.1.0", "0.1.0"));
        assertEquals(0, Versions.compare("1.2", "1.2.0"));
    }

    @Test
    void newerOlder() {
        assertTrue(Versions.isNewer("0.2.0", "0.1.9"));
        assertTrue(Versions.isNewer("v1.0.0", "0.99.99"));
        assertFalse(Versions.isNewer("0.1.0", "0.1.1"));
        assertTrue(Versions.compare("0.10.0", "0.9.0") > 0);
    }

    @Test
    void suffixIgnored() {
        assertEquals(0, Versions.compare("0.2.0-SNAPSHOT", "0.2.0"));
        assertTrue(Versions.isNewer("0.3.0-rc1", "0.2.0"));
    }

    @Test
    void missingParts() {
        assertTrue(Versions.isNewer("2", "1.9.9"));
        assertEquals(0, Versions.compare("", "0.0.0"));
    }
}
