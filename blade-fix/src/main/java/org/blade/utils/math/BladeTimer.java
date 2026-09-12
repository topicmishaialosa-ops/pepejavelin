package org.blade.utils.math;

public final class BladeTimer {
    private long start;

    public BladeTimer() {
        this.reset();
    }

    public void reset() {
        this.start = System.currentTimeMillis();
    }

    public boolean finished(long delayMs) {
        return System.currentTimeMillis() - this.start >= delayMs;
    }

    public long elapsed() {
        return System.currentTimeMillis() - this.start;
    }
}
