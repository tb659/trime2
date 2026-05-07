## Context

Trime2 是一个 Android 输入法应用，当前使用双引擎语音识别架构：
- **VivoRecognizer**：基于 vivo 语音 SDK（`com.vivo.speechsdk.asr_tts_5.2.4.00_external.aar`），需要 vivo 企业开发者帐号和 API 凭证
- **Android SpeechRecognizer**：系统级语音识别服务，通过 Intent 解析可用服务

核心抽象是 `Recognizer` 接口，定义了 `startListening()`、`startInputting()`、`stop()`、`cancel()`、`destroy()`、`setLanguage()`、`updateUserData()` 等方法。

`Speech` 类作为中央协调器，根据 `recognition_service` 偏好设置选择引擎。vivo 是默认选项（`"vivo"`），其他值回退到 Android 系统识别器。

## Goals / Non-Goals

**Goals:**
- 新增 `BaiduRecognizer` 实现 `Recognizer` 接口，与 `VivoRecognizer` 平级
- 用户在偏好设置中选择 `"baidu"` 即可使用百度语音识别
- 百度 API 凭证（API Key / Secret Key）可配置，存储在应用的 SharedPreferences 中
- 使用百度语音识别在线版 SDK（Android），通过 REST API 或 SDK 方式实现
- 保持与现有架构一致：回调 `RecognizerListener`，兼容 `Speech` 类的状态管理和音效/震动反馈

**Non-Goals:**
- 不修改 vivo 引擎的实现
- 不实现离线语音识别（百度离线 SDK 需要额外授权）
- 不提供凭证的云端管理或轮换机制
- 不改变简繁转换、Lua hook 等现有后处理逻辑

## Decisions

### 1. 使用百度语音识别 Android SDK（在线版）

**Decision**: 使用百度官方 Android SDK（`com.baidu.speech:android-speech`）而非直接调用 REST API。

**Rationale**:
- SDK 封装了音频采集、编码、网络传输等细节，与 vivo SDK 的使用模式一致
- 提供 `SpeechRecognizer` 实例和事件回调，易于映射到 `Recognizer` 接口
- 减少自行处理音频格式（16kHz PCM）和鉴权（access_token）的复杂度

**Alternatives considered**:
- 直接调用百度 REST API：需要自行处理录音、PCM 编码、鉴权、网络请求，复杂度高
- 使用 Android 系统 SpeechRecognizer + 百度语音服务组件：百度不提供标准 Android RecognitionService 组件

### 2. API 凭证通过 SharedPreferences 配置

**Decision**: 百度 API Key 和 Secret Key 通过 SharedPreferences 存储，与应用现有的偏好设置机制一致。

**Rationale**:
- 与应用现有的 `Function.getPref()` 模式一致
- vivo 引擎的凭证从 `BuildConfig` 读取（编译时注入），但百度凭证需要用户自行申请并在运行时配置
- 可在设置界面中提供输入框让用户填写

**Alternatives considered**:
- 使用 `BuildConfig`：用户需要自行编译应用，不适合分发场景
- 使用 Android Keystore：增加复杂度，且密钥仍需用户输入

### 3. BaiduRecognizer 直接实现 Recognizer 接口

**Decision**: `BaiduRecognizer` 实现 `Recognizer` 接口，不继承 `VivoRecognizer` 或引入新的中间抽象。

**Rationale**:
- `Recognizer` 接口已经足够简洁，两种 SDK 的使用模式差异较大
- vivo SDK 使用 `ASREngine.start(Bundle)` 方式，百度 SDK 使用 `SpeechRecognizer.start(SpeechRecognizerListener)` 方式
- 保持与现有架构一致，`Speech` 类只需增加一个 `else if` 分支

### 4. 音频参数与 VivoRecognizer 保持一致

**Decision**: 百度识别器的 VAD 参数、采样率、声道设置等尽量与 `VivoRecognizer` 的 `startListening()` / `startInputting()` 保持一致。

**Rationale**:
- 确保用户体验的一致性（静音检测时间、标点行为等）
- `VivoRecognizer` 的参数已经经过调优

## Risks / Trade-offs

| Risk | Mitigation |
|------|------------|
| 百度语音 SDK 的包体积增加（约 2-3MB） | 使用在线版 SDK，不包含离线模型；可考虑 ProGuard 优化 |
| 百度 API 有免费调用额度限制（通常每日 50000 次） | 在文档中说明限制；用户可申请更高额度 |
| 需要 `INTERNET` 权限（应用可能已有） | 检查 `AndroidManifest.xml`，如未声明则新增 |
| 百度 SDK 的回调线程可能与 vivo SDK 不同 | 在 `onResult` 回调中通过 `mService.getHandler().post()` 切换到主线程，与 `VivoRecognizer` 一致 |
| 用户需要自行注册百度开发者帐号并获取凭证 | 在设置界面中提供引导链接和说明 |
