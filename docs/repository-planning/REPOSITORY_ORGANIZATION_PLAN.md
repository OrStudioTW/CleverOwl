# CleverOwl 原始碼分類與 GitHub 提交規劃

> 此文件是遷移前的分類規劃快照。使用者已授權執行，實際狀態與新路徑見 [MIGRATION_REPORT](MIGRATION_REPORT.md)。下文的「本輪」指原規劃階段。

盤點日期：2026-10-29，Asia/Taipei。根目錄：`E:\Github\Private\CleverOwl`。

## 1. 結論與本輪產出

建議將 `outputs/duolingo-xposed` 的 Android 專案提升為根目錄專案，使用單一儲存庫；`docs` 收納經審閱的技術文件，`tools` 收納可重用分析工具，`scripts` 收納建置封裝輔助程式，`dist` 保留不納入 Git 的發行成品，`work` 保留本機工具、快取與研究資料。

此輪交付分類規劃及檔案清單，沒有搬移或刪除既有檔案，沒有在工作區初始化 Git、建立提交、設定遠端或推送。排除規則保存在 `gitignore.proposed`，尚未套用為根目錄 `.gitignore`。

盤點的既有檔案共 **84,821 份、1,568,653,350 bytes，約 1.46 GiB**，已全部歸入 17 個互斥類別。真正的 Android 專案提交候選只有 **43 份、289,850 bytes，約 283 KiB**，包含 Gradle wrapper JAR；這 43 份與現有 `CleverOwl-0.4.3-alpha-source.zip` 逐檔 SHA-256 一致。本次規劃新建檔案不計入既有檔案基準。

## 2. 現況與資料權威來源

| 項目 | 本次讀取的現況 | 規劃影響 |
| --- | --- | --- |
| Git | 根目錄沒有 `.git`，全樹盤點未見巢狀 `.git` | 目前沒有提交、追蹤分支或遠端可供沿用 |
| Android 專案 | `outputs/duolingo-xposed` | 原始碼不應長期置於產物目錄 |
| 版本 | `0.4.3-alpha`，versionCode `11` | 首次提交以現存版本為基準，不捏造歷史提交 |
| 套件／namespace | `dev.local.duolingoxposed` | 目錄整理不變更套件、設定 schema 或更新識別 |
| Gradle 專案名稱 | `DuolingoXposed` | 可在後續另行改顯示名稱；本次搬移不需要更改 |
| 工具版本 | Gradle 8.13、AGP 8.13.2、Java 編譯目標 17、SDK 36 | 保留既有固定版本與 wrapper 下載雜湊 |
| 依賴 | DexKit 2.3.0、Kotlin stdlib 1.5.0、Xposed API 82、JUnit 4.13.2 | 維持建置契約，不順便升級依賴 |
| 根目錄 `build.ps1` | 指向 `outputs/duolingo-xposed`、`work/tools/gradle-8.13`、固定 Android Studio Java 路徑，預設離線 | 搬移時必須同步調整，提供乾淨環境的 wrapper 建置方式 |
| `work/package_delivery.py` | 固定 0.4.3、個人 SDK/JDK 路徑、讀取舊版 APK 和歷史研究結果 | 目前不能直接當作乾淨 checkout 的通用封裝入口 |
| 驗證與人工結果 | 各版本有不同時間、目標和範圍的記錄 | 保留版本與證據來源，不把舊版結果標成新版通過 |

`outputs/verification-0.4.3/summary.json` 是既有交付記錄：39 項單元測試、lint 0 errors / 5 warnings，160 項原始 APK 靜態斷言明確沿用 0.4.2。本輪只讀取並核對既有檔案，**沒有重跑上述 Android 建置、測試、裝置注入或線上驗收**。既有 README 與歷史報告如有敘述時點不同，整理時應註明版本與時間，避免用最新名稱覆蓋歷史結論。

## 3. 全檔分類與提交政策

下表加總為 84,821 份；大小的精確 bytes 與機器可讀分類見 `inventory-summary.json`。

