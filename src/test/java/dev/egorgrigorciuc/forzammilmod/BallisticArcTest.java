package dev.egorgrigorciuc.forzammilmod;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class BallisticArcTest {

    private static final double V = BallisticArc.MUZZLE_SPEED;
    private static final double G = BallisticArc.GRAVITY;

    @Test
    void rangeAt45IsAboutThreeHundred() {
        double r = BallisticArc.flatRange(V, G, 45);
        assertEquals(300, r, 30, "formula " + r);
        assertEquals(300, BallisticArc.simulatedRange(V, G, 45), 30);
    }

    @Test
    void lowerAnglesReachLess() {
        double r45 = BallisticArc.simulatedRange(V, G, 45);
        double r30 = BallisticArc.simulatedRange(V, G, 30);
        double r15 = BallisticArc.simulatedRange(V, G, 15);
        assertTrue(r30 < r45 && r15 < r30, r15 + " " + r30 + " " + r45);
        assertEquals(BallisticArc.flatRange(V, G, 30), r30, 1.0);
    }

    @Test
    void levelBarrelFallsAlmostAtOnce() {
        assertTrue(BallisticArc.simulatedRange(V, G, 0) < 5);
        assertEquals(0, BallisticArc.flatRange(V, G, 0), 1e-9);
    }

    @Test
    void positionFollowsAParabola() {
        BallisticArc arc = new BallisticArc(10, 64, -5, 1, 1, 0, V, G);
        double[] p0 = arc.at(0);
        assertArrayEquals(new double[]{10, 64, -5}, p0, 1e-9);
        double a = V / Math.sqrt(2);
        double[] p = arc.at(40);
        assertEquals(10 + a * 40, p[0], 1e-9);
        assertEquals(64 + a * 40 - 0.5 * G * 1600, p[1], 1e-9);
        assertEquals(-5, p[2], 1e-9);
        assertTrue(arc.verticalSpeed(0) > 0);
        assertTrue(arc.verticalSpeed(100) < 0);
    }

    @Test
    void noChordIsLongerThanOneBlockAndTheyJoinUp() {
        for (double angle : new double[]{0, 10, 30, 45}) {
            double a = Math.toRadians(angle);
            BallisticArc arc = new BallisticArc(0, 100, 0, Math.cos(a), Math.sin(a), 0, V, G);
            double[] end = arc.at(0);
            double total = 0;
            while (!arc.done()) {
                List<ShellModel.Segment> segs = arc.nextTick();
                assertFalse(segs.isEmpty());
                for (ShellModel.Segment s : segs) {
                    assertTrue(s.length() <= BallisticArc.MAX_CHORD + 1e-9, "chord " + s.length() + " at " + angle);
                    assertEquals(end[0], s.x(), 1e-6);
                    assertEquals(end[1], s.y(), 1e-6);
                    assertEquals(end[2], s.z(), 1e-6);
                    assertEquals(1.0, Math.sqrt(s.dx() * s.dx() + s.dy() * s.dy() + s.dz() * s.dz()), 1e-9);
                    end = new double[]{s.endX(), s.endY(), s.endZ()};
                    total += s.length();
                }
            }
            double[] last = new BallisticArc(0, 100, 0, Math.cos(a), Math.sin(a), 0, V, G).at(BallisticArc.MAX_TICKS);
            assertEquals(last[0], end[0], 1e-6);
            assertEquals(last[1], end[1], 1e-6);
            assertEquals(total, arc.travelled(), 1e-6);
        }
    }

    @Test
    void chordsStayShortWhenFallingFast() {
        // Shot straight down a long drop: speed grows, the pieces must still be at most a block.
        BallisticArc arc = new BallisticArc(0, 300, 0, 0.1, -1, 0, V, G);
        for (int i = 0; i < 100; i++) {
            for (ShellModel.Segment s : arc.nextTick()) assertTrue(s.length() <= BallisticArc.MAX_CHORD + 1e-9);
        }
    }

    @Test
    void endsAfterTheCutOff() {
        BallisticArc arc = new BallisticArc(0, 64, 0, 1, 0.5, 0, V, G);
        int ticks = 0;
        while (!arc.nextTick().isEmpty()) ticks++;
        assertEquals(BallisticArc.MAX_TICKS, ticks);
        assertTrue(arc.done());
    }

    @Test
    void aimedShellKeepsItsHeadingAndHasLongRange() {
        BallisticArc arc = BallisticArc.fire(0, 0, 0, 0, 0.7071, 0.7071);
        double[] p = arc.at(50);
        assertEquals(0, p[0], 1e-9);
        assertTrue(p[2] > 100);
    }
}
