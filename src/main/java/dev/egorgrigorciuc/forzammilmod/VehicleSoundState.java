package dev.egorgrigorciuc.forzammilmod;

import dev.egorgrigorciuc.forzammilmod.SoundLoop.Action;

/**
 * Decides which vehicle sounds start and stop each tick. Pure: the Bukkit side ({@link VehicleSounds}) just
 * plays what this says. Lengths are the sound file lengths in ticks.
 */
final class VehicleSoundState {

    static final int TANK_IDLE_TICKS = 80;
    static final int JEEP_ENGINE_TICKS = 60;
    static final int TRACKS_TICKS = 80;
    static final int TURRET_TICKS = 60;
    /** Tank "moving" thresholds: blocks per tick (a turn on the spot counts as yaw degrees * {@link #YAW_WEIGHT}). */
    static final double MOVE_ON = 0.01, MOVE_OFF = 0.004;
    static final double YAW_WEIGHT = 0.02;
    static final int MOVE_HOLD_TICKS = 4;
    /** Turret turning thresholds: degrees per tick (the turret tops out at 2 per tick). */
    static final double TURRET_ON = 0.3, TURRET_OFF = 0.1;
    static final int TURRET_HOLD_TICKS = 5;
    static final int REV_COOLDOWN_TICKS = 40;
    static final int HORN_COOLDOWN_TICKS = 20;

    record Out(Action engine, Action tracks, Action turret, boolean rev, boolean turretStop) {}

    private final boolean tank;
    private final SoundLoop engine;
    private final SoundLoop tracks = new SoundLoop(TRACKS_TICKS);
    private final SoundLoop turret = new SoundLoop(TURRET_TICKS);
    private final Hysteresis moving = new Hysteresis(MOVE_ON, MOVE_OFF, MOVE_HOLD_TICKS);
    private final Hysteresis turning = new Hysteresis(TURRET_ON, TURRET_OFF, TURRET_HOLD_TICKS);
    private final TickCooldown revCooldown = new TickCooldown(REV_COOLDOWN_TICKS);
    private final TickCooldown hornCooldown = new TickCooldown(HORN_COOLDOWN_TICKS);
    private boolean hadThrottle;

    VehicleSoundState(boolean tank) {
        this.tank = tank;
        this.engine = new SoundLoop(tank ? TANK_IDLE_TICKS : JEEP_ENGINE_TICKS);
    }

    /**
     * @param occupied    somebody sits in the vehicle
     * @param throttle    the driver holds forward or back
     * @param moveMetric  blocks per tick of speed (tanks: use {@link #moveMetric})
     * @param turretDelta degrees the turret turned this tick
     */
    Out update(long now, boolean occupied, boolean throttle, double moveMetric, double turretDelta) {
        boolean isMoving = moving.update(moveMetric);
        boolean isTurning = turning.update(turretDelta);
        Action e = engine.update(occupied, now);
        Action t = tracks.update(tank && occupied && isMoving, now);
        Action tu = turret.update(tank && occupied && isTurning, now);
        boolean rev = tank && occupied && throttle && !hadThrottle && revCooldown.tryUse(now);
        hadThrottle = throttle;
        return new Out(e, t, tu, rev, tu == Action.STOP && occupied);
    }

    /** True if the horn should sound now (jump pressed, outside the cooldown). */
    boolean horn(long now) {
        return hornCooldown.tryUse(now);
    }

    /** Speed in blocks per tick, plus a turn-on-the-spot allowance. */
    static double moveMetric(double speed, float yawDelta) {
        return Math.max(Math.abs(speed), Math.abs(yawDelta) * YAW_WEIGHT);
    }
}
