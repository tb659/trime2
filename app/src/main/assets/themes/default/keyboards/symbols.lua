name = "符号键盘"
author = "tbagr"
key_width = 10
key_height = 50
rows = {
    -- 第一行
    {
        keys = {
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
        }
    },

    -- 第二行
    {
        keys = {
            { click = "……", ascii = "^", style = "text_size_14" },
            { click = "&", ascii = "&" },
            { click = "*", ascii = "*" },
            { click = "（", ascii = "(" },
            { click = "）", ascii = ")" },
            { click = "-", ascii = "-" },
            { click = "=", ascii = "=" },
            { click = "4" },
            { click = "5" },
            { click = "6" },
        }
    },

    -- 第三行
    {
        keys = {
            { click = "——", ascii = "_", style = "text_size_14" },
            { click = "+", ascii = "+" },
            { click = "【", ascii = "[" },
            { click = "】", ascii = "]" },
            { click = "{", ascii = "{" },
            { click = "}", ascii = "}" },
            { click = "|", ascii = "|" },
            { click = "7" },
            { click = "8" },
            { click = "9" },
        }
    },

    -- 第四行
    {
        keys = {
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
        }
    },

    -- 第五行
    {
        height = 56,
        keys = {
            { click = "Keyboard_back", swipe_up = "Keyboard_symbols_ext", width = 15, text_size = 16 },
            { click = "Mode_switch", width = 15, text_size = 16 },
            { click = "，", ascii = "," },
            { click = "space1", width = 25 },
            { click = "。", ascii = "." },
            { click = "/", ascii = "/" },
            { click = "Return", style = "enter2", label = "Enter", width = 15 }
        }
    }
}
