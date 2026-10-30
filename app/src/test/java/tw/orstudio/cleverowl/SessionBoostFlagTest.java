package tw.orstudio.cleverowl;

import org.junit.Test;
import static org.junit.Assert.*;

public class SessionBoostFlagTest {
    @Test public void enablesFlagOnlyForValidBoostedMultipliers() {
        for (double multiplier : new double[]{1.5, 2.0, 3.0, 6.0, 10.0})
            assertTrue(FeatureLogic.sessionHasBoost(multiplier, false));
        for (double multiplier : new double[]{-1.0, 0.0, 1.0, Double.NaN, Double.POSITIVE_INFINITY, Double.NEGATIVE_INFINITY})
            assertFalse(FeatureLogic.sessionHasBoost(multiplier, false));
    }
    @Test public void preservesExistingBoostFlagForUnexpectedValues() {
        for (double multiplier : new double[]{-1.0, 0.0, 1.0, 3.0, Double.NaN, Double.POSITIVE_INFINITY})
            assertTrue(FeatureLogic.sessionHasBoost(multiplier, true));
    }
}
