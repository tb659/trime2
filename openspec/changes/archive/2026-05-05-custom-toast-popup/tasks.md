## 1. 创建自定义Toast组件

- [x] 1.1 在`com.osfans.trime.util`包中创建`CustomToast`工具类
- [x] 1.2 实现基于PopupWindow的Toast显示逻辑
- [x] 1.3 实现显示位置计算逻辑（默认键盘上方）
- [x] 1.4 实现自动消失逻辑（短/长持续时间）

## 2. 创建Toast布局和资源

- [x] 2.1 创建Toast布局文件`toast_custom.xml`（定义背景、文字样式等）
- [x] 2.2 定义Toast样式资源（背景圆角、颜色等）
- [x] 2.3 添加必要的尺寸资源（padding、margin等）

## 3. 实现CustomToast API

- [x] 3.1 实现静态`show()`方法，类似系统Toast API
- [x] 3.2 支持传入Context、文字、持续时间参数
- [x] 3.3 支持自定义位置参数（可选）
- [x] 3.4 处理上下文生命周期，避免内存泄漏

## 4. 替换Trime核心模块中的Toast调用

- [x] 4.1 替换`TrimeService.java`中的Toast调用（2处）
- [x] 4.2 替换`Function.java`中的Toast调用（约12处）
- [x] 4.3 替换`DeployDialog.java`中的Toast调用（3处）
- [x] 4.4 替换`SchemaDialog.java`、`OptionsDialog.java`等Dialog中的Toast调用（4处）
- [x] 4.5 替换`Speech.java`中的Toast调用（1处）
- [x] 4.6 替换`PrefLauncher.java`中的Toast调用（1处）

## 5. 替换androlua模块中的Toast调用

- [x] 5.1 替换`LuaActivity.java`中的Toast调用和Toast相关字段（多处）
- [x] 5.2 替换`LuaService.java`中的Toast调用和Toast相关字段
- [x] 5.3 替换`LuaWallpaperService.java`中的Toast调用和Toast相关字段
- [x] 5.4 替换`LuaNotificationListenerService.java`中的Toast调用和Toast相关字段
- [x] 5.5 替换`LuaAccessibilityService.java`中的Toast调用和Toast相关字段
- [x] 5.6 替换`LuaEditorActivity.java`中的Toast调用（约10处）
- [x] 5.7 替换`LuaEditor.java`中的Toast调用（3处）
- [x] 5.8 替换`ImportProject.java`中的Toast调用（2处）

## 6. 替换其他模块中的Toast调用

- [x] 6.1 替换`ScreenShot.java`中的Toast调用（1处）
- [ ] 6.2 替换`ScreenCaptureActivity.java`中的Toast调用（2处）

## 7. 测试和验证

- [x] 7.1 测试自定义Toast在键盘上方显示
- [x] 7.2 测试短/长持续时间的自动消失
- [x] 7.3 测试不同场景下的显示位置
- [x] 7.4 验证无内存泄漏
- [x] 7.5 测试在TrimeService中的显示时机（确保Window附加后再显示）
