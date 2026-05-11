# Trime (同文输入法) 项目文档

## 项目概述

**Trime** 是一个基于 [RIME](https://rime.im) 输入法框架的 Android 平台输入法引擎（IME）。

- **项目类型**: Android 输入法应用
- **开发语言**: Kotlin、Java、C++ (通过 JNI)
- **许可证**: GPL-3.0-or-later
- **当前版本**: 3.3.7 (根据 CHANGELOG)
- **包名**: com.nirenr.trime (应用ID)

## 功能特性

### 核心功能
- **多语言支持**: 支持简体中文、繁体中文、方言输入
- **多种输入方案**: 拼音、五笔、二笔等形码和音码输入法
- **方言保护**: 专为保护中国各地方言而设计
- **通用输入平台**: 统一的形码和音码输入法平台
- **RIME 集成**: 通过 JNI 集成 librime 核心引擎

### 特色功能
- 液态键盘 (Liquid Keyboard)
- 剪贴板管理
- 主题定制
- 语音输入集成
- 候选词窗口自定义
- 横屏键盘适配
- 手势操作支持

## 技术架构

### 整体架构

```
Trime (Android IME)
├── Java/Kotlin 层 (UI & Android Framework)
│   ├── Input Method Service
│   ├── Keyboard UI Components
│   ├── Theme System
│   └── Data Management
└── Native 层 (JNI)
    ├── librime (核心输入法引擎)
    ├── OpenCC (繁简转换)
    ├── Lua 支持 (通过 librime-lua)
    └── 其他 C++ 依赖库
```

### 技术栈

| 层级 | 技术 |
|------|------|
| **构建系统** | Gradle 8.11.0 (Kotlin DSL) |
| **Android SDK** | Compile SDK 35, Min SDK 21, Target SDK 35 |
| **编程语言** | Kotlin 2.2.0, Java 11, C++ |
| **异步处理** | Kotlin Coroutines 1.10.2 |
| **序列化** | kotlinx-serialization-json 1.8.1 |
| **依赖注入** | kotlin-inject 0.8.0 (KSP) |
| **数据库** | Room 2.7.2 + Paging 3.3.5 |
| **UI 组件** | AndroidX, ConstraintLayout, RecyclerView, ViewPager2 |
| **其他库** | Timber (日志), Splitties, Iconics, Flexbox |

## 项目结构

```
trime2/
├── app/                          # 主应用模块
│   ├── src/main/
│   │   ├── java/com/osfans/trime/  # Kotlin/Java 源码
│   │   │   ├── candidate/         # 候选词相关
│   │   │   ├── core/             # 核心功能
│   │   │   ├── data/             # 数据管理
│   │   │   │   ├── opencc/       # 繁简转换
│   │   │   │   └── userdict/     # 用户词典
│   │   │   ├── dialog/           # 对话框
│   │   │   ├── enums/            # 枚举定义
│   │   │   ├── keyboard/         # 键盘相关
│   │   │   │   └── adapter/     # 适配器
│   │   │   ├── speech/           # 语音输入
│   │   │   ├── theme/            # 主题系统
│   │   │   └── util/             # 工具类
│   │   ├── jni/                  # JNI 原生代码
│   │   │   ├── librime/          # RIME 核心库
│   │   │   ├── librime-lua/      # Lua 支持
│   │   │   ├── librime-octagram/ # Octagram 插件
│   │   │   ├── librime-predict/  # 预测插件
│   │   │   ├── OpenCC/           # 繁简转换库
│   │   │   ├── snappy/           # 压缩库
│   │   │   └── boost/            # Boost C++ 库
│   │   ├── res/                  # Android 资源
│   │   ├── assets/               # 资产文件
│   │   └── AndroidManifest.xml
│   ├── build.gradle.kts          # 模块构建配置
│   └── proguard-rules.pro        # 混淆规则
├── codegen/                      # 代码生成模块 (KSP)
├── build-logic/                  # 自定义 Gradle 插件
├── doc/                          # 项目文档
│   ├── Keyboard.md               # 键盘参数说明
│   └── trime-schema.json         # 配置 schema
├── script/                       # 脚本文件
├── fastlane/                     # Fastlane 配置
├── openspec/                     # OpenSpec 相关
├── build.gradle.kts              # 根构建配置
├── settings.gradle.kts           # 项目设置
├── gradle.properties             # Gradle 属性
├── gradle/libs.versions.toml    # 依赖版本目录
├── README.md                     # 项目说明 (英文)
├── README_sc.md                  # 项目说明 (简体中文)
├── README_tc.md                  # 项目说明 (繁体中文)
├── CONTRIBUTING.md               # 贡献指南
├── CODE_OF_CONDUCT.md            # 行为准则
├── CHANGELOG.md                  # 变更日志
├── LICENSE                       # GPL-3.0 许可证
└── Makefile                      # Make 构建脚本
```

## 核心模块说明

### 1. 输入法服务层
- 实现 Android InputMethodService
- 处理键盘事件和文本输入
- 管理输入会话和候选词

### 2. RIME 集成层 (JNI)
- **librime**: RIME 核心引擎，处理输入法逻辑
- **librime-jni**: Java Native Interface 桥接
- **librime-lua**: 支持 Lua 脚本扩展
- **librime-octagram**: 八码输入支持
- **librime-predict**: 输入预测功能

### 3. 数据处理层
- **OpenCC**: 繁体简体转换
- **用户词典**: 用户自定义词库管理
- **配置系统**: YAML 格式的配置文件解析

### 4. UI 层
- **键盘视图**: 自定义键盘布局
- **候选词窗口**: 候选词展示
- **主题系统**: 可定制的界面主题
- **液态键盘**: 符号、表情等扩展键盘

## 开发环境准备

### 系统要求
- **操作系统**: Windows/macOS/Linux
- **Android SDK**: API 35 (Android 15)
- **Android NDK**: 用于编译原生代码
- **JDK**: OpenJDK 17
- **Python 3**: 用于 OpenCC 字典生成
- **Git**: 用于版本控制和子模块管理

### Windows 特殊配置
需要启用开发者模式以支持符号链接：
```powershell
# 启用开发者模式 (Windows 10/11)
# 设置 -> 更新和安全 -> 开发者选项 -> 开发人员模式

# 配置 git 支持符号链接
git config --global core.symlinks true
```

## 构建说明

### 1. 克隆项目
```bash
git clone https://github.com/osfans/trime.git
cd trime
git submodule update --init --recursive
# 或使用 partial clone 节省时间
git submodule update --init --recursive --filter=blob:none
```

### 2. 调试版本构建
```bash
# Linux/macOS
make debug

# Windows
.\gradlew assembleDebug
```

### 3. 发布版本构建
创建 `keystore.properties` 文件：
```properties
storePassword=yourStorePassword
keyPassword=yourKeyPassword
keyAlias=yourKeyAlias
storeFile=/path/to/your/keystore.jks
```

然后构建：
```bash
# Linux/macOS
make release

# Windows
.\gradlew assembleRelease
```

### 4. 清理构建
```bash
# Linux/macOS
make clean

# Windows
.\gradlew clean
```

## 配置说明

### 键盘配置 (preset_keyboards)
键盘通过 YAML 配置定义，支持：
- **横屏键盘**: `landscape_keyboard` 指定横屏时使用的键盘
- **自动分割**: `landscape_split_percent` 控制横屏时键盘分割比例
- **液态键盘**: `liquid_keyboard` 配置符号、表情等扩展键盘

示例配置：
```yaml
patch:
  "style/keyboard": [my_keyboard, my_landscape_keyboard, mini]
  "preset_keyboards/my_keyboard":
    name: My Keyboard
    landscape_keyboard: my_landscape_keyboard
  "preset_keyboards/my_landscape_keyboard":
    name: My Landscape Keyboard
    landscape_split_percent: 0
```

### 主题配置
主题系统支持自定义：
- 键盘外观 (颜色、背景、按键样式)
- 候选词窗口样式
- 工具栏配置
- 预编辑视图

## 依赖管理

项目使用 Gradle Version Catalog (`libs.versions.toml`) 管理依赖：

| 依赖类别 | 主要库 |
|---------|--------|
| **AndroidX** | Activity, AppCompat, Core KTX, Navigation, Room, WorkManager |
| **Kotlin** | Coroutines, Serialization, KSP |
| **UI** | ConstraintLayout, RecyclerView, ViewPager2, Flexbox |
| **依赖注入** | kotlin-inject (KSP 处理器) |
| **工具** | Timber (日志), Splitties, Iconics |
| **第三方** | BaseRecyclerViewAdapterHelper, XXPermissions |

## 贡献指南

### 提交流程
1. Fork 项目仓库
2. 创建功能分支: `git checkout -b feature/my-feature`
3. 提交更改: `git commit -am 'feat: add new feature'`
4. 推送到分支: `git push origin feature/my-feature`
5. 创建 Pull Request 到 `trime:develop`

### 代码规范
- 遵循 [Google Java Style Guide](https://github.com/google/google-java-format)
- 使用 Spotless 进行代码格式化
- 提交信息遵循 [Conventional Commits](https://www.conventionalcommits.org/):
  - `feat`: 新功能
  - `fix`: Bug 修复
  - `docs`: 文档更新
  - `refactor`: 代码重构
  - `test`: 测试相关
  - `build`: 构建系统相关
  - `ci`: CI 配置相关

### 行为准则
项目遵循 [Contributor Covenant Code of Conduct](../CODE_OF_CONDUCT.md)，要求：
- 尊重所有社区成员
- 不接受骚扰、歧视行为
- 建设性地给予和接受反馈

## 第三方库

Trime 使用了多个开源库：

| 库名 | 许可证 | 用途 |
|------|--------|------|
| **Boost** | Boost Software License | C++ 基础库 |
| **darts-clone** | New BSD License | 双数组 Trie 实现 |
| **LevelDB** | New BSD License | 键值存储 |
| **libiconv** | LGPL | 字符编码转换 |
| **marisa-trie** | BSD License | 内存高效 Trie |
| **glog** | New BSD License | Google 日志库 |
| **OpenCC** | Apache 2.0 | 繁简转换 |
| **RIME** | BSD License | 输入法核心 |
| **snappy** | BSD License | 快速压缩 |
| **utfcpp** | Boost Software License | UTF-8/16/32 处理 |
| **yaml-cpp** | MIT License | YAML 解析 |

## 社区与支持

- **GitHub**: https://github.com/osfans/trime
- **文档 Wiki**: https://github.com/osfans/trime/wiki
- **QQ 群**: 
  - 811142286
  - 224230445
- **Telegram**: https://t.me/trime_dev
- **百度贴吧**: RIME 吧

## 下载渠道

- **F-Droid**: https://f-droid.org/packages/com.osfans.trime
- **Google Play**: https://play.google.com/store/apps/details?id=com.osfans.trime
- **Nightly 构建**: https://github.com/osfans/trime/releases/tag/nightly
- **Canary 构建**: https://github.com/osfans/trime/actions

## 历史版本

- **TRIME 1.0**: TaeRv Pinyin (泰如拼音输入法)
- **TRIME 2.0**: 同文输入法平台 - 支持吴语等方言
- **TRIME 3.0**: 基于 librime 的同文输入法

## 许可证

本项目基于 **GNU General Public License v3.0 or later** 许可证发布。

```
SPDX-FileCopyrightText: 2015 - 2024 Rime community
SPDX-License-Identifier: GPL-3.0-or-later
```

完整许可证文本见 [LICENSE](../LICENSE) 文件。

## 致谢

### 开发者
- [osfans](https://github.com/osfans)

### 贡献者
- [boboIqiqi](https://github.com/boboIqiqi)
- [Bambooin](https://github.com/Bambooin)
- [senchi96](https://github.com/senchi96)
- [heiher](https://github.com/heiher)
- [abay](https://github.com/a342191555)
- [iovxw](https://github.com/iovxw)
- 以及更多贡献者...

### 基于项目
- [RIME 输入法引擎](https://rime.im)
- [OpenCC 繁简转换](https://github.com/BYVoid/OpenCC)
- [Android Traditional Chinese IME](https://code.google.com/p/android-traditional-chinese-ime/)

---

*本文档由 opencode 根据项目源码自动生成，最后更新时间: 2026-05-03*
