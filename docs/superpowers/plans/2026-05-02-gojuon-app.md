# 日语五十音 App Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Build a native Android app (`日语五十音`) showing 46 hiragana cards with swipe navigation, romaji toggle, random-order toggle, tap-to-pronounce (TTS), and tap-to-animate stroke order — installable APK on the user's connected Redmi `marble`.

**Architecture:** Single-Activity Compose app. One screen (`CardScreen`) with `HorizontalPager`. State (page index + 2 toggle bools + shuffle list) lives in `rememberSaveable`. Stroke animations are pre-generated `AnimatedVectorDrawable` XML resources from KanjiVG SVG via a one-shot Python script. Pronunciation uses Android built-in `TextToSpeech` with `Locale.JAPANESE`.

**Tech Stack:** Kotlin 1.9+ · Jetpack Compose (Material 3, BOM) · AGP 8.x · Gradle 8.x · JDK 17 (Temurin) · Android SDK 34 · Python 3 (one-time script) · KanjiVG (CC-BY-SA-3.0)

**Verification model:** Spec explicitly excludes automated tests (§2.2 / §10). Each phase ends with a manual `adb install` to the connected device + a checklist. Real-device verification is the source of truth.

**Connected device for verification:** Redmi `marble`, serial `c19636e1` (already connected via USB).

**Spec reference:** `docs/superpowers/specs/2026-05-02-gojuon-app-design.md`

---

## Phase 0: Environment Setup

### Task 0.1: Add Android + Java env vars to ~/.zshrc

**Files:**
- Modify: `/Users/kongkongyzt/.zshrc` (append section)

- [ ] **Step 1: Append env block to ~/.zshrc**

Edit `/Users/kongkongyzt/.zshrc`, append at end:

```bash

# --- Android development (added 2026-05-02 for GojuonCards) ---
export JAVA_HOME=/Library/Java/JavaVirtualMachines/temurin-17.jdk/Contents/Home
export ANDROID_HOME=$HOME/Library/Android/sdk
export PATH=$JAVA_HOME/bin:$ANDROID_HOME/platform-tools:$ANDROID_HOME/cmdline-tools/latest/bin:$ANDROID_HOME/emulator:$PATH
```

Note: `ANDROID_HOME` points to `~/Library/Android/sdk` which is where Android Studio installs SDK by default (not the brew `/opt/homebrew/share/android-commandlinetools` legacy path). The brew SDK was an artifact of earlier setup and can stay; Android Studio will manage its own going forward.

- [ ] **Step 2: Verify by sourcing in a fresh shell**

Run:
```bash
zsh -lc 'echo "JAVA_HOME=$JAVA_HOME"; echo "ANDROID_HOME=$ANDROID_HOME"; java -version 2>&1 | head -1'
```

Expected output:
```
JAVA_HOME=/Library/Java/JavaVirtualMachines/temurin-17.jdk/Contents/Home
ANDROID_HOME=/Users/kongkongyzt/Library/Android/sdk
openjdk version "17.0.X" ...
```

Note: `~/Library/Android/sdk` won't exist yet (created in Task 1.2 by Android Studio). That's fine — env var can point to a not-yet-existing dir.

- [ ] **Step 3: Commit**

```bash
# .zshrc isn't in our project repo; no git commit for this step.
# Just confirm env vars work by opening a new terminal tab and running:
echo $JAVA_HOME
echo $ANDROID_HOME
```

---

## Phase 1: Bootstrap Android Studio Project

### Task 1.1: First-launch Android Studio Welcome wizard

**Files:** None (this is interactive setup)

- [ ] **Step 1: Open Android Studio**

```bash
open -a "Android Studio"
```

- [ ] **Step 2: Walk through Welcome wizard (USER INTERACTIVE)**

Inside Android Studio:
- "Import Android Studio settings" → **Do not import** → Next
- "Welcome" splash → click **Next**
- "Install Type" → **Standard** → Next
- "Select UI Theme" → user choice → Next
- "Verify Settings" → confirms it'll install:
  - Android SDK to `~/Library/Android/sdk`
  - SDK Platform (latest, API 35 typically)
  - SDK Build-Tools (latest)
  - Android Emulator
  - Intel/Apple Silicon HAXM (Apple Silicon: skipped automatically)
- → Next → accept all licenses → **Finish**
- Wait for downloads (~5-10 min, ~2 GB)

When the "Welcome to Android Studio" project picker appears, **leave it open** for Task 1.2.

- [ ] **Step 3: Verify SDK installed**

```bash
ls ~/Library/Android/sdk
```

Expected: includes at least `platforms/`, `platform-tools/`, `build-tools/`, `licenses/`, `emulator/`.

### Task 1.2: Create New Project via Android Studio wizard

**Files:** Will be generated under `~/AndroidStudioProjects/GojuonCards_temp/` (we relocate in Task 1.3)

- [ ] **Step 1: Open New Project wizard (USER INTERACTIVE)**

In Android Studio Welcome window:
- Click **New Project**
- Select template: **Empty Activity** (under Phone and Tablet — the one with the Compose icon, says "Creates a new empty activity with Jetpack Compose")
- Click **Next**

- [ ] **Step 2: Fill wizard fields exactly**

| Field | Value |
|---|---|
| Name | `GojuonCards_temp` |
| Package name | `com.kongkongyzt.gojuon` |
| Save location | `/Users/kongkongyzt/AndroidStudioProjects/GojuonCards_temp` |
| Language | Kotlin (default) |
| Minimum SDK | API 24: Android 7.0 (Nougat) |
| Build configuration language | Kotlin DSL (build.gradle.kts) |

Click **Finish**. Wait for initial Gradle sync to complete (~2-5 min, downloads dependencies).

- [ ] **Step 3: Verify project generated correctly**

```bash
ls /Users/kongkongyzt/AndroidStudioProjects/GojuonCards_temp/
```

Expected: includes `app/`, `gradle/`, `gradlew`, `gradlew.bat`, `settings.gradle.kts`, `build.gradle.kts`, `gradle.properties`.

```bash
cat /Users/kongkongyzt/AndroidStudioProjects/GojuonCards_temp/app/src/main/java/com/kongkongyzt/gojuon/MainActivity.kt
```

Expected: a default `MainActivity` with `setContent { ... Greeting("Android") ... }` boilerplate.

### Task 1.3: Relocate generated project into our git repo

**Files:**
- Source: `/Users/kongkongyzt/AndroidStudioProjects/GojuonCards_temp/*`
- Dest: `/Users/kongkongyzt/AndroidProjects/GojuonCards/` (already has `docs/`, `.git/`)

- [ ] **Step 1: Quit Android Studio first**

