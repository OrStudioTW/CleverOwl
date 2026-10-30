package dev.local.duolingoxposed;

import android.os.Build;
import android.os.Process;
import java.util.zip.ZipFile;

/** Explicit APK path supports legacy framework class loaders without native search paths. */
final class NativeLoader {
    private NativeLoader() {}
    static void load(String modulePath) throws Exception {
        try { System.loadLibrary("dexkit"); return; }
        catch (UnsatisfiedLinkError normalLookup) {
            if (modulePath == null) throw normalLookup;
            try { System.load(explicitPath(modulePath, Build.SUPPORTED_ABIS, Process.is64Bit())); }
            catch (UnsatisfiedLinkError explicitLookup) {
                explicitLookup.addSuppressed(normalLookup);
                throw explicitLookup;
            }
        }
    }

    static String explicitPath(String modulePath, String[] abis, boolean is64Bit) throws java.io.IOException {
        try (ZipFile apk = new ZipFile(modulePath)) {
            for (String abi : abis) {
                if (abi.contains("64") != is64Bit) continue;
                String entry = "lib/" + abi + "/libdexkit.so";
                java.util.zip.ZipEntry library = apk.getEntry(entry);
                if (library == null) continue;
                if (library.getMethod() != java.util.zip.ZipEntry.STORED)
                    throw new UnsatisfiedLinkError("Packaged DexKit must be uncompressed for direct APK loading");
                return modulePath + "!/" + entry;
            }
        }
        throw new UnsatisfiedLinkError("No packaged DexKit library for current process ABI");
    }
}
