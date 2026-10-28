package com.ttclient.util;

/**
 * Client tick-rate multiplier.
 * Applied by {@link com.ttclient.mixin.MixinDeltaTrackerTimer} to
 * {@code DeltaTracker.Timer.advanceGameTime} — scales how many game ticks
 * fire per real-time second (1.0 = normal, 8.0 = 8x).
 */
public final class TimerHooks {
    /** Multiplier for client game ticks. 1.0 = vanilla. */
    public static volatile float speed = 1.0f;

    private TimerHooks() {}

    public static void set(float s) {
        if (s < 0.05f) s = 0.05f;
        if (s > 20.0f) s = 20.0f;
        speed = s;
    }

    public static void reset() {
        speed = 1.0f;
    }
}
