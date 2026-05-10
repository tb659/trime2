/*
 * Copyright (C) 2015-present, osfans
 * waxaca@163.com https://github.com/osfans
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package com.osfans.trime;

import android.text.TextUtils;
import android.util.Log;
import android.view.KeyCharacterMap;
import android.view.KeyEvent;

import com.osfans.trime.core.Rime;
import com.osfans.trime.enums.KeyEventType;
import com.osfans.trime.keyboard.KeyboardView;
import com.osfans.trime.keyboard.ModifierState;
import com.osfans.trime.theme.ThemeManager;

import org.luaj.LuaValue;
import org.luaj.Varargs;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

/**
 * 键盘按键类，表示 KeyboardView 中的各个按键。
 * 每个按键包含单击、长按、滑动等多种事件类型，以及标签、提示、描述等显示信息。
 * 支持 ASCII 模式切换、组合键、粘滞键等高级功能。
 */
public class Key {
    // 按键状态数组定义（用于 Drawable 状态选择）
    public static final int[] KEY_STATE_NORMAL_ON = {
            android.R.attr.state_checkable, android.R.attr.state_checked
    };
    public static final int[] KEY_STATE_PRESSED_ON = {
            android.R.attr.state_pressed, android.R.attr.state_checkable, android.R.attr.state_checked
    };
    public static final int[] KEY_STATE_NORMAL_OFF = {android.R.attr.state_checkable};
    public static final int[] KEY_STATE_PRESSED_OFF = {
            android.R.attr.state_pressed, android.R.attr.state_checkable
    };
    public static final int[] KEY_STATE_NORMAL = {};
    public static final int[] KEY_STATE_PRESSED = {android.R.attr.state_pressed};
    // 所有可能的按键状态组合
    public static final int[][] KEY_STATES =
            new int[][]{
                    KEY_STATE_PRESSED_ON,
                    KEY_STATE_PRESSED_OFF,
                    KEY_STATE_NORMAL_ON,
                    KEY_STATE_NORMAL_OFF,
                    KEY_STATE_PRESSED,
                    KEY_STATE_NORMAL
            };
    // Rime 输入法引擎的键名列表，与 Android KeyEvent 对应
    public static final String[] RIME_KEY_NAMES = {
            "VoidSymbol", "SOFT_LEFT", "SOFT_RIGHT", "HOME", "BACK", "CALL", "ENDCALL",
            "0", "1", "2", "3", "4", "5", "6", "7", "8", "9",
            "asterisk", "numbersign", "Up", "Down", "Left", "Right", "KP_Begin",
            "VOLUME_UP", "VOLUME_DOWN", "POWER", "CAMERA", "Clear",
            "a", "b", "c", "d", "e", "f", "g", "h", "i", "j", "k", "l", "m", "n", "o", "p", "q", "r", "s", "t", "u", "v", "w", "x", "y", "z",
            "comma", "period", "Alt_L", "Alt_R", "Shift_L", "Shift_R", "Tab", "space",
            "SYM", "EXPLORER", "ENVELOPE", "Return", "BackSpace",
            "grave", "minus", "equal", "bracketleft", "bracketright", "backslash", "semicolon", "apostrophe", "slash", "at",
            "NUM", "HEADSETHOOK", "FOCUS", "plus", "Menu", "NOTIFICATION", "Find",
            "MEDIA_PLAY_PAUSE", "MEDIA_STOP", "MEDIA_NEXT", "MEDIA_PREVIOUS", "MEDIA_REWIND", "MEDIA_FAST_FORWARD", "MUTE",
            "Page_Up", "Page_Down", "PICTSYMBOLS", "Mode_switch",
            "BUTTON_A", "BUTTON_B", "BUTTON_C", "BUTTON_X", "BUTTON_Y", "BUTTON_Z",
            "BUTTON_L1", "BUTTON_R1", "BUTTON_L2", "BUTTON_R2",
            "BUTTON_THUMBL", "BUTTON_THUMBR", "BUTTON_START", "BUTTON_SELECT", "BUTTON_MODE",
            "Escape", "Delete", "Control_L", "Control_R", "Caps_Lock", "Scroll_Lock", "Meta_L", "Meta_R",
            "function", "Sys_Req", "Pause", "Home", "End", "Insert", "Next",
            "MEDIA_PLAY", "MEDIA_PAUSE", "MEDIA_CLOSE", "MEDIA_EJECT", "MEDIA_RECORD",
            "F1", "F2", "F3", "F4", "F5", "F6", "F7", "F8", "F9", "F10", "F11", "F12",
            "Num_Lock", "KP_0", "KP_1", "KP_2", "KP_3", "KP_4", "KP_5", "KP_6", "KP_7", "KP_8", "KP_9",
            "KP_Divide", "KP_Multiply", "KP_Subtract", "KP_Add", "KP_Decimal", "KP_Separator", "KP_Enter", "KP_Equal",
            "parenleft", "parenright",
            "VOLUME_MUTE", "INFO", "CHANNEL_UP", "CHANNEL_DOWN", "ZOOM_IN", "ZOOM_OUT",
            "TV", "WINDOW", "GUIDE", "DVR", "BOOKMARK", "CAPTIONS", "SETTINGS",
            "TV_POWER", "TV_INPUT", "STB_POWER", "STB_INPUT", "AVR_POWER", "AVR_INPUT",
            "PROG_RED", "PROG_GREEN", "PROG_YELLOW", "PROG_BLUE", "APP_SWITCH",
            "BUTTON_1", "BUTTON_2", "BUTTON_3", "BUTTON_4", "BUTTON_5", "BUTTON_6", "BUTTON_7", "BUTTON_8",
            "BUTTON_9", "BUTTON_10", "BUTTON_11", "BUTTON_12", "BUTTON_13", "BUTTON_14", "BUTTON_15", "BUTTON_16",
            "LANGUAGE_SWITCH", "MANNER_MODE", "3D_MODE", "CONTACTS", "CALENDAR", "MUSIC", "CALCULATOR",
            "Zenkaku_Hankaku", "Eisu_toggle", "Muhenkan", "Henkan", "Hiragana_Katakana", "yen", "RO", "Kana_Lock",
            "ASSIST", "BRIGHTNESS_DOWN", "BRIGHTNESS_UP", "MEDIA_AUDIO_TRACK",
            "SLEEP", "WAKEUP", "PAIRING", "MEDIA_TOP_MENU", "11", "12", "LAST_CHANNEL", "TV_DATA_SERVICE", "VOICE_ASSIST",
            "TV_RADIO_SERVICE", "TV_TELETEXT", "TV_NUMBER_ENTRY", "TV_TERRESTRIAL_ANALOG", "TV_TERRESTRIAL_DIGITAL",
            "TV_SATELLITE", "TV_SATELLITE_BS", "TV_SATELLITE_CS", "TV_SATELLITE_SERVICE", "TV_NETWORK", "TV_ANTENNA_CABLE",
            "TV_INPUT_HDMI_1", "TV_INPUT_HDMI_2", "TV_INPUT_HDMI_3", "TV_INPUT_HDMI_4",
            "TV_INPUT_COMPOSITE_1", "TV_INPUT_COMPOSITE_2", "TV_INPUT_COMPONENT_1", "TV_INPUT_COMPONENT_2", "TV_INPUT_VGA_1",
            "TV_AUDIO_DESCRIPTION", "TV_AUDIO_DESCRIPTION_MIX_UP", "TV_AUDIO_DESCRIPTION_MIX_DOWN",
            "TV_ZOOM_MODE", "TV_CONTENTS_MENU", "TV_MEDIA_CONTEXT_MENU", "TV_TIMER_PROGRAMMING",
            "Help", "NAVIGATE_PREVIOUS", "NAVIGATE_NEXT", "NAVIGATE_IN", "NAVIGATE_OUT",
            "STEM_PRIMARY", "STEM_1", "STEM_2", "STEM_3",
            "Pointer_UpLeft", "Pointer_DownLeft", "Pointer_UpRight", "Pointer_DownRight",
            "MEDIA_SKIP_FORWARD", "MEDIA_SKIP_BACKWARD", "MEDIA_STEP_FORWARD", "MEDIA_STEP_BACKWARD",
            "SOFT_SLEEP", "CUT", "COPY", "PASTE",
            "SYSTEM_NAVIGATION_UP", "SYSTEM_NAVIGATION_DOWN", "SYSTEM_NAVIGATION_LEFT", "SYSTEM_NAVIGATION_RIGHT",
            "A", "B", "C", "D", "E", "F", "G", "H", "I", "J", "K", "L", "M", "N", "O", "P", "Q", "R", "S", "T", "U", "V", "W", "X", "Y", "Z",
            "exclam", "quotedbl", "dollar", "percent", "ampersand", "colon", "less", "greater", "question", "asciicircum", "underscore", "braceleft", "bar", "braceright", "asciitilde"
    };
    // Android 键名列表（从 RIME_KEY_NAMES 转换而来）
    public static List<String> androidKeys = Arrays.asList(RIME_KEY_NAMES);
    // 预定义的按键配置表（从主题中加载）
    public static LuaValue presetKeys = ThemeManager.getPresetKeys();
    // 事件类型的数量
    private static final int EVENT_NUM = KeyEventType.values().length;
    // 是否为编码区按键（影响显示和行为）
    private boolean mComposingKey;
    // 弹出按键列表（长按或滑动时显示）
    private List popupKeys;
    // 是否朗读按键标签（辅助功能）
    private boolean speak_key_label;
    // 各种事件类型的 Event 对象数组
    public Event[] events = new Event[EVENT_NUM];
    // 各种事件类型的提示文本数组
    public String[] hints = new String[EVENT_NUM];
    // 边缘标志（标识按键是否位于键盘边缘）
    public int edgeFlags;
    // 符号字符的起始索引位置
    private static int symbolStart = androidKeys.contains("A") ? Key.androidKeys.indexOf("A") : 284;
    // 符号字符集合字符串
    private static String symbols = "ABCDEFGHIJKLMNOPQRSTUVWXYZ~!@#$%^&*()_+[]\\{}|;':\",./<>?";
    // 虚拟键盘的字符映射表
    private static KeyCharacterMap kcm = KeyCharacterMap.load(KeyCharacterMap.VIRTUAL_KEYBOARD);
    // ASCII 模式下的事件配置
    private Event ascii;
    // 编码状态下的事件配置
    private Event composing;
    // 有候选词菜单时的事件配置
    private Event has_menu;
    // 翻页时的事件配置
    private Event paging;
    // 是否发送键绑定事件（根据 Rime 状态动态切换）
    private boolean send_bindings = true;
    // 按键宽度（像素）
    private int width;
    // 按键高度（像素）
    private int height;
    // 按键间距（像素）
    private int gap;
    // 按键所在行号
    private int row;
    // 按键所在列号
    private int column;
    // 按键显示的标签文本
    private String label;
    // 按键的提示信息
    private String hint;
    // 按键的详细描述（用于无障碍朗读）
    private String description;
    // 按键左上角的 X 坐标
    private int x;
    // 按键左上角的 Y 坐标
    private int y;
    // 按键是否处于按下状态
    private boolean pressed;
    // 按键是否处于开启状态（粘滞键/切换键）
    private boolean on;
    // 弹出字符集（长按时显示的字符）
    private String popupCharacters;
    // 弹出窗口的资源 ID
    private int popupResId;
    // 滑动点击事件映射表（方向 -> Event）
    private HashMap<String, Event> mSwipeTapKeys = new HashMap<>();
    // 是否使用绝对坐标检测（用于不规则形状按键）
    private boolean mAbsolute;
    // 按键样式名称（用于主题渲染）
    private String mStyle = "";
    // 是否有滑动手势事件
    private boolean mHasSwipeEvent;
    // ASCII 模式专用的按键对象（可独立配置）
    private Key mAsciiKey;
    // Shift 状态专用的按键对象（可独立配置）
    private Key mShiftKey;
    // 滑动事件是否可重复触发
    private boolean mSwipeRepeatable;
    // 当前是否处于 ASCII 模式
    private boolean mAsciiMode;

