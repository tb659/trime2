package com.osfans.trime.core;

import android.view.KeyEvent;

/**
 * Rime 键值映射类。
 * 提供 Rime 键值与 Android KeyEvent 之间的双向映射,
 * 以及键值与字符串名称之间的转换。
 */
public final class RimeKeyMap {

    /** 私有构造函数防止实例化 */
    private RimeKeyMap() {}

    // ==================== 常量定义 ====================
    
    // --- 特殊字符键 ---
    public static final int RimeKey_space = 0x0020; // 空格键
    public static final int RimeKey_numbersign = 0x0023; // # 号键
    public static final int RimeKey_apostrophe = 0x0027; // 单引号键
    public static final int RimeKey_asterisk = 0x002a; // * 号键
    public static final int RimeKey_plus = 0x002b; // + 号键
    public static final int RimeKey_comma = 0x002c; // 逗号键
    public static final int RimeKey_minus = 0x002d; // 减号键
    public static final int static_final_int_RimeKey_period = 0x002e; // 句号键(重复定义)
    public static final int RimeKey_period = 0x002e; // 句号键
    public static final int RimeKey_slash = 0x002f; // 斜杠键
    
    // --- 数字键 ---
    public static final int RimeKey_0 = 0x0030; // 数字0
    public static final int RimeKey_1 = 0x0031; // 数字1
    public static final int RimeKey_2 = 0x0032; // 数字2
    public static final int RimeKey_3 = 0x0033; // 数字3
    public static final int RimeKey_4 = 0x0034; // 数字4
    public static final int RimeKey_5 = 0x0035; // 数字5
    public static final int RimeKey_6 = 0x0036; // 数字6
    public static final int RimeKey_7 = 0x0037; // 数字7
    public static final int RimeKey_8 = 0x0038; // 数字8
    public static final int RimeKey_9 = 0x0039; // 数字9
    
    // --- 符号键 ---
    public static final int RimeKey_semicolon = 0x003b; // 分号键
    public static final int RimeKey_equal = 0x003d; // 等号键
    public static final int RimeKey_at = 0x0040; // @ 符号键
    
    // --- 大写字母键 ---
    public static final int RimeKey_A = 0x0041; // 字母A
    public static final int RimeKey_B = 0x0042; // 字母B
    public static final int RimeKey_C = 0x0043; // 字母C
    public static final int RimeKey_D = 0x0044; // 字母D
    public static final int RimeKey_E = 0x0045; // 字母E
    public static final int RimeKey_F = 0x0046; // 字母F
    public static final int RimeKey_G = 0x0047; // 字母G
    public static final int RimeKey_H = 0x0048; // 字母H
    public static final int RimeKey_I = 0x0049; // 字母I
    public static final int RimeKey_J = 0x004a; // 字母J
    public static final int RimeKey_K = 0x004b; // 字母K
    public static final int RimeKey_L = 0x004c; // 字母L
    public static final int RimeKey_M = 0x004d; // 字母M
    public static final int RimeKey_N = 0x004e; // 字母N
    public static final int RimeKey_O = 0x004f; // 字母O
    public static final int RimeKey_P = 0x0050; // 字母P
    public static final int RimeKey_Q = 0x0051; // 字母Q
    public static final int RimeKey_R = 0x0052; // 字母R
    public static final int RimeKey_S = 0x0053; // 字母S
    public static final int RimeKey_T = 0x0054; // 字母T
    public static final int RimeKey_U = 0x0055; // 字母U
    public static final int RimeKey_V = 0x0056; // 字母V
    public static final int RimeKey_W = 0x0057; // 字母W
    public static final int RimeKey_X = 0x0058; // 字母X
    public static final int RimeKey_Y = 0x0059; // 字母Y
    public static final int RimeKey_Z = 0x005a; // 字母Z
    
    // --- 括号和反引号键 ---
    public static final int RimeKey_bracketleft = 0x005b; // 左方括号 [
    public static final int RimeKey_backslash = 0x005c; // 反斜杠 \
    public static final int RimeKey_bracketright = 0x005d; // 右方括号 ]
    public static final int RimeKey_grave = 0x0060; // 反引号 `
    
    // --- 小写字母键 ---
    public static final int RimeKey_a = 0x0061; // 字母a
    public static final int RimeKey_b = 0x0062; // 字母b
    public static final int RimeKey_c = 0x0063; // 字母c
    public static final int RimeKey_d = 0x0064; // 字母d
    public static final int RimeKey_e = 0x0065; // 字母e
    public static final int RimeKey_f = 0x0066; // 字母f
    public static final int RimeKey_g = 0x0067; // 字母g
    public static final int RimeKey_h = 0x0068; // 字母h
    public static final int RimeKey_i = 0x0069; // 字母i
    public static final int RimeKey_j = 0x006a; // 字母j
    public static final int RimeKey_k = 0x006b; // 字母k
    public static final int RimeKey_l = 0x006c; // 字母l
    public static final int RimeKey_m = 0x006d; // 字母m
    public static final int RimeKey_n = 0x006e; // 字母n
    public static final int RimeKey_o = 0x006f; // 字母o
    public static final int RimeKey_p = 0x0070; // 字母p
    public static final int RimeKey_q = 0x0071; // 字母q
    public static final int RimeKey_r = 0x0072; // 字母r
    public static final int RimeKey_s = 0x0073; // 字母s
    public static final int RimeKey_t = 0x0074; // 字母t
    public static final int RimeKey_u = 0x0075; // 字母u
    public static final int RimeKey_v = 0x0076; // 字母v
    public static final int RimeKey_w = 0x0077; // 字母w
    public static final int RimeKey_x = 0x0078; // 字母x
    public static final int RimeKey_y = 0x0079; // 字母y
    public static final int RimeKey_z = 0x007a; // 字母z
    // --- 功能键 F1-F12 ---
    public static final int RimeKey_F1 = 0xffbe; // F1功能键
    public static final int RimeKey_F2 = 0xffbf; // F2功能键
    public static final int RimeKey_F3 = 0xffc0; // F3功能键
    public static final int RimeKey_F4 = 0xffc1; // F4功能键
    public static final int RimeKey_F5 = 0xffc2; // F5功能键
    public static final int RimeKey_F6 = 0xffc3; // F6功能键
    public static final int RimeKey_F7 = 0xffc4; // F7功能键
    public static final int RimeKey_F8 = 0xffc5; // F8功能键
    public static final int RimeKey_F9 = 0xffc6; // F9功能键
    public static final int RimeKey_F10 = 0xffc7; // F10功能键
    public static final int RimeKey_F11 = 0xffc8; // F11功能键
    public static final int RimeKey_F12 = 0xffc9; // F12功能键
    
