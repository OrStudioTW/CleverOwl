import jadx.api.*;
import java.io.*;
import java.nio.file.*;
import java.util.*;
class DecompileOne {
 public static void main(String[] a) throws Exception {
  JadxArgs args = new JadxArgs(); args.setInputFiles(List.of(new File(a[0])));args.setSkipResources(true);args.setThreadsCount(2);
  try (JadxDecompiler jadx = new JadxDecompiler(args)) { jadx.load();
   for(JavaClass c:jadx.getClassesWithInners()) if(c.getRawName().equals(a[1])) {Files.writeString(Path.of(a[2]),c.getCode());return;}
   throw new IllegalStateException("Class not found");
  }
 }
}
