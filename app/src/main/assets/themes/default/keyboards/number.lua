name = "数字键盘"
author = "tbagr"
ascii_mode = true

-- flex_box 弹性盒子键盘，高度和宽度为dp，
-- direction设置布局方向，row纵向，column横向，
-- grow表示份数占比
-- 可以通过style设置区域背景
flex_box = {
    direction="row",
    -- 第一列
    {
        direction = "column",
        width = 64,
        -- style = "key",
        keys = {
            { click = "+" },
            { click = "-" },
            { click = "*" },
            { click = "/" },
        }
    },

    -- 第二列
    {
        grow = 3,
        direction = "column",
        -- 第二列第一行
        {
            direction = "row",
            keys = {
                { click = "KP_1"  },
                { click = "KP_2" },
                { click = "KP_3" },
            }
        },
        -- 第二列第二行
        {
            direction = "row",
            keys = {
                { click = "KP_4" },
                { click = "KP_5" },
                { click = "KP_6" },
            }
        },
        -- 第二列第三行
        {
            direction = "row",
            keys = {
                { click = "KP_7" },
                { click = "KP_8" },
                { click = "KP_9" },
            }
        },
        -- 第四列第四行
        {
            direction = "row",
            keys = {
                { click = "="},
                { click = "KP_0"},
                { click = "."},
            }
        },
    },
    -- 第三列
    {
        direction = "column",
        width = 64,
        keys = {
            { click = "Keyboard_default", style = "functional"},
            { click = "BackSpace", style = "functional", label = "⌫"},
            { click = "space1" },
            { click = "Return", style = "enter2", label = "Enter" }
        }
    },
}
