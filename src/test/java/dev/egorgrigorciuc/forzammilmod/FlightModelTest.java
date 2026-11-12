package dev.egorgrigorciuc.forzammilmod;

import dev.egorgrigorciuc.forzammilmod.FlightModel.Input;
import dev.egorgrigorciuc.forzammilmod.FlightModel.Outcome;
import dev.egorgrigorciuc.forzammilmod.FlightModel.Result;
import dev.egorgrigorciuc.forzammilmod.FlightModel.State;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/** Flies the plane over a fake flat world: ground at y 64 (blocks up to y 63 solid), plus an optional wall. */
class FlightModelTest {

    private static final class World implements Terrain {
        int wallFromZ = Integer.MAX_VALUE;
        int unloadedFromZ = Integer.MAX_VALUE;

        @Override
        public double[] span(int x, int y, int z) {
            boolean solid = y <= 63 || (z >= wallFromZ && y < 200);
            return solid ? new double[] {y, y + 1} : null;
        }

        @Override
        public boolean loaded(int x, int z) {
            return z < unloadedFromZ;
        }
    }

    private static Input pilot(int throttle, int steer, int pitch) {
        return new Input(throttle, steer, pitch, true);
    }

    private static State air(double y, double speed, float pitch, double throttle) {
        return new State(0.5, y, 0.5, 0f, pitch, 0f, speed, throttle, 0, false, false);
    }

    /** Steps until something other than NONE happens; fails if nothing does in {@code max} ticks. */
    private static Result until(State s, Input in, World w, int max) {
        for (int i = 0; i < max; i++) {
            Result r = FlightModel.step(s, in, w);
            if (r.outcome() != Outcome.NONE) return r;
            s = r.state();
        }
        fail("nothing happened in " + max + " ticks");
        return null;
    }

    @Test
    void throttleStaysBetweenIdleAndFull() {
        World w = new World();
        State s = State.parked(0.5, 64, 0.5, 0f);
        for (int i = 0; i < 200; i++) {
            s = FlightModel.step(s, pilot(1, 0, 0), w).state();
            assertTrue(s.throttle() <= 1.0 && s.throttle() >= 0);
        }
        assertEquals(1.0, s.throttle(), 1e-9);
        assertTrue(s.speed() <= FlightModel.MAX_SPEED + 1e-9);
        for (int i = 0; i < 200; i++) {
            s = FlightModel.step(s, pilot(-1, 0, 0), w).state();
            assertTrue(s.throttle() >= 0);
        }
        assertEquals(0.0, s.throttle(), 1e-9);
        assertTrue(s.onGround(), "no Space never lifts off");
    }

    @Test
    void staysOnTheGroundBelowTakeoffSpeed() {
        World w = new World();
        // Half throttle holds 0.5 blocks a tick, just under take-off speed; the pilot holds Space.
        State s = new State(0.5, 64, 0.5, 0f, 0f, 0f, 0.5, 0.5, 0, true, false);
        for (int i = 0; i < 200; i++) {
            Result r = FlightModel.step(s, pilot(0, 0, 1), w);
            assertNotEquals(Outcome.TAKEOFF, r.outcome());
            s = r.state();
            assertTrue(s.onGround());
            assertEquals(64, s.y(), 1e-9);
        }
        assertTrue(s.speed() < FlightModel.TAKEOFF_SPEED);
    }

    @Test
    void liftsOffOnlyOnceFastEnough() {
        World w = new World();
        State s = State.parked(0.5, 64, 0.5, 0f);
        Result r = null;
        for (int i = 0; i < 400; i++) {
            State before = s;
            r = FlightModel.step(s, pilot(1, 0, 1), w);
            s = r.state();
            if (r.outcome() == Outcome.TAKEOFF) {
                assertTrue(before.onGround());
                assertTrue(s.speed() >= FlightModel.TAKEOFF_SPEED, "lifted off at " + s.speed());
                break;
            }
            assertTrue(s.onGround(), "airborne before take-off speed, at " + s.speed());
        }
        assertEquals(Outcome.TAKEOFF, r.outcome());
        assertFalse(s.onGround());
        for (int i = 0; i < 14; i++) s = FlightModel.step(s, pilot(0, 0, 1), w).state();
        for (int i = 0; i < 46; i++) s = FlightModel.step(s, pilot(0, 0, 0), w).state();
        assertTrue(s.y() > 68, "climbing away, at " + s.y());
        assertFalse(s.stalled());
    }

