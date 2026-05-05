/*
 * SPDX-FileCopyrightText: 2015 - 2026 Rime community
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package com.osfans.trime.keyboard;

import com.osfans.trime.util.Function;

/**
 * 修饰键状态管理类。
 * 用于跟踪 Shift、Ctrl、Alt 等修饰键的当前状态。
 * 所有方法均为静态方法,通过类名直接访问。
 */
public class ModifierState {
    // ==================== Shift 键状态 ====================
    /** Shift 键是否按下 */
    private static boolean shift=false;

    /**
     * 检查 Shift 键是否按下。
     *
     * @return true 表示 Shift 键已按下。
     */
    public static boolean isShifted() {
        return shift;
    }

    /**
     * 设置 Shift 键的状态。
     *
     * @param shift true 表示按下,false 表示释放。
     */
    public static void setShifted(boolean shift) {
        ModifierState.shift = shift;
    }

    // ==================== Shift Lock 状态 ====================
    /** Shift 键是否锁定(大写锁定) */
    private static boolean shiftLock=false;

    /**
     * 检查 Shift 键是否锁定。
     *
     * @return true 表示 Shift 键已锁定。
     */
    public static boolean isShiftLock() {
        return shiftLock;
    }

    /**
     * 设置 Shift 键的锁定状态。
     *
     * @param shiftLock true 表示锁定,false 表示解锁。
     */
    public static void setShiftLock(boolean shiftLock) {
        ModifierState.shiftLock = shiftLock;
    }

    // ==================== Ctrl 键状态 ====================
    /** Ctrl 键是否按下 */
    private static boolean ctrl=false;

    /**
     * 检查 Ctrl 键是否按下。
     *
     * @return true 表示 Ctrl 键已按下。
     */
    public static boolean isCtrl() {
        return ctrl;
    }

    /**
     * 设置 Ctrl 键的状态。
     *
     * @param ctrl true 表示按下,false 表示释放。
     */
    public static void setCtrl(boolean ctrl) {
        ModifierState.ctrl = ctrl;
    }

    // ==================== Alt 键状态 ====================
    /** Alt 键是否按下 */
    private static boolean alt=false;

    /**
     * 检查 Alt 键是否按下。
     *
     * @return true 表示 Alt 键已按下。
     */
    public static boolean isAlt() {
        return alt;
    }

    /**
     * 设置 Alt 键的状态。
     *
     * @param alt true 表示按下,false 表示释放。
     */
    public static void setAlt(boolean alt) {
        ModifierState.alt = alt;
    }


}
