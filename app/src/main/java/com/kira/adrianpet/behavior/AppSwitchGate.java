package com.kira.adrianpet.behavior;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

public final class AppSwitchGate {
    private static final Set<String> DEFAULT_IGNORED_PACKAGES = new HashSet<>(Arrays.asList(
        "com.kira.adrianpet",
        "com.android.systemui",
        "com.android.settings"
    ));

    private final int stableSamples;
    private final long cooldownMs;
    private final Set<String> ignoredPackages;
    private String candidatePackage;
    private int candidateSamples;
    private String activePackage;
    private Long lastAnnouncementAt;

    public AppSwitchGate() {
        this(2, 12_000L);
    }

    public AppSwitchGate(int stableSamples, long cooldownMs) {
        this(stableSamples, cooldownMs, DEFAULT_IGNORED_PACKAGES);
    }

    AppSwitchGate(int stableSamples, long cooldownMs, Set<String> ignoredPackages) {
        if (stableSamples <= 0) throw new IllegalArgumentException("stableSamples must be positive");
        if (cooldownMs < 0L) throw new IllegalArgumentException("cooldownMs must not be negative");
        this.stableSamples = stableSamples;
        this.cooldownMs = cooldownMs;
        this.ignoredPackages = ignoredPackages;
    }

    public String observe(String packageName, long nowMs) {
        if (packageName == null || packageName.trim().isEmpty() || ignoredPackages.contains(packageName)) {
            clearCandidate();
            return null;
        }
        if (packageName.equals(activePackage)) {
            clearCandidate();
            return null;
        }

        if (!packageName.equals(candidatePackage)) {
            candidatePackage = packageName;
            candidateSamples = 1;
        } else {
            candidateSamples += 1;
        }

        if (candidateSamples < stableSamples) return null;
        if (lastAnnouncementAt != null && nowMs - lastAnnouncementAt < cooldownMs) return null;

        activePackage = packageName;
        lastAnnouncementAt = nowMs;
        clearCandidate();
        return packageName;
    }

    private void clearCandidate() {
        candidatePackage = null;
        candidateSamples = 0;
    }
}
