# app/src/main/java 目录详细文档

## 目录概述

`app/src/main/java` 是 Trime 项目的核心 Java/Kotlin 源码目录，包含了输入法引擎的所有 Java 层实现代码。该目录按照功能模块组织，主要包含以下几个顶级包：

- `com.osfans.trime` - Trime 主应用包
- `com.android.cglib` - Android cglib/DX 工具库
- `com.androlua` - AndroLua 集成（Lua 脚本支持）
- `com.myopicmobile.textwarrior` - 文本编辑器组件
- `com.nirenr` - 辅助工具类
- `org.luaj` - LuaJava 实现（Lua 解释器）

**文件统计**：
- Java 文件：74 个
- Kotlin 文件：5 个

---

## 1. com.osfans.trime - Trime 主应用包

这是 Trime 输入法的核心实现包，负责输入法服务、键盘UI、候选词管理、主题等功能。

### 1.1 根目录文件

| 文件名 | 类型 | 功能说明 |
|--------|------|----------|
| `TrimeService.java` | Java | **输入法服务主类**，继承自 `InputMethodService`，是整个输入法的入口点。负责处理键盘事件、文本输入、候选词显示、与 RIME 核心通信等核心功能。文件长达 1571 行，是项目最核心的文件。 |
| `TrimeApplication.java` | Java | **应用 Application 类**，负责应用初始化、检查存储权限、解压 APK 资源等全局初始化工作。 |
| `InputView.java` | Java | **输入法主视图**，管理键盘布局、候选词窗口、预编辑视图等 UI 组件的显示和交互。 |
| `RootInputView.java` | Java | **根输入视图**，可能是 InputView 的容器或包装类。 |
| `Key.java` | Java | **按键定义类**，描述键盘上每个按键的属性（键值、标签、宽度等）。 |
| `Composition.java` | Java | **组合文本管理**，处理输入法中的预编辑文本（Composing Text）和提交文本。 |
| `Config.java` | Java | **配置管理类**，负责加载和解析 Trime 的 YAML 配置文件。 |
| `Event.java` | Java | **事件处理类**，处理各种输入事件和自定义事件。 |
| `JsonUtil.java` | Java | **JSON 工具类**，提供 JSON 解析和生成功能。 |
| `PrefLauncher.java` | Java | **偏好设置启动器**，用于启动 Trime 的设置界面。 |
| `Speech.java` | Java | **语音输入管理**，集成语音识别功能。 |
| `VivoGpt.java` | Java | **Vivo GPT 集成**，可能与 Vivo 的 AI 能力集成。 |
| `ClipboardUtil.java` | Java | **剪贴板工具**，管理剪贴板内容和剪贴板历史。 |

### 1.2 candidate/ - 候选词管理模块

负责管理输入法中的候选词显示和交互。

| 文件名 | 类型 | 功能说明 |
|--------|------|----------|
| `CandidateView.java` | Java | **候选词视图**，显示候选词列表的 UI 组件。 |
| `CandidateAdapter.java` | Java | **候选词适配器**，为候选词列表提供数据适配（可能用于 RecyclerView）。 |
| `CandidatesManager.java` | Java | **候选词管理器**，管理候选词的加载、更新、分页等逻辑。 |
| `ExpandedCandidateView.java` | Java | **展开式候选词视图**，可能是全屏或扩展的候选词显示界面。 |
| `FlexboxCandidateAdapter.java` | Java | **Flexbox 候选词适配器**，使用 Flexbox 布局显示候选词。 |
| `FloatCandidateView.java` | Java | **浮动候选词视图**，浮动显示的候选词窗口。 |
| `FloatCandidateAdapter.java` | Java | **浮动候选词适配器**，为浮动候选词视图提供数据。 |
| `ToolbarView.java` | Java | **工具栏视图**，显示输入法工具栏（如切换输入方案、设置等按钮）。 |

### 1.3 core/ - 核心功能模块

与 RIME 输入法引擎集成的核心代码。