    @Test
    void spaceClimbsSprintDivesNeitherLevels() {
        World w = new World();
        State up = air(150, 0.9, 0f, 1.0), down = air(150, 0.9, 0f, 1.0), level = air(150, 0.9, 20f, 1.0);
        for (int i = 0; i < 30; i++) {
            up = FlightModel.step(up, pilot(1, 0, 1), w).state();
            down = FlightModel.step(down, pilot(1, 0, -1), w).state();
            level = FlightModel.step(level, pilot(1, 0, 0), w).state();
        }
        assertEquals(FlightModel.PITCH_MAX, up.pitch(), 1e-6);
        assertTrue(up.y() > 150);
        assertEquals(FlightModel.PITCH_MIN, down.pitch(), 1e-6);
        assertTrue(down.y() < 150);
        assertEquals(0f, level.pitch(), 1e-6, "neither key eases the nose back to level");
        // Holding both is the same as neither: the keys cancel out in DriveInput.
        assertEquals(0, new DriveInput(false, false, false, false, true, true).pitch());
        assertEquals(1, new DriveInput(false, false, false, false, true, false).pitch());
        assertEquals(-1, new DriveInput(false, false, false, false, false, true).pitch());
    }

    @Test
    void noseMovesAtTheSetRate() {
        World w = new World();
        State s = FlightModel.step(air(150, 0.9, 0f, 1.0), pilot(1, 0, 1), w).state();
        assertEquals(FlightModel.PITCH_RATE, s.pitch(), 1e-6);
    }

    @Test
    void sprintNeverPushesTheNoseBelowLevelOnTheGround() {
        World w = new World();
        State s = State.parked(0.5, 64, 0.5, 0f);
        for (int i = 0; i < 40; i++) {
            s = FlightModel.step(s, pilot(1, 0, -1), w).state();
            assertTrue(s.pitch() >= 0f, "pitch " + s.pitch());
        }
        // Space on the ground raises the nose only to the ground limit, and letting go settles it back.
        for (int i = 0; i < 40; i++) s = FlightModel.step(s, pilot(0, 0, 1), w).state();
        if (s.onGround()) assertEquals(FlightModel.GROUND_PITCH_MAX, s.pitch(), 1e-6);
        State parked = State.parked(0.5, 64, 0.5, 0f);
        for (int i = 0; i < 10; i++) parked = FlightModel.step(parked, pilot(0, 0, 1), w).state();
        assertEquals(FlightModel.GROUND_PITCH_MAX, parked.pitch(), 1e-6);
        for (int i = 0; i < 10; i++) parked = FlightModel.step(parked, pilot(0, 0, 0), w).state();
        assertEquals(0f, parked.pitch(), 1e-6);
    }

    @Test
    void takeOffWithSpaceAtSpeed() {
        World w = new World();
        State s = new State(0.5, 64, 0.5, 0f, 0f, 0f, 0.6, 0.8, 0, true, false);
        Result r = null;
        for (int i = 0; i < 20; i++) {
            r = FlightModel.step(s, pilot(1, 0, 1), w);
            s = r.state();
            if (r.outcome() == Outcome.TAKEOFF) break;
        }
        assertEquals(Outcome.TAKEOFF, r.outcome(), "fast enough and Space held lifts off");
        // Without Space it stays on the wheels at the same speed.
        State g = new State(0.5, 64, 0.5, 0f, 0f, 0f, 0.6, 0.8, 0, true, false);
        for (int i = 0; i < 20; i++) {
            Result n = FlightModel.step(g, pilot(1, 0, 0), w);
            assertNotEquals(Outcome.TAKEOFF, n.outcome());
            g = n.state();
        }
    }

