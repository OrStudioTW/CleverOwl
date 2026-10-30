> 遷移前的完整專案說明，作為歷史記錄。最新使用方式見 [根 README](../../README.md)。

# CleverOwl 0.4.3-alpha

CleverOwl 是 Duolingo 的實驗擴充模組。設定只從 Duolingo 設定的「隱私設置」下方 CleverOwl 開啟；沒有獨立設定頁或桌面圖示。使用者已回報0.4.2內嵌頁面正常工作並提供截圖，0.4.3移除外部設定介面。

內嵌整合限定精確樣本 Duolingo 6.103.6 (2488)。0.3.3 單堂×3已有結算及重啟保留證據，0.3.4起修改後的最終倍率上限為×3。其他模式、完整會員線上功能、延期與跨版本依各自驗收結果判定。

## 已寫入的功能

| 設定 | 實作 | 已知界線 |
| --- | --- | --- |
| 會員 | 原始／Super／Max，在本機 User 建構前調整輸入，並處理快取還原用衍生狀態 | 依唯一錨點及 85／109 參數模型結構匹配，僅在 6.103.6 (2488) 做過靜態驗證；不會建立伺服器訂閱，Max 線上功能未驗證 |
| XP | 原始／額外 ×2／額外 ×3；修改啟用時最終上限 ×3 | 原生加倍合併後也限制在 ×3；舊 ×5／×10 讀取為 ×3。0.3.3 樣本單堂 ×3 已驗證重啟保留；0.3.4 的新上限規則與 ×2 仍待實機驗證 |
| 加倍時間 | 既有加倍道具延長 15／30／60／180／1440 分鐘 | 相對原到期時間固定增加，不累加、不建立新道具；仍可能受伺服器限制 |
| 計時 | Lightning、Match 及 MultiSession 初始時間至少 60,000 秒（約 16.7 小時）；樣本 Rampup 倒數另最低維持 2 秒 | 前三種依唯一錨點／建構式結構匹配；倒數備援限定樣本。不是所有模式的真正無限時間 |

四項功能獨立且預設關閉；設定讀取失敗也關閉修改。倒數備援額外核對 base APK SHA-256。會員、三種練習初始時間、XP 與延長時間使用 DexKit 特徵與方法結構匹配；缺失或候選不唯一時跳過並記錄 LSPosed 日誌。動態匹配並不等於跨版本驗證。

目標 APK 憑證 SHA-256：`1233ff6de4c70ef4d7deae54af15b167fc7cfd2539517bd2175870f3cb771731`。
樣本 base SHA-256：`30d915c7e55423f071157e6ce9064bae8cd7e65ed09bb99383bae505709f456c`。

## 建置

需要 JDK 17 或 21、Android SDK platform 36、網路下載 Maven 依賴。Windows 使用 `gradlew.bat`；其他系統使用 `./gradlew`（必要時加執行權限）。以 Android Studio 開啟專案並設定 SDK，或設定 `ANDROID_HOME`；壓縮檔不含個人 `local.properties`。

```text
gradlew.bat :app:assembleDebug :app:testDebugUnitTest :app:lintDebug
```

輸出：`app/build/outputs/apk/debug/app-debug.apk`。Gradle 8.13 的下載校驗碼已固定；AGP 8.13.2、DexKit 2.3.0、Kotlin stdlib 1.5.0 與 Xposed API 82 固定版本。模組使用傳統 Xposed 入口，設定共享要求 API 93+，不是將框架版本號設成 82。

沒有 INTERNET 或儲存權限。設定共享只存開關及倍率，不存帳戶或認證資料。修改程式碼時可將 DexKit 替換為自行編譯的 AAR；模組 APK 不限制替換依賴。授權與來源見 `app/src/main/assets/THIRD_PARTY_NOTICES.txt`。

## 安裝與待完成的驗收步驟

