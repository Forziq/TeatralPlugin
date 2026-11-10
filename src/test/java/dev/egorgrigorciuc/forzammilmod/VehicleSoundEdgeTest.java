package dev.egorgrigorciuc.forzammilmod;

import dev.egorgrigorciuc.forzammilmod.SoundLoop.Action;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class VehicleSoundEdgeTest {

    @Test
    void loopRestartsAfterStopAndNoReplayWhileStopped() {
        SoundLoop l = new SoundLoop(80);
        assertEquals(Action.PLAY, l.update(true, 0));
        assertEquals(Action.STOP, l.update(false, 10));
        for (long t = 11; t < 300; t++) assertEquals(Action.NONE, l.update(false, t));
        assertEquals(Action.PLAY, l.update(true, 300));
        assertTrue(l.playing());
        assertEquals(Action.NONE, l.update(true, 301));
    }

    @Test
    void loopReplaysExactlyEveryLengthMinusTwoOverCycles() {
        SoundLoop l = new SoundLoop(80);
        int period = 80 - SoundLoop.REPLAY_EARLY_TICKS;
        for (int cycle = 0; cycle < 5; cycle++) {
            long start = (long) cycle * period;
            assertEquals(Action.PLAY, l.update(true, start), "cycle " + cycle);
            for (long t = start + 1; t < start + period; t++) {
                assertEquals(Action.NONE, l.update(true, t), "tick " + t);
            }
        }
    }

    @Test
    void loopReplayIsRelativeToLateCall() {
        SoundLoop l = new SoundLoop(80);
        l.update(true, 0);
        assertEquals(Action.PLAY, l.update(true, 100)); // late: replays at once
        assertEquals(Action.NONE, l.update(true, 177));
        assertEquals(Action.PLAY, l.update(true, 178));
    }

    @Test
    void loopVeryShortLengthStillAdvances() {
        SoundLoop l = new SoundLoop(1);
        assertEquals(Action.PLAY, l.update(true, 0));
        assertEquals(Action.PLAY, l.update(true, 1));
    }

    @Test
    void hysteresisDoesNotFlickerAroundThresholds() {
        Hysteresis h = new Hysteresis(0.3, 0.1, 3);
        assertFalse(h.update(0.29)); // just under on
        assertTrue(h.update(0.3));   // exactly on switches on
        for (int i = 0; i < 20; i++) assertTrue(h.update(0.1)); // exactly off threshold is not below off
        for (int i = 0; i < 20; i++) assertTrue(h.update(i % 2 == 0 ? 0.05 : 0.15)); // alternating never holds
    }

    @Test
    void hysteresisSwitchesOffOnlyAfterExactHold() {
        Hysteresis h = new Hysteresis(0.3, 0.1, 4);
        assertTrue(h.update(1));
        assertTrue(h.update(0));
        assertTrue(h.update(0));
        assertTrue(h.update(0));
        assertFalse(h.update(0)); // 4th quiet tick
        assertFalse(h.update(0.2)); // between thresholds does not re-arm
        assertTrue(h.update(0.3));
    }

    @Test
    void cooldownBoundaries() {
        TickCooldown c = new TickCooldown(20);
        assertTrue(c.tryUse(0));
        assertFalse(c.tryUse(19));
        assertTrue(c.tryUse(20));
        assertFalse(c.tryUse(39));
        assertTrue(c.tryUse(40));
        assertFalse(c.tryUse(59)); // failed tries must not extend it
        assertTrue(c.tryUse(60));
    }

    @Test
    void reloadFinishesExactlyOncePerReload() {
        ReloadTimer r = new ReloadTimer(80);
        assertTrue(r.tryFire(0));
        assertFalse(r.pollFinished(79));
        assertTrue(r.pollFinished(80));
        assertFalse(r.pollFinished(80));
        assertFalse(r.pollFinished(500));
        assertTrue(r.tryFire(500));
        assertFalse(r.tryFire(501)); // refused shot does not queue another notice
        assertTrue(r.pollFinished(580));
        assertFalse(r.pollFinished(581));
    }

    @Test
    void reloadPollDoesNotFireWithoutShot() {
        ReloadTimer r = new ReloadTimer(80);
        for (long t = 0; t < 200; t += 10) assertFalse(r.pollFinished(t));
    }

    @Test
    void revFiresOnlyOnStoppedToMovingEdge() {
        VehicleSoundState s = new VehicleSoundState(true);
        assertTrue(s.update(100, true, true, 0, 0).rev());
        // held throttle never re-fires, even after the cooldown
        for (long t = 101; t < 300; t++) assertFalse(s.update(t, true, true, 0, 0).rev());
        s.update(300, true, false, 0, 0);
        assertTrue(s.update(301, true, true, 0, 0).rev());
    }

    @Test
    void revCooldownBoundaryAndNotWhenEmpty() {
        VehicleSoundState s = new VehicleSoundState(true);
        assertTrue(s.update(100, true, true, 0, 0).rev());
        s.update(101, true, false, 0, 0);
        assertFalse(s.update(139, true, true, 0, 0).rev());
        s.update(140, true, false, 0, 0);
        assertTrue(s.update(141, true, true, 0, 0).rev()); // 100 + 40 passed
        VehicleSoundState e = new VehicleSoundState(true);
        assertFalse(e.update(100, false, true, 0, 0).rev());
    }

    @Test
    void hornCooldownBoundary() {
        VehicleSoundState s = new VehicleSoundState(false);
        assertTrue(s.horn(0));
        assertFalse(s.horn(19));
        assertTrue(s.horn(20));
    }

    @Test
    void tracksStopWhenTankEmpties() {
        VehicleSoundState s = new VehicleSoundState(true);
        assertEquals(Action.PLAY, s.update(1, true, true, 0.05, 0).tracks());
        assertEquals(Action.STOP, s.update(2, false, true, 0.05, 0).tracks());
        assertEquals(Action.NONE, s.update(3, false, false, 0, 0).tracks());
    }
}