    @Test
    void levelFlightHoldsHeightAndSpeed() {
        World w = new World();
        State s = air(120, FlightModel.MAX_SPEED, 0f, 1.0);
        for (int i = 0; i < 100; i++) s = FlightModel.step(s, pilot(0, 0, 0), w).state();
        assertEquals(120, s.y(), 1e-6);
        assertEquals(FlightModel.MAX_SPEED, s.speed(), 1e-6);
        assertEquals(100.5, s.z(), 1e-6);
    }

    @Test
    void stallDropsTheNoseAndFalls() {
        World w = new World();
        // Too slow, engine idle, and the pilot tries to hold the nose up: the wing gives up anyway.
        State s = air(150, 0.3, 0f, 0.0);
        s = FlightModel.step(s, pilot(0, 0, 1), w).state();
        assertTrue(s.stalled());
        double earlySink = s.verticalSpeed();
        for (int i = 0; i < 14; i++) s = FlightModel.step(s, pilot(0, 0, 1), w).state();
        assertTrue(s.pitch() <= -20f, "nose should have dropped, pitch " + s.pitch());
        assertTrue(s.verticalSpeed() < earlySink - 0.3, "falling faster and faster");
        assertTrue(s.y() < 145);
    }

    @Test
    void stallRecoversWithSpeed() {
        World w = new World();
        State s = air(250, 0.3, 0f, 1.0);
        s = FlightModel.step(s, pilot(0, 0, 0), w).state();
        assertTrue(s.stalled());
        for (int i = 0; i < 200; i++) s = FlightModel.step(s, pilot(0, 0, 0), w).state();
        assertFalse(s.stalled(), "full throttle and a dropped nose bring the speed back");
        assertTrue(s.speed() > FlightModel.UNSTALL_SPEED);
        assertTrue(s.y() > 64, "recovered before hitting the ground");
    }

    @Test
    void slowShallowTouchdownLands() {
        World w = new World();
        State s = air(66, 0.6, -5f, 0.5);
        // The pilot holds a shallow descent (about 5 degrees nose down) with Sprint and Space, then flares.
        Result r = null;
        for (int i = 0; i < 300; i++) {
            int key = s.pitch() > -5f ? -1 : s.pitch() < -5f ? 1 : 0;
            r = FlightModel.step(s, pilot(0, 0, key), w);
            if (r.outcome() != Outcome.NONE) break;
            s = r.state();
        }
        assertEquals(Outcome.LANDED, r.outcome());
        assertTrue(r.state().onGround());
        assertEquals(64, r.state().y(), 1e-9);
        // And it rolls on without further drama, slowing with the engine at idle.
        s = r.state();
        for (int i = 0; i < 400; i++) {
            Result next = FlightModel.step(s, pilot(-1, 0, 0), w);
            assertNotEquals(Outcome.CRASHED, next.outcome());
            s = next.state();
        }
        assertTrue(s.onGround());
        assertEquals(0, s.speed(), 1e-9);
    }

    @Test
    void steepDiveIntoTheGroundCrashes() {
        World w = new World();
        Result r = until(air(80, 1.2, -30f, 1.0), pilot(0, 0, -1), w, 300);
        assertEquals(Outcome.CRASHED, r.outcome());
    }

    @Test
    void touchingDownTooFastCrashes() {
        World w = new World();
        // A gentle descent, but at full speed.
        Result r = until(air(64.5, FlightModel.MAX_SPEED, -3f, 1.0), pilot(0, 0, -1), w, 300);
        assertEquals(Outcome.CRASHED, r.outcome());
    }

    @Test
    void stalledFallCrashes() {
        World w = new World();
        Result r = until(air(100, 0.0, 0f, 0.0), pilot(0, 0, 0), w, 400);
        assertEquals(Outcome.CRASHED, r.outcome());
        assertTrue(-r.state().verticalSpeed() > FlightModel.LAND_MAX_SINK);
    }

