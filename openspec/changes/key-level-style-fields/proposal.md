## Why

键盘按键目前支持 `width` / `height` / `style` 等字段，其中 `height` 已具备多级优先级（key → row → keyboard 配置 → 主题 key 样式）。但 `text_color`、`text_size`、`padding`、`margin`、`background` 等常用样式字段仅能从主题的 `key` 样式获取，无法在键盘 Lua 配置中按 key/row/keyboard 级别逐层覆盖，导致用户需要为细微的样式差异创建大量主题变体。

## What Changes

- 为 key 级别新增 `text_color`、`text_size`、`padding`、`margin`、`background` 字段，支持在按键 Lua 配置表中直接设置
- 为 row 级别新增 `background`、`padding`、`margin` 等行级样式字段
- 为 keyboard 级别（Lua 文件全局作用域）新增如 `key_text_color`、`key_text_size`、`key_background` 等默认值字段
- 建立四层优先级解析系统，与 `height` 的优先级机制一致：
  **key → row → keyboard 全局 → 主题 key 样式**
- 影响模块：`RowKeyboardView`、`AbsKeyboardView`、`KeyView`、`KeyStyle`

## Capabilities

### New Capabilities
- `key-style-priority`: 按键样式字段多级优先级解析系统，定义 color/size/padding/margin/bg 在 key → row → keyboard → theme 四层的查询与回退逻辑

### Modified Capabilities

无

## Impact

- `RowKeyboardView.java`: `loadKey()` 中创建 Key 时传递 row/keyboard 级默认值
- `AbsKeyboardView.java`: `loadKey()` 中创建 Key 时传递 keyboard 级默认值
- `KeyView.java`: `initView()` 中使用合并后的样式属性替代纯主题样式
- `KeyStyle.java`: 新增键盘级和行级样式合并逻辑
- 主题 Lua 文件：无需变更（现有 `key.text_color` 等作为最底层 fallback）
- 键盘 Lua 文件：新增可选字段（向后兼容，不设时行为不变）
