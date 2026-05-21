-- 自定义浅拷贝合并函数
function table.merge(t1, t2)
  local result = {}
  for k, v in pairs(t1) do
    result[k] = v
  end
  for k, v in pairs(t2) do
    result[k] = v
  end
  return result
end

name = "可乐"
author = "tbagr"
-- 输入主颜色或图片
background = 0xffdddddd

-- 键盘
keyboard = {
    -- 键盘高度
    height = 240,
    -- 键盘背景颜色或图片
    background = 0xffdddddd,
    -- 指定样式全局字体或字体集
    -- font="b.ttf",
    font = "xr.ttf"
}

-- 默认按键样式
key = {
    -- 全局默认键高（dp），即 key_height 不存在时的兜底值
    height = 60,
    -- 按键文字颜色
    text_color = 0xff000000,
    -- 按键文字大小
    text_size = 24,
    -- 按键背景颜色或图片
    background = 0xffffffff,
    -- 按键阴影高度
    elevation = 4,
    -- 按键圆角半径
    corner_radius = 4,
    -- 按键阴影颜色
    shadow_color = 0xff000000,
    -- 按键长按超时
    long_click_time = 300,
    -- 按键重复执行间隔
    repeat_click_time = 200,
    -- 按键字体
    -- font="b.ttf"
    -- font={"a.ttf","b.ttf"}
    -- font = "JetBrainsMono-Medium.ttf",
    -- font = "kafei.ttf",
    -- 震动开关
    vibration_enabled = true,
    vibration_effect = {
        -- 震动时长
        {0, 12, 10, 20},
        -- 震动强度
        {0, 80, 0, 160}
    },
    -- 音效开关
    sound_enabled = true,
    -- 音效声音
    sound_effect = "click1.ogg",
    -- sound_effect = { "click1.ogg", "click2.ogg", "click3.ogg" }
    -- 按键四周留白
    margins = {
        left = 2,
        top = 5,
        right = 2,
        bottom = 5
    },
    padding = {
        left = 0,
        top = 0,
        right = 0,
        bottom = 2
    },
    -- 按键助记
    -- 在按键使用hint默认hint,或者使用hint_up,hint_down,hint_left,hint_right定义四个方向hint
    hint = {
        show = true,
        -- 助记文字颜色
        text_color = 0xff444444,
        -- 助记文字大小
        text_size = 10,
        up = {
            show = true,
            -- 助记文字颜色
            text_color = 0xff444444,
            -- 助记文字大小
            -- text_size = 8,
            -- 定义偏移
	    -- offset_x = -10,
        },
        down = {
            show = true,
            -- 助记文字颜色
            text_color = 0xff444444,
            -- 助记文字大小
            -- text_size = 8
        },
        left = {
            show = true,
            -- 助记文字颜色
            text_color = 0xff444444,
            -- 助记文字大小
            -- text_size = 8
        },
        right = {
            show = true,
            -- 助记文字颜色
            text_color = 0xff444444,
            -- 助记文字大小
            -- text_size = 8
        }
    },
    -- 按键长按
    long_click = {
        show = true,
        -- 长按文字颜色
        text_color = 0xff444444,
        -- 长按文字大小
        text_size = 10,
        -- 震动开关
        vibration_enabled = true,
        -- 定义位置
        -- gravity="top|left",
        -- 定义偏移
        -- offset_x = 10,
        -- offset_y = 10,
    },
    -- 按键按下状态
    pressed = {
        -- 宽度缩放
        scale_x = 0.9,
        -- 高度缩放
        scale_y = 0.9,
        -- 高度改变
        translation_z = 8,
        -- 水平移动
        translation_x = 0,
        -- 垂直移动
        translation_y = 0,
        -- 阴影颜色
        shadow_color = 0xff00ffff,
        -- 背景按键颜色或图片
        background = 0xff888888,
        text_color = 0xffffffff,
        -- 助记文字颜色
        hint = {
            text_color = 0xff444444,
        },
        -- 长按文字颜色
        long_click = {
            text_color = 0xff444444,
        }
    },
    -- 按键预览
    preview = {
        -- 宽度缩放
        scale_x = 0.9,
        -- 高度缩放
        scale_y = 0.9,
        -- 文字颜色
        text_color = 0xff000000,
        -- 文字大小
        text_size = 22,
        -- 按键背景颜色或图片
        background = 0xffffffff,
        -- 按键阴影高度
        elevation = 16,
        -- 按键圆角半径
        corner_radius = 4,
        -- 边框颜色
        stroke_color = 0x88dddddd,
        -- 边框宽度
        stroke_width = 1,
        -- 按键阴影颜色
        shadow_color = 0xffff0000
    }
}

