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
import android.view.KeyEvent;

import com.osfans.trime.core.RimeKeyMap;
import com.osfans.trime.keyboard.ModifierState;
import com.osfans.trime.keyboard.KeyboardView;

import java.io.File;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import com.osfans.trime.core.Rime;

import org.luaj.LuaValue;

/**
 * 按键事件处理类，负责管理键盘按键的各种事件（单击、长按、滑动等）。
 * 该类解析按键配置，处理按键的显示标签、文本输出、命令执行等逻辑。
 * 支持通过预设配置（presetKeys）或直接的按键码来定义按键行为。
 *
 * @author osfans
 * @see Key
 */
public class Event {
    /** 原始按键字符串，未经解析的原始配置值 */
    private String mRaw;

    /** 按键索引，用于标识按键在列表中的位置，默认为0 */
    private Integer index = 0;

    /** 日志标签，用于调试输出 */
    private String TAG = "Event";

    /** 按键码，对应Android KeyEvent的按键代码 */
    private int code = 0;

    /** 修饰键掩码，用于表示Shift、Ctrl、Alt等修饰键的状态 */
    private int mask = 0;

    /** 按键文本，按下按键后需要提交的文本内容 */
    private String text;

    /** 按键显示标签，在键盘上显示的文本 */
    private String label;

    /** 按键描述信息，用于无障碍服务或提示信息 */
    private String description;

    /** 按键预览文本，长按或滑动时显示的预览内容 */
    private String preview;

    /** 按键状态列表，用于toggle类型的按键在不同状态间切换时显示不同的标签 */
    private List<String> states;

    /** 命令字符串，按键触发时要执行的命令 */
    private String command;

    /** 选项字符串，用于设置或清除Rime选项 */
    private String option;

    /** 选择字符串，用于选择候选项或执行选择相关操作 */
    private String select;

    /** 切换选项名称，用于切换Rime的某个布尔选项（如ascii_mode） */
    private String toggle;

    /** 直接提交的字符串，按下按键后直接提交的内容 */
    private String commit;

    /** Shift锁定行为，定义Shift键的锁定方式，如"click"（单击锁定）等，默认为"click" */
    private String shiftLock = "click";

    /** 是否为功能键，功能键通常不产生文本输出，而是执行特定功能，默认为false */
    private boolean functional;

    /** 是否可重复，为true时长按按键会重复触发事件，默认为false */
    private boolean repeatable;

    /** 是否为粘性键，粘性键在点击后保持激活状态直到下次点击，默认为false */
    private boolean sticky;

    /**
     * 检查给定的掩码中是否包含指定的修饰键。
     *
     * @param mask 要检查的掩码值
     * @param modifier 要检查的修饰键（如KeyEvent.META_SHIFT_ON）
     * @return 如果掩码包含该修饰键则返回true，否则返回false
     */
    public static boolean hasModifier(int mask, int modifier) {
        return (mask & modifier) > 0;
    }

    /**
     * 修饰键名称到Android KeyEvent修饰键常量的映射表。
     * 用于将配置中的修饰键名称（如"Shift"）转换为对应的键值。
     */
    private static Map<String, Integer> masks =
            new HashMap<String, Integer>() {
                {
                    put("Shift", KeyEvent.META_SHIFT_ON);
                    put("Control", KeyEvent.META_CTRL_ON);
                    put("Alt", KeyEvent.META_ALT_ON);
                }
            };

    /**
     * 符号别名映射表，将特殊符号字符映射到对应的Android按键码。
     * 用于处理那些不能直接通过字符表示的符号按键。
     */
    private final static Map<String, Integer> symbolAliases =
            new HashMap<String, Integer>() {
                {
                    put("#", KeyEvent.KEYCODE_POUND);
                    put("'", KeyEvent.KEYCODE_APOSTROPHE);
                    put("(", KeyEvent.KEYCODE_NUMPAD_LEFT_PAREN);
                    put(")", KeyEvent.KEYCODE_NUMPAD_RIGHT_PAREN);
                    put("*", KeyEvent.KEYCODE_STAR);
                    put("+", KeyEvent.KEYCODE_PLUS);
                    put(",", KeyEvent.KEYCODE_COMMA);
                    put("-", KeyEvent.KEYCODE_MINUS);
                    put(".", KeyEvent.KEYCODE_PERIOD);
                    put("/", KeyEvent.KEYCODE_SLASH);
                    put(";", KeyEvent.KEYCODE_SEMICOLON);
                    put("=", KeyEvent.KEYCODE_EQUALS);
                    put("@", KeyEvent.KEYCODE_AT);
                    put("\\", KeyEvent.KEYCODE_BACKSLASH);
                    put("[", KeyEvent.KEYCODE_LEFT_BRACKET);
                    put("`", KeyEvent.KEYCODE_GRAVE);
                    put("]", KeyEvent.KEYCODE_RIGHT_BRACKET);
                }
            };

