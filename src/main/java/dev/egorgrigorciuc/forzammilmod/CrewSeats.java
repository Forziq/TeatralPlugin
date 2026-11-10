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
        PASSENGER2("passenger2"),
        PASSENGER3("passenger3"),
        PASSENGER4("passenger4"),
        PASSENGER5("passenger5"),
        PASSENGER6("passenger6"),
        PASSENGER7("passenger7");

        final String bone;

        Seat(String bone) {
            this.bone = bone;
        }
    }

    private CrewSeats() { }

    /** Seats the type has, in the order people fill them. */
    static List<Seat> seatsOf(VehicleType type) {
        return switch (type) {
            case TANK, TANK_HEAVY -> List.of(Seat.DRIVER, Seat.GUNNER);
            // One crew seat, behind the shield: the gunner turns the gun and fires. It is the driver seat so A and D work.
            case ARTILLERY -> List.of(Seat.DRIVER);
            case JEEP, JEEP_HEAVY -> List.of(Seat.DRIVER, Seat.PASSENGER1, Seat.PASSENGER2);
            // Driver, cab passenger, then six on the benches in the back.
            case TRUCK -> List.of(Seat.DRIVER, Seat.PASSENGER1, Seat.PASSENGER2, Seat.PASSENGER3, Seat.PASSENGER4,
                    Seat.PASSENGER5, Seat.PASSENGER6, Seat.PASSENGER7);
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
        if (!type.hasGun()) return null;
        if (taken.contains(Seat.GUNNER)) return Seat.GUNNER;
        if (taken.contains(Seat.DRIVER)) return Seat.DRIVER;
        return null;
    }

    /** True if whoever sits in {@code seat} may fire the gun. */
    static boolean canFire(VehicleType type, Collection<Seat> taken, Seat seat) {
        return seat != null && seat == turretController(type, taken);
    }
}
