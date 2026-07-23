name = "26键"
author = "tbagr"

lock = true
-- background = 0xfffff000

offset_y = 3
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
            { click = "q", swipe_up = "1", long_click = "!", style = "hint_offset" },
            { click = "w", swipe_up = "2", long_click = "@", style = "hint_offset" },
            { click = "e", swipe_up = "3", long_click = "#", style = "hint_offset" },
            { click = "r", swipe_up = "4", long_click = "$", style = "hint_offset" },
            { click = "t", swipe_up = "5", long_click = "%", style = "hint_offset" },
            { click = "y", swipe_up = "6", long_click = "^", style = "hint_offset" },
            { click = "u", swipe_up = "7", long_click = "&", style = "hint_offset" },
            { click = "i", swipe_up = "8", long_click = "*", style = "hint_offset" },
            { click = "o", swipe_up = "9", long_click = "(){Left}", style = "hint_offset" },
            { click = "p", swipe_up = "0", long_click = "Hide", style = "hint_offset" },
            { width = 1 },
        }
    },
    -- 第二行
    {
        width = 10.0857,
        keys = {
            { width = 4.61435, background = 0xff },
            { click = "a", long_click = "select_all", style = "hint_offset" },
            { click = "s" },
            { click = "d" },
            { click = "f" },
            { click = "g" },
            { click = "h", swipe_up = "-", long_click = "_", style = "hint_offset" },
            { click = "j", swipe_up = "=", long_click = "+", style = "hint_offset" },
            { click = "k", swipe_up = "[]{Left}", long_click = "{}{Left}", style = "hint_offset" },
            { click = "l", swipe_up = "\\", long_click = "|", style = "hint_offset" },
            { width = 4.61435, background = 0xff },
        }
    },
    -- 第三行
    {
        width = 10.0857,
        keys = {
            { width = 1 },
            { click = "Shift_L", width = 13.7, swipe_up = "Candidate_switch", swipe_down = "Keyboard_touch_typing", composing = "delimiter", style = "key_shift" },
            { click = "z", swipe_up = "`", long_click = "~", style = "hint_offset" },
            { click = "x", long_click = "cut", style = "hint_offset" },
            { click = "c", long_click = "copy", style = "hint_offset" },
            { click = "v", long_click = "paste", style = "hint_offset" },
            { click = "b", swipe_up = "Keyboard_editor", long_click = "Keyboard_clipboard", swipe_down = "Keyboard_phrase", style = "hint_offset" },
            { click = "n", swipe_up = ";", long_click = ":", style = "hint_offset" },
            { click = "m", swipe_up = "'", long_click = "\"", style = "hint_offset" },
            { click = "BackSpace", width = 13.7, swipe_up = "Clear", swipe_left = "ClearH", style = "key_backspace" },
            { width = 1 },
        }
    },
    -- 第四行
    {
        width = 10.0857,
        keys = {
            { width = 1 },
            { click = "Keyboard_symbols", width = 13.7, long_click = "Mode_switch", swipe_up = "Keyboard_menu", swipe_right = "Schema_settings", style = "key_symbols" },
            { click = "Keyboard_number", swipe_up = "Keyboard_default_zhuji", swipe_left = "Theme_settings", swipe_right = "Color_switch", composing = "Select_five", style = "key_numbers" },
            { click = "<", long_click = ",", composing = "Select_four" },
            { click = "space", width = 30.2751, long_click="VOICE_ASSIST", swipe_left = "Left", swipe_right = "Right", swipe_up = "Up", swipe_down = "Down", style = "key_space" },
            { click = ">", long_click = ".", composing = "Select_two" },
            { click = "/", long_click = "?" , composing = "Select_three" },
            { click = "Return", width = 13.7, label = "Enter", style = "key_enter" },
            { width = 1 },
        }
    },
}
