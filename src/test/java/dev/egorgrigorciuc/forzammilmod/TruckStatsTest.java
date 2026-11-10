package dev.egorgrigorciuc.forzammilmod;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/** The army truck sits between the jeep and the tank and drives like a heavy wheeled vehicle. */
class TruckStatsTest {

    private static final VehicleType T = VehicleType.TRUCK;

    @Test
    void speedIsBetweenTankAndJeep() {
        assertTrue(T.maxSpeed > VehicleType.TANK.maxSpeed);
        assertTrue(T.maxSpeed < VehicleType.JEEP.maxSpeed);
    }

    @Test
    void healthTurningAndGun() {
        assertEquals(60, T.maxHealth);
        assertTrue(T.turnRate < VehicleType.JEEP.turnRate);
        assertFalse(T.turnsOnSpot);
        assertFalse(T.isTank());
        assertEquals(40, DamageRules.shell(T, HitSide.FRONT));
        assertEquals(DamageRules.MELEE_JEEP, DamageRules.melee(T));
    }

    @Test
    void wheelsAndIdLookup() {
        assertSame(T, VehicleType.byId("truck"));
        assertEquals("truck", T.model());
        assertEquals(6, WheelSpin.bones(T).size());
        assertTrue(WheelSpin.radius(T) > 0);
    }

    @Test
    void engineIsLowerAndSlowerThanTheJeep() {
        assertTrue(T.soundPitch() < VehicleType.JEEP.soundPitch());
        assertTrue(T.soundPitch() >= 0.5);
        VehicleSoundState truck = new VehicleSoundState(false, T.soundPitch());
        VehicleSoundState jeep = new VehicleSoundState(false);
        // At a lower pitch the loop file plays longer, so it is restarted later.
        long firstTruck = firstRestart(truck), firstJeep = firstRestart(jeep);
        assertTrue(firstTruck > firstJeep, firstTruck + " vs " + firstJeep);
    }

    private static long firstRestart(VehicleSoundState s) {
        int plays = 0;
        for (long t = 0; t < 400; t++) {
            var out = s.update(t, true, false, 0, 0);
            if (out.engine() == SoundLoop.Action.PLAY && ++plays == 2) return t;
        }
        return -1;
    }
}
