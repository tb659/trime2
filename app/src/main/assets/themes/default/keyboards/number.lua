name = "数字键盘"
author = "tbagr"
ascii_mode = true

text_size = 24
-- rows行键盘，宽度和高度为键盘总宽度的百分比
rows = {
    -- 第一行
    {
        width = 19.6,
        keys = {
            { width = 1 },
            { click = "+" },
            { click = "KP_1"  },
            { click = "KP_2" },
            { click = "KP_3" },
            { click = "Keyboard_back", style = "num_back" },
            { width = 1 },
        }
    },
    -- 第二行
    {
        width = 19.6,
        keys = {
            { width = 1 },
            { click = "-" },
            { click = "KP_4"  },
            { click = "KP_5" },
            { click = "KP_6" },
            { click = "BackSpace", label = "⌫", style = "num_backspace" },
            { width = 1 },
        }
    },
    -- 第三行
    {
        width = 19.6,
        keys = {
            { width = 1 },
            { click = "*" },
            { click = "KP_7" },
            { click = "KP_8" },
            { click = "KP_9" },
            { click = "space1", style = "num_space" },
            { width = 1 },
        }
    },
    -- 第四行
    {
        width = 19.6,
        keys = {
            { width = 1 },
            { click = "/" },
            { click = "=" },
            { click = "KP_0" },
            { click = "." },
            { click = "Return1", label = "Enter", style = "num_enter" },
            { width = 1 },
        }
    },
}
