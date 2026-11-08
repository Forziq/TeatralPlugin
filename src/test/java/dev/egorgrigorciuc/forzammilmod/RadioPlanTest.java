package dev.egorgrigorciuc.forzammilmod;

import org.junit.jupiter.api.Test;

import java.io.StringReader;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class RadioPlanTest {

    private static final List<String> IDS = List.of("a", "b", "c", "d");

    private RadioPlan plan() {
        return new RadioPlan(new ShuffleQueue(IDS, new Random(42)));
    }

    @Test
    void queueHasNoRepeatsUntilEmpty() {
        ShuffleQueue q = new ShuffleQueue(IDS, new Random(1));
        for (int cycle = 0; cycle < 20; cycle++) {
            Set<String> seen = new HashSet<>();
            for (int i = 0; i < IDS.size(); i++) assertTrue(seen.add(q.next()));
            assertEquals(new HashSet<>(IDS), seen);
        }
    }

    @Test
    void noSameTrackAcrossReshuffle() {
        ShuffleQueue q = new ShuffleQueue(IDS, new Random(7));
        String prev = null;
        for (int i = 0; i < 400; i++) {
            String n = q.next();
            assertNotEquals(prev, n);
            prev = n;
        }
    }

    @Test
    void emptyQueueGivesNull() {
        assertNull(new ShuffleQueue(List.of(), new Random()).next());
    }

    @Test
    void playWithoutLoopStops() {
        RadioPlan p = plan();
        p.play("a");
        assertInstanceOf(RadioPlan.Stop.class, p.onTrackEnd());
        assertNull(p.current());
    }

    @Test
    void loopReplays() {
        RadioPlan p = plan();
        p.play("b");
        assertTrue(p.toggleLoop());
        assertEquals(new RadioPlan.Replay("b"), p.onTrackEnd());
        assertFalse(p.toggleLoop());
        assertInstanceOf(RadioPlan.Stop.class, p.onTrackEnd());
    }

    @Test
    void loopBeatsShuffle() {
        RadioPlan p = plan();
        String first = p.startShuffle();
        assertTrue(p.toggleLoop());
        assertTrue(p.shuffle());
        assertEquals(new RadioPlan.Replay(first), p.onTrackEnd());
    }

    @Test
    void shuffleAdvances() {
        RadioPlan p = plan();
        Set<String> seen = new HashSet<>();
        seen.add(p.startShuffle());
        for (int i = 0; i < 3; i++) {
            var step = assertInstanceOf(RadioPlan.Next.class, p.onTrackEnd());
            assertTrue(seen.add(step.id()));
        }
        assertEquals(4, seen.size());
    }

    @Test
    void stopClearsBoth() {
        RadioPlan p = plan();
        p.startShuffle();
        p.toggleLoop();
        p.stop();
        assertFalse(p.loop());
        assertFalse(p.shuffle());
        assertNull(p.current());
        assertInstanceOf(RadioPlan.Stop.class, p.onTrackEnd());
    }

    @Test
    void playTurnsShuffleOff() {
        RadioPlan p = plan();
        p.startShuffle();
        p.play("c");
        assertFalse(p.shuffle());
        assertInstanceOf(RadioPlan.Stop.class, p.onTrackEnd());
    }

    @Test
    void loopDoesNothingWhenIdle() {
        assertFalse(plan().toggleLoop());
    }

    @Test
    void parsesTracks() {
        String yml = """
                tracks:
                  - id: one
                    en: "One"
                    ru: "Один"
                    length_ms: 1000
                  - id: bad
                    en: "No length"
                  - id: two
                    en: "Two"
                    length_ms: 2500
                """;
        List<Track> t = Track.parse(new StringReader(yml));
        assertEquals(2, t.size());
        assertEquals("Один", t.get(0).name("ru"));
        assertEquals("One", t.get(0).name("en"));
        assertEquals("Two", t.get(1).name("ru"));
        assertEquals("forzammilmod:music.one", t.get(0).sound());
    }
}