    private static final String TAG = "Key";


    /**
     * 构造函数，从 Lua 配置表中解析按键的所有属性。
     * 支持单击、长按、滑动等多种事件类型，以及 ASCII 模式、编码状态等条件分支。
     *
     * @param mk 从 YAML 配置中解析得到的 Lua 表，包含按键的所有配置项。
     */
    public Key(LuaValue mk) {
        String s;
        // 事件类型名称数组，与 KeyEventType 枚举对应
        String[] eventTypes =
                new String[]{
                        "click", "long_click", "swipe_left", "swipe_right", "swipe_up", "swipe_down", "combo"
                };
        // 提示文本类型名称数组，与事件类型一一对应
        String[] hintTypes =
                new String[]{
                        "hint", "hint_long", "hint_left", "hint_right", "hint_up", "hint_down", "combo"
                };
        // 遍历所有事件类型，解析事件配置和提示文本
        for (int i = 0; i < EVENT_NUM; i++) {
            String hintType = hintTypes[i];
            LuaValue h = mk.get(hintType);
            if (h.isstring()) {
                hints[i] = h.tojstring();
            }
            String eventType = eventTypes[i];
            LuaValue o = mk.get(eventType);
            // 检测是否有滑动手势事件（索引 >= 2 表示滑动事件）
            if (i >= 2 && !mHasSwipeEvent && !o.isnil()) {
                mHasSwipeEvent = true;
            }
            // 根据配置类型创建 Event 对象：表类型或字符串类型
            if (o != null && o.istable()) {
                events[i] = new Event(o);
            } else if (o != null && o.isstring()) {
                s = o.toString();
                events[i] = new Event(s);
            } else if (i == KeyEventType.CLICK.ordinal()) {
                // 单击事件特殊处理：如果有 commit 字段则从整个表构造
                if (!mk.get("commit").isnil())
                    events[i] = new Event(mk);
                else
                    events[i] = new Event("");
            }
        }
        // 解析编码状态下的事件配置
        s = mk.get("composing").optjstring("");
        if (!TextUtils.isEmpty(s)) composing = new Event(s);

        // 解析有候选词菜单时的事件配置
        s = mk.get("has_menu").optjstring("");
        if (!TextUtils.isEmpty(s)) has_menu = new Event(s);

        // 解析翻页时的事件配置
        s = mk.get("paging").optjstring("");
        if (!TextUtils.isEmpty(s)) paging = new Event(s);

        // 如果有任何条件事件，则标记为编码区按键
        if (composing != null || has_menu != null || paging != null) {
            mComposingKey = true;
        }

        // 解析 ASCII 模式下的事件配置（可以是字符串或表）
        LuaValue a = mk.get("ascii");
        if (a.isstring()) {
            ascii = new Event(a.tojstring());
        } else if (a.istable()) {
            if (!a.get("click").isnil()) {
                // 如果 ASCII 配置包含完整的按键定义，则创建独立的 Key 对象
                mAsciiKey = new Key(a);
                mAsciiKey.setAsciiMode(true);
            } else {
                ascii = new Event(a);
            }
        }
        
        // 解析 Shift 状态下的事件配置（可以是字符串或表）
        LuaValue shift = mk.get("shift");
        if (shift.isstring()) {
            // shift 配置为字符串时，创建一个简单的事件
            // 这种情况较少见，通常 shift 配置是一个表
        } else if (shift.istable()) {
            if (!shift.get("click").isnil()) {
                // 如果 Shift 配置包含完整的按键定义，则创建独立的 Key 对象
                mShiftKey = new Key(shift);
            }
        }
        // 确定按键样式名称，优先使用 style 字段，否则使用 click 事件
        mStyle = mk.get("style").optjstring(mk.get("click").optjstring("key"));
        // 解析按键的基本显示属性
        label = mk.get("label").optjstring("");
        hint = mk.get("hint").optjstring("");
        description = mk.get("description").optjstring("");
        // 解析滑动事件是否可重复触发
        mSwipeRepeatable = mk.get("swipe_repeatable").toboolean();
        // send_bindings 逻辑转换：控制是否根据 Rime 状态动态切换事件
        if (!mk.get("send_bindings").isnil()) {
            send_bindings = mk.get("send_bindings").optboolean(false);
        } else if (composing == null && has_menu == null && paging == null) {
            // 如果没有条件事件，默认不发送绑定
            send_bindings = false;
        }

        int c = getCode();
        String l = getLabel();

        // 读取是否启用按键朗读功能（无障碍支持）
        speak_key_label = Config.isSpeakKeyLabel();
        // 解析长按弹出窗口配置
        LuaValue obj = mk.get("popup");
        if (!obj.isnil()) {
            // 弹出配置可以是表（按键列表）或字符串（字符集）
            if (obj.istable()) {
                popupKeys = (List) obj.checktable().stringValues();
                // 如果启用了长按弹出功能，且按键是单字母，则自动添加大小写变体
                if (TrimeService.getInstance().isLongPressPopup()) {
                    String ll = getLabel();
                    if (ll.length() == 1 && Character.isLetter(ll.charAt(0))) {
                        if (getX() < getWidth()) {
                            popupKeys.add(ll.toUpperCase());
                        } else {
                            popupKeys.add(0, ll.toUpperCase());
                        }
                        if (getX() > TrimeService.getInstance().getWidth() - getWidth() * 1.5) {
                            popupKeys.add(1, ll.toLowerCase());
                        } else {
                            popupKeys.add(ll.toLowerCase());
                        }
                    }
                }
            } else {
                popupCharacters = obj.toString();
            }
            // 设置弹出窗口的资源 ID（1=长按弹出，2=其他）
            if (TrimeService.getInstance().isLongPressPopup())
                popupResId = 1;
            else
                popupResId = 2;
        } else if (getLongClick() != null && getLongClick().getCode() != KeyEvent.KEYCODE_VOICE_ASSIST && !TextUtils.isEmpty(getLongClick().getRawText())) {
            // 如果有长按事件且不是语音输入，也使用长按弹出窗口
            if (TrimeService.getInstance().isLongPressPopup())
                popupResId = 1;
        }

        // 解析滑动点击事件映射（swipe 表配置）
        if (TrimeService.getInstance().isKeySwipeTap()) {
            LuaValue swipe = mk.get("swipe");
            if (swipe.istable()) {
                LuaValue k = LuaValue.NIL;
                while (true) {
                    Varargs n = swipe.next(k);
                    if ((k = n.arg1()).isnil())
                        break;
                    mSwipeTapKeys.put(k.optjstring(""), new Event(n.arg(2).optjstring("")));
                }
            }
        }

    }