    /**
     * 根据按键码获取显示标签。
     * 该方法会优先使用KeyCharacterMap获取显示标签，如果失败则尝试从androidKeys或symbols中获取。
     *
     * @param keyCode 按键码
     * @return 对应的显示标签字符串，如果无法获取则返回空字符串
     */
    public static String getDisplayLabel(int keyCode) {
        String s = "";
        if (keyCode < Key.getSymbolStart()) { // 字母数字
            if (Key.getKcm().isPrintingKey(keyCode)) {
                char c = Key.getKcm().getDisplayLabel(keyCode);
                if (Character.isUpperCase(c)) c = Character.toLowerCase(c);
                s = String.valueOf(c);
            } else {
                s = Key.androidKeys.get(keyCode);
            }
        } else if (keyCode < Key.getSymbols().length() + Key.getSymbolStart()) { // 可见符号
            keyCode -= Key.getSymbolStart();
            s = Key.getSymbols().substring(keyCode, keyCode + 1);
        }
        return s;
    }

    /**
     * 解析发送字符串，提取按键码和修饰键掩码。
     * 发送字符串格式可以包含修饰键，如"Shift+A"表示按住Shift键的同时按A键。
     *
     * @param s 发送字符串，如"A"、"Shift+A"、"Ctrl+Alt+Delete"等
     * @return 包含按键码和掩码的数组，[0]=按键码，[1]=修饰键掩码
     */
    public static int[] parseSend(String s) {
        int[] sends = new int[2];
        if (TextUtils.isEmpty(s)) return sends;
        String codes;
        if (!s.contains("+")) codes = s;
        else {
            String[] ss = s.split("\\+");
            int n = ss.length;
            for (int i = 0; i < n - 1; i++)
                if (masks.containsKey(ss[i])) sends[1] |= masks.get(ss[i]);
            codes = ss[n - 1];
        }
        sends[0] = getClickCode(codes);
        return sends;
    }

    /**
     * 根据按键字符串获取对应的按键码。
     * 使用缓存机制提高查找效率，时间复杂度O(1)。
     *
     * @param s 按键字符串，可以是字母、数字、符号或特殊按键名称
     * @return 对应的按键码，如果找不到则返回-1
     */
    public static int getClickCode(String s) {
        if (s == null || s.isEmpty()) {
            return 0;
        }
        // 直接从 Map 获取，时间复杂度 O(1)
        Integer code = CLICK_CODE_CACHE.get(s);
        return (code != null) ? code : -1;
    }

    /*public static int getClickCode(String s) {
        int keyCode = -1;
        if (TextUtils.isEmpty(s)) { // 空键
            return 0;
        }
        if (Key.androidKeys.contains(s)) { // 字母数字
            return Key.androidKeys.indexOf(s);
        }
        if (symbolAliases.containsKey(s)) {
            return symbolAliases.get(s);
        }
        if (Key.getSymbols().contains(s)) { // 可见符号
            return Key.getSymbolStart() + Key.getSymbols().indexOf(s);
        }

        return -1;
    }*/

    /**
     * 按键码缓存表，用于快速查找按键字符串对应的按键码。
     * 在类加载时初始化，避免每次查找都进行线性搜索。
     */
    // 在类级别定义一个静态缓存
    private static final Map<String, Integer> CLICK_CODE_CACHE = new HashMap<>();

