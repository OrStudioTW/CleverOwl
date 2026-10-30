package dev.local.duolingoxposed;

import org.junit.Test;
import org.junit.Rule;
import org.junit.rules.TemporaryFolder;
import java.io.*;
import java.util.zip.*;
import static org.junit.Assert.*;

public class NativeLoaderTest {
    @Rule public TemporaryFolder temp = new TemporaryFolder();
    private String apk(boolean stored, String... abis) throws IOException {
        File file = temp.newFile();
        try (ZipOutputStream zip = new ZipOutputStream(new FileOutputStream(file))) {
            byte[] data = {1, 2, 3};
            for (String abi : abis) {
                ZipEntry entry = new ZipEntry("lib/" + abi + "/libdexkit.so");
                if (stored) {
                    CRC32 crc = new CRC32(); crc.update(data);
                    entry.setMethod(ZipEntry.STORED); entry.setSize(data.length); entry.setCrc(crc.getValue());
                }
                zip.putNextEntry(entry); zip.write(data); zip.closeEntry();
            }
        }
        return file.getAbsolutePath();
    }
    @Test public void chooses64BitLibraryDespite32BitCandidateFirst() throws Exception {
        String file = apk(true, "arm64-v8a", "armeabi-v7a");
        assertEquals(file + "!/lib/arm64-v8a/libdexkit.so", NativeLoader.explicitPath(file, new String[]{"armeabi-v7a", "arm64-v8a"}, true));
    }
    @Test public void chooses32BitLibraryFor32BitProcess() throws Exception {
        String file = apk(true, "arm64-v8a", "armeabi-v7a");
        assertEquals(file + "!/lib/armeabi-v7a/libdexkit.so", NativeLoader.explicitPath(file, new String[]{"arm64-v8a", "armeabi-v7a"}, false));
    }
    @Test public void rejectsMissingArchitecture() throws Exception {
        String file = apk(true, "arm64-v8a");
        assertThrows(UnsatisfiedLinkError.class, () -> NativeLoader.explicitPath(file, new String[]{"x86_64"}, true));
    }
    @Test public void catchesOldCompressedPackaging() throws Exception {
        String file = apk(false, "arm64-v8a");
        assertThrows(UnsatisfiedLinkError.class, () -> NativeLoader.explicitPath(file, new String[]{"arm64-v8a"}, true));
    }
}
