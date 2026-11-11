package dev.egorgrigorciuc.forzammilmod;

import dev.egorgrigorciuc.forzammilmod.CrewSeats.Seat;
import dev.egorgrigorciuc.forzammilmod.HudModel.Band;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class HudModelTest {

    @Test
    void bandBoundaries() {
        assertEquals(Band.GREEN, HudModel.band(1.0));
        assertEquals(Band.GREEN, HudModel.band(0.61));
        assertEquals(Band.YELLOW, HudModel.band(0.60));
        assertEquals(Band.YELLOW, HudModel.band(0.31));
        assertEquals(Band.RED, HudModel.band(0.30));
        assertEquals(Band.RED, HudModel.band(0.0));
    }

    @Test
    void bandOutOfRange() {
        assertEquals(Band.GREEN, HudModel.band(1.5));
        assertEquals(Band.RED, HudModel.band(-0.2));
        assertEquals(Band.RED, HudModel.band(Double.NaN));
    }

    @Test
    void progressClamps() {
        assertEquals(0.0, HudModel.progress(0));
        assertEquals(1.0, HudModel.progress(1));
        assertEquals(0.5, HudModel.progress(0.5));
        assertEquals(1.0, HudModel.progress(3.2));
        assertEquals(0.0, HudModel.progress(-0.1));
        assertEquals(0.0, HudModel.progress(Double.NaN));
        assertEquals(1.0, HudModel.progress(Double.POSITIVE_INFINITY));
        assertEquals(0.0, HudModel.progress(Double.NEGATIVE_INFINITY));
    }

    @Test
    void blocksPerSecond() {
        assertEquals(0.0, HudModel.blocksPerSecond(0));
        assertEquals(4.0, HudModel.blocksPerSecond(0.2), 1e-9);
        assertEquals(4.0, HudModel.blocksPerSecond(-0.2), 1e-9);
    }

    @Test
    void formatSpeed() {
        assertEquals("0.0", HudModel.formatSpeed(0));
        assertEquals("4.0", HudModel.formatSpeed(-0.2));
        assertEquals("4.2", HudModel.formatSpeed(0.21));
        assertEquals("1.0", HudModel.formatSpeed(0.0496)); // 0.992 rounds up
        assertEquals("0.1", HudModel.formatSpeed(0.004)); // 0.08
        assertEquals("0.0", HudModel.formatSpeed(0.0001));
        assertEquals("12.0", HudModel.formatSpeed(0.6));
    }

    @Test
    void formatSpeedUsesDotWhateverTheDefaultLocale() {
        java.util.Locale old = java.util.Locale.getDefault();
        try {
            java.util.Locale.setDefault(java.util.Locale.forLanguageTag("ru-RU"));
            assertEquals("4.2", HudModel.formatSpeed(0.21));
        } finally {
            java.util.Locale.setDefault(old);
        }
    }

    @Test
    void onlyDriverSeesSpeed() {
        for (Seat s : Seat.values()) {
            assertEquals(s == Seat.DRIVER, HudModel.showsSpeed(s), s.name());
        }
        assertFalse(HudModel.showsSpeed(null));
    }

    @Test
    void reloadGoesToControllerOnly() {
        assertTrue(HudModel.showsReload(Seat.GUNNER, Seat.GUNNER));
        assertFalse(HudModel.showsReload(Seat.DRIVER, Seat.GUNNER));
        assertTrue(HudModel.showsReload(Seat.DRIVER, Seat.DRIVER));
        assertFalse(HudModel.showsReload(Seat.GUNNER, Seat.DRIVER));
        assertFalse(HudModel.showsReload(Seat.PASSENGER1, Seat.GUNNER));
        assertFalse(HudModel.showsReload(null, null));
        assertFalse(HudModel.showsReload(Seat.DRIVER, null));
    }

    @Test
    void tankWithoutGunnerDriverGetsBothLines() {
        Seat c = CrewSeats.turretController(VehicleType.TANK, Set.of(Seat.DRIVER));
        assertEquals(Seat.DRIVER, c);
        assertTrue(HudModel.showsSpeed(Seat.DRIVER));
        assertTrue(HudModel.showsReload(Seat.DRIVER, c));
    }

    @Test
    void tankWithGunnerSplitsLines() {
        Seat c = CrewSeats.turretController(VehicleType.TANK, List.of(Seat.DRIVER, Seat.GUNNER));
        assertFalse(HudModel.showsReload(Seat.DRIVER, c));
        assertTrue(HudModel.showsReload(Seat.GUNNER, c));
        assertFalse(HudModel.showsSpeed(Seat.GUNNER));
    }

    @Test
    void jeepNeverShowsReload() {
        Set<Seat> all = Set.of(Seat.DRIVER, Seat.PASSENGER1, Seat.PASSENGER2);
        Seat c = CrewSeats.turretController(VehicleType.JEEP, all);
        for (Seat s : all) assertFalse(HudModel.showsReload(s, c), s.name());
        assertTrue(HudModel.showsSpeed(Seat.DRIVER));
        assertFalse(HudModel.showsSpeed(Seat.PASSENGER1));
    }

    @Test
    void heightIsWholeBlocksAndNeverNegative() {
        assertEquals("42", HudModel.formatHeight(41.6));
        assertEquals("0", HudModel.formatHeight(-3));
        assertEquals("0", HudModel.formatHeight(Double.NaN));
    }
}