    static {
        // 1. 初始化 androidKeys (假设 Key.androidKeys 是 List<String>)
        for (int i = 0; i < Key.androidKeys.size(); i++) {
            CLICK_CODE_CACHE.put(Key.androidKeys.get(i), i);
        }

        // 2. 初始化 symbolAliases (假设它是一个已存在的 Map)
        //CLICK_CODE_CACHE.putAll(symbolAliases);

        // 3. 初始化 Symbols
        /*String symbols = Key.getSymbols();
        int start = Key.getSymbolStart();
        for (int i = 0; i < symbols.length(); i++) {
            // 1. 获取单个字符
            char c = symbols.charAt(i);
            // 2. 转换为 String 作为 key
            String key = String.valueOf(c);

            // 只有当缓存中不存在该 key 时才放入，或者直接放入以覆盖旧优先级
            // 建议：如果 androidKeys 优先级更高，可以用 putIfAbsent
            if (!CLICK_CODE_CACHE.containsKey(key))
                CLICK_CODE_CACHE.put(key, start + i);
        }*/
    }

    /**
     * 使用按键字符串构造Event对象。
     * 解析按键配置字符串，支持多种格式：
     * 1. {send} 或 {key} 格式
     * 2. 预设按键名称（从presetKeys中查找）
     * 3. 直接的按键码或字符
     * 4. .lua脚本文件
     * 5. 纯文本（作为text处理）
     *
     * @param s 按键配置字符串
     */
    public Event(String s) {
        mRaw = s;
        // 匹配 {send} 或 {key} 格式的字符串
        if (s.matches("\\{[^\\{\\}]+\\}")) { //{send|key}
            label = s.substring(1, s.length() - 1);
            int[] sends = parseSend(label); // 解析send
            code = sends[0];
            mask = sends[1];
            if (code >= 0) return;
            s = label; // 作为key处理
            label = null;
        }
        // 从预设按键表中查找配置
        LuaValue m = Key.presetKeys.get(s);
        if (!m.isnil()) {
            command = m.get("command").optjstring("");
            option = m.get("option").optjstring("");
            select = m.get("select").optjstring("");
            toggle = m.get("toggle").optjstring("");
            {
                LuaValue lv = m.get("label");
                label = lv.isnil() ? null : lv.optjstring("");
            }
            preview = m.get("preview").optjstring("");
            description = m.get("description").optjstring("");
            shiftLock = m.get("shift_lock").optjstring("");
            commit = m.get("commit").optjstring("");
            String send = m.get("send").optjstring("");
            if (TextUtils.isEmpty(send) && !TextUtils.isEmpty(command))
                send = "function"; // command默认发function
            int[] sends = parseSend(send);
            code = sends[0];
            mask = sends[1];
            parseLabel();
            text = m.get("text").optjstring("");
            if (code < 0 && TextUtils.isEmpty(text)){
                if(TextUtils.isEmpty(send))
                    text=s;
                else
                    text = send;
            }
            LuaValue st = m.get("states");
            if (st.istable()) {
                states = st.checktable().stringValues();
            }
            sticky = m.get("sticky").optboolean(false);
            repeatable = m.get("repeatable").optboolean(false);
            functional = m.get("functional").optboolean(true);
        } else if ((code = getClickCode(s)) >= 0) {
            // 如果是Rime的VoidSymbol（无效符号），则将原始字符串作为text
            if (getRimeCode(code)==RimeKeyMap.RimeKey_VoidSymbol)
                text = s;
            parseLabel();
        } else if (s.endsWith(".lua")) {
            // 处理Lua脚本文件
            String send = "function";
            int[] sends = parseSend(send);
            code = sends[0];
            mask = sends[1];
            command = s;
            s = new File(s).getName();
            label = s.substring(0, s.length() - 4);
            option = "";
        } else {
            // 作为纯文本处理
            text = s;
            label = s.replaceAll("\\{[^\\{\\}]+?\\}", "");
        }
    }