1. 使用已具備相容 Root／Zygisk 及官方 LSPosed 的測試環境。本交付不包含框架安裝或 Root 操作；目前只核對過官方下載包 v2.2.1 (7912) 的 metadata，沒有框架實測結果。
2. 安裝模組 APK，在 LSPosed 啟用，作用域勾選 Duolingo (`com.duolingo`) 與本模組 (`dev.local.duolingoxposed`)。完整重開 Duolingo，從設定 → 隱私設置下方 → CleverOwl 開啟設定。模組本身的作用域用於無畫面的設定讀寫。
3. 先記錄原版功能與課程結果，每次只開一項功能；儲存後完整關閉再重開 Duolingo。只有頁面顯示不能證明各項功能或線上效果成功，需分別核對結果。
4. 會員：分別選 Super、Max，測試實際功能，再重啟／同步檢查。XP：記錄課前、結算、重新同步後 XP。加倍時間：跨原到期點與延長到期點測試。計時：逐模式記錄倒數、暫停、恢復、作答、提交及完成，不能只看倒數停住。
5. 驗證關閉功能／停用模組後行為。會員物件可能被 Duolingo 自己保存於快取；停用並重啟不保證立即清除先前顯示，需重新同步帳戶。避免課程中途切換。

查 LSPosed 日誌中的 `[DuolingoXposed]`；`hooks registered` 只代表註冊成功，`constructor reached`／`XP inputs reached`／`Path button preview reached` 才代表相應方法真的被呼叫（仍不代表伺服器接受）。方法可能因執行期最佳化而未經過 hook，仍須實機定位。初次啟動會計算 APK 雜湊並掃描 DEX，啟動耗時尚未量測。未檢查 16 KB 記憶體頁裝置的 native 載入相容性。

## 自動檢查

- JUnit：39 項設定／倍率／時間邊界與診斷輸出測試。10,000 次計時推進測試是已分析 reducer 的模擬，並非實機測試。
- `tools/VerifySample.java`：對原始 APK 執行 160 項 SHA／欄位／唯一錨點／方法呼叫、診斷入口與真實計時分支斷言。需要外部 JADX 1.5.6 的 `jadx-1.5.6-all.jar`（不隨附）以及自己的原版 APK。

```text
java -Xmx2g -cp jadx-1.5.6-all.jar tools/VerifySample.java base.apk app/src/main/java/dev/local/duolingoxposed/XpCallers2488.java
```

驗證程式只支援本次樣本，不會把不同 APK 當作已適配版本。APK、原版反編譯內容、個人 SDK 路徑、私鑰與帳戶資料均不在原始碼壓縮包內。

## 0.2.0 調整依據

依使用者提供 Owl Auxiliary 1.0.1.0 的 APK 分析，其會員做法是在建構前修改參數，計時做法是延長三種練習模型的初始時間。該 APK 有混淆與 native 方法，只採用可核對的行為設計，未引用或打包其程式碼、資源與 native library。現有原版樣本已確認參數位置，未照搬舊模組索引或其額外資料改動。

使用者後續發現先前未開設定，啟用後部分功能已成功；因此沒有將先前回報當成已證實的 hook 故障。本次維持預設關閉與既有設定相容，新增首次設定步驟與命中日誌。

## 0.3.0：依實機日誌修正共同載入失敗

測試裝置 LSPosed 2.0.4 (7741) 的日誌顯示設定成功讀取，但 DEX-based features 出現 UnsatisfiedLinkError。它發生在功能 hook 註冊前，不是以改畫面文字就能修正的問題。日誌只保留錯誤類型，無法單憑它確定完整 linker 原因。

對 versionName 6.103.6、versionCode 2488 且 base SHA256 完全相符的安裝，所有功能使用已驗證的類別／方法設定直接註冊，完全略過 native DEX 掃描。未知 APK 仍使用結構定位；native library 改為未壓縮、16KB對齊，正常搜尋失敗時，使用 LSPosed initZygote 提供的本模組 APK 路徑與程序 ABI 明確載入。沒有下載或執行外部程式碼。

新增載入 ABI／壓縮格式回歸測試，及打包後的四 ABI 對齊檢查。lint 目前 0 錯誤、6 警告，新增警告是明確 System.load 路徑；其來源限定為框架提供的本模組 APK，並保留說明。通過檔案與路徑驗證不等於已在另一支手機成功注入。

