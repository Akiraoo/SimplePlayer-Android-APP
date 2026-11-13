# Simple Player Android

> [!IMPORTANT]
> **本專案已停止維護，由 [Resona](https://github.com/Akiraoo/SimplePlayer-Resona) 接替。**
>
> Resona 是 Simple Player Android 的後繼專案，以 Rust 核心重新打造，功能更完整。它沿用相同的 Application ID 與簽章，可以直接覆蓋安裝在本 App 上，伺服器位址、封面快取與等化器設定都會保留。
>
> * 已安裝本 App：打開「應用程式更新」即可直接升級到 Resona（[v1.5.0](https://github.com/Akiraoo/SimplePlayer-android-app/releases/tag/v1.5.0)）。
> * 新安裝：請至 [Resona Releases](https://github.com/Akiraoo/SimplePlayer-Resona/releases) 下載。
>
> 本 Repository 已封存（唯讀），以下內容保留作為紀錄。

---

Simple Player 的 Android Client。

使用 Jetpack Compose 建立介面，並透過 Simple Player Server 的 Mobile API 取得音樂資料與播放內容。

## 功能

* Android 原生音樂播放器
* Jetpack Compose UI
* AndroidX Media3 播放引擎
* MediaSession / 系統媒體控制
* 播放清單
* 封面顯示
* 歌詞顯示
* 本地 Metadata Cache
* 背景播放
* 音訊頻譜顯示
* 參數等化器（最多 10 段，可匯入 Poweramp / AutoEq 預設）
* 歌曲 / 播放清單分享連結與下載
* App 內檢查更新（GitHub Releases）
* 與 Simple Player Web Server 整合

## Requirements

* Android Studio
* JDK 17
* Android SDK 35

本專案可以直接使用 Android Studio 開啟。

不需要自行建立新的 Android Project。

## 開啟專案

Clone Repository 後，在 Android Studio 中選擇：

```text
Open
```

然後開啟本專案的根目錄。

等待 Gradle Sync 完成後即可進行編譯與執行。

本專案包含 Gradle Wrapper，因此不需要另外安裝 Gradle。

## Build

Debug：

```powershell
.\gradlew.bat :app:assembleDebug
```

Release：

```powershell
.\gradlew.bat :app:assembleRelease
```

APK 會輸出至：

```text
app/build/outputs/apk/debug/app-debug.apk
```

```text
app/build/outputs/apk/release/app-release-unsigned.apk
```

Debug APK 使用 Android 的 debug key 簽章，可以直接安裝測試。

Release APK 在沒有設定 signing key 時為**未簽章**，無法直接安裝。設定方式詳見下方「APK簽名」；設定後輸出檔名會是 `app-release.apk`。

不想自己 Build 的話，可以直接從 [GitHub Releases](https://github.com/Akiraoo/SimplePlayer-android-app/releases) 下載預先編譯好的 APK。

## Package Name

目前預設 Package Name / Application ID：

```text
com.akira.simpleplayer
```

這是作者目前使用的 Package Name。

如果你要將本專案作為自己的 Fork 或衍生專案，可以自行修改成自己的 Package Name。

需要修改的部分包含：

```kotlin
android {
    namespace = "your.package.name"

    defaultConfig {
        applicationId = "your.package.name"
    }
}
```

以及 Kotlin source 中的：

```kotlin
package your.package.name
```

與對應的 source directory。

熟悉 Android Studio、Gradle 與 Kotlin 的使用者可以自行修改。

## Server

本 App 需要搭配 Simple Player Web Server 使用。

[SimplePlayer-Web-Server](https://github.com/Akiraoo/SimplePlayer-Web-Server)

開源版本的 Simple Player Server 預設使用：

| 功能         | Port |
| ---------- | ---: |
| Web Server | 50000 |
| Mobile API | 55555 |

Android Client 應連接 Mobile API，例如：

```text
http://192.168.0.100:55555
```

其中 `192.168.0.100` 必須替換成實際執行 Simple Player Server 的電腦 LAN IP。

如果 Server 是透過反向代理對外開放（例如 `https://music.example.com`），直接填入對外網址即可，不需要加上 Port。

### 分享連結

App 同步時會讀取 Server 的 `/api/config`：

* Server 有設定 `publicOrigin` 時，分享與下載連結會使用該公開網址，傳給區域網路以外的人也能開啟。
* 沒有設定時，會以 App 填入的 API 位址推算 Web Player 網址（例如 `:55555` → `:50000`）。

### 注意

Android 裝置中的：

```text
localhost
127.0.0.1
```

代表的是**Android 裝置本身**，不是你的電腦。

因此如果 Server 執行於電腦上，不能直接將：

```text
http://localhost:55555
```

填入 Android Client。

## 本地 Metadata Cache

App 支援將 Metadata 快取於 Android 裝置本地。

本地 Metadata Cache 的用途包括：

* 減少重複向 Server 請求 Metadata
* 加快重新開啟 App 時的資料顯示
* 減少不必要的網路請求

這個 Cache 與 Server 端的 Metadata Cache 是分開的。

也就是：

```text
Android Client
    │
    ├── Local Metadata Cache
    │
    ▼
Simple Player Mobile API
    │
    ├── Server Metadata Cache
    └── Music Files
```

本地 Metadata Cache 只保存相關 Metadata，不會將整個音樂庫下載到手機。

清除 App Cache 或 App 資料也不會影響 Server 上的原始音樂檔案。

## 環境

| Component         |    Version |
| ----------------- | ---------: |
| Compile SDK       |         35 |
| Target SDK        |         35 |
| Minimum SDK       |         26 |
| Java              |         17 |
| Kotlin JVM Target |         17 |
| Compose BOM       | 2025.01.00 |
| AndroidX Media3   |      1.5.1 |
| Coil              |      2.7.0 |
| Kotlin Coroutines |     1.10.1 |

## App 內更新

設定畫面中的「應用程式更新」會向 GitHub Releases 查詢最新版本，有新版時可以直接下載並交給系統安裝。

運作方式：

* 讀取 Repository 的最新 Release（`/releases/latest`），並使用該 Release 附加的第一個 `.apk` 檔。
* 以 Release 的 Tag（例如 `v1.2.0`）與 App 的 `versionName`（例如 `1.2.0`）比較版本。
* 第一次安裝時，Android 會要求允許本 App「安裝未知應用程式」。

### 發布新版本時

1. 在 `app/build.gradle.kts` 調高 `versionCode`，並將 `versionName` 設為與 Release Tag 相同的版本號（不含 `v`）。
2. 以**同一把** signing key 簽章 Release APK。簽章不同時，Android 會拒絕覆蓋安裝。
3. 在 GitHub 建立 Release（Tag 例如 `v1.2.0`），並附上簽章後的 `.apk`。

### Fork 時改成自己的 Repository

App 預設檢查本專案的 Releases。如果你發布自己的版本，請修改：

```text
app/src/main/java/com/akira/simpleplayer/update/Updater.kt
```

中的：

```kotlin
const val REPO = "Akiraoo/SimplePlayer-android-app"
```

改成你自己的 `擁有者/Repository名稱`。

否則你的 App 會收到本專案的更新，而且因為簽章不同，也無法安裝。

## APK簽名

本專案不包含作者個人的 Android signing key。

正式發布自己的 APK 時，請使用自己的 signing key。

在專案根目錄（與 `settings.gradle.kts` 同一層）建立 `keystore.properties`：

```properties
storeFile=E:/keys/simpleplayer.jks
storePassword=你的 keystore 密碼
keyAlias=你的 key alias
keyPassword=你的 key 密碼
```

`storeFile` 可以是絕對路徑，或相對於 `keystore.properties` 所在資料夾的路徑。

`keystore.properties` 也可以放在專案以外的地方，例如和 keystore 放在一起，再到使用者層級的 Gradle 設定（Windows 為 `%USERPROFILE%\.gradle\gradle.properties`）加入：

```properties
simpleplayer.keystoreProperties=D:/keys/keystore.properties
```

或設定環境變數 `SIMPLEPLAYER_KEYSTORE_PROPERTIES` 指向該檔案。專案根目錄的 `keystore.properties` 會優先使用。

設定完成後，`assembleRelease` 會直接產生已簽章的 APK：

```text
app/build/outputs/apk/release/app-release.apk
```

沒有 `keystore.properties` 時不影響 Build，只是 Release APK 不會簽章。

也可以不建立這個檔案，改用 Android Studio 的 **Build → Generate Signed App Bundle / APK** 手動選擇 keystore。

請務必備份 keystore。遺失後將無法再發布可以覆蓋安裝的更新。

請不要將以下檔案提交到公開 Repository：

```text
*.jks
*.keystore
keystore.properties
```

## License

本專案採用 **Apache License 2.0** 授權。

完整授權條款請參閱 Repository 根目錄的 `LICENSE`。

SPDX-License-Identifier：

```text
Apache-2.0
```

## 相關專案

Simple Player Web Server：

[SimplePlayer-Web-Server](https://github.com/Akiraoo/SimplePlayer-Web-Server)
