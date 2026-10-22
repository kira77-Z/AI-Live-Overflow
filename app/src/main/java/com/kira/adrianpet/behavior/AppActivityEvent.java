package com.kira.adrianpet.behavior;

public final class AppActivityEvent {
    private final String packageName;
    private final boolean movedToForeground;
    private final long timestampMs;

    public AppActivityEvent(String packageName, boolean movedToForeground, long timestampMs) {
        this.packageName = packageName;
        this.movedToForeground = movedToForeground;
        this.timestampMs = timestampMs;
    }

    public String getPackageName() {
        return packageName;
    }

    public boolean isMovedToForeground() {
        return movedToForeground;
    }

    public long getTimestampMs() {
        return timestampMs;
    }
}
