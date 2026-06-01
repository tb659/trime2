name = "26键"
author = "tbagr"
lock = true
key_width = 10
-- key_background = "0xfffff000"
-- rows行键盘，宽度和高度为键盘总宽度的百分比
rows = {
    -- 第一行
    {
        -- text_size = 24,
        -- height = 60,
        keys = {
            --{ text_size = 24, click = "q", swipe_up = "1", long_click = "1", shift = { click = "Q", long_click = "！", ascii = { click = "Q", long_click = "!" } } },
            { click = "q", swipe_up = "1", long_click = "!", hint_up = { offset_x = -10 }, style_long_click = { offset_x = 10 } },
            { click = "w", swipe_up = "2", long_click = "@", hint_up = { offset_x = -10 }, style_long_click = { offset_x = 10 } },
            { click = "e", swipe_up = "3", long_click = "#", hint_up = { offset_x = -10 }, style_long_click = { offset_x = 10 } },
            { click = "r", swipe_up = "4", long_click = "$", hint_up = { offset_x = -10 }, style_long_click = { offset_x = 10 } },
            { click = "t", swipe_up = "5", long_click = "%", hint_up = { offset_x = -10 }, style_long_click = { offset_x = 10 } },
            { click = "y", swipe_up = "6", long_click = "^", hint_up = { offset_x = -10 }, style_long_click = { offset_x = 10 } },
            { click = "u", swipe_up = "7", long_click = "&", hint_up = { offset_x = -10 }, style_long_click = { offset_x = 10 } },
            { click = "i", swipe_up = "8", long_click = "*", hint_up = { offset_x = -10 }, style_long_click = { offset_x = 10 } },
            { click = "o", swipe_up = "9", long_click = "(){Left}", hint_up = { offset_x = -10 }, style_long_click = { offset_x = 10 } },
            { click = "p", swipe_up = "0", long_click = "~", hint_up = { offset_x = -10 }, style_long_click = { offset_x = 10 } },
        }
    },
    -- 第二行
    {
        keys = {
            { width = 5 },
            { click = "a", long_click = "select_all" },
            { click = "s" },
            { click = "d" },
            { click = "f" },
            { click = "g" },
            { click = "h", swipe_up = "-", long_click = "_", hint_up = { offset_x = -10 }, style_long_click = { offset_x = 10 } },
            { click = "j", swipe_up = "=", long_click = "+", hint_up = { offset_x = -10 }, style_long_click = { offset_x = 10 } },
            { click = "k", swipe_up = "[]{Left}", long_click = "{}{Left}", hint_up = { offset_x = -10 }, style_long_click = { offset_x = 10 } },
            { click = "l", swipe_up = "\\", long_click = "|", hint_up = { offset_x = -10 }, style_long_click = { offset_x = 10 } },
        }
    },
    -- 第三行
    {
        keys = {
            { click = "Shift_L", swipe_up = "Candidate_switch", swipe_down = "", hint_up = "", style = "functional", width = 15, composing = "delimiter" },
            { click = "z", swipe_up = "`", hint_up = { offset_x = -10 } },
            { click = "x", long_click = "cut" },
            { click = "c", long_click = "copy" },
            { click = "v", long_click = "paste" },
            { click = "b", swipe_up = "Keyboard_editor", hint_up = "", long_click = "Keyboard_clipboard", swipe_down = "Keyboard_phrase" },
            { click = "n", swipe_up = ";", long_click = ":", hint_up = { offset_x = -10 }, style_long_click = { offset_x = 10 } },
            { click = "m", swipe_up = "'", long_click = "\"", hint_up = { offset_x = -10 }, style_long_click = { offset_x = 10 } },
            { click = "BackSpace", style = "BackSpace", label = "⌫", width = 15, swipe_up = "delete_all" },
        }
    },
    -- 第四行
    {
        keys = {
            { click = "Keyboard_symbols", swipe_up = "Keyboard_settings", hint_up = "", swipe_right = "Schema_settings", hint_right = "", hint_long = "菜单", style = "functional", width = 15,
              ascii = { click = "Keyboard_symbols", swipe_up = "Keyboard_settings", hint_up = "", swipe_right = "Schema_settings", hint_right = "", hint_long = "菜单" } },
            { click = "Keyboard_number", swipe_up = "Keyboard_default_zhuji", hint_up = "", long_click = "Theme_settings", composing = "Select_five", popup = {"Theme_settings", "Color_switch"}, style = "functional" },
            { click = "，", ascii = ",", long_click = "<", composing = "Select_four", style = "text_size_14" },
            { click = "space", label = "schema_name", long_click="VOICE_ASSIST", width = 30, swipe_repeatable = true, swipe_left = "Left", swipe_right = "Right", swipe_up = "Up", swipe_down = "Down", repeat_click_time = 100 },
            { click = "。", ascii = ".", long_click = ">", composing = "Select_two", style = "text_size_14" },
            { click = "/", long_click = "?" , composing = "Select_three", style = "text_size_14" },
            { click = "Return", style = "enter", label = "Enter", width = 15, long_click = "" }

        }
    },
}
