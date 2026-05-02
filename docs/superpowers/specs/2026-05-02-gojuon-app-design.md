# 日语五十音 - 设计文档

**日期**: 2026-05-02
**目标平台**: Android(原生 Kotlin + Jetpack Compose)
**目标用户**: 个人(用户本人在学日语五十音)
**产物**: 可 `adb install` 的 `.apk`

---

## 1. 一句话描述

一个**单页**的安卓 App,展示一张五十音卡片,左右滑动或按钮切换,点击发音,可触发笔顺动画;顶部两个开关控制"是否显示罗马字"和"是否随机顺序"。

## 2. 目标 / 非目标

### 2.1 目标(In Scope)
- 展示 46 个清音平假名(あ-ん)
- 卡片轮播:左右滑动 + 上一/下一按钮 + 首尾循环
- 假名下方显示罗马字,可一键隐藏(自测模式)
- 五十音表标准顺序(あいうえお → かきくけこ → ...),可一键切换为随机顺序
- 点击卡片发音(Android 内置 TTS,日语)
- 笔顺动画:点"笔顺"按钮,大假名变为逐笔绘制的动画
- 蓝色调主题(Material 3, seed `#1E88E5`),自动支持系统深浅色
- App 名:**日语五十音**;包名:`com.kongkongyzt.gojuon`

### 2.2 非目标(Out of Scope, YAGNI)
- 浊音 / 半浊音 / 拗音(以后扩,只需追加数据 + 笔顺资源)
- 片假名(以后扩,加"假名集"切换)
- 进度跟踪 / "已掌握"标记 / 复习算法(SRS)
- 自动连续播放 / 跟读评分
- 用户账号 / 云同步 / 设置页(顶部两个开关已是全部"设置")
- 单元/UI 自动化测试 —— 代码量 <500 行,真机肉眼验证更高效

## 3. 界面设计

### 3.1 唯一界面布局

```
┌──────────────────────────────┐
│  日语五十音    [罗马字 ●] [随机 ○]│   ← TopAppBar(蓝色)
├──────────────────────────────┤
│                              │
│            あ                 │   ← 大假名(屏宽 ~50%,占垂直 ~40%)
│            a                 │   ← 罗马字(开关控制可见)
│                              │     整张大假名区域可点 → 发音
│                              │
│         [ ✎ 笔顺 ]            │   ← 笔顺按钮(Outlined)
│           あ行                │   ← 行号小字
│                              │
├──────────────────────────────┤
│   [ ← 上一个 ]    [ 下一个 → ]  │   ← 翻页按钮(Filled,蓝色)
│         3 / 46                │   ← 位置指示
└──────────────────────────────┘
```

### 3.2 交互规则

| 操作 | 行为 |
|---|---|
| **左右滑动**卡片区域 | 上一张 / 下一张(`HorizontalPager`) |
| **点底部按钮** | 上一张 / 下一张(可访问性兜底) |
| **点大假名区域** | 触发 TTS 发音 |
| **点"笔顺"按钮** | 大假名替换为 AVD 动画,逐笔绘制,完成后停在终态;再点 = 重播 |
| **切换到新卡片** | 自动复位:笔顺动画恢复为静态假名;不自动发音 |
| **点顶部"罗马字"开关** | 切换罗马字可见性 |
| **点顶部"随机"开关** | 重新洗牌卡片顺序;关闭时回到标准顺序;**当前页不变(尽量保持当前假名)** |
| **首尾循环** | 第 46 张点"下一个" → 回到第 1 张;第 1 张点"上一个" → 跳到第 46 张 |

### 3.3 主题
- Material 3,种子色 `#1E88E5`
- 通过 `dynamicColor = false`(关闭 Android 12+ Material You 取色),保证蓝色调始终生效
- 自动跟随系统深浅色模式

## 4. 数据

### 4.1 五十音表

```kotlin
data class Kana(
    val char: String,    // "あ"
    val romaji: String,  // "a"
    val row: String,     // "あ行"
    val drawableRes: Int // R.drawable.stroke_a
)
```

硬编码 46 项 `List<Kana>`:あいうえお / かきくけこ / さしすせそ / たちつてと / なにぬねの / はひふへほ / まみむめも / やゆよ / らりるれろ / わを / ん。

行号约定:や行只有 や/ゆ/よ;わ行只有 わ/を;ん 单独标"ん行"。