    // --- 修饰键(Shift、Control、Caps Lock、Meta、Alt) ---
    public static final int RimeKey_Shift_L = 0xffe1; // 左Shift键
    public static final int RimeKey_Shift_R = 0xffe2; // 右Shift键
    public static final int RimeKey_Control_L = 0xffe3; // 左Control键
    public static final int RimeKey_Control_R = 0xffe4; // 右Control键
    public static final int RimeKey_Caps_Lock = 0xffe5; // Caps Lock大写锁定键
    public static final int RimeKey_Meta_L = 0xffe7; // 左Meta键
    public static final int RimeKey_Meta_R = 0xffe8; // 右Meta键
    public static final int RimeKey_Alt_L = 0xffe9; // 左Alt键
    public static final int RimeKey_Alt_R = 0xffea; // 右Alt键
    
    // --- 编辑和导航键 ---
    public static final int RimeKey_Insert = 0xff63; // Insert插入键
    public static final int RimeKey_Delete = 0xffff; // Delete删除键
    public static final int RimeKey_Home = 0xff50; // Home键(行首)
    public static final int RimeKey_End = 0xff57; // End键(行尾)
    public static final int RimeKey_Page_Down = 0xff56; // Page Down下翻页键
    public static final int RimeKey_Page_Up = 0xff55; // Page Up上翻页键
    public static final int RimeKey_Tab = 0xff09; // Tab制表键
    public static final int RimeKey_BackSpace = 0xff08; // BackSpace退格键
    public static final int RimeKey_Return = 0xff0d; // Return回车键
    public static final int RimeKey_Escape = 0xff1b; // Escape退出键
    public static final int RimeKey_Up = 0xff52; // 向上方向键
    public static final int RimeKey_Down = 0xff54; // 向下方向键
    public static final int RimeKey_Left = 0xff51; // 向左方向键
    public static final int RimeKey_Right = 0xff53; // 向右方向键
    
    // --- 小键盘键 ---
    public static final int RimeKey_KP_Divide = 0xffaf; // 小键盘除号键 /
    public static final int RimeKey_KP_Multiply = 0xffaa; // 小键盘乘号键 *
    public static final int RimeKey_KP_Subtract = 0xffad; // 小键盘减号键 -
    public static final int RimeKey_KP_7 = 0xffb7; // 小键盘数字7
    public static final int RimeKey_KP_8 = 0xffb8; // 小键盘数字8
    public static final int RimeKey_KP_9 = 0xffb9; // 小键盘数字9
    public static final int RimeKey_KP_Add = 0xffab; // 小键盘加号键 +
    public static final int RimeKey_KP_4 = 0xffb4; // 小键盘数字4
    public static final int RimeKey_KP_5 = 0xffb5; // 小键盘数字5
    public static final int RimeKey_KP_6 = 0xffb6; // 小键盘数字6
    public static final int RimeKey_KP_1 = 0xffb1; // 小键盘数字1
    public static final int RimeKey_KP_2 = 0xffb2; // 小键盘数字2
    public static final int RimeKey_KP_3 = 0xffb3; // 小键盘数字3
    public static final int RimeKey_KP_Enter = 0xff8d; // 小键盘回车键
    public static final int RimeKey_KP_0 = 0xffb0; // 小键盘数字0
    public static final int RimeKey_KP_Decimal = 0xffae; // 小键盘小数点键 .
    
    // --- 日语输入相关键 ---
    public static final int RimeKey_Eisu_toggle = 0xff30; // 英数切换键(日语输入法)
    public static final int RimeKey_Kana_Lock = 0xff2d; // 假名锁定键(日语输入法)
    public static final int RimeKey_Hiragana_Katakana = 0xff27; // 平假名/片假名切换键
    public static final int RimeKey_Zenkaku_Hankaku = 0xff2a; // 全角/半角切换键
    public static final int RimeKey_VoidSymbol = 0xffffff; // 无效符号(空值)

    // ==================== 方法定义 ====================

