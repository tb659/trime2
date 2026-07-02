name = "设置键盘"
author = "tbagr"

width = 20
-- height = 60
text_size = 16

flex_box = {
    direction="row",
    -- 第一列
    {
        direction = "column",
        width = 30,
        style = "menu_note",
        keys = {
            { hint = "爱" },
            { hint = "澈" },
            { hint = "到" },
            { hint = "底" },
        }
    },

    -- 第二列
    {
        grow = 5,
        direction = "column",
        -- 第二列第一行
        {
            direction = "row",
            keys = {
                { click = "Settings" },
                { click = "Deploy" },
                { click = "IME_switch" },
                { click = "Schema_group"  },
                { click = "Menu" },
            }
        },
        -- 第二列第二行
        {
            direction = "row",
            keys = {
                { click = "Theme_settings" },
                { click = "Color_settings" },
                -- { click = "AddPhrase" },
                { click = "Mode_small" },
                { click = "Mode_float" },
            }
        },
        -- 第二列第三行
        {
            direction = "row",
            keys = {
                { click = "Candidate_switch" },
                { click = "Comment_switch" },
                { click = "Hint_switch" },
                { click = "Sound_switch" },
            }
        },
        -- 第四列第四行
        {
            direction = "row",
            keys = {
                { click = "="},
                { click = "KP_0"},
                { click = "Keyboard_back"},
            }
        },
    },
    -- 第三列
    {
        direction = "column",
        width = 30,
        style = "menu_note",
        keys = {
            { hint = "可" },
            { hint = "乐" },
            { hint = "瑞" },
            { hint = "宝" }
        }
    },
}
