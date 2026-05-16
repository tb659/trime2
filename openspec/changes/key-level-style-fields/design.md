## Context

键盘按键样式属性目前存在三条独立的解析路径：

1. **高度 (height)**：通过 `key.height` → `row.height` → `key_height` (键盘 Lua 全局) → 主题 `key.height` 四层优先级解析，代码实现在 `RowKeyboardView.loadKey()` / `AbsKeyboardView.loadKey()` 中
2. **按键名命样式 (style)**：通过 `key.style` 字段引用主题中已命名的样式表（如 `style = "functional"`），回退到主题 `key` 默认样式，代码在 `KeyView` 构造函数中
3. **子样式覆盖 (style_long_click / style_hint)**：通过 `key.style_long_click` 或 `key.style_hint` 直接在 key 表中定义内联样式，回退到主题对应子样式，代码在 `KeyView` 的 `setLongClickText()` / `setHintText()` 中

当前方案的问题是：`text_color`、`text_size`、`background`、`padding`、`margin` 等字段只能通过路径 2（主题命名样式）或路径 3（内联子样式）设置，缺少类似 height 的 key → row → keyboard → theme 多级优先级链。用户无法在键盘 Lua 中通过 `rows[i].keys[j].text_color` 或 `rows[i].background` 等方式逐层覆盖，也没有 keyboard 级别的 `key_text_color` 默认值。

## Goals / Non-Goals

**Goals:**

- 为 key 级别的 `text_color`、`text_size`、`background`、`padding`、`margin` 字段建立与 height 一致的四层优先级解析
- 支持 row 级别设置 `background`、`padding`、`margin`（行背景和内边距）
- 支持 keyboard 级别（Lua 全局作用域）设置 `key_text_color`、`key_text_size`、`key_background`、`key_padding`、`key_margin` 作为整键盘的样式默认值
- 保持向后兼容：未设置新字段的现有键盘 Lua 文件行为完全不变
- 最小化修改范围：不修改 `Key.java` 数据模型，不修改主题系统

**Non-Goals:**

- 不新增主题侧的新字段或新机制
- 不修改 `height` 本身的优先级逻辑
- 不引入运行时动态样式切换
- 不修改 `FlexboxKeyboardView`（仅行布局和绝对布局键盘受支持）

## Decisions

### Decision 1: 通过 Lua metatable 实现优先级链

**方案**：为每个 key 的 Lua 表构建 metatable 链，利用 LuaJ 的 Metatable `__index` 机制自动解析属性优先级。

链结构：
```
key表 → row上下文表 → keyboard全局默认表 → 主题 key 样式
```

优先级顺序：
1. `key.text_color` — key 表自身的字段
2. `row._key_style.text_color` / `row.text_color` — row 上下文
3. `keyboard_text_color` — keyboard 全局作用域（Lua 文件中的 `key_text_color`）
4. 主题 `key.text_color` — 主题风格定义

**为什么用 metatable 而非手动优先读取**

- 与 LuaJ 的现有机制一致（Style 已经在用 `__index` 做样式继承）
- 无需为每个属性写重复的 if-else 回退逻辑
- 自然支持嵌套表结构（如 `padding.left`）
- 新增属性时无需修改解析代码

**为什么不修改 Key.java**

Key.java 是按键数据模型，职责是存储事件和属性，不应对样式解析负责。将优先级链构建放在 View 层（`RowKeyboardView.loadKey()`）更符合关注点分离。

### Decision 2: 定义清晰的字段映射

| 概念 | Key 级 | Row 级 | Keyboard 级 | Theme 级 |
|------|--------|--------|-------------|----------|
| 文字颜色 | `text_color` | — | `key_text_color` | `key.text_color` |
| 文字大小 | `text_size` | — | `key_text_size` | `key.text_size` |
| 背景 | `background` | `background` | `key_background` | `key.background` |
| 外边距 | `margin` (table) | `margin` (table) | `key_margin` (table) | `key.margins` |
| 内边距 | `padding` (table) | `padding` (table) | `key_padding` (table) | `key.padding` |

注意 Theme 级使用 `margins` 而其他层级使用 `margin`（更直观），在合并时做映射。

Row 级的 `text_color` / `text_size` 语义不清晰（行没有自己的文字），因此不支持 Row 级的文字属性。

### Decision 3: 在 RowKeyboardView.loadKey 构建上下文

```java
private LuaTable buildKeyContext(LuaTable key, LuaTable row, Globals globals) {
    // Row 级默认值表
    LuaTable rowDefaults = new LuaTable();
    LuaValue rowBg = row.get("background");
    if (!rowBg.isnil()) rowDefaults.set("background", rowBg);
    LuaValue rowPadding = row.get("padding");
    if (rowPadding.istable()) rowDefaults.set("padding", rowPadding);
    // ... margin
    
    // Keyboard 级默认值表
    LuaTable kbdDefaults = new LuaTable();
    for (String field : KBD_LEVEL_FIELDS) {
        LuaValue v = globals.get(KBD_PREFIX + field);
        if (!v.isnil()) kbdDefaults.set(field, v);
    }
    
    // 构建 metatable 链: key → rowDefaults → kbdDefaults
    LuaTable kbdMeta = new LuaTable();
    kbdMeta.set("__index", kbdDefaults);
    rowDefaults.setmetatable(kbdMeta);
    
    LuaTable rowMeta = new LuaTable();
    rowMeta.set("__index", rowDefaults);
    key.setmetatable(rowMeta);  // 慎重：修改了原始 key 表
    
    return key;
}
```

### Decision 4: KeyStyle 构造时注入主题 fallback

`KeyView` 构造时已有 `mKeyStyle = ThemeManager.getStyle().getKeyStyle(v.getStyle(), themeKeyStyle)`。修改后：
在 `getKeyStyle(v.getStyle(), ...)` 之前，先为 key 的 `mMk` 表设置 metatable 链，使得后续 `KeyStyle` 在查询属性时自然遵循优先级。

```java
// KeyView 构造函数中
LuaValue mk = v.getMk();
if (mk.istable()) {
    LuaTable mkTable = mk.checktable();
    // mkTable 此时已有 metatable 链（在 loadKey 中设置）
    // 直接创建 KeyStyle，链中的 keyboard/row 默认值会优先于 theme fallback
    mKeyStyle = new KeyStyle(mkTable, mkTable);
    // 再通过 style 名称覆盖
    mKeyStyle = ThemeManager.getStyle().getKeyStyle(v.getStyle(), mKeyStyle);
}
```

这里需要确保 `KeyStyle` 构造时的 fallback 链正确：key 自身的属性通过 metatable 链解析，最终回退到 theme。

## Risks / Trade-offs

- **[侵入性] 修改 key Lua 表的 metatable** → 方案改为不直接修改原始 key 表，而是创建一个包装表 `KeyStyle.resolve(key, row, globals, theme)`，返回一个新的 LuaTable 作为 KeyStyle 的源
- **[性能] 每个按键都创建包装表** → 仅在 `loadKey()` 中创建一次，且仅在有样式覆盖的按键上创建；无覆盖时直接使用原始表。缓存可进一步优化。
- **[复杂度] 嵌套 metatable 链的层数** → 限制为 3 层（key → row → keyboard），theme 通过 KeyStyle 构造函数的 `def` 参数传递，不放入 metatable 链中