    /**
     * 将 Rime 键值转换为 Android KeyEvent 键码。
     *
     * @param val Rime 键值(X11 keysym 标准)。
     * @return 对应的 Android KeyEvent 键码,未知键返回 KEYCODE_UNKNOWN。
     */
    public static int valToKeyCode(int val) {
        switch (val) {
            case RimeKey_space: return KeyEvent.KEYCODE_SPACE;
            case RimeKey_numbersign: return KeyEvent.KEYCODE_POUND;
            case RimeKey_apostrophe: return KeyEvent.KEYCODE_APOSTROPHE;
            case RimeKey_asterisk: return KeyEvent.KEYCODE_STAR;
            case RimeKey_plus: return KeyEvent.KEYCODE_PLUS;
            case RimeKey_comma: return KeyEvent.KEYCODE_COMMA;
            case RimeKey_minus: return KeyEvent.KEYCODE_MINUS;
            case RimeKey_period: return KeyEvent.KEYCODE_PERIOD;
            case RimeKey_slash: return KeyEvent.KEYCODE_SLASH;
            case RimeKey_0: return KeyEvent.KEYCODE_0;
            case RimeKey_1: return KeyEvent.KEYCODE_1;
            case RimeKey_2: return KeyEvent.KEYCODE_2;
            case RimeKey_3: return KeyEvent.KEYCODE_3;
            case RimeKey_4: return KeyEvent.KEYCODE_4;
            case RimeKey_5: return KeyEvent.KEYCODE_5;
            case RimeKey_6: return KeyEvent.KEYCODE_6;
            case RimeKey_7: return KeyEvent.KEYCODE_7;
            case RimeKey_8: return KeyEvent.KEYCODE_8;
            case RimeKey_9: return KeyEvent.KEYCODE_9;
            case RimeKey_semicolon: return KeyEvent.KEYCODE_SEMICOLON;
            case RimeKey_equal: return KeyEvent.KEYCODE_EQUALS;
            case RimeKey_at: return KeyEvent.KEYCODE_AT;
            case RimeKey_A: return KeyEvent.KEYCODE_A;
            case RimeKey_B: return KeyEvent.KEYCODE_B;
            case RimeKey_C: return KeyEvent.KEYCODE_C;
            case RimeKey_D: return KeyEvent.KEYCODE_D;
            case RimeKey_E: return KeyEvent.KEYCODE_E;
            case RimeKey_F: return KeyEvent.KEYCODE_F;
            case RimeKey_G: return KeyEvent.KEYCODE_G;
            case RimeKey_H: return KeyEvent.KEYCODE_H;
            case RimeKey_I: return KeyEvent.KEYCODE_I;
            case RimeKey_J: return KeyEvent.KEYCODE_J;
            case RimeKey_K: return KeyEvent.KEYCODE_K;
            case RimeKey_L: return KeyEvent.KEYCODE_L;
            case RimeKey_M: return KeyEvent.KEYCODE_M;
            case RimeKey_N: return KeyEvent.KEYCODE_N;
            case RimeKey_O: return KeyEvent.KEYCODE_O;
            case RimeKey_P: return KeyEvent.KEYCODE_P;
            case RimeKey_Q: return KeyEvent.KEYCODE_Q;
            case RimeKey_R: return KeyEvent.KEYCODE_R;
            case RimeKey_S: return KeyEvent.KEYCODE_S;
            case RimeKey_T: return KeyEvent.KEYCODE_T;
            case RimeKey_U: return KeyEvent.KEYCODE_U;
            case RimeKey_V: return KeyEvent.KEYCODE_V;
            case RimeKey_W: return KeyEvent.KEYCODE_W;
            case RimeKey_X: return KeyEvent.KEYCODE_X;
            case RimeKey_Y: return KeyEvent.KEYCODE_Y;
            case RimeKey_Z: return KeyEvent.KEYCODE_Z;
            case RimeKey_bracketleft: return KeyEvent.KEYCODE_LEFT_BRACKET;
            case RimeKey_backslash: return KeyEvent.KEYCODE_BACKSLASH;
            case RimeKey_bracketright: return KeyEvent.KEYCODE_RIGHT_BRACKET;
            case RimeKey_grave: return KeyEvent.KEYCODE_GRAVE;
            case RimeKey_a: return KeyEvent.KEYCODE_A;
            case RimeKey_b: return KeyEvent.KEYCODE_B;
            case RimeKey_c: return KeyEvent.KEYCODE_C;
            case RimeKey_d: return KeyEvent.KEYCODE_D;
            case RimeKey_e: return KeyEvent.KEYCODE_E;
            case RimeKey_f: return KeyEvent.KEYCODE_F;
            case RimeKey_g: return KeyEvent.KEYCODE_G;
            case RimeKey_h: return KeyEvent.KEYCODE_H;
            case RimeKey_i: return KeyEvent.KEYCODE_I;
            case RimeKey_j: return KeyEvent.KEYCODE_J;
            case RimeKey_k: return KeyEvent.KEYCODE_K;
            case RimeKey_l: return KeyEvent.KEYCODE_L;
            case RimeKey_m: return KeyEvent.KEYCODE_M;
            case RimeKey_n: return KeyEvent.KEYCODE_N;
            case RimeKey_o: return KeyEvent.KEYCODE_O;
            case RimeKey_p: return KeyEvent.KEYCODE_P;
            case RimeKey_q: return KeyEvent.KEYCODE_Q;
            case RimeKey_r: return KeyEvent.KEYCODE_R;
            case RimeKey_s: return KeyEvent.KEYCODE_S;
            case RimeKey_t: return KeyEvent.KEYCODE_T;
            case RimeKey_u: return KeyEvent.KEYCODE_U;
            case RimeKey_v: return KeyEvent.KEYCODE_V;
            case RimeKey_w: return KeyEvent.KEYCODE_W;
            case RimeKey_x: return KeyEvent.KEYCODE_X;
            case RimeKey_y: return KeyEvent.KEYCODE_Y;
            case RimeKey_z: return KeyEvent.KEYCODE_Z;
            case RimeKey_F1: return KeyEvent.KEYCODE_F1;
            case RimeKey_F2: return KeyEvent.KEYCODE_F2;
            case RimeKey_F3: return KeyEvent.KEYCODE_F3;
            case RimeKey_F4: return KeyEvent.KEYCODE_F4;
            case RimeKey_F5: return KeyEvent.KEYCODE_F5;
            case RimeKey_F6: return KeyEvent.KEYCODE_F6;
            case RimeKey_F7: return KeyEvent.KEYCODE_F7;
            case RimeKey_F8: return KeyEvent.KEYCODE_F8;
            case RimeKey_F9: return KeyEvent.KEYCODE_F9;
            case RimeKey_F10: return KeyEvent.KEYCODE_F10;
            case RimeKey_F11: return KeyEvent.KEYCODE_F11;
            case RimeKey_F12: return KeyEvent.KEYCODE_F12;
            case RimeKey_Shift_L: return KeyEvent.KEYCODE_SHIFT_LEFT;
            case RimeKey_Shift_R: return KeyEvent.KEYCODE_SHIFT_RIGHT;
            case RimeKey_Control_L: return KeyEvent.KEYCODE_CTRL_LEFT;
            case RimeKey_Control_R: return KeyEvent.KEYCODE_CTRL_RIGHT;
            case RimeKey_Caps_Lock: return KeyEvent.KEYCODE_CAPS_LOCK;
            case RimeKey_Meta_L: return KeyEvent.KEYCODE_META_LEFT;
            case RimeKey_Meta_R: return KeyEvent.KEYCODE_META_RIGHT;
            case RimeKey_Alt_L: return KeyEvent.KEYCODE_ALT_LEFT;
            case RimeKey_Alt_R: return KeyEvent.KEYCODE_ALT_RIGHT;
            case RimeKey_Insert: return KeyEvent.KEYCODE_INSERT;
            case RimeKey_Delete: return KeyEvent.KEYCODE_FORWARD_DEL;
            case RimeKey_Home: return KeyEvent.KEYCODE_MOVE_HOME;
            case RimeKey_End: return KeyEvent.KEYCODE_MOVE_END;
            case RimeKey_Page_Down: return KeyEvent.KEYCODE_PAGE_DOWN;
            case RimeKey_Page_Up: return KeyEvent.KEYCODE_PAGE_UP;
            case RimeKey_Tab: return KeyEvent.KEYCODE_TAB;
            case RimeKey_BackSpace: return KeyEvent.KEYCODE_DEL;
            case RimeKey_Return: return KeyEvent.KEYCODE_ENTER;
            case RimeKey_Escape: return KeyEvent.KEYCODE_ESCAPE;
            case RimeKey_Up: return KeyEvent.KEYCODE_DPAD_UP;
            case RimeKey_Down: return KeyEvent.KEYCODE_DPAD_DOWN;
            case RimeKey_Left: return KeyEvent.KEYCODE_DPAD_LEFT;
            case RimeKey_Right: return KeyEvent.KEYCODE_DPAD_RIGHT;
            case RimeKey_KP_Divide: return KeyEvent.KEYCODE_NUMPAD_DIVIDE;
            case RimeKey_KP_Multiply: return KeyEvent.KEYCODE_NUMPAD_MULTIPLY;
            case RimeKey_KP_Subtract: return KeyEvent.KEYCODE_NUMPAD_SUBTRACT;
            case RimeKey_KP_7: return KeyEvent.KEYCODE_NUMPAD_7;
            case RimeKey_KP_8: return KeyEvent.KEYCODE_NUMPAD_8;
            case RimeKey_KP_9: return KeyEvent.KEYCODE_NUMPAD_9;
            case RimeKey_KP_Add: return KeyEvent.KEYCODE_NUMPAD_ADD;
            case RimeKey_KP_4: return KeyEvent.KEYCODE_NUMPAD_4;
            case RimeKey_KP_5: return KeyEvent.KEYCODE_NUMPAD_5;
            case RimeKey_KP_6: return KeyEvent.KEYCODE_NUMPAD_6;
            case RimeKey_KP_1: return KeyEvent.KEYCODE_NUMPAD_1;
            case RimeKey_KP_2: return KeyEvent.KEYCODE_NUMPAD_2;
            case RimeKey_KP_3: return KeyEvent.KEYCODE_NUMPAD_3;
            case RimeKey_KP_Enter: return KeyEvent.KEYCODE_NUMPAD_ENTER;
            case RimeKey_KP_0: return KeyEvent.KEYCODE_NUMPAD_0;
            case RimeKey_KP_Decimal: return KeyEvent.KEYCODE_NUMPAD_DOT;
            case RimeKey_Eisu_toggle: return KeyEvent.KEYCODE_EISU;
            case RimeKey_Kana_Lock: return KeyEvent.KEYCODE_KANA;
            case RimeKey_Hiragana_Katakana: return KeyEvent.KEYCODE_KATAKANA_HIRAGANA;
            case RimeKey_Zenkaku_Hankaku: return KeyEvent.KEYCODE_ZENKAKU_HANKAKU;
            case RimeKey_VoidSymbol: return KeyEvent.KEYCODE_UNKNOWN;
            default: return KeyEvent.KEYCODE_UNKNOWN;
        }
    }