    /**
     * 使用LuaValue配置构造Event对象。
     * 从Lua表中读取按键配置信息，包括命令、标签、文本等属性。
     *
     * @param m 包含按键配置的Lua表
     */
    public Event(LuaValue m) {
        command = m.get("command").optjstring("");
        index = m.get("index").optint(0);
        option = m.get("option").optjstring("");
        select = m.get("select").optjstring("");
        toggle = m.get("toggle").optjstring("");
        {
            LuaValue lv = m.get("label");
            label = lv.isnil() ? null : lv.optjstring("");
        }
        preview = m.get("preview").optjstring("");
        description = m.get("description").optjstring("");
        shiftLock = m.get("shift_lock").optjstring("");
        commit = m.get("commit").optjstring("");
        String send = m.get("send").optjstring("");

        if (TextUtils.isEmpty(send) && !TextUtils.isEmpty(command))
            send = "function"; // command默认发function

        int[] sends = parseSend(send);
        code = sends[0];
        mask = sends[1];
        parseLabel();

        text = m.get("text").optjstring("");
        LuaValue st = m.get("states");
        if (st.istable()) {
            states = st.checktable().stringValues();
        }
        sticky = m.get("sticky").optboolean(false);
        repeatable = m.get("repeatable").optboolean(false);
        functional = m.get("functional").optboolean(true);
    }

    /**
     * 将Android按键码和修饰键掩码转换为Rime引擎所需的格式。
     *
     * @param code Android按键码
     * @param mask Android修饰键掩码
     * @return 包含Rime按键码和修饰键掩码的数组 [Rime按键码, Rime修饰键掩码]
     */
    public static int[] getRimeEvent(int code, int mask) {
        int i = getRimeCode(code);
        int m = 0;
        if (hasModifier(mask, KeyEvent.META_SHIFT_ON)) m |= Rime.META_SHIFT_ON;
        if (hasModifier(mask, KeyEvent.META_CTRL_ON)) m |= Rime.META_CTRL_ON;
        if (hasModifier(mask, KeyEvent.META_ALT_ON)) m |= Rime.META_ALT_ON;
        if (mask == Rime.META_RELEASE_ON) m |= Rime.META_RELEASE_ON;
        return new int[]{i, m};
    }

    /**
     * 将Android按键码转换为Rime按键码。
     *
     * @param code Android按键码
     * @return 对应的Rime按键码
     */
    private static int getRimeCode(int code) {
        return RimeKeyMap.keyCodeToVal(code);
    }

    /**
     * 获取Android按键码。
     *
     * @return Android按键码
     */
    public int getCode() {
        return code;
    }

    /**
     * 获取按键索引。
     *
     * @return 按键索引
     */
    public int getIndex() {
        return index;
    }

    /**
     * 获取修饰键掩码。
     *
     * @return 修饰键掩码
     */
    public int getMask() {
        return mask;
    }

    /**
     * 获取命令字符串。
     *
     * @return 命令字符串
     */
    public String getCommand() {
        return command;
    }

    /**
     * 获取选项字符串。
     *
     * @return 选项字符串
     */
    public String getOption() {
        return option;
    }

    /**
     * 获取选择字符串。
     *
     * @return 选择字符串
     */
    public String getSelect() {
        return select;
    }

    /**
     * 判断是否为功能键。
     * 功能键通常不产生文本输出，而是执行特定功能。
     *
     * @return 如果是功能键返回true，否则返回false
     */
    public boolean isFunctional() {
        return functional;
    }

    /**
     * 判断是否可重复。
     * 可重复的按键在长按时会重复触发事件。
     *
     * @return 如果可重复返回true，否则返回false
     */
    public boolean isRepeatable() {
        return repeatable;
    }

    /**
     * 判断是否为粘性键。
     * 粘性键在点击后保持激活状态直到下次点击。
     *
     * @return 如果是粘性键返回true，否则返回false
     */
    public boolean isSticky() {
        return sticky;
    }

    /**
     * 获取Shift锁定行为。
     *
     * @return Shift锁定行为字符串
     */
    public String getShiftLock() {
        return shiftLock;
    }

    /**
     * 根据Shift状态调整字符串的大小写。
     * 如果Shift处于激活状态且字符串长度为1，则转换为大写。
     *
     * @param s 要调整大小写的字符串
     * @return 调整后的字符串
     */
    private String adjustCase(String s) {
        if (TextUtils.isEmpty(s)) return "";
        if (s.length() == 1 && ModifierState.isShifted())
            s = s.toUpperCase(Locale.getDefault());
        //else if (s.length() == 1
        //        //&& mKeyboardView != null
        //        && !Rime.isAsciiMode()
        //        //&& mKeyboardView.isLabelUppercase()
        //) s = s.toUpperCase(Locale.getDefault());
        return s;
    }

