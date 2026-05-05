## 新增需求

### 需求：自定义Toast SHALL能在指定位置显示
自定义Toast SHALL能够在屏幕指定位置显示，特别是在输入法场景中显示在键盘上方。

#### 场景：在键盘上方显示
- **当** 输入法正在显示且从TrimeService调用`CustomToast.show()`时
- **则** Toast SHALL出现在键盘区域上方，用户可见

#### 场景：在自定义位置显示
- **当** 调用`CustomToast.show()`并传入自定义位置参数（x, y）时
- **则** Toast SHALL显示在指定坐标处

### 需求：自定义Toast SHALL支持外观自定义
自定义Toast SHALL支持自定义背景、文字颜色和其他视觉属性，以匹配应用主题。

#### 场景：默认样式
- **当** 调用`CustomToast.show()`且未传入自定义样式参数时
- **则** Toast SHALL使用布局资源中定义的默认样式

#### 场景：自定义文字
- **当** 调用`CustomToast.show()`并传入文字参数时
- **则** Toast SHALL显示提供的文字并应用适当的样式

### 需求：自定义Toast SHALL在持续时间后自动消失
自定义Toast SHALL在指定持续时间（短或长）后自动消失，类似于系统Toast的行为。

#### 场景：短时间
- **当** 调用`CustomToast.show()`并传入`Toast.LENGTH_SHORT`时
- **则** Toast SHALL在约2秒后消失

#### 场景：长时间
- **当** 调用`CustomToast.show()`并传入`Toast.LENGTH_LONG`时
- **则** Toast SHALL在约3.5秒后消失

### 需求：自定义Toast SHALL提供简单API
自定义Toast SHALL提供类似系统Toast的简单静态API，最小化替换现有Toast调用的工作量。

#### 场景：简单显示调用
- **当** 开发者调用`CustomToast.show(context, "消息", Toast.LENGTH_SHORT)`时
- **则** SHALL显示包含该消息的Toast，持续短时间

#### 场景：替换现有Toast
- **当** 将现有的`Toast.makeText(context, text, duration).show()`代码替换为`CustomToast.show(context, text, duration)`时
- **则** 在消息显示和持续时间方面行为SHALL等效

### 需求：自定义Toast SHALL正确处理生命周期
自定义Toast SHALL正确处理其生命周期，避免内存泄漏并确保在适当时机关闭。

#### 场景：持续时间结束后关闭
- **当** Toast正在显示且持续时间到期时
- **则** Toast SHALL被关闭并释放资源

#### 场景：无内存泄漏
- **当** Toast正在显示且上下文失效时
- **则** Toast SHALL被关闭而不导致内存泄漏