In Android Studio menu: Android Studio → Quit Android Studio. (Important — moving an open project's files corrupts AS internal indexes.)

Verify quit:
```bash
pgrep -f "Android Studio" && echo "STILL RUNNING — quit it first" || echo "OK, AS quit"
```

- [ ] **Step 2: Move generated files into existing repo**

```bash
cd /Users/kongkongyzt/AndroidStudioProjects/GojuonCards_temp
# Use rsync to merge contents (preserves dotfiles, doesn't overwrite our docs/)
rsync -a --exclude='.git' --exclude='.idea' --exclude='build/' --exclude='app/build/' \
  ./ /Users/kongkongyzt/AndroidProjects/GojuonCards/
```

- [ ] **Step 3: Add .gitignore for Android**

Create `/Users/kongkongyzt/AndroidProjects/GojuonCards/.gitignore`:

```gitignore
# Android Studio / IntelliJ
.idea/
*.iml
.gradle/
local.properties
captures/

# Build outputs
build/
app/build/

# OS
.DS_Store
Thumbs.db

# Generated
*.hprof
```

- [ ] **Step 4: Open project in Android Studio at the new location**

```bash
open -a "Android Studio" /Users/kongkongyzt/AndroidProjects/GojuonCards
```

Wait for Gradle sync (1-2 min). It will recreate `.idea/` and `local.properties` (gitignored).

- [ ] **Step 5: Delete the temp project**

```bash
rm -rf /Users/kongkongyzt/AndroidStudioProjects/GojuonCards_temp
```

- [ ] **Step 6: Initial commit of scaffold**

```bash
cd /Users/kongkongyzt/AndroidProjects/GojuonCards
git add .gitignore settings.gradle.kts build.gradle.kts gradle.properties gradle/ gradlew gradlew.bat app/
git commit -m "$(cat <<'EOF'
chore: scaffold Empty Compose Activity project

Generated via Android Studio New Project wizard (Empty Activity template,
Kotlin DSL, minSdk 24, package com.kongkongyzt.gojuon).

Co-Authored-By: Claude Opus 4.7 (1M context) <noreply@anthropic.com>
EOF
)"
```

### Task 1.4: First build + install to verify toolchain

- [ ] **Step 1: Open new terminal tab (so env vars from Task 0 are loaded)**

Run:
```bash
cd /Users/kongkongyzt/AndroidProjects/GojuonCards
echo $ANDROID_HOME  # should be /Users/kongkongyzt/Library/Android/sdk
adb devices         # should list c19636e1
```

- [ ] **Step 2: Run installDebug**

```bash
cd /Users/kongkongyzt/AndroidProjects/GojuonCards
./gradlew installDebug
```

Expected: BUILD SUCCESSFUL after **5-15 min** on first run (downloads AGP / Kotlin / Compose / Material 3 / activity-compose deps, ~500 MB total to `~/.gradle/caches/`). Subsequent builds take seconds. At end:
```
> Task :app:installDebug
Installing APK 'app-debug.apk' on 'marble - 14' for app:debug
Installed on 1 device.
BUILD SUCCESSFUL
```

If it fails with "SDK location not found" — `local.properties` is missing. Create:
```bash
echo "sdk.dir=$HOME/Library/Android/sdk" > /Users/kongkongyzt/AndroidProjects/GojuonCards/local.properties
```
Then retry.

- [ ] **Step 3: Launch on device**

```bash
adb shell monkey -p com.kongkongyzt.gojuon -c android.intent.category.LAUNCHER 1
```

Expected on device: a white screen with "Hello Android!" text. (Stock Compose template.)

If you see this — **the entire toolchain works end-to-end on real hardware.** Proceed.

- [ ] **Step 4: Commit if any local changes**

```bash
git status
# If clean, no commit needed.
# If local.properties was created, it's gitignored so still clean.
```

---

## Phase 2: Theme + Data

### Task 2.1: Set Material 3 blue theme (#1E88E5)

**Files:**
- Modify: `app/src/main/java/com/kongkongyzt/gojuon/ui/theme/Color.kt`
- Modify: `app/src/main/java/com/kongkongyzt/gojuon/ui/theme/Theme.kt`

- [ ] **Step 1: Replace Color.kt**

Open `app/src/main/java/com/kongkongyzt/gojuon/ui/theme/Color.kt`. Replace entire file with:

```kotlin
package com.kongkongyzt.gojuon.ui.theme

import androidx.compose.ui.graphics.Color

// Brand seed (Material 3 will derive the rest)
val BluePrimary = Color(0xFF1E88E5)        // 主色:中等亮度蓝
val BluePrimaryDark = Color(0xFF1565C0)    // 深色模式主色
val BlueSecondary = Color(0xFF42A5F5)      // 次要(浅一档)
val BlueOnPrimary = Color(0xFFFFFFFF)      // 主色上的文字
```

- [ ] **Step 2: Replace Theme.kt**

Open `app/src/main/java/com/kongkongyzt/gojuon/ui/theme/Theme.kt`. Replace entire file with:

```kotlin
package com.kongkongyzt.gojuon.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

private val LightColors = lightColorScheme(
    primary = BluePrimary,
    onPrimary = BlueOnPrimary,
    secondary = BlueSecondary,
    onSecondary = BlueOnPrimary,
)

private val DarkColors = darkColorScheme(
    primary = BluePrimaryDark,
    onPrimary = BlueOnPrimary,
    secondary = BlueSecondary,
    onSecondary = BlueOnPrimary,
)

@Composable
fun GojuonCardsTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    // dynamicColor=false intentionally: brand blue overrides Material You.
    val colors = if (darkTheme) DarkColors else LightColors
    MaterialTheme(
        colorScheme = colors,
        typography = Typography,  // keep generated Typography
        content = content
    )
}
```

(Keep `Typography.kt` as generated — no edits needed.)

- [ ] **Step 3: Build to verify no compile errors**

```bash
cd /Users/kongkongyzt/AndroidProjects/GojuonCards
./gradlew assembleDebug
```

Expected: BUILD SUCCESSFUL.

- [ ] **Step 4: Commit**

```bash
git add app/src/main/java/com/kongkongyzt/gojuon/ui/theme/
git commit -m "$(cat <<'EOF'
feat: apply blue Material 3 theme

Brand seed #1E88E5; intentionally disable dynamicColor so
Material You does not override.

Co-Authored-By: Claude Opus 4.7 (1M context) <noreply@anthropic.com>
EOF
)"
```

### Task 2.2: Define Kana data + 五十音表

**Files:**
- Create: `app/src/main/java/com/kongkongyzt/gojuon/data/Kana.kt`

- [ ] **Step 1: Create Kana.kt**

Create file `app/src/main/java/com/kongkongyzt/gojuon/data/Kana.kt`:

```kotlin
package com.kongkongyzt.gojuon.data

import androidx.annotation.DrawableRes

/**
 * 一个清音平假名条目。
 *
 * @param char 假名字符,例: "あ"
 * @param romaji 罗马字(Hepburn),例: "a"、"shi"、"tsu"、"chi"、"fu"、"wo"、"n"
 * @param row 所在行,例: "あ行"、"か行"、"わ行"、"ん行"
 * @param drawableRes 笔顺动画 AVD 资源 ID(在 stroke 数据生成后由 Phase 7 任务填入有效值;
 *                    占位期间用 0,UI 在 0 时把"笔顺"按钮置灰)
 */
data class Kana(
    val char: String,
    val romaji: String,
    val row: String,
    @DrawableRes val drawableRes: Int = 0,
)

/**
 * 46 个清音平假名,按五十音表标准顺序。
 * 顺序:あ行 → か行 → さ行 → た行 → な行 → は行 → ま行 → や行 → ら行 → わ行 → ん。
 */
val GOJUON: List<Kana> = listOf(
    // あ行
    Kana("あ", "a", "あ行"),
    Kana("い", "i", "あ行"),
    Kana("う", "u", "あ行"),
    Kana("え", "e", "あ行"),
    Kana("お", "o", "あ行"),
    // か行
    Kana("か", "ka", "か行"),
    Kana("き", "ki", "か行"),
    Kana("く", "ku", "か行"),
    Kana("け", "ke", "か行"),
    Kana("こ", "ko", "か行"),
    // さ行
    Kana("さ", "sa", "さ行"),
    Kana("し", "shi", "さ行"),
    Kana("す", "su", "さ行"),
    Kana("せ", "se", "さ行"),
    Kana("そ", "so", "さ行"),
    // た行
    Kana("た", "ta", "た行"),
    Kana("ち", "chi", "た行"),
    Kana("つ", "tsu", "た行"),
    Kana("て", "te", "た行"),
    Kana("と", "to", "た行"),
    // な行
    Kana("な", "na", "な行"),
    Kana("に", "ni", "な行"),
    Kana("ぬ", "nu", "な行"),
    Kana("ね", "ne", "な行"),
    Kana("の", "no", "な行"),
    // は行
    Kana("は", "ha", "は行"),
    Kana("ひ", "hi", "は行"),
    Kana("ふ", "fu", "は行"),
    Kana("へ", "he", "は行"),
    Kana("ほ", "ho", "は行"),
    // ま行
    Kana("ま", "ma", "ま行"),
    Kana("み", "mi", "ま行"),
    Kana("む", "mu", "ま行"),
    Kana("め", "me", "ま行"),
    Kana("も", "mo", "ま行"),
    // や行
    Kana("や", "ya", "や行"),
    Kana("ゆ", "yu", "や行"),
    Kana("よ", "yo", "や行"),
    // ら行
    Kana("ら", "ra", "ら行"),
    Kana("り", "ri", "ら行"),
    Kana("る", "ru", "ら行"),
    Kana("れ", "re", "ら行"),
    Kana("ろ", "ro", "ら行"),
    // わ行
    Kana("わ", "wa", "わ行"),
    Kana("を", "wo", "わ行"),
    // ん
    Kana("ん", "n", "ん行"),
)

@Suppress("unused")
private val gojuonSizeCheck: Unit = run {
    require(GOJUON.size == 46) { "GOJUON must contain exactly 46 entries, got ${GOJUON.size}" }
}
```

- [ ] **Step 2: Build to verify**

```bash
./gradlew assembleDebug
```

Expected: BUILD SUCCESSFUL.

- [ ] **Step 3: Commit**

```bash
git add app/src/main/java/com/kongkongyzt/gojuon/data/
git commit -m "$(cat <<'EOF'
feat: add Kana data class + 46 五十音清音表

Hardcoded list in standard order(あ行 → ん). drawableRes
defaults to 0 until Phase 7 wires up stroke AVD resources.

Co-Authored-By: Claude Opus 4.7 (1M context) <noreply@anthropic.com>
EOF
)"
```

### Task 2.3: Set App display name

**Files:**
- Modify: `app/src/main/res/values/strings.xml`

- [ ] **Step 1: Replace strings.xml**

Replace the entire content of `app/src/main/res/values/strings.xml` with:

```xml
<resources>
    <string name="app_name">日语五十音</string>

    <!-- TopAppBar -->
    <string name="toggle_romaji">罗马字</string>
    <string name="toggle_random">随机</string>

    <!-- Card -->
    <string name="btn_stroke">笔顺</string>
    <string name="btn_prev">上一个</string>
    <string name="btn_next">下一个</string>

    <!-- TTS errors -->
    <string name="tts_unavailable">系统未安装日语 TTS,请到「设置 → 文字转语音」安装日语语音包</string>
    <string name="tts_init_failed">语音引擎初始化失败</string>
</resources>
```

- [ ] **Step 2: Verify on device**

```bash
./gradlew installDebug
adb shell monkey -p com.kongkongyzt.gojuon -c android.intent.category.LAUNCHER 1
```

On device: launcher icon label and the in-app TopAppBar (when implemented) will read 日语五十音.

- [ ] **Step 3: Commit**

```bash
git add app/src/main/res/values/strings.xml
git commit -m "$(cat <<'EOF'
feat: set app name and UI string resources

App name: 日语五十音. Add labels for toggles, buttons, and
TTS error messages to be wired in later phases.

Co-Authored-By: Claude Opus 4.7 (1M context) <noreply@anthropic.com>
EOF
)"
```

---

## Phase 3: Static Card UI + Pager Navigation

### Task 3.1: Build CardScreen with HorizontalPager

**Files:**
- Create: `app/src/main/java/com/kongkongyzt/gojuon/ui/CardScreen.kt`
- Modify: `app/src/main/java/com/kongkongyzt/gojuon/MainActivity.kt`

- [ ] **Step 1: Create CardScreen.kt**

Create file `app/src/main/java/com/kongkongyzt/gojuon/ui/CardScreen.kt`:

```kotlin
package com.kongkongyzt.gojuon.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kongkongyzt.gojuon.R
import com.kongkongyzt.gojuon.data.GOJUON
import com.kongkongyzt.gojuon.data.Kana
import kotlinx.coroutines.launch

/**
 * 卡片总数。固定 46(清音平假名)。
 * Pager 用 [VIRTUAL_PAGE_COUNT] 模拟无限滚动以支持首尾循环。
 */
private const val KANA_COUNT = 46
private const val VIRTUAL_PAGE_COUNT = 10_000  // 大数模拟无限;中点附近开始
private val INITIAL_VIRTUAL_PAGE = (VIRTUAL_PAGE_COUNT / 2).let { it - it % KANA_COUNT }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CardScreen() {
    val pagerState = rememberPagerState(
        initialPage = INITIAL_VIRTUAL_PAGE,
        pageCount = { VIRTUAL_PAGE_COUNT }
    )
    val scope = rememberCoroutineScope()

    val currentRealIndex = ((pagerState.currentPage % KANA_COUNT) + KANA_COUNT) % KANA_COUNT
    val displayList: List<Kana> = GOJUON  // Phase 4 will swap to shuffled order when toggle is on

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(text = "日语五十音") },
                colors = TopAppBarDefaults.topAppBarColors()
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            HorizontalPager(
                state = pagerState,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(0.dp)
                    .weight(1f)
            ) { virtualPage ->
                val realIndex = ((virtualPage % KANA_COUNT) + KANA_COUNT) % KANA_COUNT
                CardContent(kana = displayList[realIndex])
            }

            BottomNav(
                currentIndex = currentRealIndex,
                onPrev = {
                    scope.launch { pagerState.animateScrollToPage(pagerState.currentPage - 1) }
                },
                onNext = {
                    scope.launch { pagerState.animateScrollToPage(pagerState.currentPage + 1) }
                },
            )
        }
    }
}

@Composable
private fun CardContent(kana: Kana) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = kana.char,
                fontSize = 220.sp,
                fontWeight = FontWeight.Normal,
            )
            Spacer(Modifier.height(8.dp))
            Text(
                text = kana.romaji,
                fontSize = 36.sp,
            )
            Spacer(Modifier.height(48.dp))
            OutlinedButton(onClick = { /* Phase 7 stroke trigger */ }) {
                Text(stringRes(R.string.btn_stroke))
            }
            Spacer(Modifier.height(8.dp))
            Text(
                text = kana.row,
                fontSize = 14.sp,
            )
        }
    }
}

@Composable
private fun BottomNav(currentIndex: Int, onPrev: () -> Unit, onNext: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            Button(onClick = onPrev) { Text("← " + stringRes(R.string.btn_prev)) }
            Button(onClick = onNext) { Text(stringRes(R.string.btn_next) + " →") }
        }
        Spacer(Modifier.height(8.dp))
        Text(text = "${currentIndex + 1} / $KANA_COUNT", fontSize = 14.sp)
    }
}

@Composable
private fun stringRes(id: Int): String =
    androidx.compose.ui.res.stringResource(id = id)
```

- [ ] **Step 2: Replace MainActivity.kt**

Open `app/src/main/java/com/kongkongyzt/gojuon/MainActivity.kt`. Replace entire content with:

```kotlin
package com.kongkongyzt.gojuon

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.kongkongyzt.gojuon.ui.CardScreen
import com.kongkongyzt.gojuon.ui.theme.GojuonCardsTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            GojuonCardsTheme {
                CardScreen()
            }
        }
    }
}
```

- [ ] **Step 3: Build + install + launch on device**

```bash
cd /Users/kongkongyzt/AndroidProjects/GojuonCards
./gradlew installDebug && adb shell monkey -p com.kongkongyzt.gojuon -c android.intent.category.LAUNCHER 1
```

- [ ] **Step 4: Manual verification on device**

- [ ] App opens with TopAppBar reading "日语五十音" (蓝色,Material 3)
- [ ] Center shows 「あ」(超大字号)+ 下方 「a」+ 「笔顺」按钮 + 「あ行」
- [ ] 底部按钮 [← 上一个] [下一个 →] + "1 / 46"
- [ ] **Swipe left** → 切到「い」, 计数变 "2 / 46"
- [ ] **Swipe right** → 回到「あ」, 计数变 "1 / 46"
- [ ] 在「あ」上再 **Swipe right** → 跳到「ん」("46 / 46") —— 首尾循环验证
- [ ] **点 [下一个 →]** → 同 swipe left 一致
- [ ] **点 [← 上一个]** → 同 swipe right 一致
- [ ] 在「ん」上点 [下一个 →] → 跳回「あ」,"1 / 46"

如果以上全部 ✓,继续。否则定位问题(常见:HorizontalPager 没拿到高度 → 检查 `Modifier.weight(1f)` 是否生效)。

- [ ] **Step 5: Commit**

```bash
git add app/src/main/java/com/kongkongyzt/gojuon/
git commit -m "$(cat <<'EOF'
feat: card screen with HorizontalPager + nav buttons

Single Composable screen showing big kana, romaji, row label,
and a 笔顺 button (placeholder). Pager simulates infinite
scroll(VIRTUAL_PAGE_COUNT = 10000) for first/last looping.
Bottom buttons mirror swipe.

Co-Authored-By: Claude Opus 4.7 (1M context) <noreply@anthropic.com>
EOF
)"
```

---

## Phase 4: Toggle Switches (Romaji + Random Order)

### Task 4.1: Add state holders + TopAppBar switches

**Files:**
- Modify: `app/src/main/java/com/kongkongyzt/gojuon/ui/CardScreen.kt`

- [ ] **Step 1: Replace CardScreen.kt entirely**

Replace the full content of `app/src/main/java/com/kongkongyzt/gojuon/ui/CardScreen.kt` with:

```kotlin
package com.kongkongyzt.gojuon.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kongkongyzt.gojuon.R
import com.kongkongyzt.gojuon.data.GOJUON
import com.kongkongyzt.gojuon.data.Kana
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch

private const val KANA_COUNT = 46
private const val VIRTUAL_PAGE_COUNT = 10_000
private val INITIAL_VIRTUAL_PAGE = (VIRTUAL_PAGE_COUNT / 2).let { it - it % KANA_COUNT }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CardScreen() {
    // ── 持久化状态 ──
    var showRomaji by rememberSaveable { mutableStateOf(true) }
    var randomOrder by rememberSaveable { mutableStateOf(false) }
    // 当前真实 kana 索引(在 GOJUON 中的 index, 0..45),独立于 pager 的 virtual page
    var currentRealIndex by rememberSaveable { mutableStateOf(0) }
    // 随机洗牌结果(GOJUON 索引的排列);非 random 模式时固定 0..45
    var shuffleSeed by rememberSaveable { mutableStateOf(0L) }

    val orderedIndices: List<Int> = remember(randomOrder, shuffleSeed) {
        if (randomOrder) {
            (0 until KANA_COUNT).shuffled(java.util.Random(shuffleSeed))
        } else {
            (0 until KANA_COUNT).toList()
        }
    }

    // 当前 kana 在 orderedIndices 中的位置,用于初始化 pager
    val currentDisplayPos: Int = remember(orderedIndices, currentRealIndex) {
        orderedIndices.indexOf(currentRealIndex).coerceAtLeast(0)
    }

    val pagerState = rememberPagerState(
        initialPage = INITIAL_VIRTUAL_PAGE + currentDisplayPos,
        pageCount = { VIRTUAL_PAGE_COUNT }
    )
    val scope = rememberCoroutineScope()

    // ── 把 pager 当前页同步回 currentRealIndex(用户滑动时持续更新) ──
    LaunchedEffect(pagerState, orderedIndices) {
        snapshotFlow { pagerState.currentPage }
            .distinctUntilChanged()
            .collect { virtualPage ->
                val displayPos = ((virtualPage % KANA_COUNT) + KANA_COUNT) % KANA_COUNT
                currentRealIndex = orderedIndices[displayPos]
            }
    }

    // ── 切换 random 开关时:洗牌种子刷新,并把 pager 跳到当前 kana 的新位置(尽量保持当前假名) ──
    LaunchedEffect(randomOrder) {
        if (randomOrder) {
            shuffleSeed = System.currentTimeMillis()
        }
        // 等 orderedIndices 重算
        // 注意:这个 effect 在 randomOrder/shuffleSeed 变后会重新触发,通过下面 effect 处理跳转
    }
    LaunchedEffect(orderedIndices) {
        val newPos = orderedIndices.indexOf(currentRealIndex)
        if (newPos >= 0) {
            val targetVirtual = INITIAL_VIRTUAL_PAGE + newPos
            // 不要 animate 跳转(会动画穿过中间所有页),用 scrollToPage(瞬时)
            pagerState.scrollToPage(targetVirtual)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(text = "日语五十音") },
                actions = {
                    SwitchLabel(
                        text = androidx.compose.ui.res.stringResource(R.string.toggle_romaji),
                        checked = showRomaji,
                        onCheckedChange = { showRomaji = it }
                    )
                    SwitchLabel(
                        text = androidx.compose.ui.res.stringResource(R.string.toggle_random),
                        checked = randomOrder,
                        onCheckedChange = { randomOrder = it }
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors()
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            HorizontalPager(
                state = pagerState,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(0.dp)
                    .weight(1f)
            ) { virtualPage ->
                val displayPos = ((virtualPage % KANA_COUNT) + KANA_COUNT) % KANA_COUNT
                val kanaIndex = orderedIndices[displayPos]
                CardContent(
                    kana = GOJUON[kanaIndex],
                    showRomaji = showRomaji,
                )
            }

            BottomNav(
                positionLabel = "${currentDisplayPos + 1} / $KANA_COUNT",
                onPrev = { scope.launch { pagerState.animateScrollToPage(pagerState.currentPage - 1) } },
                onNext = { scope.launch { pagerState.animateScrollToPage(pagerState.currentPage + 1) } },
            )
        }
    }
}

@Composable
private fun SwitchLabel(text: String, checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.padding(horizontal = 4.dp)
    ) {
        Text(text = text, fontSize = 12.sp)
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}

@Composable
private fun CardContent(kana: Kana, showRomaji: Boolean) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = kana.char,
                fontSize = 220.sp,
                fontWeight = FontWeight.Normal,
            )
            Spacer(Modifier.height(8.dp))
            if (showRomaji) {
                Text(text = kana.romaji, fontSize = 36.sp)
            } else {
                Spacer(Modifier.height(36.dp))  // 占位保持布局稳定
            }
            Spacer(Modifier.height(48.dp))
            OutlinedButton(onClick = { /* Phase 7 stroke trigger */ }) {
                Text(androidx.compose.ui.res.stringResource(R.string.btn_stroke))
            }
            Spacer(Modifier.height(8.dp))
            Text(text = kana.row, fontSize = 14.sp)
        }
    }
}

@Composable
private fun BottomNav(positionLabel: String, onPrev: () -> Unit, onNext: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            Button(onClick = onPrev) {
                Text("← " + androidx.compose.ui.res.stringResource(R.string.btn_prev))
            }
            Button(onClick = onNext) {
                Text(androidx.compose.ui.res.stringResource(R.string.btn_next) + " →")
            }
        }
        Spacer(Modifier.height(8.dp))
        Text(text = positionLabel, fontSize = 14.sp)
    }
}
```

- [ ] **Step 2: Build + install + launch**

```bash
./gradlew installDebug && adb shell monkey -p com.kongkongyzt.gojuon -c android.intent.category.LAUNCHER 1
```

- [ ] **Step 3: Manual verification**

- [ ] TopAppBar 右侧两个开关:「罗马字」(默认开 ✓)、「随机」(默认关 ○)
- [ ] 关掉「罗马字」→ 卡片中的 "a" 消失,布局不抖动(空 Spacer 占位生效)
- [ ] 重开「罗马字」→ "a" 重新出现
- [ ] 开启「随机」→ 当前卡片**保持不变**(不会跳到第一张),但向左/右 swipe 后不再是相邻的五十音表顺序
- [ ] 关闭「随机」→ 同样**保持当前假名**,继续 swipe 恢复五十音表顺序
- [ ] 多次开关「随机」→ 每次开启都重新洗牌(因为 shuffleSeed 重置)
- [ ] **旋转屏幕** 横屏 ↔ 竖屏(`adb shell wm rotation 1` 然后 `adb shell wm rotation 0`)→ 当前页码、两个开关状态都保留(`rememberSaveable` 配置变更测试;注意 `force-stop` 会清掉 saved state,**不是**正确测法)

- [ ] **Step 4: Commit**

```bash
git add app/src/main/java/com/kongkongyzt/gojuon/ui/CardScreen.kt
git commit -m "$(cat <<'EOF'
feat: romaji and random-order toggles in TopAppBar

Both switches saved via rememberSaveable. Random order seeded
by System.currentTimeMillis on each enable. Toggling order
preserves current kana via pager scrollToPage to its new index.

Co-Authored-By: Claude Opus 4.7 (1M context) <noreply@anthropic.com>
EOF
)"
```

---

## Phase 5: TTS (点假名发音)

### Task 5.1: JapaneseTts wrapper

**Files:**
- Create: `app/src/main/java/com/kongkongyzt/gojuon/tts/JapaneseTts.kt`

- [ ] **Step 1: Create JapaneseTts.kt**

Create file `app/src/main/java/com/kongkongyzt/gojuon/tts/JapaneseTts.kt`:

```kotlin
package com.kongkongyzt.gojuon.tts

import android.content.Context
import android.speech.tts.TextToSpeech
import android.widget.Toast
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import com.kongkongyzt.gojuon.R
import java.util.Locale

/**
 * 日语 TTS 封装。生命周期跟 Composition 绑定(离开 Composition 时 shutdown)。
 * 用法: `val tts = rememberJapaneseTts(); tts.speak("あ")`
 *
 * 内部初始化是异步(TextToSpeech 回调式)。在 ready=false 期间,speak() 静默丢弃。
 * 当系统未装日语数据时,第一次 speak 会 Toast 提示一次。
 */
class JapaneseTts(private val appContext: Context) {
    private val tts: TextToSpeech
    @Volatile private var ready: Boolean = false
    @Volatile private var japaneseAvailable: Boolean = false
    @Volatile private var initFailed: Boolean = false
    private var hasShownUnavailableToast: Boolean = false

    init {
        tts = TextToSpeech(appContext) { status ->
            if (status != TextToSpeech.SUCCESS) {
                initFailed = true
            } else {
                val result = tts.setLanguage(Locale.JAPANESE)
                japaneseAvailable = result != TextToSpeech.LANG_MISSING_DATA
                        && result != TextToSpeech.LANG_NOT_SUPPORTED
            }
            ready = true
        }
    }

    fun speak(text: String) {
        if (!ready) return  // 初始化未完成时静默丢弃(用户 0.5s 内再点就行)
        if (initFailed) {
            Toast.makeText(appContext, R.string.tts_init_failed, Toast.LENGTH_SHORT).show()
            return
        }
        if (!japaneseAvailable) {
            if (!hasShownUnavailableToast) {
                Toast.makeText(appContext, R.string.tts_unavailable, Toast.LENGTH_LONG).show()
                hasShownUnavailableToast = true
            }
            return
        }
        tts.stop()  // 中断上一次,避免连击堆积
        tts.speak(text, TextToSpeech.QUEUE_FLUSH, null, "kana_${text.hashCode()}")
    }

    fun shutdown() {
        try {
            tts.stop()
            tts.shutdown()
        } catch (_: Throwable) {
            // best effort
        }
    }
}

@Composable
fun rememberJapaneseTts(): JapaneseTts {
    val ctx = LocalContext.current.applicationContext
    val instance = remember(ctx) { JapaneseTts(ctx) }
    DisposableEffect(instance) {
        onDispose { instance.shutdown() }
    }
    return instance
}
```

- [ ] **Step 2: Build to verify compile**

```bash
./gradlew assembleDebug
```

Expected: BUILD SUCCESSFUL.

- [ ] **Step 3: Commit**

```bash
git add app/src/main/java/com/kongkongyzt/gojuon/tts/
git commit -m "$(cat <<'EOF'
feat: TTS wrapper for Japanese pronunciation

JapaneseTts holds a TextToSpeech tied to a composition's
lifetime via DisposableEffect. speak() is a no-op when
Japanese voice data is unavailable; first call shows a
Toast pointing at system TTS settings.

Co-Authored-By: Claude Opus 4.7 (1M context) <noreply@anthropic.com>
EOF
)"
```

### Task 5.2: Wire tap-on-card to TTS

**Files:**
- Modify: `app/src/main/java/com/kongkongyzt/gojuon/ui/CardScreen.kt`

- [ ] **Step 1: Edit CardScreen.kt**

In `CardScreen.kt`:

1. Add to imports (top of file, with other imports):

```kotlin
import androidx.compose.foundation.clickable
import com.kongkongyzt.gojuon.tts.rememberJapaneseTts
```

2. In `CardScreen()` composable, near the top after the `var ... rememberSaveable {...}` blocks, add:

```kotlin
val tts = rememberJapaneseTts()
```

3. Change `CardContent`'s signature and body to accept and use `onTapKana`:

Replace existing `CardContent` definition:

```kotlin
@Composable
private fun CardContent(kana: Kana, showRomaji: Boolean) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = kana.char,
                fontSize = 220.sp,
                fontWeight = FontWeight.Normal,
            )
            ...
```

with:

```kotlin
@Composable
private fun CardContent(kana: Kana, showRomaji: Boolean, onTapKana: () -> Unit) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = kana.char,
                fontSize = 220.sp,
                fontWeight = FontWeight.Normal,
                modifier = Modifier.clickable { onTapKana() }
            )
            Spacer(Modifier.height(8.dp))
            if (showRomaji) {
                Text(text = kana.romaji, fontSize = 36.sp)
            } else {
                Spacer(Modifier.height(36.dp))
            }
            Spacer(Modifier.height(48.dp))
            OutlinedButton(onClick = { /* Phase 7 stroke trigger */ }) {
                Text(androidx.compose.ui.res.stringResource(R.string.btn_stroke))
            }
            Spacer(Modifier.height(8.dp))
            Text(text = kana.row, fontSize = 14.sp)
        }
    }
}
```

4. In the `HorizontalPager { virtualPage -> ... }` block, change the `CardContent(...)` call to:

```kotlin
CardContent(
    kana = GOJUON[kanaIndex],
    showRomaji = showRomaji,
    onTapKana = { tts.speak(GOJUON[kanaIndex].char) },
)
```

- [ ] **Step 2: Build + install + launch**

```bash
./gradlew installDebug && adb shell monkey -p com.kongkongyzt.gojuon -c android.intent.category.LAUNCHER 1
```

- [ ] **Step 3: Manual verification**

- [ ] 点大假名「あ」→ 听到日语 /a/ 发音
- [ ] 快速连点 → 不会堆积排队,后一次中断前一次
- [ ] 切到「ち」点击 → 听到 /chi/(不是 /chee/ 之类英语化)
- [ ] 切到「ふ」点击 → 听到 /fu/(不是 /hu/)
- [ ] 全 46 字过一遍发音(不强制,但建议至少抽查 5-10 字确认日语 TTS 数据完整)
- [ ] **若听到完全无反应**:`adb shell pm list packages | grep -iE "tts|speech"`,如果只看到 `com.android.tts` 没看到 google.tts 的日语数据包,需要在系统设置 → 文字转语音 → 引擎设置中安装日语数据。安装后立即可用,无需重启 App。

- [ ] **Step 4: Commit**

```bash
git add app/src/main/java/com/kongkongyzt/gojuon/ui/CardScreen.kt
git commit -m "$(cat <<'EOF'
feat: tap large kana to play TTS pronunciation

CardContent now takes onTapKana callback, applied to the big
kana Text via Modifier.clickable.

Co-Authored-By: Claude Opus 4.7 (1M context) <noreply@anthropic.com>
EOF
)"
```

---

## Phase 6: Stroke Order Data Generation (Python)

### Task 6.1: Download KanjiVG hiragana SVGs

**Files:**
- Create: `scripts/download_kanjivg.sh`
- Create: `scripts/kanjivg_raw/` (data dir, gitignored)

- [ ] **Step 1: Create download script**

Create `scripts/download_kanjivg.sh`:

```bash
#!/usr/bin/env bash
# Downloads KanjiVG SVGs for the 46 清音 hiragana into scripts/kanjivg_raw/.
# License: KanjiVG is CC-BY-SA-3.0, see https://kanjivg.tagaini.net/

set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
RAW_DIR="$SCRIPT_DIR/kanjivg_raw"
mkdir -p "$RAW_DIR"

# 46 hiragana codepoints (lowercase hex, 5 digits, matching KanjiVG file naming)
CODEPOINTS=(
  03042 03044 03046 03048 0304a    # あいうえお
  0304b 0304d 0304f 03051 03053    # かきくけこ
  03055 03057 03059 0305b 0305d    # さしすせそ
  0305f 03061 03064 03066 03068    # たちつてと
  0306a 0306b 0306c 0306d 0306e    # なにぬねの
  0306f 03072 03075 03078 0307b    # はひふへほ
  0307e 0307f 03080 03081 03082    # まみむめも
  03084 03086 03088                # やゆよ
  03089 0308a 0308b 0308c 0308d    # らりるれろ
  0308f 03092                       # わを
  03093                             # ん
)

BASE_URL="https://raw.githubusercontent.com/KanjiVG/kanjivg/master/kanji"

for cp in "${CODEPOINTS[@]}"; do
  out="$RAW_DIR/${cp}.svg"
  if [ -f "$out" ]; then
    echo "skip $cp (cached)"
    continue
  fi
  url="$BASE_URL/${cp}.svg"
  echo "fetch $cp ..."
  curl -fsSL --retry 3 -o "$out" "$url" || { echo "FAILED $cp"; exit 1; }
done

echo "Done: $(ls "$RAW_DIR" | wc -l | tr -d ' ') files in $RAW_DIR"
```

Make executable:
```bash
chmod +x /Users/kongkongyzt/AndroidProjects/GojuonCards/scripts/download_kanjivg.sh
```

- [ ] **Step 2: Add scripts/kanjivg_raw/ to .gitignore**

Append to `/Users/kongkongyzt/AndroidProjects/GojuonCards/.gitignore`:

```gitignore

# KanjiVG raw data (regeneratable, ~200 KB)
scripts/kanjivg_raw/
```

- [ ] **Step 3: Run download**

```bash
cd /Users/kongkongyzt/AndroidProjects/GojuonCards
bash scripts/download_kanjivg.sh
```

Expected: 46 SVG files in `scripts/kanjivg_raw/`. If any fail (network), re-run.

- [ ] **Step 4: Spot check one file**

```bash
head -5 /Users/kongkongyzt/AndroidProjects/GojuonCards/scripts/kanjivg_raw/03042.svg
```

Expected: starts with `<?xml version="1.0" encoding="UTF-8"?>` and references KanjiVG namespace.

- [ ] **Step 5: Commit script**

```bash
git add .gitignore scripts/download_kanjivg.sh
git commit -m "$(cat <<'EOF'
chore: script to download KanjiVG hiragana SVGs

Fetches the 46 清音 SVG files from KanjiVG/master into a
gitignored cache dir. KanjiVG is CC-BY-SA-3.0.

Co-Authored-By: Claude Opus 4.7 (1M context) <noreply@anthropic.com>
EOF
)"
```

### Task 6.2: Python script: KanjiVG SVG → AnimatedVectorDrawable XML

**Files:**
- Create: `scripts/kanjivg_to_avd.py`

- [ ] **Step 1: Create Python converter**

Create `scripts/kanjivg_to_avd.py`:

```python
#!/usr/bin/env python3
"""
Convert KanjiVG SVG files (in scripts/kanjivg_raw/) to Android
AnimatedVectorDrawable XML files (in app/src/main/res/drawable/).

Each output file animates strokes one-by-one using
trimPathEnd ObjectAnimators sequenced by startOffset.

Usage:
    python3 scripts/kanjivg_to_avd.py
"""

import xml.etree.ElementTree as ET
from pathlib import Path
import sys

# (codepoint hex, romaji-based filename suffix) — mirrors data/Kana.kt order
KANA_TABLE = [
    ("03042", "a"), ("03044", "i"), ("03046", "u"), ("03048", "e"), ("0304a", "o"),
    ("0304b", "ka"), ("0304d", "ki"), ("0304f", "ku"), ("03051", "ke"), ("03053", "ko"),
    ("03055", "sa"), ("03057", "shi"), ("03059", "su"), ("0305b", "se"), ("0305d", "so"),
    ("0305f", "ta"), ("03061", "chi"), ("03064", "tsu"), ("03066", "te"), ("03068", "to"),
    ("0306a", "na"), ("0306b", "ni"), ("0306c", "nu"), ("0306d", "ne"), ("0306e", "no"),
    ("0306f", "ha"), ("03072", "hi"), ("03075", "fu"), ("03078", "he"), ("0307b", "ho"),
    ("0307e", "ma"), ("0307f", "mi"), ("03080", "mu"), ("03081", "me"), ("03082", "mo"),
    ("03084", "ya"), ("03086", "yu"), ("03088", "yo"),
    ("03089", "ra"), ("0308a", "ri"), ("0308b", "ru"), ("0308c", "re"), ("0308d", "ro"),
    ("0308f", "wa"), ("03092", "wo"),
    ("03093", "n"),
]

SVG_NS = "{http://www.w3.org/2000/svg}"
STROKE_DURATION_MS = 400
STROKE_WIDTH = "3"
STROKE_COLOR_LIGHT = "#FF000000"  # AVD will inherit from theme via tint? Use solid for now.
VIEWPORT = 109                    # KanjiVG canonical viewbox

ROOT = Path(__file__).resolve().parent.parent
RAW_DIR = ROOT / "scripts" / "kanjivg_raw"
OUT_DIR = ROOT / "app" / "src" / "main" / "res" / "drawable"


def extract_paths(svg_file: Path) -> list[str]:
    """Return list of `d=` attribute strings for each stroke path, in order."""
    tree = ET.parse(svg_file)
    root = tree.getroot()
    # KanjiVG: <g id="kvg:StrokePaths_XXXXX">...<path d="..."/>...</g>
    paths: list[str] = []
    for g in root.iter(f"{SVG_NS}g"):
        gid = g.attrib.get("id", "")
        if gid.startswith("kvg:StrokePaths_"):
            for p in g.iter(f"{SVG_NS}path"):
                d = p.attrib.get("d")
                if d:
                    paths.append(d)
            break
    return paths


def build_avd_xml(paths: list[str]) -> str:
    """Build the AnimatedVectorDrawable XML string."""
    n = len(paths)
    if n == 0:
        raise ValueError("no strokes found")

    # Vector with all stroke paths; initial trimPathEnd=0 (invisible), animated to 1.
    # Render strokes (no fill) with even stroke width.
    path_elements = "\n".join(
        f'        <path\n'
        f'            android:name="stroke{i + 1}"\n'
        f'            android:pathData="{d}"\n'
        f'            android:strokeColor="{STROKE_COLOR_LIGHT}"\n'
        f'            android:strokeWidth="{STROKE_WIDTH}"\n'
        f'            android:strokeLineCap="round"\n'
        f'            android:strokeLineJoin="round"\n'
        f'            android:trimPathStart="0"\n'
        f'            android:trimPathEnd="0"\n'
        f'            android:fillColor="#00000000"/>'
        for i, d in enumerate(paths)
    )

    target_blocks = "\n".join(
        f'    <target android:name="stroke{i + 1}">\n'
        f'        <aapt:attr name="android:animation">\n'
        f'            <objectAnimator\n'
        f'                android:propertyName="trimPathEnd"\n'
        f'                android:duration="{STROKE_DURATION_MS}"\n'
        f'                android:valueFrom="0"\n'
        f'                android:valueTo="1"\n'
        f'                android:startOffset="{i * STROKE_DURATION_MS}"/>\n'
        f'        </aapt:attr>\n'
        f'    </target>'
        for i in range(n)
    )

    return (
        '<?xml version="1.0" encoding="utf-8"?>\n'
        '<animated-vector\n'
        '    xmlns:android="http://schemas.android.com/apk/res/android"\n'
        '    xmlns:aapt="http://schemas.android.com/aapt">\n'
        '    <aapt:attr name="android:drawable">\n'
        f'        <vector\n'
        f'            android:width="220dp"\n'
        f'            android:height="220dp"\n'
        f'            android:viewportWidth="{VIEWPORT}"\n'
        f'            android:viewportHeight="{VIEWPORT}">\n'
        f'{path_elements}\n'
        '        </vector>\n'
        '    </aapt:attr>\n'
        f'{target_blocks}\n'
        '</animated-vector>\n'
    )


def main() -> int:
    OUT_DIR.mkdir(parents=True, exist_ok=True)
    successes = 0
    failures: list[tuple[str, str, str]] = []

    for cp, suffix in KANA_TABLE:
        src = RAW_DIR / f"{cp}.svg"
        dst = OUT_DIR / f"stroke_{suffix}.xml"
        if not src.exists():
            failures.append((cp, suffix, "missing raw SVG"))
            continue
        try:
            paths = extract_paths(src)
            if not paths:
                failures.append((cp, suffix, "no stroke paths"))
                continue
            xml = build_avd_xml(paths)
            dst.write_text(xml, encoding="utf-8")
            successes += 1
            print(f"OK   {cp} -> {dst.name}  ({len(paths)} strokes)")
        except Exception as e:
            failures.append((cp, suffix, repr(e)))

    print(f"\n--- {successes} success / {len(failures)} failures ---")
    for cp, suf, why in failures:
        print(f"  FAIL {cp} ({suf}): {why}")

    return 0 if not failures else 1


if __name__ == "__main__":
    sys.exit(main())
```

- [ ] **Step 2: Run script**

```bash
cd /Users/kongkongyzt/AndroidProjects/GojuonCards
python3 scripts/kanjivg_to_avd.py
```

Expected:
```
OK   03042 -> stroke_a.xml  (3 strokes)
OK   03044 -> stroke_i.xml  (2 strokes)
...
--- 46 success / 0 failures ---
```

- [ ] **Step 3: Spot check one generated file**

```bash
head -30 /Users/kongkongyzt/AndroidProjects/GojuonCards/app/src/main/res/drawable/stroke_a.xml
```

Expected: starts with `<?xml version="1.0" encoding="utf-8"?>` then `<animated-vector ...>` with `<path>` and `<target>` elements.

```bash
ls /Users/kongkongyzt/AndroidProjects/GojuonCards/app/src/main/res/drawable/stroke_*.xml | wc -l
```

Expected: `46`.

- [ ] **Step 4: Build to make sure all 46 AVD compile**

```bash
./gradlew assembleDebug
```

Expected: BUILD SUCCESSFUL. (If any AVD has malformed XML, gradle aapt2 will report which file/line.)

- [ ] **Step 5: Commit**

```bash
git add scripts/kanjivg_to_avd.py app/src/main/res/drawable/stroke_*.xml
git commit -m "$(cat <<'EOF'
feat: generate 46 AnimatedVectorDrawable stroke files from KanjiVG

Python converter reads scripts/kanjivg_raw/*.svg and emits
AVD XML to app/src/main/res/drawable/stroke_<romaji>.xml.
Each stroke animates trimPathEnd 0→1 over 400ms, sequenced
by startOffset.

Stroke data © KanjiVG (CC-BY-SA-3.0).

Co-Authored-By: Claude Opus 4.7 (1M context) <noreply@anthropic.com>
EOF
)"
```

---

## Phase 7: Stroke Animation UI

### Task 7.1: Wire drawableRes into Kana data

**Files:**
- Modify: `app/src/main/java/com/kongkongyzt/gojuon/data/Kana.kt`

- [ ] **Step 1: Replace each `Kana(...)` constructor call to include drawable lookup**

In `app/src/main/java/com/kongkongyzt/gojuon/data/Kana.kt`, replace the 46-item `GOJUON` list with one that passes the matching drawable. Replace the entire `val GOJUON = listOf(...)` block with:

```kotlin
val GOJUON: List<Kana> = listOf(
    Kana("あ", "a", "あ行", R.drawable.stroke_a),
    Kana("い", "i", "あ行", R.drawable.stroke_i),
    Kana("う", "u", "あ行", R.drawable.stroke_u),
    Kana("え", "e", "あ行", R.drawable.stroke_e),
    Kana("お", "o", "あ行", R.drawable.stroke_o),
    Kana("か", "ka", "か行", R.drawable.stroke_ka),
    Kana("き", "ki", "か行", R.drawable.stroke_ki),
    Kana("く", "ku", "か行", R.drawable.stroke_ku),
    Kana("け", "ke", "か行", R.drawable.stroke_ke),
    Kana("こ", "ko", "か行", R.drawable.stroke_ko),
    Kana("さ", "sa", "さ行", R.drawable.stroke_sa),
    Kana("し", "shi", "さ行", R.drawable.stroke_shi),
    Kana("す", "su", "さ行", R.drawable.stroke_su),
    Kana("せ", "se", "さ行", R.drawable.stroke_se),
    Kana("そ", "so", "さ行", R.drawable.stroke_so),
    Kana("た", "ta", "た行", R.drawable.stroke_ta),
    Kana("ち", "chi", "た行", R.drawable.stroke_chi),
    Kana("つ", "tsu", "た行", R.drawable.stroke_tsu),
    Kana("て", "te", "た行", R.drawable.stroke_te),
    Kana("と", "to", "た行", R.drawable.stroke_to),
    Kana("な", "na", "な行", R.drawable.stroke_na),
    Kana("に", "ni", "な行", R.drawable.stroke_ni),
    Kana("ぬ", "nu", "な行", R.drawable.stroke_nu),
    Kana("ね", "ne", "な行", R.drawable.stroke_ne),
    Kana("の", "no", "な行", R.drawable.stroke_no),
    Kana("は", "ha", "は行", R.drawable.stroke_ha),
    Kana("ひ", "hi", "は行", R.drawable.stroke_hi),
    Kana("ふ", "fu", "は行", R.drawable.stroke_fu),
    Kana("へ", "he", "は行", R.drawable.stroke_he),
    Kana("ほ", "ho", "は行", R.drawable.stroke_ho),
    Kana("ま", "ma", "ま行", R.drawable.stroke_ma),
    Kana("み", "mi", "ま行", R.drawable.stroke_mi),
    Kana("む", "mu", "ま行", R.drawable.stroke_mu),
    Kana("め", "me", "ま行", R.drawable.stroke_me),
    Kana("も", "mo", "ま行", R.drawable.stroke_mo),
    Kana("や", "ya", "や行", R.drawable.stroke_ya),
    Kana("ゆ", "yu", "や行", R.drawable.stroke_yu),
    Kana("よ", "yo", "や行", R.drawable.stroke_yo),
    Kana("ら", "ra", "ら行", R.drawable.stroke_ra),
    Kana("り", "ri", "ら行", R.drawable.stroke_ri),
    Kana("る", "ru", "ら行", R.drawable.stroke_ru),
    Kana("れ", "re", "ら行", R.drawable.stroke_re),
    Kana("ろ", "ro", "ら行", R.drawable.stroke_ro),
    Kana("わ", "wa", "わ行", R.drawable.stroke_wa),
    Kana("を", "wo", "わ行", R.drawable.stroke_wo),
    Kana("ん", "n", "ん行", R.drawable.stroke_n),
)
```

Add to imports at top of file:

```kotlin
import com.kongkongyzt.gojuon.R
```

- [ ] **Step 2: Build to verify all R.drawable.stroke_* exist**

```bash
./gradlew assembleDebug
```

Expected: BUILD SUCCESSFUL. (If any `R.drawable.stroke_xxx` is unresolved, the matching file is missing — re-check Phase 6 output.)

- [ ] **Step 3: Commit**

```bash
git add app/src/main/java/com/kongkongyzt/gojuon/data/Kana.kt
git commit -m "$(cat <<'EOF'
feat: link Kana entries to their stroke AVD resources

Each of the 46 Kana entries now references its matching
R.drawable.stroke_<romaji> AVD generated in Phase 6.

Co-Authored-By: Claude Opus 4.7 (1M context) <noreply@anthropic.com>
EOF
)"
```

### Task 7.2: StrokeAnimator composable

**Files:**
- Create: `app/src/main/java/com/kongkongyzt/gojuon/ui/StrokeAnimator.kt`

- [ ] **Step 1: Create StrokeAnimator.kt**

Create file `app/src/main/java/com/kongkongyzt/gojuon/ui/StrokeAnimator.kt`:

```kotlin
package com.kongkongyzt.gojuon.ui

import android.widget.ImageView
import androidx.annotation.DrawableRes
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import androidx.vectordrawable.graphics.drawable.AnimatedVectorDrawableCompat

private class ImageViewRef { var view: ImageView? = null }

/**
 * 渲染并播放一次 AVD 笔顺动画。
 *
 * 调用方约定:此 Composable **只在需要播放时才组合进来**。
 * - `playToken == 0` → 调用方应该渲染静态 `Text(kana.char)`,**不**调用此 Composable。
 * - `playToken > 0` → 渲染此 Composable;每次 `playToken` 增量都会重置并重播一次。
 *   父组件因其它原因(例如开关重组)的 recomposition **不会**误触发重播 ——
 *   播放由 `LaunchedEffect(playToken, drawableRes)` 驱动,只在 key 变化时执行。
 */
@Composable
fun StrokeAnimator(
    @DrawableRes drawableRes: Int,
    playToken: Int,
    modifier: Modifier = Modifier,
) {
    if (drawableRes == 0 || playToken == 0) return
    val ref = remember { ImageViewRef() }

    AndroidView(
        factory = { ctx -> ImageView(ctx).also { ref.view = it } },
        modifier = modifier,
    )

    LaunchedEffect(playToken, drawableRes) {
        val iv = ref.view ?: return@LaunchedEffect
        val avd = AnimatedVectorDrawableCompat.create(iv.context, drawableRes)
            ?: return@LaunchedEffect
        iv.setImageDrawable(avd)
        avd.start()
    }
}
```

CardScreen wires this in Task 7.3 with the `playToken == 0 → Text` branch.

- [ ] **Step 2: Build to verify**

```bash
./gradlew assembleDebug
```

Expected: BUILD SUCCESSFUL.

- [ ] **Step 3: Commit**

```bash
git add app/src/main/java/com/kongkongyzt/gojuon/ui/StrokeAnimator.kt
git commit -m "$(cat <<'EOF'
feat: StrokeAnimator composable wrapping AVD playback

AndroidView wraps ImageView holding an AnimatedVectorDrawableCompat.
Caller drives playback via playToken (0 = no display, >0 = play once).

Co-Authored-By: Claude Opus 4.7 (1M context) <noreply@anthropic.com>
EOF
)"
```

### Task 7.3: Integrate StrokeAnimator into CardScreen

**Files:**
- Modify: `app/src/main/java/com/kongkongyzt/gojuon/ui/CardScreen.kt`

- [ ] **Step 1: Add stroke playback state per card**

Edit `CardScreen.kt`:

1. Imports (add to top with others):

```kotlin
import androidx.compose.foundation.layout.size
```

2. In `CardContent`, change signature and large-character rendering. Replace the entire `CardContent` definition with:

```kotlin
@Composable
private fun CardContent(
    kana: Kana,
    showRomaji: Boolean,
    strokePlayToken: Int,
    onTapKana: () -> Unit,
    onTapStroke: () -> Unit,
) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // 大假名 / 笔顺动画区域(同一位置)
            Box(
                modifier = Modifier.size(260.dp),
                contentAlignment = Alignment.Center
            ) {
                if (strokePlayToken > 0) {
                    StrokeAnimator(
                        drawableRes = kana.drawableRes,
                        playToken = strokePlayToken,
                        modifier = Modifier.size(220.dp)
                    )
                } else {
                    Text(
                        text = kana.char,
                        fontSize = 220.sp,
                        fontWeight = FontWeight.Normal,
                        modifier = Modifier.clickable { onTapKana() }
                    )
                }
            }
            Spacer(Modifier.height(8.dp))
            if (showRomaji) {
                Text(text = kana.romaji, fontSize = 36.sp)
            } else {
                Spacer(Modifier.height(36.dp))
            }
            Spacer(Modifier.height(48.dp))
            OutlinedButton(onClick = onTapStroke) {
                Text(androidx.compose.ui.res.stringResource(R.string.btn_stroke))
            }
            Spacer(Modifier.height(8.dp))
            Text(text = kana.row, fontSize = 14.sp)
        }
    }
}
```

3. In `CardScreen`, just before the `Scaffold(...)` call, add per-card stroke state. Each card's playToken is keyed by the real kana index. Use a `mutableStateMapOf`:

After the line `val tts = rememberJapaneseTts()`, add:

```kotlin
// 每个 kana 一个 playToken。点笔顺按钮 → ++,触发 StrokeAnimator 重新创建并播放。
// 切换卡片不会清零(用户回到那张时 token 仍 > 0,会显示终态)。
// 简化:不 saveable,杀进程后所有 playToken 归零,卡片回到静态文字 — 这是期望行为(切卡片相当于重置)。
val strokeTokens = remember { mutableStateMapOf<Int, Int>() }
```

Add to imports:

```kotlin
import androidx.compose.runtime.mutableStateMapOf
```

4. In the `HorizontalPager { virtualPage -> ... }`, change the existing `CardContent(...)` call to:

```kotlin
val token = strokeTokens[kanaIndex] ?: 0
CardContent(
    kana = GOJUON[kanaIndex],
    showRomaji = showRomaji,
    strokePlayToken = token,
    onTapKana = { tts.speak(GOJUON[kanaIndex].char) },
    onTapStroke = { strokeTokens[kanaIndex] = (strokeTokens[kanaIndex] ?: 0) + 1 },
)
```

5. Reset stroke state when leaving the card. Add a `LaunchedEffect` inside the pager block (still inside `HorizontalPager`'s lambda but the simplest approach is to reset all tokens when the user moves away from a card):

Better approach — keep token only for the **currently visible** card. Right after the existing `LaunchedEffect(pagerState, orderedIndices) { snapshotFlow ... }` block, add a second `LaunchedEffect`:

```kotlin
// 每次切换卡片(currentRealIndex 变),把所有非当前卡片的 token 清零(下次访问时回到静态)
LaunchedEffect(currentRealIndex) {
    val keep = currentRealIndex
    val toRemove = strokeTokens.keys.filter { it != keep }
    toRemove.forEach { strokeTokens.remove(it) }
}
```

- [ ] **Step 2: Build + install + launch**

```bash
./gradlew installDebug && adb shell monkey -p com.kongkongyzt.gojuon -c android.intent.category.LAUNCHER 1
```

- [ ] **Step 3: Manual verification**

- [ ] 默认显示静态「あ」;点「笔顺」按钮 → 「あ」位置变成空白 → 逐笔(3 笔)绘制 → 完成后停在终态字形(全部笔画显示)
- [ ] 再点「笔顺」→ 重新从空白开始播放
- [ ] 点笔顺播完后切到「い」→ 「い」是静态 Text,**不是**残留的「あ」终态
- [ ] 切回「あ」→ 也回到静态 Text(playToken 已被清零)
- [ ] 在「ち」点笔顺 → 多笔字(3 笔)依次绘制
- [ ] 抽 3-5 个其它字测试,确认 AVD 都能正确播放
- [ ] 旋转屏幕 → 当前页保持,笔顺状态会重置(可接受 — 旋转不常发生)

如果某个字笔顺动画显示乱七八糟(线条歪/颜色异常):大概率是 KanjiVG SVG path 在我们的 viewport(109)下被 stroke-width 3 渲染太粗,可在 `kanjivg_to_avd.py` 调小 `STROKE_WIDTH` 后重跑 + 重新 build。

- [ ] **Step 4: Commit**

```bash
git add app/src/main/java/com/kongkongyzt/gojuon/ui/CardScreen.kt
git commit -m "$(cat <<'EOF'
feat: integrate stroke animation into card

Per-kana playToken in mutableStateMapOf; tapping 笔顺 increments,
StrokeAnimator picks it up and replays. Tokens for non-current
cards are cleared on swipe so they return to static state.

Co-Authored-By: Claude Opus 4.7 (1M context) <noreply@anthropic.com>
EOF
)"
```

---

## Phase 8: Final Verification + Polish

### Task 8.1: Run full §10 acceptance checklist on device

**Files:** None (verification)

- [ ] **Step 1: Clean install**

```bash
cd /Users/kongkongyzt/AndroidProjects/GojuonCards
adb uninstall com.kongkongyzt.gojuon || true
./gradlew installDebug
adb shell monkey -p com.kongkongyzt.gojuon -c android.intent.category.LAUNCHER 1
```

- [ ] **Step 2: Walk the spec §10 checklist**

Tick each (each can be a Linus-style "yes / no"; if any "no", file follow-up):

- [ ] App 启动,首屏显示「あ」,罗马字默认显示「a」
- [ ] 左右滑动 + 上一/下一按钮都能切换;顺序为 あいうえお → かきくけこ → … → ん
- [ ] 在第 1 张点"上一个" → 跳到第 46 张「ん」;在第 46 张点"下一个" → 回到「あ」
- [ ] 点大假名 → 听到日语发音(あ → /a/)
- [ ] 关掉「罗马字」开关 → 罗马字消失;再开 → 重新显示
- [ ] 开启「随机」开关 → 卡片顺序洗牌;关闭 → 回到标准顺序;两次切换都保持当前假名
- [ ] 点「笔顺」按钮 → 「あ」按 3 笔的顺序逐笔绘制完成;再点 → 重播
- [ ] 切到下一张卡片 → 笔顺动画区恢复为静态假名(不残留前一字的动画状态)
- [ ] 旋转屏幕 / 杀掉进程重开 → 页码、罗马字开关、随机开关都恢复
- [ ] 切换深色模式 → 主题颜色正确切换,蓝色保持识别度
  - 切深色: `adb shell "cmd uimode night yes"`
  - 切回浅色: `adb shell "cmd uimode night no"`
- [ ] 全 46 个字逐一过一遍发音,确认 KanjiVG 数据齐全(笔顺动画无缺字)

### Task 8.2: KanjiVG attribution Toast on long-press TopAppBar

**Files:**
- Modify: `app/src/main/java/com/kongkongyzt/gojuon/ui/CardScreen.kt`
- Modify: `app/src/main/res/values/strings.xml`

- [ ] **Step 1: Add attribution string**

Append inside `<resources>` in `app/src/main/res/values/strings.xml`:

```xml
    <string name="attribution">Stroke data © KanjiVG (CC-BY-SA-3.0)</string>
```

- [ ] **Step 2: Add long-press attribution Toast on TopAppBar title**

In `CardScreen.kt`, find the `TopAppBar(...)` block. Replace its `title = { Text(text = "日语五十音") }` parameter with:

```kotlin
title = {
    val ctx = androidx.compose.ui.platform.LocalContext.current
    val attrText = androidx.compose.ui.res.stringResource(R.string.attribution)
    Text(
        text = "日语五十音",
        modifier = Modifier.combinedClickable(
            onClick = {},
            onLongClick = {
                android.widget.Toast.makeText(ctx, attrText, android.widget.Toast.LENGTH_LONG).show()
            }
        )
    )
},
```

Add imports:

```kotlin
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
```

Add `@OptIn(ExperimentalFoundationApi::class)` to the existing `@OptIn(ExperimentalMaterial3Api::class)` line at the top of `CardScreen()`:

```kotlin
@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun CardScreen() {
    ...
```

- [ ] **Step 3: Build + install + verify**

```bash
./gradlew installDebug && adb shell monkey -p com.kongkongyzt.gojuon -c android.intent.category.LAUNCHER 1
```

On device: long-press the title "日语五十音" → Toast shows "Stroke data © KanjiVG (CC-BY-SA-3.0)".

- [ ] **Step 4: Commit**

```bash
git add app/src/main/res/values/strings.xml app/src/main/java/com/kongkongyzt/gojuon/ui/CardScreen.kt
git commit -m "$(cat <<'EOF'
chore: KanjiVG CC-BY-SA-3.0 attribution via long-press toast

Discoverable but unobtrusive — long-pressing the AppBar title
shows credits. Required by KanjiVG license.

Co-Authored-By: Claude Opus 4.7 (1M context) <noreply@anthropic.com>
EOF
)"
```

### Task 8.3: Tag v0.1 release

- [ ] **Step 1: Tag**

```bash
cd /Users/kongkongyzt/AndroidProjects/GojuonCards
git log --oneline | head -20
git tag -a v0.1 -m "v0.1: 五十音 App MVP — 46 hiragana cards, swipe nav, romaji + random toggles, TTS, stroke animation"
git tag -l
```

- [ ] **Step 2: Locate the final APK**

```bash
ls -lh app/build/outputs/apk/debug/app-debug.apk
```

This is the file you can `adb install -r` to any other Android device, or copy to your phone for permanent install.

---

## Done

The app is installed on `c19636e1` (Redmi marble) and tagged v0.1 in git.

Future expansion (out of scope for this plan): 浊音/半浊音/拗音, 片假名, 进度跟踪, 测验模式 — all are orthogonal additions building on this same architecture.
