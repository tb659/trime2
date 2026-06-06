/*
 * SPDX-FileCopyrightText: 2015 - 2025 Rime community
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package com.osfans.trime.keyboard;

import android.animation.Animator;
import android.content.Context;
import android.content.res.ColorStateList;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffColorFilter;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.RippleDrawable;
import android.graphics.drawable.TransitionDrawable;
import android.os.Build;
import android.os.Bundle;
import android.os.VibrationEffect;
import android.text.TextUtils;
import android.util.Log;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.HapticFeedbackConstants;
import android.view.MotionEvent;
import android.view.SoundEffectConstants;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewOutlineProvider;
import android.view.ViewParent;
import android.view.accessibility.AccessibilityNodeInfo;
import android.view.animation.Interpolator;
import android.widget.FrameLayout;
import android.widget.TextView;

import androidx.annotation.Keep;
import androidx.annotation.NonNull;
import androidx.interpolator.view.animation.FastOutSlowInInterpolator;

import com.androlua.LuaBitmapDrawable;
import com.osfans.trime.Config;
import com.osfans.trime.Event;
import com.osfans.trime.Key;
import com.osfans.trime.TrimeService;
import com.osfans.trime.core.Rime;
import com.osfans.trime.enums.KeyEventType;
import com.osfans.trime.theme.KeyStyle;
import com.osfans.trime.theme.Style;
import com.osfans.trime.theme.ThemeManager;

import org.luaj.LuaTable;
import org.luaj.LuaValue;

import java.util.List;

/**
 * 按键视图类。
 * 表示键盘上的单个按键,支持单击、长按、滑动等多种事件类型。
 * 包含主文本、提示文本、长按文本等显示元素,以及按下动画、预览窗口等视觉效果。
 */
public class KeyView extends FrameLayout implements View.OnClickListener {
    private static final String TAG = "KeyView";

    // --- 1. 静态常量 ---
    /**
     * 快速进出缓动插值器
     */
    private static final Interpolator FAST_OUT_SLOW_IN = new FastOutSlowInInterpolator();

    // --- 2. 成员变量 ---
    /**
     * TrimeService 实例
     */
    private final TrimeService mTrime;
    /**
     * 按键样式
     */
    private final KeyStyle mKeyStyle;
    /**
     * 按下状态样式
     */
    private final KeyStyle mPressedStyle;
    /**
     * ASCII 模式专用的按键对象
     */
    private final Key mAsciiKey;
    /**
     * 默认按键对象
     */
    private final Key mDefKey;
    /**
     * 当前按键对象(可能在 ASCII 模式和默认模式之间切换)
     */
    private Key mKey;
    /**
     * 主文本 TextView
     */
    private TextView mClick;
    /**
     * 提示文本 TextView
     */
    private TextView mHint;
    /**
     * 长按文本 TextView
     */
    private TextView mLongClick;
    /**
     * 按键根布局容器
     */
    private FrameLayout keyRoot;
    /**
     * 点击文本内容
     */
    private String mClickText;
    /**
     * 背景过渡动画(正常状态 -> 按下状态)
     */
    private TransitionDrawable transition;
    /**
     * 是否处于按下状态
     */
    private boolean mPressed;
    /**
     * 命中矩形是否失效(需要重新计算)
     */
    private boolean mRectInvalidated;
    /**
     * 是否处于选中状态
     */
    private boolean mSelected;
    /**
     * 预览窗口 TextView(长按或滑动时显示)
     */
    private TextView keyPreview;
    /**
     * 动画监听器(暂未使用)
     */
    private Animator.AnimatorListener mAnimatorListener;
    // private TextView mHintUp;
    // private TextView mHintDown;
    // private TextView mHintLeft;
    // private TextView mHintRight;
    /**
     * 8个方向的提示文本数组(上、下、左、右、长按等)
     */
    private TextView[] mHints = new TextView[8];
    /**
     * 是否隐藏按键提示
     */
    private boolean _hide_key_hint;

    // --- 3. 构造函数 ---

    /**
     * 构造函数(无按键配置)。
     *
     * @param context 上下文。
     */
    public KeyView(@NonNull Context context) {
        // 调用父类 FrameLayout 的构造函数，初始化视图层级
        super(context);
        // 由于未传入 Key 配置，将默认按键和 ASCII 按键设为 null
        mDefKey = null;
        mAsciiKey = null;
        // 获取 TrimeService 的单例实例，用于处理输入法事件
        mTrime = TrimeService.getInstance();
        // 从主题管理器中获取默认的 "key" 样式作为当前按键样式
        mKeyStyle = ThemeManager.getStyle().getKeyStyle("key");
        // 基于当前按键样式，获取其对应的“按下”状态样式，若未定义则回退到原样式
        mPressedStyle = mKeyStyle.getKeyStyle("pressed", mKeyStyle);
        // 初始化视图结构，包括创建根布局、主文本 TextView、背景及预览窗口等 UI 组件
        initView();
    }

    /**
     * 构造函数(带按键样式)。
     *
     * @param context 上下文。
     * @param v       按键样式对象。
     */
    public KeyView(@NonNull Context context, KeyStyle v) {
        // 调用父类 FrameLayout 的构造函数，初始化视图层级
        super(context);
        // 由于未传入 Key 配置，将默认按键和 ASCII 按键设为 null
        mDefKey = null;
        mAsciiKey = null;
        // 获取 TrimeService 的单例实例，用于处理输入法事件
        mTrime = TrimeService.getInstance();
        // 直接使用传入的 KeyStyle 对象作为当前按键样式
        mKeyStyle = v;
        // 基于当前按键样式，获取其对应的“按下”状态样式，若未定义则回退到原样式
        mPressedStyle = mKeyStyle.getKeyStyle("pressed", mKeyStyle);
        // 初始化视图结构，包括创建根布局、主文本 TextView、背景及预览窗口等 UI 组件
        initView();
    }

    /**
     * 构造函数(带按键配置)。
     *
     * @param context 上下文。
     * @param v       按键配置对象。
     */
    public KeyView(@NonNull Context context, Key v) {
        // 调用父类 FrameLayout 的构造函数，初始化视图层级
        super(context);
        // 获取 TrimeService 的单例实例，用于处理输入法事件
        mTrime = TrimeService.getInstance();
        // 将传入的按键配置对象 v 赋值给当前按键成员变量
        mKey = v;
        // 将传入的按键配置对象 v 同时设为默认按键配置（非 ASCII 模式下的配置）
        mDefKey = v;
        // 从当前按键配置中获取 ASCII 模式下的专用按键配置
        mAsciiKey = mKey.getAsciiKey();
        // 根据按键配置中的样式名称和主题默认的 key 样式，合并生成最终的按键样式
        // 优先级: key 级字段 → row 级默认值 → keyboard 级默认值 → 命名样式 → 主题 key 样式
        KeyStyle themeKeyStyle = ThemeManager.getStyle().getKeyStyle("key");
        KeyStyle namedStyle = ThemeManager.getStyle().getKeyStyle(v.getStyle(), themeKeyStyle);
        LuaValue mk = v.getMk();
        if (mk != null && mk.istable()) {
            LuaValue styleTable = mk.checktable().get("__style");
            if (styleTable.istable()) {
                namedStyle = new KeyStyle(styleTable.checktable(), namedStyle);
            }
        }
        mKeyStyle = namedStyle;
        // 基于生成的按键样式，获取其对应的"按下"状态样式，若未定义则回退到原样式
        mPressedStyle = mKeyStyle.getKeyStyle("pressed", mKeyStyle);
        // 初始化视图结构，包括创建根布局、主文本 TextView、背景及预览窗口等 UI 组件
        initView();
        // 初始化按键的具体内容，如设置主文本、长按文本、提示文本及滑动方向的事件绑定
        initKey();
    }

    /**
     * 构造函数(带按键配置和样式)。
     *
     * @param context 上下文。
     * @param v       按键配置对象。
     * @param s       按键样式对象。
     */
    public KeyView(@NonNull Context context, Key v, KeyStyle s) {
        // 调用父类 FrameLayout 的构造函数，初始化视图层级
        super(context);
        // 获取 TrimeService 的单例实例，用于处理输入法事件
        mTrime = TrimeService.getInstance();
        // 将传入的按键配置对象 v 赋值给当前按键成员变量
        mKey = v;
        // 将传入的按键配置对象 v 同时设为默认按键配置（非 ASCII 模式下的配置）
        mDefKey = v;
        // 从当前按键配置中获取 ASCII 模式下的专用按键配置
        mAsciiKey = mKey.getAsciiKey();
        // 根据按键配置中的样式名称和传入的基础样式 s，合并生成最终的按键样式
        // 优先使用 key 表上的 __style 字段，作为 style 解析的基准
        KeyStyle effectiveBase = s;
        LuaValue mk2 = v.getMk();
        if (mk2 != null && mk2.istable()) {
            LuaValue styleTable = mk2.checktable().get("__style");
            if (styleTable.istable()) {
                effectiveBase = new KeyStyle(styleTable.checktable(), s);
            }
        }
        mKeyStyle = ThemeManager.getStyle().getKeyStyle(v.getStyle(), effectiveBase);
        // 基于生成的按键样式，获取其对应的"按下"状态样式，若未定义则回退到原样式
        mPressedStyle = mKeyStyle.getKeyStyle("pressed", mKeyStyle);
        // 初始化视图结构，包括创建根布局、主文本 TextView、背景及预览窗口等 UI 组件
        initView();
        // 初始化按键的具体内容，如设置主文本、长按文本、提示文本及滑动方向的事件绑定
        initKey();
    }

    // --- 4. 生命周期与重写方法 (Overrides) ---

    /**
     * 点击事件处理。
     * 处理 Shift 键的单击逻辑(切换大小写、锁定等),或其他按键的事件分发。
     *
     * @param v 被点击的视图。
     */
    /**
     * 点击事件处理。
     * 处理 Shift 键的单击逻辑(切换大小写、锁定等),或其他按键的事件分发。
     *
     * @param v 被点击的视图。
     */
    @Override
    public void onClick(View v) {
        // 如果之前触发了长按事件，则重置标志并直接返回，避免重复执行单击逻辑
        if (mLongClicked) {
            mLongClicked = false;
            return;
        }

        // 如果当前按键配置对象不为空
        if (mKey != null) {
            // 判断当前按键是否为 Shift 键
            if (mKey.isShift()) {
                // 如果当前处于 Shift 锁定状态（大写锁定）
                if (ModifierState.isShiftLock()) {
                    // 取消 Shift 锁定
                    ModifierState.setShiftLock(false);
                    // 设置输入法状态为非 Shift 状态
                    mTrime.setShifted(false);
                    // 注释掉的振动反馈代码
                    // if(mKeyStyle.isVibrationEnabled()) {
                    //    performHapticFeedback(
                    //            HapticFeedbackConstants.KEYBOARD_TAP,
                    //            HapticFeedbackConstants.FLAG_IGNORE_GLOBAL_SETTING
                    //    );
                    //}
                } else {
                    // 如果 Shift 键的点击行为配置为“双击”模式
                    if (mKey.getClick().getShiftLock().equals("double")) {
                        // 如果当前已经处于 Shift 状态（临时大写），则切换为锁定状态
                        if (ModifierState.isShifted()) {
                            ModifierState.setShiftLock(true);
                        } else {
                            // 否则仅设置为临时 Shift 状态
                            mTrime.setShifted(true);
                        }
                    } else {
                        // 普通 Shift 键逻辑：切换 Shift 状态
                        boolean shifted = !ModifierState.isShifted();
                        // 如果该 Shift 键支持锁定功能，则更新锁定状态
                        if (mKey.isShiftLock())
                            ModifierState.setShiftLock(shifted);
                        // 更新输入法的 Shift 状态
                        mTrime.setShifted(shifted);
                    }
                }
                // 根据当前的 Shift 状态更新按键的选中视觉效果
                setSelected(ModifierState.isShifted());
                // 根据是否处于 Shift 锁定状态更新主文本颜色
                updateTextColor(mClick, ModifierState.isShiftLock(), mKeyStyle);
            } else {
                // 非 Shift 键：触发按键对应的事件
                mTrime.onEvent(mKey.getEvent());
                // 如果当前处于临时 Shift 状态且未锁定，则在点击后取消 Shift 状态
                if (ModifierState.isShifted() && !ModifierState.isShiftLock())
                    mTrime.setShifted(false);
            }
            return;
        }
        // 如果没有 Key 配置，检查点击文本是否为空，为空则直接返回
        if (TextUtils.isEmpty(mClickText)) return;
        // 触发基于点击文本的事件
        mTrime.onEvent(new Event(mClickText));
    }