    @Test
    void flyingIntoAWallCrashes() {
        World w = new World();
        w.wallFromZ = 40;
        Result r = until(air(90, FlightModel.MAX_SPEED, 0f, 1.0), pilot(0, 0, 0), w, 200);
        assertEquals(Outcome.CRASHED, r.outcome());
        assertTrue(r.state().z() + VehicleType.PLANE.halfLength <= 40 + 1e-6, "stopped at the wall, not inside it");
    }

    @Test
    void taxiingSlowlyIntoAWallJustStops() {
        World w = new World();
        w.wallFromZ = 10;
        State s = new State(0.5, 64, 0.5, 0f, 0f, 0f, 0.2, 0.2, 0, true, false);
        for (int i = 0; i < 200; i++) {
            Result r = FlightModel.step(s, pilot(0, 0, 0), w);
            assertNotEquals(Outcome.CRASHED, r.outcome());
            s = r.state();
        }
        assertTrue(s.z() + VehicleType.PLANE.halfLength <= 10 + 1e-6);
    }

    @Test
    void bankingTurnsTheRightWay() {
        World w = new World();
        State s = air(200, 0.9, 0f, 1.0);
        for (int i = 0; i < 30; i++) s = FlightModel.step(s, pilot(0, 1, 0), w).state();
        assertEquals(FlightModel.MAX_ROLL, s.roll(), 1e-6);
        assertTrue(s.yaw() > 30f, "D turns clockwise from above, yaw " + s.yaw());
        State l = air(200, 0.9, 0f, 1.0);
        for (int i = 0; i < 30; i++) l = FlightModel.step(l, pilot(0, -1, 0), w).state();
        assertEquals(-FlightModel.MAX_ROLL, l.roll(), 1e-6);
        assertTrue(l.yaw() < -30f, "A turns the other way, yaw " + l.yaw());
        // Letting go levels the wings and stops the turn.
        for (int i = 0; i < 30; i++) s = FlightModel.step(s, pilot(0, 0, 0), w).state();
        assertEquals(0f, s.roll(), 1e-6);
    }

    @Test
    void noPilotMeansTheEngineWindsDown() {
        World w = new World();
        State s = air(200, 0.9, 0f, 1.0);
        for (int i = 0; i < 40; i++) s = FlightModel.step(s, Input.NONE, w).state();
        assertEquals(0, s.throttle(), 1e-9);
        assertTrue(s.pitch() < 0, "an empty plane glides down");
    }

    @Test
    void waitsAtTheEdgeOfUnloadedLand() {
        World w = new World();
        w.unloadedFromZ = 30;
        State s = air(100, FlightModel.MAX_SPEED, 0f, 1.0);
        boolean waited = false;
        for (int i = 0; i < 100; i++) {
            Result r = FlightModel.step(s, pilot(0, 0, 0), w);
            assertNotEquals(Outcome.CRASHED, r.outcome());
            if (r.outcome() == Outcome.WAITING) waited = true;
            s = r.state();
        }
        assertTrue(waited);
        assertTrue(s.z() < 30, "never moved into unloaded land");
    }

    @Test
    void landingLimits() {
        assertTrue(FlightModel.gentle(FlightModel.LAND_MAX_SPEED, FlightModel.LAND_MAX_SINK, 0f, 0f));
        assertFalse(FlightModel.gentle(FlightModel.LAND_MAX_SPEED + 0.05, 0.1, 0f, 0f), "too fast");
        assertFalse(FlightModel.gentle(0.5, FlightModel.LAND_MAX_SINK + 0.05, 0f, 0f), "coming down too hard");
        assertFalse(FlightModel.gentle(0.5, 0.1, 45f, 0f), "wing tip first");
        assertFalse(FlightModel.gentle(0.5, 0.1, 0f, -30f), "nose first");
        // The numbers have to leave a window: you can land above stall speed.
        assertTrue(FlightModel.LAND_MAX_SPEED > FlightModel.UNSTALL_SPEED);
        assertTrue(FlightModel.TAKEOFF_SPEED > FlightModel.STALL_SPEED);
        assertTrue(FlightModel.MAX_SPEED > FlightModel.LAND_MAX_SPEED);
    }
}
