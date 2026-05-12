# 动态键盘高度

## 概述

传统 Trime 键盘高度由主题 `style.yaml` 中 `keyboard.height` 固定值决定，所有键盘共用同一高度，无法适应不同行数键盘的差异化需求。

动态键盘高度特性引入了 **dp 模式**，允许每个键盘独立定义行高（以 dp 为单位），总高度由各行的 dp 值累加计算。同时保留旧的百分比模式作为向后兼容的回落方案。

---

## 核心概念：双模式

系统通过 `ThemeManager.keyRowHeight(globals)` 方法自动检测采用哪种模式：

- **dp 模式**：当 Lua 中定义了 `key_height`（或主题中定义了 `key.height`）且值 > 0 时激活。键盘总高度 = 所有行 dp 值之和 × 缩放系数。
- **百分比模式**：当 `key_height` 不存在或为 0 时回落。键盘总高度 = 主题 `keyboard.height`，各按行等分（旧行为）。

---

## 配置方式

### dp 模式（推荐）

在键盘 Lua 文件中设置 `key_height`（单位：dp）：

```lua
-- 4 行键盘
key_height = 60

-- 5 行键盘
key_height = 54
```

各行的优先级链：`row.height` → `key_height` → 主题 `key.height`

**示例：各行自定义高度**

```lua
key_height = 60
rows = {
  { height = 70, keys = { ... } },   -- 第一行 70dp
  { height = 60, keys = { ... } },   -- 第二行 60dp
  { height = 50, keys = { ... } },   -- 第三行 50dp
  { height = 60, keys = { ... } },   -- 第四行 60dp
}
```

**按键级 dp 覆盖**：单个按键可设置 dp 高度覆盖该按键的行高：

```lua
{ click = "Return", height = 80, ... }  -- 此按键高 80dp，其余按键填满行高
```

### 百分比模式（旧行为）

不设置 `key_height` 即可。键盘高度来自主题 `keyboard.height`，各按键高度为百分比。

---

## 架构说明

### 数据流

```
Keyboard Lua 配置
    │
    ▼
ThemeManager.keyRowHeight(globals)  ← 检测 dp 模式
    │
    ├─ dp 模式 (返回值 > 0)
    │     ▼
    │   RowKeyboardView / AbsKeyboardView
    │   loadRows() 计算各行 dp 值并累加
    │       │
    │       ▼
    │   setComputedKeyboardHeight(px) → KeyboardView.mComputedKeyboardHeight
    │   ThemeManager.setComputedKeyboardHeight(px) → ThemeManager.sComputedKeyboardHeight
    │       │
    │       ▼
    │   InputView.setKeyboardView():
    │       getComputedKeyboardHeight() > 0 → 使用计算高度
    │
    └─ 百分比模式 (返回值 = 0)
          ▼
        RowKeyboardView / AbsKeyboardView
        loadRows() 清空 computedKeyboardHeight = 0
            │
            ▼
        ThemeManager.getKeyboardHeight() → 读取主题 keyboard.height
```

### 高度传递路径

1. **KeyboardView**（基类）：持有 `mComputedKeyboardHeight` 字段
2. **ThemeManager**：持有静态 `sComputedKeyboardHeight` 覆盖值
   - `getKeyboardHeight()`：优先返回 `sComputedKeyboardHeight`，否则读主题
   - `getRawContentHeight()`：优先使用 `sComputedKeyboardHeight`
   - `getContentHeight()` = `getCandidateHeight()` + `getKeyboardHeight()`
3. **InputView**: `setKeyboardView()` 中根据 `getComputedKeyboardHeight()` 更新 LayoutParams
4. **RootInputView**: `setKeyboard()` 中更新 `mCenterLayout` 高度

### 涉及的文件

| 文件 | 职责 |
|------|------|
| `ThemeManager.java` | `keyRowHeight()` 检测模式，`sComputedKeyboardHeight` 覆盖机制，`dp2px()` / `px2dp()` 工具方法 |
| `KeyboardView.java` | `mComputedKeyboardHeight` 字段 + getter/setter |
| `RowKeyboardView.java` | dp 模式下按行累加 dp 高度，`row.height` → `key_height` 优先链 |
| `AbsKeyboardView.java` | dp 模式下 `y` / `height` 为 dp 值，计算最远边界线作为总高度 |
| `InputView.java` | `setKeyboardView()` 根据计算高度更新容器 LayoutParams |
| `RootInputView.java` | `initView()` 时序调整（InputView 先于容器创建），`setKeyboard()` 更新容器高度 |

---

## 关键设计决策

1. **dp 模式检测**：`ThemeManager.keyRowHeight(globals) > 0` 是唯一入口，优先级链为 `key_height`（Lua）→ `key.height`（主题）。返回 0 则回落百分比模式。

2. **RowKeyboardView 行高累加**：dp 模式下行高来自 `row.height`（若存在）→ `key_height` → 主题 `key.height`。总高度 = sum(每行 dp) × 缩放系数。每行占百分比 = 该行 dp / 总 dp × 100。

3. **按键 height 语义变化**：dp 模式下，按键 `height` 是 dp 值，作为单个按键的高度覆盖（仅覆盖该按键，不参与行高计算）。无 `height` 的按键填满行高。百分比模式下 `height` 保持百分比语义。

4. **AbsKeyboardView 兼容**：dp 模式下 `y` 和 `height` 为 dp 值，`x` 和 `width` 保持百分比。百分比模式下保持旧行为。

5. **缓存切换安全**：切换回已缓存的键盘视图时，`mComputedKeyboardHeight` 持久存在。百分比模式路径显式清零避免遗留。

6. **初始化时序**：`RootInputView.initView()` 中 `InputView` 先于 `mCenterLayout` 创建，确保键盘视图首次加载时有正确的 `ThemeManager.sComputedKeyboardHeight`。

---

## 常见问题

### Q: 如何让某个键盘使用 dp 模式？
在键盘 Lua 文件中设置 `key_height = <dp值>`。建议 4 行键盘 60dp，5 行键盘 54dp。

### Q: 默认键盘的 key_height 值是多少？
| 键盘 | 行数 | key_height |
|------|------|-----------|
| qwerty26 / 27 | 4 | 60dp |
| qwerty36 | 5 | 54dp |
| ascii | 5 | 54dp |
| symbols | 5 | 54dp |

### Q: 如何创建百分比模式的键盘？
不在 Lua 中设置 `key_height` 即可。主题 `keyboard.height` 控制总高度。

### Q: 切换键盘时高度会闪烁吗？
所有容器高度在键盘切换时同步更新（`InputView.setKeyboardView()` + `RootInputView.setKeyboard()`），理论上不应闪烁。如有闪烁请检查候选词栏高度是否稳定。