| 類別 | 數量 | 現有位置 | 處理／提交政策 |
| --- | ---: | --- | --- |
| 專案原始碼與建置設定 | 43 | `outputs/duolingo-xposed`，排除建置及個人設定 | 提升到根目錄；列為初始提交候選 |
| 可重用／一次性腳本待審 | 5 | 根 `build.ps1` 與 `work` 四份 Java/Python 工具 | 逐份判斷；先移除個人路徑與舊版產物依賴，再納入必要工具 |
| 工作區導覽 | 1 | 根 `README.txt` | 與專案 README 合併，形成唯一根 `README.md` |
| 技術與歷史文件 | 14 | `outputs/*.md` | 經內容審閱後分到 `docs/design`、`docs/history`、`docs/verification` |
| 自動驗證／診斷證據 | 143 | `outputs/verification*` | 先保留本機原件；選擇、去識別後才納入文件或發行附件 |
| 發行校驗清單 | 11 | `outputs/SHA256SUMS*.txt` | 屬發行 metadata；保留對應版本與相對路徑，不當作原始碼 |
| 模組發行成品 | 22 | `outputs` 的 11 份 APK + 11 份 source ZIP | 移到 `dist` 或本機歷史歸檔；GitHub 使用對應 Release 附件 |
| 第三方來源封裝 | 1 | `outputs/third-party-sources/DexKit-2.3.0-source.zip` | 與依賴對應的發行附件一起保存；維持現有 notices 中的來源安排 |
| 專案建置生成物 | 297 | 專案內 `.gradle`、`build`、`app/build` | 排除並可重新產生；不納入初始提交 |
| 本機 SDK 設定 | 1 | 專案 `local.properties` | 移到新根後仍僅本機使用 |
| Gradle 依賴快取 | 4,210 | `work/gradle-home` | 本機保留，Git 排除 |
| 外部工具與下載 | 326 | `work/tools`、`work/downloads`、LSPosed 等 | 本機保留，Git 排除 |
| 第三方原版／參考 APK | 5 | `work/apk`、`work/reference/reference.apk` | 本機保留，不隨原始碼提交 |
| 第三方反編譯內容 | 79,651 | `work/decompiled-2488`、`work/reference/decompiled` | 本機保留，不因副檔名是 `.java` 就當作專案原始碼 |
| 執行日誌與任務追蹤 | 25 | `work/runtime-logs`、`work/todoist*.json` | 本機保留，Git 排除 |
| 歷史遷移清單 | 3 | `outputs/migration` | 轉存 `work/archive/migration`；不公開含本機全路徑的原件 |
| 其餘研究資料 | 63 | `work` 其餘反組譯、分析輸出及暫存 | 本機保留；有長期價值的結論另整理成去識別文件 |

完整逐檔分類：`work/repository-planning/all-files.csv`。此份含大量研究檔名的清單留在本機，不建議提交 Git。每列包含原路徑、bytes、唯一分類、處理決策與規劃去向。

## 4. 建議目錄結構

以下是**遷移完成後的目標**，不是目前已存在的實作結果；標示「新增」的內容需在後續建立。

```text
CleverOwl/
├── README.md                       # 以現有專案 README 為主，合併工作區導覽
├── LICENSE                         # 新增；自有程式授權由開發者決定
├── .gitignore                      # 以本次 proposed 為起點
├── .gitattributes                  # 新增；文字換行與 wrapper 腳本規則
├── build.gradle
├── settings.gradle
├── gradle.properties
├── gradlew
├── gradlew.bat
├── gradle/wrapper/
│   ├── gradle-wrapper.jar           # 必須提交
│   └── gradle-wrapper.properties
├── build.ps1                       # 改為可攜入口；保留本機快取選項
├── app/
│   ├── build.gradle
│   └── src/
│       ├── main/
│       │   ├── AndroidManifest.xml
│       │   ├── java/dev/local/duolingoxposed/
│       │   ├── res/
│       │   └── assets/              # xposed_init、第三方聲明與授權全文
│       └── test/java/dev/local/duolingoxposed/
├── tools/
│   ├── VerifySample.java           # 原路徑保留，避免無必要變更
│   └── analysis/                   # 選擇性收納整理後的 InspectDex/DecompileOne
├── scripts/
│   └── package_delivery.py         # 改寫後再納入，不直接照搬現有腳本
├── docs/
│   ├── design/
│   ├── history/                    # 各版變更、診斷及驗證邊界
│   ├── verification/               # 建議按 v0.1.0、v0.2.0…v0.4.3 分目錄
│   ├── releases/                   # 校驗清單及發行資料索引
│   └── repository-planning/        # 本次規劃、候選清單及核對證據
├── dist/                           # Git 排除；APK/source ZIP/第三方 source 附件
└── work/                           # Git 排除；現有工具、快取、原版樣本與研究資料
```

