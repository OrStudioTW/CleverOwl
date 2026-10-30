package tw.orstudio.cleverowl;

import java.util.Map;
import java.util.Set;
import java.util.Arrays;
import java.util.HashSet;

/** The only configuration fields exposed to the in-app settings screen. */
public final class SettingsContract {
    public static final String SIGNER = "1233ff6de4c70ef4d7deae54af15b167fc7cfd2539517bd2175870f3cb771731";
    private static final Set<String> KEYS = new HashSet<>(Arrays.asList("schema", "enabled", "membership", "xp_factor", "extra_minutes", "timer"));
    private SettingsContract() {}

    public static FeatureSettings validate(Map<String, ?> values) {
        if (!values.keySet().equals(KEYS) || !Integer.valueOf(FeatureSettings.SCHEMA).equals(values.get("schema"))
                || !(values.get("enabled") instanceof Boolean) || !(values.get("timer") instanceof Boolean)
                || !(values.get("xp_factor") instanceof Integer) || !(values.get("extra_minutes") instanceof Integer)
                || !(values.get("membership") instanceof String)
                || !Arrays.asList("ORIGINAL", "SUPER", "MAX").contains(values.get("membership")))
            throw new IllegalArgumentException("Invalid configuration");
        int factor = (Integer) values.get("xp_factor"), minutes = (Integer) values.get("extra_minutes");
        if (factor < 1 || factor > 3 || !Arrays.asList(0, 15, 30, 60, 180, 1440).contains(minutes))
            throw new IllegalArgumentException("Unsupported option");
        return FeatureSettings.from(values);
    }

    public static boolean trustedTarget(String packageName, boolean multipleSigners, int signerCount, String signer) {
        return FeatureSettings.TARGET.equals(packageName) && !multipleSigners && signerCount == 1 && SIGNER.equals(signer);
    }
}