    //@Override
    // public void setBackgroundColor(int color) {
    //    //super.setBackgroundColor(color);
    //    keyRoot.setBackground(createButtonBackground(color, 24f));
    //}

    /**
     * 设置背景颜色(带圆角)。
     *
     * @param color  背景颜色。
     * @param radius 圆角半径。
     */
    public void setBackgroundColor(int color, float radius) {
        // super.setBackgroundColor(color);
        keyRoot.setBackground(createButtonBackground(color, radius));
    }
    // --- 4. 生命周期管理 (重点优化部分) ---

    /**
     * 设置按下状态。
     * 处理长按检测、重复按键、预览显示和状态动画。
     *
     * @param pressed true 表示按下,false 表示释放。
     */
    @Override
    public void setPressed(boolean pressed) {
        // 计算按下状态是否发生变化
        boolean changed = mPressed != pressed;
        // 更新成员变量 mPressed 为新的按下状态
        mPressed = pressed;
        // 调用父类方法更新视图的按下状态
        super.setPressed(pressed);

        // 如果状态发生了变化
        if (changed) {
            // 如果当前没有发生滑动操作（方向为无滑动）
            if (direction == SWIPE_NONE)
                // 根据按下状态显示或隐藏预览窗口，并传入主文本内容
                showPreview(pressed, mClick.getText());
        }

        // 如果按键配置对象不为空且状态发生了变化
        if (mKey != null && changed) {
            // 如果是按下状态
            if (pressed) {
                // 设置点击监听器
                setOnClickListener(this);
                // 如果定义了长按事件、事件可重复或是 Shift 键
                if (mKey.getLongClick() != null || mKey.getEvent().isRepeatable() || (mKey.isShift())) {
                    // 延迟执行长按检测任务
                    postDelayed(mLongClickRunnable, mKeyStyle.getLongClickTime());
                }
            } else {
                // 如果是释放状态，移除长按检测任务的回调
                removeCallbacks(mLongClickRunnable);
                // 移除重复按键任务的回调
                removeCallbacks(mRepeatableRunnable);
            }
        }
        // 如果按键处于选中状态且当前是释放操作
        if (mSelected && !pressed) {
            // 强制将 mPressed 设回 true，保持视觉上的按下/选中效果
            mPressed = true;
            // 直接返回，不执行后续的动画重置逻辑
            return;
        }
        // 如果状态发生了变化，触发重构后的轻量级状态动画
        if (changed) {
            applyStateAnimation(pressed);
        }
    }

    /**
     * 设置选中状态。
     * 用于 Shift 键等需要保持选中状态的按键。
     *
     * @param selected true 表示选中,false 表示取消选中。
     */
    @Override
    public void setSelected(boolean selected) {
        mSelected = selected;
        mPressed = selected;
        super.setSelected(selected);
        applyStateAnimation(selected);
        if (!selected && transition != null)
            transition.resetTransition();
    }