    /**
     * 设置按键的 ASCII 模式标志。
     *
     * @param b true 表示处于 ASCII 模式，false 表示中文模式。
     */
    private void setAsciiMode(boolean b) {
        mAsciiMode = b;
    }

    /**
     * 构造函数，从字符串创建简单的按键对象。
     * 如果字符串是单个字母，则自动设置提交文本为该字母。
     *
     * @param s 按键的事件文本或命令。
     */
    public Key(String s) {
        events[0] = new Event(s);
        if (s.length() == 1 && Character.isLetter(s.charAt(0)))
            events[0].setCommit(s);
    }

    /**
     * 构造函数，直接使用 Event 对象创建按键。
     *
     * @param e 单击事件的 Event 对象。
     */
    public Key(Event e) {
        events[0] = e;
    }

    /**
     * 判断该按键是否为编码区按键（有 composing、has_menu 或 paging 事件）。
     *
     * @return true 如果是编码区按键，false 否则。
     */
    public boolean isComposingKey() {
        return mComposingKey;
    }

    /**
     * 获取 Android 键名列表。
     *
     * @return 包含所有 Android 键名的不可变列表。
     */
    public static List<String> getAndroidKeys() {
        return androidKeys;
    }

    /**
     * 获取预定义的按键配置表。
     *
     * @return Lua 表，包含从主题中加载的预设按键配置。
     */
    public static LuaValue getPresetKeys() {
        return presetKeys;
    }

