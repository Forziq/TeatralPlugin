package dev.egorgrigorciuc.forzammilmod;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class DriveModelTest {

    private static final DriveInput W = new DriveInput(true, false, false, false);
    private static final DriveInput S = new DriveInput(false, true, false, false);
    private static final DriveInput A = new DriveInput(false, false, true, false);
    private static final DriveInput D = new DriveInput(false, false, false, true);
    private static final DriveInput WD = new DriveInput(true, false, false, true);

    @Test
    void accelerationCapsAtMaxSpeed() {
        for (VehicleType t : VehicleType.values()) {
            double s = 0;
            double prev = -1;
            for (int i = 0; i < 400; i++) {
                s = DriveModel.step(t, W, s, 0).speed();
                assertTrue(s >= prev, "speed never drops while accelerating");
                assertTrue(s <= t.maxSpeed + 1e-9, t + " over max");
                prev = s;
            }
            assertEquals(t.maxSpeed, s, 1e-9);
            assertEquals(t.acceleration, DriveModel.step(t, W, 0, 0).speed(), 1e-9);
        }
    }

    @Test
    void reverseCapsAtReverseMax() {
        for (VehicleType t : VehicleType.values()) {
            double s = 0;
            for (int i = 0; i < 400; i++) s = DriveModel.step(t, S, s, 0).speed();
            assertEquals(-t.reverseMax, s, 1e-9);
        }
    }

    @Test
    void brakingStopsWithoutOvershoot() {
        VehicleType t = VehicleType.TANK;
        double s = t.maxSpeed;
        int ticks = 0;
        while (s > 0) {
            s = DriveModel.step(t, S, s, 0).speed();
            ticks++;
            assertTrue(s >= 0, "braking must land on zero, not go backwards in the same tick");
        }
        assertEquals(0, s);
        assertEquals((int) Math.ceil(t.maxSpeed / t.braking), ticks);
        // Only after standing still does S start reversing.
        assertTrue(DriveModel.step(t, S, 0, 0).speed() < 0);
    }

    @Test
    void pressingForwardWhileReversingBrakesFirst() {
        VehicleType t = VehicleType.JEEP;
        double s = DriveModel.step(t, W, -t.reverseMax, 0).speed();
        assertEquals(-t.reverseMax + t.braking, s, 1e-9);
    }

    @Test
    void coastingStopsWithoutOvershoot() {
        for (VehicleType t : VehicleType.values()) {
            double s = t.maxSpeed;
            for (int i = 0; i < 500; i++) {
                s = DriveModel.step(t, DriveInput.NONE, s, 0).speed();
                assertTrue(s >= 0);
            }
            assertEquals(0, s);
            s = -t.reverseMax;
            for (int i = 0; i < 500; i++) s = DriveModel.step(t, DriveInput.NONE, s, 0).speed();
            assertEquals(0, s);
        }
    }

    @Test
    void tankTurnsOnTheSpot() {
        VehicleType t = VehicleType.TANK;
        assertEquals((float) t.turnRate, DriveModel.step(t, D, 0, 0).yaw(), 1e-4);
        assertEquals((float) -t.turnRate, DriveModel.step(t, A, 0, 0).yaw(), 1e-4);
        assertEquals(0, DriveModel.step(t, A, 0, 0).speed());
    }

    @Test
    void turningRatePerTick() {
        VehicleType t = VehicleType.TANK;
        float yaw = 0;
        for (int i = 0; i < 10; i++) yaw = DriveModel.step(t, D, 0, yaw).yaw();
        assertEquals((float) (10 * t.turnRate), yaw, 1e-3);
    }

    @Test
    void jeepTurnScalesWithSpeed() {
        VehicleType t = VehicleType.JEEP;
        assertEquals((float) t.turnRate, DriveModel.step(t, D, t.maxSpeed, 0).yaw(), 1e-4);
        double v = t.maxSpeed / 4;
        double afterDrag = v - t.coastDrag;
        float expected = (float) (t.turnRate * afterDrag / (t.maxSpeed * 0.5));
        assertEquals(expected, DriveModel.step(t, D, v, 0).yaw(), 1e-4);
    }

    @Test
    void jeepDoesNotTurnStandingStillButDoesWhenMoving() {
        VehicleType t = VehicleType.JEEP;
        assertEquals(0f, DriveModel.step(t, D, 0, 0).yaw());
        float atSpeed = DriveModel.step(t, WD, t.maxSpeed, 0).yaw();
        assertEquals((float) t.turnRate, atSpeed, 1e-4);
        float slow = DriveModel.step(t, D, t.maxSpeed * 0.1, 0).yaw();
        assertTrue(slow > 0 && slow < atSpeed, "slow jeep turns less");
        float reversing = DriveModel.step(t, D, -t.reverseMax, 0).yaw();
        assertTrue(reversing < 0, "reversing with right lock swings the nose left");
    }

    @Test
    void plannedDeltaFollowsYaw() {
        VehicleType t = VehicleType.JEEP;
        DriveModel.Result south = DriveModel.step(t, W, t.maxSpeed, 0);
        assertEquals(0, south.dx(), 1e-9);
        assertEquals(t.maxSpeed, south.dz(), 1e-9);
        DriveModel.Result west = DriveModel.step(t, W, t.maxSpeed, 90);
        assertEquals(-t.maxSpeed, west.dx(), 1e-9);
        assertEquals(0, west.dz(), 1e-9);
    }

    @Test
    void yawWraps() {
        assertEquals(-178f, DriveModel.wrap(182f), 1e-4);
        assertEquals(179f, DriveModel.wrap(-181f), 1e-4);
        assertEquals(-180f, DriveModel.wrap(180f), 1e-4);
    }

    @Test
    void stepDecision() {
        assertEquals(DriveModel.Step.MOVE, DriveModel.stepDecision(0, 1.0));
        assertEquals(DriveModel.Step.STEP_UP, DriveModel.stepDecision(0.5, 1.0));
        assertEquals(DriveModel.Step.STEP_UP, DriveModel.stepDecision(1.0, 1.0));
        assertEquals(DriveModel.Step.BLOCKED, DriveModel.stepDecision(1.5, 1.0));
        assertEquals(DriveModel.Step.BLOCKED, DriveModel.stepDecision(2.0, 1.0));
    }

    @Test
    void gravityAcceleratesAndCapsAtTerminal() {
        double vy = 0;
        vy = DriveModel.fallVelocity(vy);
        assertEquals(-0.08 * 0.98, vy, 1e-9);
        for (int i = 0; i < 200; i++) vy = DriveModel.fallVelocity(vy);
        assertEquals(-DriveModel.TERMINAL_FALL, vy, 1e-9);
    }

    @Test
    void fallLandsOnGroundInsteadOfPassingIt() {
        double[] r = DriveModel.fall(64.05, -1.5, 64);
        assertEquals(64, r[0]);
        assertEquals(0, r[1]);
        r = DriveModel.fall(64, -0.3, 64);
        assertArrayEquals(new double[] {64, 0}, r);
        r = DriveModel.fall(70, 0, 64);
        assertTrue(r[0] < 70 && r[0] > 64);
        r = DriveModel.fall(70, 0, Double.NEGATIVE_INFINITY);
        assertTrue(r[0] < 70);
    }
}
