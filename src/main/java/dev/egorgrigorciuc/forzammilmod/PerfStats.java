package dev.egorgrigorciuc.forzammilmod;

import java.util.LinkedHashMap;
import java.util.Map;

/** Time our own repeating tasks take per tick. Cheap enough to stay on. Main thread only. */
final class PerfStats {

    private final Map<String, TickTimer> timers = new LinkedHashMap<>();

    void record(String name, long nanos) {
        timers.computeIfAbsent(name, k -> new TickTimer()).record(nanos);
    }

    Map<String, TickTimer> timers() {
        return timers;
    }

    /** Average of the summed per-tick time of all tasks (they run once per tick each). */
    double totalAverageMillis() {
        double sum = 0;
        for (TickTimer t : timers.values()) sum += t.averageMillis();
        return sum;
    }

    void reset() {
        for (TickTimer t : timers.values()) t.reset();
    }
}