| 文件名 | 类型 | 功能说明 |
|--------|------|----------|
| `Rime.java` | Java | **RIME 核心接口**，封装与 librime (RIME 核心库) 的 JNI 调用，管理输入法状态、候选词获取、按键处理等。文件长达 830 行，是 RIME 集成的核心。 |
| `RimeApi.java` | Java | **RIME API 接口定义**，定义与 RIME 交互的接口规范。 |
| `RimeConfig.java` | Java | **RIME 配置管理**，处理 RIME 的配置文件和运行时配置。 |
| `RimeProto.java` | Java | **RIME 协议数据**，可能定义与 RIME 通信的数据结构（ProtoBuf 相关）。 |
| `RimeMessage.java` | Java | **RIME 消息**，定义 RIME 返回的消息类型。 |
| `RimeKeyEvent.java` | Java | **RIME 按键事件**，处理按键码和修饰键（Shift/Ctrl/Alt）的映射。 |
| `RimeKeyMap.java` | Java | **按键映射**，定义按键名称到按键码的映射关系。 |
| `RimeLifecycle.java` | Java | **RIME 生命周期接口**，定义 RIME 的初始化和销毁接口。 |
| `RimeLifecycleImpl.java` | Java | **RIME 生命周期实现**，实现 RIME 的初始化、启动、停止等生命周期管理。 |
| `RimeLifecycleOwner.java` | Java | **RIME 生命周期所有者**，管理 RIME 生命周期的持有者。 |
| `RimeLifecycleUtils.java` | Java | **RIME 生命周期工具**，提供生命周期相关的工具方法。 |
| `RimeDispatcher.java` | Java | **RIME 消息分发器**，负责将 RIME 的响应分发给各个组件。 |
| `RimeSchema.java` | Java | **RIME 方案管理**，管理输入方案（Schema）的加载和切换。 |
| `SchemaItem.java` | Java | **方案项**，表示一个输入方案的元数据。 |
| `CandidateItem.java` | Java | **候选词项**，表示一个候选词的数据结构。 |
| `DataManager.java` | Java | **数据管理器**，管理 RIME 的数据文件（词典、配置等）。 |
| `KeyModifier.java` | Java | **按键修饰符**，表示单个修饰键的状态。 |
| `KeyModifiers.java` | Java | **按键修饰符集合**，管理多个修饰键的组合状态。 |
| `KeyValue.java` | Java | **键值**，表示按键的值（可能包含按键码和修饰符）。 |
| `Flow.java` | Java | **流处理**，可能用于响应式数据流（类似 Kotlin Flow）。 |
| `SharedFlowImpl.java` | Java | **共享流实现**，实现类似 Kotlin SharedFlow 的功能。 |

### 1.4 data/ - 数据管理模块

管理输入法的数据，包括用户词典和繁简转换。

#### 1.4.1 opencc/ - 繁简转换

| 文件名 | 类型 | 功能说明 |
|--------|------|----------|
| `OpenCCDictManager.kt` | Kotlin | **OpenCC 词典管理器**，管理 OpenCC (繁简转换) 的词典文件。 |

##### dict/ - OpenCC 词典实现

| 文件名 | 类型 | 功能说明 |
|--------|------|----------|
| `Dictionary.kt` | Kotlin | **词典接口/基类**，定义词典的抽象接口。 |
| `OpenCCDictionary.kt` | Kotlin | **OpenCC 词典实现**，实现 OpenCC 繁简转换词典的加载和查询。 |
| `TextDictionary.kt` | Kotlin | **文本词典**，可能用于加载文本格式的词典文件。 |

#### 1.4.2 userdict/ - 用户词典

| 文件名 | 类型 | 功能说明 |
|--------|------|----------|
| `UserDictManager.kt` | Kotlin | **用户词典管理器**，管理用户的自定义词库（添加、删除、查询用户词组）。 |

### 1.5 dialog/ - 对话框模块

各种类型的对话框实现。

