package dev.egorgrigorciuc.forzammilmod;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class WheelSpinTest {

    @Test
    void zeroSpeedKeepsAngle() {
        assertEquals(42f, WheelSpin.advance(42f, 0, 0.3125), 1e-4);
    }

    @Test
    void forwardAddsSpeedOverRadiusInDegrees() {
        float a = WheelSpin.advance(10f, 0.1, 0.5);
        assertEquals(10f + (float) Math.toDegrees(0.2), a, 1e-3);
    }

    @Test
    void reverseSubtracts() {
        float a = WheelSpin.advance(100f, -0.1, 0.5);
        assertEquals(100f - (float) Math.toDegrees(0.2), a, 1e-3);
    }

    @Test
    void wrapsIntoZeroTo360() {
        float up = WheelSpin.advance(359f, 0.1, 0.5);
        assertTrue(up >= 0f && up < 360f);
        assertEquals((359f + (float) Math.toDegrees(0.2)) - 360f, up, 1e-3);
        float down = WheelSpin.advance(1f, -0.1, 0.5);
        assertTrue(down >= 0f && down < 360f);
        assertEquals(1f - (float) Math.toDegrees(0.2) + 360f, down, 1e-3);
    }
}
