# Tasks: 为 com/osfans/trime 包添加详细注释

## 任务概览

为 `app/src/main/java/com/osfans/trime` 包下的所有 Java/Kotlin 文件添加详细注释，并将繁体中文注释转换为简体中文。

---

## Phase 1: 根目录文件注释

### 1.1 核心服务类
- [ ] **TrimeService.java** (1571行)
  - 添加类注释：说明这是输入法服务主类
  - 为所有字段添加注释
  - 为所有方法添加详细注释
  - 为关键代码行添加行注释
  - 转换所有繁体注释为简体

- [ ] **TrimeApplication.java** (95行)
  - 添加类注释
  - 为 onCreate、isStorageAvailable、unApk 等方法添加注释
  - 转换繁体注释

- [ ] **InputView.java**
  - 添加类注释：输入法主视图
  - 为键盘、候选词、预编辑等组件添加注释

- [ ] **RootInputView.java**
  - 添加类注释：根输入视图容器

### 1.2 基础功能类
- [ ] **Key.java** - 按键定义类
- [ ] **Composition.java** - 组合文本管理
- [ ] **Config.java** - 配置管理
- [ ] **Event.java** - 事件处理
- [ ] **JsonUtil.java** - JSON工具
- [ ] **PrefLauncher.java** - 偏好设置启动器
- [ ] **Speech.java** - 语音输入管理
- [ ] **VivoGpt.java** - Vivo GPT集成
- [ ] **ClipboardUtil.java** - 剪贴板工具

---

## Phase 2: candidate/ 候选词模块

- [ ] **CandidateView.java** - 候选词视图
- [ ] **CandidateAdapter.java** - 候选词适配器
- [ ] **CandidatesManager.java** - 候选词管理器
- [ ] **ExpandedCandidateView.java** - 展开式候选词视图
- [ ] **FlexboxCandidateAdapter.java** - Flexbox候选词适配器
- [ ] **FloatCandidateView.java** - 浮动候选词视图
- [ ] **FloatCandidateAdapter.java** - 浮动候选词适配器
- [ ] **ToolbarView.java** - 工具栏视图

---

## Phase 3: core/ 核心功能模块

### 3.1 RIME集成核心
- [ ] **Rime.java** (830行) - RIME核心接口
  - 重点注释：与librime的JNI调用
  - 输入法状态管理
  - 候选词获取逻辑
  - 按键处理流程

- [ ] **RimeApi.java** - RIME API接口定义
- [ ] **RimeConfig.java** - RIME配置管理
- [ ] **RimeProto.java** - RIME协议数据

### 3.2 消息和事件
- [ ] **RimeMessage.java** - RIME消息
- [ ] **RimeKeyEvent.java** - RIME按键事件
- [ ] **RimeKeyMap.java** - 按键映射
- [ ] **RimeDispatcher.java** - RIME消息分发器

### 3.3 生命周期管理
- [ ] **RimeLifecycle.java** - RIME生命周期接口
- [ ] **RimeLifecycleImpl.java** - RIME生命周期实现
- [ ] **RimeLifecycleOwner.java** - RIME生命周期所有者
- [ ] **RimeLifecycleUtils.java** - RIME生命周期工具

### 3.4 其他核心类
- [ ] **RimeSchema.java** - RIME方案管理
- [ ] **SchemaItem.java** - 方案项
- [ ] **CandidateItem.java** - 候选词项
- [ ] **DataManager.java** - 数据管理器
- [ ] **KeyModifier.java** - 按键修饰符
- [ ] **KeyModifiers.java** - 按键修饰符集合
- [ ] **KeyValue.java** - 键值
- [ ] **Flow.java** - 流处理
- [ ] **SharedFlowImpl.java** - 共享流实现

---

## Phase 4: data/ 数据管理模块

### 4.1 opencc/ 繁简转换
- [ ] **OpenCCDictManager.kt** - OpenCC词典管理器

#### dict/ 词典实现
- [ ] **Dictionary.kt** - 词典接口/基类
- [ ] **OpenCCDictionary.kt** - OpenCC词典实现
- [ ] **TextDictionary.kt** - 文本词典

