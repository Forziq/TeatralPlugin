package dev.egorgrigorciuc.forzammilmod;

import dev.egorgrigorciuc.forzammilmod.ModelInstallPlan.Bundled;
import dev.egorgrigorciuc.forzammilmod.ModelInstallPlan.Reason;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

class ModelInstallPlanTest {

    private static byte[] b(String s) {
        return s.getBytes(StandardCharsets.UTF_8);
    }

    @Test
    void mapsPlayersAndRegularModels() {
        assertEquals(Optional.of("players/forzammil_player.bbmodel"),
                ModelInstallPlan.targetPath("models/players/forzammil_player.bbmodel"));
        assertEquals(Optional.of("models/tank.bbmodel"), ModelInstallPlan.targetPath("models/tank.bbmodel"));
        assertEquals(Optional.of("models/vehicles/tank.bbmodel"), ModelInstallPlan.targetPath("models/vehicles/tank.bbmodel"));
        assertEquals(Optional.of("players/sub/x.bbmodel"), ModelInstallPlan.targetPath("models/players/sub/x.bbmodel"));
    }

    @Test
    void rejectsOddEntries() {
        for (String bad : new String[]{null, "", "models/", "models/players/", "lang/en.yml", "plugin.yml",
                "models/../plugin.yml", "models/players/../../x", "models//x.bbmodel", "models/./x",
                "models\\x.bbmodel", "models/sub/", "Models/x.bbmodel"}) {
            assertEquals(Optional.empty(), ModelInstallPlan.targetPath(bad), String.valueOf(bad));
        }
    }

    @Test
    void sha1MatchesKnownValue() {
        assertEquals("a9993e364706816aba3e25717850c26c9cd0d89d", ModelInstallPlan.sha1(b("abc")));
    }

    @Test
    void reasonForMissingChangedSame() {
        assertEquals(Optional.of(Reason.MISSING), ModelInstallPlan.reason(b("a"), null));
        assertEquals(Optional.of(Reason.CHANGED), ModelInstallPlan.reason(b("a"), b("b")));
        assertEquals(Optional.empty(), ModelInstallPlan.reason(b("a"), b("a")));
    }

    @Test
    void planCopiesOnlyWhatDiffers() {
        var bundled = List.of(
                new Bundled("models/players/p.bbmodel", b("player")),
                new Bundled("models/tank.bbmodel", b("tank v2")),
                new Bundled("models/jeep.bbmodel", b("jeep")),
                new Bundled("lang/en.yml", b("ignored")));
        Map<String, byte[]> disk = Map.of(
                "models/tank.bbmodel", b("tank v1"),
                "models/jeep.bbmodel", b("jeep"));
        var plan = ModelInstallPlan.plan(bundled, disk::get);
        assertEquals(2, plan.size());
        assertEquals("players/p.bbmodel", plan.get(0).target());
        assertEquals(Reason.MISSING, plan.get(0).reason());
        assertEquals("models/tank.bbmodel", plan.get(1).target());
        assertEquals(Reason.CHANGED, plan.get(1).reason());
        assertArrayEquals(b("tank v2"), plan.get(1).data());
    }
}
