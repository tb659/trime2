name = "36键密码"
author = "tbagr"
ascii_mode = true
lock = true
key_width = 10
rows = {
    -- 第一行
    {
        height = 50,
        keys = {
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
        }
    },
    -- 第二行
    {
        keys = {
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
        }
    },
    -- 第三行
    {
        keys = {
            { width = 5 },
            { click = "a" },
            { click = "s" },
            { click = "d" },
            { click = "f" },
            { click = "g" },
            { click = "h" },
            { click = "j" },
            { click = "k" },
            { click = "l" },
        }
    },
    -- 第四行
    {
        keys = {
            { click = "Shift_L", width = 15 },
            { click = "z" },
            { click = "x" },
            { click = "c" },
            { click = "v" },
            { click = "b" },
            { click = "n" },
            { click = "m" },
            { click = "BackSpace", label = "⌫", width = 15 },
        }
    },
    -- 第五行
    {
        -- height = 50,
        keys = {
            { click="Keyboard_symbols", width = 15 },
            { click="Keyboard_number" },
            { click = "," },
            { click = "space", width = 30, swipe_repeatable=true, swipe_left="Left",swipe_right="Right",swipe_up="Up",swipe_down="Down"},
            { click = "." },
            { click="@" },
            { click = "Return", style = "enter", label = "Enter", width = 15 }
        }
    },
}
