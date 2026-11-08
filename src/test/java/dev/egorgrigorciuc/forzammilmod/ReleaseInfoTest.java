package dev.egorgrigorciuc.forzammilmod;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ReleaseInfoTest {
    @Test
    void picksJarAsset() {
        String json = "{\"tag_name\":\"v0.2.0\",\"assets\":[{\"id\":1,\"name\":\"release.json\"},"
                + "{\"id\":42,\"name\":\"forzammilmod-0.2.0.jar\"},{\"id\":3,\"name\":\"pack.zip\"}]}";
        ReleaseInfo r = ReleaseInfo.parse(json);
        assertEquals("0.2.0", r.version());
        assertEquals(42, r.assetId());
        assertEquals("forzammilmod-0.2.0.jar", r.assetName());
    }

    @Test
    void noJarGivesNull() {
        assertNull(ReleaseInfo.parse("{\"tag_name\":\"v1\",\"assets\":[{\"id\":1,\"name\":\"a.zip\"}]}"));
        assertNull(ReleaseInfo.parse("{\"assets\":[]}"));
    }
}
