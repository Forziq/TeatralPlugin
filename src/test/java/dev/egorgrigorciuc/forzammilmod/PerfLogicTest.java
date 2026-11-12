package dev.egorgrigorciuc.forzammilmod;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class PerfLogicTest {

    @Test
    void timerAveragesAndFindsMax() {
        TickTimer t = new TickTimer();
        t.record(1_000_000);
        t.record(3_000_000);
        assertEquals(2.0, t.averageMillis(), 1e-9);
        assertEquals(3.0, t.maxMillis(), 1e-9);
        assertEquals(2, t.samples());
    }

    @Test
    void timerKeepsOnlyTheLastWindow() {
        TickTimer t = new TickTimer();
        t.record(900_000_000L); // falls out of the window
        for (int i = 0; i < TickTimer.WINDOW; i++) t.record(1_000_000);
        assertEquals(TickTimer.WINDOW, t.samples());
        assertEquals(1.0, t.maxMillis(), 1e-9);
    }

    @Test
    void emptyTimerReadsZero() {
        TickTimer t = new TickTimer();
        assertEquals(0, t.averageMillis());
        assertEquals(0, t.maxMillis());
        t.record(5);
        t.reset();
        assertEquals(0, t.samples());
    }

    @Test
    void statsSumTheAverages() {
        PerfStats s = new PerfStats();
        s.record("a", 1_000_000);
        s.record("b", 2_000_000);
        assertEquals(3.0, s.totalAverageMillis(), 1e-9);
    }

    @Test
    void shellCapRefusesAtTheLimit() {
        assertTrue(ShellCap.canFire(ShellCap.MAX_LIVE - 1));
        assertFalse(ShellCap.canFire(ShellCap.MAX_LIVE));
    }

    @Test
    void trailPuffsAreBounded() {
        assertEquals(1, ShellCap.trailPuffs(0.2));
        assertEquals(3, ShellCap.trailPuffs(2.5));
        assertEquals(ShellCap.MAX_TRAIL_PUFFS, ShellCap.trailPuffs(50));
    }

    @Test
    void busyVehicleNeverSleeps() {
        SleepRule r = new SleepRule();
        for (int i = 0; i < 200; i++) assertFalse(r.sleepsNow(false, i, 0));
        assertFalse(r.asleep());
    }

    @Test
    void quietVehicleSleepsAfterTheGracePeriodAndWakesPeriodically() {
        SleepRule r = new SleepRule();
        long tick = 0;
        for (int i = 0; i < SleepRule.QUIET_TICKS; i++) assertFalse(r.sleepsNow(true, tick++, 0));
        assertTrue(r.asleep());
        int slept = 0, awake = 0;
        for (int i = 0; i < SleepRule.WAKE_EVERY * 5; i++) {
            if (r.sleepsNow(true, tick++, 0)) slept++;
            else awake++;
        }
        assertEquals(5, awake);
        assertEquals(SleepRule.WAKE_EVERY * 5 - 5, slept);
    }

    @Test
    void movingOrBoardedWakesAtOnce() {
        SleepRule r = new SleepRule();
        for (int i = 0; i < SleepRule.QUIET_TICKS + 5; i++) r.sleepsNow(true, i, 0);
        assertTrue(r.asleep());
        assertFalse(r.sleepsNow(false, 7, 0));
        assertFalse(r.asleep());
    }

    @Test
    void phasesSpreadTheWakeUps() {
        SleepRule a = new SleepRule(), b = new SleepRule();
        for (int i = 0; i < SleepRule.QUIET_TICKS + 1; i++) {
            a.sleepsNow(true, i, 0);
            b.sleepsNow(true, i, 7);
        }
        for (long t = 100; t < 140; t++) {
            assertFalse(!a.sleepsNow(true, t, 0) && !b.sleepsNow(true, t, 7) && (t % 20 == 0) && (t + 7) % 20 == 0);
        }
    }
}