| 文件名 | 类型 | 功能说明 |
|--------|------|----------|
| `DeployDialog.java` | Java | **部署对话框**，在 RIME 部署（重新加载配置）时显示进度或状态。 |
| `KeyboardDialog.java` | Java | **键盘对话框**，用于选择或配置键盘布局。 |
| `OptionsDialog.java` | Java | **选项对话框**，显示和修改输入法的各种选项。 |
| `SchemaDialog.java` | Java | **方案对话框**，用于选择和切换输入方案。 |
| `SchemaGroupDialog.java` | Java | **方案组对话框**，可能用于管理方案分组。 |
| `StyleDialog.java` | Java | **样式对话框**，配置键盘和界面的样式。 |
| `ThemeDialog.java` | Java | **主题对话框**，用于选择和配置主题。 |

### 1.6 enums/ - 枚举定义

| 文件名 | 类型 | 功能说明 |
|--------|------|----------|
| `InlineModeType.java` | Java | **内联模式类型**，定义输入法内联编辑的模式（如光标移动、选择等）。 |
| `KeyEventType.java` | Java | **按键事件类型**，定义按键事件的类型（按下、释放等）。 |
| `WindowsPositionType.java` | Java | **窗口位置类型**，定义各 UI 组件（候选词窗口、工具栏等）的显示位置。 |

### 1.7 keyboard/ - 键盘模块

管理键盘的显示、交互和布局。

| 文件名 | 类型 | 功能说明 |
|--------|------|----------|
| `KeyboardView.java` | Java | **键盘视图基类**，显示键盘布局和处理按键交互。文件 282 行，管理按键视图集合和键盘状态。 |
| `AbsKeyboardView.java` | Java | **抽象键盘视图**，可能是 KeyboardView 的抽象基类，定义通用行为。 |
| `RowKeyboardView.java` | Java | **行式键盘视图**，按行布局的键盘视图。 |
| `FlexboxKeyboardView.java` | Java | **Flexbox 键盘视图**，使用 Flexbox 布局实现灵活的键盘布局。 |
| `FloatKeyboard.java` | Java | **浮动键盘**，浮动显示的键盘窗口。 |
| `ClipboardKeyboardView.java` | Java | **剪贴板键盘视图**，显示剪贴板内容的键盘界面。 |
| `SymbolsKeyboardView.java` | Java | **符号键盘视图**，显示符号、表情等扩展键盘。 |
| `KeyView.java` | Java | **按键视图**，表示键盘上的单个按键，处理按键绘制和点击事件。 |
| `ModifierState.java` | Java | **修饰键状态**，跟踪 Shift、Ctrl、Alt 等修饰键的状态。 |
| `TightTextView.java` | Java | **紧凑文本视图**，可能是用于按键标签的紧凑型 TextView。 |

#### 1.7.1 adapter/ - 键盘适配器

| 文件名 | 类型 | 功能说明 |
|--------|------|----------|
| `ListPagerAdapter.java` | Java | **列表分页适配器**，用于分页显示键盘布局或候选词。 |
| `LuaValueListAdapter.java` | Java | **Lua 值列表适配器**，将 Lua 数据适配为列表显示。 |
| `WaterfallAdapter.java` | Java | **瀑布流适配器**，使用瀑布流布局显示内容。 |

### 1.8 speech/ - 语音输入模块

| 文件名 | 类型 | 功能说明 |
|--------|------|----------|
| `Recognizer.java` | Java | **语音识别器接口/基类**，定义语音识别的通用接口。 |
| `RecognizerListener.java` | Java | **识别监听器**，监听语音识别的结果和状态变化。 |
| `VivoRecognizer.java` | Java | **Vivo 语音识别器**，集成 Vivo 的语音识别 SDK。 |

### 1.9 theme/ - 主题模块

| 文件名 | 类型 | 功能说明 |
|--------|------|----------|
| `ThemeManager.java` | Java | **主题管理器**，负责主题的加载、切换和应用。 |
| `Style.java` | Java | **样式定义**，定义键盘、按键、候选词等 UI 组件的样式属性。 |
| `KeyStyle.java` | Java | **按键样式**，专门定义按键的视觉样式（颜色、背景、字体等）。 |

### 1.10 util/ - 工具类模块

| 文件名 | 类型 | 功能说明 |
|--------|------|----------|
| `BackUtil.java` | Java | **返回键工具**，处理返回键的逻辑。 |
| `Cloud.java` | Java | **云服务工具**，可能与云端同步或云输入相关。 |
| `Function.java` | Java | **函数工具**，提供通用的函数式编程工具方法。 |
| `HttpUtil.java` | Java | **HTTP 工具**，处理 HTTP 请求，可能用于下载配置或同步数据。 |

