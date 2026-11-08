package dev.egorgrigorciuc.forzammilmod;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.junit.jupiter.api.Test;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

/** Checks the placeholder tank and jeep models have the bones the vehicle code will look up. */
class VehicleModelTest {

    private static class Model {
        final JsonObject json;
        final Map<String, String> parentByName = new HashMap<>();
        final Set<String> elementUuids = new HashSet<>();
        final double[] min = {1e9, 1e9, 1e9};
        final double[] max = {-1e9, -1e9, -1e9};

        Model(String path) throws Exception {
            try (InputStream in = VehicleModelTest.class.getClassLoader().getResourceAsStream(path)) {
                assertNotNull(in, path + " missing");
                json = JsonParser.parseReader(new InputStreamReader(in, StandardCharsets.UTF_8)).getAsJsonObject();
            }
            Map<String, String> nameByUuid = new HashMap<>();
            for (JsonElement g : json.getAsJsonArray("groups")) {
                JsonObject o = g.getAsJsonObject();
                assertNull(nameByUuid.put(o.get("uuid").getAsString(), o.get("name").getAsString()), "duplicate group uuid");
            }
            for (JsonElement e : json.getAsJsonArray("elements")) {
                JsonObject o = e.getAsJsonObject();
                elementUuids.add(o.get("uuid").getAsString());
                for (int i = 0; i < 3; i++) {
                    min[i] = Math.min(min[i], o.getAsJsonArray("from").get(i).getAsDouble());
                    max[i] = Math.max(max[i], o.getAsJsonArray("to").get(i).getAsDouble());
                }
                for (var f : o.getAsJsonObject("faces").entrySet()) {
                    assertEquals(0, f.getValue().getAsJsonObject().get("texture").getAsInt());
                }
            }
            walk(json.getAsJsonArray("outliner"), null, nameByUuid);
        }

        private void walk(Iterable<JsonElement> nodes, String parent, Map<String, String> names) {
            for (JsonElement n : nodes) {
                if (n.isJsonPrimitive()) {
                    assertTrue(elementUuids.contains(n.getAsString()), "unknown element " + n);
                } else {
                    JsonObject o = n.getAsJsonObject();
                    String name = names.get(o.get("uuid").getAsString());
                    assertNotNull(name, "outliner group not in groups");
                    parentByName.put(name, parent);
                    walk(o.getAsJsonArray("children"), name, names);
                }
            }
        }

        double size(int axis) {
            return max[axis] - min[axis];
        }
    }

    @Test
    void tankHasBones() throws Exception {
        Model m = new Model("models/tank.bbmodel");
        assertEquals(Set.of("hull", "turret", "barrel", "muzzle", "p_driver", "sp_gunner"), m.parentByName.keySet());
        assertNull(m.parentByName.get("hull"));
        assertEquals("hull", m.parentByName.get("turret"));
        assertEquals("turret", m.parentByName.get("barrel"));
        assertEquals("barrel", m.parentByName.get("muzzle"));
        assertEquals("hull", m.parentByName.get("p_driver"));
        assertEquals("turret", m.parentByName.get("sp_gunner"));
        basics(m);
        assertTrue(m.size(0) >= 40 && m.size(0) <= 56, "width " + m.size(0));
        assertTrue(m.size(2) >= 70 && m.size(2) <= 90, "length " + m.size(2));
        assertTrue(m.size(1) >= 24 && m.size(1) <= 40, "height " + m.size(1));
    }

    @Test
    void jeepHasBones() throws Exception {
        Model m = new Model("models/jeep.bbmodel");
        assertEquals(Set.of("body", "wheel_fl", "wheel_fr", "wheel_rl", "wheel_rr", "p_driver", "sp_passenger1", "sp_passenger2"), m.parentByName.keySet());
        for (String w : new String[]{"wheel_fl", "wheel_fr", "wheel_rl", "wheel_rr", "p_driver", "sp_passenger1", "sp_passenger2"}) {
            assertEquals("body", m.parentByName.get(w), w);
        }
        basics(m);
        assertTrue(m.size(0) >= 26 && m.size(0) <= 38, "width " + m.size(0));
        assertTrue(m.size(2) >= 48 && m.size(2) <= 64, "length " + m.size(2));
    }

    private static void basics(Model m) {
        assertEquals(1, m.json.getAsJsonArray("textures").size());
        assertEquals(0, m.json.getAsJsonArray("animations").size());
    }
}