    /**
     * 将 Android KeyEvent 键码转换为 Rime 键值。
     *
     * @param code Android KeyEvent 键码。
     * @return 对应的 Rime 键值,未知键返回 RimeKey_VoidSymbol。
     * @note 字母键统一映射到小写 rime key。
     */
    public static int keyCodeToVal(int code) {
        switch (code) {
            case KeyEvent.KEYCODE_SPACE: return RimeKey_space;
            case KeyEvent.KEYCODE_POUND: return RimeKey_numbersign;
            case KeyEvent.KEYCODE_APOSTROPHE: return RimeKey_apostrophe;
            case KeyEvent.KEYCODE_STAR: return RimeKey_asterisk;
            case KeyEvent.KEYCODE_PLUS: return RimeKey_plus;
            case KeyEvent.KEYCODE_COMMA: return RimeKey_comma;
            case KeyEvent.KEYCODE_MINUS: return RimeKey_minus;
            case KeyEvent.KEYCODE_PERIOD: return RimeKey_period;
            case KeyEvent.KEYCODE_SLASH: return RimeKey_slash;
            case KeyEvent.KEYCODE_0: return RimeKey_0;
            case KeyEvent.KEYCODE_1: return RimeKey_1;
            case KeyEvent.KEYCODE_2: return RimeKey_2;
            case KeyEvent.KEYCODE_3: return RimeKey_3;
            case KeyEvent.KEYCODE_4: return RimeKey_4;
            case KeyEvent.KEYCODE_5: return RimeKey_5;
            case KeyEvent.KEYCODE_6: return RimeKey_6;
            case KeyEvent.KEYCODE_7: return RimeKey_7;
            case KeyEvent.KEYCODE_8: return RimeKey_8;
            case KeyEvent.KEYCODE_9: return RimeKey_9;
            case KeyEvent.KEYCODE_SEMICOLON: return RimeKey_semicolon;
            case KeyEvent.KEYCODE_EQUALS: return RimeKey_equal;
            case KeyEvent.KEYCODE_AT: return RimeKey_at;
            case KeyEvent.KEYCODE_LEFT_BRACKET: return RimeKey_bracketleft;
            case KeyEvent.KEYCODE_BACKSLASH: return RimeKey_backslash;
            case KeyEvent.KEYCODE_RIGHT_BRACKET: return RimeKey_bracketright;
            case KeyEvent.KEYCODE_GRAVE: return RimeKey_grave;
            case KeyEvent.KEYCODE_A: return RimeKey_a; // 映射到小写 rime key
            case KeyEvent.KEYCODE_B: return RimeKey_b;
            case KeyEvent.KEYCODE_C: return RimeKey_c;
            case KeyEvent.KEYCODE_D: return RimeKey_d;
            case KeyEvent.KEYCODE_E: return RimeKey_e;
            case KeyEvent.KEYCODE_F: return RimeKey_f;
            case KeyEvent.KEYCODE_G: return RimeKey_g;
            case KeyEvent.KEYCODE_H: return RimeKey_h;
            case KeyEvent.KEYCODE_I: return RimeKey_i;
            case KeyEvent.KEYCODE_J: return RimeKey_j;
            case KeyEvent.KEYCODE_K: return RimeKey_k;
            case KeyEvent.KEYCODE_L: return RimeKey_l;
            case KeyEvent.KEYCODE_M: return RimeKey_m;
            case KeyEvent.KEYCODE_N: return RimeKey_n;
            case KeyEvent.KEYCODE_O: return RimeKey_o;
            case KeyEvent.KEYCODE_P: return RimeKey_p;
            case KeyEvent.KEYCODE_Q: return RimeKey_q;
            case KeyEvent.KEYCODE_R: return RimeKey_r;
            case KeyEvent.KEYCODE_S: return RimeKey_s;
            case KeyEvent.KEYCODE_T: return RimeKey_t;
            case KeyEvent.KEYCODE_U: return RimeKey_u;
            case KeyEvent.KEYCODE_V: return RimeKey_v;
            case KeyEvent.KEYCODE_W: return RimeKey_w;
            case KeyEvent.KEYCODE_X: return RimeKey_x;
            case KeyEvent.KEYCODE_Y: return RimeKey_y;
            case KeyEvent.KEYCODE_Z: return RimeKey_z;
            case KeyEvent.KEYCODE_F1: return RimeKey_F1;
            case KeyEvent.KEYCODE_F2: return RimeKey_F2;
            case KeyEvent.KEYCODE_F3: return RimeKey_F3;
            case KeyEvent.KEYCODE_F4: return RimeKey_F4;
            case KeyEvent.KEYCODE_F5: return RimeKey_F5;
            case KeyEvent.KEYCODE_F6: return RimeKey_F6;
            case KeyEvent.KEYCODE_F7: return RimeKey_F7;
            case KeyEvent.KEYCODE_F8: return RimeKey_F8;
            case KeyEvent.KEYCODE_F9: return RimeKey_F9;
            case KeyEvent.KEYCODE_F10: return RimeKey_F10;
            case KeyEvent.KEYCODE_F11: return RimeKey_F11;
            case KeyEvent.KEYCODE_F12: return RimeKey_F12;
            case KeyEvent.KEYCODE_SHIFT_LEFT: return RimeKey_Shift_L;
            case KeyEvent.KEYCODE_SHIFT_RIGHT: return RimeKey_Shift_R;
            case KeyEvent.KEYCODE_CTRL_LEFT: return RimeKey_Control_L;
            case KeyEvent.KEYCODE_CTRL_RIGHT: return RimeKey_Control_R;
            case KeyEvent.KEYCODE_CAPS_LOCK: return RimeKey_Caps_Lock;
            case KeyEvent.KEYCODE_META_LEFT: return RimeKey_Meta_L;
            case KeyEvent.KEYCODE_META_RIGHT: return RimeKey_Meta_R;
            case KeyEvent.KEYCODE_ALT_LEFT: return RimeKey_Alt_L;
            case KeyEvent.KEYCODE_ALT_RIGHT: return RimeKey_Alt_R;
            case KeyEvent.KEYCODE_INSERT: return RimeKey_Insert;
            case KeyEvent.KEYCODE_FORWARD_DEL: return RimeKey_Delete;
            case KeyEvent.KEYCODE_MOVE_HOME: return RimeKey_Home;
            case KeyEvent.KEYCODE_MOVE_END: return RimeKey_End;
            case KeyEvent.KEYCODE_PAGE_DOWN: return RimeKey_Page_Down;
            case KeyEvent.KEYCODE_PAGE_UP: return RimeKey_Page_Up;
            case KeyEvent.KEYCODE_TAB: return RimeKey_Tab;
            case KeyEvent.KEYCODE_DEL: return RimeKey_BackSpace;
            case KeyEvent.KEYCODE_ENTER: return RimeKey_Return;
            case KeyEvent.KEYCODE_ESCAPE: return RimeKey_Escape;
            case KeyEvent.KEYCODE_DPAD_UP: return RimeKey_Up;
            case KeyEvent.KEYCODE_DPAD_DOWN: return RimeKey_Down;
            case KeyEvent.KEYCODE_DPAD_LEFT: return RimeKey_Left;
            case KeyEvent.KEYCODE_DPAD_RIGHT: return RimeKey_Right;
            case KeyEvent.KEYCODE_NUMPAD_DIVIDE: return RimeKey_KP_Divide;
            case KeyEvent.KEYCODE_NUMPAD_MULTIPLY: return RimeKey_KP_Multiply;
            case KeyEvent.KEYCODE_NUMPAD_SUBTRACT: return RimeKey_KP_Subtract;
            case KeyEvent.KEYCODE_NUMPAD_7: return RimeKey_KP_7;
            case KeyEvent.KEYCODE_NUMPAD_8: return RimeKey_KP_8;
            case KeyEvent.KEYCODE_NUMPAD_9: return RimeKey_KP_9;
            case KeyEvent.KEYCODE_NUMPAD_ADD: return RimeKey_KP_Add;
            case KeyEvent.KEYCODE_NUMPAD_4: return RimeKey_KP_4;
            case KeyEvent.KEYCODE_NUMPAD_5: return RimeKey_KP_5;
            case KeyEvent.KEYCODE_NUMPAD_6: return RimeKey_KP_6;
            case KeyEvent.KEYCODE_NUMPAD_1: return RimeKey_KP_1;
            case KeyEvent.KEYCODE_NUMPAD_2: return RimeKey_KP_2;
            case KeyEvent.KEYCODE_NUMPAD_3: return RimeKey_KP_3;
            case KeyEvent.KEYCODE_NUMPAD_ENTER: return RimeKey_KP_Enter;
            case KeyEvent.KEYCODE_NUMPAD_0: return RimeKey_KP_0;
            case KeyEvent.KEYCODE_NUMPAD_DOT: return RimeKey_KP_Decimal;
            case KeyEvent.KEYCODE_EISU: return RimeKey_Eisu_toggle;
            case KeyEvent.KEYCODE_KANA: return RimeKey_Kana_Lock;
            case KeyEvent.KEYCODE_KATAKANA_HIRAGANA: return RimeKey_Hiragana_Katakana;
            case KeyEvent.KEYCODE_ZENKAKU_HANKAKU: return RimeKey_Zenkaku_Hankaku;
            case KeyEvent.KEYCODE_UNKNOWN: return RimeKey_VoidSymbol;
            default: return RimeKey_VoidSymbol;
        }
    }