---

## 2. com.android.cglib - Android cglib/DX 工具库

这个包包含了 Android Dex 文件处理和代码生成的工具库，可能是从第三方库（如 dexmaker 或 cglib）集成而来。

### 2.1 dx/ - Dex 文件处理

包含 Android Dex 文件格式的处理类，用于动态生成或操作 Dex 文件。

#### 主要子包：
- `io/` - Dex 文件 I/O 操作
- `rop/` - Register Operation (寄存器操作) 相关类
  - `annotation/` - 注解处理
  - `code/` - 字节码操作
  - `cst/` - 常量池管理
  - `type/` - 类型处理
- `ssa/` - Static Single Assignment 形式优化
  - `back/` - SSA 后端（寄存器分配等）
- `stock/` - 代理类生成
- `util/` - 各种工具类

**文件数量**：约 120+ 个 Java 文件

这个库主要用于：
- 动态生成 Dex 字节码
- Lua 脚本的编译和执行
- 可能用于插件或动态功能扩展

---

## 3. com.androlua - AndroLua 集成

AndroLua 是一个允许在 Android 上运行 Lua 脚本的框架，Trime 集成它以支持 Lua 脚本扩展。

### 3.1 主要类

| 文件名 | 功能说明 |
|--------|----------|
| `LuaApplication.java` | Lua 应用的 Application 类，初始化 Lua 环境。 |
| `LuaActivity.java` | Lua 活动，允许用 Lua 编写 Android Activity。 |
| `LuaActivityX.java` | 扩展的 Lua Activity。 |
| `LuaService.java` | Lua 服务，允许用 Lua 编写 Android Service。 |
| `LuaBroadcastReceiver.java` | Lua 广播接收器。 |
| `LuaAppWidgetProvider.java` | Lua App Widget 提供器。 |
| `LuaFragment.java` | Lua Fragment。 |
| `LuaView.java` | Lua 视图，允许用 Lua 创建和操作 Android View。 |
| `LuaEditor.java` | Lua 编辑器，可能是内嵌的代码编辑器。 |
| `LuaEditorActivity.java` | Lua 编辑器活动。 |
| `LuaContext.java` | Lua 上下文接口，定义 Lua 执行环境。 |
| `LuaDexClassLoader.java` | Lua Dex 类加载器，用于加载 Lua 编译后的 Dex。 |
| `LuaDexLoader.java` | Lua Dex 加载器。 |
| `LuaResources.java` | Lua 资源管理。 |
| `Http.java` | Lua 中的 HTTP 请求支持。 |
| `JsonUtil.java` | Lua 中的 JSON 处理。 |
| `GifDecoder.java` | GIF 解码器。 |
| `LoadingDrawable.java` | 加载动画 Drawable。 |
| `CrashHandler.java` | 崩溃处理器。 |
| `ImportProject.java` | 导入项目功能。 |
| `EditDialog.java` | 编辑对话框。 |
| `LuaBitmap.java` | Lua 中的位图处理。 |
| `LuaBitmapDrawable.java` | Lua 位图 Drawable。 |
| `LuaDrawable.java` | Lua Drawable 支持。 |
| `LuaLayout.java` | Lua 布局，允许用 Lua 定义 UI 布局。 |
| `LuaLexer.java` | Lua 词法分析器。 |
| `LuaTimer.java` | Lua 定时器。 |
| `LuaTimerTask.java` | Lua 定时任务。 |
| `LuaWallpaperService.java` | Lua 壁纸服务。 |
| `LuaWebView.java` | Lua WebView 支持。 |
| `LuaNotificationListenerService.java` | Lua 通知监听服务。 |
| `LuaPreferenceFragment.java` | Lua 偏好设置 Fragment。 |
| `LuaAdapter.java` | Lua 适配器（列表适配器）。 |
| `LuaArrayAdapter.java` | Lua 数组适配器。 |
| `LuaMultiAdapter.java` | Lua 多类型适配器。 |
| `LuaExpandableListAdapter.java` | Lua 可展开列表适配器。 |
| `CallLuaFunction.java` | 调用 Lua 函数。 |
| `AsyncTaskX.java` | 扩展的异步任务。 |
| `LocaleComparator.java` | 区域比较器。 |
| `NineBitmapDrawable.java` | 九宫格位图 Drawable。 |
| `Ticker.java` | 定时器/时钟。 |
| `TimerX.java` | 扩展定时器。 |
| `TimerTaskX.java` | 扩展定时任务。 |
| `Main.java` | 主入口类。 |
| `Welcome.java` | 欢迎界面。 |
| `LuaGcable.java` | Lua 垃圾回收接口。 |
| `LuaClient.java` | Lua 客户端。 |
| `LuaServer.java` | Lua 服务器。 |
| `LuaEnhancer.java` | Lua 增强器（字节码增强）。 |
| `LuaAbstractMethodInterceptor.java` | 抽象方法拦截器。 |
| `LuaMethodInterceptor.java` | 方法拦截器。 |
| `LuaTokenTypes.java` | Lua 词法单元类型。 |
| `LuaUtil.java` | Lua 工具类。 |

