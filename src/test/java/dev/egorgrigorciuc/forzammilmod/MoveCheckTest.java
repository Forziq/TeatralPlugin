package dev.egorgrigorciuc.forzammilmod;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MoveCheckTest {
    @Test void rotationOnlyIsNotMoving() { assertFalse(MoveCheck.isMoving(1.5, 64, 1.5, 1.5, 64, 1.5)); }
    @Test void smallDriftIsNotMoving() { assertFalse(MoveCheck.isMoving(1.5, 64.2, 1.5, 1.52, 64.25, 1.48)); }
    @Test void yJitterInsideBlockIsNotMoving() { assertFalse(MoveCheck.isMoving(1.5, 64.0, 1.5, 1.5, 64.9, 1.5)); }
    @Test void stepOnXIsMoving() { assertTrue(MoveCheck.isMoving(1.2, 64, 1.5, 1.26, 64, 1.5)); }
    @Test void stepOnZIsMoving() { assertTrue(MoveCheck.isMoving(1.5, 64, 1.2, 1.5, 64, 1.26)); }
    @Test void blockChangeViaYIsMoving() { assertTrue(MoveCheck.isMoving(1.5, 64.9, 1.5, 1.5, 65.1, 1.5)); }
    @Test void tinyCrossOfBlockEdgeIsMoving() { assertTrue(MoveCheck.isMoving(1.999, 64, 1.5, 2.001, 64, 1.5)); }
}