    /**
     * 将字符串名称转换为 Rime 键值。
     *
     * @param name 键的字符串名称(如 "space", "F1", "Shift_L" 等)。
     * @return 对应的 Rime 键值,未知名称或 null 返回 RimeKey_VoidSymbol。
     */
    public static int nameToKeyVal(String name) {
        if (name == null) return RimeKey_VoidSymbol;
        switch (name) {
            case "space": return RimeKey_space;
            case "numbersign": return RimeKey_numbersign;
            case "apostrophe": return RimeKey_apostrophe;
            case "asterisk": return RimeKey_asterisk;
            case "plus": return RimeKey_plus;
            case "comma": return RimeKey_comma;
            case "minus": return RimeKey_minus;
            case "period": return RimeKey_period;
            case "slash": return RimeKey_slash;
            case "0": return RimeKey_0;
            case "1": return RimeKey_1;
            case "2": return RimeKey_2;
            case "3": return RimeKey_3;
            case "4": return RimeKey_4;
            case "5": return RimeKey_5;
            case "6": return RimeKey_6;
            case "7": return RimeKey_7;
            case "8": return RimeKey_8;
            case "9": return RimeKey_9;
            case "semicolon": return RimeKey_semicolon;
            case "equal": return RimeKey_equal;
            case "at": return RimeKey_at;
            case "A": return RimeKey_A;
            case "B": return RimeKey_B;
            case "C": return RimeKey_C;
            case "D": return RimeKey_D;
            case "E": return RimeKey_E;
            case "F": return RimeKey_F;
            case "G": return RimeKey_G;
            case "H": return RimeKey_H;
            case "I": return RimeKey_I;
            case "J": return RimeKey_J;
            case "K": return RimeKey_K;
            case "L": return RimeKey_L;
            case "M": return RimeKey_M;
            case "N": return RimeKey_N;
            case "O": return RimeKey_O;
            case "P": return RimeKey_P;
            case "Q": return RimeKey_Q;
            case "R": return RimeKey_R;
            case "S": return RimeKey_S;
            case "T": return RimeKey_T;
            case "U": return RimeKey_U;
            case "V": return RimeKey_V;
            case "W": return RimeKey_W;
            case "X": return RimeKey_X;
            case "Y": return RimeKey_Y;
            case "Z": return RimeKey_Z;
            case "bracketleft": return RimeKey_bracketleft;
            case "backslash": return RimeKey_backslash;
            case "bracketright": return RimeKey_bracketright;
            case "grave": return RimeKey_grave;
            case "a": return RimeKey_a;
            case "b": return RimeKey_b;
            case "c": return RimeKey_c;
            case "d": return RimeKey_d;
            case "e": return RimeKey_e;
            case "f": return RimeKey_f;
            case "g": return RimeKey_g;
            case "h": return RimeKey_h;
            case "i": return RimeKey_i;
            case "j": return RimeKey_j;
            case "k": return RimeKey_k;
            case "l": return RimeKey_l;
            case "m": return RimeKey_m;
            case "n": return RimeKey_n;
            case "o": return RimeKey_o;
            case "p": return RimeKey_p;
            case "q": return RimeKey_q;
            case "r": return RimeKey_r;
            case "s": return RimeKey_s;
            case "t": return RimeKey_t;
            case "u": return RimeKey_u;
            case "v": return RimeKey_v;
            case "w": return RimeKey_w;
            case "x": return RimeKey_x;
            case "y": return RimeKey_y;
            case "z": return RimeKey_z;
            case "F1": return RimeKey_F1;
            case "F2": return RimeKey_F2;
            case "F3": return RimeKey_F3;
            case "F4": return RimeKey_F4;
            case "F5": return RimeKey_F5;
            case "F6": return RimeKey_F6;
            case "F7": return RimeKey_F7;
            case "F8": return RimeKey_F8;
            case "F9": return RimeKey_F9;
            case "F10": return RimeKey_F10;
            case "F11": return RimeKey_F11;
            case "F12": return RimeKey_F12;
            case "Shift_L": return RimeKey_Shift_L;
            case "Shift_R": return RimeKey_Shift_R;
            case "Control_L": return RimeKey_Control_L;
            case "Control_R": return RimeKey_Control_R;
            case "Caps_Lock": return RimeKey_Caps_Lock;
            case "Meta_L": return RimeKey_Meta_L;
            case "Meta_R": return RimeKey_Meta_R;
            case "Alt_L": return RimeKey_Alt_L;
            case "Alt_R": return RimeKey_Alt_R;
            case "Insert": return RimeKey_Insert;
            case "Delete": return RimeKey_Delete;
            case "Home": return RimeKey_Home;
            case "End": return RimeKey_End;
            case "Page_Down": return RimeKey_Page_Down;
            case "Page_Up": return RimeKey_Page_Up;
            case "Tab": return RimeKey_Tab;
            case "BackSpace": return RimeKey_BackSpace;
            case "Return": return RimeKey_Return;
            case "Escape": return RimeKey_Escape;
            case "Up": return RimeKey_Up;
            case "Down": return RimeKey_Down;
            case "Left": return RimeKey_Left;
            case "Right": return RimeKey_Right;
            case "KP_Divide": return RimeKey_KP_Divide;
            case "KP_Multiply": return RimeKey_KP_Multiply;
            case "KP_Subtract": return RimeKey_KP_Subtract;
            case "KP_7": return RimeKey_KP_7;
            case "KP_8": return RimeKey_KP_8;
            case "KP_9": return RimeKey_KP_9;
            case "KP_Add": return RimeKey_KP_Add;
            case "KP_4": return RimeKey_KP_4;
            case "KP_5": return RimeKey_KP_5;
            case "KP_6": return RimeKey_KP_6;
            case "KP_1": return RimeKey_KP_1;
            case "KP_2": return RimeKey_KP_2;
            case "KP_3": return RimeKey_KP_3;
            case "KP_Enter": return RimeKey_KP_Enter;
            case "KP_0": return RimeKey_KP_0;
            case "KP_Decimal": return RimeKey_KP_Decimal;
            case "Eisu_toggle": return RimeKey_Eisu_toggle;
            case "Kana_Lock": return RimeKey_Kana_Lock;
            case "Hiragana_Katakana": return RimeKey_Hiragana_Katakana;
            case "Zenkaku_Hankaku": return RimeKey_Zenkaku_Hankaku;
            case "VoidSymbol": return RimeKey_VoidSymbol;
            default: return RimeKey_VoidSymbol;
        }
    }