    /**
     * 获取符号字符的起始索引。
     *
     * @return 符号字符在 androidKeys 列表中的起始位置。
     */
    public static int getSymbolStart() {
        return symbolStart;
    }

    /**
     * 设置符号字符的起始索引。
     *
     * @param symbolStart 新的起始索引值。
     */
    public static void setSymbolStart(int symbolStart) {
        Key.symbolStart = symbolStart;
    }

    /**
     * 获取符号字符集合。
     *
     * @return 包含所有符号字符的字符串。
     */
    public static String getSymbols() {
        return symbols;
    }

    /**
     * 设置符号字符集合。
     *
     * @param symbols 新的符号字符字符串。
     */
    public static void setSymbols(String symbols) {
        Key.symbols = symbols;
    }

    /**
     * 获取虚拟键盘的字符映射表。
     *
     * @return KeyCharacterMap 对象，用于将 Unicode 字符转换为键码。
     */
    public static KeyCharacterMap getKcm() {
        return kcm;
    }

    /**
     * 判断键码是否为数字或字母。
     *
     * @param keyCode Android 键码。
     * @return true 如果是 0-9 或 A-Z，false 否则。
     */
    public static boolean isNumOrAlpha(int keyCode) {
        return (keyCode >= KeyEvent.KEYCODE_0 && keyCode <= KeyEvent.KEYCODE_9) || (keyCode >= KeyEvent.KEYCODE_A && keyCode <= KeyEvent.KEYCODE_Z);
    }

    /**
     * 获取按键宽度。
     *
     * @return 按键宽度（像素）。
     */
    public int getWidth() {
        return width;
    }

    /**
     * 设置按键宽度。
     *
     * @param width 新的宽度值（像素）。
     */
    public void setWidth(int width) {
        this.width = width;
    }