    /**
     * 应用状态动画。
     * 根据按下/释放状态,执行缩放、位移、背景过渡和阴影颜色变化动画。
     *
     * @param isPressed true 表示按下状态,false 表示释放状态。
     */
    private void applyStateAnimation(boolean isPressed) {

        // 1. 执行缩放、位移动画 (Scale & Translation Animation)
        // 根据是否按下状态，从按下样式中获取缩放比例，否则恢复默认值 1.0f
        float scaleX = isPressed ? mPressedStyle.getScaleX() : 1.0f;
        float scaleY = isPressed ? mPressedStyle.getScaleY() : 1.0f;
        // 根据是否按下状态，从按下样式中获取 Z 轴位移，否则归零
        float translationZ = isPressed ? mPressedStyle.getTranslationZ() : 0;
        // 根据是否按下状态，从按下样式中获取 Y 轴位移，否则归零
        float translationY = isPressed ? mPressedStyle.getTranslationY() : 0;
        // 根据是否按下状态，从按下样式中获取 X 轴位移，否则归零
        float translationX = isPressed ? mPressedStyle.getTranslationX() : 0;

        // 启动属性动画，应用上述计算的缩放和位移效果
        keyRoot.animate()
                .scaleX(scaleX)       // 设置 X 轴缩放比例
                .scaleY(scaleY)       // 设置 Y 轴缩放比例
                .translationZ(translationZ) // 设置 Z 轴位移（影响阴影）
                .translationY(translationY) // 设置 Y 轴位移
                .translationX(translationX) // 设置 X 轴位移
                .setDuration(50)      // 动画持续时间 50 毫秒
                .setInterpolator(FAST_OUT_SLOW_IN) // 使用快速进出缓动插值器
                .start();             // 开始动画

        // 2. 背景过渡动画 (Background Transition)
        if (transition != null) {
            // 如果存在过渡 Drawable，根据按下状态启动正向或反向过渡
            if (isPressed) {
                transition.startTransition(50); // 开始过渡到按下状态背景，耗时 50ms
            } else {
                transition.reverseTransition(50); // 反向过渡回正常状态背景，耗时 50ms
            }
        } else {
            // 如果没有 TransitionDrawable，直接修改 GradientDrawable 颜色
            Drawable bg = keyRoot.getBackground(); // 获取当前背景 Drawable
            if (bg instanceof GradientDrawable) { // 检查是否为渐变形状 Drawable
                // 根据按下状态选择颜色：按下时使用按下样式背景色（默认灰色），否则使用正常样式背景色
                int dColor = isPressed ? mPressedStyle.getBackgroundColor(0xffaaaaaa)
                        : mKeyStyle.getBackgroundColor();
                ((GradientDrawable) bg).setColor(dColor); // 直接设置渐变背景的颜色
            }
        }

        // 3. 更新主文本颜色 (Update Main Text Color)
        // 调用辅助方法，根据按下状态和按键样式更新主点击文本的颜色
        updateTextColor(mClick, isPressed, mKeyStyle);

        // 4. 更新所有方向提示文本颜色 (Update Hint Text Colors)
        // 遍历所有可能的提示文本视图数组（包括上下左右滑动提示、长按提示等）
        for (int i = 0; i < mHints.length; i++) {
            TextView hint = mHints[i]; // 获取第 i 个提示文本视图
            if (hint == null)
                continue; // 如果该位置没有视图，跳过
            // 使用对应位置的样式数组更新提示文本颜色
            updateTextColor(hint, isPressed, mHintStyles[i]);
        }

        /*KeyStyle mHintKeyStyle = mKeyStyle.getHintKeyStyle();
        if (mHint != null)
            updateTextColor(mHint, isPressed, mHintKeyStyle);
        if (mLongClick != null)
            updateTextColor(mLongClick, isPressed, mKeyStyle.getLongClickKeyStyle());
        if (mHintUp != null)
            updateTextColor(mHintUp, isPressed, mHintKeyStyle.getKeyStyle("up"));
        if (mHintDown != null)
            updateTextColor(mHintDown, isPressed, mHintKeyStyle.getKeyStyle("down"));
        if (mHintLeft != null)
            updateTextColor(mHintLeft, isPressed, mHintKeyStyle.getKeyStyle("left"));
        if (mHintRight != null)
            updateTextColor(mHintRight, isPressed, mHintKeyStyle.getKeyStyle("right"));*/

        // 5. 更新阴影颜色 (Update Shadow Color, API 28+)
        // 仅在 Android P (API 28) 及以上版本支持自定义阴影颜色
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            int dShadowColor = mKeyStyle.getShadowColor(); // 获取正常状态下的阴影颜色
            if (dShadowColor != 0) { // 如果定义了阴影颜色
                if (isPressed) {
                    // 如果是按下状态
                    int pShadowColor = mPressedStyle.getShadowColor(); // 获取按下状态的阴影颜色
                    if (pShadowColor != 0) { // 如果按下状态也定义了阴影颜色
                        // 设置环境光和点光源阴影颜色为按下状态的颜色
                        keyRoot.setOutlineAmbientShadowColor(pShadowColor);
                        keyRoot.setOutlineSpotShadowColor(pShadowColor);
                    }
                } else {
                    // 如果是释放状态，恢复为正常状态的阴影颜色
                    keyRoot.setOutlineAmbientShadowColor(dShadowColor);
                    keyRoot.setOutlineSpotShadowColor(dShadowColor);
                }
            }
        }
    }

    /**
     * 显示/隐藏预览窗口。
     * 在长按或滑动时显示预览,手指抬起时隐藏。
     *
     * @param isPressed true 表示显示预览,false 表示隐藏。
     * @param text      预览文本内容。
     */
    private void showPreview(boolean isPressed, CharSequence text) {
        // 如果按键对象为空，直接返回
        if (mKey == null)
            return;
        // 如果预览视图未初始化，直接返回
        if (keyPreview == null)
            return;

        // 防御性检查（同 initView 逻辑）：使用 rawget 绕过 __index 链
        // 检查 mKey 表上的 "preview" 字段
        if (mKey != null) {
            LuaValue mk = mKey.getMk();
            if (mk != null && mk.istable()) {
                LuaValue pv = mk.checktable().get("preview");
                if (pv.isboolean() && !pv.toboolean()) {
                    if (isPressed) keyPreview.setVisibility(GONE);
                    return;
                }
                // 检查命名样式：mColor[styleName] 的 rawget("preview")
                LuaValue styleName = mk.checktable().get("style");
                if (styleName.isstring()) {
                    LuaValue namedStyle = ThemeManager.getStyle().get(styleName.tojstring());
                    if (namedStyle.istable()) {
                        LuaValue nsPv = namedStyle.checktable().rawget(LuaValue.valueOf("preview"));
                        if (nsPv.isboolean() && !nsPv.toboolean()) {
                            if (isPressed) keyPreview.setVisibility(GONE);
                            return;
                        }
                    }
                }
            }
        }
        // 检查 mKeyStyle 样式链
        LuaValue stylePv = mKeyStyle.get("preview");
        if (stylePv.isboolean() && !stylePv.toboolean()) {
            if (isPressed) keyPreview.setVisibility(GONE);
            return;
        }

        // 处理非按下状态：取消动画并隐藏预览
        if (!isPressed) {
            keyPreview.animate().cancel();
            keyPreview.setVisibility(GONE);
            return;
        }

        // 设置预览文本，如果文本为空则隐藏预览
        if (text != null) {
            keyPreview.setText(text);
            keyPreview.setMinimumWidth(getWidth());
            int widthSpec = MeasureSpec.makeMeasureSpec(0, MeasureSpec.UNSPECIFIED);
            int heightSpec = MeasureSpec.makeMeasureSpec(getMeasuredHeight(), MeasureSpec.EXACTLY);
            keyPreview.measure(widthSpec, heightSpec);
        } else {
            keyPreview.setVisibility(GONE);
            return;
        }

        /*if (mAnimatorListener == null) {
            mAnimatorListener = new Animator.AnimatorListener() {
                @Override
                public void onAnimationStart(@NonNull Animator animation) {
                    if (isPressed)
                        keyPreview.setVisibility(VISIBLE);
                }

                @Override
                public void onAnimationEnd(@NonNull Animator animation) {
                    if (!isPressed)
                        keyPreview.setVisibility(GONE);
                }

                @Override
                public void onAnimationCancel(@NonNull Animator animation) {
                    keyPreview.setVisibility(GONE);
                }

                @Override
                public void onAnimationRepeat(@NonNull Animator animation) {

                }
            };
        }*/

        // 获取预览样式配置
        KeyStyle previewStyle = mKeyStyle.getKeyStyle("preview", mKeyStyle);
        // 预览偏移
        float offsetX = previewStyle.getSize("offset_x", 0);
        float offsetY = previewStyle.getSize("offset_y", 0);

        // 边界检查，确保预览不超出屏幕
        // 获取按键在屏幕上的绝对坐标
        int[] location = new int[2];
        getLocationOnScreen(location);
        int keyLeft = location[0];
        int keyTop = location[1];
        int keyWidth = getWidth();
        int keyHeight = getHeight();
        // 预览实际内容宽度（经过 UNSPECIFIED 测量，不受按键宽度限制）
        int previewWidth = keyPreview.getMeasuredWidth();
        // 预览上浮的基础高度（按键高度 × 缩放 × 1.01）
        float baseTransY = -keyHeight * previewStyle.getScaleY() * 1.01f;
        // 获取 preview 在 KeyView 中的布局参数（含 margins）
        LayoutParams lp = (LayoutParams) keyPreview.getLayoutParams();
        // 计算 preview 在 KeyView 中的水平位置（CENTER_HORIZONTAL 对齐）
        int previewLayoutLeft = lp.leftMargin + (keyWidth - previewWidth - lp.leftMargin - lp.rightMargin) / 2;
        // 计算 preview 在 KeyView 中的垂直位置（默认 TOP 对齐）
        int previewLayoutTop = lp.topMargin;
        TrimeService trime = TrimeService.getInstance();
        int screenWidth = trime.getWidth();
        // 从预览样式中读取到屏幕边缘的最小距离
        int marginLeft = previewStyle.getSize("boundary_margin_left", 0);
        int marginRight = previewStyle.getSize("boundary_margin_right", 0);
        int marginTop = previewStyle.getSize("boundary_margin_top", 0);
        // 预测预览最终的屏幕坐标
        int predictedLeft = keyLeft + previewLayoutLeft + (int) offsetX;
        int predictedRight = predictedLeft + previewWidth;
        int predictedTop = keyTop + previewLayoutTop + (int) (baseTransY + offsetY);
        // 超出左边界 → 右移
        if (predictedLeft < marginLeft) offsetX += marginLeft - predictedLeft;
        // 超出右边界 → 左移
        else if (predictedRight > screenWidth - marginRight) offsetX += screenWidth - marginRight - predictedRight;
        // 超出上边界 → 下移
        if (predictedTop < marginTop) offsetY += marginTop - predictedTop;

        // 初始化预览视图的起始状态（缩小、透明、低位）
        if (isPressed) {
            keyPreview.setAlpha(0);
            keyPreview.setScaleX(0.5f);
            keyPreview.setScaleY(0.5f);
            keyPreview.setTranslationY(offsetY);
            keyPreview.setTranslationZ(1);
            keyPreview.setVisibility(VISIBLE);
        }

        // 执行显示动画：放大、上浮、不透明
        keyPreview.animate()
                .scaleX(previewStyle.getScaleX())
                .scaleY(previewStyle.getScaleY())
                .translationZ(1)
                .translationY(-getHeight() * previewStyle.getScaleY() * 1.01f + offsetY)
                .translationX(offsetX)
                .setDuration(100)
                .alpha(1.f)
                .setInterpolator(FAST_OUT_SLOW_IN)
                //.setListener(mAnimatorListener)
                .start();
    }

    /**
     * 更新文本颜色。
     * 根据按下状态切换正常颜色和按下颜色。
     *
     * @param view      TextView 视图。
     * @param isPressed 是否处于按下状态。
     * @param style     按键样式。
     */
    private void updateTextColor(TextView view, boolean isPressed, KeyStyle style) {
        if (view == null)
            return;
        view.setTextColor(isPressed ? style.getPressedStyle().getTextColor() : style.getTextColor());
    }

    /**
     * 当视图附加到窗口时调用。
     * 重置动画状态,确保视图重新显示时状态正确。
     */
    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        // 确保 View 重新进入窗口时状态正确
        if (keyRoot != null) {
            keyRoot.setScaleX(1.0f);
            keyRoot.setScaleY(1.0f);
            keyRoot.setTranslationZ(0);
            keyRoot.setTranslationY(0);
            keyRoot.setTranslationX(0);
        }
        if (mSelected) {
            mPressed = false;
            setPressed(true);
        }
        if (keyPreview != null) {
            keyPreview.animate().cancel();
            keyPreview.setVisibility(GONE);
        }
    }


    /**
     * 当视图从窗口分离时调用。
     * 清理所有动画、回调和资源,防止内存泄漏。
     */
    @Override
    protected void onDetachedFromWindow() {
        // 1. 停止并取消所有正在运行的属性动画，释放引用
        if (keyRoot != null) {
            keyRoot.animate().cancel();
        }
        if (keyPreview != null) {
            keyPreview.animate().cancel();
            keyPreview.setVisibility(GONE);
        }
        // 2. 彻底移除所有 Callback，防止销毁后执行 Runnable 导致的内存泄漏
        removeCallbacks(mLongClickRunnable);
        removeCallbacks(mRepeatableRunnable);
        removeCallbacks(mSwipRepeatableRunnable);

        // 3. 重置 Transition 状态，释放 Drawable 资源
        if (transition != null) {
            transition.resetTransition();
        }

        // 4. 清除监听器，断开与 Service 的强关联
        if (mKey != null)
            setOnClickListener(null);
        if (mPressed)
            setPressed(false);
        // 视图销毁时释放内存
        if (maskBitmap != null) {
            maskBitmap.recycle();
            maskBitmap = null;
        }
        super.onDetachedFromWindow();
    }

    /**
     * 执行无障碍操作。
     * 支持 ACTION_CLICK 操作,用于 TalkBack 等无障碍服务。
     *
     * @param action    操作类型。
     * @param arguments 操作参数。
     * @return true 表示操作已处理。
     */
    @Override
    public boolean performAccessibilityAction(int action, Bundle arguments) {
        switch (action) {
            case AccessibilityNodeInfo.ACTION_CLICK:
                performClick();
                return true;
        }
        return super.performAccessibilityAction(action, arguments);
    }

    // 预先分配内存，整个生命周期只创建这一次
    private final Rect mHitRect = new Rect();

    /**
     * 当测量大小时调用。
     * 测量预览视图的宽度。
     *
     * @param widthMeasureSpec  宽度测量规格。
     * @param heightMeasureSpec 高度测量规格。
     */
    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        if (keyPreview != null) {
            keyPreview.setMinimumWidth(getMeasuredWidth());
            int widthSpec = MeasureSpec.makeMeasureSpec(0, MeasureSpec.UNSPECIFIED);
            int heightSpec = MeasureSpec.makeMeasureSpec(getMeasuredHeight(), MeasureSpec.EXACTLY);
            keyPreview.measure(widthSpec, heightSpec);
        }
    }

    /**
     * 当布局发生变化时调用。
     * 标记命中矩形失效,延迟到需要时再重新计算。
     *
     * @param changed 布局是否改变。
     * @param l       左边界。
     * @param t       上边界。
     * @param r       右边界。
     * @param b       下边界。
     */
    @Override
    protected void onLayout(boolean changed, int l, int t, int r, int b) {
        super.onLayout(changed, l, t, r, b);
        // 布局变动时，仅标记失效，不立即计算（因为父容器可能还在定）
        if (changed)
            mRectInvalidated = true;
    }

    /**
     * 检查坐标是否在按键范围内。
     * 使用懒加载方式计算绝对坐标,提高性能。
     *
     * @param x X 坐标。
     * @param y Y 坐标。
     * @return true 表示坐标在按键范围内。
     */
    public boolean contains(int x, int y) {
        // 只有在真正需要判定点击时，才计算绝对坐标
        if (mRectInvalidated) {
            updateHitRect();
            mRectInvalidated = false;
        }
        return mHitRect.contains(x, y);
    }

    /**
     * 更新命中矩形(计算绝对坐标)。
     * 递归累加父容器的偏移量,直到找到 KeyboardView 根容器。
     */
    private void updateHitRect() {
        int left = getLeft();
        int top = getTop();

        ViewParent parent = getParent();
        // 1. 检查 parent 是否为 View 类型
        // 2. 检查 parent 是否已经是 KeyboardView（目标根容器）
        // 3. 只有 parent 是 View 且不是我们想要的根容器时，才继续累加
        while (parent instanceof View && !(parent instanceof KeyboardView)) {
            View parentView = (View) parent;
            left += parentView.getLeft();
            top += parentView.getTop();
            // 继续向上寻找
            parent = parentView.getParent();
        }

        // 最后更新 Rect 缓存
        mHitRect.set(left, top, left + getWidth(), top + getHeight());
    }

    // --- 5. 公开 API 方法 (Setters & Getters) ---

    /**
     * 设置文本内容(支持多行)。
     *
     * @param text 文本内容。
     */
    public void setText(String text) {
        // 根据文本是否为空设置视图可见性：有文本则显示，无文本则隐藏（保留布局空间）
        setVisibility(text != null ? VISIBLE : INVISIBLE);
        // 设置格式化后的主文本内容
        mClick.setText(mKeyStyle.getSpan(text));
        // 允许文本多行显示
        mClick.setSingleLine(false);
        // 设置最大行数为4行
        mClick.setMaxLines(4);
        // 设置文本超出部分以省略号结尾
        mClick.setEllipsize(TextUtils.TruncateAt.END);
        // 设置无障碍描述内容，方便 TalkBack 等辅助功能读取
        setContentDescription(text);
    }

    /**
     * 设置标签文本(单行显示)。
     *
     * @param text 标签文本。
     */
    public void setLabel(String text) {
        if (TextUtils.isEmpty(text)) return;
        // 设置视图为可见状态
        setVisibility(VISIBLE);
        // 设置格式化后的主文本内容
        mClick.setText(mKeyStyle.getSpan(text));
        // 允许文本多行显示
        mClick.setSingleLine(false);
        // 设置无障碍描述内容，方便 TalkBack 等辅助功能读取
        setContentDescription(text);
    }

    /**
     * 设置点击文本。
     * 用于没有 Key 配置的按键,直接设置文本和点击事件。
     *
     * @param text 点击文本。
     */
    public void setClickText(String text) {
        // 保存点击文本内容
        mClickText = text;
        // 根据文本是否为空设置视图可见性：有文本则显示，无文本则隐藏（保留布局空间）
        setVisibility(text != null ? VISIBLE : INVISIBLE);
        // 设置格式化后的主文本内容
        mClick.setText(mKeyStyle.getSpan(text));
        // 如果文本不为空，根据是否包含换行符决定单行显示模式
        // 注意：此处原代码使用 "/n" 可能是笔误，通常换行符为 "\n"，但为了保持原有逻辑行为不变，暂不修改字符串匹配内容
        if (text != null)
            mClick.setSingleLine(!text.contains("\n"));
        // 设置点击监听器，使该按键可响应点击事件
        setOnClickListener(this);
        // 设置无障碍描述内容，方便 TalkBack 等辅助功能读取
        setContentDescription(text);
    }


    /**
     * 设置长按文本。
     * 动态创建长按提示 TextView,并设置样式和位置。
     *
     * @param text 长按文本。
     */
    public void setLongClickText(String text) {
        if (mLongClick == null) {
            // 初始化 LongClick TextView
            if (!mKeyStyle.hasKey("long_click"))
                return;

            // 优先使用按键级别的 long_click 样式配置，其次使用主题全局的 key.long_click 配置
            KeyStyle mLongClickStyle;
            LuaValue keyLongClickStyle = mKey.getMk().get("long_click_style");
            if (keyLongClickStyle.istable()) {
                // 按键级别配置存在，基于主题 long_click 样式创建新的样式对象
                mLongClickStyle = new KeyStyle(keyLongClickStyle, mKeyStyle.getLongClickKeyStyle());
            } else {
                // 使用主题全局的 key.long_click 配置
                mLongClickStyle = mKeyStyle.getLongClickKeyStyle();
            }

            // 将解析后的长按样式存入样式数组，供后续状态切换时复用
            mHintStyles[HINT_LONG] = mLongClickStyle;
            // 如果样式配置为不显示，则直接返回，不再创建视图
            if (!mLongClickStyle.isShow())
                return;

            // 初始化长按文本 TextView
            mLongClick = new TightTextView(getContext());
            // 将创建的视图存入提示视图数组，方便统一管理（如隐藏/显示）
            mHints[HINT_LONG] = mLongClick;

            // 包含字体内边距，确保文本布局紧凑且一致
            mLongClick.setIncludeFontPadding(true);
            // 强制单行显示，防止长按文本换行影响布局
            mLongClick.setSingleLine(true);
            // 初始设置为可见，具体显示状态由 setText 控制
            mLongClick.setVisibility(View.VISIBLE);

            // 设置文本大小（单位：dp），默认值为 8
            mLongClick.setTextSize(TypedValue.COMPLEX_UNIT_DIP, mLongClickStyle.getTextSize(8));
            // if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            //    mLongClick.setLineHeight((int) mLongClickStyle.getTextSize(12));
            //}

            // 设置文本颜色
            mLongClick.setTextColor(mLongClickStyle.getTextColor());
            // 设置字体类型
            mLongClick.setTypeface(mLongClickStyle.getFont());

            // 设置水平和垂直偏移量，允许微调长按文本位置
            mLongClick.setTranslationX(mLongClickStyle.getSize("offset_x", 0));
            mLongClick.setTranslationY(mLongClickStyle.getSize("offset_y", 0));

            // 将 long_click 添加到 keyRoot 容器中
            // 布局参数：宽高包裹内容，重力为顶部居中（可被样式覆盖）
            keyRoot.addView(mLongClick, new FrameLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT, mLongClickStyle.getGravity(Gravity.TOP | Gravity.CENTER)));
            // 再次设置 TextView 内部的重力，确保文本在 View 内部的对齐方式与布局重力一致
            mLongClick.setGravity(mLongClickStyle.getGravity(Gravity.TOP | Gravity.CENTER));
            // Paint paint = mLongClick.getPaint();
            // Rect bounds = new Rect();
            // paint.getTextBounds(text, 0, text.length(), bounds);
            // int actualHeight = bounds.height(); // 符号真实的像素高度
            // int actualWidth = bounds.width();
            // mLongClick.setFirstBaselineToTopHeight(actualHeight);
        }

        // 根据文本内容控制可见性：有文本则显示，无文本则隐藏
        mLongClick.setVisibility(text != null ? VISIBLE : GONE);
        // 设置格式化后的长按文本内容
        mLongClick.setText(mKeyStyle.getLongClickKeyStyle().getSpan(text));
        // mLongClick.postInvalidate();
    }

    /**
     * 设置提示文本。
     * 动态创建提示 TextView,并设置样式和位置。
     *
     * @param text 提示文本。
     */
    public void setHintText(String text) {
        if (mHint == null) {
            // 初始化 Hint TextView
            if (!mKeyStyle.hasKey("hint"))
                return;

            // 优先使用按键级别的 hint 样式配置，其次使用主题全局的 key.hint 配置
            KeyStyle mHintStyle;
            LuaValue keyHintStyle = mKey.getMk().get("style_hint");
            if (keyHintStyle.istable()) {
                // 按键级别配置存在，基于主题 hint 样式创建新的样式对象
                mHintStyle = new KeyStyle(keyHintStyle, mKeyStyle.getHintKeyStyle());
            } else {
                // 使用主题全局的 key.hint 配置
                mHintStyle = mKeyStyle.getHintKeyStyle();
            }

            // 将解析后的提示样式存入样式数组，供后续滑动或状态切换时复用
            mHintStyles[HINT] = mHintStyle;
            // 如果样式配置为不显示，则直接返回，不再创建视图
            if (!mHintStyle.isShow()) return;

            // 初始化普通提示文本 TextView
            mHint = new TightTextView(getContext());
            // 将创建的视图存入提示视图数组，方便统一管理（如隐藏/显示）
            mHints[HINT] = mHint;

            // 包含字体内边距，确保文本布局紧凑且一致
            mHint.setIncludeFontPadding(true);
            // 强制单行显示，防止提示文本换行影响布局
            mHint.setSingleLine(true);
            // 初始设置为可见，具体显示状态由 setText 控制
            mHint.setVisibility(View.VISIBLE);

            // 设置文本大小（单位：dp），默认值为 8
            mHint.setTextSize(TypedValue.COMPLEX_UNIT_DIP, mHintStyle.getTextSize(8));
            // 设置文本颜色
            mHint.setTextColor(mHintStyle.getTextColor());
            // 设置字体类型
            mHint.setTypeface(mHintStyle.getFont());

            // 设置水平和垂直偏移量，允许微调提示文本位置
            mHint.setTranslationX(mHintStyle.getSize("offset_x", 0));
            mHint.setTranslationY(mHintStyle.getSize("offset_y", 0));

            // 将 hint 添加到 keyRoot 容器中
            // 布局参数：宽高包裹内容，重力为底部居中（可被样式覆盖）
            keyRoot.addView(mHint, new FrameLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT, mHintStyle.getGravity(Gravity.BOTTOM | Gravity.CENTER)));
            // 再次设置 TextView 内部的重力，确保文本在 View 内部的对齐方式与布局重力一致
            mHint.setGravity(mHintStyle.getGravity(Gravity.BOTTOM | Gravity.CENTER));
        }

        mHint.setVisibility(text != null ? VISIBLE : GONE);
        mHint.setText(mKeyStyle.getHintKeyStyle().getSpan(text));
        // mHint.postInvalidate();
    }

    /**
     * 设置文本颜色(保留方法)。
     *
     * @param color 颜色值。
     */
    @Keep
    public void setTextColor(int color) {
        mClick.setTextColor(color);
    }


    /**
     * 设置文本内边距。
     *
     * @param left   左边距。
     * @param top    上边距。
     * @param right  右边距。
     * @param bottom 下边距。
     */
    public void setTextPadding(int left, int top, int right, int bottom) {
        keyRoot.setPadding(left, top, right, bottom);
    }

    /**
     * 刷新按键显示。
     * 根据 ASCII 模式切换按键配置,或更新提示文本的显示/隐藏状态。
     */
    public void invalidateKey() {
        if (mKey != null) {
            // 如果存在 ASCII 模式的按键配置，根据当前 Rime 的 ASCII 模式状态切换按键对象
            if (mAsciiKey != null) {
                mKey = Rime.isAsciiMode() ? mAsciiKey : mDefKey;
                // 重新初始化按键内容（文本、提示等）
                initKey();
            } else {
                // 无 ASCII 切换逻辑，直接刷新按键内容
                initKey();
            }

            // 如果是 Shift 键，根据当前的 Shift 状态更新选中视觉效果
            if (mKey.isShift()) {
                setSelected(ModifierState.isShifted());
            }

            // 更新 q-p 等键位的动态长按助记显示（根据 Shift/ASCII 状态变化）
            updateDynamicLongClickHint();
        }

        // 检查全局配置中“隐藏按键提示”的状态是否发生变化
        boolean shouldHideHint = Config.is_hide_key_hint();
        if (shouldHideHint == _hide_key_hint) {
            // 状态未变，无需重复操作
            return;
        }

        // 更新本地缓存的状态
        _hide_key_hint = shouldHideHint;

        // 遍历所有方向的提示文本视图，统一设置可见性
        for (TextView hint : mHints) {
            if (hint != null) {
                // 如果配置为隐藏，则设为 GONE；否则设为 VISIBLE
                hint.setVisibility(shouldHideHint ? GONE : VISIBLE);
            }
        }
    }

    /**
     * 更新 q-p 键位的动态长按助记显示。
     * 根据当前 Shift 状态和 ASCII 模式，动态更新长按助记文本。
     */
    private void updateDynamicLongClickHint() {
        // 如果按键对象或长按文本视图为空，则直接返回
        if (mKey == null || mLongClick == null) return;

        // 获取按键的主标签文本
        String label = mKey.getLabel();
        // 如果标签为空或长度不为1（非单字符键），则不处理动态长按提示
        if (label == null || label.length() != 1) return;

        char ch = label.charAt(0);
        // 判断当前字符是否为英文字母（支持小写 a-z 和大写 A-Z）
        // 注意：在 Shift 状态下，字母键的 label 通常会变为大写，因此需要同时检查两种情况
        boolean isLetter = (ch >= 'a' && ch <= 'z') || (ch >= 'A' && ch <= 'Z');
        // 如果不是字母键，则不应用动态长按逻辑
        if (!isLetter) return;

        // 获取当前的 Shift 状态和 ASCII 模式状态
        boolean isShifted = ModifierState.isShifted();
        boolean isAsciiMode = Rime.isAsciiMode();

        // 根据当前状态获取动态生成的长按标签文本
        String dynamicLabel = mKey.getDynamicLongClickLabel(isShifted, isAsciiMode);

        // 如果动态标签不为空，则更新长按文本显示
        if (!TextUtils.isEmpty(dynamicLabel)) {
            setLongClickText(dynamicLabel);
        }
    }

    /**
     * 检查是否为编码区按键。
     *
     * @return true 表示是编码区按键。
     */
    public boolean isComposingKey() {
        return mKey != null && mKey.isComposingKey();
    }

    /**
     * 检查是否为 Shift 键。
     *
     * @return true 表示是 Shift 键。
     */
    public boolean isShift() {
        return mKey != null && mKey.isShift();
    }

    // --- 6. 私有初始化与辅助方法 ---

    /**
     * 初始化视图结构。
     * 创建 keyRoot 容器、mClick TextView、背景过渡动画、预览窗口等 UI 组件。
     */
    private void initView() {
        // 设置 KeyView 本身不裁剪子视图和内边距，允许预览等元素溢出显示
        setClipChildren(false);
        setClipToPadding(false);
        // 设置按键可点击且可获取焦点
        setClickable(true);
        setFocusable(true);

        // 初始化按键根布局容器 keyRoot
        keyRoot = new FrameLayout(getContext());
        // keyRoot 同样不裁剪子视图，确保内部元素（如提示文本）能正确显示
        keyRoot.setClipChildren(false);
        keyRoot.setClipToPadding(false);

        // 设置阴影颜色 (Android P / API 28+)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            int dShadowColor = mKeyStyle.getShadowColor();
            if (dShadowColor != 0) {
                keyRoot.setOutlineAmbientShadowColor(dShadowColor);
                keyRoot.setOutlineSpotShadowColor(dShadowColor);
            }
        }

        // 构建背景过渡动画 (正常状态 -> 按下状态)
        Drawable[] layers = new Drawable[2];
        // 第0层：正常状态背景
        layers[0] = mKeyStyle.getBackground();
        // 第1层：按下状态背景
        layers[1] = mPressedStyle.getBackground();

        // 特殊处理：如果正常背景是 LuaBitmapDrawable 且按下背景是 GradientDrawable
        // 则对 Bitmap 应用颜色滤镜以模拟按下效果
        if (layers[0] instanceof LuaBitmapDrawable) {
            if (layers[1] instanceof GradientDrawable) {
                // 使用 MULTIPLY 模式将按下状态的颜色叠加到 Bitmap 上
                PorterDuffColorFilter colorFilter = new PorterDuffColorFilter(mPressedStyle.getBackgroundColor(), PorterDuff.Mode.MULTIPLY);
                LuaBitmapDrawable bg = ((LuaBitmapDrawable) mKeyStyle.getBackground());
                bg.setColorFilter(colorFilter);
                // 将处理后的 Bitmap 作为按下状态的背景层
                layers[1] = bg;
            }
        }
        // 创建过渡动画并设置为 keyRoot 的背景
        transition = new TransitionDrawable(layers);
        keyRoot.setBackground(transition);

        // 初始化主文本 TextView (mClick)
        mClick = new TextView(getContext());
        mClick.setIncludeFontPadding(false); // 移除字体默认内边距，使文本更紧凑
        mClick.setSingleLine(true);          // 单行显示
        // 设置文本大小、颜色、字体
        mClick.setTextSize(TypedValue.COMPLEX_UNIT_DIP, mKeyStyle.getTextSize());
        mClick.setTextColor(mKeyStyle.getTextColor());
        mClick.setTypeface(mKeyStyle.getFont());

        // 初始状态设为不可见，等待内容设置后再显示
        setVisibility(View.INVISIBLE);

        // 将 mClick 添加到 keyRoot 中，居中对齐
        keyRoot.addView(mClick, new FrameLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT, mKeyStyle.getGravity(Gravity.CENTER)));

        // 设置 mClick 的偏移量
        mClick.setTranslationX(mKeyStyle.getSize("offset_x", 0));
        mClick.setTranslationY(mKeyStyle.getSize("offset_y", 0));

        // 设置 keyRoot 的海拔高度 (Elevation)，影响阴影大小
        int elevation = mKeyStyle.getElevation();
        keyRoot.setElevation(elevation);

        // 计算并设置 keyRoot 在 KeyView 中的边距 (Margins)
        Style margins = mKeyStyle.getStyle("margins");
        LayoutParams params = new LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
        // 左右上下边距为 elevation/3
        params.setMargins(margins.getSize("left", elevation / 3), margins.getSize("top", elevation / 3), margins.getSize("right", elevation / 3), margins.getSize("bottom", elevation / 3));

        // 设置按键内部文本的内边距
        Style padding = mKeyStyle.getStyle("padding");
        mClick.setPadding(padding.getSize("left", 0), padding.getSize("top", 0), padding.getSize("right", 0), padding.getSize("bottom", 0));

        // 设置 keyRoot 自身的内边距 (Padding)
        keyRoot.setPadding(padding.getSize("left", 0), padding.getSize("top", 0), padding.getSize("right", 0), padding.getSize("bottom", 0));

        // 将 keyRoot 添加到 KeyView 中
        addView(keyRoot, params);

        // 如果配置了预览窗口 (preview)，则初始化 keyPreview
        boolean previewDisabled = false;
        // 检查顺序（优先级从高到低）：
        // 1. 按键表上 mKey.getMk() 的 "preview" 字段
        // 2. 按键表上内联 style 表的 "preview" 字段
        // 3. 命名样式（mColor 中 styleName 对应表的 "preview" 字段，使用 rawget 绕过 __index）
        // 4. 样式链（mKeyStyle）中 "preview" 字段
        Log.d(TAG, "initView: style=" + (mKey != null ? mKey.getStyle() : "null") + " label=" + (mKey != null ? mKey.getLabel() : "null"));
        if (mKey != null) {
            LuaValue mk = mKey.getMk();
            if (mk != null && mk.istable()) {
                LuaTable keyTable = mk.checktable();
                LuaValue pv = keyTable.get("preview");
                Log.d(TAG, "initView: pv=" + pv + " isboolean=" + pv.isboolean());
                if (pv.isboolean()) {
                    previewDisabled = !pv.toboolean();
                }
                if (!previewDisabled) {
                    LuaValue styleVal = keyTable.get("style");
                    Log.d(TAG, "initView: styleVal=" + styleVal + " istable=" + styleVal.istable());
                    if (styleVal.istable()) {
                        LuaValue spv = styleVal.checktable().get("preview");
                        if (spv.isboolean()) {
                            previewDisabled = !spv.toboolean();
                        }
                    }
                }
            }
        }
        // 3. 命名样式 rawget 检查：直接读取 mColor[styleName]，不使用 __index 链
        if (!previewDisabled && mKey != null) {
            LuaValue mk = mKey.getMk();
            if (mk != null && mk.istable()) {
                LuaValue styleName = mk.checktable().get("style");
                Log.d(TAG, "initView: styleName=" + styleName);
                if (styleName.isstring()) {
                    LuaValue namedStyle = ThemeManager.getStyle().get(styleName.tojstring());
                    Log.d(TAG, "initView: namedStyle=" + namedStyle + " istable=" + namedStyle.istable());
                    if (namedStyle.istable()) {
                        LuaValue nsPv = namedStyle.checktable().rawget(LuaValue.valueOf("preview"));
                        Log.d(TAG, "initView: rawget preview from namedStyle=" + nsPv + " isboolean=" + nsPv.isboolean());
                        if (nsPv.isboolean()) {
                            previewDisabled = !nsPv.toboolean();
                        }
                    }
                }
            }
        }
        // 4. mKeyStyle 样式链检查
        if (!previewDisabled) {
            LuaValue stylePv = mKeyStyle.get("preview");
            Log.d(TAG, "initView: mKeyStyle.get(preview)=" + stylePv + " isboolean=" + stylePv.isboolean());
            if (stylePv.isboolean()) {
                previewDisabled = !stylePv.toboolean();
            }
        }
        Log.d(TAG, "initView: previewDisabled=" + previewDisabled);
        // 只有当 preview 未禁用且样式链中存在有效的 preview 表时才创建预览
        if (!previewDisabled) {
            LuaValue previewTable = mKeyStyle.get("preview");
            if (previewTable.istable()) {
            // 获取预览窗口的样式配置，若未定义则回退到当前按键样式
            KeyStyle previewStyle = mKeyStyle.getKeyStyle("preview", mKeyStyle);
            // 创建预览窗口的 TextView 实例
            keyPreview = new TextView(getContext());
            // 包含字体内边距，确保文本布局紧凑且一致
            keyPreview.setIncludeFontPadding(true);

            // 设置轮廓提供者以支持阴影绘制
            keyPreview.setOutlineProvider(ViewOutlineProvider.BACKGROUND);
            // 允许阴影溢出边界绘制，避免被裁剪
            keyPreview.setClipToOutline(false);

            // 设置预览窗口的背景 Drawable
            keyPreview.setBackground(previewStyle.getBackground());
            // 初始状态设为隐藏，仅在按下或滑动时显示
            keyPreview.setVisibility(GONE);
            // 设置文本在预览窗口中的对齐方式为居中
            keyPreview.setGravity(Gravity.CENTER);
            // label 不换行
            keyPreview.setSingleLine(true);
            // 设置预览窗口的海拔高度，影响阴影大小
            keyPreview.setElevation(previewStyle.getElevation());
            // 设置文本大小，单位为 DIP (密度独立像素)
            keyPreview.setTextSize(TypedValue.COMPLEX_UNIT_DIP, previewStyle.getTextSize());
            // 设置文本字体
            keyPreview.setTypeface(previewStyle.getFont());
            // 设置文本颜色
            keyPreview.setTextColor(previewStyle.getTextColor());

            // 设置预览窗口的阴影颜色 (Android P+)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                int dShadowColor = previewStyle.getShadowColor();
                if (dShadowColor != 0) {
                    keyPreview.setOutlineAmbientShadowColor(dShadowColor);
                    keyPreview.setOutlineSpotShadowColor(dShadowColor);
                }
            }

            // 设置预览窗口的内边距
            Style previewPadding = previewStyle.getStyle("padding");
            keyPreview.setPadding(
                previewPadding.getSize("left", 0),
                previewPadding.getSize("top", 0),
                previewPadding.getSize("right", 0),
                previewPadding.getSize("bottom", 0)
            );

            // 设置预览窗口的外边距
            Style previewMargins = previewStyle.getStyle("margins");
            LayoutParams previewLp = new LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.MATCH_PARENT, Gravity.CENTER_HORIZONTAL);
            previewLp.setMargins(
                previewMargins.getSize("left", 0),
                previewMargins.getSize("top", 0),
                previewMargins.getSize("right", 0),
                previewMargins.getSize("bottom", 0)
            );
            // 将预览窗口添加到 KeyView 中，宽度自适应内容
            addView(keyPreview, previewLp);
            // 预览偏移
            keyPreview.setTranslationX(previewStyle.getSize("offset_x", 0));
            keyPreview.setTranslationY(previewStyle.getSize("offset_y", 0));
            }
        }

        // 设置 keyRoot 的轮廓提供者以支持阴影绘制，并允许阴影溢出
        keyRoot.setOutlineProvider(ViewOutlineProvider.BACKGROUND);
        keyRoot.setClipToOutline(false);
    }

    /**
     * 初始化按键内容。
     * 设置主文本、长按文本、提示文本和滑动提示文本。
     */
    private void initKey() {
        if (mKey == null) return;

        // 1. 设置主标签文本 (Click Label)
        String click = mKey.getLabel();
        if (click != null) {
            setClickText(click);
        }
        // 根据编码状态重新应用按键样式
        reapplyKeyStyle();

        // 2. 设置长按标签文本 (Long Click Label)
        String longClick = mKey.getLongClickLabel();
        if (!TextUtils.isEmpty(longClick)) {
            setLongClickText(longClick);
        }

        // 3. 设置普通提示文本 (Hint)
        String hint = mKey.getHint();
        if (!TextUtils.isEmpty(hint)) {
            setHintText(hint);
        }

        // 4. 设置无障碍描述内容
        setContentDescription(mKey.getDescription());

        // 获取基础 Hint 样式，用于后续滑动方向样式的继承基准
        KeyStyle mHintKeyStyle = mKeyStyle.getHintKeyStyle();

        // 5. 处理上滑提示 (Swipe Up)
        String ev = mKey.getHint(SWIPE_UP);
        if (ev != null) {
            // 优先使用按键级别的 hint_up 配置，若不存在则继承主题全局的 hint.up 配置
            KeyStyle ht;
            LuaValue keyHintUp = mKey.getMk().get("hint_up");
            if (keyHintUp.istable()) {
                // 按键级别配置存在：基于主题的 hint.up 样式（若存在）或基础 hint 样式创建新样式对象
                KeyStyle defaultUpStyle = mHintKeyStyle.hasKey("up")
                        ? mHintKeyStyle.getKeyStyle("up", mHintKeyStyle)
                        : mHintKeyStyle;
                ht = new KeyStyle(keyHintUp, defaultUpStyle);
            } else if (mHintKeyStyle.hasKey("up")) {
                // 无按键级别配置：直接使用主题全局的 hint.up 配置
                ht = mHintKeyStyle.getKeyStyle("up", mHintKeyStyle);
            } else {
                // 均无特定配置：使用基础 hint 样式
                ht = mHintKeyStyle;
            }
            mHintStyles[SWIPE_UP] = ht;
            // 更新或创建上滑提示视图
            if (mHints[SWIPE_UP] != null) {
                mHints[SWIPE_UP].setText(ht.getSpan(ev));
            } else {
                mHints[SWIPE_UP] = addHint(ev, Gravity.TOP, ht);
            }
        }

        // 6. 处理下滑提示 (Swipe Down)
        ev = mKey.getHint(SWIPE_DOWN);
        if (ev != null) {
            // 优先使用按键级别的 hint_down 配置，若不存在则继承主题全局的 hint.down 配置
            KeyStyle ht;
            LuaValue keyHintDown = mKey.getMk().get("hint_down");
            if (keyHintDown.istable()) {
                KeyStyle defaultDownStyle = mHintKeyStyle.hasKey("down")
                        ? mHintKeyStyle.getKeyStyle("down", mHintKeyStyle)
                        : mHintKeyStyle;
                ht = new KeyStyle(keyHintDown, defaultDownStyle);
            } else if (mHintKeyStyle.hasKey("down")) {
                ht = mHintKeyStyle.getKeyStyle("down", mHintKeyStyle);
            } else {
                ht = mHintKeyStyle;
            }
            mHintStyles[SWIPE_DOWN] = ht;
            // 更新或创建下滑提示视图
            if (mHints[SWIPE_DOWN] != null) {
                mHints[SWIPE_DOWN].setText(ht.getSpan(ev));
            } else {
                mHints[SWIPE_DOWN] = addHint(ev, Gravity.BOTTOM, ht);
            }
        }

        // 7. 处理左滑提示 (Swipe Left)
        ev = mKey.getHint(SWIPE_LEFT);
        if (ev != null) {
            // 优先使用按键级别的 hint_left 配置，若不存在则继承主题全局的 hint.left 配置
            KeyStyle ht;
            LuaValue keyHintLeft = mKey.getMk().get("hint_left");
            if (keyHintLeft.istable()) {
                KeyStyle defaultLeftStyle = mHintKeyStyle.hasKey("left")
                        ? mHintKeyStyle.getKeyStyle("left", mHintKeyStyle)
                        : mHintKeyStyle;
                ht = new KeyStyle(keyHintLeft, defaultLeftStyle);
            } else if (mHintKeyStyle.hasKey("left")) {
                ht = mHintKeyStyle.getKeyStyle("left", mHintKeyStyle);
            } else {
                ht = mHintKeyStyle;
            }
            mHintStyles[SWIPE_LEFT] = ht;
            // 更新或创建左滑提示视图
            if (mHints[SWIPE_LEFT] != null) {
                mHints[SWIPE_LEFT].setText(ht.getSpan(ev));
            } else {
                mHints[SWIPE_LEFT] = addHint(ev, Gravity.LEFT, ht);
            }
        }

        // 8. 处理右滑提示 (Swipe Right)
        ev = mKey.getHint(SWIPE_RIGHT);
        if (ev != null) {
            // 优先使用按键级别的 hint_right 配置，若不存在则继承主题全局的 hint.right 配置
            // 处理右滑提示 (Swipe Right)
            KeyStyle ht;
            // 获取按键配置中的 hint_right 样式定义
            LuaValue keyHintRight = mKey.getMk().get("hint_right");
            if (keyHintRight.istable()) {
                // 如果按键级别配置存在：基于主题的 hint.right 样式（若存在）或基础 hint 样式创建新样式对象
                KeyStyle defaultRightStyle = mHintKeyStyle.hasKey("right")
                        ? mHintKeyStyle.getKeyStyle("right", mHintKeyStyle)
                        : mHintKeyStyle;
                ht = new KeyStyle(keyHintRight, defaultRightStyle);
            } else if (mHintKeyStyle.hasKey("right")) {
                // 无按键级别配置：直接使用主题全局的 hint.right 配置
                ht = mHintKeyStyle.getKeyStyle("right", mHintKeyStyle);
            } else {
                // 均无特定配置：使用基础 hint 样式
                ht = mHintKeyStyle;
            }
            // 将解析后的右滑样式存入样式数组，供后续滑动时复用
            mHintStyles[SWIPE_RIGHT] = ht;
            // 更新或创建右滑提示视图
            if (mHints[SWIPE_RIGHT] != null) {
                // 如果视图已存在，直接更新文本内容
                mHints[SWIPE_RIGHT].setText(ht.getSpan(ev));
            } else {
                // 如果视图不存在，创建新的右滑提示视图并添加到布局中
                mHints[SWIPE_RIGHT] = addHint(ev, Gravity.RIGHT, ht);
            }
        }
    }

    /**
     * 获取当前生效的 KeyStyle。
     * 如果按键有编码配置表且 Rime 处于编码状态，
     * 则返回以编码表中字段覆盖按键样式的合并样式；否则返回按键默认样式。
     */
    private KeyStyle getActiveStyle() {
        LuaValue mk = mKey != null ? mKey.getComposingMk() : null;
        if (mk != null && !mk.isnil() && Rime.isComposing()) {
            return new KeyStyle(mk, mKeyStyle);
        }
        return mKeyStyle;
    }

    /**
     * 根据编码状态重新应用按键样式（文字大小/颜色/字体、偏移、海拔、内边距、外边距）。
     * 使用 getActiveStyle() 获取的样式，支持编码表任意字段覆盖。
     */
    private void reapplyKeyStyle() {
        KeyStyle style = getActiveStyle();
        // 1. 文字样式
        mClick.setTextSize(TypedValue.COMPLEX_UNIT_DIP, style.getTextSize());
        mClick.setTextColor(style.getTextColor());
        mClick.setTypeface(style.getFont());
        // 2. 偏移量
        mClick.setTranslationX(style.getSize("offset_x", 0));
        mClick.setTranslationY(style.getSize("offset_y", 0));
        // 3. 海拔
        keyRoot.setElevation(style.getElevation());
        // 4. 阴影颜色 (Android P+)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            int dShadowColor = style.getShadowColor();
            if (dShadowColor != 0) {
                keyRoot.setOutlineAmbientShadowColor(dShadowColor);
                keyRoot.setOutlineSpotShadowColor(dShadowColor);
            }
        }
        // 5. 内边距
        Style padding = style.getStyle("padding");
        mClick.setPadding(padding.getSize("left", 0), padding.getSize("top", 0),
                padding.getSize("right", 0), padding.getSize("bottom", 0));
        keyRoot.setPadding(padding.getSize("left", 0), padding.getSize("top", 0),
                padding.getSize("right", 0), padding.getSize("bottom", 0));
        // 6. 外边距
        Style margins = style.getStyle("margins");
        LayoutParams params = (LayoutParams) keyRoot.getLayoutParams();
        if (params != null) {
            params.setMargins(margins.getSize("left", style.getElevation() / 3),
                    margins.getSize("top", style.getElevation() / 3),
                    margins.getSize("right", style.getElevation() / 3),
                    margins.getSize("bottom", style.getElevation() / 3));
            keyRoot.setLayoutParams(params);
        }
    }

    /**
     * 添加提示文本。
     * 创建指定方向的提示 TextView,并设置样式和位置。
     *
     * @param label      提示文本。
     * @param g          对齐方式(Gravity)。
     * @param mHintStyle 提示样式。
     * @return 创建的 TextView,如果不需要显示则返回 null。
     */
    private TextView addHint(String label, int g, KeyStyle mHintStyle) {
        // 如果样式配置为不显示，则直接返回 null
        if (!mHintStyle.isShow())
            return null;

        // 根据重力方向选择 TextView 类型：上下方向使用 TightTextView 以优化垂直间距，左右方向使用普通 TextView
        TextView hint = (g == Gravity.TOP || g == Gravity.BOTTOM) ? new TightTextView(getContext()) : new TextView(getContext());

        // 包含字体内边距，确保文本布局一致
        hint.setIncludeFontPadding(true);

        // 设置格式化后的文本内容
        hint.setText(mHintStyle.getSpan(label));

        // 强制单行显示
        hint.setSingleLine(true);

        // 设置为可见状态
        hint.setVisibility(View.VISIBLE);

        // 设置文本大小（单位：dp），默认值为 8
        hint.setTextSize(TypedValue.COMPLEX_UNIT_DIP, mHintStyle.getTextSize(8));

        // 设置文本颜色
        hint.setTextColor(mHintStyle.getTextColor());

        // 设置字体类型
        hint.setTypeface(mHintStyle.getFont());

        // 将 hint 添加到 keyRoot 容器中，布局参数为包裹内容，重力为指定方向居中
        keyRoot.addView(hint, new FrameLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT, g | Gravity.CENTER));

        // 再次设置字体类型（冗余调用，但保留以维持原逻辑）
        hint.setTypeface(mHintStyle.getFont());

        // 设置水平和垂直偏移量
        hint.setTranslationX(mHintStyle.getSize("offset_x", 0));
        hint.setTranslationY(mHintStyle.getSize("offset_y", 0));

        // 如果新添加的 hint 与长按文本 (mLongClick) 重力位置相同，则隐藏长按文本以避免重叠
        if (mLongClick != null && hint.getGravity() == mLongClick.getGravity())
            mLongClick.setVisibility(GONE);

        // 如果新添加的 hint 与普通提示文本 (mHint) 重力位置相同，则隐藏普通提示文本以避免重叠
        if (mHint != null && hint.getGravity() == mHint.getGravity())
            mHint.setVisibility(GONE);

        // 返回创建并配置好的 hint 视图
        return hint;
    }

    /**
     * 创建按钮背景(带圆角和波纹效果)。
     *
     * @param color  背景颜色。
     * @param radius 圆角半径。
     * @return RippleDrawable 背景。
     */
    private Drawable createButtonBackground(int color, float radius) {
        GradientDrawable content = new GradientDrawable();
        content.setShape(GradientDrawable.RECTANGLE);
        content.setColor(color);
        content.setCornerRadius(radius);
        return new RippleDrawable(ColorStateList.valueOf(Color.parseColor("#40000000")), content, content);
    }


    /**
     * 创建按钮背景(使用现有 Drawable)。
     *
     * @param content 内容 Drawable。
     * @return RippleDrawable 背景。
     */
    private Drawable createButtonBackground(Drawable content) {
        return new RippleDrawable(ColorStateList.valueOf(Color.parseColor("#40000000")), content, content);
    }

    /**
     * 按律动模式播放按键音效。
     * 由 KeyStyle 的 sound_rhythm 字段决定播放行为:
     * random（随机选音效）/ rate（固定第一个音效）。
     * 两种模式都会循环应用播放速率（默认 0.8→1.0→1.2）制造音高起伏的律动。
     * 当样式未配置自定义音效时,回退到系统默认点击音效。
     */
    private void playKeySound(KeyStyle style) {
        if (style == null) return;
        float[] params = style.pickNextSoundEffect();
        int soundId = (int) params[0];
        if (soundId > 0) {
            ThemeManager.play(soundId, params[1], 1.0f);
        } else {
            // 播放系统默认点击音效
            playSoundEffect(SoundEffectConstants.CLICK);
        }
    }

    private boolean mLongClicked;
    private FloatKeyboard popupKeyboard;
    // --- 7. 内部类与 Runnables ---
    /**
     * 长按检测 Runnable。
     * 在长按超时后触发,显示弹出键盘或执行长按事件。
     */
    private final Runnable mLongClickRunnable = new Runnable() {
        @Override
        public void run() {
            // 检查当前是否仍处于按下状态，如果已释放则直接返回，避免误触发长按逻辑
            if (!isPressed()) return;

            // 1. 处理弹出键盘 (Popup Keyboard)
            // 获取按键配置的弹出键列表（通常用于长按显示候选字或符号面板）
            List<String> popup = mKey.getPopupKeys();
            Log.d(TAG, "popup: " + popup);
            if (popup != null) {
                // 创建浮动键盘实例
                popupKeyboard = new FloatKeyboard(getContext(), ThemeManager.getGlobals(), popup);
                // 显示浮动键盘
                showPopup();
                // 取消当前按键的按下状态，因为焦点已转移到浮动键盘
                setPressed(false);
                return;
            }

            // 2. 处理长按反馈 (振动与声音)
            // 检查长按样式配置中是否启用了振动反馈
            if (mKeyStyle.getLongClickKeyStyle().isVibrationEnabled()) {
                // 获取自定义振动效果
                VibrationEffect ve = mKeyStyle.getLongClickKeyStyle().getVibrationEffect();
                if (ve != null) {
                    // 执行自定义振动
                    ThemeManager.vibrate(ve);
                } else {
                    // 执行系统默认的长按振动反馈
                    boolean ret = performHapticFeedback(
                            HapticFeedbackConstants.LONG_PRESS,
                            HapticFeedbackConstants.FLAG_IGNORE_GLOBAL_SETTING
                    );
                }
            }
            // 检查长按样式配置中是否启用了声音反馈
            if (mKeyStyle.getLongClickKeyStyle().isSoundEnabled() && !Rime.getRimeOption("_hide_key_sound")) {
                // 按律动模式播放长按音效
                playKeySound(mKeyStyle.getLongClickKeyStyle());
            }

            // 标记长按事件已发生，防止抬起时触发单击事件
            mLongClicked = true;

            // 3. 处理 Shift 键的特殊长按逻辑
            if (mKey.isShift()) {
                // Shift 键长按通常意味着锁定大写 (Shift Lock)
                ModifierState.setShiftLock(true);
                mTrime.setShifted(true);
                return;
            }

            // 4. 处理动态长按事件
            // 获取当前的 Shift 状态和 ASCII 模式状态
            boolean isShifted = ModifierState.isShifted();
            boolean isAsciiMode = Rime.isAsciiMode();
            // 根据当前状态获取动态定义的长按事件（例如不同模式下长按同一键产生不同字符）
            Event dynamicLongClick = mKey.getDynamicLongClick(isShifted, isAsciiMode);

            Log.d(TAG, "isShifted: " + isShifted);
            Log.d(TAG, "isAsciiMode: " + isAsciiMode);
            Log.d(TAG, "dynamicLongClick: " + dynamicLongClick);

            if (dynamicLongClick != null) {
                // 如果有动态长按事件，显示预览文本并发送事件
                showPreview(true, dynamicLongClick.getLabel());
                TrimeService.getInstance().onEvent(dynamicLongClick);
                return;
            }

            // 5. 启动重复按键任务
            // 如果没有上述特殊处理，则启动重复按键 Runnable，实现长按连续输入
            postDelayed(mRepeatableRunnable, 200);
        }
    };

    /**
     * 显示弹出键盘。
     * 计算弹出窗口的位置,确保不超出屏幕边界。
     */
    private void showPopup() {
        // 获取当前按键在屏幕上的绝对坐标
        int[] point = new int[2];
        getLocationOnScreen(point);
        int x = point[0];
        int width = getWidth();

        // 将弹出键盘添加到当前视图中，初始位置为左上角对齐
        addView(popupKeyboard, new FrameLayout.LayoutParams(popupKeyboard.getRawWidth(), popupKeyboard.getRawHeight(), Gravity.TOP | Gravity.LEFT));

        // 计算弹出键盘水平居中对齐时的目标 X 坐标
        int dx = x + width / 2 - popupKeyboard.getRawWidth() / 2;

        // 获取 TrimeService 实例以获取屏幕宽度进行边界检查
        TrimeService trime = TrimeService.getInstance();

        // 边界检查：确保弹出键盘不超出屏幕左边界
        if (dx < 0) {
            dx = 0;
        }
        // 边界检查：确保弹出键盘不超出屏幕右边界
        else if (dx + popupKeyboard.getRawWidth() > trime.getWidth()) {
            dx = trime.getWidth() - popupKeyboard.getRawWidth();
        }

        // 设置弹出键盘的平移偏移量，使其显示在计算后的正确位置
        // TranslationX 是相对于原始布局位置的偏移
        popupKeyboard.setTranslationX(dx - x);
        // TranslationY 设置为负的键盘高度，使其显示在按键上方
        popupKeyboard.setTranslationY(-popupKeyboard.getRawHeight());

        // 记录原始 X 坐标与最终显示 X 坐标的差值，用于后续触摸事件坐标转换
        popupKeyboard.setOffsetX(x - dx);
    }

    /**
     * 重复按键 Runnable。
     * 在长按后持续触发点击事件,实现快速输入。
     */
    private final Runnable mRepeatableRunnable = new Runnable() {
        @Override
        public void run() {
            // 如果按键不再处于按下状态，则停止重复任务
            if (!isPressed()) return;

            // 1. 处理振动反馈
            if (mKeyStyle.getLongClickKeyStyle().isVibrationEnabled()) {
                VibrationEffect ve = mKeyStyle.getLongClickKeyStyle().getVibrationEffect();
                if (ve != null) {
                    // 使用自定义振动效果
                    ThemeManager.vibrate(ve);
                } else {
                    // 使用系统默认的长按振动反馈
                    performHapticFeedback(
                            HapticFeedbackConstants.LONG_PRESS,
                            HapticFeedbackConstants.FLAG_IGNORE_GLOBAL_SETTING
                    );
                }
            }

            // 2. 处理声音反馈
            if (mKeyStyle.getLongClickKeyStyle().isSoundEnabled() && !Rime.getRimeOption("_hide_key_sound")) {
                // 按律动模式播放长按音效
                playKeySound(mKeyStyle.getLongClickKeyStyle());
            }

            // 3. 触发点击事件（执行按键逻辑）
            onClick(KeyView.this);

            // 4. 延迟再次执行自身，实现连续重复触发
            postDelayed(this, mKeyStyle.getRepeatClickTime());
        }
    };

    /**
     * 滑动重复按键 Runnable。
     * 在滑动到某个方向后持续触发该方向的事件。
     */
    private final Runnable mSwipRepeatableRunnable = new Runnable() {
        @Override
        public void run() {
            // 移除当前的回调，防止重复执行，稍后如果需要会再次添加
            removeCallbacks(this);

            // 处理振动反馈
            if (mKeyStyle.getLongClickKeyStyle().isVibrationEnabled()) {
                VibrationEffect ve = mKeyStyle.getLongClickKeyStyle().getVibrationEffect();
                if (ve != null) {
                    // 使用自定义振动效果
                    ThemeManager.vibrate(ve);
                } else {
                    // 使用系统默认的长按振动反馈
                    performHapticFeedback(
                            HapticFeedbackConstants.LONG_PRESS,
                            HapticFeedbackConstants.FLAG_IGNORE_GLOBAL_SETTING
                    );
                }
            }

            // 处理声音反馈
            if (mKeyStyle.getLongClickKeyStyle().isSoundEnabled() && !Rime.getRimeOption("_hide_key_sound")) {
                // 按律动模式播放长按音效
                playKeySound(mKeyStyle.getLongClickKeyStyle());
            }

            // 如果当前存在有效的滑动方向，则触发对应的事件
            if (direction != SWIPE_NONE) {
                Event ev = mKey.getEvent(direction);
                if (ev != null) {
                    mTrime.onEvent(ev);
                }
            }

            // 延迟再次执行自身，实现连续重复触发
            postDelayed(this, mKeyStyle.getRepeatClickTime());
        }
    };

    /**
     * 设置点击文本内边距。
     *
     * @param left   左边距。
     * @param top    上边距。
     * @param right  右边距。
     * @param bottom 下边距。
     */
    public void setClickPadding(int left, int top, int right, int bottom) {
        mClick.setPadding(left, top, right, bottom);
    }


    private Bitmap maskBitmap;
    private int lastWidth, lastHeight;
    /**
     * 是否启用异形形状触摸检测(默认关闭,提升性能)
     */
    private boolean isShapeDetectionEnabled = false;

    /**
     * 设置是否开启异形形状触摸检测。
     *
     * @param enabled true: 只有点在不透明区域才响应; false: 点击矩形区域均响应。
     */
    public void setShapeDetectionEnabled(boolean enabled) {
        this.isShapeDetectionEnabled = enabled;
        // 如果关闭开关，建议释放内存
        if (!enabled && maskBitmap != null) {
            maskBitmap.recycle();
            maskBitmap = null;
        }
    }


    /**
     * 设置最小宽度。
     *
     * @param minWidth 最小宽度(像素)。
     */
    @Override
    public void setMinimumWidth(int minWidth) {
        super.setMinimumWidth(minWidth);
        keyRoot.setMinimumWidth(minWidth);
    }

    /**
     * 处理触摸事件。
     * 支持异形按键检测、弹出键盘、滑动选择等功能。
     *
     * @param event 触摸事件。
     * @return true 表示事件已处理。
     */
    @Override
    public boolean onTouchEvent(MotionEvent event) {
        // 1. 处理按下事件 (ACTION_DOWN)：触发触觉反馈和音效
        if (event.getAction() == MotionEvent.ACTION_DOWN) {
            // 检查是否启用振动反馈
            if (mKeyStyle.isVibrationEnabled()) {
                VibrationEffect ve = mKeyStyle.getVibrationEffect();
                if (ve != null) {
                    // 使用自定义振动效果
                    ThemeManager.vibrate(ve);
                } else {
                    // 使用系统默认键盘点击振动反馈
                    performHapticFeedback(
                            HapticFeedbackConstants.KEYBOARD_TAP,
                            HapticFeedbackConstants.FLAG_IGNORE_GLOBAL_SETTING
                    );
                }
            }
            // 检查是否启用声音反馈
            if (mKeyStyle.isSoundEnabled() && !Rime.getRimeOption("_hide_key_sound")) {
                // 按律动模式播放按键音效
                playKeySound(mKeyStyle);
            }
        }

        // 2. 异形按键检测：如果启用且点击位置透明，则不响应触摸
        if (isShapeDetectionEnabled && event.getAction() == MotionEvent.ACTION_DOWN) {
            if (isPixelTransparent(event)) return false;
        }

        // 3. 处理弹出键盘 (Popup Keyboard) 的触摸事件分发
        if (popupKeyboard != null) {
            if (event.getActionMasked() == MotionEvent.ACTION_MOVE) {
                // 将移动事件分发给弹出键盘
                popupKeyboard.dispatchTouchEvent(event);
                return true;
            }

            if (event.getAction() == MotionEvent.ACTION_UP || event.getAction() == MotionEvent.ACTION_CANCEL) {
                // 将抬起或取消事件分发给弹出键盘
                popupKeyboard.dispatchTouchEvent(event);
                // 移除并清理弹出键盘
                removeView(popupKeyboard);
                popupKeyboard = null;
                return true;
            }
        }

        // 4. 如果当前按键为空或不支持滑动事件，交由父类处理
        if (mKey == null || !mKey.hasSwipeEvent())
            return super.onTouchEvent(event);

        // 5. 在按下时请求父容器不要拦截触摸事件，以支持滑出边界的滑动操作
        if (event.getAction() == MotionEvent.ACTION_DOWN) {
            ViewParent parent = getParent();
            if (parent != null) {
                parent.requestDisallowInterceptTouchEvent(true);
            }
        }

        // 6. 处理滑动逻辑 (ACTION_MOVE)
        if (event.getAction() == MotionEvent.ACTION_MOVE) {
            handleSwipeEvent(event);
        }
        // 7. 处理抬起或取消事件 (ACTION_UP / ACTION_CANCEL)
        else if (event.getAction() == MotionEvent.ACTION_UP || event.getAction() == MotionEvent.ACTION_CANCEL) {
            // 如果发生了滑动（方向不为 NONE）
            if (direction != SWIPE_NONE) {
                if (mKeyStyle.getBoolean("swipe_repeatable", false)) {
                    // 如果是可重复滑动的按键，停止重复任务
                    removeCallbacks(mSwipRepeatableRunnable);
                } else {
                    // 否则，触发对应方向的单次事件
                    Event ev = mKey.getEvent(direction);
                    if (ev != null) mTrime.onEvent(ev);
                }
                // 重置状态：隐藏预览，重置方向，取消按下状态
                showPreview(false, null);
                direction = SWIPE_NONE;
                lastDirection = SWIPE_NONE;
                setPressed(false);
                return true; // 拦截事件，防止触发默认的 onClick
            }
            // 如果没有发生滑动，重置状态并通知服务手指抬起
            lastDirection = SWIPE_NONE;
            showPreview(false, null);
            mTrime.onUp(0);
        }

        // 8. 其他情况交由父类处理
        return super.onTouchEvent(event);
    }

    // 1. 定义方向常量
    /**
     * 无滑动
     */
    public static final int SWIPE_NONE = 0;
    /**
     * 提示文本索引
     */
    public static final int HINT = KeyEventType.CLICK.ordinal();
    /**
     * 长按文本索引
     */
    public static final int HINT_LONG = KeyEventType.LONG_CLICK.ordinal();
    /**
     * 上滑索引
     */
    public static final int SWIPE_UP = KeyEventType.SWIPE_UP.ordinal();
    /**
     * 下滑索引
     */
    public static final int SWIPE_DOWN = KeyEventType.SWIPE_DOWN.ordinal();
    /**
     * 左滑索引
     */
    public static final int SWIPE_LEFT = KeyEventType.SWIPE_LEFT.ordinal();
    /**
     * 右滑索引
     */
    public static final int SWIPE_RIGHT = KeyEventType.SWIPE_RIGHT.ordinal();
    /**
     * 当前滑动方向
     */
    private int direction = 0;
    /**
     * 上一次滑动方向
     */
    private int lastDirection = 0;
    /**
     * 8个方向的提示样式数组
     */
    private final KeyStyle[] mHintStyles = new KeyStyle[8];

    /**
     * 处理滑动事件。
     * 根据手指位置判断滑动方向,并触发对应的事件。
     *
     * @param event 触摸事件。
     */
    private void handleSwipeEvent(MotionEvent event) {
        // 获取当前触摸点的坐标以及按键视图的宽高
        float x = event.getX();
        float y = event.getY();
        float w = getWidth();
        float h = getHeight();

        // 1. 计算手指超出按键边界的偏移量（绝对值）
        // 如果手指在按键内部，dx/dy 为 0；如果在外部，则为超出的距离
        float dx = (x < 0) ? -x : (x > w ? x - w : 0);
        float dy = (y < 0) ? -y : (y > h ? y - h : 0);

        // 2. 根据偏移量判断滑动方向
        // 如果未超出边界，则方向为无滑动
        if (dx == 0 && dy == 0) {
            direction = SWIPE_NONE;
        }
        // 如果水平偏移量大于垂直偏移量，判定为水平滑动
        else if (dx > dy) {
            direction = (x < 0) ? SWIPE_LEFT : SWIPE_RIGHT;
        }
        // 否则判定为垂直滑动
        else {
            direction = (y < 0) ? SWIPE_UP : SWIPE_DOWN;
        }

        // 3. 当滑动方向发生改变时，执行相应的逻辑
        if (direction != lastDirection) {
            // 情况 A: 进入了某个滑动方向（非中心区域）
            if (direction != SWIPE_NONE) {
                // 移除长按和重复点击的回调，防止在滑动过程中误触发单击或长按事件
                removeCallbacks(mLongClickRunnable);
                removeCallbacks(mRepeatableRunnable);

                // 获取当前滑动方向对应的事件
                Event ev = mKey.getEvent(direction);
                if (ev != null) {
                    // 显示预览窗口：优先使用对应方向的样式格式化文本，否则直接使用事件标签
                    CharSequence previewText = (mHintStyles[direction] != null)
                            ? mHintStyles[direction].getSpan(ev.getLabel())
                            : ev.getLabel();
                    showPreview(true, previewText);

                    // 如果该方向支持滑动重复触发，则启动重复任务
                    if (mKeyStyle.getBoolean("swipe_repeatable", false)) {
                        postDelayed(mSwipRepeatableRunnable, mKeyStyle.getRepeatClickTime());
                    }
                } else {
                    // 如果该方向没有定义事件，隐藏预览并停止重复任务
                    showPreview(false, null);
                    removeCallbacks(mSwipRepeatableRunnable);
                }
            }
            // 情况 B: 回到了按键中心区域（无滑动状态）
            else {
                // 恢复显示普通的按键按下预览
                showPreview(true, mKeyStyle.getPressedStyle().getSpan(mKey.getLabel()));
                // 停止滑动重复任务
                removeCallbacks(mSwipRepeatableRunnable);
            }

            // 更新上一次的方向记录
            lastDirection = direction;
        }
    }

    /**
     * 检查像素是否透明(异形按键检测)。
     * 使用 ALPHA_8 格式的 Bitmap 缓存背景,检测触摸点的透明度。
     *
     * @param event 触摸事件。
     * @return true 表示像素透明(不响应点击)。
     */
    private boolean isPixelTransparent(MotionEvent event) {
        // 1. 获取相对于 keyRoot 的触摸坐标及尺寸
        int x = (int) event.getX() - keyRoot.getLeft();
        int y = (int) event.getY() - keyRoot.getTop();
        int w = keyRoot.getWidth();
        int h = keyRoot.getHeight();

        // 2. 边界防御：如果触摸点在矩形外，或者 View 还没加载完，视为透明（不响应点击）
        if (x < 0 || x >= w || y < 0 || y >= h || w <= 0 || h <= 0) {
            return true;
        }

        // 3. 只有尺寸变化时才重新绘制 Mask，节省性能
        if (maskBitmap == null || w != lastWidth || h != lastHeight) {
            // 释放旧资源，防止内存泄漏
            if (maskBitmap != null) {
                maskBitmap.recycle();
            }

            lastWidth = w;
            lastHeight = h;

            // ALPHA_8 格式最省内存，每个像素仅占 1 字节，只存储透明度信息
            maskBitmap = Bitmap.createBitmap(w, h, Bitmap.Config.ALPHA_8);
            Canvas maskCanvas = new Canvas(maskBitmap);

            Drawable bg = keyRoot.getBackground();
            if (bg instanceof TransitionDrawable) {
                // 获取过渡动画的第一层（正常状态背景）作为掩码基准
                Drawable tempBg = ((TransitionDrawable) bg).getDrawable(0).mutate();
                tempBg.setBounds(0, 0, w, h);
                tempBg.draw(maskCanvas);
            } else if (bg != null) {
                // 复制一份背景，防止修改原背景的 Bounds 影响其他绘制
                Drawable tempBg = bg.mutate();
                tempBg.setBounds(0, 0, w, h);
                tempBg.draw(maskCanvas);
            }
        }

        // 4. 检测像素透明度。
        // 对于 ALPHA_8 格式的 Bitmap，getPixel 返回的值中，Alpha 分量位于最高 8 位。
        // 阈值 0x40 (64/255 ≈ 25%)：允许一点点微弱的阴影/羽化边缘被判定为有效点击区域，
        // 如果 Alpha 值小于此阈值，则认为是透明区域，不响应点击。
        return (maskBitmap.getPixel(x, y) >> 24 & 0xff) < 0x40;
    }

    /**
     * 设置文本大小。
     *
     * @param i    单位类型。
     * @param size 大小值。
     */
    public void setTextSize(int i, float size) {
        mClick.setTextSize(i, size);
    }

    /**
     * 获取文本内容。
     *
     * @return CharSequence 文本内容。
     */
    public CharSequence getText() {
        return mClick.getText();
    }

    /**
     * 设置内边距。
     *
     * @param left   左边距。
     * @param top    上边距。
     * @param right  右边距。
     * @param bottom 下边距。
     */
    @Override
    public void setPadding(int left, int top, int right, int bottom) {
        keyRoot.setPadding(left, top, right, bottom);
    }

    /**
     * 设置是否单行显示。
     *
     * @param b true 表示单行显示。
     */
    public void setSingleLine(boolean b) {
        mClick.setSingleLine(b);
    }
}
