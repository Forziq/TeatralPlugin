package dev.egorgrigorciuc.forzammilmod;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class MachineGunTest {

    /** Runs a burst from tick {@code start} and returns the tick of each bullet. */
    private static java.util.List<Long> burst(MachineGun g, long start) {
        assertTrue(g.tryStart(start));
        java.util.List<Long> shots = new java.util.ArrayList<>();
        for (long t = start; t < start + 40 && (g.bursting() || t == start); t++) {
            for (int i = g.shotsDue(t); i > 0; i--) shots.add(t);
        }
        return shots;
    }

    @Test
    void aBurstIsFiveBulletsOverHalfASecond() {
        MachineGun g = new MachineGun();
        var shots = burst(g, 100);
        assertEquals(MachineGun.BURST_SHOTS, shots.size());
        assertEquals(100, shots.get(0), "first bullet the tick you click");
        assertTrue(shots.get(shots.size() - 1) - shots.get(0) <= MachineGun.BURST_TICKS);
        assertFalse(g.bursting());
        assertEquals(0, g.shotsDue(200));
    }

    @Test
    void cooldownRunsFromTheEndOfTheBurst() {
        MachineGun g = new MachineGun();
        burst(g, 0);
        long ready = MachineGun.BURST_TICKS + MachineGun.COOLDOWN_TICKS;
        assertFalse(g.ready(ready - 1));
        assertFalse(g.tryStart(ready - 1));
        assertTrue(g.ready(ready));
        assertTrue(g.tryStart(ready));
    }

    @Test
    void clickDuringABurstDoesNothing() {
        MachineGun g = new MachineGun();
        assertTrue(g.tryStart(5));
        assertFalse(g.tryStart(6));
        g.shotsDue(5);
        assertFalse(g.tryStart(7));
    }

    @Test
    void lateTickCatchesUp() {
        MachineGun g = new MachineGun();
        g.tryStart(0);
        assertEquals(MachineGun.BURST_SHOTS, g.shotsDue(50), "a skipped tick fires what was due");
        assertFalse(g.bursting());
    }

    @Test
    void cancelStopsTheBurst() {
        MachineGun g = new MachineGun();
        g.tryStart(0);
        g.shotsDue(0);
        g.cancel();
        assertEquals(0, g.shotsDue(4));
        assertFalse(g.tryStart(5), "the cooldown still holds");
    }

    @Test
    void firstClickIsNeverBlocked() {
        assertTrue(new MachineGun().tryStart(0));
    }

    @Test
    void noseDirection() {
        double[] fwd = MachineGun.direction(0f, 0f);
        assertArrayEquals(new double[] {0, 0, 1}, fwd, 1e-9);
        double[] east = MachineGun.direction(90f, 0f); // yaw 90 faces -X, as DriveModel
        assertArrayEquals(new double[] {-1, 0, 0}, east, 1e-9);
        double[] up = MachineGun.direction(0f, 30f);
        assertEquals(0.5, up[1], 1e-9);
        double len = Math.sqrt(up[0] * up[0] + up[1] * up[1] + up[2] * up[2]);
        assertEquals(1.0, len, 1e-9);
    }

    @Test
    void damageNumbers() {
        assertEquals(3.0, MachineGun.LIVING_DAMAGE);
        assertEquals(2, MachineGun.VEHICLE_DAMAGE);
        assertEquals(60.0, MachineGun.RANGE);
        // A full burst does 15 to a player (7.5 hearts) and 10 to a vehicle, a quarter of the plane's 40 health.
        assertEquals(10, MachineGun.BURST_SHOTS * MachineGun.VEHICLE_DAMAGE);
        assertEquals(40, VehicleType.PLANE.maxHealth);
    }
}
