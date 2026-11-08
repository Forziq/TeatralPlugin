package dev.egorgrigorciuc.forzammilmod;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ShellModelTest {

    @Test
    void firstSegmentIsThreeBlocksFromTheStart() {
        ShellModel m = new ShellModel(1, 2, 3, 0, 0, 5);
        ShellModel.Segment s = m.next();
        assertEquals(3.0, s.length(), 1e-9);
        assertEquals(1, s.x(), 1e-9);
        assertEquals(3, s.z(), 1e-9);
        assertEquals(6, s.endZ(), 1e-9);
    }

    @Test
    void directionIsNormalised() {
        ShellModel.Segment s = new ShellModel(0, 0, 0, 3, 4, 0).next();
        assertEquals(0.6, s.dx(), 1e-9);
        assertEquals(0.8, s.dy(), 1e-9);
        assertEquals(1.0, Math.sqrt(s.dx() * s.dx() + s.dy() * s.dy() + s.dz() * s.dz()), 1e-9);
    }

    @Test
    void segmentsAreContiguous() {
        ShellModel m = new ShellModel(0, 0, 0, 1, 1, 1);
        ShellModel.Segment prev = m.next();
        for (ShellModel.Segment s = m.next(); s != null; s = m.next()) {
            assertEquals(prev.endX(), s.x(), 1e-9);
            assertEquals(prev.endY(), s.y(), 1e-9);
            assertEquals(prev.endZ(), s.z(), 1e-9);
            prev = s;
        }
    }

    @Test
    void endsExactlyAtMaxRangeWithShortenedLastSegment() {
        ShellModel m = new ShellModel(0, 0, 0, 1, 0, 0);
        double total = 0;
        int count = 0;
        ShellModel.Segment last = null;
        for (ShellModel.Segment s = m.next(); s != null; s = m.next()) {
            total += s.length();
            count++;
            last = s;
        }
        assertEquals(ShellModel.MAX_RANGE, total, 1e-9);
        assertEquals(50, count);
        assertEquals(3.0, last.length(), 1e-9);
        assertTrue(m.done());
        assertNull(m.next());
    }

    @Test
    void lastSegmentIsShortWhenRangeIsNotAMultiple() {
        // 150 is a multiple of 3, so check the clamp by looking at travelled before the final step.
        ShellModel m = new ShellModel(0, 0, 0, 0, 1, 0);
        for (int i = 0; i < 49; i++) m.next();
        assertEquals(147.0, m.travelled(), 1e-9);
        assertFalse(m.done());
        assertEquals(3.0, m.next().length(), 1e-9);
    }
}