### 3.2 proxy/ - 代理模式支持

| 文件名 | 功能说明 |
|--------|----------|
| `Enhancer.java` | 增强器，用于动态生成代理类。 |
| `EnhancerInterface.java` | 增强器接口。 |
| `MethodFilter.java` | 方法过滤器。 |
| `MethodInterceptor.java` | 方法拦截器接口。 |
| `MethodProxy.java` | 方法代理。 |
| `MethodProxyExecuter.java` | 方法代理执行器。 |
| `ProxyException.java` | 代理异常。 |
| `Const.java` | 常量定义。 |

**文件数量**：约 60+ 个 Java 文件

---

## 4. com.myopicmobile.textwarrior - 文本编辑器组件

一个功能丰富的文本编辑器组件，可能用于代码编辑或文本输入。

### 4.1 android/ - Android 特定实现

| 文件名 | 功能说明 |
|--------|----------|
| `FreeScrollingTextField.java` | 自由滚动文本字段，核心编辑器视图。 |
| `AutoCompletePanel.java` | 自动完成面板。 |
| `ClipboardPanel.java` | 剪贴板面板。 |
| `KeysInterpreter.java` | 按键解释器。 |
| `TouchNavigationMethod.java` | 触摸导航方法。 |
| `TrackpadNavigationMethod.java` | 触控板导航方法。 |
| `YoyoNavigationMethod.java` | Yoyo 导航方法。 |
| `OnSelectionChangedListener.java` | 选择变化监听器接口。 |
| `TextChangeListener.java` | 文本变化监听器接口。 |

### 4.2 common/ - 通用文本处理

| 文件名 | 功能说明 |
|--------|----------|
| `Document.java` | 文档模型，表示文本文档。 |
| `DocumentProvider.java` | 文档提供者接口。 |
| `TextBuffer.java` | 文本缓冲区。 |
| `TextBufferCache.java` | 文本缓冲区缓存。 |
| `AutoComplete.java` | 自动完成功能。 |
| `AutoIndent.java` | 自动缩进。 |
| `Language.java` | 语言定义基类。 |
| `LanguageC.java` | C 语言定义。 |
| `LanguageLua.java` | Lua 语言定义。 |
| `LanguageNonProg.java` | 非编程语言定义。 |
| `Lexer.java` | 词法分析器基类。 |
| `LexState.java` | 词法状态。 |
| `llex.java` | Lua 词法分析器。 |
| `LuaC.java` | Lua 编译器。 |
| `LuaParser.java` | Lua 解析器。 |
| `ColorScheme.java` | 颜色方案基类。 |
| `ColorSchemeDark.java` | 深色颜色方案。 |
| `ColorSchemeLight.java` | 浅色颜色方案。 |
| `Constants.java` | 常量定义。 |
| `Flag.java` | 标志位。 |
| `FuncState.java` | 函数状态（编译器用）。 |
| `InstructionPtr.java` | 指令指针。 |
| `IntPtr.java` | 整数指针。 |
| `LinearSearchStrategy.java` | 线性搜索策略。 |
| `Pair.java` | 键值对。 |
| `PackageUtil.java` | 包工具。 |
| `ReadTask.java` | 读取任务。 |
| `RowListener.java` | 行监听器。 |
| `SearchStrategy.java` | 搜索策略。 |
| `TextWarriorException.java` | 文本编辑器异常。 |
| `UndoStack.java` | 撤销栈。 |
| `WriteTask.java` | 写入任务。 |

