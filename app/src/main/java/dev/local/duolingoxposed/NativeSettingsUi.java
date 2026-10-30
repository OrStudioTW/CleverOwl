package dev.local.duolingoxposed;

import android.app.Activity;
import android.app.Dialog;
import android.content.Context;
import android.graphics.drawable.ColorDrawable;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowInsets;
import android.widget.FrameLayout;
import android.widget.Toast;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import java.lang.ref.WeakReference;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;

/** Calls the inspected application's SettingsPage, SettingsActionBar and controls directly. */
final class NativeSettingsUi {
    // Resource IDs belong to the exact SHA-gated sample, verified against resources.arsc.
    private static final int PRIVACY_LABEL = 0x7f131027;
    private static final int[] OWNER_TAGS = {0x7f0a1350, 0x7f0a1354, 0x7f0a1353};
    private final ClassLoader loader;
    private final Consumer<String> log;
    private final Class<?> composer, modifier, function0, function1, function2, function3, position;
    private final Method row, section, textBody, toggle, radio, radioGroup, page, actionBar, setContent, colors, backgroundColor, background;
    private final Constructor<?> composable, composeView;
    private final Object unit, emptyModifier, enabledToggleState;
    private WeakReference<Activity> activity = new WeakReference<>(null);
    private WeakReference<Dialog> opened = new WeakReference<>(null);
    private Object arrow;
    private boolean opening;

    private NativeSettingsUi(ClassLoader loader, Consumer<String> log) throws Exception {
        this.loader = loader; this.log = log;
        composer = cls("o18"); modifier = cls("z5q");
        emptyModifier = java.util.Objects.requireNonNull(cls("w5q").getField("a").get(null));
        Class<?> toggleState = cls("com.duolingo.core.design.compose.components.inputs.ToggleState");
        enabledToggleState = java.util.Objects.requireNonNull(toggleState.getField("ENABLED").get(null));
        if (!modifier.isInstance(emptyModifier) || !toggleState.isInstance(enabledToggleState))
            throw new IllegalStateException("Native settings control defaults do not match the inspected profile");
        function0 = cls("kotlin.jvm.functions.Function0"); function1 = cls("qqh");
        function2 = cls("erh"); function3 = cls("grh");
        position = cls("com.duolingo.core.design.compose.components.GroupedListItemPosition");
        unit = cls("xv80").getField("a").get(null);
        Class<?> elements = cls("oup"), lambda = cls("uv7");
        composable = lambda.getDeclaredConstructor(Object.class, boolean.class, int.class);
        row = elements.getDeclaredMethod("v", String.class, position, modifier, String.class, String.class,
                boolean.class, Integer.class, String.class, float.class, function0, function2, composer, int.class, int.class);
        section = elements.getDeclaredMethod("B", String.class, modifier, boolean.class, composer, int.class, int.class);
        textBody = elements.getDeclaredMethod("C", int.class, composer, modifier, String.class);
        toggle = elements.getDeclaredMethod("F", boolean.class, function1, modifier,
                cls("com.duolingo.core.design.compose.components.inputs.ToggleState"), composer, int.class, int.class);
        radio = elements.getDeclaredMethod("z", int.class, composer, modifier, String.class, function0, boolean.class);
        radioGroup = elements.getDeclaredMethod("A", int.class, lambda, composer, modifier);
        page = cls("uxp").getDeclaredMethod("w", lambda, modifier, lambda, composer, int.class, int.class);
        actionBar = elements.getDeclaredMethod("p", String.class, modifier, String.class, function0, function0, composer, int.class, int.class);
        colors = cls("i2c").getDeclaredMethod("a", composer);
        backgroundColor = cls("f2c").getMethod("u");
        background = cls("djb0").getDeclaredMethod("f", modifier, long.class, cls("nc20"));
        Class<?> view = cls("com.duolingo.core.common.compose.DuoComposeView2");
        composeView = view.getDeclaredConstructor(Context.class);
        setContent = view.getDeclaredMethod("setContent", function2);
    }

