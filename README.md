# AircraftWar（Android）

把 Windows / Java Swing 单机版**飞机大战**迁移到 Android。玩法逻辑从桌面版平移，
渲染、输入、资源、音频、存储五个平台边界重写。

- **迁移源**：`D:\unusual\软件构造实验\实验一\实验一\AircraftWar-base`，commit **`50474cc`**（只读参照，不在该仓库提交）
- 客户端工程：本仓库（独立仓库，不与迁移源共享历史）
- 课程文档与阶段计划：`D:\HuaweiMoveData\Users\20805\Desktop\homework\软件构造实践\AGENTS.md`

## 环境

| 项 | 值 |
|---|---|
| Android Studio | 2026.2.2（自带 JBR 25，Gradle JDK 选 Embedded） |
| AGP / Gradle | 9.5.1 / 9.9.0（wrapper 已锁定，不要手改） |
| SDK | `D:\Android\Sdk`，compileSdk / targetSdk 37，minSdk 24 |
| Java 源级 | 17 |

## 一页跑起来

```powershell
cd D:\dev\AircraftWar

# 编译
.\gradlew.bat :app:assembleDebug

# 逻辑层单测（纯 JVM，不需要模拟器）
.\gradlew.bat :app:testDebugUnitTest

# 安装到已启动的模拟器/真机
& D:\Android\Sdk\platform-tools\adb.exe install -r app\build\outputs\apk\debug\app-debug.apk
& D:\Android\Sdk\platform-tools\adb.exe shell am start -n edu.hitsz.aircraftwar/.ui.MainActivity
```

无头启动模拟器（开发机内存紧张时用）：

```powershell
& D:\Android\Sdk\emulator\emulator.exe -avd Medium_Phone_API_37.0 -no-window -gpu host -no-audio
```

## 代码地图

```
app/src/main/java/edu/hitsz/aircraftwar/
├── core/       纯 Java 逻辑，禁 android.*（可跑 JVM 单测）
│   ├── config/GameConfig.java   唯一常量源：512×768、40ms、19 张精灵尺寸、分数表
│   ├── basic/ aircraft/ bullet/ drop/ strategy/ observer/ record/   ← 迁移来的玩法规则
│   └── logic/  GameLogic / Simple|Normal|HardGame / PlayerLevelManager / GameEvents
├── platform/   唯一允许碰 Android API 的层
│   ├── render/ GameView(SurfaceView+循环线程) / GameRenderer
│   ├── input/  TouchController
│   └── resource/ImageStore
└── ui/         MainActivity（S1：全屏游戏页；导航在 S2）
```

依赖方向单向：`ui → platform → core`。`core` 不依赖任何其他包。

## 当前进度（S1，实验二前半）

已完成：逻辑层全量平移、`GameConfig` 抽常量、SurfaceView 渲染线程、触摸拖动、碰撞计分、
JVM 单测 17 条。AVD 实跑截图见课程仓库 `docs/evidence/s1-game.png`、`s1-gameover.png`。

S1 未做（排 S2）：主菜单/难度选择/结算页三页导航、音效与 BGM、暂停。

## 已知限制

- 模拟器（软件渲染）实测约 **21.5fps**，逻辑固定 25Hz（掉帧时追帧）；真机预期更高，S6 用真机复测。
- 资源名 `elitePlus` / `elitePro` / `prop_bulletPlus` 因 Android 不允许大写，落位时改为 `elite_plus` / `elite_pro` / `prop_bullet_plus`。
- `core/record/GameRecordDaoImpl` 未迁：S3 直接用 Room 实现，接口与实体不变。

## 提交规范

`<type>(<scope>): <中文描述>`，scope 取 `core | platform | ui | data | network | server | docs | build`。
迁移类提交在正文注明来源 `50474cc`。详见课程仓库 `AGENTS.md` §11。
