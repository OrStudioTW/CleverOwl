package tw.orstudio.cleverowl;

import android.app.Application;
import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.SystemClock;
import de.robv.android.xposed.*;
import de.robv.android.xposed.callbacks.XC_LoadPackage;
import org.luckypray.dexkit.DexKitBridge;

import java.lang.reflect.*;
import java.security.MessageDigest;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;

public final class ModuleEntry implements IXposedHookLoadPackage, IXposedHookZygoteInit {
    private static final String SIGNER = "1233ff6de4c70ef4d7deae54af15b167fc7cfd2539517bd2175870f3cb771731";
    private final AtomicBoolean installed = new AtomicBoolean();
    private XSharedPreferences preferences;
    private volatile FeatureSettings settings = FeatureSettings.OFF;
    private long nextReload;
    private static String modulePath;

    @Override public void initZygote(StartupParam param) { modulePath = param.modulePath; }

    @Override public void handleLoadPackage(XC_LoadPackage.LoadPackageParam loaded) throws Throwable {
        if (!FeatureSettings.TARGET.equals(loaded.packageName) || !loaded.packageName.equals(loaded.processName)) return;
        XposedHelpers.findAndHookMethod(Application.class, "attach", Context.class, new XC_MethodHook() {
            @Override protected void afterHookedMethod(MethodHookParam param) {
                Context context = (Context) param.args[0];
                if (FeatureSettings.TARGET.equals(context.getPackageName()) && installed.compareAndSet(false, true)) {
                    try { install(context, context.getClassLoader()); }
                    catch (Throwable failure) { log("Initialization unavailable: " + failure); }
                }
            }
        });
    }

    private void install(Context context, ClassLoader loader) throws Exception {
        PackageInfo info = context.getPackageManager().getPackageInfo(FeatureSettings.TARGET, PackageManager.GET_SIGNING_CERTIFICATES);
        if (info.signingInfo == null || info.signingInfo.hasMultipleSigners()
                || info.signingInfo.getApkContentsSigners().length != 1
                || !SIGNER.equals(hex(MessageDigest.getInstance("SHA-256").digest(info.signingInfo.getApkContentsSigners()[0].toByteArray())))) {
            log("Target signing certificate does not match the inspected original. No feature hooks installed.");
            return;
        }
        preferences = new XSharedPreferences(FeatureSettings.PACKAGE, FeatureSettings.PREFS);
        current();
        log("CleverOwl 0.4.4-alpha; Target " + info.versionName + " (" + info.getLongVersionCode() + "); hook registration is not functional verification.");
        boolean inspected = ApkProfile2488.matches(context.getApplicationInfo().sourceDir,
                info.getLongVersionCode(), info.versionName);
        boolean settingsAvailable = false;
        if (inspected) {
            log("Inspected 2488 code/resource profile matched (10 DEX, manifest and resource table).");
            settingsAvailable = attempt("CleverOwl native settings", () -> NativeSettingsUi.install(loader, ModuleEntry::log));
        } else log("Native settings integration skipped: version, DEX, manifest or resource table does not match the inspected profile.");
        if (!settings.enabled) {
            log(settingsAvailable
                    ? "Feature modifications disabled. CleverOwl settings hooks registered; check the settings row and restart after saving."
                    : "Feature modifications disabled. CleverOwl settings entry unavailable; export logs for compatibility diagnosis.");
            return;
        }
        log("Settings: membership=" + settings.membership + ", timer=" + settings.unlimitedTimer + ", XP=" + settings.xpFactor
                + ", boost extra minutes=" + settings.boostExtraMinutes);
        try {
            installDexFeatures(context, loader, inspected);
            log("Feature registration pass finished; inspect each feature result above.");
        } catch (Exception | LinkageError failed) { log("Feature discovery unavailable: " + failed); }
    }

    private void installDexFeatures(Context context, ClassLoader loader, boolean inspected) throws Exception {
        if (inspected) {
            log("Verified APK profile: registering features without the native DEX scanner.");
            installFeatures(null, loader, true);
            return;
        }
        NativeLoader.load(modulePath);
        try (DexKitBridge bridge = DexKitBridge.create(context.getApplicationInfo().sourceDir)) {
            installFeatures(bridge, loader, false);
        }
    }