    static void install(ClassLoader loader, Consumer<String> log) throws Exception {
        NativeSettingsUi ui = new NativeSettingsUi(loader, log);
        AtomicBoolean inserted = new AtomicBoolean();
        Class<?> fragment = ui.cls("com.duolingo.core.mvvm.view.MvvmComposeFragment");
        XC_MethodHook.Unhook fragmentHook = XposedBridge.hookMethod(fragment.getDeclaredMethod("onViewCreated", View.class, Bundle.class), new XC_MethodHook() {
            @Override protected void afterHookedMethod(MethodHookParam param) {
                if (param.thisObject.getClass().getName().equals("com.duolingo.settings.main.SettingsMainFragment"))
                    ui.activity = new WeakReference<>((Activity) XposedHelpers.callMethod(param.thisObject, "requireActivity"));
            }
        });
        if (fragmentHook == null) throw new IllegalStateException("Settings lifecycle hook unavailable");
        try {
            Class<?> account = ui.cls("com.duolingo.settings.main.AccountItem");
            Class<?> screen = ui.cls("com.duolingo.settings.main.c");
            Object privacy = XposedHelpers.getStaticObjectField(account, "PRIVACY");
            Constructor<?> privacyClick = ui.cls("ok10").getDeclaredConstructor(int.class, ui.function1);
            ui.arrow = ui.cls("uay").getField("N").get(null);
            Method text = ui.cls("oaz").getDeclaredMethod("i", ui.composer, int.class);
            Method render = screen.getDeclaredMethod("a", account, ui.cls("oy10"), ui.function1, ui.position, ui.composer, int.class);
            XC_MethodHook.Unhook rowHook = XposedBridge.hookMethod(render, new XC_MethodHook() {
                @Override protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                    if (param.args[0] != privacy) return;
                    Object click = privacyClick.newInstance(20, param.args[2]);
                    Object rows = ui.content(0x4f574c00, false, c -> {
                        String label = (String) text.invoke(null, c, PRIVACY_LABEL);
                        ui.drawRow(label, "MIDDLE", "privacySettingsRowItem", click, ui.arrow, c);
                        ui.drawRow("CleverOwl", "BOTTOM", "cleverOwlSettingsRowItem", ui.fn0(ui::open), ui.arrow, c);
                    });
                    XposedHelpers.callMethod(rows, "invoke", param.args[4], 0);
                    param.setResult(null);
                    if (inserted.compareAndSet(false, true)) log.accept("CleverOwl settings row rendered after privacy settings.");
                }
            });
            if (rowHook == null) throw new IllegalStateException("Settings row hook unavailable");
            try {
                XposedBridge.class.getDeclaredMethod("deoptimizeMethod", java.lang.reflect.Member.class).invoke(null,
                        screen.getDeclaredMethod("b", ui.cls("oy10"), ui.function1, ui.composer, int.class));
            } catch (ReflectiveOperationException unsupported) { log.accept("Settings caller deoptimization unavailable; check row runtime hit."); }
        } catch (Exception | LinkageError failure) { fragmentHook.unhook(); throw failure; }
    }

    private Class<?> cls(String name) throws ClassNotFoundException { return Class.forName(name, false, loader); }
    private Object pos(String name) throws Exception { return position.getField(name).get(null); }
    private interface Action { void run() throws Exception; }
    private interface Body { void draw(Object composer) throws Exception; }
    private interface Callback { Object invoke(Object[] args) throws Exception; }
    private Object callback(Class<?> type, Callback action) {
        return Proxy.newProxyInstance(loader, new Class<?>[]{type}, (proxy, method, args) -> {
            if (method.getDeclaringClass() == Object.class) return switch (method.getName()) {
                case "hashCode" -> System.identityHashCode(proxy);
                case "equals" -> proxy == args[0];
                default -> "CleverOwl callback";
            };
            return action.invoke(args == null ? new Object[0] : args);
        });
    }
    private Object fn0(Action action) { return callback(function0, args -> { action.run(); return unit; }); }
    private Object content(int key, boolean column, Body body) throws Exception {
        Object function = callback(column ? function3 : function2, args -> { body.draw(args[column ? 1 : 0]); return unit; });
        return composable.newInstance(function, true, key);
    }
    private void drawRow(String title, String edge, String tag, Object click, Object tail, Object c) throws Exception {
        row.invoke(null, title, pos(edge), emptyModifier, null, null, false, null, tag, 0f, click, tail, c, 0, 380);
    }
    private void title(String title, Object c) throws Exception { section.invoke(null, title, emptyModifier, true, c, 0, 2); }

    private void open() {
        Activity host = activity.get();
        Dialog previous = opened.get();
        if (opening || host == null || host.isFinishing() || host.isDestroyed() || (previous != null && previous.isShowing())) return;
        opening = true;
        SettingsClient.request(host, null, new SettingsClient.Result() {
            @Override public void success(Bundle saved) {
                opening = false;
                if (host.isFinishing() || host.isDestroyed()) return;
                log.accept("CleverOwl settings exchange read completed.");
                Screen screen = null;
                try {
                    screen = new Screen(host, saved);
                    screen.show();
                } catch (Exception unavailable) {
                    if (screen != null) screen.dialog.dismiss();
                    logFailure("page initialization", unavailable);
                    Toast.makeText(host, "CleverOwl 設定已讀取，但頁面無法開啟，請匯出日誌。", Toast.LENGTH_LONG).show();
                }
            }
            @Override public void failure(Exception unavailable) {
                opening = false;
                logFailure("exchange read", unavailable);
                if (!host.isFinishing() && !host.isDestroyed())
                    Toast.makeText(host, "無法讀取 CleverOwl 設定，請確認已安裝最新版模組後重開 Duolingo。", Toast.LENGTH_LONG).show();
            }
        });
    }

    private void logFailure(String stage, Throwable failure) {
        // Record call sites and causes without exception messages, settings or account data.
        StringBuilder details = new StringBuilder("CleverOwl settings ").append(stage).append(" failed");
        for (int depth = 0; failure != null && depth < 5; depth++, failure = failure.getCause()) {
            details.append("; cause=").append(failure.getClass().getName());
            StackTraceElement[] frames = failure.getStackTrace();
            for (int i = 0; i < Math.min(6, frames.length); i++)
                details.append(" at ").append(frames[i].getClassName()).append('.').append(frames[i].getMethodName())
                        .append(':').append(frames[i].getLineNumber());
        }
        log.accept(details.toString());
    }

    private final class Screen {
        final Activity host;
        final Bundle draft;
        final Dialog dialog;
        final View view;
        boolean saving;
        boolean firstComposition;
        Screen(Activity host, Bundle saved) throws Exception {
            this.host = host; draft = new Bundle(saved); dialog = new Dialog(host);
            dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
            view = (View) composeView.newInstance(host);
            view.setId(View.generateViewId());
            FrameLayout root = new FrameLayout(host);
            for (int tag : OWNER_TAGS) root.setTag(tag, host);
            root.addView(view, new FrameLayout.LayoutParams(-1, -1));
            root.setOnApplyWindowInsetsListener((v, insets) -> {
                if (Build.VERSION.SDK_INT >= 30) {
                    var bars = insets.getInsets(WindowInsets.Type.systemBars() | WindowInsets.Type.displayCutout());
                    v.setPadding(bars.left, bars.top, bars.right, bars.bottom);
                } else v.setPadding(insets.getSystemWindowInsetLeft(), insets.getSystemWindowInsetTop(), insets.getSystemWindowInsetRight(), insets.getSystemWindowInsetBottom());
                return insets;
            });
            dialog.setContentView(root);
            // Window recomposition resolves owners from the decor, above the content root.
            Window window = dialog.getWindow();
            if (window != null) for (int tag : OWNER_TAGS) window.getDecorView().setTag(tag, host);
            dialog.setOnDismissListener(ignored -> {
                try { XposedHelpers.callMethod(view, "e"); } catch (Throwable ignoredFailure) { }
                opened.clear();
            });
        }
        void show() throws Exception {
            redraw(); dialog.show();
            Window window = dialog.getWindow();
            if (window != null) {
                window.setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
                android.util.TypedValue color = new android.util.TypedValue();
                host.getTheme().resolveAttribute(android.R.attr.colorBackground, color, true);
                window.setBackgroundDrawable(new ColorDrawable(color.data));
                window.setDimAmount(0f);
                if (Build.VERSION.SDK_INT >= 30) window.setDecorFitsSystemWindows(false);
            }
            opened = new WeakReference<>(dialog);
            log.accept("CleverOwl settings window shown; waiting for first composition.");
        }
        void redraw() throws Exception {
            setContent.invoke(view, content(0x4f574c01, false, c -> {
                Object themedBackground = background.invoke(null, emptyModifier,
                        backgroundColor.invoke(colors.invoke(null, c)), XposedHelpers.getStaticObjectField(cls("djb0"), "f"));
                page.invoke(null,
                    content(0x4f574c02, false, header -> actionBar.invoke(null, "CleverOwl", emptyModifier, "儲存", fn0(this::save), fn0(dialog::dismiss), header, 0, 2)),
                    themedBackground, content(0x4f574c03, true, this::body), c, 0, 0);
                if (!firstComposition) {
                    firstComposition = true;
                    log.accept("CleverOwl native settings first composition completed.");
                }
            }));
        }
        void body(Object c) throws Exception {
            title("功能設定", c);
            switchRow("啟用 CleverOwl", "enabled", "TOP", c);
            switchRow("延長限時練習／倒數不歸零", "timer", "BOTTOM", c);
            title("會員模式", c);
            options("membership", new String[]{"ORIGINAL", "SUPER", "MAX"}, new String[]{"原始", "Super", "Max"}, c);
            title("課程 XP 倍率（最高 ×3）", c);
            options("xp_factor", new Integer[]{1, 2, 3}, new String[]{"原始", "×2", "×3"}, c);
            title("延長 XP 加倍時間", c);
            options("extra_minutes", new Integer[]{0, 15, 30}, new String[]{"不延長", "15 分鐘", "30 分鐘"}, c);
            options("extra_minutes", new Integer[]{60, 180, 1440}, new String[]{"1 小時", "3 小時", "24 小時"}, c);
            textBody.invoke(null, 0, c, emptyModifier, "儲存後請完整關閉並重新開啟 Duolingo。");
            textBody.invoke(null, 0, c, emptyModifier, "會員為本機狀態；延期需已有加倍道具。計時僅涵蓋已支援的限時練習。");
        }
        void switchRow(String label, String key, String edge, Object c) throws Exception {
            boolean checked = draft.getBoolean(key);
            Object change = callback(function1, args -> { if (!saving) { draft.putBoolean(key, (Boolean) args[0]); redraw(); } return unit; });
            // Mask 8 defaults only ToggleState. Modifier needs bit 4 or a real instance.
            // Supply both explicitly so neither nullable placeholder reaches the native control.
            Object tail = content(key.hashCode(), false, t -> toggle.invoke(null, checked, change, emptyModifier, enabledToggleState, t, 0, 0));
            drawRow(label, edge, "cleverOwl_" + key, fn0(() -> { if (!saving) { draft.putBoolean(key, !draft.getBoolean(key)); redraw(); } }), tail, c);
        }
        void options(String key, Object[] values, String[] labels, Object c) throws Exception {
            // SettingsRadioButtonGroup invokes (Composer, flags), without a scope argument.
            radioGroup.invoke(null, 0, content(key.hashCode() + values[0].hashCode(), false, radioComposer -> {
                for (int i = 0; i < values.length; i++) {
                    Object value = values[i];
                    Object choose = fn0(() -> {
                        if (saving) return;
                        if (value instanceof Integer) draft.putInt(key, (Integer) value); else draft.putString(key, (String) value);
                        redraw();
                    });
                    radio.invoke(null, 0, radioComposer, emptyModifier, labels[i], choose, value.equals(draft.get(key)));
                }
            }), c, emptyModifier);
        }
        void save() {
            if (saving) return;
            saving = true;
            Bundle submitted = new Bundle(draft);
            SettingsClient.request(host, submitted, new SettingsClient.Result() {
                @Override public void success(Bundle stored) {
                    for (String key : submitted.keySet()) if (!java.util.Objects.equals(submitted.get(key), stored.get(key))) {
                        failure(new IllegalStateException("Saved value mismatch")); return;
                    }
                    saving = false;
                    log.accept("CleverOwl settings saved and read back.");
                    if (!host.isFinishing() && !host.isDestroyed())
                        Toast.makeText(host, "CleverOwl 設定已儲存，請重新開啟 Duolingo。", Toast.LENGTH_LONG).show();
                    dialog.dismiss();
                }
                @Override public void failure(Exception unavailable) {
                    saving = false;
                    logFailure("exchange save", unavailable);
                    if (!host.isFinishing() && !host.isDestroyed())
                        Toast.makeText(host, "設定未確認儲存成功。請重新開啟頁面核對，或匯出日誌。", Toast.LENGTH_LONG).show();
                }
            });
        }
    }
}
