package tw.orstudio.cleverowl;

import org.junit.Test;
import java.util.Arrays;
import static org.junit.Assert.*;

public class ConstructorInputsTest {
    @Test public void canonicalMembershipChangesOnlyPlusAndLevel() {
        Object[] args = new Object[85];
        Arrays.fill(args, "unchanged");
        Object[] expected = args.clone();
        Object level = new Object();
        expected[24] = true; expected[79] = level;
        ConstructorInputs.membership(args, level, false);
        assertArrayEquals(expected, args);
    }
    @Test public void cachedSuperStateOverridesOldMaxWithoutChangingOtherMaskBits() {
        Object[] args = new Object[109];
        Arrays.fill(args, false);
        args[2] = 0x123; args[3] = 0x80;
        ConstructorInputs.membership(args, "PREMIUM", false);
        assertEquals(0x3c000123, args[2]); assertEquals(0x84, args[3]);
        assertEquals(true, args[28]); assertEquals("PREMIUM", args[83]);
        assertEquals(false, args[94]); assertEquals(true, args[95]);
        assertEquals(false, args[96]); assertEquals(1, args[97]); assertEquals(true, args[102]);
    }
    @Test public void cachedMaxStateIsConsistent() {
        Object[] args = new Object[109]; args[2] = 0; args[3] = 0;
        ConstructorInputs.membership(args, "GOLD", true);
        assertEquals("GOLD", args[83]); assertEquals(true, args[94]);
        assertEquals(2, args[97]); assertEquals(false, args[102]);
    }
    @Test public void multisessionChangesTimeRatherThanSessionXp() {
        Object[] args = {43, 90, 2, 20, 19, "event", "sessions"};
        ConstructorInputs.practice(args, 1);
        assertArrayEquals(new Object[]{43, 60000, 2, 20, 19, "event", "sessions"}, args);
    }
    @Test public void lightningPreservesLargerTimeAndChallengeCount() {
        Object[] args = {90000, "event", "checkpoints", 9};
        ConstructorInputs.practice(args, 0);
        assertEquals(90000, args[0]); assertEquals(9, args[3]);
        args[0] = 90; ConstructorInputs.practice(args, 0); assertEquals(60000, args[0]);
    }
    @Test public void unknownMembershipConstructorIsRejected() {
        assertThrows(IllegalArgumentException.class, () -> ConstructorInputs.membership(new Object[86], "GOLD", true));
        assertFalse(ConstructorInputs.membershipShape(new Class<?>[86], Object.class));
    }
}
