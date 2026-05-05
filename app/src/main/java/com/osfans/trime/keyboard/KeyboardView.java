/*
 * SPDX-FileCopyrightText: 2015 - 2025 Rime community
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package com.osfans.trime.keyboard;

import android.content.Context;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.accessibility.AccessibilityManager;
import android.widget.FrameLayout;

import androidx.annotation.NonNull;

import com.androlua.LuaApplication;

import org.luaj.Globals;

import java.util.ArrayList;

/**
 * 键盘视图基类。
 * 管理所有按键(KeyView),处理触摸事件、ASCII 模式切换等功能。
 * 支持滑动选择和无障碍访问。
 */
public class KeyboardView extends FrameLayout {
    // ==================== 成员变量 ====================
    /** 所有按键列表 */
    private final ArrayList<KeyView> mKeys;
    /** 编码区按键列表 */
    private final ArrayList<KeyView> mComposingKeys;
    /** Lua 全局环境 */
    protected Globals globals;
    /** Shift 键状态 */
    private boolean mShifted;
    /** 无障碍管理器(单例缓存) */
    private static AccessibilityManager mAm;
    /** 是否启用按键滑动选择 */
    private boolean keySwipe = false;
    /** ASCII 模式状态 */
    private boolean mAsciiMode;
    /** ASCII 模式锁定状态 */
    private boolean mAsciiModeLock;
    /** 键盘锁定状态 */
    private boolean mLock;

    /**
     * 检查是否启用触摸探索(无障碍模式)。
     * 使用单例缓存 AccessibilityManager,避免重复获取系统服务。
     *
     * @return true 表示启用了触摸探索。
     */
    public static boolean isTouchExplorationEnabled() {
        if (mAm == null) {
            Context context = LuaApplication.getInstance();
            if (context != null) {
                // 缓存单例对象
                mAm = (AccessibilityManager) context.getApplicationContext()
                        .getSystemService(Context.ACCESSIBILITY_SERVICE);
            }
        }
        // 使用变量判空，防止 context 获取失败导致的 NPE
        return mAm != null && mAm.isTouchExplorationEnabled();
    }

