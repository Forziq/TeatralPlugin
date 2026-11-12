package dev.egorgrigorciuc.forzammilmod;

/** Keeps the time of the last {@link #WINDOW} runs of a task, to show average and worst. Main thread only. */
final class TickTimer {

    static final int WINDOW = 200;

    private final long[] nanos = new long[WINDOW];
    private int next;
    private int count;

    void record(long durationNanos) {
        nanos[next] = Math.max(0, durationNanos);
        next = (next + 1) % WINDOW;
        if (count < WINDOW) count++;
    }

    int samples() {
        return count;
    }

    double averageMillis() {
        if (count == 0) return 0;
        long sum = 0;
        for (int i = 0; i < count; i++) sum += nanos[i];
        return sum / (double) count / 1_000_000.0;
    }

    double maxMillis() {
        long max = 0;
        for (int i = 0; i < count; i++) max = Math.max(max, nanos[i]);
        return max / 1_000_000.0;
    }

    void reset() {
        next = 0;
        count = 0;
    }
}
