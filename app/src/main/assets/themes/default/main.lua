name = "可乐"
author = "tbagr"
style = "light"
keyboard = "qwerty26"

function get_keyboard(id, alphabet)
    if id == "" then
        return keyboard
    end
    if string.find(alphabet, "%d") then
        return "qwerty36"
    end
    return keyboard
end

action_labels = {
    none = "Enter", --默认状态
    send = "发送", --回车键发送按钮文本
    go = "前往", --回车键前往按钮文本
    done = "完成", --回车键完成按钮文本
    search = "搜索", --回车键搜索按钮文本
    previous = "上一个", --回车键上一个按钮文本
    next = "下一个", --回车键下一个按钮文本
}

preset_keys = {
    -- 安卓
    BRIGHTNESS_DOWN = { label = "亮度 -", send = "BRIGHTNESS_DOWN" },
    BRIGHTNESS_UP = { label = "亮度 +", send = "BRIGHTNESS_UP" },
    CALCULATOR = { label = "计算器", send = "CALCULATOR" },
    CALENDAR = { label = "日历", send = "CALENDAR" },
    CONTACTS = { label = "电话簿", send = "CONTACTS" },
    ENVELOPE = { label = "信箱", send = "ENVELOPE" },
    EXPLORER = { label = "浏览器", send = "EXPLORER" },
    MUSIC = { label = "音乐", send = "MUSIC" },
    POWER = { label = "电源", send = "POWER" },
    SEARCH = { label = "搜索", send = "Find" },
    SLEEP = { label = "休眠", send = "SLEEP" },
    VOICE_ASSIST = { label = "语 音", send = "VOICE_ASSIST" },
    VOLUME_DOWN = { label = "音量 -", send = "VOLUME_DOWN" },
    VOLUME_UP = { label = "音量 +", send = "VOLUME_UP" },
    VOLUME_MUTE = { label = "静音", send = "VOLUME_MUTE" },

    -- 编辑
    Hide = { label = "隐藏", send = "BACK" },
    Escape = { label = "Esc", send = "Escape" },
    Insert = { label = "插入", send = "Insert" },
    Home = { label = "行首", send = "Home" },
    End = { label = "行尾", send = "End" },
    Left = { label = "←", send = "Left" },
    Down = { label = "↓", send = "Down" },
    Up = { label = "↑", send = "Up" },
    Right = { label = "→", send = "Right" },
    Page_Up = { label = "上页", send = "Page_Up" },
    Page_Down = { label = "下页", send = "Page_Down" },
    select_all = { label = "全选", send = "Control+a" },
    Shift_L = { label = "", send = "Shift_L", shift_lock = "double" }, -- shift_lock click,double,long
    Return = { label = "action_labels", send = "Return" },
    Return1 = { label = "Enter", send = "Return" },
    Return2 = { label = "回车", send = "Return" },
    space = { repeatable = false, send = "space" },
    space1 = { label = "␣", repeatable = false, send = "space" },
    BackSpace = { label = "⌫", description = "退格", repeatable = true, send = "BackSpace" },
    Clear = { label = "清除", text = "{Control+a}{BackSpace}" }, -- 全选并删除
    ClearH = { label = "Del↔", send = "{End}{Shift+Home}{Delete}" },
    Delete = { label = "删除", send = "Delete" },
    delete_all = { label = "全删", text = "{Control+a}{Delete}" }, -- 全选并删除
    cut = { label = "剪切", send = "Control+x" },
    cut_all = { label = "全剪", text = "{Control+a}{Control+x}" }, -- 全选并剪切
    copy = { label = "复制", send = "Control+c" },
    copy_all = { label = "全部复制", text = "{Control+a}{Control+c}" }, -- 全选并复制
    paste = { label = "粘贴", send = "Control+v" },
    paste_text = { label = "粘贴文本", send = "Control+Shift+Alt+v" }, -- >= Android 6.0
    share_text = { label = "分享文本", send = "Control+Alt+s" }, -- >= Android 6.0
    redo = { label = "重做", send = "Control+Shift+z" }, -- >= Android 6.0
    undo = { label = "撤销", send = "Control+z" }, -- >= Android 6.0

    -- rime组合键
    F4 = { label = "菜单", send = "Control+grave" },
    BackToPreviousSyllable = { label = "删音节", send = "Control+BackSpace" },
    CommitRawInput = { label = "编码", send = "Control+Return" },
    CommitScriptText = { label = "编码", send = "Shift+Return" },
    CommitComment = { label = "编码", send = "Control+Shift+Return" },
    DeleteCandidate = { label = "删词", send = "Control+Delete" },
    delimiter = { label = "分词", text = "'", description = "分词" },

    -- rime状态
    Mode_switch = { toggle = "ascii_mode", send = "Mode_switch", states = { "中文", "英文" } },
    Zenkaku_Hankaku = { toggle = "full_shape", send = "Mode_switch", states = { "半角", "全角" } },
    Henkan = { toggle = "simplification", send = "Mode_switch", states = { "繁体", "简体" } },
    Charset_switch = { toggle = "extended_charset", send = "Mode_switch", states = { "常用", "全部" } },
    Punct_switch = { toggle = "ascii_punct", send = "Mode_switch", states = { "。，", "．，" } },

    -- 切换键盘
    Keyboard_symbols = { label = "符号", send = "Eisu_toggle", select = "symbols" },
    Keyboard_symbols_ext = { label = "更多", send = "Eisu_toggle", select = "symbols_ext" },
    Keyboard_number = { label = "123", send = "Eisu_toggle", select = "number" },
    Keyboard_letter = { label = "字母", send = "Eisu_toggle", select = "default" },
    Keyboard_default = { label = "返回", send = "Eisu_toggle", select = ".default" },
    Keyboard_back = { label = "返回", send = "Eisu_toggle", select = ".last" },
    Keyboard_switch = { label = "键盘", send = "Eisu_toggle", select = ".next" },
    Keyboard_clipboard = { label = "剪贴板", send = "Eisu_toggle", select = "clipboard" },
    Keyboard_phrase = { label = "短语", send = "Eisu_toggle", select = "phrase" },
    Keyboard_editor = { label = "编辑", send = "Eisu_toggle", select = "editor" },
    Keyboard_settings = { label = "菜单", send = "Eisu_toggle", select = "settings" },
    Keyboard_default_zhuji = { label = "助记", send = "Eisu_toggle", select = "qwerty26_zhuji" },

    -- trime设定
    IME_switch = { label = "输入法", send = "LANGUAGE_SWITCH" }, -- 弹出对话框选择输入法
    IME_last = { label = "上一输入法", send = "LANGUAGE_SWITCH", select = ".last" }, -- 直接切换到上一输入法
    IME_next = { label = "下一输入法", send = "LANGUAGE_SWITCH", select = ".next" }, -- 直接切换到下一输入法
    Schema_switch = { label = "下一方案", send = "Control+Shift+1" },
    Color_switch = { label = "样式", send = "PROG_RED" },
    Menu = { label = "方案", send = "Menu" },
    Settings = { label = "设置", send = "SETTINGS", option = "app" },
    Color_settings = { label = "样式", send = "SETTINGS", option = "color" }, -- 添加select参数可以直接设置指定配色
    Theme_settings = { label = "主题", send = "SETTINGS", option = "theme" }, -- 添加select参数可以直接设置指定主题
    Schema_settings = { label = "方案", send = "SETTINGS", option = "schema" },  --添加select参数可以直接设置指定方案
    Schema_group = { label = "方案组", send = "SETTINGS", option = "group" }, -- 添加select参数可以直接设置指定方案
    Candidate_switch = { toggle = "_hide_candidate", send = "Mode_switch", states = { "有候选", "无候选" } },
    Comment_switch = { toggle = "_hide_comment", send = "Mode_switch", states = { "有注释", "无注释" } },
    Hint_switch = { toggle = "_hide_key_hint", send = "Mode_switch", states = { "有助记", "无助记" } },
    Sound_switch = { toggle = "_hide_key_sound", send = "Mode_switch", states = { "有音效", "无音效" } },
    Mode_small = { toggle = "small_mode", send = "Mode_switch", states = { "单手关", "单手开" } },
    Mode_float = { toggle = "float_mode", send = "Mode_switch", states = { "悬浮关", "悬浮开" } },

    -- 候选过滤
    Filter_h = { label = "一", command = "filter", option = "h" },
    Filter_s = { label = "丨", command = "filter", option = "s" },
    Filter_p = { label = "丿", command = "filter", option = "p" },
    Filter_n = { label = "丶", command = "filter", option = "n" },
    Filter_z = { label = "乙", command = "filter", option = "z" },
    Filter_x = { label = "X", command = "filter", option = "" },
    Filter_char = { label = "字/词", command = "filter", option = "char" },

    -- trime命令
    LunarDate = { label = "农历", command = "nongli.lua" },
    Date = { label = "日期", command = "date", option = "yyyy-MM-dd" },
    ChineseDate = { label = "农历", command = "date", option = "zh_CN@calendar=chinese" }, -- 农历等日期(>=Android 7.0)：date 語言@calendar=历法 格式。具体参见https://developer.android.com/reference/android/icu/util/Calendar.html
    Time = { label = "时间", command = "date", option = "HH=mm=ss" }, -- 时间： date 格式
    TrimeApp = { label = "同文", command = "run", option = "com.osfans.trime" }, -- 运行程序 = run 包名
    TrimeCmp = { label = "同文组件", command = "run", option = "com.osfans.trime/.ui.main.MainActivity" }, -- 运行程序指定組件 = run 包名/组件名
    Homepage = { label = "同文主页", command = "run", option = "https://github.com/osfans/trime" }, -- 查看网页 = run 网址
    CommitHomepage = { label = "同文网址", commit = "https://github.com/osfans/trime" }, -- 直接上屏
    Wiki = { label = "维基", command = "run", option = "https://zh.wikipedia.org/wiki/%s" }, -- 搜索网页 = %s或者%1$s为当前字符
    Google = { label = "谷歌", command = "run", option = "https://www.google.com/search?q=%s" }, -- 搜索网页 = %s或者%1$s为当前字符
    MoeDict = { label = "萌典", command = "run", option = "https://www.moedict.tw/%3$s" }, -- 搜索网页 = %3$s为光标前字符
    Baidu = { label = "百度搜索", command = "run", option = "https://www.baidu.com/s?wd=%4$s" }, -- 搜索网页 = %4s为光标前所有字符
    Zdic = { label = "汉典", command = "run", option = "http://www.zdic.net/hans/%1$s" }, -- 搜索网页 = %s或者%1$s为当前字符
    Zdic2 = { label = "汉典", command = "run", option = "http://www.zdic.net/hans/%2$s" }, -- 搜索网页 = %2$s为当前输入的编码
    WebSearch = { label = "搜索网页", command = "web_search", option = "%4$s" }, -- 搜索，其他view、dial、edit、search等intent，参考安卓的intent文档：https://developer.android.com/reference/android/content/Intent.html
    Search = { label = "搜索", command = "search", option = "%1$s" }, -- 搜索短信、字典等
    Share = { label = "分享", command = "send", option = "%s" }, -- 分享指定文本 = %s或者%1$s为当前字符
    Deploy = { label = "部署", command = "deploy" },
    Sync = { label = "同步", command = "broadcast", option = "com.osfans.trime.action.SYNC_USER_DATA" },
    RepeatCommit = { label = "重复", command = "commit", option = "%1$s" }, -- 重复输入刚上屏的内容
    AddPhrase = { label = "添加", command = "add_phrase", option = "%1$s" }, -- 将输入的内容添加到短语

    -- 候选
    Select_two = { label = "次选", send = 2 },
    Select_three = { label = "三选", send = 3 },
    Select_four = { label = "四选", send = 4 },
    Select_five = { label = "五选", send = 5 },
}

-- 支持回调，onConfigurationChanged，onStartInput，onWindowShown，onWindowHidden，onFinishInput，onDestroy,onSpeechResults

