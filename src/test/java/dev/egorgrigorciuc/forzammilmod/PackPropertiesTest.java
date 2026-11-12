package dev.egorgrigorciuc.forzammilmod;

import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.*;

class PackPropertiesTest {
    private static java.io.InputStream s(String t) {
        return new ByteArrayInputStream(t.getBytes(StandardCharsets.UTF_8));
    }

    @Test
    void valid() {
        var p = PackProperties.parse(s("url=https://x.test/pack.zip\nsha1=00ff00ff00ff00ff00ff00ff00ff00ff00ff00ff\n")).orElseThrow();
        assertEquals("https://x.test/pack.zip", p.url());
        assertEquals(20, p.sha1().length);
        assertEquals((byte) 0xff, p.sha1()[1]);
    }

    @Test
    void missingKey() {
        assertTrue(PackProperties.parse(s("url=https://x.test/p.zip\n")).isEmpty());
        assertTrue(PackProperties.parse(s("sha1=00ff00ff00ff00ff00ff00ff00ff00ff00ff00ff\n")).isEmpty());
        assertTrue(PackProperties.parse(null).isEmpty());
    }

    @Test
    void badSha() {
        assertTrue(PackProperties.parse(s("url=u\nsha1=xyz\n")).isEmpty());
        assertTrue(PackProperties.parse(s("url=u\nsha1=" + "g".repeat(40) + "\n")).isEmpty());
    }

    private static final String SHA = "sha1=00ff00ff00ff00ff00ff00ff00ff00ff00ff00ff\n";

    @Test
    void versionKeyWins() {
        var p = PackProperties.parse(s("url=https://x.test/forzammilmod-pack-0.7.0.zip\n" + SHA + "version=0.8.0\n")).orElseThrow();
        assertEquals("0.8.0", p.version());
    }

    @Test
    void versionFallsBackToUrl() {
        var p = PackProperties.parse(s("url=https://x.test/pack/forzammilmod-pack-0.7.0.zip\n" + SHA)).orElseThrow();
        assertEquals("0.7.0", p.version());
    }

    @Test
    void versionEmptyWhenUnknown() {
        var p = PackProperties.parse(s("url=https://x.test/pack.zip\n" + SHA)).orElseThrow();
        assertEquals("", p.version());
    }
}
