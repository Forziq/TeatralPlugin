package dev.egorgrigorciuc.forzammilmod;

/** Pure radio rules: what is playing, loop and shuffle flags, and what happens when a track ends. */
public final class RadioPlan {

    public sealed interface NextStep permits Replay, Next, Stop {}

    public record Replay(String id) implements NextStep {}

    public record Next(String id) implements NextStep {}

    public record Stop() implements NextStep {}

    private final ShuffleQueue queue;
    private String current;
    private boolean loop;
    private boolean shuffle;

    public RadioPlan(ShuffleQueue queue) {
        this.queue = queue;
    }

    public String current() {
        return current;
    }

    public boolean loop() {
        return loop;
    }

    public boolean shuffle() {
        return shuffle;
    }

    /** A chosen track: plays once (or loops), shuffle off. */
    public void play(String id) {
        current = id;
        shuffle = false;
        queue.reset();
    }

    /** Turns shuffle on and returns the first track, or null if there are none. Loop is turned off. */
    public String startShuffle() {
        queue.reset();
        loop = false;
        String first = queue.next();
        if (first == null) return null;
        shuffle = true;
        current = first;
        return first;
    }

    public void stop() {
        current = null;
        loop = false;
        shuffle = false;
        queue.reset();
    }

    /** Flips loop. Returns the new state; does nothing (stays false) when nothing is playing. */
    public boolean toggleLoop() {
        if (current == null) return false;
        loop = !loop;
        return loop;
    }

    /** Called when the current track ran out. Loop beats shuffle; with neither, the radio stops. */
    public NextStep onTrackEnd() {
        if (current == null) return new Stop();
        if (loop) return new Replay(current);
        if (shuffle) {
            String next = queue.next();
            if (next != null) {
                current = next;
                return new Next(next);
            }
        }
        stop();
        return new Stop();
    }
}
