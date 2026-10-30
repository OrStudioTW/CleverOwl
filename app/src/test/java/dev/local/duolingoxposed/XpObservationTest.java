package dev.local.duolingoxposed;

import org.junit.Test;
import static org.junit.Assert.*;

public class XpObservationTest {
    @Test public void selectsSessionWritesOnDuolingoHosts() {
        assertTrue(XpObservation.sessionWrite("POST", "android-api.duolingo.com", "/2017-06-30/sessions"));
        assertTrue(XpObservation.sessionWrite("PUT", "android-api.duolingo.cn", "/users/123/sessions/abc"));
        assertTrue(XpObservation.sessionWrite("PATCH", "duolingo.com", "/session/abc"));
    }
    @Test public void excludesReadsOtherRoutesAndLookalikeHosts() {
        assertFalse(XpObservation.sessionWrite("GET", "duolingo.com", "/sessions"));
        assertFalse(XpObservation.sessionWrite("POST", "duolingo.com.evil.test", "/sessions"));
        assertFalse(XpObservation.sessionWrite("POST", "duolingo.com", "/subscriptions"));
        assertFalse(XpObservation.sessionWrite("POST", null, null));
    }
    @Test public void emitsScalarFieldsWithoutPretendingEncodingProvesSubmission() {
        String result = XpObservation.submission(3.0, true, 75, null);
        assertTrue(result.contains("multiplier=3.0")); assertTrue(result.contains("xpPromised=75"));
        assertTrue(result.contains("expectedXpGain=null")); assertTrue(result.contains("does not prove network"));
    }
    @Test public void unexpectedObjectsAndNonfiniteValuesCannotLeakDetails() {
        Object secret = new Object() { public String toString() { throw new AssertionError("Must not stringify arbitrary data"); } };
        String result = XpObservation.submission(secret, "sensitive", Double.NaN, Double.POSITIVE_INFINITY);
        assertFalse(result.contains("sensitive")); assertTrue(result.contains("unsupported"));
    }
    @Test public void decodedSummaryDoesNotClaimNetworkOrigin() {
        String result = XpObservation.summary(25, 123L);
        assertTrue(result.contains("gainedXp=25")); assertTrue(result.contains("network or cache"));
    }
}
