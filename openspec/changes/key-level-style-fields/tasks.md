## 1. 基础设施：优先级链构建工具

- [x] 1.1 在 `ThemeManager` 或新建工具类中实现 `buildKeyStyleChain(key, row, globals)`：创建 metatable 链 `key → rowDefaults → kbdDefaults`
- [x] 1.2 定义常量映射：`KBD_PREFIX_FIELDS`（keyboard 级字段如 `key_text_color`→`text_color`），以及 row 级支持的字段列表
- [x] 1.3 处理 `margin` → `margins` 的命名映射（Lua 中 key/row/keyboard 用 `margin`，Theme 用 `margins`）

## 2. RowKeyboardView：行布局键盘的优先级链接入

- [x] 2.1 在 `RowKeyboardView.loadRows()` 的 dp 模式循环中，为每行构建 row 级默认值表（读取 `row.background`, `row.padding`, `row.margin`）
- [x] 2.2 在 `RowKeyboardView.loadKey()` 中调用 `buildKeyStyleChain()` 构建完整优先级链，将包装后的 key 表传入 `new Key()`
- [x] 2.3 验证百分比模式无需改动（百分比不涉及 dp 样式字段，但 keyboard 级默认值仍需传递）

## 3. AbsKeyboardView：绝对定位键盘的优先级链接入

- [x] 3.1 在 `AbsKeyboardView.loadRows()` 的循环中，为每个 key 构建 keyboard 级默认值表（无 row 级）
- [x] 3.2 在 `AbsKeyboardView.loadKey()` 中调用 `buildKeyStyleChain()`，将包装后的 key 表传入 `new Key()`

## 4. KeyView：消费优先级链

- [x] 4.1 修改 `KeyView` 构造函数：在 `getKeyStyle(v.getStyle(), ...)` 之前，检查 key Lua 表是否有 `__style` 字段，确保 `KeyStyle` 构造时使用增强后的表。修复 `mMk == null` 时的 NPE。
- [x] 4.2 `initView()` 中的样式属性通过 KeyStyle 的 __index 链正确解析；`hasKey` 改用 `mTable.get()`（遵循 __index 链），子样式（preview/hint/long_click）正常查找
- [x] 4.3 `hasKey` 改为遵循 __index 链后，`preview`、`hint`、`long_click` 等子样式可通过回退链找到，所有现有调用者行为无变化

## 5. 验证与兼容性

- [x] 5.1 编译项目，确保无错误（两次编译均通过）
- [x] 5.2 验证现有键盘 Lua（无新字段）行为与修改前一致（__style 不存在时走原始路径）
- [x] 5.3 FlexboxKeyboardView 等未添加 __style 下发的键盘视图行为不变（`v.getMk().get("__style")` 返回 nil，走原始路径）
- [ ] 5.4 在 qwerty26.lua 等键盘中试用 key 级 `text_color`，确认正确渲染
- [ ] 5.5 在键盘配置中添加 `key_text_color` 等全局变量，验证 keyboard 级默认值生效
