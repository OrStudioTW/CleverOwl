package tw.orstudio.cleverowl;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import static org.junit.Assert.*;

public class ApkProfile2488Test {
    @Rule public TemporaryFolder temporary = new TemporaryFolder();
    private final Map<String, String> contents = Map.of("AndroidManifest.xml", "manifest",
            "resources.arsc", "resources", "classes.dex", "code", "classes2.dex", "more code");

    private Map<String, String> hashes() throws Exception {
        Map<String, String> hashes = new HashMap<>();
        for (var entry : contents.entrySet()) {
            byte[] hash = MessageDigest.getInstance("SHA-256").digest(entry.getValue().getBytes(StandardCharsets.UTF_8));
            StringBuilder text = new StringBuilder();
            for (byte value : hash) text.append(String.format("%02x", value & 255));
            hashes.put(entry.getKey(), text.toString());
        }
        return hashes;
    }
    private String apk(Map<String, String> files, int level, String comment) throws Exception {
        Path file = temporary.newFile().toPath();
        try (ZipOutputStream zip = new ZipOutputStream(Files.newOutputStream(file))) {
            zip.setLevel(level); zip.setComment(comment);
            for (var entry : files.entrySet()) {
                zip.putNextEntry(new ZipEntry(entry.getKey()));
                zip.write(entry.getValue().getBytes(StandardCharsets.UTF_8)); zip.closeEntry();
            }
        }
        return file.toString();
    }
    @Test public void acceptsSameContentAcrossDifferentContainers() throws Exception {
        String first = apk(contents, 0, "first installation");
        Map<String, String> reversed = new LinkedHashMap<>();
        contents.keySet().stream().sorted(java.util.Comparator.reverseOrder()).forEach(k -> reversed.put(k, contents.get(k)));
        String second = apk(reversed, 9, "second installation");
        assertFalse(java.util.Arrays.equals(Files.readAllBytes(Path.of(first)), Files.readAllBytes(Path.of(second))));
        assertTrue(ApkProfile2488.matchesEntries(first, hashes()));
        assertTrue(ApkProfile2488.matchesEntries(second, hashes()));
    }
    @Test public void rejectsChangesToEveryCodeAndResourceEntry() throws Exception {
        for (String name : contents.keySet()) {
            Map<String, String> changed = new HashMap<>(contents); changed.put(name, "different");
            assertFalse(name, ApkProfile2488.matchesEntries(apk(changed, 9, ""), hashes()));
        }
    }
    @Test public void rejectsMissingCodeOrResources() throws Exception {
        for (String name : contents.keySet()) {
            Map<String, String> missing = new HashMap<>(contents); missing.remove(name);
            assertFalse(name, ApkProfile2488.matchesEntries(apk(missing, 9, ""), hashes()));
        }
    }
    @Test public void rejectsAdditionalDex() throws Exception {
        Map<String, String> extra = new HashMap<>(contents); extra.put("classes3.dex", "additional code");
        assertFalse(ApkProfile2488.matchesEntries(apk(extra, 9, ""), hashes()));
    }
    @Test public void rejectsUnknownVersionBeforeInspectingFile() throws Exception {
        assertFalse(ApkProfile2488.matches("missing.apk", 2489, "6.103.6"));
        assertFalse(ApkProfile2488.matches("missing.apk", 2488, "other"));
    }
    @Test public void rejectsUninspectedContentEvenAtKnownVersion() throws Exception {
        assertFalse(ApkProfile2488.matches(apk(contents, 9, ""), 2488, "6.103.6"));
    }
}
