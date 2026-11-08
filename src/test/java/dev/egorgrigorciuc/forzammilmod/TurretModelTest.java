package dev.egorgrigorciuc.forzammilmod;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class TurretModelTest {

    private static final float DT = 0.05f;
    private static final float EPS = 1e-3f;

    private static TurretModel at(float yaw) {
        TurretModel t = new TurretModel();
        t.tick(yaw, 0f, 100f); // big dt: jump straight there
        return t;
    }

    @Test
    void wrapPutsAnglesInHalfOpenRange() {
        assertEquals(-180f, TurretModel.wrap(180f), EPS);
        assertEquals(-170f, TurretModel.wrap(190f), EPS);
        assertEquals(170f, TurretModel.wrap(-190f), EPS);
        assertEquals(0f, TurretModel.wrap(720f), EPS);
        assertEquals(-180f, TurretModel.wrap(-180f), EPS);
    }

    @Test
    void turnsTheShortWayAcrossTheSeam() {
        TurretModel t = at(170f);
        t.tick(-170f, 0f, DT);
        assertEquals(172f, t.yaw(), EPS); // +2 deg in one tick, not -2 and not -340
        TurretModel u = at(-170f);
        u.tick(170f, 0f, DT);
        assertEquals(-172f, u.yaw(), EPS);
    }

    @Test
    void yawRateIsLimitedPerTick() {
        TurretModel t = new TurretModel();
        for (int i = 0; i < 20; i++) t.tick(90f, 0f, DT);
        assertEquals(40f, t.yaw(), EPS); // 1 second at 40 deg/s
    }

    @Test
    void reachesTargetWithoutOvershoot() {
        TurretModel t = new TurretModel();
        for (int i = 0; i < 100; i++) {
            t.tick(30f, 10f, DT);
            assertTrue(t.yaw() <= 30f + EPS);
            assertTrue(t.pitch() <= 10f + EPS);
        }
        assertEquals(30f, t.yaw(), EPS);
        assertEquals(10f, t.pitch(), EPS);
    }

    @Test
    void holdsWhenTargetEqualsCurrent() {
        TurretModel t = at(45f);
        t.tick(45f, 0f, DT);
        assertEquals(45f, t.yaw(), EPS);
    }

    @Test
    void pitchIsClampedBothEnds() {
        TurretModel t = new TurretModel();
        for (int i = 0; i < 200; i++) t.tick(0f, 80f, DT);
        assertEquals(20f, t.pitch(), EPS);
        for (int i = 0; i < 400; i++) t.tick(0f, -80f, DT);
        assertEquals(-5f, t.pitch(), EPS);
    }

    @Test
    void pitchRateIsLimited() {
        TurretModel t = new TurretModel();
        for (int i = 0; i < 20; i++) t.tick(0f, 20f, DT);
        assertEquals(20f, t.pitch(), EPS); // 25 deg/s would allow 25, the limit stops at 20
        TurretModel u = new TurretModel();
        for (int i = 0; i < 10; i++) u.tick(0f, 20f, DT);
        assertEquals(12.5f, u.pitch(), EPS);
    }

    @Test
    void signConversion() {
        assertEquals(15f, TurretModel.pitchUpFromMc(-15f), EPS); // looking up -> barrel up
        assertEquals(-5f, TurretModel.pitchUpFromMc(5f), EPS);
        assertEquals(-170f, TurretModel.relativeYaw(10f, 180f), EPS);
        assertEquals(20f, TurretModel.relativeYaw(-170f, 170f), EPS);
    }

    @Test
    void muzzleStraightAheadFacingSouth() {
        // yaw 0 faces +Z
        TurretModel.Muzzle m = TurretModel.muzzle(10, 64, 20, 0f, 0f, 0f);
        assertEquals(10, m.x(), 1e-6);
        assertEquals(64 + 21.5 / 16, m.y(), 1e-6);
        assertEquals(20 + 2.75, m.z(), 1e-6);
        assertEquals(1.0, m.dz(), 1e-6);
        assertEquals(0.0, m.dy(), 1e-6);
    }

    @Test
    void muzzleFollowsYawAndPitch() {
        // yaw 90 faces -X; turret +90 on top of hull 0 gives the same
        TurretModel.Muzzle m = TurretModel.muzzle(0, 0, 0, 0f, 90f, 0f);
        assertEquals(-2.75, m.x(), 1e-6);
        assertEquals(0.0, m.z(), 1e-6);
        assertEquals(-1.0, m.dx(), 1e-6);
        TurretModel.Muzzle up = TurretModel.muzzle(0, 0, 0, 0f, 0f, 20f);
        assertTrue(up.dy() > 0.3);
        assertEquals(1.0, Math.sqrt(up.dx() * up.dx() + up.dy() * up.dy() + up.dz() * up.dz()), 1e-6);
        assertTrue(up.y() > 21.5 / 16);
    }
}
