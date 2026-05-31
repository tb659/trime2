name = "设置键盘"
author = "tbagr"
key_width = 20
key_text_size = 16
-- key_height = 60
flex_box = {
    direction="row",
    -- 第一列
    {
        direction = "column",
        width = 30,
        -- style = "key",
        keys = {
            { click = "", hint = "爱" },
            { click = "", hint = "澈" },
            { click = "", hint = "到" },
            { click = "", hint = "底" },
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
        keys = {
            { click = "", hint = "可" },
            { click = "", hint = "乐" },
            { click = "", hint = "瑞" },
            { click = "", hint = "宝" }
        }
    },
}
