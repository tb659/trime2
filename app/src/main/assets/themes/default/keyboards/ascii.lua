name = "36键密码"
author = "tbagr"

ascii_mode = true
lock = true
width = 10

rows = {
    -- 第一行
    {
        width = 9.8,
        height = 45,
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
    -- 第二行
    {
        width = 9.8,
        keys = {
            { width = 1 },
            { click = "q" },
            { click = "w" },
            { click = "e" },
            { click = "r" },
            { click = "t" },
            { click = "y" },
            { click = "u" },
            { click = "i" },
            { click = "o" },
            { click = "p" },
            { width = 1 },
        }
    },
    -- 第三行
    {
        width = 10.0857,
        keys = {
            { width = 4.61435, background = 0xff },
            { click = "a" },
            { click = "s" },
            { click = "d" },
            { click = "f" },
            { click = "g" },
            { click = "h" },
            { click = "j" },
            { click = "k" },
            { click = "l" },
            { width = 4.61435, background = 0xff },
        }
    },
    -- 第四行
    {
        width = 10.0857,
        keys = {
            { width = 1 },
            { click = "Shift_L", width = 13.7, style = "key_shift" },
            { click = "z" },
            { click = "x" },
            { click = "c" },
            { click = "v" },
            { click = "b" },
            { click = "n" },
            { click = "m" },
            { click = "BackSpace", width = 13.7, style = "key_backspace" },
            { width = 1 },
        }
    },
    -- 第五行
    {
        -- height = 50,
        width = 10.0857,
        keys = {
            { width = 1 },
            { click="Keyboard_symbols", width = 13.7, long_click = "Mode_switch", swipe_up = "Keyboard_menu", swipe_right = "Schema_settings", style = "key_symbols" },
            { click="Keyboard_number", swipe_up = "Keyboard_default_zhuji", swipe_left = "Theme_settings", swipe_right = "Color_switch", composing = "Select_five", style = "key_numbers" },
            { click = "," },
            { click = "space", width = 30.2751, swipe_repeatable=true, swipe_left="Left",swipe_right="Right",swipe_up="Up",swipe_down="Down", style = "key_space"},
            { click = "." },
            { click="@" },
            { click = "Return", width = 13.7, label = "Enter", style = "key_enter" },
            { width = 1 },
        }
    },
}
