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

local defalut_font = { "kafei.ttf", "xr.ttf" }
local hint_font = { "TH-Feon.ttf", "xr.ttf" }
local key_color = 0xff525993

name = "可乐"
author = "tbagr"
-- 输入主颜色或图片
background = 0xffEBEBEB

-- 键盘
keyboard = {
    height = 240,
    background = 0xffEBEBEB,
    -- font = defalut_font,
}

-- 默认按键样式
key = {
    -- 全局默认键高（dp），即 key_height 不存在时的兜底值
    height = 52,
    -- 按键文字颜色
    text_color = key_color,
    -- 按键字体
    font = defalut_font,
    -- font={ "a.ttf","b.ttf" }
    -- 按键文字大小
    text_size = 20,
    -- 按键背景颜色或图片
    background = 0xffFFFFFF,
    -- 按键阴影高度
    elevation = 2,
    -- 按键圆角半径
    corner_radius = 6,
    -- 按键阴影颜色
    shadow_color = 0xff000000,
    -- 按键长按超时
    long_click_time = 300,
    -- 按键重复执行间隔
    repeat_click_time = 200,
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
    -- sound_effect = { "click.ogg", "click.ogg", "click.ogg" },
    sound_effect = "click.ogg",
    -- 按键音效律动模式
    --   "random"  从 sound_effect 数组中随机选一个音效,并从默认速率集 {0.8, 1.0, 1.2} 中随机选一个播放速率
    --   "rate"    固定选 sound_effect 数组的第一个音效,并从默认速率集 {0.8, 1.0, 1.2} 中随机选一个播放速率
    -- 也可以是 table 配置自定义速率集合：
    --   sound_rhythm = { mode = "random", params = { 0.7, 0.9, 1.0, 1.1, 1.3 } }
    sound_rhythm = "random",
    -- 按键四周留白
    margins = { left = 2, top = 5, right = 2, bottom = 5 },
    padding = { left = 0, top = 0, right = 0, bottom = 0 },
    -- 按键助记
    -- 在按键使用hint默认hint,或者使用hint_up,hint_down,hint_left,hint_right定义四个方向hint
    hint = {
        show = true,
        -- 助记文字颜色
        text_color = key_color,
        -- 助记字体
        font = hint_font,
        -- 助记文字大小
        text_size = 10,
        up = {
            show = true,
            -- 助记文字颜色
            -- text_color = 0xff444444,
            -- 助记文字大小
            -- text_size = 8,
            -- 定义偏移
            -- offset_x = -10,
        },
        down = {
            show = true,
        },
        left = {
            show = true,
        },
        right = {
            show = true,
        }
    },
    -- 按键长按
    long_click = {
        show = true,
        text_color = key_color,
        font = hint_font,
        text_size = 10,
        vibration_enabled = true,
        -- 定义位置
        -- gravity="top|left",
        -- offset_x = 10,
        -- offset_y = 10,
    },
    -- 按键按下状态
    pressed = {
        -- 宽度缩放
        scale_x = 1,
        -- 高度缩放
        scale_y = 1,
        -- 高度改变
        translation_z = 8,
        -- 水平移动
        translation_x = 0,
        -- 垂直移动
        translation_y = 0,
        -- shadow_color = 0xff00ffff,
        background = 0xff888888,
        text_color = 0xffffffff,
        hint = {
            text_color = key_color,
        },
        long_click = {
            text_color = key_color,
        }
    },
    -- 按键预览
    preview = {
        scale_x = 1,
        scale_y = 1,
        text_color = key_color,
        text_size = 30,
        font = defalut_font,
        background = 0xffffffff,
        elevation = 16,
        corner_radius = 6,
        stroke_color = 0x88dddddd,
        stroke_width = 1,
        shadow_color = 0xffff0000,
        boundary_margin_top = 0,
        boundary_margin_left = 4,
        boundary_margin_right = 4,
        padding = { left = 8, top = 0, right = 8, bottom = 0 },
    }
}

-- 弹出键盘
popup = {
    elevation = 16,
    corner_radius = 6,
    background = 0xffdddd00,
    stroke_color = 0x88dddddd,
    stroke_width = 1,
    shadow_color = 0xff000000,
    key = table.merge(key, {
        width = 10,
        height = 15,
        text_size = 12,
    })
}

-- 符号更多面板
symbol = {
    background = 0xffdddddd,
    text = table.clone(key),
    -- 符号更多面板工具栏
    tool_bar = {
        -- 位置 left,top,right,bottom
        gravity = "bottom",
        keys = { "hide", "page_up", "page_down", "BackSpace" }
    },
    -- 符号更多面板工具栏文字
    key = {
        text_color = 0xff000000,
        text_size = 14,
        background = 0xffeeeeee,
        elevation = 2,
        corner_radius = 6,
        shadow_color = 0x800000ff,
        -- 符号更多面板工具栏按键按下状态
        pressed = {
            scale_x = 0.9,
            scale_y = 0.9,
            translation_z = -1,
            translation_x = 0,
            translation_y = 0,
            -- shadow_color = 0xff00ffff,
            background = 0xffaaaaaa,
        }
    }
}

