package dev.egorgrigorciuc.forzammilmod;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ReloadTimerTest {

    @Test
    void firstShotIsAllowedThenBlockedForFourSeconds() {
        ReloadTimer t = new ReloadTimer(80);
        assertTrue(t.tryFire(100));
        assertFalse(t.tryFire(100));
        assertFalse(t.tryFire(179));
        assertTrue(t.tryFire(180));
    }

    @Test
    void remainingSecondsRoundUp() {
        ReloadTimer t = new ReloadTimer(80);
        assertEquals(0, t.remainingSeconds(0));
        t.tryFire(0);
        assertEquals(4, t.remainingSeconds(0));
        assertEquals(4, t.remainingSeconds(1));
        assertEquals(3, t.remainingSeconds(20));
        assertEquals(2, t.remainingSeconds(40));
        assertEquals(1, t.remainingSeconds(79));
        assertEquals(0, t.remainingSeconds(80));
        assertTrue(t.ready(80));
    }
}
