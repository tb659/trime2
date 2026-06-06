name = "26键-助记"
author = "tbagr"
base = "qwerty26"
-- 按键覆盖配置：继承 base 键盘（qwerty26）的布局，只在此处覆盖需要修改的按键字段
-- 格式: overrides[行号] = { [列号] = { 字段名 = 新值, ... }, ... }
-- 行号和列号都从 1 开始
overrides = {
    [1] = {
        margins = { left = 0, top = 0, right = 0, bottom = 0 },
        padding = { left = 0, top = 0, right = 0, bottom = 0 },
        [1]  = { background = 0xff },
        [2]  = { background = "zhuji/q.png", style = "hint_offset" },
        [3]  = { background = "zhuji/w.png", style = "hint_offset" },
        [4]  = { background = "zhuji/e.png", style = "hint_offset" },
        [5]  = { background = "zhuji/r.png", style = "hint_offset" },
        [6]  = { background = "zhuji/t.png", style = "hint_offset" },
        [7]  = { background = "zhuji/y.png", style = "hint_offset" },
        [8]  = { background = "zhuji/u.png", style = "hint_offset" },
        [9]  = { background = "zhuji/i.png", style = "hint_offset" },
        [10] = { background = "zhuji/o.png", style = "hint_offset" },
        [11] = { background = "zhuji/p.png", style = "hint_offset" },
        [12]  = { background = 0xff },
    },
    [2] = {
        margins = { left = 0, top = 0, right = 0, bottom = 0 },
        padding = { left = 0, top = 0, right = 0, bottom = 0 },
        [2] = { background = "zhuji/a.png", style = "hint_offset" },
        [3] = { background = "zhuji/s.png", style = "hint_offset" },
        [4] = { background = "zhuji/d.png", style = "hint_offset" },
        [5] = { background = "zhuji/f.png", style = "hint_offset" },
        [6] = { background = "zhuji/g.png", style = "hint_offset" },
        [7] = { background = "zhuji/h.png", style = "hint_offset" },
        [8] = { background = "zhuji/j.png", style = "hint_offset" },
        [9] = { background = "zhuji/k.png", style = "hint_offset" },
        [10] = { background = "zhuji/l.png", style = "hint_offset" },
    },
    [3] = {
        margins = { left = 0, top = 0, right = 0, bottom = 0 },
        padding = { left = 0, top = 0, right = 0, bottom = 0 },
        [1]  = { background = 0xff },
        [2] = { margins = { left = 0, top = 0, right = 0, bottom = 0 } },
        [3] = { background = "zhuji/z.png", style = "hint_offset" },
        [4] = { background = "zhuji/x.png", style = "hint_offset" },
        [5] = { background = "zhuji/c.png", style = "hint_offset" },
        [6] = { background = "zhuji/v.png", style = "hint_offset" },
        [7] = { background = "zhuji/b.png", style = "hint_offset" },
        [8] = { background = "zhuji/n.png", style = "hint_offset" },
        [9] = { background = "zhuji/m.png", style = "hint_offset" },
        [10] = { margins = { left = 0, top = 0, right = 0, bottom = 0 } },
        [11]  = { background = 0xff },
    },
    [4] = {
        [3] = { swipe_up = { send = "Eisu_toggle", select = "qwerty26" } }
    },
}
