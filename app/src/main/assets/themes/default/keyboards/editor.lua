name = "编辑键盘"
author = "tbagr"
text_size = 16
flex_box = {
    direction = "row",
    -- 第一列
    {
        grow = 1,
        direction = "column",
        keys = {
            {click = "select_all" },
            {click = "cut" },
            {click = "copy" },
            {click = "paste" }
        }
    },
    -- 第二列
    {
        grow = 3,
        direction = "column",
        {
            direction = "row",
            keys = {
                { click = "undo" },
                { click = "Up" },
                { click = "redo" },
            }
        },
        {
            direction = "row",
            keys = {
                { click = "Left" },
                { click = "Shift_R", label = "Shift" },
                { click = "Right" },
            }
        },
        {
            direction = "row",
            keys = {
                { click = "Home" },
                { click = "Down" },
                { click = "End" },
            }
        },
        {
            direction = "row",
            keys = {
                { click = "Page_Up" },
                { click = "ClearH" },
                { click = "Page_Down" },
            }
        }
    },
    -- 第三列
    {
        grow = 1,
        direction = "column",
        keys = {
            { click = "Keyboard_back", style = "functional"},
            { click = "BackSpace", style = "functional", label = "⌫"},
            { click = "space1" },
            { click = "Return", style = "enter2", label = "Enter" }
        }
    }
}
