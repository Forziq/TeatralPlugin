package dev.egorgrigorciuc.forzammilmod;

/** Pure clock: a vehicle with nobody driving for {@link #LIMIT_MILLIS} should be removed. */
public final class IdleTimer {

    public static final long LIMIT_MILLIS = 10 * 60 * 1000L;

    private long idleSince;

    public IdleTimer(long nowMillis) {
        this.idleSince = nowMillis;
    }

    /** Call regularly. Returns true once the vehicle has gone without a driver for the whole limit. */
    public boolean update(boolean hasDriver, long nowMillis) {
        if (hasDriver) {
            idleSince = nowMillis;
            return false;
        }
        return nowMillis - idleSince >= LIMIT_MILLIS;
    }
}
