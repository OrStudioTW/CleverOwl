package dev.local.duolingoxposed;

import java.util.Map;

/** Configuration contains no account identifiers or credentials. */
public final class FeatureSettings {
    public enum Membership { ORIGINAL, SUPER, MAX }
    public static final String PACKAGE = "dev.local.duolingoxposed";
    public static final String TARGET = "com.duolingo";
    public static final String PREFS = "features";
    public static final int SCHEMA = 1;
    public static final int MAX_XP_FACTOR = 3;
    public static final FeatureSettings OFF = new FeatureSettings(false, Membership.ORIGINAL, 1, 0, false);

    public final boolean enabled;
    public final Membership membership;
    public final int xpFactor;
    public final int boostExtraMinutes;
    public final boolean unlimitedTimer;

    public FeatureSettings(boolean enabled, Membership membership, int xpFactor, int extraMinutes, boolean timer) {
        this.enabled = enabled;
        this.membership = membership == null ? Membership.ORIGINAL : membership;
        // Retain schema 1: old valid high settings become x3, malformed values stay off.
        this.xpFactor = xpFactor >= 1 && xpFactor <= 10 ? Math.min(xpFactor, MAX_XP_FACTOR) : 1;
        this.boostExtraMinutes = extraMinutes >= 0 && extraMinutes <= 1440 ? extraMinutes : 0;
        this.unlimitedTimer = timer;
    }

    public static FeatureSettings from(Map<String, ?> values) {
        if (!Integer.valueOf(SCHEMA).equals(values.get("schema"))) return OFF;
        Membership membership;
        try { membership = Membership.valueOf(String.valueOf(values.get("membership"))); }
        catch (IllegalArgumentException e) { membership = Membership.ORIGINAL; }
        return new FeatureSettings(Boolean.TRUE.equals(values.get("enabled")), membership,
                integer(values.get("xp_factor"), 1), integer(values.get("extra_minutes"), 0),
                Boolean.TRUE.equals(values.get("timer")));
    }

    private static int integer(Object value, int fallback) {
        return value instanceof Integer ? (Integer) value : fallback;
    }
}