    /**
     * 获取按键高度。
     *
     * @return 按键高度（像素）。
     */
    public int getHeight() {
        return height;
    }

    /**
     * 设置按键高度。
     *
     * @param height 新的高度值（像素）。
     */
    public void setHeight(int height) {
        this.height = height;
    }

    /**
     * 获取按键间距。
     *
     * @return 按键之间的间距（像素）。
     */
    public int getGap() {
        return gap;
    }

    /**
     * 设置按键间距。
     *
     * @param gap 新的间距值（像素）。
     */
    public void setGap(int gap) {
        this.gap = gap;
    }

    /**
     * 获取边缘标志。
     *
     * @return 边缘标志位，标识按键是否位于键盘边缘。
     */
    public int getEdgeFlags() {
        return edgeFlags;
    }

    /**
     * 设置边缘标志。
     *
     * @param edgeFlags 新的边缘标志值。
     */
    public void setEdgeFlags(int edgeFlags) {
        this.edgeFlags = edgeFlags;
    }

    /**
     * 获取按键所在行号。
     *
     * @return 行号（从0开始）。
     */
    public int getRow() {
        return row;
    }

    /**
     * 设置按键所在行号。
     *
     * @param row 新的行号。
     */
    public void setRow(int row) {
        this.row = row;
    }

    /**
     * 获取按键所在列号。
     *
     * @return 列号（从0开始）。
     */
    public int getColumn() {
        return column;
    }

    /**
     * 设置按键所在列号。
     *
     * @param column 新的列号。
     */
    public void setColumn(int column) {
        this.column = column;
    }

    /**
     * 获取按键的提示文本。
     * 在中文状态下，如果 hint 不为空且当前事件是单击事件，则返回 hint。
     *
     * @return 提示文本字符串。
     */
    public String getHint() {
        Event event = getEvent();
        if (!TextUtils.isEmpty(hint) && event == getClick() && (ascii == null && !Rime.isAsciiMode()))
            return hint; //中文状态显示标签
        /*String h = event.getUnToggleLabel();
        if(!TextUtils.isEmpty(h))
            return h;*/
        return hint;
    }

    /**
     * 获取按键左上角的 X 坐标。
     *
     * @return X 坐标值（像素）。
     */
    public int getX() {
        return x;
    }

    /**
     * 设置按键左上角的 X 坐标。
     *
     * @param x 新的 X 坐标值。
     */
    public void setX(int x) {
        this.x = x;
    }

    /**
     * 获取按键左上角的 Y 坐标。
     *
     * @return Y 坐标值（像素）。
     */
    public int getY() {
        return y;
    }

    /**
     * 设置按键左上角的 Y 坐标。
     *
     * @param y 新的 Y 坐标值。
     */
    public void setY(int y) {
        this.y = y;
    }

    /**
     * 判断按键是否处于按下状态。
     *
     * @return true 如果按下，false 否则。
     */
    public boolean isPressed() {
        return pressed;
    }

    /**
     * 判断按键是否处于开启状态（粘滞键/切换键）。
     *
     * @return true 如果开启，false 否则。
     */
    public boolean isOn() {
        return on;
    }

    /**
     * 设置按键的开启状态。
     *
     * @param on true 表示开启，false 表示关闭。
     */
    public void setOn(boolean on) {
        this.on = on;
    }

    /**
     * 获取弹出字符集。
     *
     * @return 长按时显示的字符字符串。
     */
    public String getPopupCharacters() {
        return popupCharacters;
    }

    /**
     * 获取弹出按键列表。
     * 如果 popupKeys 为空但 popupResId 为1，则根据 popupCharacters 或长按事件生成列表。
     * 对于单字母按键，会自动添加大小写变体。
     *
     * @return 弹出按键的文本列表。
     */
    public List<String> getPopupKeys() {
        if (popupKeys == null && popupResId == 1) {
            popupKeys = new ArrayList();
            if (popupCharacters != null) {
                for (int i = 0; i < popupCharacters.length(); i++) {
                    popupKeys.add(String.valueOf(popupCharacters.charAt(i)));
                }
            } else {
                popupKeys.add(getLongClick().getRawText());
            }
            String ll = getLabel();
            if (ll.length() == 1 && Character.isLetter(ll.charAt(0))) {
                if (getX() < getWidth()) {
                    popupKeys.add(ll.toUpperCase());
                } else {
                    popupKeys.add(0, ll.toUpperCase());
                }
                if (getX() > TrimeService.getInstance().getWidth() - getWidth() * 1.5) {
                    popupKeys.add(1, ll.toLowerCase());
                } else {
                    popupKeys.add(ll.toLowerCase());
                }
            }
            popupResId = 1;
        }
        return popupKeys;
    }

    /**
     * 获取弹出窗口的资源 ID。
     *
     * @return 1 表示长按弹出，2 表示其他类型。
     */
    public int getPopupResId() {
        return popupResId;
    }

    /**
     * 判断给定的 Drawable 状态是否为普通状态（未按下）。
     *
     * @param drawableState Drawable 的状态数组。
     * @return true 如果是普通状态（NORMAL、NORMAL_ON 或 NORMAL_OFF），false 否则。
     */
    public boolean isNormal(int[] drawableState) {
        return (drawableState == KEY_STATE_NORMAL
                || drawableState == KEY_STATE_NORMAL_ON
                || drawableState == KEY_STATE_NORMAL_OFF);
    }

