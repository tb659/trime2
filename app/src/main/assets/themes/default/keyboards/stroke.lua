name = "笔画键盘"
author = "trime"

lock = true

offset_y = 3
rows = {
    -- 第一行：数字
    {
        width = 9.8,
        keys = {
            { width = 1 },
            { click = "1" },
            { click = "2" },
            { click = "3" },
            { click = "4" },
            { click = "5" },
            { click = "6" },
            { click = "7" },
            { click = "8" },
            { click = "9" },
            { click = "0" },
            { width = 1 },
        }
    },
    -- 第二行：五个基本笔画
    {
        width = 19.6,
        keys = {
            { width = 1 },
            { click = "h", label = "一" },
            { click = "s", label = "丨" },
            { click = "p", label = "丿" },
            { click = "n", label = "丶" },
            { click = "z", label = "乙" },
            { width = 1 },
        }
    },
    -- 第三行：功能键
    {
        width = 10.0857,
        keys = {
            { width = 1 },
            { click = "Shift_L", width = 13.7, swipe_up = "Candidate_switch", style = "key_shift" },
            { click = "select_all", width = 11.7666, style = "hint_offset" },
            { click = "cut", width = 11.7666, style = "hint_offset" },
            { click = "copy", width = 11.7666, style = "hint_offset" },
            { click = "paste", width = 11.7666, style = "hint_offset" },
            { click = "Keyboard_clipboard", width = 11.7666, long_click = "Keyboard_clipboard", swipe_up = "Keyboard_editor", swipe_down = "Keyboard_phrase", style = "hint_offset" },
            { click = "!", width = 11.7666, long_click = "?" , composing = "Select_three" }, { click = "BackSpace", width = 13.7, swipe_up = "Clear", swipe_left = "ClearH", style = "key_backspace" },
            { width = 1 },
        }
    },
    -- 第四行：符号键
    {
        width = 10.0857,
        keys = {
            { width = 1 },
            { click = "Keyboard_symbols", width = 13.7, long_click = "Mode_switch", swipe_up = "Keyboard_menu", swipe_right = "Schema_settings", style = "key_symbols" },
            { click = "，", width = 11.7666, ascii = ",", long_click = "<", composing = "Select_four" },
            { click = "space", width = 47.0668, long_click = "VOICE_ASSIST", swipe_left = "Left", swipe_right = "Right", style = "key_space" },
            { click = "。", width = 11.7666, ascii = ".", long_click = ">", composing = "Select_two" },
            { click = "Return", width = 13.7, label = "Enter", style = "key_enter" },
            { width = 1 },
        }
    },
}