目前只有一個 Android `app`，不需要增加 `src/CleverOwl`、`packages` 或把它拆成多個儲存庫。`app/src/main/java` 與 `app/src/test/java` 是 Android／Gradle 的實際來源邊界，不再套一層籠統的 `src`。

## 5. 43 份專案候選的內部分工

精確逐檔對照見 `source-file-map.csv`；本次維持現有 Java package，不進行功能重構。

| 分工 | 現有檔案／內容 | 整理方式 |
| --- | --- | --- |
| 建置與導覽，10 份 | `.gitignore`、`README.md`、兩份 Gradle 建置檔、settings/gradle properties、兩份 wrapper 腳本、wrapper JAR/properties | 路徑提升到根；根 `build.ps1` 另列待整理工具 |
| 模組入口與定位 | `ModuleEntry`、`HookResolver`、`MethodKey`、`NativeLoader` | 保持 `app/src/main/java/dev/local/duolingoxposed` |
| 設定資料與交換 | `FeatureSettings`、`SettingsContract`、`SettingsClient`、`SettingsExchangeActivity` | 保留無畫面交換 Activity，不補回已移除的外部設定頁 |
| 原生內嵌設定介面 | `NativeSettingsUi` | 保留現有位置；目錄整理不改原生資源契約 |
| 功能及 XP 診斷 | `FeatureLogic`、`ConstructorInputs`、`XpObservation`、`XpDiagnostics`、`XpDeoptimization`、`XpCallers2488` | 保留 15 份 main Java 的整體集合與既有樣本範圍 |
| 單元測試，7 份 | ConstructorInputs、FeatureLogic、MethodKey、NativeLoader、SessionBoostFlag、SettingsContract、XpObservation 各 Test | 保留 Gradle 測試目錄，不搬到根 `tests` |
| manifest 與資源，10 份 | Manifest、strings、圖示、xposed_init、THIRD_PARTY_NOTICES、5 份 licenses | 全部保留；授權資產不是可刪除的 build 檔 |
| 樣本檢查工具，1 份 | `tools/VerifySample.java` | 保留；原版 APK 與 JADX 由開發者另外提供 |

上表除細分 main Java 的數列外，共為 10 + 15 + 7 + 10 + 1 = 43 份。`XpCallers2488.java` 雖然依樣本產生，仍是現有程式使用的版本契約，先列入專案來源候選；它不等同於整包反編譯來源。未來如補上產生器，另記錄輸入及產生方法。

## 6. 文件與工具的具體去向

`review-file-map.csv` 列出待處理的 201 份檔案（143 份驗證證據、22 份發行成品、14 技術文件、11 校驗清單、5 腳本、3 遷移紀錄、1 第三方來源包、1 工作區說明、1 本機設定）；它是處理清單，**不是全部可以提交的清單**。

| 現有項目 | 目標與必要調整 |
| --- | --- |
| 根 `README.txt` + 專案 `README.md` | 合併為根 `README.md`：功能範圍、支援樣本、建置、安裝、文件索引與待驗收；歷史長段落移到 `docs/history` |
| `outputs/開發規格與相容性基準.md` | `docs/design/開發規格與相容性基準.md`，核對最新版本狀態 |
| `outputs/驗證報告.md` | `docs/verification/驗證報告.md`；明示它的版本與時間，不作最新全功能總結 |
| 12 份版本變更／診斷 `.md` | `docs/history`，保留檔名與版本脈絡；審閱畫面數字、使用者描述與路徑後納入 |
| `outputs/verification*` | 規劃最終統一為 `docs/verification/v<version>`；原 `verification` 依其 summary 確認為 0.1.0 後標記。保留本機原件，再產生去識別副本 |
| `outputs/SHA256SUMS*.txt` | 歷史清單保持內容；搬到 `docs/releases` 時註明原先以發行附件根目錄解析。新版封裝重新產生以 `dist` 附件為基準的清單 |
| 根 `build.ps1` | 保留根入口；改用根專案與 wrapper，JDK 由 `JAVA_HOME`／工具設定解析；本機 `work/gradle-home` 可作顯式快取選項，不要求新環境已有快取 |
| `work/package_delivery.py` | 改成參數化版本、SDK、JDK、專案與輸出路徑；驗證當次建置，不能把舊版靜態結果默默當作本次重跑；移除對 0.2.0／0.4.2 產物的強制依賴 |
| `work/InspectDex.java`、`work/DecompileOne.java` | 可整理成 `tools/analysis`；說明命令列輸入、外部 JADX/dexlib2 依賴及輸出，不夾帶分析對象 |
| `work/migrate_workspace.py` | 一次性歷史遷移工具，留 `work/archive`，不作必要專案工具提交 |
| `outputs/migration` | 原件移 `work/archive/migration`，不把 20 MB 的全機路徑 manifest 當作專案文件 |

