package dev.local.duolingoxposed;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.ResultReceiver;
import java.util.concurrent.TimeoutException;

/** Activities can launch explicitly even when the host cannot query the module package. */
final class SettingsClient {
    interface Result {
        void success(Bundle settings);
        void failure(Exception error);
    }
    private final Activity host;
    private final Bundle draft;
    private final Result result;
    private final Handler main = new Handler(Looper.getMainLooper());
    private final Runnable timeout = () -> finish(null, new TimeoutException("Settings exchange timed out"));
    private boolean finished;

    private SettingsClient(Activity host, Bundle draft, Result result) {
        this.host = host;
        this.draft = draft == null ? null : new Bundle(draft);
        this.result = result;
    }
    static void request(Activity host, Bundle draft, Result result) {
        SettingsClient client = new SettingsClient(host, draft, result);
        client.main.post(client::connect);
    }
    private void connect() {
        ResultReceiver receiver = new ResultReceiver(main) {
            @Override protected void onReceiveResult(int code, Bundle data) {
                if (code == 1 && data != null) finish(data, null);
                else finish(null, code == 2 ? new SecurityException("Settings sharing unavailable")
                        : new IllegalStateException("Settings exchange failed"));
            }
        };
        Intent intent = new Intent().setComponent(new ComponentName(FeatureSettings.PACKAGE,
                FeatureSettings.PACKAGE + ".SettingsExchangeActivity"));
        intent.putExtra("receiver", receiver).putExtra("operation", draft == null ? "read" : "write");
        if (draft != null) intent.putExtra("settings", draft);
        main.postDelayed(timeout, 10_000);
        try {
            // A positive request code provides an OS-authenticated calling package.
            // Configuration uses ResultReceiver, so no host result hook is needed.
            host.startActivityForResult(intent, 0x4f57);
        } catch (Exception error) { finish(null, error); }
    }
    private void finish(Bundle saved, Exception error) {
        if (finished) return;
        finished = true;
        main.removeCallbacks(timeout);
        if (error == null) result.success(saved); else result.failure(error);
    }
}
