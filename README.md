# CleverOwl 0.4.4-alpha

CleverOwl 是 Duolingo 的實驗 Xposed 擴充模組。設定入口位於 Duolingo 設定的「隱私設置」下方；沒有獨立設定頁或桌面圖示。套件名稱、應用程式 ID 與 namespace 統一為 `tw.orstudio.cleverowl`。

內嵌介面限定 Duolingo **6.103.6（2488）已分析的程式與資源內容**，需同時符合官方簽章、版本、全部 10 份 DEX、Manifest 與資源表雜湊；不再因 APK 外層封裝不同而誤拒絕。詳見 [0.4.4 調查與修正](docs/diagnostics/0.4.4-套件命名與設定入口修正.md)。動態定位及本機建置不等於跨版本、全模式或伺服器功能驗證。0.3.3 的樣本單堂 XP ×3 有結算與重啟保留記錄；新版入口、儲存與功能仍需實機驗收。

## 功能範圍

| 項目 | 實作 | 範圍 |
| --- | --- | --- |
| 會員 | 原始／Super／Max 的本機會員模型 | 不會建立伺服器訂閱，Max 線上功能未驗證 |
| XP | 原始／額外 ×2／額外 ×3 | 修改啟用時合併倍率上限 ×3，舊高倍率設定降為 ×3 |
| 加倍時間 | 延長既有道具 15／30／60／180／1440 分鐘 | 不建立新道具，仍可能受伺服器限制 |
| 計時 | 三種已辨識練習初始至少 60,000 秒，樣本 Rampup 最低 2 秒 | 不是所有模式的真正無限時間 |

四項功能獨立、預設關閉，設定讀取失敗也關閉修改。模組沒有 INTERNET 或儲存權限；無畫面的設定交換 Activity 驗證 Android 提供的呼叫套件與原版簽章。

## 建置

需求：JDK 17 或 21、Android SDK platform 36／build-tools 36.0.0，以及首次下載 Gradle／Maven 依賴的網路。設定 `JAVA_HOME`，並以 `ANDROID_HOME` 或本機 `local.properties` 指定 SDK；`local.properties` 不提交。

從本目錄執行：

```powershell
.\build.ps1
```

或使用 wrapper：

```text
gradlew.bat :app:assembleDebug :app:testDebugUnitTest :app:lintDebug
```

Linux／macOS 使用 `./gradlew`。Gradle 8.13、AGP 8.13.2 與依賴版本維持固定，wrapper 下載 SHA-256 已設定。APK 輸出為 `app/build/outputs/apk/debug/app-debug.apk`。

本機已備妥 wrapper 與依賴快取時，可使用：

```powershell
.\build.ps1 -Offline -JavaHome $env:JAVA_HOME -GradleUserHome "$PWD/work/gradle-home"
```

`-JavaHome`、`-GradleUserHome` 為選用參數，預設沿用環境設定。`-Tasks` 可指定 Gradle 工作；`-Online` 保留為明確線上模式，不指定模式也允許下載。離線模式不會替尚未安裝的 wrapper 下載發行包。

## 封裝

先執行建置、單元測試與 lint，再以 Python 3.11+ 執行：

```text
python scripts/package_delivery.py --android-sdk <SDK> --java-home <JDK>
```

預設輸出至 `dist/rebuilt/0.4.4-alpha/`，不覆蓋既有附件。封裝核對當前 APK 的版本、簽章、入口、權限、native ABI 對齊，以及現存測試／lint 報告；不會代跑 Gradle 或原版樣本斷言。請在本次建置成功後立即封裝，避免使用舊報告。來源 ZIP 排除快取、APK、個人設定與本機研究資料。

歷史附件保存在 `dist/`，DexKit 來源附件保存在 `dist/third-party-sources/`。新封裝在來源附件可用時一併複製，否則摘要會標示缺少附件；發佈前核對 [第三方聲明](app/src/main/assets/THIRD_PARTY_NOTICES.txt)。

## 樣本檢查與分析工具

`tools/VerifySample.java` 需要自行提供原版 APK 與外部 JADX 1.5.6，不隨專案散佈。此檢查只支援兩份已核對、內容相同的 2488 樣本封裝：

```text
java -Xmx2g -cp <jadx-1.5.6-all.jar> tools/VerifySample.java <base.apk> app/src/main/java/tw/orstudio/cleverowl/XpCallers2488.java
```

其他可重用工具見 [tools/analysis](tools/analysis/README.md)。

## 安裝與驗收

使用已具備相容 Root／Zygisk／LSPosed 的測試環境，安裝模組並勾選 Duolingo 與本模組作用域。完整重開 Duolingo 後從隱私設置下方的 CleverOwl 進入設定；每次只開一項功能，分別核對畫面、重啟、同步與實際課程結果。hook 註冊、HTTP 2xx 或畫面變化的證據範圍不同，不能互相代替。

0.4.4 改變套件身份，Android 會視為新 App，不能覆蓋原 `dev.local.duolingoxposed`，舊設定不會自動搬移。請停用舊模組，在 LSPosed 啟用新的 `tw.orstudio.cleverowl` 並重新確認作用域，避免兩份模組同時注入。新模組預設關閉功能；入口註冊不受總開關影響。

## 目錄與文件

- `app/`、`gradle/`：Android 原始碼、測試及 wrapper。
- `tools/`、`scripts/`：樣本檢查、分析與封裝工具。
- [docs](docs/README.md)：設計、歷史診斷與版本驗證摘要。
- [遷移結果](docs/repository-planning/MIGRATION_REPORT.md)：本次搬移與重新驗證。
- `dist/`、`work/`：Git 排除的成品、原始證據、樣本、工具及快取。

自有程式授權尚待選定；第三方授權全文在 `app/src/main/assets/licenses/`。[提交前的專案設定](docs/PUBLICATION_NOTES.md) 列出其餘由開發者決定的發佈項目。