**文件数量**：约 40+ 个 Java 文件

---

## 5. com.nirenr - 辅助工具包

提供一些辅助功能和工具类。

### 5.1 根目录文件

| 文件名 | 功能说明 |
|--------|----------|
| `Color.java` | 颜色工具类。 |
| `ColorFinder.java` | 颜色查找器。 |
| `ColorPoint.java` | 颜色点。 |
| `Point.java` | 点坐标类。 |
| `SplitEditView.java` | 分割编辑视图。 |

### 5.2 screencapture/ - 截屏功能

| 文件名 | 功能说明 |
|--------|----------|
| `ScreenCaptureActivity.java` | 截屏活动。 |
| `ScreenCaptureListener.java` | 截屏监听器。 |
| `ScreenShot.java` | 截屏功能实现。 |
| `FileUtil.java` | 文件工具（截屏保存用）。 |

**文件数量**：约 8 个 Java 文件

---

## 6. org.luaj - LuaJava 实现

完整的 Lua 解释器实现，允许在 Java 上运行 Lua 脚本。

### 6.1 根目录 - Lua 核心

| 文件名 | 功能说明 |
|--------|----------|
| `Lua.java` | Lua 脚本执行入口。 |
| `LuaValue.java` | Lua 值基类，所有 Lua 数据类型的基类。 |
| `LuaTable.java` | Lua 表，实现 Lua 的 table 类型。 |
| `LuaFunction.java` | Lua 函数基类。 |
| `LuaClosure.java` | Lua 闭包，实现 Lua 的函数闭包。 |
| `LuaString.java` | Lua 字符串。 |
| `LuaNumber.java` | Lua 数字基类。 |
| `LuaInteger.java` | Lua 整数。 |
| `LuaDouble.java` | Lua 双精度浮点数。 |
| `LuaBoolean.java` | Lua 布尔值。 |
| `LuaNil.java` | Lua nil 值。 |
| `LuaUserdata.java` | Lua 用户数据。 |
| `LuaThread.java` | Lua 线程（协程）。 |
| `LuaMetaTable.java` | Lua 元表。 |
| `LuaError.java` | Lua 错误/异常。 |
| `LuaSyntaxError.java` | Lua 语法错误。 |
| `Globals.java` | Lua 全局环境。 |
| `Prototype.java` | Lua 函数原型（编译后的字节码）。 |
| `LocVars.java` | 局部变量信息。 |
| `Upvaldesc.java` | Upvalue 描述。 |
| `UpValue.java` | Upvalue（闭包捕获的变量）。 |
| `Varargs.java` | 可变参数。 |
| `TailcallVarargs.java` | 尾调用可变参数。 |
| `WeakTable.java` | 弱引用表。 |
| `Metatable.java` | 元表接口。 |
| `NonTableMetatable.java` | 非表元表。 |
| `OrphanedThread.java` | 孤立线程。 |
| `Print.java` | 打印功能。 |
| `LoadState.java` | 加载状态（二进制块加载）。 |
| `Buffer.java` | 缓冲区。 |

### 6.2 compiler/ - Lua 编译器

| 文件名 | 功能说明 |
|--------|----------|
| `LuaC.java` | Lua 编译器主类。 |
| `LexState.java` | 词法分析器状态。 |
| `FuncState.java` | 函数编译状态。 |
| `InstructionPtr.java` | 指令指针。 |
| `IntPtr.java` | 整数指针。 |
| `Constants.java` | 常量定义。 |
| `DumpState.java` | 转储状态（输出编译结果）。 |

### 6.3 lib/ - Lua 标准库