### 4.2 笔顺数据来源
- **KanjiVG**(https://kanjivg.tagaini.net/, GitHub: KanjiVG/kanjivg),CC-BY-SA-3.0
- 平假名 SVG 文件名为 Unicode codepoint,如 あ = U+3042 = `03042.svg`
- 全部 46 个文件预计总计 < 200 KB
- App 内放一行 attribution(显示位置:Toast 或 long-press 顶栏弹出"关于"),内容:`Stroke data © KanjiVG (CC-BY-SA-3.0)`

## 5. 笔顺动画子系统

### 5.1 转换流程(开发期一次性)

```
KanjiVG SVG (46 个)
    │
    ▼
scripts/kanjivg_to_avd.py  ← Python 脚本,开发机本地跑
    │
    ▼
app/src/main/res/drawable/stroke_*.xml (46 个 AnimatedVectorDrawable)
```

### 5.2 AVD XML 结构(每个文件)
- 一个 `<vector>` 定义所有笔画 path,初始 `trimPathStart="0"` `trimPathEnd="0"`
- 一个或多个 `<target>` + `<set>` `<objectAnimator>`,按笔顺 sequentially 播放,每笔 ~400ms,把 `trimPathEnd` 从 0 → 1
- 全部 stroke 总动画时长 = 笔数 × 400ms(单笔字 ~400ms,4 笔字 ~1.6s,可接受)

### 5.3 运行时

```kotlin
// ui/StrokeAnimator.kt
@Composable
fun StrokeAnimator(@DrawableRes resId: Int, playToken: Int, modifier: Modifier) {
    AndroidView(
        factory = { ctx -> ImageView(ctx) },
        update = { iv ->
            val avd = AnimatedVectorDrawableCompat.create(iv.context, resId)!!
            iv.setImageDrawable(avd)
            avd.start()
        },
        modifier = modifier
    )
    // playToken 变化 = 重播触发器(用户点笔顺按钮)
}
```

切换卡片时,`playToken` 重置为 0(不播放,只显示静态终态)。点"笔顺"按钮 → `playToken++` → AVD 重新创建 + start。

## 6. 发音子系统

### 6.1 引擎
- `android.speech.tts.TextToSpeech`,语种 `Locale.JAPANESE`
- 在 Application 级或顶层 Composable `remember` 单例,生命周期跟 Activity 绑定
- onDestroy 时 `tts.stop()` + `tts.shutdown()`

### 6.2 失败兜底
- 初始化时检查 `tts.isLanguageAvailable(Locale.JAPANESE)`:
  - `LANG_AVAILABLE` 及以上 → 正常
  - `LANG_MISSING_DATA` 或 `LANG_NOT_SUPPORTED` → 用 Toast 提示"请在系统设置 → 文字转语音中安装日语语音包",**不**自动跳转
- 用户那台 Redmi marble 一般预装 Google TTS + 日语数据,默认能用

## 7. 状态管理

| 状态 | 类型 | 持久化 | 范围 |
|---|---|---|---|
| 当前页码 | `MutableState<Int>` | `rememberSaveable` | 杀进程恢复 |
| 罗马字可见 | `MutableState<Boolean>`,默认 true | `rememberSaveable` | 杀进程恢复 |
| 随机顺序模式 | `MutableState<Boolean>`,默认 false | `rememberSaveable` | 杀进程恢复 |
| 随机洗牌结果 | `MutableState<List<Int>>` | `rememberSaveable` | 杀进程恢复;切换"随机"开关时重新生成 |
| 笔顺播放 token | `MutableState<Int>`,默认 0 | 不持久化 | 切换卡片时重置为 0 |
| TTS 实例 | `remember` | 不持久化 | Activity scope |

不引入 DataStore / SharedPreferences / Room。

## 8. 项目结构

```
~/AndroidProjects/GojuonCards/
├── settings.gradle.kts
├── build.gradle.kts                 (root, AGP 8.x + Kotlin 1.9+)
├── gradle.properties
├── gradle/wrapper/
│   ├── gradle-wrapper.jar
│   └── gradle-wrapper.properties
├── gradlew, gradlew.bat
├── docs/
│   └── superpowers/
│       └── specs/
│           └── 2026-05-02-gojuon-app-design.md   ← 本文件
├── scripts/
│   └── kanjivg_to_avd.py            (一次性运行,转换笔顺数据)
└── app/
    ├── build.gradle.kts             (compileSdk 34, minSdk 24, targetSdk 34)
    ├── proguard-rules.pro
    └── src/main/
        ├── AndroidManifest.xml
        ├── java/com/kongkongyzt/gojuon/
        │   ├── MainActivity.kt              (入口,setContent)
        │   ├── data/
        │   │   └── Kana.kt                  (data class + 五十音表)
        │   ├── tts/
        │   │   └── JapaneseTts.kt           (TTS 封装 + 失败处理)
        │   ├── ui/
        │   │   ├── CardScreen.kt            (主界面 Composable)
        │   │   ├── StrokeAnimator.kt        (AVD 动画播放)
        │   │   └── theme/
        │   │       ├── Color.kt             (#1E88E5 配色)
        │   │       ├── Theme.kt             (Material 3 主题)
        │   │       └── Type.kt              (字体)
        └── res/
            ├── drawable/
            │   ├── stroke_a.xml             (...46 个 AVD)
            │   └── ic_launcher_*.xml        (App 图标,Material You 风格蓝)
            ├── values/
            │   └── strings.xml              (App 名"日语五十音" + UI 文案)
            └── mipmap-*/                    (启动图标 PNG)
```

## 9. 构建 / 安装流程

### 9.1 开发机环境(已就位)
- JDK 17 (Temurin) `/Library/Java/JavaVirtualMachines/temurin-17.jdk`
- Android Studio (含 SDK Manager / IDE / Layout Inspector)
- Android SDK platform 34 + build-tools 34.0.0(经 brew cmdline-tools 已装)
- adb 36.0.2(通过 brew android-platform-tools)
- 真机:Redmi `marble` (Note 12 Turbo / Poco F5),USB 已连接,序列号 `c19636e1`

### 9.2 命令
```bash
cd ~/AndroidProjects/GojuonCards

# 准备笔顺数据(仅首次/数据更新时跑)
python3 scripts/kanjivg_to_avd.py

# Debug build + 安装到真机
./gradlew installDebug

# 或拿到 APK 文件
./gradlew assembleDebug
# → app/build/outputs/apk/debug/app-debug.apk
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

### 9.3 环境变量
需要在 `~/.zshrc` 加:
```bash
export ANDROID_HOME=/opt/homebrew/share/android-commandlinetools
export PATH=$ANDROID_HOME/platform-tools:$ANDROID_HOME/cmdline-tools/latest/bin:$PATH
export JAVA_HOME=/Library/Java/JavaVirtualMachines/temurin-17.jdk/Contents/Home
```

## 10. 测试策略

不写自动化测试。验收方式:`adb install` 装到真机,人肉过一遍 checklist:

- [ ] App 启动,首屏显示「あ」,罗马字默认显示「a」
- [ ] 左右滑动 + 上一/下一按钮都能切换,顺序为 あいうえお → かきくけこ → ... → ん
- [ ] 在第 1 张点"上一个"跳到第 46 张「ん」;在第 46 张点"下一个"回到「あ」
- [ ] 点大假名 → 听到日语发音(あ → /a/)
- [ ] 关掉"罗马字"开关 → 罗马字消失;再开 → 重新显示
- [ ] 开启"随机"开关 → 卡片顺序洗牌;关闭 → 回到标准顺序
- [ ] 点"笔顺"按钮 → 「あ」按 3 笔的顺序逐笔绘制完成;再点 → 重播
- [ ] 切到下一张卡片 → 笔顺动画区恢复为静态假名(不残留前一字的动画状态)
- [ ] 旋转屏幕 / 杀掉进程重开 → 页码、罗马字开关、随机开关都恢复
- [ ] 切换深色模式 → 主题颜色正确切换,蓝色保持识别度
- [ ] 全 46 个字逐一过一遍发音,确认 KanjiVG 数据齐全(笔顺动画无缺字)

## 11. 风险 / 未决项

| 风险 | 缓解 |
|---|---|
| MIUI/HyperOS 系统对后台 TTS 限制 | TTS 在前台 Activity 中调用,不存在后台问题;失败给 Toast |
| KanjiVG 个别字 SVG path 解析异常导致 AVD 生成失败 | 转换脚本对每个字 try/except,失败的字记录到日志,App 端缺失时 fallback 到静态假名(笔顺按钮置灰) |
| Compose `HorizontalPager` 边界翻页(首尾循环)需要自定义实现 | 用 `Int.MAX_VALUE` 模拟无限 pager + 模运算映射到实际 index;社区标准做法 |
| `dynamicColor = false` 可能导致部分用户的 Material You 偏好失效 | 这是有意为之 —— 蓝色是产品身份,优先级高于动态取色 |

## 12. 实施分阶段

| 阶段 | 内容 | 依赖 | 预计 |
|---|---|---|---|
| **0. 环境** | 设置 ANDROID_HOME / JAVA_HOME;打开一次 AS 走完 Welcome wizard | (已基本就位) | 15 min |
| **1. 脚手架** | 用 AS 新建 Empty Compose Activity 项目;配 Material 3 蓝主题 | 0 | 30 min |
| **2. 数据 + 静态界面** | Kana 数据;CardScreen 显示假名 + 罗马字 + 行号;HorizontalPager 滑动 + 按钮 + 首尾循环 | 1 | 1 h |
| **3. 顶部开关** | 罗马字开关 + 随机开关;rememberSaveable 持久化 | 2 | 30 min |
| **4. TTS 发音** | JapaneseTts 封装;点击大假名 → 发音;失败 Toast | 2 | 30 min |
| **5. 笔顺数据** | 跑 `kanjivg_to_avd.py`,生成 46 个 AVD | 1 | 1 h(主要在脚本调试) |
| **6. 笔顺动画** | StrokeAnimator Composable;笔顺按钮 + playToken 触发 | 5 | 1.5 h |
| **7. 真机验收** | `adb install`,跑 §10 checklist | 全部 | 30 min |
| **总计** | | | **~6 h** |

每阶段完成都对应一次 git commit。
