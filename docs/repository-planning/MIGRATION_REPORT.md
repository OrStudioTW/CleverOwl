# CleverOwl 目錄遷移結果

遷移日期：2026-10-29，Asia/Taipei。狀態：**遷移與本機自動驗證完成**。GitHub 提交由開發者操作。

## 完成的目錄整理

| 位置 | 現在的用途 |
| --- | --- |
| 根目錄、`app/`、`gradle/` | Android 專案、Gradle wrapper、根 `build.ps1` 與專案導覽 |
| `tools/`、`scripts/` | 樣本檢查、兩份可重用分析工具，以及參數化封裝程式 |
| `docs/design`、`docs/history` | 14 份既有技術文件分類、歷史完整 README 與統一索引 |
| `docs/verification` | 11 版歷史摘要，另選錄 0.4.3 的去識別測試／建置／lint 記錄 |
| `docs/releases` | 11 份歷史校驗清單與相對路徑說明 |
| `dist/` | 11 版歷史 APK／source ZIP、第三方來源附件，以及新位置重新建置的交付 |
| `work/archive/pre-github-migration` | 遷移前原專案、建置生成物、143 份原始驗證證據、舊腳本與原工作區說明 |
| `work/` | 原版樣本、反編譯資料、外部工具及快取；全部由 Git 排除 |

原 `outputs` 目錄已移入本機歸檔；必要的來源與資料按以上分類放到新位置。歷史檔案未刪除。原分類規劃和清單保留為歷史快照，實際來源新舊路徑與雜湊見 [migration-source-map.csv](migration-source-map.csv)。

## 完整性核對

- 原始分類清單中的 **84,821 份檔案全部仍有對應位置**。既有 Gradle 快取的少數索引在建置時正常更新；它們不屬於提交來源。
- 遷移前 `outputs` 的 **535 份檔案 SHA-256 全部一致**；原件對照保存在本機 `work/archive/pre-github-migration/preserved-files.csv`。
- 原 43 份來源候選中，**40 份內容完全一致**；有意修改的 3 份是根 `.gitignore`、根 `README.md`、第三方聲明的發行路徑。
- Java 功能、測試、套件識別、versionName 和 versionCode 沒有變更。新舊 APK 除簽章 metadata 外，只有 `assets/THIRD_PARTY_NOTICES.txt` 的內容不同；DEX、資源及 native library 內容一致。
- 舊版成品校驗清單保持原內容；重新建置的附件與新的 checksum 放在 `dist/rebuilt/0.4.3-alpha/`。

## 本次重新驗證

| 核對項目 | 結果 |
| --- | --- |
| 根目錄 wrapper 建置／測試／lint | 51 個工作執行成功；39 項單元測試通過；lint 0 errors / 4 warnings |
| 乾淨來源副本 | 僅複製 105 份當時的提交候選，不含本機 SDK 設定、原版 APK、歷史成品或研究資料；同樣 51 個工作成功、39 測試通過、lint 0 / 4 |
| 根與乾淨副本 APK | SHA-256 完全一致 |
| 原版樣本靜態斷言 | 從新根路徑重新執行，160 / 160 通過；使用本機另存的精確樣本及外部 JADX |
| APK 內容／簽章 | v2 debug 簽章通過，與原版 0.4.3 使用同一 debug 憑證；沒有桌面入口或額外權限；4 種 ABI 未壓縮且 16KB 對齊 |
| 提交來源與排除 | 以 Git 自身規則讀取候選集合，與來源封裝清單一致，保留 wrapper JAR 和無副檔名的 `xposed_init` |
| 文件與有限模式掃描 | 本機 Markdown 連結核對通過；候選內容的私鑰／GitHub token／AWS key／個人 Windows 家目錄四種格式檢查無命中 |

乾淨副本使用共用 Android SDK 與既有的外部 Gradle 依賴快取，**沒有驗證空快取的新電腦線上下載**。完整記錄見 [migration-verification.json](migration-verification.json)。原始建置與樣本核對日誌保存在本機 `work/repository-planning/`，不放入來源封裝。

目前 debug APK SHA-256：`1707f41b7151d69b73640836755c69cdf62bfab53c6d5dd32e63eed5d7f88e1c`。版本仍為 `0.4.3-alpha`（11）；新 APK 雜湊與舊交付不同，是因為第三方來源路徑聲明已更新，舊附件保留原樣。

## 建置與提交入口

從專案根目錄使用 `./build.ps1` 或 wrapper；必要的 Java／SDK 環境設定見 [根 README](../../README.md)。封裝工具使用 `scripts/package_delivery.py`，不再依賴舊版 APK 或歷史靜態結果。

`.gitignore` 已排除 `work`、`dist`、建置輸出、本機 SDK 設定及私鑰；`.gitattributes` 已設定文字換行。此次**沒有初始化工作區 Git、stage、提交、設定遠端或推送**。開發者建立 Git 提交後，需確認 `gradlew` 的 executable bit；相關命令見 [提交前的專案設定](../PUBLICATION_NOTES.md)。

自有程式根 `LICENSE` 尚待開發者選定，第三方授權檔已保留。GitHub 可見性與提交、裝置覆蓋更新、功能／模式／伺服器驗收仍由開發者確認；有限格式掃描不等於完整機密或授權審核。
