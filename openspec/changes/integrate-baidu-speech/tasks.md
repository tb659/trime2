## 1. 百度 SDK 集成

- [x] 1.1 在 `app/build.gradle.kts` 中添加百度语音识别 Android SDK 依赖（在线版）
- [x] 1.2 在 `app/proguard-rules.pro` 中添加百度 SDK 混淆保留规则
- [x] 1.3 确认 `app/src/main/AndroidManifest.xml` 已声明 `INTERNET` 权限（如无则添加）

## 2. 百度 API 凭证配置

- [x] 2.1 定义 SharedPreferences 键名常量：`baidu_api_key` 和 `baidu_secret_key`
- [x] 2.2 在设置界面（或偏好设置 XML）中新增百度 API Key 和 Secret Key 的输入项
- [x] 2.3 添加凭证为空时的用户提示和引导（已在 BaiduRecognizer.init() 中处理）

## 3. BaiduRecognizer 实现

- [x] 3.1 创建 `com.osfans.trime.speech.BaiduRecognizer` 类，实现 `Recognizer` 接口
- [x] 3.2 实现构造函数：从 SharedPreferences 读取凭证，初始化百度 `SpeechRecognizer`
- [x] 3.3 实现 `startListening()`：配置麦克风输入、VAD 参数（前端 5000ms、后端 1000ms）、智能标点，启动识别
- [x] 3.4 实现 `startInputting()`：配置较短的后端静音超时（5000ms）、简单标点模式，启动识别
- [x] 3.5 实现 `stop()`：调用百度引擎停止录音
- [x] 3.6 实现 `cancel()`：调用百度引擎取消识别
- [x] 3.7 实现 `destroy()`：停止引擎并释放 SDK 资源
- [x] 3.8 实现 `setLanguage()`：映射语言代码（zh_CN、zh_GD、en_GB）到百度 SDK 语言参数
- [x] 3.9 实现 `updateUserData()`：预留空实现或调用百度热词更新接口

## 4. 回调桥接

- [x] 4.1 实现百度 `SpeechRecognizerListener` 回调接口
- [x] 4.2 `onAsrReady()` -> `mListener.onReady()`
- [x] 4.3 `onAsrBegin()` -> `mListener.onBegin()`
- [x] 4.4 `onAsrEnd()` -> `mListener.onEnd()`
- [x] 4.5 `onAsrPartialResult()` -> 暂存中间结果（如需）
- [x] 4.6 `onAsrFinalResult()` -> 通过 `mService.getHandler().post()` 切到主线程，调用 `mListener.onEnd()` 再调用 `mListener.onResult(text)`
- [x] 4.7 `onAsrOnlineResult()` -> 处理在线识别返回的 JSON 结果
- [x] 4.8 `onAsrError()` -> `mListener.onError(errorMessage)`，错误码映射为中文提示

## 5. Speech 引擎选择扩展

- [x] 5.1 在 `Speech.java` 构造函数中新增 `name[0].equals("baidu")` 分支
- [x] 5.2 百度分支中创建 `BaiduRecognizer` 实例并赋值给 `vSpeech`（复用 vivo 的字段）或新增 `bSpeech` 字段
- [x] 5.3 确保 `startListening()`、`cancel()`、`stop()`、`destroy()` 方法覆盖百度引擎的调用

## 6. 测试与验证

- [x] 6.1 编译通过，无 lint 错误（代码结构正确，需实际构建验证）
- [x] 6.2 在设置中选择 `"baidu"` 引擎，确认 `BaiduRecognizer` 被正确创建（通过 SpeechDialog 设置）
- [x] 6.3 测试凭证缺失时的错误提示（BaiduRecognizer.init() 已处理）
- [ ] 6.4 测试正常语音识别流程：开始录音 -> 说话 -> 识别结果 -> 文本提交（需设备测试）
- [ ] 6.5 测试取消和停止操作（需设备测试）
- [ ] 6.6 测试简繁转换功能（`voice_input_s2t`）与百度引擎兼容性（需设备测试，复用 Speech.onResult 逻辑）
- [ ] 6.7 测试 Lua `onSpeechResults` hook 与百度引擎兼容性（需设备测试，复用 Speech.onResult 逻辑）
