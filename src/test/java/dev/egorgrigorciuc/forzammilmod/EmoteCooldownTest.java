package dev.egorgrigorciuc.forzammilmod;

import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;

class EmoteCooldownTest {

    private final EmoteCooldown cd = new EmoteCooldown();
    private final UUID a = UUID.randomUUID();
    private final UUID b = UUID.randomUUID();

    @Test
    void freeBeforeFirstEmote() {
        assertEquals(0, cd.secondsLeft(a, 1000));
    }

    @Test
    void blockedWithinThreeSecondsAllowedAfter() {
        cd.markStarted(a, 10_000);
        assertEquals(3, cd.secondsLeft(a, 10_000));
        assertEquals(1, cd.secondsLeft(a, 12_999));
        assertEquals(0, cd.secondsLeft(a, 13_000));
        assertEquals(0, cd.secondsLeft(a, 20_000));
    }

    @Test
    void secondsRoundUp() {
        cd.markStarted(a, 0);
        assertEquals(3, cd.secondsLeft(a, 1));
        assertEquals(2, cd.secondsLeft(a, 1000));
        assertEquals(2, cd.secondsLeft(a, 1001));
    }

    @Test
    void playersAreIndependentAndForgettable() {
        cd.markStarted(a, 0);
        assertEquals(0, cd.secondsLeft(b, 100));
        cd.forget(a);
        assertEquals(0, cd.secondsLeft(a, 100));
    }
}
