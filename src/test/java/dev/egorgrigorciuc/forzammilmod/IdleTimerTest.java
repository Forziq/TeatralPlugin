package dev.egorgrigorciuc.forzammilmod;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class IdleTimerTest {
    @Test
    void despawnsAfterTenMinutesWithoutDriver() {
        IdleTimer t = new IdleTimer(0);
        assertFalse(t.update(false, IdleTimer.LIMIT_MILLIS - 1));
        assertTrue(t.update(false, IdleTimer.LIMIT_MILLIS));
    }

    @Test
    void driverResetsClock() {
        IdleTimer t = new IdleTimer(0);
        assertFalse(t.update(true, IdleTimer.LIMIT_MILLIS - 1));
        assertFalse(t.update(false, IdleTimer.LIMIT_MILLIS + 5000));
        assertTrue(t.update(false, 2 * IdleTimer.LIMIT_MILLIS - 1));
    }

    @Test
    void driverPresentNeverDespawns() {
        IdleTimer t = new IdleTimer(0);
        assertFalse(t.update(true, 5 * IdleTimer.LIMIT_MILLIS));
    }

    @Test
    void driverLeavesThenReturnsRestartsClock() {
        IdleTimer t = new IdleTimer(0);
        assertFalse(t.update(false, IdleTimer.LIMIT_MILLIS - 1000));
        assertFalse(t.update(true, IdleTimer.LIMIT_MILLIS - 500));
        assertFalse(t.update(false, IdleTimer.LIMIT_MILLIS + 1000));
        assertFalse(t.update(false, IdleTimer.LIMIT_MILLIS - 500 + IdleTimer.LIMIT_MILLIS - 1));
        assertTrue(t.update(false, IdleTimer.LIMIT_MILLIS - 500 + IdleTimer.LIMIT_MILLIS));
    }
}
