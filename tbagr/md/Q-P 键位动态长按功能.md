# Q-P 键位动态长按功能实现说明

## 功能概述

实现了 q-p 键位的长按行为根据 Shift 状态和 ASCII 模式动态切换的功能：

- **无 Shift（中文/英文模式）**：长按输出数字 1-0（从 `swipe_up` 配置获取）
- **有 Shift（英文模式）**：长按输出符号 !@#$%^&*()=（从 `swipe_down` 配置获取）
- **有 Shift（中文模式）**：长按输出中文符号 ！（从 `swipe_down` 配置获取）

## 配置文件示例

在用户自定义主题的 `qwerty26.lua` 文件中配置：

```lua
{ click = "q", swipe_up = "1", swipe_down = "!", long_click = "" },
{ click = "w", swipe_up = "2", swipe_down = "@", long_click = "" },
{ click = "e", swipe_up = "3", swipe_down = "#", long_click = "" },
{ click = "r", swipe_up = "4", swipe_down = "$", long_click = "" },
{ click = "t", swipe_up = "5", swipe_down = "%", long_click = "" },
{ click = "y", swipe_up = "6", swipe_down = "^", long_click = "" },
{ click = "u", swipe_up = "7", swipe_down = "&", long_click = "" },
{ click = "i", swipe_up = "8", swipe_down = "*", long_click = "" },
{ click = "o", swipe_up = "9", swipe_down = "(){Left}", long_click = "" },
{ click = "p", swipe_up = "0", swipe_down = "=", long_click = "" },
```

**关键配置项：**
- `click`: 短按时输出的字符（保持为字母 q-p）
- `swipe_up`: 无 Shift 时长按输出的内容（数字 1-0）
- `swipe_down`: 有 Shift 时长按输出的内容（符号）
- `long_click`: 设为空字符串（由代码动态决定使用 swipe_up 还是 swipe_down）
- `popup`: 长按弹出的键盘选项

## 技术实现

### 1. Key.java 新增方法

#### `getDynamicLongClick(boolean isShifted, boolean isAsciiMode)`
根据 Shift 状态和 ASCII 模式动态返回长按事件：
- 检测是否为 q-p 键位（单字母且在 'q'-'p' 范围内）
- 无 Shift 时返回 `swipe_up` 事件
- 有 Shift 时返回 `swipe_down` 事件
- 其他键位返回普通的 `long_click` 事件

#### `getDynamicLongClickLabel(boolean isShifted, boolean isAsciiMode)`
获取动态长按事件的标签文本，用于更新按键助记显示。

### 2. KeyView.java 修改

#### 长按处理逻辑修改
在 `mLongClickRunnable` 中：
```java
// 动态获取长按事件（根据 Shift 状态和 ASCII 模式）
boolean isShifted = ModifierState.isShifted();
boolean isAsciiMode = Rime.isAsciiMode();
Event dynamicLongClick = mKey.getDynamicLongClick(isShifted, isAsciiMode);

if (dynamicLongClick != null) {
    showPreview(true, dynamicLongClick.getLabel());
    TrimeService.getInstance().onEvent(dynamicLongClick);
    return;
}
```

#### 动态助记更新
新增 `updateDynamicLongClickHint()` 方法：
- 在 `invalidateKey()` 中调用
- 只对 q-p 键位进行更新
- 根据当前 Shift 状态和 ASCII 模式更新长按助记文本

### 3. 状态同步机制

当 Shift 状态变化时：
1. `TrimeService.setShifted()` 被调用
2. 触发 `KeyboardView.setShifted()`
3. 调用 `invalidateAllKeys()` 刷新所有按键
4. 每个 KeyView 的 `invalidateKey()` 被调用
5. `updateDynamicLongClickHint()` 更新 q-p 键位的助记显示

## 使用场景

### 场景 1：中文模式下输入数字
1. 确保处于中文模式（非 ASCII）
2. 确保 Shift 未按下
3. 长按 q-p 中的任意键
4. 输出对应的数字（1-0）

### 场景 2：英文模式下输入符号
1. 按一下 Shift 键（进入大写锁定或临时大写）
2. 长按 q-p 中的任意键
3. 输出对应的符号（!@#$%^&*()=）

### 场景 3：快速切换
- 无需切换输入法或键盘布局
- 通过 Shift 键即可在数字和符号之间切换
- 助记文本会实时更新，提示当前长按会输出什么

## 注意事项

1. **配置文件要求**：必须在 qwerty26.lua 中正确配置 `swipe_up` 和 `swipe_down` 字段
2. **long_click 留空**：建议将 `long_click` 设为空字符串，让代码动态决定
3. **仅适用于 q-p**：该功能只针对 q-p 这 10 个字母键位，其他键位不受影响
4. **弹出键盘**：如果配置了 `popup`，长按时会先显示弹出键盘，选择后才会触发事件

## 扩展建议

如果需要将此功能扩展到其他键位（如 a-l），可以修改 `Key.getDynamicLongClick()` 方法中的判断条件：

```java
// 原代码：只处理 q-p
if (ch >= 'q' && ch <= 'p') { ... }

// 扩展：处理所有字母键
if (ch >= 'a' && ch <= 'z') { ... }
```

但需要注意：
- 确保配置文件中所有目标键位都有 `swipe_up` 和 `swipe_down` 配置
- 考虑是否会影响现有的长按功能（如 a 键的 select_all）