### 4.2 userdict/ 用户词典
- [ ] **UserDictManager.kt** - 用户词典管理器

---

## Phase 5: dialog/ 对话框模块

- [ ] **DeployDialog.java** - 部署对话框
- [ ] **KeyboardDialog.java** - 键盘对话框
- [ ] **OptionsDialog.java** - 选项对话框
- [ ] **SchemaDialog.java** - 方案对话框
- [ ] **SchemaGroupDialog.java** - 方案组对话框
- [ ] **StyleDialog.java** - 样式对话框
- [ ] **ThemeDialog.java** - 主题对话框

---

## Phase 6: enums/ 枚举定义

- [ ] **InlineModeType.java** - 内联模式类型
- [ ] **KeyEventType.java** - 按键事件类型
- [ ] **WindowsPositionType.java** - 窗口位置类型

---

## Phase 7: keyboard/ 键盘模块

### 7.1 键盘视图
- [ ] **KeyboardView.java** (282行) - 键盘视图基类
- [ ] **AbsKeyboardView.java** - 抽象键盘视图
- [ ] **RowKeyboardView.java** - 行式键盘视图
- [ ] **FlexboxKeyboardView.java** - Flexbox键盘视图
- [ ] **FloatKeyboard.java** - 浮动键盘
- [ ] **ClipboardKeyboardView.java** - 剪贴板键盘视图
- [ ] **SymbolsKeyboardView.java** - 符号键盘视图
- [ ] **KeyView.java** - 按键视图
- [ ] **ModifierState.java** - 修饰键状态
- [ ] **TightTextView.java** - 紧凑文本视图

### 7.2 adapter/ 键盘适配器
- [ ] **ListPagerAdapter.java** - 列表分页适配器
- [ ] **LuaValueListAdapter.java** - Lua值列表适配器
- [ ] **WaterfallAdapter.java** - 瀑布流适配器

---

## Phase 8: speech/ 语音输入模块

- [ ] **Recognizer.java** - 语音识别器接口
- [ ] **RecognizerListener.java** - 识别监听器
- [ ] **VivoRecognizer.java** - Vivo语音识别器

---

## Phase 9: theme/ 主题模块

- [ ] **ThemeManager.java** - 主题管理器
- [ ] **Style.java** - 样式定义
- [ ] **KeyStyle.java** - 按键样式

---

## Phase 10: util/ 工具类模块

- [ ] **BackUtil.java** - 返回键工具
- [ ] **Cloud.java** - 云服务工具
- [ ] **Function.java** - 函数工具
- [ ] **HttpUtil.java** - HTTP工具

---

## 实施指南

### 每个文件的处理步骤
1. **读取文件**：使用 read 工具读取完整文件内容
2. **分析结构**：识别类、方法、字段等需要注释的元素
3. **添加注释**：
   - 文件头注释（如果缺失）
   - 类注释（在 class 声明之前）
   - 方法注释（在方法声明之前）
   - 字段注释（在字段声明处或上方）
   - 行注释（在关键代码行上方或右侧）
4. **转换繁体**：将繁体中文注释转换为简体中文
5. **写入文件**：使用 write 或 edit 工具保存修改后的文件
6. **验证**：确保文件格式正确，没有语法错误

### 注释规范
- 使用简体中文
- 保留英文技术术语
- 注释说明"为什么"而不是只说"是什么"
- 对于复杂逻辑，说明算法思路
- 对于特殊处理，说明原因和场景

### 注意事项
- **不改变代码逻辑**：只添加注释，不修改代码实现
- **保留版权信息**：文件头部的 SPDX 信息保留不变
- **保持格式**：保留代码的缩进、换行等格式
- **大文件分段**：对于大文件（如 TrimeService.java），分段处理避免遗漏

---

## 完成标准

- [ ] 所有 `com.osfans.trime` 包下的文件都已添加详细注释
- [ ] 所有繁体中文注释已转换为简体中文
- [ ] 代码逻辑未被修改（仅添加注释）
- [ ] 所有文件可以正常编译
