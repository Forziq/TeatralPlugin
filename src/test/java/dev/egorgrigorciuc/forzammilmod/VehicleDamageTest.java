package dev.egorgrigorciuc.forzammilmod;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class VehicleDamageTest {

    /** Direction vector (dx, dz) of a unit move along a Minecraft yaw. */
    private static double[] dir(double yaw) {
        double r = Math.toRadians(yaw);
        return new double[]{-Math.sin(r), Math.cos(r)};
    }

    private static HitSide hit(double hullYaw, double shellYaw) {
        double[] d = dir(shellYaw);
        return HitSide.classify(hullYaw, d[0], d[1]);
    }

    @Test
    void shellTravellingTheSameWayAsTheHullHitsTheBack() {
        for (double yaw : new double[]{0, 90, -90, 180, 350, 10}) {
            assertEquals(HitSide.BACK, hit(yaw, yaw), "yaw " + yaw);
        }
    }

    @Test
    void shellTravellingAgainstTheHullHitsTheFront() {
        for (double yaw : new double[]{0, 90, -90, 180, 350, 10}) {
            assertEquals(HitSide.FRONT, hit(yaw, yaw + 180), "yaw " + yaw);
        }
    }

    @Test
    void quarterTurnsAreSides() {
        for (double yaw : new double[]{0, 90, -90, 180}) {
            assertEquals(HitSide.SIDE, hit(yaw, yaw + 90));
            assertEquals(HitSide.SIDE, hit(yaw, yaw - 90));
        }
    }

    @Test
    void wrapAroundNearMinus180And180() {
        assertEquals(HitSide.BACK, hit(179, -179));
        assertEquals(HitSide.FRONT, hit(-179, 1));
        assertEquals(HitSide.BACK, hit(350, 10));
        assertEquals(HitSide.BACK, hit(10, 350));
    }

    @Test
    void coneEdges() {
        assertEquals(HitSide.BACK, HitSide.classifyYaw(0, 44));
        assertEquals(HitSide.SIDE, HitSide.classifyYaw(0, 50));
        assertEquals(HitSide.SIDE, HitSide.classifyYaw(0, 130));
        assertEquals(HitSide.FRONT, HitSide.classifyYaw(0, 136));
        assertEquals(HitSide.FRONT, HitSide.classifyYaw(0, -136));
    }

    @Test
    void healthClampsAtZeroAndMax() {
        VehicleHealth h = new VehicleHealth(40);
        assertTrue(h.isFull());
        h.damage(1000);
        assertEquals(0, h.current());
        assertTrue(h.isDestroyed());
        h.repair(1000);
        assertEquals(40, h.current());
        assertEquals(1.0, h.fraction(), 1e-9);
        h.damage(10);
        assertEquals(75, h.percent());
        assertFalse(h.isFull());
    }

    @Test
    void repairAmountIsTenPercentRoundedUp() {
        assertEquals(4, VehicleHealth.repairAmount(VehicleType.JEEP.maxHealth));
        assertEquals(10, VehicleHealth.repairAmount(VehicleType.TANK.maxHealth));
    }

    @Test
    void maxHealthPerType() {
        assertEquals(100, VehicleType.TANK.maxHealth);
        assertEquals(40, VehicleType.JEEP.maxHealth);
    }

    @Test
    void shellDamageByTypeAndSide() {
        assertEquals(25, DamageRules.shell(VehicleType.TANK, HitSide.FRONT));
        assertEquals(35, DamageRules.shell(VehicleType.TANK, HitSide.SIDE));
        assertEquals(50, DamageRules.shell(VehicleType.TANK, HitSide.BACK));
        for (HitSide s : HitSide.values()) assertEquals(40, DamageRules.shell(VehicleType.JEEP, s));
    }

    @Test
    void splashFallsOffLinearly() {
        assertEquals(10, DamageRules.splash(0));
        assertEquals(5, DamageRules.splash(2));
        assertEquals(0, DamageRules.splash(4));
        assertEquals(0, DamageRules.splash(9));
    }

    @Test
    void meleeAndArrowsOnlyHurtJeeps() {
        assertEquals(0, DamageRules.melee(VehicleType.TANK));
        assertEquals(0, DamageRules.arrow(VehicleType.TANK));
        assertEquals(2, DamageRules.melee(VehicleType.JEEP));
        assertEquals(3, DamageRules.arrow(VehicleType.JEEP));
    }

    @Test
    void rayHitsTurnedHull() {
        HullBox b = new HullBox(0, 0, 0, 0, 1, 2, 1.5);
        // along +Z at the hull: front face (local z=-2) reached after 8 blocks from z=-10
        assertEquals(8.0, b.rayHit(0, 0.5, -10, 0, 0, 1, 20), 1e-9);
        assertEquals(-1.0, b.rayHit(5, 0.5, -10, 0, 0, 1, 20), 1e-9);
        assertEquals(-1.0, b.rayHit(0, 0.5, -10, 0, 0, 1, 5), 1e-9);
        // turned 90 degrees the long side lies along X
        HullBox t = new HullBox(0, 0, 0, 90, 1, 2, 1.5);
        assertEquals(8.0, t.rayHit(-10, 0.5, 0, 1, 0, 0, 20), 1e-9);
        assertEquals(-1.0, t.rayHit(0, 3, -10, 0, 0, 1, 20), 1e-9);
    }

    @Test
    void distanceToHull() {
        HullBox b = new HullBox(0, 0, 0, 0, 1, 2, 1.5);
        assertEquals(0, b.distanceTo(0, 1, 0), 1e-9);
        assertEquals(3, b.distanceTo(0, 1, 5), 1e-9);
        assertEquals(2, b.distanceTo(3, 1, 0), 1e-9);
    }

    @Test
    void splashIntermediateAndNegativeDistances() {
        assertEquals(8, DamageRules.splash(1));   // 7.5 rounds up
        assertEquals(3, DamageRules.splash(3));   // 2.5 rounds up
        assertEquals(1, DamageRules.splash(3.6));
        assertEquals(10, DamageRules.splash(-2));
    }

    @Test
    void damageAndRepairIgnoreNonPositiveAmounts() {
        VehicleHealth h = new VehicleHealth(40);
        h.damage(-5);
        h.damage(0);
        assertEquals(40, h.current());
        h.damage(15);
        h.repair(-5);
        h.repair(0);
        assertEquals(25, h.current());
        h.damage(25);
        assertTrue(h.isDestroyed());
        assertEquals(0, h.percent());
    }

    @Test
    void repairingInStepsNeverExceedsMax() {
        VehicleHealth h = new VehicleHealth(VehicleType.JEEP.maxHealth);
        h.damage(40);
        for (int i = 0; i < 20; i++) h.repair(VehicleHealth.repairAmount(h.max()));
        assertEquals(40, h.current());
        assertTrue(h.isFull());
    }

    @Test
    void wrapAroundAtMoreHullYaws() {
        assertEquals(HitSide.FRONT, hit(350, 170));
        assertEquals(HitSide.FRONT, hit(10, 190));
        assertEquals(HitSide.SIDE, hit(350, 80));
        assertEquals(HitSide.BACK, hit(-170, 190));
        assertEquals(HitSide.BACK, hit(540, 180));
    }
}