    /**
     * 通知按键已被按下，可能需要改变外观或状态。
     * 切换 pressed 标志位。
     *
     * @see #onReleased(boolean)
     */
    public void onPressed() {
        pressed = !pressed;
    }

    /**
     * 改变按键的按下状态。如果是粘滞键，当手指在按键内释放时还会切换开启状态。
     *
     * @param inside 手指是否在按键内部释放。
     * @see #onPressed()
     */
    public void onReleased(boolean inside) {
        pressed = !pressed;
        if (getClick().isSticky()) on = !on;
    }

    /**
     * Detects if a point falls inside this key.
     *
     * @param x the x-coordinate of the point
     * @param y the y-coordinate of the point
     * @return whether or not the point falls inside the key. If the key is attached to an edge, it
     * will assume that all points between the key and the edge are considered to be inside the
     * key.
     */
    /*public boolean isInside(int x, int y) {
        boolean leftEdge = (edgeFlags & KeyboardView.EDGE_LEFT) > 0;
        boolean rightEdge = (edgeFlags & KeyboardView.EDGE_RIGHT) > 0;
        boolean topEdge = (edgeFlags & KeyboardView.EDGE_TOP) > 0;
        boolean bottomEdge = (edgeFlags & KeyboardView.EDGE_BOTTOM) > 0;
        if ((x >= this.x || (leftEdge && x <= this.x + this.width))
                && (x < this.x + this.width || (rightEdge && x >= this.x))
                && (y >= this.y || (topEdge && y <= this.y + this.height))
                && (y < this.y + this.height || (bottomEdge && y >= this.y))) {
            if (mAbsolute && key_back_color instanceof LuaBitmapDrawable) {
                return ((LuaBitmapDrawable) key_back_color).isInside(x - this.x, y - this.y);
            }
            return true;
        } else {
            return false;
        }
    }
    */

    /**
     * 计算给定点与按键中心之间的距离的平方。
     * 用于判断触摸点是否接近按键中心。
     *
     * @param x 点的 X 坐标。
     * @param y 点的 Y 坐标。
     * @return 距离的平方值。
     */
    public int squaredDistanceFrom(int x, int y) {
        int xDist = this.x + width / 2 - x;
        int yDist = this.y + height / 2 - y;
        return xDist * xDist + yDist * yDist;
    }

    /**
     * 根据当前状态和按键类型获取 Drawable 的状态数组。
     * 用于 StateListDrawable 选择合适的背景图片。
     *
     * @return Drawable 的状态数组。
     * @see android.graphics.drawable.StateListDrawable#setState(int[])
     */
    public int[] getCurrentDrawableState() {
        int[] states = KEY_STATE_NORMAL;
        boolean isShifted = isShift() && ModifierState.isShifted(); //临时大写
        if (isShifted || on) {
            if (pressed) {
                states = KEY_STATE_PRESSED_ON;
            } else {
                states = KEY_STATE_NORMAL_ON;
            }
        } else {
            if (getClick().isSticky() || getClick().isFunctional()) {
                if (pressed) {
                    states = KEY_STATE_PRESSED_OFF;
                } else {
                    states = KEY_STATE_NORMAL_OFF;
                }
            } else {
                if (pressed) {
                    states = KEY_STATE_PRESSED;
                }
            }
        }
        return states;
    }

    /**
     * 判断该按键是否为 Shift 键。
     *
     * @return true 如果是左 Shift 或右 Shift，false 否则。
     */
    public boolean isShift() {
        int c = getEvent().getCode();
        return (c == KeyEvent.KEYCODE_SHIFT_LEFT || c == KeyEvent.KEYCODE_SHIFT_RIGHT);
    }

    /**
     * 判断 Shift 键的锁定模式。
     *
     * @return true 如果单击锁定，false 如果长按锁定或根据 ASCII 模式决定。
     */
    public boolean isShiftLock() {
        switch (getClick().getShiftLock()) {
            case "long":
                return false;
            case "click":
                return true;
        }
        return !Rime.isAsciiMode();
    }

    /**
     * 判断是否应该发送键绑定事件。
     * 根据 Rime 引擎的状态（编码中、有菜单、翻页等）动态决定使用哪个事件。
     *
     * @param type 事件类型索引（0-6）。
     * @return true 如果应该发送该类型的事件，false 否则。
     */
    public boolean sendBindings(int type) {
        Event e = null;
        if (type > 0 && type <= EVENT_NUM) e = events[type];
        if (e != null) return true;
        if (ascii != null && Rime.isAsciiMode()) return false;
        if (send_bindings) {
            if (paging != null && Rime.isPaging()) return true;
            if (has_menu != null && Rime.hasMenu()) return true;
            if (composing != null && Rime.getRimeStatus().isComposing()) return true;
        }
        return false;
    }

    /**
     * 获取当前应该触发的事件对象。
     * 根据 Rime 状态和 ASCII 模式动态选择：ASCII 模式 -> paging -> has_menu -> composing -> click。
     *
     * @return 当前有效的 Event 对象。
     */
    public Event getEvent() {
        if (ascii != null && Rime.isAsciiMode()) return ascii;
        if (paging != null && Rime.isPaging()) return paging;
        if (has_menu != null && Rime.hasMenu()) return has_menu;
        if (composing != null && Rime.getRimeStatus().isComposing()) return composing;
        return getClick();
    }

    /**
     * 获取单击事件。
     *
     * @return 单击事件的 Event 对象。
     */
    public Event getClick() {
        return events[KeyEventType.CLICK.ordinal()];
    }

