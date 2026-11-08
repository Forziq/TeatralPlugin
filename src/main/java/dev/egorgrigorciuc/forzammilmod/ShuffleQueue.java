package dev.egorgrigorciuc.forzammilmod;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

/** Random order without repeats until every id has come up, then a fresh shuffle. */
public final class ShuffleQueue {

    private final List<String> ids;
    private final Random random;
    private final List<String> queue = new ArrayList<>();
    private String last;

    public ShuffleQueue(List<String> ids, Random random) {
        this.ids = List.copyOf(ids);
        this.random = random;
    }

    public void reset() {
        queue.clear();
        last = null;
    }

    /** Next id, or null when there are no tracks at all. */
    public String next() {
        if (ids.isEmpty()) return null;
        if (queue.isEmpty()) {
            queue.addAll(ids);
            Collections.shuffle(queue, random);
            if (ids.size() > 1 && queue.get(0).equals(last)) {
                Collections.swap(queue, 0, 1 + random.nextInt(queue.size() - 1));
            }
        }
        last = queue.remove(0);
        return last;
    }
}
