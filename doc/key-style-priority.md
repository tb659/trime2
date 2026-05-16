# 按键样式优先级系统

按键的样式属性（文字颜色、文字大小、背景色、内边距、外边距）现在支持与高度 `height` 一致的四层优先级：**按键级 > 行级 > 键盘级 > 主题级**。

## 优先级总览

```
按键自身字段 (text_color, background, ...)   ← 最高优先级
  │
行级默认值 (row.background, row.padding, ...)
  │
键盘级默认值 (key_text_color, key_background, ...)
  │
主题 key 样式 (style: key.text_color, ...)    ← 最低优先级
```

## 支持的字段

| 字段 | 含义 | 受支持级别 | 对应主题 key 字段 |
|------|------|-----------|-----------------|
| `text_color` | 文字颜色 | key, keyboard | `key.text_color` |
| `text_size` | 文字大小（sp） | key, keyboard | `key.text_size` |
| `background` | 背景色或图片 | key, row, keyboard | `key.background` |
| `padding` | 按键内边距 `{left,top,right,bottom}` | key, row, keyboard | `key.padding` |
| `margin` | 按键外边距 `{left,top,right,bottom}` | key, row, keyboard | `key.margins` |

> 注意：key/row/kbd 级使用 `margin`，主题级使用 `margins`（带 s），系统会自动映射。

## 第一级：按键级（Key Level）

在按键的 Lua 配置表中直接设置字段，优先级最高。

```lua
rows = {
  {
    keys = {
      { click = "q", text_color = 0xff0000ff, background = 0xffffff00 },
      { click = "w", text_size = 26 },
      { click = "e", padding = { left = 4, top = 4, right = 4, bottom = 4 },
                       margin = { left = 2, top = 1, right = 2, bottom = 2 } },
    }
  }
}
```

## 第二级：行级（Row Level）

在行表上设置字段，作为该行所有按键的默认值。当按键自身未设置时生效。

```lua
rows = {
  {
    background = 0xffeeeeee,  -- 该行所有按键的默认背景
    padding = { left = 2, top = 2, right = 2, bottom = 2 },
    margin = { left = 1, top = 1, right = 1, bottom = 2 },
    keys = {
      { click = "q" },  -- 使用行级背景
      { click = "w", background = 0xffffffff },  -- 覆盖行级背景
    }
  }
}
```

## 第三级：键盘级（Keyboard Level）

在键盘 Lua 文件的全局作用域中设置 `key_` 前缀变量，作为整个键盘的默认值。

```lua
-- keyboard.lua
name = "我的键盘"
key_width = 10
key_height = 60

-- 键盘级默认值（优先级高于主题）
key_text_color = 0xff333333
key_text_size = 20
key_background = 0xffdddddd
key_padding = { left = 2, top = 1, right = 2, bottom = 1 }
key_margin = { left = 1, top = 1, right = 1, bottom = 2 }

rows = {
  {
    keys = {
      { click = "q" },  -- 文字颜色来自 key_text_color
    }
  }
}
```

## 第四级：主题级（Theme Level）

最低优先级，使用主题样式文件中定义的 `key.*` 字段。仅在以上三级均未设置时生效。

```lua
-- themes/default/styles/light/main.lua
key = {
    text_color = 0xff000000,
    text_size = 22,
    background = 0xffffffff,
}
key.margins = { left = 2, top = 2, right = 2, bottom = 3 }
key.padding = { left = 0, top = 0, right = 0, bottom = 0 }
```

## 完整示例

```lua
-- theme key 样式（最低优先级）
-- 在主题样式文件中定义：
-- key.text_color = 0xff000000
-- key.text_size = 22

-- keyboard.lua（键盘级）
name = "示例键盘"
key_text_color = 0xff444444  -- 键盘级默认值
key_background = 0xffeeeeee

rows = {
  {
    -- 行级默认值
    background = 0xffcccccc,
    padding = { left = 2, top = 1, right = 2, bottom = 1 },
    keys = {
      { click = "q" },                          -- text_color = 0xff444444 (kbd级)
                                                -- background = 0xffcccccc (row级)
      { click = "w", text_color = 0xffff0000 }, -- text_color = 0xffff0000 (key级)
                                                -- background = 0xffcccccc (row级)
      { click = "e", background = 0xffffffff }, -- background = 0xffffffff (key级覆盖)
    }
  },
  {
    -- 第二行，无行级背景
    keys = {
      { click = "a" },                          -- text_color = 0xff444444 (kbd级)
                                                -- background = 0xffeeeeee (kbd级)
    }
  }
}
```

## 支持的键盘视图

该功能支持以下键盘视图：
- **RowKeyboardView**：使用 `rows` 配置的键盘
- **AbsKeyboardView**：使用 `width`/`height`/`x`/`y` 定位的键盘
- **FlexboxKeyboardView**：使用 `flex_box` 配置的键盘

> 注意：Flexbox 键盘目前仅支持键盘级（第三级）默认值，不支持行级（第二级）默认值。

## 向后兼容

所有新增字段均为可选项。未设置任何新字段的现有键盘 Lua 文件，行为与旧版本完全一致。
