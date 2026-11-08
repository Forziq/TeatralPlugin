package dev.egorgrigorciuc.forzammilmod;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class PadRulesTest {
    @Test
    void emptyPadIsOk() {
        assertEquals(PadRules.Verdict.OK, PadRules.check(new double[]{}, 0));
        assertEquals(PadRules.Verdict.OK, PadRules.check(new double[]{3.0, 20}, 9));
    }

    @Test
    void closeVehicleOccupiesPad() {
        assertEquals(PadRules.Verdict.OCCUPIED, PadRules.check(new double[]{2.9}, 1));
        assertEquals(PadRules.Verdict.OCCUPIED, PadRules.check(new double[]{50, 0.5}, 10));
    }

    @Test
    void capReached() {
        assertEquals(PadRules.Verdict.FULL, PadRules.check(new double[]{}, 10));
        assertEquals(PadRules.Verdict.FULL, PadRules.check(new double[]{10}, 11));
    }

    @Test
    void exactlyThreeBlocksIsNotOccupied() {
        assertEquals(PadRules.Verdict.OK, PadRules.check(new double[]{3.0}, 1));
        assertEquals(PadRules.Verdict.OCCUPIED, PadRules.check(new double[]{Math.nextDown(3.0)}, 1));
    }

    @Test
    void ninevsTen() {
        assertEquals(PadRules.Verdict.OK, PadRules.check(new double[]{}, 9));
        assertEquals(PadRules.Verdict.FULL, PadRules.check(new double[]{}, 10));
    }

    @Test
    void occupiedBeatsFull() {
        assertEquals(PadRules.Verdict.OCCUPIED, PadRules.check(new double[]{1}, 10));
        assertEquals(PadRules.Verdict.OCCUPIED, PadRules.check(new double[]{1}, 25));
    }

    @Test
    void customCap() {
        assertEquals(PadRules.Verdict.OK, PadRules.check(new double[]{}, 4, 5));
        assertEquals(PadRules.Verdict.FULL, PadRules.check(new double[]{}, 5, 5));
        assertEquals(PadRules.Verdict.OK, PadRules.check(new double[]{}, 19, 20));
    }

    @Test
    void capBelowOneBecomesOne() {
        assertEquals(1, PadRules.sanitiseMax(0));
        assertEquals(1, PadRules.sanitiseMax(-3));
        assertEquals(7, PadRules.sanitiseMax(7));
    }
}
