## Why

Trime2 当前默认使用 vivo 语音 SDK 作为语音识别引擎，但该服务需要 vivo 企业开发者帐号才能正常使用。对于没有 vivo 企业帐号的开发者和用户，语音输入功能无法工作。接入百度语音识别 API 可以提供一个开放的替代方案，使所有用户都能使用语音输入功能。

## What Changes

- 新增百度语音识别引擎实现 `BaiduRecognizer`，实现现有 `Recognizer` 接口
- 在 `Speech` 管理器中注册百度语音引擎选项
- 新增百度语音 SDK 依赖和必要的网络权限配置
- 新增百度语音 API 凭证（API Key / Secret Key）配置项
- 保持现有 vivo 引擎和 Android 系统引擎不变，用户可通过偏好设置切换

## Capabilities

### New Capabilities
- `baidu-speech-recognizer`: 百度语音识别引擎集成，包括 SDK 初始化、语音识别会话管理、语言设置和回调处理

### Modified Capabilities
- `speech-engine-selection`: 语音引擎选择机制扩展，新增 `baidu` 选项到 `recognition_service` 偏好设置

## Impact

- 新增文件：`app/src/main/java/com/osfans/trime/speech/BaiduRecognizer.java`
- 修改文件：`app/src/main/java/com/osfans/trime/Speech.java`（引擎选择逻辑）
- 修改文件：`app/build.gradle.kts`（新增百度语音 SDK 依赖）
- 修改文件：`app/src/main/AndroidManifest.xml`（新增网络权限，如尚未声明）
- 修改文件：`app/proguard-rules.pro`（百度 SDK 混淆规则）
- 新增依赖：百度语音识别 SDK（在线识别版）
