name = "26键-盲打"
author = "tbagr"

base = "qwerty26"
-- 按键覆盖配置：继承 base 键盘（qwerty26）的布局，只在此处覆盖需要修改的按键字段
-- 格式: overrides[行号] = { [列号] = { 字段名 = 新值, ... }, ... }
-- 行号和列号都从 1 开始
overrides = {
    [1] = {
        [2]  = { style = "touch_typing" },
        [3]  = { style = "touch_typing" },
        [4]  = { style = "touch_typing" },
        [5]  = { style = "touch_typing" },
        [6]  = { style = "touch_typing" },
        [7]  = { style = "touch_typing" },
        [8]  = { style = "touch_typing" },
        [9]  = { style = "touch_typing" },
        [10] = { style = "touch_typing" },
        [11] = { style = "touch_typing" },
    },
    [2] = {
        [2] = { style = "touch_typing" },
        [3] = { style = "touch_typing" },
        [4] = { style = "touch_typing" },
        [5] = { style = "touch_typing" },
        [6] = { style = "touch_typing" },
        [7] = { style = "touch_typing" },
        [8] = { style = "touch_typing" },
        [9] = { style = "touch_typing" },
        [10] = { style = "touch_typing" },
    },
    [3] = {
        [2] = { swipe_down = { send = "Eisu_toggle", select = "qwerty26" } },
        [3] = { style = "touch_typing" },
        [4] = { style = "touch_typing" },
        [5] = { style = "touch_typing" },
        [6] = { style = "touch_typing" },
        [7] = { style = "touch_typing" },
        [8] = { style = "touch_typing" },
        [9] = { style = "touch_typing" },
    },
    [4] = {
        [3] = { style = "touch_typing" },
        [4] = { style = "touch_typing" },
        [6] = { style = "touch_typing" },
        [7] = { style = "touch_typing" },
    },
}
