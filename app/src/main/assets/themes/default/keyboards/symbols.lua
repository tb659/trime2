name = "符号键盘"
author = "tbagr"

width = 9.8
height = 40
font = { "TH-Feon.ttf", "xr.ttf" }

rows = {
    -- 第一行
    {
        keys = {
            { width = 1 },
            { click = "·", ascii = "`" },
            { click = "~", ascii = "~" },
            { click = "！", ascii = "!" },
            { click = "@", ascii = "@" },
            { click = "#", ascii = "#" },
            { click = "￥", ascii = "$" },
            { click = "%", ascii = "%" },
            { click = "1" },
            { click = "2" },
            { click = "3" },
            { width = 1 },
        }
    },

    -- 第二行
    {
        keys = {
            { width = 1 },
            { click = "……", ascii = "^", text_size = 14 },
            { click = "&", ascii = "&" },
            { click = "*", ascii = "*" },
            { click = "（", ascii = "(" },
            { click = "）", ascii = ")" },
            { click = "-", ascii = "-" },
            { click = "=", ascii = "=" },
            { click = "4" },
            { click = "5" },
            { click = "6" },
            { width = 1 },
        }
    },

    -- 第三行
    {
        keys = {
            { width = 1 },
            { click = "——", ascii = "_", text_size = 14 },
            { click = "+", ascii = "+" },
            { click = "【", ascii = "[" },
            { click = "】", ascii = "]" },
            { click = "{", ascii = "{" },
            { click = "}", ascii = "}" },
            { click = "|", ascii = "|" },
            { click = "7" },
            { click = "8" },
            { click = "9" },
            { width = 1 },
        }
    },

    -- 第四行
    {
        keys = {
            { width = 1 },
            { click = "“", ascii = "\"" },
            { click = "”", ascii = "'" },
            { click = "《", ascii = "<" },
            { click = "》", ascii = ">" },
            { click = "？", ascii = "?" },
            { click = "：", ascii = ":" },
            { click = "；", ascii = ";" },
            { click = "、", ascii = "\\" },
            { click = "0" },
            { click = "BackSpace" },
            { width = 1 },
        }
    },

    -- 第五行
    {
        height = 56,
        keys = {
            { width = 1 },
            { click = "Keyboard_back", swipe_up = "Keyboard_symbols_ext", width = 15, text_size = 16, offset_y = 3 },
            { click = "Mode_switch", width = 12, text_size = 16 },
            { click = "，", ascii = "," },
            { click = "space1", width = 25 },
            { click = "。", ascii = "." },
            { click = "/", ascii = "/" },
            { click = "Return", style = "enter2", label = "Enter", width = 16.6 },
            { width = 1 },
        }
    }
}