命中日誌另包含 XP boost remaining-time／active-state 與 Tick fallback，且同步輸出到 logcat。錯誤現在保留訊息，不再只有 UnsatisfiedLinkError 類型。請對每項功能分別查核 registered 與 reached；伺服器結果仍獨立驗收。

## 0.3.1：額外 XP 持久化診斷

本版維持 0.3.0 的功能邏輯，沒有宣稱修復額外 XP 入帳。只有精確樣本匹配且開啟 XP 倍率時，新增三項獨立觀察：

- `XP serialization completed`：CompletedSessionSurrogate 編碼完成時的 xpBoostMultiplier、hasBoost、xpPromised、expectedXpGain。編碼也可能用於快取，不代表已送出。
- `Session network write response`：Duolingo 網域下、路徑含 session 或 sessions 區段的 POST／PUT／PATCH HTTP 狀態。沒有讀取或消耗請求／回應串流；其他路由不涵蓋，沒有命中不能推定沒有提交。HTTP 2xx 不代表額外 XP 核發成功。
- `XP summary decoded`：解碼的 gainedXp 與日期數值；來源可能是網路或快取，不視為已確認的伺服器核發回應。

觀察不修改任何參數、回傳值或網路內容，不記錄帳戶識別、網址、認證、標頭或完整本文。每次程序啟動限量記錄（編碼 24、HTTP 24、摘要 32），需在測試當次結束後、再次重啟前保留日誌。安裝診斷版後，保持只有額外 ×3，重啟 Duolingo 並完成一堂課，記錄課前／結算後／重啟後總 XP。標準 XP 基準測試已有使用者結果，不需為本次診斷重做。

## 0.3.2：倍率建構方法沒有命中

使用者本次截圖是課前聯賽 70、結算 35、課後聯賽 105。0.3.1 當次程序有完整註冊及摘要／編碼／HTTP 命中，但沒有 `XP inputs reached` 與課前預覽建構命中。14:45:44 的兩次編碼均為 multiplier=1.0、hasBoost=false、xpPromised=30、expectedXpGain=null；因此不能將這次失效說成伺服器拒絕 ×3。

本版保留一次建構輸入倍率修改與唯讀診斷，針對精確樣本使用 LSPosed 的可選 `XposedBridge.deoptimizeMethod(Member)`，向 53 個直接呼叫倍率計算／工廠／建構式的方法請求去最佳化。這些呼叫點由原始 DEX 產生，靜態檢查比對完整方法描述與集合，避免修改無關多載或其他版本。靜態初始化器不能以一般 Java Method 取得，因此排除 `ywr.<clinit>`。未更動全域編譯設定、未清除其他 App 的編譯資料。框架不提供該 API 時會獨立記錄 unavailable，其他既有功能仍可註冊。

