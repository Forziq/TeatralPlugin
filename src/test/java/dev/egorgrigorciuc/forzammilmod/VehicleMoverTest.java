package dev.egorgrigorciuc.forzammilmod;

import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

/** Drives vehicles over small fake worlds: flat ground at y 64 (blocks up to y 63 solid), plus extras. */
class VehicleMoverTest {

    private static final DriveInput W = new DriveInput(true, false, false, false);
    private static final double[] FULL = {0, 1};

    /** Flat world; extra solid blocks given as "x,y,z", plus optional "wall rows" across the whole width. */
    private static final class World implements Terrain {
        final Set<String> extra = new HashSet<>();
        int wallFromZ = Integer.MAX_VALUE;
        int wallHeight;

        @Override
        public double[] span(int x, int y, int z) {
            boolean solid = y <= 63 || extra.contains(x + "," + y + "," + z)
                    || (z >= wallFromZ && y >= 64 && y < 64 + wallHeight);
            return solid ? new double[] {y + FULL[0], y + FULL[1]} : null;
        }
    }

    private static VehicleMover.State drive(VehicleType t, Terrain w, VehicleMover.State s, DriveInput in, int ticks) {
        for (int i = 0; i < ticks; i++) s = VehicleMover.tick(t, w, s, in);
        return s;
    }

    private static VehicleMover.State at(double x, double y, double z) {
        return new VehicleMover.State(x, y, z, 0f, 0, 0);
    }

    @Test
    void drivesOnFlatGround() {
        for (VehicleType t : VehicleType.values()) {
            if (!t.moves()) continue; // the field gun does not drive
            VehicleMover.State s = drive(t, new World(), at(0.5, 64, 0.5), W, 100);
            assertEquals(64, s.y(), 1e-9);
            assertEquals(0.5, s.x(), 1e-9);
            assertTrue(s.z() > 5, t + " moved forward");
            assertEquals(t.maxSpeed, s.speed(), 1e-9);
        }
    }

    @Test
    void climbsOneBlockStep() {
        for (VehicleType t : VehicleType.values()) {
            if (!t.moves()) continue; // the field gun does not drive
            World w = new World();
            w.wallFromZ = 6;
            w.wallHeight = 1;
            VehicleMover.State s = drive(t, w, at(0.5, 64, 0.5), W, 200);
            assertEquals(65, s.y(), 1e-9, t + " should be on top of the step");
            assertTrue(s.z() > 6 + t.halfLength, t + " drove onto the step");
            assertTrue(s.speed() > 0);
        }
    }

    @Test
    void stopsAtTwoBlockWall() {
        for (VehicleType t : VehicleType.values()) {
            if (!t.moves()) continue; // the field gun does not drive
            World w = new World();
            w.wallFromZ = 6;
            w.wallHeight = 2;
            VehicleMover.State s = drive(t, w, at(0.5, 64, 0.5), W, 200);
            assertEquals(64, s.y(), 1e-9);
            assertTrue(s.z() + t.halfLength <= 6 + 1e-6, t + " front must not enter the wall");
            assertTrue(s.z() + t.halfLength > 6 - 0.5, t + " should get close to the wall");
            assertEquals(0, s.speed(), 1e-9);
        }
    }

    @Test
    void doesNotClimbUnderLowCeiling() {
        World w = new World();
        w.wallFromZ = 6;
        w.wallHeight = 1;
        for (int x = -5; x <= 5; x++) for (int z = 0; z <= 20; z++) w.extra.add(x + ",66," + z);
        VehicleType t = VehicleType.TANK; // 1.7 tall: on the step at 65 it would hit the ceiling at 66
        VehicleMover.State s = drive(t, w, at(0.5, 64, 0.5), W, 200);
        assertEquals(64, s.y(), 1e-9);
        assertTrue(s.z() + t.halfLength <= 6 + 1e-6);
    }

    @Test
    void fallsAndLandsWithoutSinking() {
        for (VehicleType t : VehicleType.values()) {
            VehicleMover.State s = at(0.5, 80, 0.5);
            double lowest = s.y();
            for (int i = 0; i < 100; i++) {
                s = VehicleMover.tick(t, new World(), s, DriveInput.NONE);
                lowest = Math.min(lowest, s.y());
            }
            assertEquals(64, s.y(), 1e-9);
            assertEquals(64, lowest, 1e-9, "never below the ground");
            assertEquals(0, s.vy(), 1e-9);
        }
    }

    @Test
    void drivesOffALedgeAndFalls() {
        // Raised platform at y 64 (top 65) for z < 4, ground at 64 beyond.
        World w = new World();
        for (int x = -5; x <= 5; x++) for (int z = -10; z < 4; z++) w.extra.add(x + ",64," + z);
        VehicleType t = VehicleType.JEEP;
        VehicleMover.State s = drive(t, w, at(0.5, 65, -5), W, 120);
        assertEquals(64, s.y(), 1e-9);
        assertTrue(s.z() > 4 + t.halfLength);
    }

    @Test
    void coastsToAStopWithoutDriver() {
        VehicleType t = VehicleType.JEEP;
        VehicleMover.State s = new VehicleMover.State(0.5, 64, 0.5, 0f, t.maxSpeed, 0);
        s = drive(t, new World(), s, DriveInput.NONE, 200);
        assertEquals(0, s.speed());
        assertEquals(64, s.y(), 1e-9);
    }

    @Test
    void canDriveOutWhenSpawnedInsideAWall() {
        World w = new World();
        for (int x = -5; x <= 5; x++) for (int y = 64; y <= 66; y++) w.extra.add(x + "," + y + ",0");
        VehicleType t = VehicleType.JEEP;
        VehicleMover.State s = drive(t, w, at(0.5, 64, 0.5), W, 60);
        assertTrue(s.z() > 0.5 + t.halfLength + 1, "escaped the wall");
        assertEquals(64, s.y(), 1e-9);
    }

    @Test
    void footprintCoversCornersWithTightSpacing() {
        VehicleType t = VehicleType.TANK;
        List<double[]> pts = VehicleMover.samples(t, 0, 0, 0);
        double minX = 1e9, maxX = -1e9, minZ = 1e9, maxZ = -1e9;
        for (double[] p : pts) {
            minX = Math.min(minX, p[0]);
            maxX = Math.max(maxX, p[0]);
            minZ = Math.min(minZ, p[1]);
            maxZ = Math.max(maxZ, p[1]);
        }
        assertEquals(-t.halfWidth, minX, 1e-9);
        assertEquals(t.halfWidth, maxX, 1e-9);
        assertEquals(-t.halfLength, minZ, 1e-9);
        assertEquals(t.halfLength, maxZ, 1e-9);
        double[][] local = VehicleMover.localSamples(t.halfWidth, t.halfLength);
        int nw = (int) Math.ceil(2 * t.halfWidth / VehicleMover.SAMPLE_SPACING) + 1;
        assertTrue(2 * t.halfWidth / (nw - 1) <= VehicleMover.SAMPLE_SPACING);
        assertEquals(local.length, pts.size());
    }

    @Test
    void footprintRotatesWithYaw() {
        VehicleType t = VehicleType.JEEP;
        // Facing west (yaw 90) the length runs along X.
        double maxX = -1e9;
        for (double[] p : VehicleMover.samples(t, 0, 0, 90)) maxX = Math.max(maxX, Math.abs(p[0]));
        assertEquals(t.halfLength, maxX, 1e-9);
    }
}
