name = "26键"
author = "nirenr"
key_width = 10
key_height = 60
lock = true
--rows行键盘，宽度和高度为键盘总宽度的百分比
rows = {
    --第一行
    {
        keys = {
            { click = "q", long_click = "!" },
            { click = "w", long_click = "@" },
            { click = "e", long_click = "#" },
            { click = "r", long_click = "$" },
            { click = "t", long_click = "%" },
            { click = "y", long_click = "^" },
            { click = "u", long_click = "&" },
            { click = "i", long_click = "*" },
            { click = "o", long_click = "(){Left}" },
            { click = "p", long_click = "Hide" },
        }
    },
    --第二行
    {
        keys = {
            { width = 5 },
            { click = "a", long_click = "select_all" },
            { click = "s", long_click = "~" },
            { click = "d", long_click = "-" },
            { click = "f", long_click = "+" },
            { click = "g", long_click = "\\" },
            { click = "h", long_click = "[]{Left}" },
            { click = "j", long_click = "{}{Left}" },
            { click = "k", long_click = ":" },
            { click = "l", long_click = ";" },
        }
    },
    --第一行
    {
        keys = {
            { click = "Shift_L", style = "functional", width = 15 },
            { click = "z", long_click = "`" },
            { click = "x", long_click = "cut" },
            { click = "c", long_click = "copy" },
            { click = "v", long_click = "paste" },
            { click = "b", long_click = "/" },
            { click = "n", long_click = "\"" },
            { click = "m", long_click = "'" },
            { click = "BackSpace", style = "functional", label = " ⌫", width = 15 },
        }
    },
    --第四行
    {
        keys = {
            { click = "Keyboard_symbols", long_click = "F4", style = "functional", width = 15,
              ascii = {
                  click = "Keyboard_symbols", label = "Sys", long_click = "F4", hint_long = "menu"
              }
            },
            { click = "Keyboard_number", long_click = "Theme_settings", popup = {"Theme_settings", "Color_switch"}, style = "functional" },
            { click = "，", ascii = ",", long_click = "<" },
            { click = "space", label = "schema_name", long_click="VOICE_ASSIST", width = 30, swipe_repeatable = true, swipe_left = "Left", swipe_right = "Right", swipe_up = "Up", swipe_down = "Down" },
            { click = "。", ascii = ".", long_click = ">" },
            { click = "Mode_switch", style = "functional" , composing = "CommitScriptText"},
            { click = "Return", style = "enter", label = "Enter", width = 15 ,long_click="AI", popup={"gpt1","gpt2","gpt3","gpt4","gpt5",}},
        }
    },
}