-- 弹出键盘
popup = {
    -- 键盘阴影高度
    elevation = 16,
    -- 键盘圆角半径
    corner_radius = 4,
    -- 键盘背景颜色或图片
    background = 0xffdddddd,
    -- 边框颜色
    stroke_color = 0x88dddddd,
    -- 边框宽度
    stroke_width = 1,
    -- 键盘阴影颜色
    shadow_color = 0xff000000,
    key = table.merge(key, {
        -- 弹出键盘按键文字大小
        text_size = 12,
        -- 弹出键盘按键宽度
        width = 10,
        -- 弹出键盘按键高度
        height = 15,
        -- 弹出键盘按键禁止预览
        preview = nil
    })
}

-- 删除键
BackSpace = table.merge(key, {
    -- 按键重复执行间隔
    repeat_click_time = 50,
    -- 音效开关
    sound_enabled = true,
    -- 音效声音
    sound_effect = "del.ogg",
    -- 功能键文字大小
    text_size = 14,
    -- 功能键背景颜色或图片
    background = 0xffaaaaaa,
    -- 功能键按下背景颜色或图片
    pressed = table.merge(key.pressed, {
        background = 0xff888888,
        -- 功能键按下文字颜色
        text_color = 0xffffffff
    }),
})

-- 空格键
space = table.merge(key, {
    -- 空格键文字大小
    text_size = 12,
    -- 空格键长按超时
    long_click_time = 300,
    -- 音效开关
    sound_enabled = true,
    -- 音效声音
    sound_effect = "space.ogg"
})

-- 回车键样式，需要在回车键定义style="enter"
enter = table.merge(key, {
    -- 回车键文字大小
    text_size = 14,
    -- 回车键背景颜色或图片
    background = 0xff1976D2,
    -- 回车键按下背景颜色或图片
    pressed = table.merge(key.pressed, {
        background = 0xff1565C0,
        -- 回车键按下文字颜色
        text_color = 0xff000000
    }),
    -- 回车键禁止预览
    preview = nil,
    -- 音效开关
    sound_enabled = true,
    -- 音效声音
    sound_effect = "enter.ogg"
})

-- 数字键盘回车键
enter2 = table.merge(enter, {
    -- 数字键盘回车键圆角半径
    -- corner_radius = 4
    -- 数字键盘回车键禁止预览
    preview = nil,
    -- 音效开关
    sound_enabled = true,
    -- 音效声音
    sound_effect = "enter.ogg"
})

-- 功能键样式，需要在功能按键定义style="functional"
functional = table.merge(key, {
    -- 功能键文字大小
    text_size = 14,
    -- 功能键背景颜色或图片
    background = 0xffaaaaaa,
    -- 功能键按下背景颜色或图片
    pressed = table.merge(key.pressed, {
        background = 0xff888888,
        -- 功能键按下文字颜色
        text_color = 0xffffffff
    }),
    -- 功能键禁止预览
    preview = nil
})

-- 文字大小
text_size_14 = table.merge(key, {
    text_size = 14,
})