檔案表的 `ProposedPath` 是分類規劃；像 `verification-0.4.3` 先忠實保留目前版本名稱，正式執行時再按上面的 `v0.4.3` 命名統一並更新所有相對連結。

## 7. Git 排除、版本與發行策略

根 `.gitignore` 草案見同目錄 `gitignore.proposed`：排除整個 `work`、`outputs`、`dist`、快取、建置產物、本機 SDK 設定、私鑰和環境檔；保留 `.env.example` 類範本。**不能使用全面的 `*.jar` 排除**，否則會漏掉 `gradle-wrapper.jar`。亦不把所有 ZIP 一律當成原碼或必須入庫的附件。

草案的 `/outputs/` 是遷移後的保護規則。來源尚未提升到根之前就套用它，會把仍在 `outputs/duolingo-xposed` 的來源一起隱藏；因此需與原始碼搬移同一步驟套用。現存 `outputs/duolingo-xposed/.gitignore` 只能保護該子目錄，無法保護同層 APK、`work` 或遷移紀錄。

新增 `.gitattributes` 時採文字自動辨識，Gradle/Python/Java/Markdown 與 shell wrapper 使用 LF；Windows `.bat`／`.ps1` 依團隊執行環境明訂換行。`gradlew` 在 Git index 標示可執行，檢查 wrapper JAR、properties 及下載 SHA-256 一併存在。首次搬移不要混入全專案換行重寫。

初始 Git 提交依序建議：

1. **來源基準**：根 Android 專案候選、必要導覽、排除規則、換行設定及已選定的自有程式授權。先檢查 staged 清單與內容，再簽署提交。
2. **可攜工具**：已修改並在乾淨 checkout 驗證的 `build.ps1`、通用封裝及選定分析工具。
3. **文件與索引**：完成公開內容審閱的設計、歷史、驗證摘要與本次規劃。原始執行日誌和個人遷移清單繼續留本機。

目前沒有 Git 歷史；不能從舊 source ZIP 假造先前真實提交。歷史 ZIP／APK 作為發行存檔保留；核對最終建置與版本後，才建立相符的 `v0.4.3-alpha` tag／Release。自動產生的 GitHub source archive 與舊手工 source ZIP 的內容、根目錄名稱及 SHA 可能不同，不互相冒充。

APK 與 source ZIP 不放入一般程式碼提交；發行附件包含選定 APK、需要保留的來源封裝、校驗清單及依現有 third-party notices 所列的對應來源附件。歷史 `debug.apk` 仍標示 debug，不重新命名成正式 release 簽署版本。簽署金鑰及密碼不入庫；金鑰管理與正式簽署另訂，不在分類時替換現有簽章。

自有程式根 `LICENSE` 目前缺少，不能把 `assets/licenses` 內的第三方授權當成整個專案的授權。保留現有第三方聲明與完整授權文字，並同步修正 `THIRD_PARTY_NOTICES.txt` 內 `third-party-sources/...` 的發行相對路徑。這裡規劃資料位置與來源連結，不宣稱已完成完整授權審核。

## 8. 執行遷移的順序與完成條件

