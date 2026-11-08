package dev.egorgrigorciuc.forzammilmod;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

/** Checks the bundled player model matches BetterModel's player rig (steve.bbmodel at 2.2.0) and the wave animation. */
class PlayerModelTest {

    private static final String PATH = "models/players/forzammil_player.bbmodel";
    private static final List<String> BONES = List.of(
            "player_root", "shadow", "phip_hip", "pw_waist", "pc_chest", "h_ph_head",
            "pra_right_arm", "prfa_right_forearm", "pri_right_item",
            "pla_left_arm", "plfa_left_forearm", "pli_left_item",
            "prl_right_leg", "prfl_right_foreleg", "pll_left_leg", "plfl_left_foreleg",
            "tag_name", "cape_cape");
    private static final Set<String> MOVING = Set.of("pra_right_arm", "prfa_right_forearm");

    private static JsonObject model;
    private static Map<String, String> groupNameByUuid;

    @BeforeAll
    static void load() throws Exception {
        try (InputStream in = PlayerModelTest.class.getClassLoader().getResourceAsStream(PATH)) {
            assertNotNull(in, PATH + " missing from resources");
            model = JsonParser.parseReader(new InputStreamReader(in, StandardCharsets.UTF_8)).getAsJsonObject();
        }
        groupNameByUuid = new HashMap<>();
        for (JsonElement g : model.getAsJsonArray("groups")) {
            JsonObject o = g.getAsJsonObject();
            groupNameByUuid.put(o.get("uuid").getAsString(), o.get("name").getAsString());
        }
    }

    private static JsonObject wave() {
        for (JsonElement a : model.getAsJsonArray("animations")) {
            if (a.getAsJsonObject().get("name").getAsString().equals("wave")) return a.getAsJsonObject();
        }
        return fail("no wave animation");
    }

    @Test
    void hasAllPlayerBones() {
        assertEquals(new HashSet<>(BONES), new HashSet<>(groupNameByUuid.values()));
        assertEquals(BONES.size(), groupNameByUuid.size(), "duplicate group names");
    }

    @Test
    void outlinerReferencesOnlyKnownGroups() {
        Set<String> seen = new HashSet<>();
        walk(model.getAsJsonArray("outliner"), seen);
        assertEquals(groupNameByUuid.keySet(), seen);
    }

    private static void walk(JsonArray nodes, Set<String> seen) {
        for (JsonElement n : nodes) {
            if (!n.isJsonObject()) continue; // element uuid
            JsonObject o = n.getAsJsonObject();
            assertTrue(groupNameByUuid.containsKey(o.get("uuid").getAsString()));
            seen.add(o.get("uuid").getAsString());
            walk(o.getAsJsonArray("children"), seen);
        }
    }

    @Test
    void waveAnimation() {
        assertEquals(2, model.getAsJsonArray("animations").size());
        JsonObject w = wave();
        assertEquals(2.0, w.get("length").getAsDouble(), 1e-9);
        assertEquals("once", w.get("loop").getAsString());
    }

    @Test
    void onlyRightArmMoves() {
        JsonObject animators = wave().getAsJsonObject("animators");
        Set<String> withKeys = new HashSet<>();
        for (var e : animators.entrySet()) {
            String name = groupNameByUuid.get(e.getKey());
            assertNotNull(name, "animator keyed by unknown group uuid " + e.getKey());
            assertEquals(name, e.getValue().getAsJsonObject().get("name").getAsString());
            if (!e.getValue().getAsJsonObject().getAsJsonArray("keyframes").isEmpty()) withKeys.add(name);
        }
        assertTrue(withKeys.contains("pra_right_arm"));
        assertTrue(MOVING.containsAll(withKeys), "unexpected moving bones: " + withKeys);
    }

    @Test
    void keyframesAreSmoothAndReturnToRest() {
        for (var e : wave().getAsJsonObject("animators").entrySet()) {
            JsonArray kfs = e.getValue().getAsJsonObject().getAsJsonArray("keyframes");
            double first = Double.MAX_VALUE, last = -1;
            JsonObject firstKf = null, lastKf = null;
            for (JsonElement k : kfs) {
                JsonObject o = k.getAsJsonObject();
                double t = o.get("time").getAsDouble();
                assertTrue(t >= 0 && t <= 2.0, "keyframe outside 0..2 s: " + t);
                assertEquals("rotation", o.get("channel").getAsString());
                assertEquals("catmullrom", o.get("interpolation").getAsString());
                if (t < first) { first = t; firstKf = o; }
                if (t > last) { last = t; lastKf = o; }
            }
            assertEquals(0.0, first, 1e-9);
            assertEquals(2.0, last, 1e-9);
            assertZero(firstKf);
            assertZero(lastKf);
        }
    }

    @Test
    void armIsRaisedAtPointFour() {
        String armUuid = groupNameByUuid.entrySet().stream()
                .filter(e -> e.getValue().equals("pra_right_arm")).findFirst().orElseThrow().getKey();
        JsonArray kfs = wave().getAsJsonObject("animators").getAsJsonObject(armUuid).getAsJsonArray("keyframes");
        for (JsonElement k : kfs) {
            JsonObject o = k.getAsJsonObject();
            if (Math.abs(o.get("time").getAsDouble() - 0.4) < 1e-9) {
                double z = o.getAsJsonArray("data_points").get(0).getAsJsonObject().get("z").getAsDouble();
                assertEquals(150.0, z, 1e-9);
                return;
            }
        }
        fail("no keyframe at 0.4 s");
    }

    @Test
    void greetingIsPlayOnceAndOnlyMovesKnownBones() {
        JsonObject g = null;
        for (JsonElement a : model.getAsJsonArray("animations")) {
            if (a.getAsJsonObject().get("name").getAsString().equals("greeting")) g = a.getAsJsonObject();
        }
        assertNotNull(g, "no greeting animation");
        assertEquals(2.5, g.get("length").getAsDouble(), 1e-9);
        assertEquals("once", g.get("loop").getAsString());
        for (var e : g.getAsJsonObject("animators").entrySet()) {
            String name = groupNameByUuid.get(e.getKey());
            assertNotNull(name, "animator keyed by unknown group uuid " + e.getKey());
            assertEquals(name, e.getValue().getAsJsonObject().get("name").getAsString());
            for (JsonElement k : e.getValue().getAsJsonObject().getAsJsonArray("keyframes")) {
                double t = k.getAsJsonObject().get("time").getAsDouble();
                assertTrue(t >= 0 && t <= 2.5, "keyframe outside 0..2.5 s: " + t);
            }
        }
        assertEquals(Set.of("pra_right_arm"), g.getAsJsonObject("animators").entrySet().stream()
                .map(e -> groupNameByUuid.get(e.getKey())).collect(java.util.stream.Collectors.toSet()));
    }

    private static void assertZero(JsonObject kf) {
        JsonObject p = kf.getAsJsonArray("data_points").get(0).getAsJsonObject();
        for (String axis : List.of("x", "y", "z")) assertEquals(0.0, p.get(axis).getAsDouble(), 1e-9, axis);
    }
}