    /**
     * 将 Rime 键值转换为字符串名称。
     *
     * @param val Rime 键值。
     * @return 对应的字符串名称,未知键返回 "VoidSymbol"。
     */
    public static String keyValToName(int val) {
        switch (val) {
            case RimeKey_space: return "space";
            case RimeKey_numbersign: return "numbersign";
            case RimeKey_apostrophe: return "apostrophe";
            case RimeKey_asterisk: return "asterisk";
            case RimeKey_plus: return "plus";
            case RimeKey_comma: return "comma";
            case RimeKey_minus: return "minus";
            case RimeKey_period: return "period";
            case RimeKey_slash: return "slash";
            case RimeKey_0: return "0";
            case RimeKey_1: return "1";
            case RimeKey_2: return "2";
            case RimeKey_3: return "3";
            case RimeKey_4: return "4";
            case RimeKey_5: return "5";
            case RimeKey_6: return "6";
            case RimeKey_7: return "7";
            case RimeKey_8: return "8";
            case RimeKey_9: return "9";
            case RimeKey_semicolon: return "semicolon";
            case RimeKey_equal: return "equal";
            case RimeKey_at: return "at";
            case RimeKey_A: return "A";
            case RimeKey_B: return "B";
            case RimeKey_C: return "C";
            case RimeKey_D: return "D";
            case RimeKey_E: return "E";
            case RimeKey_F: return "F";
            case RimeKey_G: return "G";
            case RimeKey_H: return "H";
            case RimeKey_I: return "I";
            case RimeKey_J: return "J";
            case RimeKey_K: return "K";
            case RimeKey_L: return "L";
            case RimeKey_M: return "M";
            case RimeKey_N: return "N";
            case RimeKey_O: return "O";
            case RimeKey_P: return "P";
            case RimeKey_Q: return "Q";
            case RimeKey_R: return "R";
            case RimeKey_S: return "S";
            case RimeKey_T: return "T";
            case RimeKey_U: return "U";
            case RimeKey_V: return "V";
            case RimeKey_W: return "W";
            case RimeKey_X: return "X";
            case RimeKey_Y: return "Y";
            case RimeKey_Z: return "Z";
            case RimeKey_bracketleft: return "bracketleft";
            case RimeKey_backslash: return "backslash";
            case RimeKey_bracketright: return "bracketright";
            case RimeKey_grave: return "grave";
            case RimeKey_a: return "a";
            case RimeKey_b: return "b";
            case RimeKey_c: return "c";
            case RimeKey_d: return "d";
            case RimeKey_e: return "e";
            case RimeKey_f: return "f";
            case RimeKey_g: return "g";
            case RimeKey_h: return "h";
            case RimeKey_i: return "i";
            case RimeKey_j: return "j";
            case RimeKey_k: return "k";
            case RimeKey_l: return "l";
            case RimeKey_m: return "m";
            case RimeKey_n: return "n";
            case RimeKey_o: return "o";
            case RimeKey_p: return "p";
            case RimeKey_q: return "q";
            case RimeKey_r: return "r";
            case RimeKey_s: return "s";
            case RimeKey_t: return "t";
            case RimeKey_u: return "u";
            case RimeKey_v: return "v";
            case RimeKey_w: return "w";
            case RimeKey_x: return "x";
            case RimeKey_y: return "y";
            case RimeKey_z: return "z";
            case RimeKey_F1: return "F1";
            case RimeKey_F2: return "F2";
            case RimeKey_F3: return "F3";
            case RimeKey_F4: return "F4";
            case RimeKey_F5: return "F5";
            case RimeKey_F6: return "F6";
            case RimeKey_F7: return "F7";
            case RimeKey_F8: return "F8";
            case RimeKey_F9: return "F9";
            case RimeKey_F10: return "F10";
            case RimeKey_F11: return "F11";
            case RimeKey_F12: return "F12";
            case RimeKey_Shift_L: return "Shift_L";
            case RimeKey_Shift_R: return "Shift_R";
            case RimeKey_Control_L: return "Control_L";
            case RimeKey_Control_R: return "Control_R";
            case RimeKey_Caps_Lock: return "Caps_Lock";
            case RimeKey_Meta_L: return "Meta_L";
            case RimeKey_Meta_R: return "Meta_R";
            case RimeKey_Alt_L: return "Alt_L";
            case RimeKey_Alt_R: return "Alt_R";
            case RimeKey_Insert: return "Insert";
            case RimeKey_Delete: return "Delete";
            case RimeKey_Home: return "Home";
            case RimeKey_End: return "End";
            case RimeKey_Page_Down: return "Page_Down";
            case RimeKey_Page_Up: return "Page_Up";
            case RimeKey_Tab: return "Tab";
            case RimeKey_BackSpace: return "BackSpace";
            case RimeKey_Return: return "Return";
            case RimeKey_Escape: return "Escape";
            case RimeKey_Up: return "Up";
            case RimeKey_Down: return "Down";
            case RimeKey_Left: return "Left";
            case RimeKey_Right: return "Right";
            case RimeKey_KP_Divide: return "KP_Divide";
            case RimeKey_KP_Multiply: return "KP_Multiply";
            case RimeKey_KP_Subtract: return "KP_Subtract";
            case RimeKey_KP_7: return "KP_7";
            case RimeKey_KP_8: return "KP_8";
            case RimeKey_KP_9: return "KP_9";
            case RimeKey_KP_Add: return "KP_Add";
            case RimeKey_KP_4: return "KP_4";
            case RimeKey_KP_5: return "KP_5";
            case RimeKey_KP_6: return "KP_6";
            case RimeKey_KP_1: return "KP_1";
            case RimeKey_KP_2: return "KP_2";
            case RimeKey_KP_3: return "KP_3";
            case RimeKey_KP_Enter: return "KP_Enter";
            case RimeKey_KP_0: return "KP_0";
            case RimeKey_KP_Decimal: return "KP_Decimal";
            case RimeKey_Eisu_toggle: return "Eisu_toggle";
            case RimeKey_Kana_Lock: return "Kana_Lock";
            case RimeKey_Hiragana_Katakana: return "Hiragana_Katakana";
            case RimeKey_Zenkaku_Hankaku: return "Zenkaku_Hankaku";
            case RimeKey_VoidSymbol: return "VoidSymbol";
            default: return "VoidSymbol";
        }
    }
}