-- 符号更多面板
symbol = {
    -- 面板背景颜色或图片
    background = 0xffdddddd,
    -- 面板文字大小
    text_size = 22,
    -- 面板文字颜色
    text_color = 0xff000000,
    indicator_color = 0xFF0055FF,
    text = table.clone(key),
    -- 符号更多面板工具栏
    key = {
        -- 按键文字颜色
        text_color = 0xff000000,
        -- 按键文字大小
        text_size = 14,
        -- 按键背景颜色或图片
        background = 0xffeeeeee,
        -- 按键阴影高度
        elevation = 2,
        -- 按键圆角半径
        corner_radius = 4,
        -- 按键阴影颜色
        shadow_color = 0x800000ff,
        -- 符号更多面板工具栏按键按下状态
        pressed = {
            -- 宽度缩放
            scale_x = 0.9,
            -- 高度缩放
            scale_y = 0.9,
            -- 高度改变
            translation_z = -1,
            -- 水平移动
            translation_x = 0,
            -- 垂直移动
            translation_y = 0,
            -- 阴影颜色
            shadow_color = 0xff00ffff,
            -- 背景颜色或图片
            background = 0xffaaaaaa,
        }
    },
    -- 符号更多面板工具栏
    tool_bar = {
        -- 位置 left,top,right,bottom
        gravity = "right",
        keys = { "hide", "page_up", "page_down", "BackSpace" }
    }
}

-- 候选栏面板样式
candidate = {
    -- 候选面板高度
    height = 48,
    -- 背景颜色或图片
    background = 0xffdddddd,
    -- 候选文字大小
    text_size = 16,
    -- 候选文字颜色
    text_color = 0xff000000,
    -- 阴影高度
    elevation = 2,
    -- 阴影颜色
    shadow_color = 0xff000000,
    -- 候选栏面板按下状态
    pressed = {
        -- 背景色
        background = 0x44888888,
        -- 文字颜色
        text_color = 0xff000000,
        -- 圆角半径
        corner_radius = 0,
    },
    -- 候选栏面板助记
    comment = {
        -- 助记文字大小
        text_size = 12,
        -- 助记文字颜色
        text_color = 0xff444444,
        -- 候选栏面板助记按下状态
        pressed = {
            -- 助记文字大小
            text_size = 12,
            -- 助记文字颜色
            text_color = 0xff444444
        }
    },
    -- 候选栏按键
    key = {
        -- 文本
        text = "▽",
        -- 文字颜色
        text_color = 0xff000000,
        -- 文字大小
        text_size = 12,
        -- 背景颜色或图片
        background = 0xffdddddd,
        -- 阴影高度
        elevation = 0,
        -- 圆角半径
        corner_radius = 4,
        -- 按键阴影颜色
        shadow_color = 0x800000ff,
        -- 候选按键按下状态
        pressed = {
            -- 宽度缩放
            scale_x = 0.9,
            -- 高度缩放
            scale_y = 0.9,
            -- 高度改变
            translation_z = 2,
            -- 水平移动
            translation_x = 0,
            -- 垂直移动
            translation_y = 0,
            -- 阴影颜色
            shadow_color = 0xff00ffff,
            -- 背景颜色或图片
            background = 0xffaaaaaa,
        }
    },
    -- 候选栏展开面板样式
    expanded = {
        -- 背景颜色或图片
        background = 0xffdddddd,
        -- 文字大小
        text_size = 14,
        -- 文字颜色
        text_color = 0xff000000,
        -- 候选栏展开面板笔画筛选栏
        filter_bar = {
            -- 位置 left,top,right,bottom
            gravity = "bottom",
            -- 设置为false隐藏
            show = true
        },
        -- 候选栏展开面板工具栏
        tool_bar = {
            -- 位置 left,top,right,bottom
            gravity = "right",
            keys = { "hide", "page_up", "page_down", "char_filter", "BackSpace" }
        },
        -- 候选栏展开面板按下状态
        pressed = {
            -- 背景颜色
            background = 0xffffffff,
            -- 水波纹颜色
            ripple_color = 0x40000000,
        },
        -- 候选栏展开面板助记
        comment = {
            -- 文字大小
            text_size = 12,
            -- 文字颜色
            text_color = 0xff444444
        },
        -- 候选栏展开面板按键
        key = {
            -- 按键文字颜色
            text_color = 0xff000000,
            -- 按键文字大小
            text_size = 14,
            -- 按键背景颜色或图片
            background = 0xffeeeeee,
            -- 按键阴影高度
            elevation = 2,
            -- 按键圆角半径
            corner_radius = 4,
            -- 按键阴影颜色
            shadow_color = 0x800000ff,
            -- 候选栏展开面板按键按下状态
            pressed = {
                -- 宽度缩放
                scale_x = 0.9,
                -- 高度缩放
                scale_y = 0.9,
                -- 高度改变
                translation_z = -1,
                -- 水平移动
                translation_x = 0,
                -- 垂直移动
                translation_y = 0,
                -- 阴影颜色
                shadow_color = 0xff00ffff,
                -- 背景颜色或图片
                background = 0xffaaaaaa,
            }
        }
    }
}