原理參考：[LSPosed 官方 XposedBridge 原始碼](https://github.com/LSPosed/LSPosed/blob/master/core/src/main/java/de/robv/android/xposed/XposedBridge.java)。另已核對本機官方 2.2.1 (7912) release 的 framework.dex 確實提供此方法；不能以此代替手機實際呼叫驗證。

`XP caller deoptimization requested: applied=53, failed=0` 只代表請求返回，仍需看到 `XP inputs reached: ... effective multiplier=3.0` 與編碼數值變化。維持只有 ×3，重開後測一堂課，確認結算、排行榜與重啟後數字。即使恢復入口，也尚未證明伺服器會採用額外倍率。本次解碼摘要也曾出現 71；記錄沒有綁定帳戶，不能將它當成使用者 XP 回退。

## 0.3.3：完成課程的加倍旗標

0.3.2 的實機日誌確認每次 53 個去最佳化請求返回、倍率建構方法恢復命中、編碼倍率已是 3.0；但 hasBoost=false、xpPromised=null、expectedXpGain=null。使用者確認本機結算 75，實際從 105 變成 130，重啟後仍為 130，表示額外部分依然沒有入帳。

原始樣本中，vu10 分別以 dqb0.a(User) 計算倍率、dqb0.d(User) 取得實際道具啟用旗標，再交給 fs7.a 建立完成課程模型。先前只改倍率輸入，沒有同步後者。本版僅在精確樣本且開啟額外倍率時，在 fs7.a 的第 10 個倍率參數大於 1 且有限時，使第 11 個 hasBoost 參數為 true；保留原本 true。不改倍率、XP 數值、其他參數或預設遮罩，不增加倍率相乘次數。這是針對已觀察到的狀態不一致修正，不能保證伺服器因此採用額外倍率。

新版日誌增加 `XP session inputs reached`。需確認其輸出與既有 `XP serialization completed` 的 hasBoost 都變成 true，並核對畫面／同步／重啟後實際增加量。若仍只有標準 XP，將代表此次一致性修正仍不足；不要將本機結算 75 或 HTTP200 當成完成。官方框架、其他版本、其他模式及其他功能的待驗收界線維持不變。

## 0.3.4：僅保留 ×3 以下設定

依使用者追加測試結果，高於 ×3 的設定失效。本版移除 ×5／×10 選項，只提供原始、額外 ×2、額外 ×3。修改啟用時，原生倍率與設定倍率合併計算的結果最高為 ×3；例如原生 ×2 再選 ×2，實際使用 ×3，不送出 ×4。選原始倍率或關閉修改時不限制 Duolingo 原生倍率。

維持設定 schema 1；舊版合法的高倍率（含 ×5／×10）在讀取時降為 ×3，設定頁也選中 ×3，下一次儲存會寫回 3。其餘開關保留；非法或錯誤型別設定仍使用關閉倍率修改的預設值。直接計算函式拒絕選擇倍率大於 3。

新增舊設定降級與原始模式保留原生倍率的回歸測試，並更新倍率疊加上限測試。本版不把 0.3.3 的實測當成 0.3.4 已完成實機驗收。

## 0.4.0：CleverOwl 與 Duolingo 原生設定

模組顯示名稱、備用設定 App 及 LSPosed 說明改為 CleverOwl。保留套件識別與設定 schema，支援覆蓋更新。精確樣本新增「隱私設置」下方的 CleverOwl 入口：原生隱私列改為中間位置，新列為分組底部，並保留原有隱私路由。

新頁面在 Duolingo 內以全頁視窗開啟，直接使用 Duolingo 自己的 DuoComposeView2、SettingsPage、SettingsActionBar、分組列、原生切換／單選元件、字體與 DuoTheme。設定包含總開關、會員、XP、延期及計時。右上角「儲存」提交並讀回核對，返回箭頭與系統返回會關閉頁面；未儲存的變更不會寫入。儲存後需完整重開 Duolingo。總開關關閉時入口仍註冊。此整合只有完整版本／版本碼／SHA 匹配才啟用。

設定經 ContentProvider 在模組 UID 下寫入現有共享設定。Provider 僅允許自身 UID 或套件名與原版簽章均符合的 Duolingo 呼叫，並嚴格限制六個欄位、型別與可選值；不提供 query／insert／update／delete 檔案接口，不存帳戶或認證。設定 App 保留相同資料的備用編輯入口。

39 項單元測試包含設定契約與偽冒呼叫者識別；150 項原始 DEX 斷言包含原生元件簽章與 Compose callback，另有 4 個 resources.arsc ID 核對。lint 0 錯誤、8 警告，新增的 exported provider 與共享偏好檔案警告已保留：跨程序接口在程式內檢查 UID／原版簽章，且只暴露功能配置。這些檢查不能代替實機 provider 授權、深淺色、重組／返回、儲存重啟與原隱私導航驗收。


## 0.4.1：修正原生設定無法讀取

0.4.0 實機日誌顯示入口已成功插入，但兩次開啟均出現 `Failed to find provider info for dev.local.duolingoxposed.settings`，隨後為 IllegalArgumentException。手機套件資料確認 provider 確有註冊，因此是從 Duolingo 解析設定接口的失敗，尚未進入原生設定頁初始化。

0.4.1 移除原 ContentProvider，改由明確指定的 SettingsExchangeActivity 進行讀寫。此 Activity 使用 Theme.NoDisplay，不呈現設定畫面，於 onCreate 完成後立即 finish；設定畫面仍由 Duolingo 的原生元件呈現。使用 startActivityForResult 取得 Android 提供的真實 calling package，再核對原版 Duolingo 簽章。來源資訊不接受 Intent 傳入的套件名或 referrer。回傳只有六個設定欄位，寫入仍經嚴格契約驗證並 commit、讀回核對。

ResultReceiver 將結果傳回主執行緒，不攔截 Duolingo 的 onActivityResult；10 秒未收到結果會顯示未確認成功，晚到的回覆不再開啟頁面。儲存使用點擊當時的設定快照，期間忽略重複儲存與選項修改。連線、頁面初始化、儲存失敗分別記錄類型及呼叫位置，不記錄例外訊息或帳戶資料。未確認儲存時應重開頁面核對。

建置、39 項單元測試、150 項原始 APK 靜態斷言通過；lint 0 錯誤、7 警告。實機橋接啟動、簽章授權、原生頁面、儲存後重啟與原隱私導航仍待安裝新版驗證。

平台依據：[Android 的自動可見性規則](https://developer.android.google.cn/training/package-visibility/automatic?hl=en) 明確允許啟動不可查詢 App 的 Activity；不能將此規則套用為所有服務／provider 均可存取。模組不要求 QUERY_ALL_PACKAGES，也不修改 Duolingo 的 Manifest。


## 0.4.2：修正設定頁首次繪製崩潰

使用者提供 18:29:33 崩潰報告與 183017.zip。0.4.1 日誌已確認設定讀取成功，錯誤發生在 Dialog.show 返回之後、第一輪非同步 Compose 繪製；「視窗已建立」不代表控制項已顯示。根因為 SettingsToggle 的 Modifier 傳入 null，卻只設定預設遮罩8；原始 DEX 的位元4才會提供 Modifier，位元8只提供 ToggleState。修正為直接提供原生 w5q.a 與 ToggleState.ENABLED、遮罩0，其他控制項也明確提供原生空 Modifier。

一併檢查原生元件的回呼鏈，發現 SettingsRadioButtonGroup 透過 nm9.B 呼叫 Function2（Composer、flags），原模組卻提供 Function3。現已改為 Function2；SettingsPage 的 ColumnScope 內容維持 Function3。這是額外的靜態缺陷發現，不把它當成本次已觀測到的崩潰。

日誌現在分開記錄視窗已建立／等待繪製，以及第一輪原生控制項組合已完成。未加入全域例外吞掉機制。

39 項單元測試、160 項原始 DEX 斷言、建置與 lint 通過（0錯誤7警告）。新增斷言從原始 DEX 的預設參數分支重現舊呼叫的 null Modifier、驗證明確參數，以及兩種回呼參數數量；這不會執行 Android Compose，實機版面、操作、儲存重啟與原隱私導航仍需安裝0.4.2驗收。


## 0.4.3：僅保留 Duolingo 內嵌設定

使用者確認0.4.2已正常工作並提供內嵌設定頁截圖。依要求刪除 MainActivity 外部設定頁、桌面 MAIN／LAUNCHER 入口及 isModuleActive 自檢hook，移除外部頁面使用的應用程式主題。

只保留無畫面的 SettingsExchangeActivity，供 Duolingo 原生設定讀寫使用；它沒有可瀏覽的設定頁或啟動器入口。保留原套件識別、偏好檔名與schema、原生頁面、設定交換與LSPosed自身作用域。以相同憑證覆蓋安裝可延續既有設定。

0.4.3建置及39項單元測試通過，lint0錯誤5警告。封裝檢查確認只有無畫面橋接Activity，沒有MainActivity類別、桌面入口或外部設定原始碼。160項原始APK斷言沿用0.4.2結果，原生元件契約與驗證工具沒有變更。新版覆蓋更新後的入口／儲存仍需實機確認。
