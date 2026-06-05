name = "26键"
author = "tbagr"
lock = true
-- key_background = "0xfffff000"
-- rows行键盘，宽度和高度为键盘总宽度的百分比
rows = {
    -- 第一行
    {
        -- text_size = 24,
        -- height = 60,
        width = 9.8,
        keys = {
            { width = 1 },
            --{ text_size = 24, click = "q", swipe_up = "1", long_click = "1", shift = { click = "Q", long_click = "！", ascii = { click = "Q", long_click = "!" } } },
            { click = "q", swipe_up = "1", long_click = "!", hint_up = { offset_x = -10 }, long_click_style = { offset_x = 10 } },
            { click = "w", swipe_up = "2", long_click = "@", hint_up = { offset_x = -10 }, long_click_style = { offset_x = 10 } },
            { click = "e", swipe_up = "3", long_click = "#", hint_up = { offset_x = -10 }, long_click_style = { offset_x = 10 } },
            { click = "r", swipe_up = "4", long_click = "$", hint_up = { offset_x = -10 }, long_click_style = { offset_x = 10 } },
            { click = "t", swipe_up = "5", long_click = "%", hint_up = { offset_x = -10 }, long_click_style = { offset_x = 10 } },
            { click = "y", swipe_up = "6", long_click = "^", hint_up = { offset_x = -10 }, long_click_style = { offset_x = 10 } },
            { click = "u", swipe_up = "7", long_click = "&", hint_up = { offset_x = -10 }, long_click_style = { offset_x = 10 } },
            { click = "i", swipe_up = "8", long_click = "*", hint_up = { offset_x = -10 }, long_click_style = { offset_x = 10 } },
            { click = "o", swipe_up = "9", long_click = "(){Left}", hint_up = { offset_x = -10 }, long_click_style = { offset_x = 10 } },
            { click = "p", swipe_up = "0", long_click = "~", hint_up = { offset_x = -10 }, long_click_style = { offset_x = 10 } },
            { width = 1 },
        }
    },
    -- 第二行
    {
        width = 10.0857,
        keys = {
            { width = 4.61435, background = 0xff },
            { click = "a", long_click = "select_all" },
            { click = "s" },
            { click = "d" },
            { click = "f" },
            { click = "g" },
            { click = "h", swipe_up = "-", long_click = "_", hint_up = { offset_x = -10 }, long_click_style = { offset_x = 10 } },
            { click = "j", swipe_up = "=", long_click = "+", hint_up = { offset_x = -10 }, long_click_style = { offset_x = 10 } },
            { click = "k", swipe_up = "[]{Left}", long_click = "{}{Left}", hint_up = { offset_x = -10 }, long_click_style = { offset_x = 10 } },
            { click = "l", swipe_up = "\\", long_click = "|", hint_up = { offset_x = -10 }, long_click_style = { offset_x = 10 } },
            { width = 4.61435, background = 0xff },
        }
    },
    -- 第三行
    {
        width = 10.0857,
        keys = {
            { width = 1 },
            { click = "Shift_L", width = 13.7, swipe_up = "Candidate_switch", composing = "delimiter", style = "shift" },
            { click = "z", swipe_up = "`", hint_up = { offset_x = -10 } },
            { click = "x", long_click = "cut" },
            { click = "c", long_click = "copy" },
            { click = "v", long_click = "paste" },
            { click = "b", swipe_up = "Keyboard_editor", hint_up = "", long_click = "Keyboard_clipboard", swipe_down = "Keyboard_phrase" },
            { click = "n", swipe_up = ";", long_click = ":", hint_up = { offset_x = -10 }, long_click_style = { offset_x = 10 } },
            { click = "m", swipe_up = "'", long_click = "\"", hint_up = { offset_x = -10 }, long_click_style = { offset_x = 10 } },
            { click = "BackSpace", width = 13.7, swipe_up = "Clear", hint_up = "", swipe_left = "ClearH", hint_left = "", style = "backspace" },
            { width = 1 },
        }
    },
    -- 第四行
    {
        width = 10.0857,
        keys = {
            { width = 1 },
            { click = "Keyboard_symbols", width = 13.7, long_click = "Mode_switch", hint_long = "", swipe_up = "Keyboard_settings", hint_up = "", swipe_right = "Schema_settings", hint_right = "", style = "symbols" },
            { click = "Keyboard_number", swipe_up = "Keyboard_default_zhuji", hint_up = "", swipe_left = "Theme_settings", hint_left = "", swipe_right = "Color_switch", hint_right = "", composing = "Select_five" },
            { click = "，", ascii = ",", long_click = "<", composing = "Select_four", text_size = 14 },
            { click = "space", width = 30.2751, long_click="VOICE_ASSIST", swipe_left = "Left", swipe_right = "Right", swipe_up = "Up", swipe_down = "Down", style = "space" },
            { click = "。", ascii = ".", long_click = ">", composing = "Select_two", text_size = 14 },
            { click = "/", long_click = "?" , composing = "Select_three", text_size = 14 },
            { click = "Return", width = 13.7, label = "Enter", style = "enter" },
            { width = 1 },
        }
    },
}
