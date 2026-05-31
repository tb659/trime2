name = "26键-助记"
author = "tbagr"
base = "qwerty26"
-- 按键覆盖配置：继承 base 键盘（qwerty26）的布局，只在此处覆盖需要修改的按键字段
-- 格式: key_overrides[行号] = { [列号] = { 字段名 = 新值, ... }, ... }
-- 行号和列号都从 1 开始
key_overrides = {
    [1] = {
        margins = { left = 0, top = 0, right = 0, bottom = 0 },
        padding = { left = 0, top = 0, right = 0, bottom = 0 },
        [1]  = { background = "zhuji/q.png", text_color = 0xff, hint_up = "", hint_long = "" },
        [2]  = { background = "zhuji/w.png", text_color = 0xff, hint_up = "", hint_long = "" },
        [3]  = { background = "zhuji/e.png", text_color = 0xff, hint_up = "", hint_long = "" },
        [4]  = { background = "zhuji/r.png", text_color = 0xff, hint_up = "", hint_long = "" },
        [5]  = { background = "zhuji/t.png", text_color = 0xff, hint_up = "", hint_long = "" },
        [6]  = { background = "zhuji/y.png", text_color = 0xff, hint_up = "", hint_long = "" },
        [7]  = { background = "zhuji/u.png", text_color = 0xff, hint_up = "", hint_long = "" },
        [8]  = { background = "zhuji/i.png", text_color = 0xff, hint_up = "", hint_long = "" },
        [9]  = { background = "zhuji/o.png", text_color = 0xff, hint_up = "", hint_long = "" },
        [10] = { background = "zhuji/p.png", text_color = 0xff, hint_up = "", hint_long = "" },
    },
    [2] = {
        margins = { left = 0, top = 0, right = 0, bottom = 0 },
        padding = { left = 0, top = 0, right = 0, bottom = 0 },
        [2] = { background = "zhuji/a.png", text_color = 0xff, hint_up = "", hint_long = "" },
        [3] = { background = "zhuji/s.png", text_color = 0xff, hint_up = "", hint_long = "" },
        [4] = { background = "zhuji/d.png", text_color = 0xff, hint_up = "", hint_long = "" },
        [5] = { background = "zhuji/f.png", text_color = 0xff, hint_up = "", hint_long = "" },
        [6] = { background = "zhuji/g.png", text_color = 0xff, hint_up = "", hint_long = "" },
        [7] = { background = "zhuji/h.png", text_color = 0xff, hint_up = "", hint_long = "" },
        [8] = { background = "zhuji/j.png", text_color = 0xff, hint_up = "", hint_long = "" },
        [9] = { background = "zhuji/k.png", text_color = 0xff, hint_up = "", hint_long = "" },
        [10] = { background = "zhuji/l.png", text_color = 0xff, hint_up = "", hint_long = "" },
    },
    [3] = {
        margins = { left = 0, top = 0, right = 0, bottom = 0 },
        padding = { left = 0, top = 0, right = 0, bottom = 0 },
        [1] = { margins = { left = 2, top = 0, right = 2, bottom = 5 } },
        [2] = { background = "zhuji/z.png", text_color = 0xff, hint_up = "", hint_long = "" },
        [3] = { background = "zhuji/x.png", text_color = 0xff, hint_up = "", hint_long = "" },
        [4] = { background = "zhuji/c.png", text_color = 0xff, hint_up = "", hint_long = "" },
        [5] = { background = "zhuji/v.png", text_color = 0xff, hint_up = "", hint_long = "" },
        [6] = { background = "zhuji/b.png", text_color = 0xff, hint_up = "", hint_long = "", hint_down = "" },
        [7] = { background = "zhuji/n.png", text_color = 0xff, hint_up = "", hint_long = "" },
        [8] = { background = "zhuji/m.png", text_color = 0xff, hint_up = "", hint_long = "" },
        [9] = { margins = { left = 2, top = 0, right = 2, bottom = 5 } },
    },
    [4] = {
        [2] = { swipe_up = { send = "Eisu_toggle", select = "qwerty26" } }
    },
}
