package dev.local.duolingoxposed;

import java.util.HashMap;
import java.util.Map;
import org.junit.Test;
import static org.junit.Assert.*;

public class SettingsContractTest {
    private Map<String, Object> valid() {
        return new HashMap<>(Map.of("schema", 1, "enabled", true, "membership", "MAX",
                "xp_factor", 3, "extra_minutes", 1440, "timer", true));
    }
    @Test public void acceptsEverySettingWithoutDroppingValues() {
        FeatureSettings value = SettingsContract.validate(valid());
        assertTrue(value.enabled); assertTrue(value.unlimitedTimer);
        assertEquals(FeatureSettings.Membership.MAX, value.membership);
        assertEquals(3, value.xpFactor); assertEquals(1440, value.boostExtraMinutes);
    }
    @Test public void rejectsUnknownMissingAndFutureSchemaFields() {
        Map<String, Object> values = valid(); values.put("other", true);
        assertThrows(IllegalArgumentException.class, () -> SettingsContract.validate(values));
        values.remove("other"); values.remove("enabled");
        assertThrows(IllegalArgumentException.class, () -> SettingsContract.validate(values));
        values.put("enabled", true); values.put("schema", 2);
        assertThrows(IllegalArgumentException.class, () -> SettingsContract.validate(values));
    }
    @Test public void rejectsUnsupportedOptions() {
        for (var entry : Map.of("xp_factor", 5, "extra_minutes", 5, "membership", "GOLD").entrySet()) {
            Map<String, Object> values = valid(); values.put(entry.getKey(), entry.getValue());
            assertThrows(IllegalArgumentException.class, () -> SettingsContract.validate(values));
        }
    }
    @Test public void rejectsWrongTypesAndNulls() {
        for (String key : valid().keySet()) {
            Map<String, Object> values = valid(); values.put(key, new Object());
            assertThrows(IllegalArgumentException.class, () -> SettingsContract.validate(values));
            values.put(key, null);
            assertThrows(IllegalArgumentException.class, () -> SettingsContract.validate(values));
        }
    }
    @Test public void acceptsOnlyTheInspectedTargetIdentity() {
        assertTrue(SettingsContract.trustedTarget(FeatureSettings.TARGET, false, 1, SettingsContract.SIGNER));
    }
    @Test public void rejectsSpoofedOrAmbiguousTargetIdentity() {
        assertFalse(SettingsContract.trustedTarget("com.duolingo.fake", false, 1, SettingsContract.SIGNER));
        assertFalse(SettingsContract.trustedTarget(FeatureSettings.TARGET, true, 1, SettingsContract.SIGNER));
        assertFalse(SettingsContract.trustedTarget(FeatureSettings.TARGET, false, 2, SettingsContract.SIGNER));
        assertFalse(SettingsContract.trustedTarget(FeatureSettings.TARGET, false, 1, "bad"));
        assertFalse(SettingsContract.trustedTarget(FeatureSettings.TARGET, false, 1, null));
    }
}