    /** A null bridge is permitted only after exact version + code/resource hash verification. */
    private void installFeatures(DexKitBridge bridge, ClassLoader loader, boolean inspected) {
            if (settings.membership != FeatureSettings.Membership.ORIGINAL)
                attempt("membership constructor inputs", () -> installMembership(HookResolver.user(bridge, loader), loader));
            if (settings.unlimitedTimer) {
                attempt("Lightning initial time", () -> installPractice(HookResolver.practice(bridge, loader,
                        "TimedLightningPractice(initialSessionTime=", new int[]{0, 3}, 6), 0));
                attempt("Match initial time", () -> installPractice(HookResolver.practice(bridge, loader,
                        "TimedMatchPractice(initialSessionTime=", new int[]{0, 5, 6, 13}, 15), 0));
                attempt("MultiSession initial time", () -> installPractice(HookResolver.practice(bridge, loader,
                        "TimedMultiSessionPractice(sessionXp=", new int[]{0, 1, 2, 3, 4}, 7), 1));
            }
            if (settings.xpFactor != 1) {
                attempt("XP multiplier", () -> installXp(HookResolver.xpInputs(bridge, loader)));
                attempt("XP preview observation", () -> observeXpPreview(HookResolver.xpPreview(bridge, loader)));
                if (inspected) {
                    java.util.function.BooleanSupplier enabled = () -> current().enabled && current().xpFactor != 1;
                    attempt("XP session boost flag", () -> installXpSubmissionFlag(loader));
                    attempt("XP serialization observation", () -> XpDiagnostics.serialization(loader, enabled, ModuleEntry::log));
                    attempt("XP HTTP observation", () -> XpDiagnostics.http(loader, enabled, ModuleEntry::log));
                    attempt("XP summary observation", () -> XpDiagnostics.summaries(loader, enabled, ModuleEntry::log));
                    attempt("XP caller deoptimization", () -> XpDeoptimization.install(loader, ModuleEntry::log));
                }
            }
            if (settings.boostExtraMinutes > 0) attempt("boost extension", () -> installExtension(HookResolver.inventory(bridge, loader)));
            if (settings.unlimitedTimer) {
                // The model alone cannot prove a future reducer has the same timeout threshold.
                if (inspected) attempt("timed practice", () -> installTimer(HookResolver.timer(bridge, loader)));
                else log("Tick fallback skipped: unknown reducer. Independently matched practice hooks remain registered.");
            }
    }

    private void installMembership(Class<?> user, ClassLoader loader) throws Exception {
        Class<?> level = Class.forName(HookResolver.LEVEL, false, loader);
        Object premium = level.getField("PREMIUM").get(null);
        Object gold = level.getField("GOLD").get(null);
        List<Constructor<?>> constructors = new ArrayList<>();
        for (Constructor<?> c : user.getDeclaredConstructors())
            if (ConstructorInputs.membershipShape(c.getParameterTypes(), level)) constructors.add(c);
        if (constructors.isEmpty()) throw new IllegalStateException("User constructor layout changed");
        List<XC_MethodHook.Unhook> hooks = new ArrayList<>();
        AtomicBoolean hit = new AtomicBoolean();
        try {
            for (Constructor<?> constructor : constructors) hooks.add(XposedBridge.hookMethod(constructor, new XC_MethodHook() {
                @Override protected void beforeHookedMethod(MethodHookParam param) {
                    FeatureSettings config = current();
                    if (!config.enabled || config.membership == FeatureSettings.Membership.ORIGINAL) return;
                    boolean max = config.membership == FeatureSettings.Membership.MAX;
                    ConstructorInputs.membership(param.args, max ? gold : premium, max);
                    if (hit.compareAndSet(false, true)) log("Membership constructor reached; inputs adjusted. Server entitlement unverified.");
                }
            }));
        } catch (RuntimeException | LinkageError failed) {
            for (XC_MethodHook.Unhook hook : hooks) hook.unhook();
            throw failed;
        }
        log("User constructor hooks: " + constructors.size());
    }

    private void installPractice(Constructor<?> constructor, int timePosition) {
        AtomicBoolean hit = new AtomicBoolean();
        XposedBridge.hookMethod(constructor, new XC_MethodHook() {
            @Override protected void beforeHookedMethod(MethodHookParam param) {
                FeatureSettings config = current();
                if (!config.enabled || !config.unlimitedTimer) return;
                ConstructorInputs.practice(param.args, timePosition);
                if (hit.compareAndSet(false, true)) log("Practice constructor reached: " + constructor.getDeclaringClass().getName()
                        + "; time set to at least 60000 seconds.");
            }
        });
    }

    private void installXp(Constructor<?> constructor) {
        AtomicBoolean hit = new AtomicBoolean();
        XposedBridge.hookMethod(constructor, new XC_MethodHook() {
            @Override protected void beforeHookedMethod(MethodHookParam param) {
                FeatureSettings config = current();
                if (!config.enabled || config.xpFactor == 1) return;
                try {
                    double multiplier = FeatureLogic.xpMultiplier((Double) param.args[0], (Boolean) param.args[1], config.xpFactor);
                    param.args[0] = multiplier;
                    param.args[1] = true;
                    if (hit.compareAndSet(false, true)) log("XP inputs reached: extra factor=" + config.xpFactor
                            + ", effective multiplier=" + multiplier + "; shared by path preview and session calculations.");
                } catch (IllegalArgumentException unsupportedValue) {
                    // Preserve unexpected upstream data rather than masking a schema change.
                }
            }
        });
    }

    private void observeXpPreview(Constructor<?> constructor) {
        java.util.concurrent.atomic.AtomicInteger count = new java.util.concurrent.atomic.AtomicInteger();
        XposedBridge.hookMethod(constructor, new XC_MethodHook() {
            @Override protected void beforeHookedMethod(MethodHookParam param) {
                FeatureSettings config = current();
                if (config.enabled && config.xpFactor != 1 && count.getAndIncrement() < 8)
                    log("Path button preview reached: expected XP=" + param.args[1] + ", extra factor=" + config.xpFactor);
                // Read-only: preview already consumes the shared multiplier; never multiply it twice.
            }
        });
    }

