## Why

Android系统从某些版本开始限制了Toast弹窗的位置修改，导致项目中的Toast始终显示在屏幕底部，无法根据输入法场景调整到更合适的位置（如键盘上方）。需要自定义Toast弹窗来实现灵活的位置控制。

## What Changes

- 创建自定义Toast弹窗组件，支持在指定位置（如输入法键盘上方）显示
- 实现与系统Toast类似的API接口，降低替换成本
- 替换项目中所有使用`Toast.makeText().show()`的代码，改用自定义弹窗
- 支持自定义样式（背景、文字颜色、圆角等）以匹配应用主题
- 支持设置显示时长（短/长）和自动消失

## Capabilities

### New Capabilities
- `custom-toast`: 自定义Toast弹窗组件，支持位置控制、样式自定义、自动消失等功能

### Modified Capabilities

## Impact

- **代码影响**: 项目中约112处使用系统Toast的地方需要替换，主要涉及：
  - `app/src/main/java/com/osfans/trime/` 目录下的多个类（TrimeService、Function、各种Dialog等）
  - `app/src/main/java/com/androlua/` 目录下的多个类（LuaActivity、LuaService等）
  - `app/src/main/java/com/nirenr/screencapture/` 目录下的ScreenShot和ScreenCaptureActivity
- **依赖**: 无新增外部依赖，使用Android原生View系统实现
- **API变更**: 提供新的Toast工具类，保持类似系统Toast的调用方式（如`CustomToast.show(context, text, duration)`）
