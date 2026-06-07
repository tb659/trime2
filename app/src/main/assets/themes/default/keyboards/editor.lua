name = "编辑键盘"
author = "tbagr"

base = "number"
-- 按键覆盖配置：继承 base 键盘（qwerty26）的布局，只在此处覆盖需要修改的按键字段
-- 格式: overrides[行号] = { [列号] = { 字段名 = 新值, ... }, ... }
-- 行号和列号都从 1 开始
overrides = {
    [1] = {
        [1] = { background = 0xff },
        [2] ={ click = "select_all" },
        [3] ={ click = "redo" },
        [4] ={ click = "Up" },
        [5] ={ click = "undo" },
        [6] ={ click = "Keyboard_back" },
        [7] = { background = 0xff },
    },
    [2] = {
        [1] = { background = 0xff },
        [2] = { click = "cut" },
        [3] = { click = "Left" },
        [4] = { click = "Shift_R", label = "☩" },
        [5] = { click = "Right" },
        [6] = { click = "BackSpace" },
        [7] = { background = 0xff },
    },
    [3] = {
        [1] = { background = 0xff },
        [2] = { click = "copy" },
        [3] = { click = "Home" },
        [4] = { click = "Down" },
        [5] = { click = "End" },
        [6] = { click = "space1" },
        [7] = { background = 0xff },
    },
    [4] = {
        [1] = { background = 0xff },
        [2] = { click = "paste" },
        [3] = { click = "Page_Up" },
        [4] = { click = "ClearH" },
        [5] = { click = "Page_Down" },
        [6] = { click = "Return", label = "Enter", style = "enter2" },
        [7] = { background = 0xff },
    },
}
