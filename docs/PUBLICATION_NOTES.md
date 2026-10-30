# 提交前的專案設定

原始碼與建置入口已整理到根目錄；`work/`、`dist/`、本機 SDK 設定及建置輸出由 `.gitignore` 排除。

- 根 `LICENSE` 尚未設定：請選定自有程式授權後再補入。`assets/licenses` 是第三方授權文字，不能代替專案授權。
- 保留 `app/src/main/assets/THIRD_PARTY_NOTICES.txt` 與 licenses。發行附件中的 `third-party-sources/DexKit-2.3.0-source.zip` 應與相符版本一起保存。
- GitHub 目的儲存庫為 [OrStudioTW/CleverOwl](https://github.com/OrStudioTW/CleverOwl)，原始碼使用 `main`。依開發者授權，提交排除全部 APK、其他已編譯成品、`dist/`、`work/` 與本機設定；保留必要的 Gradle wrapper JAR。原目錄遷移階段的紀錄維持其當時狀態。
- Windows 建立 Git 提交時，請確認 `gradlew` 的 executable bit：可在加入檔案後使用 `git update-index --chmod=+x gradlew`。
- 裝置與線上功能驗收沿用各版本的範圍；來源搬移與本機建置不會完成這些人工項目。
