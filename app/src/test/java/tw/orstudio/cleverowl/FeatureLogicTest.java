package tw.orstudio.cleverowl;

import org.junit.Test;
import java.util.*;
import static org.junit.Assert.*;

public class FeatureLogicTest {
    @Test public void pathPreviewUsesTheSameMultiplierOnce() {
        assertEquals(75, (int) Math.ceil(25 * FeatureLogic.xpMultiplier(null, false, 3)));
        assertEquals(75, (int) Math.ceil(25 * FeatureLogic.xpMultiplier(2.0, true, 3)));
        assertEquals(25, (int) Math.ceil(25 * FeatureLogic.xpMultiplier(null, false, 1)));
    }
    @Test public void missingOrFutureSchemaDisablesEverything() {
        assertFalse(FeatureSettings.from(Map.of("enabled", true)).enabled);
        assertFalse(FeatureSettings.from(Map.of("schema", 2, "enabled", true)).enabled);
        assertFalse(FeatureSettings.from(Map.of("schema", "1", "enabled", true)).enabled);
    }
    @Test public void malformedAndOutOfRangeSettingsDoNotEnableFeatures() {
        FeatureSettings settings = FeatureSettings.from(Map.of("schema", 1, "enabled", "true",
                "membership", "GOLD", "xp_factor", 1000000, "extra_minutes", -5, "timer", "true"));
        assertFalse(settings.enabled);
        assertEquals(FeatureSettings.Membership.ORIGINAL, settings.membership);
        assertEquals(1, settings.xpFactor);
        assertEquals(0, settings.boostExtraMinutes);
        assertFalse(settings.unlimitedTimer);
    }
    @Test public void independentSettingsArePreserved() {
        FeatureSettings settings = FeatureSettings.from(Map.of("schema", 1, "enabled", true,
                "membership", "MAX", "xp_factor", 3, "extra_minutes", 60, "timer", true));
        assertTrue(settings.enabled);
        assertEquals(FeatureSettings.Membership.MAX, settings.membership);
        assertEquals(3, settings.xpFactor);
        assertEquals(60, settings.boostExtraMinutes);
        assertTrue(settings.unlimitedTimer);
    }
    @Test public void multiplierUsesEffectiveOriginalBoost() {
        assertEquals(3, FeatureLogic.xpMultiplier(null, false, 3), 0);
        assertEquals(3, FeatureLogic.xpMultiplier(null, true, 3), 0);
        assertEquals(3, FeatureLogic.xpMultiplier(1.5, true, 3), 0);
        assertEquals(3, FeatureLogic.xpMultiplier(2.0, true, 2), 0);
        assertEquals(2, FeatureLogic.xpMultiplier(null, false, 2), 0);
        assertEquals(3, FeatureLogic.xpMultiplier(10.0, false, 3), 0);
    }
    @Test public void invalidMultiplierPreservesTheCallersAbilityToSkip() {
        assertThrows(IllegalArgumentException.class, () -> FeatureLogic.xpMultiplier(Double.NaN, true, 2));
        assertThrows(IllegalArgumentException.class, () -> FeatureLogic.xpMultiplier(Double.POSITIVE_INFINITY, true, 2));
        assertThrows(IllegalArgumentException.class, () -> FeatureLogic.xpMultiplier(0.0, true, 2));
        assertThrows(IllegalArgumentException.class, () -> FeatureLogic.xpMultiplier(1.0, true, 11));
        for (int factor : new int[]{4, 5, 10})
            assertThrows(IllegalArgumentException.class, () -> FeatureLogic.xpMultiplier(1.0, false, factor));
    }
    @Test public void oldHighFactorsClampWithoutChangingOtherSettings() {
        for (int factor : new int[]{4, 5, 10}) {
            FeatureSettings settings = FeatureSettings.from(Map.of("schema", 1, "enabled", true,
                    "membership", "MAX", "xp_factor", factor, "extra_minutes", 60, "timer", true));
            assertEquals(3, settings.xpFactor);
            assertTrue(settings.enabled); assertTrue(settings.unlimitedTimer);
            assertEquals(FeatureSettings.Membership.MAX, settings.membership);
            assertEquals(60, settings.boostExtraMinutes);
        }
    }
    @Test public void originalModePreservesNativeBoost() {
        assertEquals(6, FeatureLogic.xpMultiplier(6.0, true, 1), 0);
        assertEquals(2, FeatureLogic.xpMultiplier(null, true, 1), 0);
        assertEquals(1, FeatureLogic.xpMultiplier(null, false, 1), 0);
    }
    @Test public void extensionKeepsAStableExpiryAcrossRepeatedReads() {
        for (int elapsed = 0; elapsed < 500; elapsed++)
            assertEquals(600 - elapsed + 900, FeatureLogic.extendedSeconds(600 - elapsed, 100000, 15));
    }
    @Test public void extensionDoesNotCreateAnUninitializedBoost() {
        assertEquals(-20, FeatureLogic.extendedSeconds(-20, 0, 60));
        assertEquals(-20, FeatureLogic.extendedSeconds(-20, -1, 60));
        assertEquals(20, FeatureLogic.extendedSeconds(20, 100000, 0));
    }
    @Test public void extensionStillExpiresAtItsNewBoundary() {
        assertEquals(1, FeatureLogic.extendedSeconds(-899, 100000, 15));
        assertEquals(0, FeatureLogic.extendedSeconds(-900, 100000, 15));
        assertEquals(-1, FeatureLogic.extendedSeconds(-901, 100000, 15));
        assertEquals(Long.MAX_VALUE, FeatureLogic.extendedSeconds(Long.MAX_VALUE - 1, 100000, 15));
    }
    @Test public void inspectedReducerNeverReachesItsExpiryThresholdWhenEnabled() {
        int remaining = FeatureLogic.nonExpiringTick(90);
        for (int tick = 0; tick < 10000; tick++) {
            assertTrue("The inspected reducer completes at <= 1", remaining > 1);
            remaining = FeatureLogic.nonExpiringTick(remaining - 1);
        }
        assertEquals(2, remaining);
    }
    @Test public void unmodifiedReducerStillExpiresWhenTheHookIsDisabled() {
        int remaining = 90;
        int ticks = 0;
        while (remaining > 1) { remaining--; ticks++; }
        assertEquals(89, ticks);
        assertEquals(1, remaining);
    }
}
