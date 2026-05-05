## Context

项目当前使用Android系统Toast（`Toast.makeText().show()`）来显示临时消息提示。从Android某些版本开始，系统限制了Toast的位置修改，导致Toast始终显示在屏幕底部。对于输入法应用（IME）来说，当键盘弹出时，底部的Toast可能被键盘遮挡或显示位置不合适，影响用户体验。

项目中约有112处使用系统Toast的代码，分布在多个模块中：
- `com.osfans.trime` 包：输入法核心功能相关
- `com.androlua` 包：Lua脚本支持相关
- `com.nirenr.screencapture` 包：截图功能相关

## Goals / Non-Goals

**Goals:**
- 创建自定义Toast弹窗组件，支持灵活的位置控制（特别是键盘上方）
- 保持与系统Toast类似的API，降低替换成本
- 支持自定义样式以匹配应用主题
- 替换项目中所有系统Toast调用

**Non-Goals:**
- 不实现Toast队列管理（多个Toast顺序显示）
- 不实现复杂动画效果（简单的渐入渐出即可）
- 不修改系统Toast的行为（仅替换，不兼容）

## Decisions

### 1. 实现方式：基于PopupWindow而非自定义Toast

**选择**: 使用`PopupWindow`实现自定义Toast

**理由**:
- `PopupWindow`可以自由控制显示位置，支持相对于某个View定位
- 不受系统Toast限制，可以在任意位置显示
- 生命周期可控，可以精确控制显示和消失

**替代方案**:
- 自定义Toast（继承系统Toast）：仍受系统限制，无法修改位置
- 使用Dialog：过于重量级，不适合短暂提示场景
- 使用Snackbar：需要CoordinatorLayout，不适合输入法场景

### 2. 显示位置：默认在键盘上方，支持自定义

**选择**: 提供默认位置（键盘上方），同时允许调用时指定位置

**理由**:
- 输入法应用的主要场景是在键盘上方显示提示
- 保留灵活性，部分场景可能需要其他位置

### 3. API设计：静态工具类

**选择**: 创建`CustomToast`静态工具类，提供类似系统Toast的API

**理由**:
- 降低替换成本，调用方式类似`CustomToast.show(context, text, duration)`
- 统一管理Toast的样式和行为
- 便于后续维护和扩展

### 4. 样式配置：支持自定义布局

**选择**: 使用自定义布局文件定义Toast样式，支持通过参数调整

**理由**:
- 样式与代码分离，便于调整
- 支持背景、文字颜色、圆角等自定义
- 可以复用项目现有的样式资源

## Risks / Trade-offs

- **[风险] 内存泄漏** → 使用`WeakReference`持有Context，确保在合适的时机 dismiss
- **[风险] 显示时机问题** → 在InputMethodService中需要正确处理View的附加时机，确保在Window显示后再显示PopupWindow
- **[权衡] 性能开销** → PopupWindow相比系统Toast有稍大的开销，但对于短暂提示场景可忽略
- **[权衡] 兼容性** → 需要测试不同Android版本的行为，特别是输入法场景
