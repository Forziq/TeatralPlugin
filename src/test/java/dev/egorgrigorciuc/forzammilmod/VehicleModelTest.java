package dev.egorgrigorciuc.forzammilmod;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.junit.jupiter.api.Test;

import javax.imageio.ImageIO;

import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

/** Checks the tank and jeep models have the bones, pivots and seats the vehicle code looks up, and that BetterModel can draw them as made. */
class VehicleModelTest {

    private static class Model {
        final JsonObject json;
        final Map<String, String> parentByName = new HashMap<>();
        final Map<String, double[]> originByName = new HashMap<>();
        final Map<String, double[]> boxByName = new HashMap<>();
        final Map<String, JsonObject> elementByUuid = new HashMap<>();
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
                var or = o.getAsJsonArray("origin");
                originByName.put(o.get("name").getAsString(), new double[]{or.get(0).getAsDouble(), or.get(1).getAsDouble(), or.get(2).getAsDouble()});
            }
            for (JsonElement e : json.getAsJsonArray("elements")) {
                JsonObject o = e.getAsJsonObject();
                elementUuids.add(o.get("uuid").getAsString());
                elementByUuid.put(o.get("uuid").getAsString(), o);
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
                    JsonObject el = elementByUuid.get(n.getAsString());
                    if (parent != null && !el.has("rotation")) {
                        // y0, z0 ... as {x0, y0, z0, y1, z1}: only the plain (unturned) cubes of the group
                        double[] b = boxByName.computeIfAbsent(parent, k -> new double[]{1e9, 1e9, 1e9, -1e9, -1e9});
                        b[0] = Math.min(b[0], el.getAsJsonArray("from").get(0).getAsDouble());
                        b[1] = Math.min(b[1], el.getAsJsonArray("from").get(1).getAsDouble());
                        b[2] = Math.min(b[2], el.getAsJsonArray("from").get(2).getAsDouble());
                        b[3] = Math.max(b[3], el.getAsJsonArray("to").get(1).getAsDouble());
                        b[4] = Math.max(b[4], el.getAsJsonArray("to").get(2).getAsDouble());
                    }
                } else {
                    JsonObject o = n.getAsJsonObject();
                    String name = names.get(o.get("uuid").getAsString());
                    assertNotNull(name, "outliner group not in groups");
                    parentByName.put(name, parent);
                    walk(o.getAsJsonArray("children"), name, names);
                }
            }
        }

        double[] origin(String group) {
            double[] o = originByName.get(group);
            assertNotNull(o, "no " + group + " bone");
            return o;
        }

        /** {minX, minY, minZ, maxY, maxZ} of the group's own unturned cubes, or null if it has none. */
        double[] unturnedBox(String group) {
            return boxByName.get(group);
        }

        double size(int axis) {
            return max[axis] - min[axis];
        }
    }

    private static final Set<Double> TURNS = Set.of(-45.0, -22.5, 22.5, 45.0);

    @Test
    void tankHasBones() throws Exception {
        Model m = tank("models/tank.bbmodel", VehicleType.TANK);
        assertTrue(m.size(0) >= 40 && m.size(0) <= 56, "width " + m.size(0));
        assertTrue(m.size(2) >= 70 && m.size(2) <= 100, "length " + m.size(2));
        assertTrue(m.size(1) >= 24 && m.size(1) <= 40, "height " + m.size(1));
    }

    @Test
    void heavyTankHasBones() throws Exception {
        Model m = tank("models/tank_heavy.bbmodel", VehicleType.TANK_HEAVY);
        assertTrue(m.size(0) >= 50 && m.size(0) <= 60, "width " + m.size(0));
        assertTrue(m.size(2) >= 80 && m.size(2) <= 104, "length " + m.size(2));
        assertTrue(m.size(1) >= 30 && m.size(1) <= 40, "height " + m.size(1));
    }

    @Test
    void jeepHasBones() throws Exception {
        Model m = jeep("models/jeep.bbmodel", VehicleType.JEEP);
        assertTrue(m.size(0) >= 26 && m.size(0) <= 38, "width " + m.size(0));
        assertTrue(m.size(2) >= 48 && m.size(2) <= 64, "length " + m.size(2));
    }

    @Test
    void armouredJeepHasBones() throws Exception {
        Model m = jeep("models/jeep_heavy.bbmodel", VehicleType.JEEP_HEAVY);
        assertTrue(m.size(0) >= 36 && m.size(0) <= 44, "width " + m.size(0));
        assertTrue(m.size(2) >= 60 && m.size(2) <= 74, "length " + m.size(2));
    }

    @Test
    void truckHasBones() throws Exception {
        Model m = new Model("models/truck.bbmodel");
        VehicleType type = VehicleType.TRUCK;
        assertNull(m.parentByName.get("body"));
        assertTrue(m.parentByName.containsKey("body"));
        assertEquals(8, CrewSeats.seatsOf(type).size());
        assertEquals("body", m.parentByName.get("p_driver"));
        for (int i = 1; i <= 7; i++) assertEquals("body", m.parentByName.get("sp_passenger" + i), "passenger" + i);
        assertEquals(6, WheelSpin.bones(type).size());
        for (String w : WheelSpin.bones(type)) assertEquals("body", m.parentByName.get(w), w);
        seats(m, type);
        wheels(m, type);
        basics(m);
        assertTrue(m.size(0) >= 36 && m.size(0) <= 44, "width " + m.size(0));
        assertTrue(m.size(2) >= 90 && m.size(2) <= 110, "length " + m.size(2));
        assertTrue(type.halfWidth * 32 <= m.size(0), "hit box wider than the model");
        assertTrue(type.halfLength * 32 <= m.size(2), "hit box longer than the model");
        assertTrue(type.height * 16 >= m.size(1), "hit box lower than the model");
    }

    @Test
    void artilleryHasBones() throws Exception {
        Model m = new Model("models/artillery.bbmodel");
        VehicleType type = VehicleType.ARTILLERY;
        assertNull(m.parentByName.get("carriage"));
        assertTrue(m.parentByName.containsKey("carriage"));
        assertEquals("carriage", m.parentByName.get("turret"));
        assertEquals("turret", m.parentByName.get("barrel"));
        assertEquals("barrel", m.parentByName.get("muzzle"));
        assertEquals("carriage", m.parentByName.get("p_driver"));
        assertTrue(WheelSpin.bones(type).isEmpty());
        seats(m, type);
        basics(m);
        // The pivots TurretModel computes the muzzle from.
        TurretModel.Gun gun = TurretModel.FIELD_GUN;
        assertArrayEquals(new double[]{0, gun.pivotUp() * 16, -gun.pivotForward() * 16}, m.origin("barrel"), 1e-6);
        assertArrayEquals(new double[]{0, gun.pivotUp() * 16, -(gun.pivotForward() + gun.length()) * 16}, m.origin("muzzle"), 1e-6);
        assertTrue(m.size(0) >= 36 && m.size(0) <= 48, "width " + m.size(0));
        assertTrue(m.size(2) >= 85 && m.size(2) <= 110, "length " + m.size(2));
        assertTrue(type.halfWidth * 32 <= m.size(0), "hit box wider than the model");
        assertTrue(type.halfLength * 32 <= m.size(2), "hit box longer than the model");
        assertTrue(type.height * 16 >= m.size(1), "hit box lower than the model");
    }

    @Test
    void everyTypeHasAModelAndBothNames() throws Exception {
        for (VehicleType t : VehicleType.values()) {
            new Model("models/" + t.model() + ".bbmodel");
            for (String lang : new String[]{"en", "ru"}) {
                try (InputStream in = getClass().getClassLoader().getResourceAsStream("lang/" + lang + ".yml")) {
                    String yml = new String(in.readAllBytes(), StandardCharsets.UTF_8);
                    assertTrue(yml.contains("\n    " + t.id + ": \""), lang + ".yml has no name for " + t.id);
                }
            }
        }
    }

    /** Bones, pivots and seats every tank model must have, whichever look it is. */
    private static Model tank(String path, VehicleType type) throws Exception {
        Model m = new Model(path);
        assertNull(m.parentByName.get("hull"));
        assertTrue(m.parentByName.containsKey("hull"));
        assertEquals("hull", m.parentByName.get("turret"));
        assertEquals("turret", m.parentByName.get("barrel"));
        assertEquals("barrel", m.parentByName.get("muzzle"));
        assertEquals("hull", m.parentByName.get("p_driver"));
        assertEquals("turret", m.parentByName.get("sp_gunner"));
        for (String w : WheelSpin.bones(type)) {
            assertEquals("hull", m.parentByName.get(w), w);
        }
        seats(m, type);
        wheels(m, type);
        // The pivots TurretModel computes the muzzle from.
        assertArrayEquals(new double[]{0, TurretModel.TURRET_PIVOT_UP * 16, 0}, m.origin("turret"), 1e-6);
        assertArrayEquals(new double[]{0, TurretModel.BARREL_PIVOT_UP * 16, -TurretModel.BARREL_PIVOT_FORWARD * 16}, m.origin("barrel"), 1e-6);
        assertArrayEquals(new double[]{0, TurretModel.BARREL_PIVOT_UP * 16,
                -(TurretModel.BARREL_PIVOT_FORWARD + TurretModel.BARREL_LENGTH) * 16}, m.origin("muzzle"), 1e-6);
        // The gunner sits on the turret pivot, so turning the turret doesn't leave the seat behind.
        assertEquals(0, m.origin("sp_gunner")[0], 1e-6);
        assertEquals(0, m.origin("sp_gunner")[2], 1e-6);
        basics(m);
        // Hit box (blocks) should still sit inside the drawn hull.
        assertTrue(type.halfWidth * 32 <= m.size(0), type + " hit box wider than the model");
        assertTrue(type.halfLength * 32 <= m.size(2), type + " hit box longer than the model");
        return m;
    }

    private static Model jeep(String path, VehicleType type) throws Exception {
        Model m = new Model(path);
        assertNull(m.parentByName.get("body"));
        assertTrue(m.parentByName.containsKey("body"));
        for (String w : new String[]{"p_driver", "sp_passenger1", "sp_passenger2"}) {
            assertEquals("body", m.parentByName.get(w), w);
        }
        for (String w : WheelSpin.bones(type)) {
            assertEquals("body", m.parentByName.get(w), w);
        }
        seats(m, type);
        wheels(m, type);
        basics(m);
        assertTrue(type.halfWidth * 32 <= m.size(0), type + " hit box wider than the model");
        assertTrue(type.halfLength * 32 <= m.size(2), type + " hit box longer than the model");
        return m;
    }

    /** Every seat the code uses has a bone: the driver a p_ (seat) bone, the others p_ or sp_. */
    private static void seats(Model m, VehicleType type) {
        for (CrewSeats.Seat seat : CrewSeats.seatsOf(type)) {
            boolean p = m.parentByName.containsKey("p_" + seat.bone);
            boolean sp = m.parentByName.containsKey("sp_" + seat.bone);
            assertTrue(seat == CrewSeats.Seat.DRIVER ? p : p || sp, type + " has no bone for seat " + seat);
        }
    }

    /** Each rolling wheel has cubes centred on its pivot and is as tall as WheelSpin's radius says. */
    private static void wheels(Model m, VehicleType type) {
        for (String w : WheelSpin.bones(type)) {
            double[] o = m.origin(w);
            double[] box = m.unturnedBox(w);
            assertNotNull(box, w + " has no cubes");
            assertEquals(WheelSpin.radius(type) * 32, box[3] - box[1], 1e-3, w + " height");
            assertEquals(o[1], (box[1] + box[3]) / 2, 1e-3, w + " pivot height");
            assertEquals(o[2], (box[2] + box[4]) / 2, 1e-3, w + " pivot position");
        }
    }

    private static void basics(Model m) throws Exception {
        assertEquals(1, m.json.getAsJsonArray("textures").size());
        assertEquals(0, m.json.getAsJsonArray("animations").size());
        JsonObject tex = m.json.getAsJsonArray("textures").get(0).getAsJsonObject();
        int w = tex.get("uv_width").getAsInt();
        int h = tex.get("uv_height").getAsInt();
        String src = tex.get("source").getAsString();
        assertTrue(src.startsWith("data:image/png;base64,"));
        BufferedImage img = ImageIO.read(new ByteArrayInputStream(Base64.getDecoder().decode(src.substring(src.indexOf(',') + 1))));
        assertNotNull(img, "texture is not a PNG");
        assertEquals(w, img.getWidth());
        assertEquals(h, img.getHeight());
        assertEquals(w, m.json.getAsJsonObject("resolution").get("width").getAsInt());
        for (JsonElement e : m.json.getAsJsonArray("elements")) {
            JsonObject o = e.getAsJsonObject();
            String name = o.get("name").getAsString();
            if (o.has("rotation")) {
                int turned = 0;
                for (JsonElement r : o.getAsJsonArray("rotation")) {
                    double a = r.getAsDouble();
                    if (a != 0) {
                        turned++;
                        assertTrue(TURNS.contains(a), name + " turned by " + a + " (BetterModel would split it off)");
                    }
                }
                assertTrue(turned <= 1, name + " turned about more than one axis");
            }
            for (var f : o.getAsJsonObject("faces").entrySet()) {
                var uv = f.getValue().getAsJsonObject().getAsJsonArray("uv");
                for (int i = 0; i < 4; i++) {
                    double v = uv.get(i).getAsDouble();
                    assertTrue(v >= 0 && v <= (i % 2 == 0 ? w : h), name + " " + f.getKey() + " uv off the texture");
                }
            }
        }
    }
}
