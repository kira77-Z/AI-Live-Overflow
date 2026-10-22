package com.kira.adrianpet.behavior;

public final class LatestForegroundAppSelector {
    private LatestForegroundAppSelector() {}

    public static String select(Iterable<AppActivityEvent> events) {
        AppActivityEvent latest = null;
        for (AppActivityEvent event : events) {
            if (!event.isMovedToForeground()) continue;
            String packageName = event.getPackageName();
            if (packageName == null || packageName.trim().isEmpty()) continue;
            if (latest == null || event.getTimestampMs() > latest.getTimestampMs()) latest = event;
        }
        return latest == null ? null : latest.getPackageName();
    }
}
