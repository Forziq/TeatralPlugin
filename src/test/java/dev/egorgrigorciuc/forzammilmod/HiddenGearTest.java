package dev.egorgrigorciuc.forzammilmod;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class HiddenGearTest {

    private final HiddenGear gear = new HiddenGear();
    private final UUID a = UUID.randomUUID();
    private final UUID b = UUID.randomUUID();
    private final UUID v1 = UUID.randomUUID();
    private final UUID v2 = UUID.randomUUID();

    @Test
    void hideThenReleaseReturnsViewers() {
        gear.hide(a, List.of(v1, v2));
        assertTrue(gear.isHidden(a));
        assertEquals(Set.of(v1, v2), gear.release(a));
        assertFalse(gear.isHidden(a));
    }

    @Test
    void releaseTwiceIsEmptySecondTime() {
        gear.hide(a, List.of(v1));
        assertEquals(Set.of(v1), gear.release(a));
        assertTrue(gear.release(a).isEmpty());
    }

    @Test
    void releaseOfNeverHiddenIsEmpty() {
        assertTrue(gear.release(a).isEmpty());
    }

    @Test
    void addViewerOnlyWhileHidden() {
        assertFalse(gear.addViewer(a, v1));
        gear.hide(a, List.of());
        assertTrue(gear.addViewer(a, v1));
        assertFalse(gear.addViewer(a, v1));
        assertEquals(Set.of(v1), gear.viewers(a));
        gear.release(a);
        assertFalse(gear.addViewer(a, v2));
        assertFalse(gear.isHidden(a));
    }

    @Test
    void emoterIsNeverTheirOwnViewer() {
        gear.hide(a, List.of(a, v1));
        assertFalse(gear.addViewer(a, a));
        assertEquals(Set.of(v1), gear.release(a));
    }

    @Test
    void releaseAllReturnsEverythingAndClears() {
        gear.hide(a, List.of(v1));
        gear.hide(b, List.of(v2));
        var all = gear.releaseAll();
        assertEquals(Set.of(v1), all.get(a));
        assertEquals(Set.of(v2), all.get(b));
        assertFalse(gear.isHidden(a));
        assertFalse(gear.isHidden(b));
        assertTrue(gear.releaseAll().isEmpty());
    }

    @Test
    void everyStopReasonReleasesTheSameWay() {
        for (EmoteService.StopReason reason : EmoteService.StopReason.values()) {
            gear.hide(a, List.of(v1));
            assertEquals(Set.of(v1), gear.release(a), reason.name());
            assertFalse(gear.isHidden(a), reason.name());
        }
    }
}