    /**
     * 构造函数。
     *
     * @param context 上下文。
     * @param globals Lua 全局环境,包含键盘配置(ascii_mode、lock)。
     */
    public KeyboardView(@NonNull Context context, Globals globals) {
        super(context);
        mKeys = new ArrayList<>();
        mComposingKeys = new ArrayList<>();
        setClipChildren(false);    // 允许子控件阴影超出边界
        setClipToPadding(false);
        setAsciiModeLock(globals.get("ascii_mode").toboolean());
        setLock(globals.get("lock").toboolean());
    }
    /**
     * 测量视图大小。
     *
     * @param widthMeasureSpec 宽度测量规范。
     * @param heightMeasureSpec 高度测量规范。
     */
    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        try {
            super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        }catch (Exception e){
            e.printStackTrace();
        }
    }

    /**
     * 检查是否为 ASCII 模式。
     *
     * @return true 表示处于 ASCII 模式或 ASCII 锁定状态。
     */
    public boolean isAsciiMode() {
        return mAsciiMode || mAsciiModeLock;
    }

    /**
     * 设置 ASCII 模式。
     *
     * @param b true 表示启用 ASCII 模式。
     */
    public void setAsciiMode(boolean b) {
        mAsciiMode = b;
    }

    /**
     * 设置 ASCII 模式锁定。
     *
     * @param b true 表示锁定 ASCII 模式。
     */
    public void setAsciiModeLock(boolean b) {
        mAsciiModeLock = b;
    }

    /**
     * 递归处理新添加的视图
     */
    private void processViewAdded(View view) {
        if (view instanceof KeyView) {
            KeyView kv = (KeyView) view;
            if (!mKeys.contains(kv)) {
                mKeys.add(kv);
                // 如果是特殊的 KeyView 类型，可以分类存储
                if (kv.isComposingKey()) mComposingKeys.add(kv);
            }
        } else if (view instanceof ViewGroup) {
            ViewGroup group = (ViewGroup) view;
            // 扫描该容器内已经存在的子 View（针对 Lua 一次性添加一个大布局的情况）
            for (int i = 0; i < group.getChildCount(); i++) {
                processViewAdded(group.getChildAt(i));
            }
        }
    }

    /**
     * 递归处理移除的视图
     */
    private void processViewRemoved(View view) {
        if (view instanceof KeyView) {
            mKeys.remove(view);
            mComposingKeys.remove(view);
            if (view == lastKey) lastKey = null; // 清理触摸追踪状态
        } else if (view instanceof ViewGroup) {
            ViewGroup group = (ViewGroup) view;
            for (int i = 0; i < group.getChildCount(); i++) {
                processViewRemoved(group.getChildAt(i));
            }
        }
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        // 2. 核心：如果列表为空，说明是重新显示，立即全量扫描当前已有的 View 树
        if (mKeys.isEmpty()) {
            processViewAdded(this);
            if (mShifted != ModifierState.isShifted()) {
                mShifted = ModifierState.isShifted();
                invalidateAllKeys();
            }
        }
    }

    @Override
    protected void onDetachedFromWindow() {
        // 1. 显式清空列表，切断强引用链
        mKeys.clear();
        mComposingKeys.clear();
        lastKey = null;
        super.onDetachedFromWindow();
    }

    /**
     * 检查标签是否为大写。
     *
     * @return 始终返回 false(暂未使用)。
     */
    public boolean isLabelUppercase() {
        return false;
    }


    /**
     * 获取编码区按键列表。
     *
     * @return 编码区按键列表。
     */
    public ArrayList<KeyView> getComposingKeys() {
        return mComposingKeys;
    }

    // 假设这是你的 KeyboardView 容器
    /** 记录上一个按下的按键 */
    private View lastKey = null;

    /**
     * 分发触摸事件。
     * 如果启用了 keySwipe,则支持滑动选择按键。
     * 当 TalkBack 开启时,走系统默认流程以保证无障碍访问。
     *
     * @param ev 触摸事件。
     * @return true 表示事件已处理。
     */
    @Override
    public boolean dispatchTouchEvent(MotionEvent ev) {
        if (!keySwipe)
            return super.dispatchTouchEvent(ev);
        // 1. 如果 TalkBack 开启，建议走系统默认流程，否则读屏无法线性遍历按键
        if (isTouchExplorationEnabled()) {
            return super.dispatchTouchEvent(ev); // 交给系统默认分发逻辑
        }
        int action = ev.getActionMasked();
        float x = ev.getX();
        float y = ev.getY();

        switch (action) {
            case MotionEvent.ACTION_DOWN:
            case MotionEvent.ACTION_MOVE:
                // 1. 寻找当前坐标对应的 View
                View currentKey = findKeyAt(x, y);
                //Log.w("KeyDebug", "findKeyAt: " + currentKey);

                if (currentKey != lastKey) {
                    // 2. 状态切换：旧按键弹起，新按键按下
                    if (lastKey != null) {
                        lastKey.setPressed(false);
                        lastKey.refreshDrawableState(); // 关键：通知系统状态已变，去匹配 SLA
                    }
                    if (currentKey != null) {
                        // 设置热点（Ripple 会从手指进入的位置开始扩散）
                        currentKey.drawableHotspotChanged(x - currentKey.getLeft(), y - currentKey.getTop());
                        currentKey.setPressed(true);
                        currentKey.refreshDrawableState(); // 关键：通知系统状态已变，去匹配 SLA
                    }
                    lastKey = currentKey;
                }
                break;

            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_CANCEL:
                // 3. 抬起逻辑
                if (lastKey != null) {
                    if (action == MotionEvent.ACTION_UP) {
                        // 手动触发点击回调
                        lastKey.performClick();
                    }
                    lastKey.setPressed(false);
                    lastKey = null;
                }
                break;
        }

        // 关键：必须返回 true，声明该布局消费所有触摸
        // 这样事件就不会传递给子 View 的 onTouchEvent
        return true;
    }

    /**
     * 查找指定坐标处的按键。
     *
     * @param x X 坐标。
     * @param y Y 坐标。
     * @return 找到的 KeyView,未找到则返回 null。
     */
    private View findKeyAt(float x, float y) {
        for (KeyView key : mKeys) {
            if (key.contains((int) x, (int) y))
                return key;
        }
        return null;
    }

    /**
     * 添加 KeyView 子视图。
     *
     * @param child 要添加的 KeyView。
     * @param params 布局参数。
     */
    public void addView(KeyView child, ViewGroup.LayoutParams params) {
        super.addView(child, params);
        if (child.isComposingKey()) {
            mComposingKeys.add(child);
        }
        mKeys.add(child);
    }

    /**
     * 添加 ViewGroup 子视图。
     * 递归收集布局树中的所有 KeyView。
     *
     * @param child 要添加的 ViewGroup。
     * @param params 布局参数。
     */
    public void addView(ViewGroup child, ViewGroup.LayoutParams params) {
        super.addView(child, params);
        // 统一调用收集逻辑，不仅限于 Flexbox
        collectKeyViews(child);
    }

    /**
     * 递归收集布局树中所有的 KeyView 并分类存储
     *
     * @param root 当前需要扫描的根布局
     */
    private void collectKeyViews(ViewGroup root) {
        for (int i = 0; i < root.getChildCount(); i++) {
            View child = root.getChildAt(i);

            if (child instanceof KeyView) {
                KeyView key = (KeyView) child;

                // 1. 添加到全局总表（使用 contains 判断防止重复注册）
                if (!mKeys.contains(key)) {
                    mKeys.add(key);

                    // 2. 根据属性进行分类登记
                    if (key.isComposingKey()) {
                        mComposingKeys.add(key);
                    }
                }
            } else if (child instanceof ViewGroup) {
                // 3. 如果是容器（如 FlexboxLayout, LinearLayout 等），递归向下扫描
                collectKeyViews((ViewGroup) child);
            }
        }
    }

    /**
     * 刷新所有编码区按键的显示。
     */
    public void invalidateComposingKeys() {
        for (KeyView key : mComposingKeys) {
            key.invalidateKey();
        }
    }

    /**
     * 刷新所有按键的显示。
     */
    public void invalidateAllKeys() {
        for (KeyView key : mKeys) {
            key.invalidateKey();
        }
    }

    /**
     * 检查 Shift 键状态。
     *
     * @return true 表示 Shift 键已按下。
     */
    public boolean isShifted() {
        return mShifted;
    }

    /**
     * 设置 Shift 键状态。
     * 设置后刷新所有按键的显示。
     *
     * @param shifted true 表示按下,false 表示释放。
     */
    public void setShifted(boolean shifted) {
        mShifted = shifted;
        invalidateAllKeys();
    }

    /**
     * 检查键盘是否锁定。
     *
     * @return true 表示键盘已锁定。
     */
    public boolean isLock() {
        return mLock;
    }

    /**
     * 设置键盘锁定状态。
     *
     * @param b true 表示锁定,false 表示解锁。
     */
    public void setLock(boolean b) {
        mLock = b;
    }

    /**
     * 设置是否启用按键滑动选择。
     *
     * @param b true 表示启用,false 表示禁用。
     */
    public void setKeySwipe(boolean b){
        keySwipe = b;
    }
}