    /**
     * 获取长按事件。
     *
     * @return 长按事件的 Event 对象。
     */
    public Event getLongClick() {
        return events[KeyEventType.LONG_CLICK.ordinal()];
    }

    /**
     * 判断指定索引的事件是否存在。
     *
     * @param i 事件类型索引。
     * @return true 如果该位置有 Event 对象，false 否则。
     */
    public boolean hasEvent(int i) {
        return events[i] != null;
    }

    /**
     * 获取指定索引的事件对象。
     * 如果该位置为空且索引不为0，则返回 null；否则根据 Rime 状态动态选择事件。
     *
     * @param i 事件类型索引（0-6）。
     * @return Event 对象或 null。
     */
    public Event getEvent(int i) {
        Event e = null;
        if (i > 0 && i <= EVENT_NUM) e = events[i];
        if (e != null) return e;
        if (i != 0)
            return null;
        if (ascii != null && Rime.isAsciiMode()) return ascii;
        if (send_bindings) {
            if (paging != null && Rime.isPaging()) return paging;
            if (has_menu != null && Rime.hasMenu()) return has_menu;
            if (composing != null && Rime.isComposing()) return composing;
        }
        return getClick();
    }

    /**
     * 获取原始的指定索引的事件对象（不进行状态判断）。
     *
     * @param i 事件类型索引。
     * @return 原始 Event 对象。
     */
    public Event getRawEvent(int i) {
        return events[i];
    }

    /**
     * 获取单击事件的键码。
     *
     * @return Android 键码值。
     */
    public int getCode() {
        return getClick().getCode();
    }

    /**
     * 获取指定类型事件的键码。
     *
     * @param type 事件类型索引。
     * @return Android 键码值。
     */
    public int getCode(int type) {
        return getEvent(type).getCode();
    }

    /**
     * 获取按键显示的标签文本。
     * 根据 Rime 状态、ASCII 模式等条件动态决定返回哪个标签。
     * 特殊处理：Enter 键在编码区外显示动作标签，空格键显示方案名称。
     *
     * @return 标签文本字符串。
     */
    public String getLabel() {
        Event event = getEvent();
        if (event == getClick() /*&& (ascii == null || !Rime.isAsciiMode())*/) {
            if (event.getCode() == KeyEvent.KEYCODE_ENTER && !Rime.isComposing() && "action_labels".equals(event.getLabel())) {
                TrimeService trime = TrimeService.getInstance();
                if (trime != null) {
                    String action = trime.getActionLabel();
                    if (!TextUtils.isEmpty(action))
                        return action;
                }
            }
            if (Rime.isAsciiMode() && !mAsciiMode)
                return event.getLabel();
            if (event.getCode() == KeyEvent.KEYCODE_SPACE) {
                if (!Rime.isAsciiMode()) {
                    if (TextUtils.isEmpty(event.getLabel()) || "space".equals(event.getLabel()) || "schema_name".equals(label)) {
                        String id = Rime.getRimeStatus().getSchemaName();
                        if (!TextUtils.isEmpty(id))
                            return id;
                        else if ("schema_name".equals(label))
                            return event.getLabel();
                    }
                }
            }
            if (!TextUtils.isEmpty(label))
                return label;
        }
        return event.getLabel();
    }

    /**
     * 获取按键的详细描述（用于无障碍朗读）。
     * 优先使用 description 字段，其次使用 label，最后使用事件的描述。
     *
     * @return 描述文本字符串。
     */
    public String getDescription() {
        Event event = getEvent();
        if (event.getCode() == KeyEvent.KEYCODE_ENTER && !Rime.isComposing() && event == getClick()) {
            TrimeService trime = TrimeService.getInstance();
            if (trime != null) {
                String action = trime.getActionLabel();
                if (!TextUtils.isEmpty(action))
                    return action;
            }
        }
        if (event == getClick() && (ascii == null && !Rime.isAsciiMode())) {
            if (!TextUtils.isEmpty(description))
                return description;
            if (speak_key_label && !TextUtils.isEmpty(label))
                return label;
        }
        return event.getDescription();
    }

    /**
     * 获取指定事件类型的预览文本。
     *
     * @param type 事件类型索引。
     * @return 预览文本字符串。
     */
    public String getPreviewText(int type) {
        if (type == KeyEventType.CLICK.ordinal()) return getEvent().getPreviewText();
        return getEvent(type).getPreviewText();
    }

    /**
     * 获取长按事件的标签文本。
     *
     * @return 长按事件的标签，如果没有长按事件则返回 null。
     */
    public String getLongClickLabel() {
        Event event = getLongClick();
        if (event == null)
            return null;
        if (hints[1] != null)
            return hints[1];
        return event.getLabel();
    }

    /**
     * 根据 Shift 状态和 ASCII 模式动态获取长按事件的标签文本。
     * 用于更新按键的助记显示，使其与实际输出保持一致。
     *
     * @param isShifted   Shift 键是否按下
     * @param isAsciiMode 当前是否为 ASCII 模式
     * @return 动态选择的长按事件标签，如果没有则返回 null
     */
    public String getDynamicLongClickLabel(boolean isShifted, boolean isAsciiMode) {
        Event dynamicEvent = getDynamicLongClick(isShifted, isAsciiMode);
        if (dynamicEvent == null)
            return null;
        // 优先使用 hint_long，其次使用事件的标签
        if (hints[KeyEventType.LONG_CLICK.ordinal()] != null)
            return hints[KeyEventType.LONG_CLICK.ordinal()];
        return dynamicEvent.getLabel();
    }

