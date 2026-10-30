package dev.local.duolingoxposed;

/** Parameter positions verified against the inspected APK, never guessed from field names. */
public final class ConstructorInputs {
    private ConstructorInputs() {}

    public static boolean membershipShape(Class<?>[] types, Class<?> level) {
        int offset = types.length == 85 ? 0 : types.length == 109 ? 4 : -1;
        if (offset < 0 || types[24 + offset] != boolean.class || types[79 + offset] != level) return false;
        if (!types[offset].getName().equals("com.duolingo.data.ads.AdsConfig")) return false;
        if (offset == 4) {
            for (int i = 0; i < 4; i++) if (types[i] != int.class) return false;
            for (int i : new int[]{94, 95, 96, 102}) if (types[i] != boolean.class) return false;
            if (types[97] != int.class) return false;
        }
        return true;
    }

    public static void membership(Object[] args, Object level, boolean max) {
        int offset = args.length == 85 ? 0 : args.length == 109 ? 4 : -1;
        if (offset < 0) throw new IllegalArgumentException("Unknown User constructor");
        args[24 + offset] = true;
        args[79 + offset] = level;
        if (offset == 4) {
            // Mark the derived values explicit, including states reconstructed from caches.
            args[2] = (Integer) args[2] | 0x3c000000;
            args[3] = (Integer) args[3] | 4;
            args[94] = max;
            args[95] = true;
            args[96] = false;
            args[97] = max ? 2 : 1;
            args[102] = !max;
        }
    }

    /** Reference module uses 60,000 seconds; preserve XP and challenge counters. */
    public static void practice(Object[] args, int timePosition) {
        args[timePosition] = Math.max((Integer) args[timePosition], 60000);
    }
}
