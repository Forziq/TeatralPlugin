package dev.egorgrigorciuc.forzammilmod;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/** Who sits where and who controls what. Pure, no Bukkit types. */
final class CrewSeats {

    /** A seat on a vehicle. {@code bone} is the BetterModel bone name (tag already stripped: p_driver is "driver"). */
    enum Seat {
        DRIVER("driver"),
        GUNNER("gunner"),
        PASSENGER1("passenger1"),
        PASSENGER2("passenger2");

        final String bone;

        Seat(String bone) {
            this.bone = bone;
        }
    }

    private CrewSeats() { }

    /** Seats the type has, in the order people fill them. */
    static List<Seat> seatsOf(VehicleType type) {
        return switch (type) {
            case TANK -> List.of(Seat.DRIVER, Seat.GUNNER);
            case JEEP -> List.of(Seat.DRIVER, Seat.PASSENGER1, Seat.PASSENGER2);
        };
    }

    /** Free seats in fill order. */
    static List<Seat> freeSeats(VehicleType type, Collection<Seat> taken) {
        List<Seat> free = new ArrayList<>();
        for (Seat s : seatsOf(type)) if (!taken.contains(s)) free.add(s);
        return free;
    }

    /** First free seat, or null if the vehicle is full. */
    static Seat nextFree(VehicleType type, Collection<Seat> taken) {
        List<Seat> free = freeSeats(type, taken);
        return free.isEmpty() ? null : free.get(0);
    }

    /** Only the driver's seat drives. */
    static boolean drives(Seat seat) {
        return seat == Seat.DRIVER;
    }

    /** Seat that aims the turret and fires: the gunner if there is one, else the driver, else nobody. Null for turretless types. */
    static Seat turretController(VehicleType type, Collection<Seat> taken) {
        if (type != VehicleType.TANK) return null;
        if (taken.contains(Seat.GUNNER)) return Seat.GUNNER;
        if (taken.contains(Seat.DRIVER)) return Seat.DRIVER;
        return null;
    }

    /** True if whoever sits in {@code seat} may fire the gun. */
    static boolean canFire(VehicleType type, Collection<Seat> taken, Seat seat) {
        return seat != null && seat == turretController(type, taken);
    }
}
