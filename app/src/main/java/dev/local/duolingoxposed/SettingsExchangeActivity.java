package dev.local.duolingoxposed;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.ResultReceiver;
import java.security.MessageDigest;
import java.util.HashMap;
import java.util.Map;

/** Writes occur under the module UID; Duolingo never writes another application's private files. */
public final class SettingsExchangeActivity extends Activity {
    @Override protected void onCreate(Bundle state) {
        super.onCreate(state);
        ResultReceiver receiver = null;
        try {
            // getCallingPackage is supplied by Android only for startActivityForResult.
            // Do not accept a package name or referrer supplied in Intent extras.
            authorize();
            receiver = getIntent().getParcelableExtra("receiver");
            if (receiver == null) return;
            String operation = getIntent().getStringExtra("operation");
            if (!"read".equals(operation) && !"write".equals(operation))
                throw new IllegalArgumentException("Unknown operation");
            Bundle result = access(getIntent().getBundleExtra("settings"), "write".equals(operation));
            receiver.send(1, result);
        } catch (Exception failure) {
            if (receiver != null) receiver.send(failure instanceof SecurityException ? 2 : 3, null);
            // No caller input, account data or exception messages enter the log.
            android.util.Log.e("DuolingoXposed", "CleverOwl settings exchange failed: " + failure.getClass().getSimpleName());
        } finally {
            // Theme.NoDisplay must finish before onResume, including every failure path.
            finish();
        }
    }

    private Bundle access(Bundle extras, boolean write) {
        SharedPreferences prefs = getSharedPreferences(FeatureSettings.PREFS, Context.MODE_WORLD_READABLE);
        if (write) {
            if (extras == null) throw new IllegalArgumentException("Missing configuration");
            Map<String, Object> values = new HashMap<>();
            for (String key : extras.keySet()) values.put(key, extras.get(key));
            FeatureSettings settings = SettingsContract.validate(values);
            if (!prefs.edit().putInt("schema", FeatureSettings.SCHEMA).putBoolean("enabled", settings.enabled)
                    .putString("membership", settings.membership.name()).putInt("xp_factor", settings.xpFactor)
                    .putInt("extra_minutes", settings.boostExtraMinutes).putBoolean("timer", settings.unlimitedTimer).commit())
                throw new IllegalStateException("Settings were not saved");
        }
        FeatureSettings settings = FeatureSettings.from(prefs.getAll());
        Bundle result = new Bundle();
        result.putInt("schema", FeatureSettings.SCHEMA); result.putBoolean("enabled", settings.enabled);
        result.putString("membership", settings.membership.name()); result.putInt("xp_factor", settings.xpFactor);
        result.putInt("extra_minutes", settings.boostExtraMinutes); result.putBoolean("timer", settings.unlimitedTimer);
        return result;
    }

    private void authorize() {
        String name = getCallingPackage();
        if (getCallingActivity() == null || !FeatureSettings.TARGET.equals(name))
            throw new SecurityException("Caller is not authorized for CleverOwl settings");
        PackageManager packages = getPackageManager();
        try {
            PackageInfo info = packages.getPackageInfo(name, PackageManager.GET_SIGNING_CERTIFICATES);
            if (info.signingInfo == null) throw new SecurityException("Missing signer");
            var signatures = info.signingInfo.getApkContentsSigners();
            if (signatures.length != 1) throw new SecurityException("Ambiguous signer");
            byte[] digest = MessageDigest.getInstance("SHA-256").digest(signatures[0].toByteArray());
            StringBuilder hash = new StringBuilder();
            for (byte value : digest) hash.append(String.format(java.util.Locale.ROOT, "%02x", value & 255));
            if (SettingsContract.trustedTarget(name, info.signingInfo.hasMultipleSigners(), signatures.length, hash.toString())) return;
        } catch (Exception unavailable) { /* Fail closed without logging caller data. */ }
        throw new SecurityException("Caller is not authorized for CleverOwl settings");
    }

}