    /**
     * 获取按键的显示标签。
     * 如果是toggle类型的按键且存在状态列表，则根据当前状态返回对应的标签。
     * 否则返回经过大小写调整的标签。
     *
     * @return 显示标签
     */
    public String getLabel() {
        if (!TextUtils.isEmpty(toggle)&&states!=null) return states.get(Rime.getRimeOption(toggle) ? 1 : 0);
        return adjustCase(label);
    }

    /**
     * 获取反转toggle状态的标签。
     * 用于显示切换前的状态标签。
     *
     * @return 反转状态的标签，如果不是toggle类型则返回null
     */
    public String getUnToggleLabel() {
        if (!TextUtils.isEmpty(toggle)) return states.get(Rime.getRimeOption(toggle) ? 0 : 1);
        return null;
    }

    /**
     * 获取按键文本。
     * 优先返回配置的text，如果没有且Shift激活、无修饰键、是字母键，则返回label。
     * 最后会根据Shift状态调整大小写。
     *
     * @return 按键文本
     */
    public String getText() {
        String s = "";
        if (!TextUtils.isEmpty(text)) s = text;
        else if (ModifierState.isShifted()
                && mask == 0
                && code >= KeyEvent.KEYCODE_A
                && code <= KeyEvent.KEYCODE_Z) s = label;
        return adjustCase(s);
    }

    /**
     * 获取直接提交的字符串。
     *
     * @return 提交的字符串
     */
    public String getCommit() {
        return commit;
    }

    /**
     * 获取预览文本。
     * 如果有配置preview则返回preview，否则返回label。
     *
     * @return 预览文本
     */
    public String getPreviewText() {
        if (!TextUtils.isEmpty(preview)) return preview;
        return getLabel();
    }

    /**
     * 获取切换选项名称。
     * 如果未配置toggle，默认返回"ascii_mode"。
     *
     * @return 切换选项名称
     */
    public String getToggle() {
        if (!TextUtils.isEmpty(toggle)) return toggle;
        return "ascii_mode";
    }

    /**
     * 解析并设置按键标签。
     * 如果label已经设置则直接返回，否则根据按键码生成显示标签。
     */
    private void parseLabel() {
        if (label != null) return;
        int c = code;
        if (c > 0)
            label = getDisplayLabel(c);
        //if (c == KeyEvent.KEYCODE_SPACE) {
        //    label = Rime.getRimeStatus().getSchemaName();
        //    if(TextUtils.isEmpty(label))
        //        label="空格";
        //} else {
        //    if (c > 0)
        //        label = getDisplayLabel(c);
        //}
    }

    /**
     * 获取Rime按键码。
     *
     * @return Rime按键码
     */
    public int getRimeCode() {
        return RimeKeyMap.keyCodeToVal(code);
    }

    /**
     * 获取按键描述信息。
     * 优先返回配置的描述，如果是toggle类型则返回状态切换描述，
     * 否则返回经过大小写调整的标签。
     *
     * @return 描述信息
     */
    public String getDescription() {
        if (!TextUtils.isEmpty(description))
            return description;
        if (!TextUtils.isEmpty(toggle)) {
            boolean t = Rime.getRimeOption(toggle);
            return "当前为" + states.get(t ? 1 : 0) + ",切换到" + states.get(t ? 0 : 1);
        }
        return adjustCase(label);
    }

    /**
     * 获取原始按键字符串。
     *
     * @return 原始按键字符串
     */
    public String getRawText() {
        return mRaw;
    }

    /**
     * 设置提交的字符串。
     *
     * @param s 要提交的字符串
     */
    public void setCommit(String s) {
        commit = s;
    }

    /**
     * 判断是否为toggle类型按键。
     *
     * @return 如果是toggle类型返回true，否则返回false
     */
    public boolean isToggle() {
        return !TextUtils.isEmpty(toggle);
    }

    /**
     * 返回对象的字符串表示。
     *
     * @return 包含标签和原始字符串的表示
     */
    public String toString() {
        return TAG+":{label:"+getLabel()+",raw:"+mRaw+"}";
    }
}
