package dev.egorgrigorciuc.forzammilmod;

import java.util.List;

/** The flight of one shell, as the stretches to ray-trace each tick. Pure, no Bukkit types. */
interface ShellPath {

    /** The straight stretches to test this tick, in order, and moves the shell on. Empty once the flight is over. */
    List<ShellModel.Segment> nextTick();

    /** Distance flown so far, in blocks. */
    double travelled();
}