| 文件名 | 功能说明 |
|--------|----------|
| `BaseLib.java` | 基础库（print, type, tostring 等）。 |
| `MathLib.java` | 数学库。 |
| `StringLib.java` | 字符串库。 |
| `TableLib.java` | 表操作库。 |
| `IoLib.java` | I/O 库。 |
| `OsLib.java` | 操作系统库。 |
| `CoroutineLib.java` | 协程库。 |
| `DebugLib.java` | 调试库。 |
| `Bit32Lib.java` | 32 位位操作库。 |
| `Utf8Lib.java` | UTF-8 库。 |
| `LibFunction.java` | 库函数基类。 |
| `ZeroArgFunction.java` | 零参数函数。 |
| `OneArgFunction.java` | 单参数函数。 |
| `TwoArgFunction.java` | 双参数函数。 |
| `ThreeArgFunction.java` | 三参数函数。 |
| `VarArgFunction.java` | 可变参数函数。 |
| `ResourceFinder.java` | 资源查找器。 |

### 6.4 lib/jse/ - Java SE 平台适配

| 文件名 | 功能说明 |
|--------|----------|
| `JsePlatform.java` | JSE 平台初始化。 |
| `JseBaseLib.java` | JSE 基础库。 |
| `JseIoLib.java` | JSE I/O 库。 |
| `JseMathLib.java` | JSE 数学库。 |
| `JseOsLib.java` | JSE 操作系统库。 |
| `JseStringLib.java` | JSE 字符串库。 |
| `JseProcess.java` | JSE 进程管理。 |
| `LuajavaLib.java` | LuaJava 库（Java 互操作）。 |
| `CoerceJavaToLua.java` | Java 到 Lua 类型转换。 |
| `CoerceLuaToJava.java` | Lua 到 Java 类型转换。 |
| `JavaClass.java` | Java 类表示。 |
| `JavaInstance.java` | Java 实例。 |
| `JavaMethod.java` | Java 方法。 |
| `JavaConstructor.java` | Java 构造函数。 |
| `JavaArray.java` | Java 数组。 |
| `JavaList.java` | Java List。 |
| `JavaMap.java` | Java Map。 |
| `JavaMember.java` | Java 成员。 |
| `JavaPackage.java` | Java 包。 |

### 6.5 android/ - Android 平台适配

| 文件名 | 功能说明 |
|--------|----------|
| `call.java` | 调用功能。 |
| `file.java` | 文件操作。 |
| `http.java` | HTTP 请求。 |
| `json.java` | JSON 处理。 |
| `loadlayout.java` | 加载布局。 |
| `print.java` | 打印功能。 |
| `printf.java` | 格式化打印。 |
| `res.java` | 资源管理。 |
| `saf.java` | 存储访问框架。 |
| `task.java` | 任务。 |
| `thread.java` | 线程。 |
| `timer.java` | 定时器。 |

**文件数量**：约 90+ 个 Java 文件

---

## 总结

`app/src/main/java` 目录是 Trime 项目的核心代码库，总共有：

- **com.osfans.trime**: ~50 个文件，实现输入法核心功能
- **com.android.cglib**: ~120+ 个文件，Dex 工具和代码生成
- **com.androlua**: ~60+ 个文件，Lua 脚本支持
- **com.myopicmobile.textwarrior**: ~40+ 个文件，文本编辑器组件
- **com.nirenr**: ~8 个文件，辅助工具
- **org.luaj**: ~90+ 个文件，完整的 Lua 解释器

**总计**: 约 370+ 个 Java/Kotlin 文件

这个目录结构清晰地展示了 Trime 的架构层次：
1. **输入法核心层** (com.osfans.trime) - 处理输入法逻辑
2. **原生集成层** (core/Rime) - 与 RIME C++ 引擎通信
3. **脚本扩展层** (org.luaj, com.androlua) - 支持 Lua 脚本定制
4. **UI 组件层** (keyboard, candidate, theme) - 键盘和界面
5. **工具支持层** (util, cglib, textwarrior) - 各种工具和组件

---

*文档生成时间: 2026-05-03*  
*生成工具: opencode*