-- 剪贴板样式
clipboard = table.merge(candidate.expanded, {
    item = table.merge(key, {
        -- 剪切板剪切项文字大小
        text_size = 14,
        -- 剪切板剪切项内边距
        padding = {
            left = 4,
            top = 4,
            right = 4,
            bottom = 4
        }
    }),
    -- 剪切板工具栏
    tool_bar = {
        -- 位置 left,top,right,bottom
        gravity = "right",
        keys = { "hide", "page_up", "page_down", "undo", "redo" }
    }
})

-- 工具栏样式
toolbar = table.merge(candidate, {
    -- 显示方案定义的开关
    schema_switches = true,
    hide = table.clone(candidate.key),
    -- 支持添加preset_keys按键，也可以直接写事件的表，
    -- 可以指定按键的style
    keys = { { label = "菜单", send = "Control+grave" }, "Mode_switch", "Keyboard_clipboard", "Keyboard_editor", "Mode_small", "Mode_float" },
    key = table.merge(candidate.key, {
        -- 工具栏文字大小
        text_size = 16,
        -- 工具栏内边距
        padding = {
            left = 8,
            top = 0,
            right = 8,
            bottom = 0
        }
    })
})

-- 预编辑提示区样式
preedit = {
    -- 文字大小
    text_size = 18,
    -- 按键文字颜色
    text_color = 0xff222222,
    -- 背景颜色或图片
    background = 0xaaffffff
}

-- 输入状态
composition = {
    -- 文字颜色
    text_color = 0xff222222,
    -- 背景颜色或图
    background = 0xaaffffff,
    -- 位置：left|right|left_up|right_up|fixed|bottom_left|bottom_right|top_left|top_right(left、right需要>=Android5.0)
    position = "fixed",
    -- 所有满足条件的词语都显示在窗口
    all_phrases = false,
    -- 边框宽度
    border = 2,
    -- 固顶行数
    sticky_lines = 0,
    -- 最大词条数,-1表示显示全部
    max_entries = -1,
    -- 最大长度,超过字数则换行
    max_length = 10,
    -- 最小长度,超过的字数显示在悬浮窗
    min_length = 8,
    -- 最大宽度，超过则自动换行
    max_width = 330,
    -- 最小宽度
    min_width = 40,
    -- 最大高度
    max_height = 400,
    -- 最小高度
    min_height = 0,
    -- 内边距
    padding = {
        left = 5,
        top = 5,
        right = 5,
        bottom = 5
    },
    -- 候选词的行间距(px)
    line_spacing = 0,
    -- 候选词的行间距(倍数)
    line_spacing_multiplier = 1,
    -- 与预编辑或边缘的距离
    spacing = 1,
    -- 窗口圆角
    round_corner = 4,
    -- 阴影
    elevation = 5,
    -- 是否可移动窗口，或仅移动一次 true|false|once
    movable = "false",
    -- 按下状态
    pressed = {
        -- 文字颜色
        text_color = 0xff222222,
        -- 背景颜色或图片
        background = 0xcccccccc
    },
    window = {
        --  悬浮窗口组件
        {
            start = "",
            move = "✎ ",
            ["end"] = ""
        },
        {
            start = "",
            composition = "%s",
            ["end"] = "",
            letter_spacing = 0
        },
        {
            start = "",
            label = "%s.",
            candidate = "%s",
            comment = " %s",
            ["end"] = "",
            sep = ""
        }
    }
}

-- 总高度
height = keyboard.height + candidate.height