-- 候选栏面板样式
candidate = {
    -- 候选面板高度
    height = 48,
    min_width = 0,
    background = 0xffFFFFFF,
    text_size = 16,
    text_color = key_color,
    font = hint_font,
    elevation = 2,
    shadow_color = 0xff000000,
    corner_radius = 6,
    -- 工具栏容器外边距（与外部的间距）
    -- bottom 至少等于 elevation 才能避免阴影被裁剪
    margins = { left = 5, top = 0, right = 5, bottom = 2 },
    -- 候选栏面板按下状态
    pressed = {
        background = 0xff,
        text_color = 0xff7ECD65,
        -- corner_radius = 0,
        margins = { left = 0, top = 0, right = 0, bottom = 0 },
    },
    -- 候选栏面板助记
    comment = {
        text_size = 12,
        text_color = 0xff444444,
        pressed = {
            text_size = 12,
            text_color = 0xff444444
        }
    },
    -- 候选栏按键
    key = {
        width = 38,
        text = "﹀",
        text_color = key_color,
        text_size = 12,
        background = 0xff,
        elevation = 0,
        corner_radius = 6,
        shadow_color = 0x800000ff,
        margins = { left = 3, top = 3, right = 3, bottom = 0 },
        pressed = {
            scale_x = 0.9,
            scale_y = 0.9,
            translation_z = 2,
            translation_x = 0,
            translation_y = 0,
            -- shadow_color = 0xff00ffff,
            background = 0xffaaaaaa,
        }
    },
    -- 候选栏展开面板样式
    expanded = {
        background = 0xffdddddd,
        text_size = 14,
        text_color = 0xff000000,
        margins = { left = 3, top = 3, right = 3, bottom = 3 },
        -- 候选栏展开面板笔画筛选栏
        filter_bar = {
            -- 位置 left,top,right,bottom
            gravity = "bottom",
            show = true
        },
        -- 候选栏展开面板工具栏
        tool_bar = {
            -- 位置 left,top,right,bottom
            gravity = "right",
            keys = { "hide", "page_up", "page_down", "char_filter", "BackSpace" }
        },
        pressed = {
            background = 0xffffffff,
            ripple_color = 0x40000000,
        },
        -- 候选栏展开面板助记
        comment = {
            text_size = 12,
            text_color = key_color
        },
        -- 候选栏展开面板按键
        key = {
            text_color = key_color,
            text_size = 14,
            background = 0xffeeeeee,
            elevation = 2,
            corner_radius = 6,
            shadow_color = 0x800000ff,
            pressed = {
                scale_x = 1,
                scale_y = 1,
                translation_z = 0,
                translation_x = 0,
                translation_y = 0,
                -- shadow_color = 0xff00ffff,
                background = 0xffaaaaaa,
            }
        }
    }
}

-- 工具栏样式
toolbar = table.merge(candidate, {
    -- 显示方案定义的开关
    schema_switches = true,
    hide = table.clone(candidate.key),
    -- 支持添加preset_keys按键，也可以直接写事件的表，
    -- 可以指定按键的style
    -- keys = { { label = "菜单", send = "Control+grave" }, "Mode_switch", "Keyboard_clipboard", "Keyboard_editor", "Mode_small", "Mode_float" },
    key = table.merge(candidate.key, {
        text_size = 16,
        text_color = key_color,
        background = 0xff,
    })
})

-- 剪贴板样式
clipboard = table.merge(candidate.expanded, {
    item = table.merge(key, {
        text_size = 14,
        padding = { left = 4, top = 4, right = 4, bottom = 4 }
    }),
    -- 剪切板工具栏
    tool_bar = {
        -- 位置 left,top,right,bottom
        gravity = "right",
        keys = { "hide", "page_up", "page_down", "undo", "redo" }
    }
})

-- 预编辑提示区样式
preedit = {
    text_size = 18,
    text_color = 0xff222222,
    background = 0xaaffffff
}

