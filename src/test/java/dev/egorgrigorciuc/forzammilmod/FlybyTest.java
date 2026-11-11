package dev.egorgrigorciuc.forzammilmod;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class FlybyTest {

    @Test
    void onlyFastPlanesInTheAir() {
        assertTrue(Flyby.fastEnough(true, Flyby.MIN_SPEED));
        assertFalse(Flyby.fastEnough(true, Flyby.MIN_SPEED - 0.01));
        assertFalse(Flyby.fastEnough(false, 1.0), "taxiing past doesn't count");
    }

    @Test
    void closeEnoughAndOncePerCooldown() {
        double near = 10 * 10, far = 30 * 30;
        assertTrue(Flyby.due(near, null, 1000));
        assertFalse(Flyby.due(far, null, 1000));
        assertFalse(Flyby.due(near, 1000L, 1000 + Flyby.COOLDOWN_TICKS - 1));
        assertTrue(Flyby.due(near, 1000L, 1000 + Flyby.COOLDOWN_TICKS));
    }
}
