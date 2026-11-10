package dev.egorgrigorciuc.forzammilmod;

import dev.egorgrigorciuc.forzammilmod.CrewSeats.Seat;
import org.junit.jupiter.api.Test;

import java.util.EnumSet;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/** The field gun: no driving, slow turning, one gunner who also fires, barrel from 0 to 45 degrees. */
class ArtilleryStatsTest {

    private static final VehicleType A = VehicleType.ARTILLERY;

    @Test
    void statsAndFlags() {
        assertEquals(50, A.maxHealth);
        assertEquals(0.0, A.maxSpeed);
        assertEquals(0.0, A.reverseMax);
        assertTrue(A.turnsOnSpot);
        assertTrue(A.turnRate > 0 && A.turnRate < VehicleType.TANK.turnRate);
        assertTrue(A.isArtillery() && A.hasGun() && !A.isTank() && !A.moves());
        assertEquals(160, A.reloadTicks());
        assertEquals(ShellService.RELOAD_TICKS, VehicleType.TANK.reloadTicks());
        assertSame(A, VehicleType.byId("artillery"));
        assertTrue(WheelSpin.bones(A).isEmpty());
    }

    @Test
    void gunnerIsTheDriverSeatAndFires() {
        assertEquals(List.of(Seat.DRIVER), CrewSeats.seatsOf(A));
        EnumSet<Seat> taken = EnumSet.of(Seat.DRIVER);
        assertEquals(Seat.DRIVER, CrewSeats.turretController(A, taken));
        assertTrue(CrewSeats.canFire(A, taken, Seat.DRIVER));
        assertNull(CrewSeats.nextFree(A, taken));
        assertNull(CrewSeats.turretController(A, EnumSet.noneOf(Seat.class)));
    }

    @Test
    void throttleDoesNothingButSteeringTurns() {
        DriveModel.Result fwd = DriveModel.step(A, new DriveInput(true, false, false, false), 0, 10f);
        DriveModel.Result back = DriveModel.step(A, new DriveInput(false, true, false, false), 0, 10f);
        assertEquals(0.0, fwd.speed(), 1e-12);
        assertEquals(0.0, back.speed(), 1e-12);
        assertEquals(10f, fwd.yaw());
        DriveModel.Result turn = DriveModel.step(A, new DriveInput(false, false, true, false), 0, 10f);
        assertNotEquals(10f, turn.yaw());
        assertEquals(0.0, turn.speed(), 1e-12);
    }

    @Test
    void barrelStaysBetweenLevelAndFortyFive() {
        TurretModel t = new TurretModel(TurretModel.FIELD_GUN_PITCH_MIN, TurretModel.FIELD_GUN_PITCH_MAX);
        for (int i = 0; i < 400; i++) t.tick(0f, 80f, 0.05f);
        assertEquals(45f, t.pitch(), 1e-3);
        for (int i = 0; i < 400; i++) t.tick(0f, -30f, 0.05f);
        assertEquals(0f, t.pitch(), 1e-3);
        assertEquals(0f, t.yaw(), 1e-6);
    }

    @Test
    void muzzleSitsAheadOfThePivotAlongTheBarrel() {
        TurretModel.Muzzle level = TurretModel.muzzle(0, 0, 0, 0f, 0f, 0f, TurretModel.FIELD_GUN);
        assertEquals(TurretModel.FIELD_GUN.pivotForward() + TurretModel.FIELD_GUN.length(), level.z(), 1e-9);
        assertEquals(TurretModel.FIELD_GUN.pivotUp(), level.y(), 1e-9);
        TurretModel.Muzzle up = TurretModel.muzzle(0, 0, 0, 0f, 0f, 45f, TurretModel.FIELD_GUN);
        assertTrue(up.y() > level.y());
        assertEquals(Math.sin(Math.toRadians(45)), up.dy(), 1e-9);
    }

    @Test
    void shellDamageLikeANonTank() {
        assertEquals(40, DamageRules.shell(A, HitSide.FRONT));
    }
}
