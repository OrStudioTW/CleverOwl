package tw.orstudio.cleverowl;

public final class FeatureLogic {
    private FeatureLogic() {}

    /** Apply the selected factor once, capped at x3 while modification is enabled. */
    public static double xpMultiplier(Double highest, boolean active, int factor) {
        if (factor < 1 || factor > FeatureSettings.MAX_XP_FACTOR) throw new IllegalArgumentException("factor");
        double base = active ? (highest == null ? 2.0 : highest) : 1.0;
        if (!Double.isFinite(base) || base < 1.0) throw new IllegalArgumentException("original multiplier");
        double result = factor == 1 ? base : Math.min(FeatureSettings.MAX_XP_FACTOR, base * factor);
        if (!Double.isFinite(result)) throw new IllegalArgumentException("multiplier overflow");
        return result;
    }

    /** Keep the completed-session boost flag consistent with an already calculated multiplier. */
    public static boolean sessionHasBoost(double effectiveMultiplier, boolean original) {
        return original || (Double.isFinite(effectiveMultiplier) && effectiveMultiplier > 1.0);
    }

    /** The offset is applied to the original expiry on read, never written back or compounded. */
    public static long extendedSeconds(long originalRemaining, long originalExpiry, int extraMinutes) {
        if (originalExpiry <= 0 || extraMinutes <= 0 || extraMinutes > 1440) return originalRemaining;
        long offset = extraMinutes * 60L;
        return originalRemaining > Long.MAX_VALUE - offset ? Long.MAX_VALUE : originalRemaining + offset;
    }

    /** The inspected reducer completes at <= 1 second, before creating the next Tick. */
    public static int nonExpiringTick(int seconds) { return Math.max(2, seconds); }
}
