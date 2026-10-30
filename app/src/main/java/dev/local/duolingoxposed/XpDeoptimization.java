package dev.local.duolingoxposed;

import de.robv.android.xposed.XposedBridge;
import java.lang.reflect.Member;
import java.lang.reflect.Method;
import java.util.function.Consumer;

/** Only the exact inspected APK's XP call sites; never alters global compiler settings. */
final class XpDeoptimization {
    private XpDeoptimization() {}
    static void install(ClassLoader loader, Consumer<String> log) throws Exception {
        // Optional LSPosed extension; retain the legacy API dependency for other frameworks.
        Method deoptimize = XposedBridge.class.getDeclaredMethod("deoptimizeMethod", Member.class);
        int applied = 0, failed = 0;
        for (String key : XpCallers2488.METHODS) {
            try {
                int end = key.indexOf(';');
                Class<?> owner = Class.forName(key.substring(1, end).replace('/', '.'), false, loader);
                Method match = null;
                for (Method candidate : owner.getDeclaredMethods()) {
                    if (MethodKey.of(candidate).equals(key)) { match = candidate; break; }
                }
                if (match == null) throw new NoSuchMethodException();
                deoptimize.invoke(null, match);
                applied++;
            } catch (ReflectiveOperationException | LinkageError unavailable) {
                failed++;
                log.accept("XP caller deoptimization unavailable: " + key.substring(0, key.indexOf('('))
                        + " (" + unavailable.getClass().getSimpleName() + ").");
            }
        }
        log.accept("XP caller deoptimization requested: applied=" + applied + ", failed=" + failed
                + "; successful requests do not prove runtime hook hits.");
    }
}
