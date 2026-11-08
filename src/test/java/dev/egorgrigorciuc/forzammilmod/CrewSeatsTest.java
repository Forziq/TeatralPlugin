package dev.egorgrigorciuc.forzammilmod;

import dev.egorgrigorciuc.forzammilmod.CrewSeats.Seat;
import org.junit.jupiter.api.Test;

import java.util.EnumSet;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class CrewSeatsTest {

    private static final Set<Seat> NONE = EnumSet.noneOf(Seat.class);

    @Test
    void tankHasDriverAndGunner() {
        assertEquals(List.of(Seat.DRIVER, Seat.GUNNER), CrewSeats.seatsOf(VehicleType.TANK));
    }

    @Test
    void jeepHasDriverAndTwoPassengers() {
        assertEquals(List.of(Seat.DRIVER, Seat.PASSENGER1, Seat.PASSENGER2), CrewSeats.seatsOf(VehicleType.JEEP));
    }

    @Test
    void seatsFillInOrder() {
        Set<Seat> taken = EnumSet.noneOf(Seat.class);
        assertEquals(Seat.DRIVER, CrewSeats.nextFree(VehicleType.JEEP, taken));
        taken.add(Seat.DRIVER);
        assertEquals(Seat.PASSENGER1, CrewSeats.nextFree(VehicleType.JEEP, taken));
        taken.add(Seat.PASSENGER1);
        assertEquals(Seat.PASSENGER2, CrewSeats.nextFree(VehicleType.JEEP, taken));
        taken.add(Seat.PASSENGER2);
        assertNull(CrewSeats.nextFree(VehicleType.JEEP, taken));
    }

    @Test
    void tankFillsDriverThenGunnerThenIsFull() {
        Set<Seat> taken = EnumSet.noneOf(Seat.class);
        assertEquals(Seat.DRIVER, CrewSeats.nextFree(VehicleType.TANK, taken));
        taken.add(Seat.DRIVER);
        assertEquals(Seat.GUNNER, CrewSeats.nextFree(VehicleType.TANK, taken));
        taken.add(Seat.GUNNER);
        assertNull(CrewSeats.nextFree(VehicleType.TANK, taken));
    }

    @Test
    void freedSeatIsReusedFirst() {
        assertEquals(Seat.DRIVER, CrewSeats.nextFree(VehicleType.JEEP, EnumSet.of(Seat.PASSENGER1)));
        assertEquals(Seat.GUNNER, CrewSeats.nextFree(VehicleType.TANK, EnumSet.of(Seat.DRIVER)));
        assertEquals(List.of(Seat.DRIVER, Seat.PASSENGER2),
                CrewSeats.freeSeats(VehicleType.JEEP, EnumSet.of(Seat.PASSENGER1)));
    }

    @Test
    void tankWithoutGunnerIsAimedByDriver() {
        Set<Seat> taken = EnumSet.of(Seat.DRIVER);
        assertEquals(Seat.DRIVER, CrewSeats.turretController(VehicleType.TANK, taken));
        assertTrue(CrewSeats.canFire(VehicleType.TANK, taken, Seat.DRIVER));
    }

    @Test
    void gunnerTakesTurretAndFireFromDriver() {
        Set<Seat> taken = EnumSet.of(Seat.DRIVER, Seat.GUNNER);
        assertEquals(Seat.GUNNER, CrewSeats.turretController(VehicleType.TANK, taken));
        assertTrue(CrewSeats.canFire(VehicleType.TANK, taken, Seat.GUNNER));
        assertFalse(CrewSeats.canFire(VehicleType.TANK, taken, Seat.DRIVER));
    }

    @Test
    void gunnerAloneControlsTurret() {
        assertEquals(Seat.GUNNER, CrewSeats.turretController(VehicleType.TANK, EnumSet.of(Seat.GUNNER)));
    }

    @Test
    void emptyTankHasNoOperator() {
        assertNull(CrewSeats.turretController(VehicleType.TANK, NONE));
        assertFalse(CrewSeats.canFire(VehicleType.TANK, NONE, Seat.DRIVER));
        assertFalse(CrewSeats.canFire(VehicleType.TANK, NONE, null));
    }

    @Test
    void jeepHasNoTurret() {
        Set<Seat> taken = EnumSet.of(Seat.DRIVER, Seat.PASSENGER1);
        assertNull(CrewSeats.turretController(VehicleType.JEEP, taken));
        assertFalse(CrewSeats.canFire(VehicleType.JEEP, taken, Seat.DRIVER));
    }

    @Test
    void onlyDriverDrives() {
        assertTrue(CrewSeats.drives(Seat.DRIVER));
        assertFalse(CrewSeats.drives(Seat.GUNNER));
        assertFalse(CrewSeats.drives(Seat.PASSENGER1));
        assertFalse(CrewSeats.drives(Seat.PASSENGER2));
    }
}