| 階段 | 操作 | 完成條件／證據 |
| --- | --- | --- |
| 1. 固定基準 | 記錄既有版本、43 份原碼 SHA、最新 APK/source ZIP SHA；避免與其他建置或修改同時操作 | 形成帶時間的基準；若檔案改變，重新盤點，不照搬本次快照 |
| 2. 整理來源 | 將 43 份來源提升到根；`local.properties` 只在本機另行移入；建置產物不搬入待提交集合 | 每份來源遷移前後 SHA 一致；README/.gitignore 有意合併的差異另列。此步不改 Java package |
| 3. 整理其他資料 | 建立 `docs`／`dist`，按對照表處理；先保留原始證據與歷史附件，再去識別文件 | 無檔案遺失、路徑碰撞、覆蓋或未處理分類；`work` 留存與排除 |
| 4. 修正引用 | 同步根 build、封裝工具、README、notice、校驗清單及文件相對連結 | 不再有必要流程依賴舊 `outputs/duolingo-xposed` 或個人絕對路徑 |
| 5. 在新位置驗證 | 使用根 wrapper 建置 assembleDebug/testDebugUnitTest/lintDebug；比對模組 metadata、入口與資源 | 新位置的本次建置、測試、lint 與 APK 證據保存；靜態樣本檢查若執行需顯式提供外部 APK/JADX |
| 6. 乾淨環境驗證 | 只使用預定入庫檔案另建乾淨 checkout，允許首次下載依賴，從根建置 | 不依賴本機 `work`、舊 APK 或舊證據；缺少外部樣本的分析工作清楚標示可選 |
| 7. Git 提交 | 選定分支與遠端後初始化；只 stage 明確列出的範圍，檢查大小、文件及排除，再簽署提交 | staged 不含快取、原版 APK、反編譯包、日誌或私密設定；提交內容與驗證來源一致 |
| 8. GitHub 交付 | 核對可見性與權限後推送指定分支，視需要建立 tag／Release | 核對遠端 SHA、附件與 checksum；GitHub CI 如未執行則保持未驗證 |

`outputs/duolingo-xposed/app/build` 與 `.gradle` 保留原件至新位置建置成功後再考慮清理；本次規劃不要求刪除。日後若要刪除研究、歷史產物或快取，另按明確範圍處理。

## 9. 待開發者決定與人工驗收

- GitHub 目的儲存庫、預設分支與公開／私人可見性；目前未設定或推測遠端。
- 自有程式的根 `LICENSE` 選擇與 third-party notices 的對應發行安排。
- 哪些歷史診斷、使用者描述及驗證證據可公開；預設先提交經審閱的摘要，保留原件在本機。
- 是否只發佈最新 0.4.3-alpha，或另保存全部 11 版附件。歷史檔案全數先保留本機，不自動刪除。
- 0.4.3 覆蓋安裝、設定延續、功能／模式／伺服器結果仍按各自的人工驗收追蹤；本次分類結果不能代替它們。

以上選項不影響本輪完成盤點和規劃。實際遷移、提交、推送與裝置驗收屬後續操作，不能在本輪標為已完成。

## 10. 本輪檔案與驗證範圍

| 本輪產出 | 用途 | 建議是否提交 |
| --- | --- | --- |
| `REPOSITORY_ORGANIZATION_PLAN.md` | 完整規劃 | 依文件公開範圍審閱 |
| `inventory-summary.json` | 17 類數量、大小與快照時間 | 可作規劃證據，先審閱 |
| `source-file-map.csv` | 43 份候選來源、新舊路徑、SHA-256 | 可作遷移核對證據 |
| `review-file-map.csv` | 201 份需決策／整理的檔案 | 工作清單，不等於提交清單 |
| `gitignore.proposed` | 新根目錄排除草案 | 遷移時轉為根 `.gitignore` |
| `verification.json` | 本次全檔分類、封裝比對、有限模式掃描及排除規則核對 | 可作規劃證據，先審閱 |
| `work/repository-planning/all-files.csv` | 84,821 份既有檔案的分類對照 | 僅本機 |
| `work/repository-planning/*.ps1` | 本次盤點與草案核對輔助工具 | 僅本機，不是產品必要工具 |

自動證據覆蓋：每個既有檔案恰好一類；來源集合與 0.4.3 ZIP 的路徑、數量和內容 SHA 一致；使用 Git 自身排除規則核對候選根路徑與排除項。來源文字只檢查私鑰標記、GitHub token 格式、AWS access-key 格式和個人 Windows 路徑四種模式，沒有發現命中；這不是完整機密掃描，也不代表 143 份歷史證據已核准公開。

規劃交付：Todoist「其他任務」對話板塊 `2026-10-29｜CleverOwl 原始碼分類規劃｜01a18757`；執行／自動核對依實際證據完成，人工驗收保持未完成。原模組功能開發對話的任務狀態沒有更改。
