# 日语五十音 (Gojuon Cards)

A native Android app for studying Japanese hiragana — 46 清音 cards with swipe navigation, native pronunciation, and KanjiVG-based stroke order animations.

<p align="center">
  <a href="https://github.com/stormeyes/gojuon-cards/releases/latest">
    <img src="https://img.shields.io/github/v/release/stormeyes/gojuon-cards?label=Download%20APK&color=1E88E5" alt="Latest release">
  </a>
  <img src="https://img.shields.io/badge/min%20SDK-24-1E88E5" alt="minSdk 24">
  <img src="https://img.shields.io/badge/Kotlin-2.2-7F52FF" alt="Kotlin">
  <img src="https://img.shields.io/badge/Compose-Material%203-1E88E5" alt="Compose Material 3">
</p>

## 功能 / Features

- 46 张平假名清音卡片(あ-ん),按五十音表标准顺序
- 左右滑动 / 按钮翻页;首尾循环
- **罗马字开关** —— 自测时一键隐藏
- **随机顺序开关** —— 每次开启重新洗牌,切换时保持当前假名
- **点击大假名 → 发音**(macOS Kyoko 母语女声预录,本地 .m4a 文件,不依赖系统 TTS)
- **「笔顺」按钮 → 笔画动画**(KanjiVG 数据,逐笔绘制,400ms / 笔)
- 静态字形 + 笔顺动画**使用同一套 KanjiVG 路径**,视觉风格完全一致
- Material 3 蓝色主题,自动跟随系统深浅色;笔画颜色自动适配亮/暗背景
- 锁定竖屏

## 下载 / Download

最新版 APK 在 [Releases](https://github.com/stormeyes/gojuon-cards/releases) 页面。

```bash
adb install gojuon-cards-v0.1.apk
```

或从手机浏览器直接下载 → 文件管理器中点击安装(系统会问"是否允许安装未知来源"→ 允许)。

最低系统要求:Android 7.0 (API 24)。

## 技术栈

- **Kotlin** 2.2 + **Jetpack Compose** Material 3
- **AGP** 9.x,Gradle Kotlin DSL
- **Audio**: 46 个 .m4a 文件(macOS `say -v Kyoko` 生成),`MediaPlayer` 播放,运行时 `LoudnessEnhancer` +15dB 增益
- **Stroke animations**: 46 个 `AnimatedVectorDrawable` (从 KanjiVG SVG 转换),`trimPathEnd` 0→1 逐笔动画
- **State**: `rememberSaveable` 持久化(页码、罗马字 / 随机开关跨配置变化保留)
- 真机验证驱动开发(无自动化测试,详见 [设计文档](docs/superpowers/specs/2026-05-02-gojuon-app-design.md) §10)

## 从源码构建

```bash
# Debug build(无需签名)
./gradlew installDebug

# Release build(需要 keystore/keystore.properties,见下)
./gradlew assembleRelease
adb install -r app/build/outputs/apk/release/app-release.apk
```

### Release 签名配置

`app/build.gradle.kts` 的 release signingConfig 从 `keystore/keystore.properties` 读取(此目录在 `.gitignore` 内)。如需复现 release build,在仓库根目录创建:

```
keystore/release.jks                    # 自己生成的 keystore
keystore/keystore.properties            # 包含 storeFile / storePassword / keyAlias / keyPassword
```

如果 `keystore.properties` 不存在,`assembleRelease` 会自动 fallback 到 debug 签名(可装但是 debug-signed)。

## 重新生成数据

需要重新生成发音音频或笔顺动画:

```bash
# 音频(需要 macOS — 用 say + ffmpeg)
bash scripts/generate_kana_audio.sh

# 笔顺(任何平台,Python 3,会从 KanjiVG/master 下载 SVG)
bash scripts/download_kanjivg.sh
python3 scripts/kanjivg_to_avd.py
```

## 项目文档

- [设计文档](docs/superpowers/specs/2026-05-02-gojuon-app-design.md) — 需求、架构、UI、笔顺/TTS/audio 子系统、风险、实施偏差(§13)
- [实施 plan](docs/superpowers/plans/2026-05-02-gojuon-app.md) — 按 phase 拆分的 task,每个含完整代码 + 验收 checklist

## Attribution

- **笔顺数据** © [KanjiVG](https://kanjivg.tagaini.net/) by Ulrich Apel et al., 协议 [CC BY-SA 3.0](https://creativecommons.org/licenses/by-sa/3.0/)
- **发音音频** 由 macOS 系统语音 Kyoko 生成(Apple)
- 长按 App 内顶栏标题可弹 attribution toast

## License

MIT (see [LICENSE](LICENSE) — to be added).

笔顺数据派生自 KanjiVG,继承 CC-BY-SA-3.0 限制。

---

🤖 Built with [Claude Code](https://claude.com/claude-code).