-- 输入状态
composition = {
    text_color = 0xff222222,
    background = 0xaaffffff,
    -- 位置：left|right|left_up|right_up|fixed|bottom_left|bottom_right|top_left|top_right|hide(left、right需要>=Android5.0)
    position = "hide",
    -- 所有满足条件的词语都显示在窗口
    all_phrases = false,
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
    max_height = 400,
    min_height = 0,
    margins = { left = 5, top = 0, right = 5, bottom = 0 },
    -- 候选词的行间距(px)
    line_spacing = 0,
    -- 候选词的行间距(倍数)
    line_spacing_multiplier = 1,
    -- 与预编辑或边缘的距离
    spacing = 1,
    round_corner = 4,
    elevation = 5,
    -- 是否可移动窗口，或仅移动一次 true|false|once
    movable = "false",
    pressed = {
        text_color = 0xff222222,
        background = 0xcccccccc
    },
    window = {
        --  悬浮窗口组件
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

-- 按键点按和长按时的助记位置
hint_offset = table.merge(key, {
    hint = table.merge(key.hint, {
        show = true,
        text_size = 12,
        up = { offset_x = -10, offset_y = 0 },
        down = { offset_y = 0 },
    }),
    long_click = table.merge(key.long_click, {
        show = true,
        text_size = 12,
        offset_x = 10,
        offset_y = 0
    }),
})

-- shift键
key_shift = table.merge(key, {
    background = "kafei/xrsh.png",
    text_color = 0xff,
    hint = { show = false },
    pressed = table.merge(key.pressed, {
        text_color = 0xff,
        background = "kafei/xrshl.png",
    }),
    margins = { left = 0, top = 0, right = 0, bottom = 0 },
})

-- 删除键
key_backspace = table.merge(key, {
    text_color = 0xff,
    background = "kafei/xrsc.png",
    elevation = 4,
    margins = { left = 0, top = 0, right = 0, bottom = 0 },
    hint = { show = false },
    repeat_click_time = 50,
    sound_enabled = true,
    sound_effect = "del.ogg",
    pressed = table.merge(key.pressed, {
        text_color = 0xff,
        background = "kafei/xrsca.png",
    }),
})

-- 符号键
key_symbols = table.merge(key, {
    text_color = 0xff,
    background = "kafei/xrcd.png",
    margins = { left = 0, top = 0, right = 0, bottom = 0 },
    hint = { show = false },
    long_click = table.merge(key.long_click, {
        text_color = 0xff,
    }),
    pressed = table.merge(key.pressed, {
        text_color = 0xff,
        background = "kafei/xrcda.png",
    }),
})

-- 符号键
key_numbers = table.merge(key, {
    hint = { show = false },
})

-- 空格键
key_space = table.merge(key, {
    text_size = 14,
    hint = { show = false },
    long_click_time = 300,
    sound_enabled = true,
    sound_effect = "space.ogg",
    swipe_repeatable = true,
    repeat_click_time = 100,
})

-- 回车键
key_enter = table.merge(key, {
    text_size = 14,
    background = 0xff3C5AB0,
    text_color = 0xffFFFFFF,
    sound_enabled = true,
    sound_effect = "enter.ogg",
    pressed = table.merge(key.pressed, {
        text_color = 0xffFFFFFF,
        background = 0xff1565C0,
    })
})

-- 数字键盘返回键
num_back = table.merge(key, {
    text_color = 0xff,
    background = "kafei/xrszfh.png",
    margins = { left = 0, top = 0, right = 0, bottom = 0 },
    pressed = table.merge(key.pressed, {
        text_color = 0xff,
        background = "kafei/xrszfha.png",
    }),
})

-- 数字键盘删除键
num_backspace = table.merge(key, {
    text_color = 0xff,
    background = "kafei/xrszsc.png",
    elevation = 4,
    margins = { left = 0, top = 0, right = 0, bottom = 0 },
    hint = { show = false },
    repeat_click_time = 50,
    pressed = table.merge(key.pressed, {
        text_color = 0xff,
        background = "kafei/xrszsca.png",
    }),
})

-- 数字键盘空格键
num_space = table.merge(key, {
    text_color = 0xff,
    background = "kafei/xrszkg.png",
    elevation = 4,
    margins = { left = 0, top = 0, right = 0, bottom = 0 },
    hint = { show = false },
    repeat_click_time = 50,
    pressed = table.merge(key.pressed, {
        text_color = 0xff,
        background = "kafei/xrszkga.png",
    }),
})

-- 数字键盘回车键
num_enter = table.merge(key_enter, {
    text_color = 0xff,
    background = "kafei/xrszhc.png",
    elevation = 4,
    margins = { left = 0, top = 0, right = 0, bottom = 0 },
    hint = { show = false },
    repeat_click_time = 50,
    pressed = table.merge(key.pressed, {
        text_color = 0xff,
        background = "kafei/xrszhca.png",
    }),
})

-- 盲打键盘
touch_typing = table.merge(key, {
    text_color = 0xff,
    background = 0xffFFFFFF,
    hint = { show = false },
    long_click = { show = false },
})
