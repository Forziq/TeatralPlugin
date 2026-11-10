package dev.egorgrigorciuc.forzammilmod;

import dev.egorgrigorciuc.forzammilmod.SoundLoop.Action;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class VehicleSoundTest {

    @Test
    void loopStartsReplaysEarlyAndStops() {
        SoundLoop l = new SoundLoop(80);
        assertEquals(Action.NONE, l.update(false, 0));
        assertEquals(Action.PLAY, l.update(true, 1));
        assertEquals(Action.NONE, l.update(true, 2));
        assertEquals(Action.NONE, l.update(true, 78));
        assertEquals(Action.PLAY, l.update(true, 79)); // 1 + (80 - 2)
        assertEquals(Action.STOP, l.update(false, 80));
        assertEquals(Action.NONE, l.update(false, 81));
        assertFalse(l.playing());
        assertEquals(Action.PLAY, l.update(true, 82));
    }

    @Test
    void hysteresisHoldsBeforeSwitchingOff() {
        Hysteresis h = new Hysteresis(0.3, 0.1, 3);
        assertFalse(h.update(0.2));
        assertTrue(h.update(0.5));
        assertTrue(h.update(0.2)); // between thresholds: stays on
        assertTrue(h.update(0.0));
        assertTrue(h.update(0.0));
        assertFalse(h.update(0.0));
        assertTrue(h.update(-0.5)); // sign ignored
    }

    @Test
    void hysteresisQuietCountResetsWhenValueRises() {
        Hysteresis h = new Hysteresis(0.3, 0.1, 3);
        h.update(1);
        h.update(0);
        h.update(0);
        h.update(0.2);
        assertTrue(h.update(0));
        assertTrue(h.update(0));
        assertFalse(h.update(0));
    }

    @Test
    void cooldownBlocksUntilTicksPass() {
        TickCooldown c = new TickCooldown(20);
        assertTrue(c.tryUse(5));
        assertFalse(c.tryUse(24));
        assertTrue(c.tryUse(25));
    }

    @Test
    void tankIdlesWhileOccupiedAndStopsWhenEmpty() {
        VehicleSoundState s = new VehicleSoundState(true);
        assertEquals(Action.NONE, s.update(1, false, false, 0, 0).engine());
        assertEquals(Action.PLAY, s.update(2, true, false, 0, 0).engine());
        assertEquals(Action.STOP, s.update(3, false, false, 0, 0).engine());
    }

    @Test
    void revOnlyOnThrottleEdgeWithCooldown() {
        VehicleSoundState s = new VehicleSoundState(true);
        assertFalse(s.update(1, true, false, 0, 0).rev());
        assertTrue(s.update(2, true, true, 0, 0).rev());
        assertFalse(s.update(3, true, true, 0, 0).rev());
        assertFalse(s.update(4, true, false, 0, 0).rev());
        assertFalse(s.update(5, true, true, 0, 0).rev()); // inside the cooldown
        s.update(6, true, false, 0, 0);
        assertTrue(s.update(60, true, true, 0, 0).rev());
    }

    @Test
    void tracksFollowMovementAndJeepHasNone() {
        VehicleSoundState tank = new VehicleSoundState(true);
        assertEquals(Action.PLAY, tank.update(1, true, true, 0.05, 0).tracks());
        VehicleSoundState jeep = new VehicleSoundState(false);
        assertEquals(Action.NONE, jeep.update(1, true, true, 0.3, 5).tracks());
        assertEquals(Action.NONE, jeep.update(2, true, true, 0.3, 5).turret());
        assertFalse(jeep.update(60, true, true, 0.3, 0).rev());
    }

    @Test
    void turretLoopThenStopSoundOnce() {
        VehicleSoundState s = new VehicleSoundState(true);
        assertEquals(Action.PLAY, s.update(1, true, false, 0, 1.5).turret());
        boolean stop = false;
        int stops = 0;
        for (int t = 2; t < 20; t++) {
            VehicleSoundState.Out o = s.update(t, true, false, 0, 0);
            if (o.turretStop()) stops++;
        }
        assertEquals(1, stops);
    }

    @Test
    void noTurretStopSoundWhenVehicleEmpties() {
        VehicleSoundState s = new VehicleSoundState(true);
        s.update(1, true, false, 0, 1.5);
        VehicleSoundState.Out o = s.update(2, false, false, 0, 1.5);
        assertEquals(Action.STOP, o.turret());
        assertFalse(o.turretStop());
    }

    @Test
    void hornHasCooldown() {
        VehicleSoundState s = new VehicleSoundState(false);
        assertTrue(s.horn(10));
        assertFalse(s.horn(20));
        assertTrue(s.horn(30));
    }

    @Test
    void moveMetricCountsSpotTurns() {
        assertEquals(0.18, VehicleSoundState.moveMetric(-0.18, 0), 1e-9);
        assertTrue(VehicleSoundState.moveMetric(0, 2.5f) >= VehicleSoundState.MOVE_ON);
    }

    @Test
    void reloadFinishedReportedOnce() {
        ReloadTimer r = new ReloadTimer(80);
        assertFalse(r.pollFinished(0));
        assertTrue(r.tryFire(10));
        assertFalse(r.pollFinished(50));
        assertTrue(r.pollFinished(90));
        assertFalse(r.pollFinished(91));
    }
}
