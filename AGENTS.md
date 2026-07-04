# Trime — 同文输入法

本仓库所有对话使用中文交流。

Rime IME for Android。Java/Kotlin + C++ (JNI) 混合项目，基于 librime。

## 构建

- JDK 17, Android SDK, Android NDK, Gradle 9.2
- 主模块 `:app`，KSP 代码生成模块 `:codegen`，约定插件在 `build-logic/convention`
- 调试构建: `.\gradlew assembleDebug` (Windows) / `make debug` (Linux/macOS)
- 发布构建: `.\gradlew assembleRelease`，需 `keystore.properties` 或 `local.properties` 配置签名 (signKeyFile/signKeyStorePwd/signKeyAlias/signKeyPwd)
- API 密钥 (百度语音) 放在 `local.properties`: `API_KEY`, `SECRET_KEY`, `API_ID`
- `gradle.properties` 已配置 Clash 代理 (127.0.0.1:7890)，如不用代理请移除

## Windows 环境要求

- 启用开发者模式（否则符号链接需要管理员权限）
- `git config --global core.symlinks true`

## JNI 原生代码

- 原生依赖通过 git 子模块引入 (`app/src/main/jni/`)
- JNI 缓存加速: 若 `app/prebuilt/` 存在则跳过 CMake 构建；缓存来自 `app/build/intermediates/stripped_native_libs/universalRelease/out/lib/`
- 清除 .cxx 中间文件: `.\gradlew :app:cleanCxxIntermediates`

## 主要命令

| 命令 | 说明 |
|---|---|
| `.\gradlew assembleDebug` | 调试构建 (Windows) |
| `.\gradlew assembleRelease` | 发布构建 |
| `.\gradlew spotlessCheck` | Kotlin 代码风格检查 (ktlint 1.7.1) |
| `.\gradlew spotlessApply` | 自动修复 Kotlin 风格问题 |
| `.\script\clang-format.sh -n` | C++ 代码风格检查 (Google 风格) |
| `.\script\clang-format.sh -i` | 自动格式化 C++ |
| `.\gradlew test` | 运行单元测试 (Kotest + JUnit Platform) |
| `make translate` | 繁→简中文翻译生成 (需 OpenCC) |

## 代码风格

- Kotlin: Spotless/ktlint (IntelliJ IDEA code style)
- C++: clang-format 基于 Google 风格
- 所有源文件需含 SPDX 头 (`SPDX-FileCopyrightText: 2015 - 2025 Rime community` / `SPDX-License-Identifier: GPL-3.0-or-later`)
- `.editorconfig`: LF 换行，末尾空行

## 测试

- 使用 Kotest (StringSpec / BehaviorSpec)，JUnit Platform runner
- 测试资源在 `app/src/test/assets/` (如 `trime.yaml`, `incorrect.yaml`)
- Room 迁移 schema 输出到 `app/schemas/`

## 提交规范

- Conventional Commits (`feat:`, `fix:`, `refactor:`, `docs:`, `test:`, `chore:`, `ci:` 等)
- 自动生成 CHANGELOG 使用 git-cliff (`make cliff`)，不合规的提交会被过滤
- scope 可选，正文不少于 20 字符，行宽 ≤ 100

## 架构要点

- 入口: `com.osfans.trime.TrimeService` (InputMethodService) + `PrefLauncher` (设置界面)
- 命名空间 `com.osfans.trime`，applicationId `com.tbagr.trime`（非官方 fork）
- `core/` 包是 JNI 桥接层 (`Rime.java` 等)，`data/` 包正在 Kotlin 化
- 主题系统: `theme/` 包 (Java)，数据模型在 `data/theme/model/`
- 带 Lua 脚本引擎 (`androlua` / `luajava`)
- Room 数据库，`kotlin-inject` 依赖注入，Splitties 视图 DSL
