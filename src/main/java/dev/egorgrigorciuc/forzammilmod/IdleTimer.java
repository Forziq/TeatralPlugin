package dev.egorgrigorciuc.forzammilmod;

/** Pure clock: a vehicle with nobody driving for the limit should be removed. */
public final class IdleTimer {

    /** Used when config.yml has no idle-despawn-minutes value. */
    public static final int DEFAULT_MINUTES = 10;
    public static final long LIMIT_MILLIS = DEFAULT_MINUTES * 60 * 1000L;
    /** Limit meaning "never despawn". */
    public static final long NEVER = Long.MAX_VALUE;

    private long idleSince;

    public IdleTimer(long nowMillis) {
        this.idleSince = nowMillis;
    }

    /** Config minutes to a limit in millis. 0 or less turns idle despawn off. */
    public static long limitMillis(int minutes) {
        return minutes <= 0 ? NEVER : minutes * 60_000L;
    }

    public boolean update(boolean hasDriver, long nowMillis) {
        return update(hasDriver, nowMillis, LIMIT_MILLIS);
    }

    /** Call regularly. Returns true once the vehicle has gone without a driver for the whole limit. */
    public boolean update(boolean hasDriver, long nowMillis, long limitMillis) {
        if (hasDriver) {
            idleSince = nowMillis;
            return false;
        }
        return nowMillis - idleSince >= limitMillis;
    }
}