    /**
     * 返回按键的字符串表示，用于调试。
     * 包含标签、键码、长按事件、ASCII 事件和位置尺寸信息。
     *
     * @return 按键信息的字符串描述。
     */
    @Override
    public String toString() {
        StringBuilder buf = new StringBuilder();
        buf.append("Key:{")
                .append("label:")
                .append(getLabel())
                .append(",")
                .append("code:")
                .append(getCode())
                .append(",")
                .append("long_click:")
                .append(getLongClick())
                .append(",")
                .append("ascii:")
                .append(ascii)
                .append(",")
                .append(" ")
                .append(getX())
                .append(",")
                .append(getY())
                .append("-")
                .append(getWidth())
                .append(",")
                .append(getHeight())
        ;
        return buf.toString();
    }

    /**
     * 获取指定方向的滑动点击事件。
     *
     * @param s 方向字符串（如 "left"、"right"、"up"、"down"）。
     * @return 对应的 Event 对象，如果没有则返回 null。
     */
    public Event getSwipeTapKeys(String s) {
        return mSwipeTapKeys.get(s);
    }

    /**
     * 判断是否有滑动点击事件配置。
     *
     * @return true 如果有滑动点击事件，false 否则。
     */
    public boolean hasSwipeTapKeys() {
        return !mSwipeTapKeys.isEmpty();
    }

    /**
     * 设置是否使用绝对坐标检测（用于不规则形状按键）。
     *
     * @param b true 表示使用绝对坐标，false 表示使用矩形边界。
     */
    public void setAbsolute(boolean b) {
        mAbsolute = b;
    }

    /**
     * 判断是否使用绝对坐标检测。
     *
     * @return true 如果使用绝对坐标，false 否则。
     */
    public boolean isAbsolute() {
        return mAbsolute;
    }

    /**
     * 获取按键的样式名称。
     *
     * @return 样式名称字符串，用于主题渲染。
     */
    public String getStyle() {
        return mStyle;
    }

    /**
     * 判断该按键是否有滑动手势事件配置。
     *
     * @return true 如果有滑动事件，false 否则。
     */
    public boolean hasSwipeEvent() {
        return mHasSwipeEvent;
    }

    /**
     * 获取指定滑动方向的提示文本。
     * 优先使用 hints 数组，其次使用对应事件的标签。
     *
     * @param swipe 滑动方向索引（0-6）。
     * @return 提示文本字符串，如果没有则返回 null。
     */
    public String getHint(int swipe) {
        String h = hints[swipe];
        if (h != null)
            return h;
        Event ev = events[swipe];
        if (ev != null)
            return ev.getLabel();
        return null;
    }

    /**
     * 获取 ASCII 模式专用的按键对象。
     *
     * @return ASCII 模式的 Key 对象，如果没有独立配置则返回 null。
     */
    public Key getAsciiKey() {
        return mAsciiKey;
    }

    /**
     * 获取 Shift 状态专用的按键对象。
     *
     * @return Shift 状态的 Key 对象，如果没有独立配置则返回 null。
     */
    public Key getShiftKey() {
        return mShiftKey;
    }

    /**
     * 判断滑动事件是否可重复触发。
     *
     * @return true 如果可重复，false 否则。
     */
    public boolean isSwipeRepeatable() {
        return mSwipeRepeatable;
    }

    /**
     * 根据 Shift 状态和 ASCII 模式动态获取长按事件。
     * 对于 q-p 键位(字母行),根据以下条件返回不同的事件:
     * - 无 Shift + 中文模式:返回 long_click(数字 1-0)
     * - 无 Shift + 英文模式:返回 ascii.long_click(可能是其他配置)
     * - 有 Shift + 中文模式:返回 shift.long_click(符号 !@#$% 等)
     * - 有 Shift + 英文模式:返回 shift.ascii.long_click(英文符号配置)
     * - 其他键位:返回普通的 long_click
     *
     * @param isShifted   Shift 键是否按下
     * @param isAsciiMode 当前是否为 ASCII 模式
     * @return 动态选择的 Event 对象
     */
    public Event getDynamicLongClick(boolean isShifted, boolean isAsciiMode) {
        // 检查是否是字母键位(单字母)
        String label = getLabel();
        if (label != null && label.length() == 1) {
            char ch = label.charAt(0);

            // 修正:支持小写(a-z)和大写(A-Z)字母键位
            // Shift 状态下 label 会变成大写,所以需要同时检查两种情况
            boolean isLetter = (ch >= 'a' && ch <= 'z') || (ch >= 'A' && ch <= 'Z');
            if (isLetter) {
                if (!isShifted) {
                    // 无 Shift:根据 ASCII 模式返回基础 long_click
                    if (isAsciiMode && mAsciiKey != null) {
                        // ASCII 模式下使用 ascii 配置的 long_click
                        return mAsciiKey.getLongClick();
                    } else {
                        // 中文模式使用默认 long_click
                        return getLongClick();
                    }
                } else {
                    // 有 Shift:根据 ASCII 模式返回 shift 配置的 long_click
                    if (mShiftKey != null) {
                        // 如果配置了独立的 shift key
                        if (isAsciiMode && mShiftKey.mAsciiKey != null) {
                            // Shift+ASCII 模式:优先使用 shift.ascii 配置
                            return mShiftKey.mAsciiKey.getLongClick();
                        } else {
                            // Shift+中文模式:使用 shift 配置
                            return mShiftKey.getLongClick();
                        }
                    }
                }
            }
        }
        // 其他键位或没有配置 shift 事件时,返回普通的 long_click
        return getLongClick();
    }
}
