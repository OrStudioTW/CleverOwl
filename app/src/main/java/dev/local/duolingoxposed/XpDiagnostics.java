package dev.local.duolingoxposed;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import java.lang.reflect.Field;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;

/** Read-only probes for the exact inspected APK; never changes arguments, results or body streams. */
final class XpDiagnostics {
    private XpDiagnostics() {}
    private static Field field(Class<?> model, String name, Class<?> type) throws Exception {
        Field f = model.getDeclaredField(name);
        if (f.getType() != type) throw new IllegalStateException("Diagnostic field type changed: " + name);
        f.setAccessible(true); return f;
    }
    static void serialization(ClassLoader loader, BooleanSupplier enabled, Consumer<String> log) throws Exception {
        Class<?> model = Class.forName("com.duolingo.feature.session.CompletedSessionSurrogate", false, loader);
        Field multiplier = field(model, "B", Double.class), boost = field(model, "C", Boolean.class);
        Field promised = field(model, "G", Integer.class), expected = field(model, "O", Integer.class);
        Class<?> encoder = Class.forName("kotlinx.serialization.encoding.Encoder", false, loader);
        Class<?> serializer = Class.forName("ms7", false, loader);
        AtomicInteger count = new AtomicInteger();
        XposedBridge.hookMethod(serializer.getDeclaredMethod("serialize", encoder, Object.class), new XC_MethodHook() {
            @Override protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                if (!enabled.getAsBoolean() || count.getAndIncrement() >= 24) return;
                if (param.hasThrowable()) { log.accept("XP serialization failed: " + param.getThrowable().getClass().getSimpleName()); return; }
                Object value = param.args[1];
                log.accept(XpObservation.submission(multiplier.get(value), boost.get(value), promised.get(value), expected.get(value)));
            }
        });
    }
    static void summaries(ClassLoader loader, BooleanSupplier enabled, Consumer<String> log) throws Exception {
        Class<?> model = Class.forName("com.duolingo.core.xpsummaries.XpSummary", false, loader);
        Field gained = field(model, "a", int.class), day = field(model, "b", long.class);
        Class<?> decoder = Class.forName("kotlinx.serialization.encoding.Decoder", false, loader);
        Class<?> serializer = Class.forName("ltb0", false, loader);
        AtomicInteger count = new AtomicInteger();
        XposedBridge.hookMethod(serializer.getDeclaredMethod("deserialize", decoder), new XC_MethodHook() {
            @Override protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                if (!enabled.getAsBoolean() || param.hasThrowable() || count.getAndIncrement() >= 32) return;
                Object value = param.getResult();
                if (model.isInstance(value)) log.accept(XpObservation.summary(gained.get(value), day.get(value)));
            }
        });
    }
    static void http(ClassLoader loader, BooleanSupplier enabled, Consumer<String> log) throws Exception {
        Class<?> chain = Class.forName("okhttp3.Interceptor$Chain", false, loader);
        Class<?> interceptor = Class.forName("okhttp3.internal.http.CallServerInterceptor", false, loader);
        AtomicInteger count = new AtomicInteger();
        XposedBridge.hookMethod(interceptor.getDeclaredMethod("intercept", chain), new XC_MethodHook() {
            @Override protected void afterHookedMethod(MethodHookParam param) {
                if (!enabled.getAsBoolean()) return;
                try {
                    Object request = XposedHelpers.callMethod(param.args[0], "request");
                    Object url = XposedHelpers.callMethod(request, "url");
                    String method = (String) XposedHelpers.callMethod(request, "method");
                    if (!XpObservation.sessionWrite(method, (String) XposedHelpers.callMethod(url, "host"),
                            (String) XposedHelpers.callMethod(url, "encodedPath")) || count.getAndIncrement() >= 24) return;
                    if (param.hasThrowable()) log.accept("Session network write failed: " + param.getThrowable().getClass().getSimpleName());
                    else log.accept("Session network write response: method=" + method + ", status="
                            + XposedHelpers.callMethod(param.getResult(), "code") + "; HTTP success alone does not prove awarded XP.");
                } catch (Throwable diagnosticFailure) {
                    // A diagnostic failure must not change a successful request or expose payload details.
                    if (count.getAndIncrement() < 2) log.accept("Session HTTP observation unavailable: " + diagnosticFailure.getClass().getSimpleName());
                }
            }
        });
    }
}