    private void installXpSubmissionFlag(ClassLoader loader) throws Exception {
        Class<?> factory = Class.forName("fs7", false, loader);
        Class<?> completed = Class.forName("com.duolingo.core.session.c", false, loader);
        List<Method> candidates = new ArrayList<>();
        for (Method method : factory.getDeclaredMethods()) {
            Class<?>[] types = method.getParameterTypes();
            if (method.getName().equals("a") && Modifier.isStatic(method.getModifiers())
                    && method.getReturnType() == completed && types.length == 60
                    && types[0] == factory && types[10] == double.class && types[11] == boolean.class
                    && types[58] == int.class && types[59] == int.class) candidates.add(method);
        }
        if (candidates.size() != 1) throw new IllegalStateException("Completed-session factory shape changed");
        AtomicBoolean hit = new AtomicBoolean();
        XC_MethodHook.Unhook hook = XposedBridge.hookMethod(candidates.get(0), new XC_MethodHook() {
            @Override protected void beforeHookedMethod(MethodHookParam param) {
                FeatureSettings config = current();
                if (!config.enabled || config.xpFactor == 1) return;
                double multiplier = (Double) param.args[10];
                boolean original = (Boolean) param.args[11];
                boolean adjusted = FeatureLogic.sessionHasBoost(multiplier, original);
                param.args[11] = adjusted;
                if (hit.compareAndSet(false, true)) log("XP session inputs reached: multiplier=" + multiplier
                        + ", hasBoost=" + original + " -> " + adjusted + "; multiplier preserved.");
            }
        });
        if (hook == null) throw new IllegalStateException("Framework did not return a session hook");
    }

    private void installExtension(HookResolver.InventoryAccess access) {
        AtomicBoolean remainingHit = new AtomicBoolean();
        AtomicBoolean activeHit = new AtomicBoolean();
        XC_MethodHook.Unhook remainingHook = XposedBridge.hookMethod(access.remaining(), new XC_MethodHook() {
            @Override protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                FeatureSettings config = current();
                if (param.hasThrowable() || !config.enabled || config.boostExtraMinutes == 0 || !HookResolver.isBoost(access, param.thisObject)) return;
                param.setResult(FeatureLogic.extendedSeconds((Long) param.getResult(), access.expiry().getLong(param.thisObject), config.boostExtraMinutes));
                if (remainingHit.compareAndSet(false, true)) log("XP boost remaining-time hook reached.");
            }
        });
        try {
            XposedBridge.hookMethod(access.active(), new XC_MethodHook() {
            @Override protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                FeatureSettings config = current();
                if (param.hasThrowable() || !config.enabled || config.boostExtraMinutes == 0 || !HookResolver.isBoost(access, param.thisObject)) return;
                long original = (Long) XposedBridge.invokeOriginalMethod(access.remaining(), param.thisObject, new Object[0]);
                param.setResult(FeatureLogic.extendedSeconds(original, access.expiry().getLong(param.thisObject), config.boostExtraMinutes) > 0);
                if (activeHit.compareAndSet(false, true)) log("XP boost active-state hook reached.");
            }
            });
        } catch (RuntimeException | LinkageError failed) {
            remainingHook.unhook();
            throw failed;
        }
    }

    private void installTimer(Constructor<?> constructor) {
        AtomicBoolean hit = new AtomicBoolean();
        XposedBridge.hookMethod(constructor, new XC_MethodHook() {
            @Override protected void beforeHookedMethod(MethodHookParam param) {
                FeatureSettings config = current();
                if (config.enabled && config.unlimitedTimer) {
                    param.args[0] = FeatureLogic.nonExpiringTick((Integer) param.args[0]);
                    if (hit.compareAndSet(false, true)) log("Tick fallback hook reached.");
                }
            }
        });
    }

    private synchronized FeatureSettings current() {
        long now = SystemClock.elapsedRealtime();
        if (preferences != null && now >= nextReload) {
            try {
                preferences.reload();
                settings = FeatureSettings.from(preferences.getAll());
            } catch (RuntimeException unreadable) { settings = FeatureSettings.OFF; }
            nextReload = now + 1000;
        }
        return settings;
    }

    private interface Install { void run() throws Exception; }
    private static boolean attempt(String feature, Install action) {
        try { action.run(); log(feature + ": hooks registered; runtime effect unverified."); return true; }
        catch (Exception | LinkageError e) { log(feature + ": unavailable (" + e + ")."); return false; }
    }
    private static void log(String message) {
        XposedBridge.log("[DuolingoXposed] " + message);
        android.util.Log.i("DuolingoXposed", message);
    }
    private static String hex(byte[] bytes) {
        StringBuilder out = new StringBuilder(bytes.length * 2);
        for (byte b : bytes) out.append(String.format(Locale.ROOT, "%02x", b & 255));
        return out.toString();
    }
}
