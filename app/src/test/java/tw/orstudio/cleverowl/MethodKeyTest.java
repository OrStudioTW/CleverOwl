package tw.orstudio.cleverowl;

import org.junit.Test;
import static org.junit.Assert.*;

public class MethodKeyTest {
    static class Sample {
        void run(int n, String s) {}
        double run(long n) { return n; }
        String[][] arrays(int[] a, String[][] b) { return b; }
    }
    @Test public void distinguishesOverloadsAndReturnTypes() throws Exception {
        String owner = "Ltw/orstudio/cleverowl/MethodKeyTest$Sample;->";
        assertEquals(owner + "run(ILjava/lang/String;)V", MethodKey.of(Sample.class.getDeclaredMethod("run", int.class, String.class)));
        assertEquals(owner + "run(J)D", MethodKey.of(Sample.class.getDeclaredMethod("run", long.class)));
    }
    @Test public void handlesPrimitiveAndReferenceArrays() throws Exception {
        assertEquals("Ltw/orstudio/cleverowl/MethodKeyTest$Sample;->arrays([I[[Ljava/lang/String;)[[Ljava/lang/String;",
                MethodKey.of(Sample.class.getDeclaredMethod("arrays", int[].class, String[][].class)));
    }
    @Test public void mapsEveryPrimitiveExactly() {
        Class<?>[] types = {void.class, boolean.class, byte.class, char.class, short.class, int.class, long.class, float.class, double.class};
        String[] codes = {"V", "Z", "B", "C", "S", "I", "J", "F", "D"};
        for (int i = 0; i < types.length; i++) assertEquals(codes[i], MethodKey.type(types[i]));
    }
}
