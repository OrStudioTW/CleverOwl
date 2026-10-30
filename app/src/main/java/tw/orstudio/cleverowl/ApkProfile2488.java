package tw.orstudio.cleverowl;

import java.io.InputStream;
import java.security.MessageDigest;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.zip.ZipFile;

/** Exact code/resource profile; the caller must also verify Duolingo's signing certificate. */
final class ApkProfile2488 {
    private static final Map<String, String> ENTRIES = Map.ofEntries(
            Map.entry("AndroidManifest.xml", "dd4277c33b3f01cb10810d24e13f0791c7bab907d42e823bf316f621eff051ca"),
            Map.entry("resources.arsc", "3b583515386fb9fe92e90a8938b553de940c9f1cc3e49976770f438bfeec3ef0"),
            Map.entry("classes.dex", "23e8a5b2c8e56ac01eb469d75717e5ef1fbcc8bdfbc7362e0351dc248125b363"),
            Map.entry("classes2.dex", "33d98933314a6072c6edf1da96214901e835b39d62f9e0c2b44dc1a1cbf09d5e"),
            Map.entry("classes3.dex", "21e37b3a9e0bf22625faaaa8a6dc6092e20e2bf8d0b1d8464acaba05c102292f"),
            Map.entry("classes4.dex", "6e9af5ec48eb06d0d7b9a7cc3a08ef9f8f2f8ec4e4de247e5e51fb095784b8c1"),
            Map.entry("classes5.dex", "cb5552e367df160fccdae47714fe7d4e14e5ba85f2444a73b3f3a7a9b168a5a7"),
            Map.entry("classes6.dex", "1a24920c0f929c7653c316004e47023a4c4747a9e45be7d0bb489ed71f7af726"),
            Map.entry("classes7.dex", "32a269128af3772f5edde01990e8c09a7ed6deb99ff1038024004a43339eb7ec"),
            Map.entry("classes8.dex", "40c1b3563df1c3036c03dcfa2fbb98e96a0c1ec75590e2e7f1779a534a3b20f2"),
            Map.entry("classes9.dex", "18a366d54327e67998698211f6340aa2ce8f954d1917b25c2799699cf92b8dfe"),
            Map.entry("classes10.dex", "ba993c145858601aad73c2940e682bab92bca23794be73f8b3ab78f530a24855"));

    static boolean matches(String apk, long versionCode, String versionName) throws Exception {
        return versionCode == 2488 && "6.103.6".equals(versionName) && matchesEntries(apk, ENTRIES);
    }

    static boolean matchesEntries(String apk, Map<String, String> expected) throws Exception {
        Set<String> found = new HashSet<>();
        try (ZipFile zip = new ZipFile(apk)) {
            var entries = zip.entries();
            byte[] buffer = new byte[65536];
            while (entries.hasMoreElements()) {
                var entry = entries.nextElement();
                String name = entry.getName();
                if (!name.equals("AndroidManifest.xml") && !name.equals("resources.arsc")
                        && !name.matches("classes[0-9]*\\.dex")) continue;
                if (!expected.containsKey(name) || !found.add(name)) return false;
                MessageDigest digest = MessageDigest.getInstance("SHA-256");
                try (InputStream input = zip.getInputStream(entry)) {
                    for (int count; (count = input.read(buffer)) != -1;) digest.update(buffer, 0, count);
                }
                StringBuilder hash = new StringBuilder(64);
                for (byte value : digest.digest()) {
                    hash.append(Character.forDigit((value & 255) >>> 4, 16));
                    hash.append(Character.forDigit(value & 15, 16));
                }
                if (!expected.get(name).contentEquals(hash)) return false;
            }
        }
        return found.equals(expected.keySet());
    }
}
