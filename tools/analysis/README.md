# 分析工具

工具不包含原版 APK、反編譯來源或 JADX 二進位，不參與一般 Android 建置。需要 JDK 17+ 與自行提供的 JADX 1.5.6 all JAR（含所需 dexlib2 類別），從專案根目錄執行。

列出 DEX 中符合正規表示式的方法參照：

```text
java -cp <jadx-1.5.6-all.jar> tools/analysis/InspectDex.java <base.apk> <regex>
```

輸出符合類別名稱的欄位與方法：

```text
java -cp <jadx-1.5.6-all.jar> tools/analysis/InspectDex.java <base.apk> <class-regex> dump
```

輸出單一類別的反編譯結果：

```text
java -cp <jadx-1.5.6-all.jar> tools/analysis/DecompileOne.java <base.apk> <raw-class-name> <output.java>
```

將分析輸出寫到 Git 排除的 `work/`。主樣本斷言工具保留在 [VerifySample.java](../VerifySample.java)，使用方式見 [根 README](../../README.md)。
