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
    /** 快速进出缓动插值器 */
    private static final Interpolator FAST_OUT_SLOW_IN = new FastOutSlowInInterpolator();

    // --- 2. 成员变量 ---
    /** TrimeService 实例 */
    private final TrimeService mTrime;
    /** 按键样式 */
    private final KeyStyle mKeyStyle;
    /** 按下状态样式 */
    private final KeyStyle mPressedStyle;
    /** ASCII 模式专用的按键对象 */
    private final Key mAsciiKey;
    /** 默认按键对象 */
    private final Key mDefKey;
    /** 当前按键对象(可能在 ASCII 模式和默认模式之间切换) */
    private Key mKey;
    /** 主文本 TextView */
    private TextView mClick;
    /** 提示文本 TextView */
    private TextView mHint;
    /** 长按文本 TextView */
    private TextView mLongClick;
    /** 按键根布局容器 */
    private FrameLayout keyRoot;
    /** 点击文本内容 */
    private String mClickText;
    /** 背景过渡动画(正常状态 -> 按下状态) */
    private TransitionDrawable transition;
    /** 是否处于按下状态 */
    private boolean mPressed;
    /** 命中矩形是否失效(需要重新计算) */
    private boolean mRectInvalidated;
    /** 是否处于选中状态 */
    private boolean mSelected;
    /** 预览窗口 TextView(长按或滑动时显示) */
    private TextView keyPreview;
    /** 动画监听器(暂未使用) */
    private Animator.AnimatorListener mAnimatorListener;
    //private TextView mHintUp;
    //private TextView mHintDown;
    //private TextView mHintLeft;
    //private TextView mHintRight;
    /** 8个方向的提示文本数组(上、下、左、右、长按等) */
    private TextView[] mHints = new TextView[8];
    /** 是否隐藏按键提示 */
    private boolean _hide_key_hint;

    // --- 3. 构造函数 ---
    /**
     * 构造函数(无按键配置)。
     *
     * @param context 上下文。
     */
    public KeyView(@NonNull Context context) {
        super(context);
        mDefKey = null;
        mAsciiKey = null;
        mTrime = TrimeService.getInstance();
        mKeyStyle = ThemeManager.getStyle().getKeyStyle("key");
        mPressedStyle = mKeyStyle.getKeyStyle("pressed", mKeyStyle);
        initView();
    }

    /**
     * 构造函数(带按键配置)。
     *
     * @param context 上下文。
     * @param v 按键配置对象。
     */
    public KeyView(@NonNull Context context, Key v) {
        super(context);
        mTrime = TrimeService.getInstance();
        mKey = v;
        mDefKey = v;
        mAsciiKey = mKey.getAsciiKey();
        mKeyStyle = ThemeManager.getStyle().getKeyStyle(v.getStyle(), ThemeManager.getStyle().getKeyStyle("key"));
        mPressedStyle = mKeyStyle.getKeyStyle("pressed", mKeyStyle);
        initView();
        initKey();
    }

    /**
     * 构造函数(带按键样式)。
     *
     * @param context 上下文。
     * @param v 按键样式对象。
     */
    public KeyView(@NonNull Context context, KeyStyle v) {
        super(context);
        mDefKey = null;
        mAsciiKey = null;
        mTrime = TrimeService.getInstance();
        mKeyStyle = v;
        mPressedStyle = mKeyStyle.getKeyStyle("pressed", mKeyStyle);
        initView();
    }

    /**
     * 构造函数(带按键配置和样式)。
     *
     * @param context 上下文。
     * @param v 按键配置对象。
     * @param s 按键样式对象。
     */
    public KeyView(@NonNull Context context, Key v, KeyStyle s) {
        super(context);
        mTrime = TrimeService.getInstance();
        mKey = v;
        mDefKey = v;
        mAsciiKey = mKey.getAsciiKey();
        mKeyStyle = ThemeManager.getStyle().getKeyStyle(v.getStyle(), s);
        mPressedStyle = mKeyStyle.getKeyStyle("pressed", mKeyStyle);
        initView();
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
        if (mLongClicked) {
            mLongClicked = false;
            return;
        }
        if (mKey != null) {
            if (mKey.isShift()) {
                if (ModifierState.isShiftLock()) {
                    ModifierState.setShiftLock(false);
                    mTrime.setShifted(false);
                    //if(mKeyStyle.isVibrationEnabled()) {
                    //    performHapticFeedback(
                    //            HapticFeedbackConstants.KEYBOARD_TAP,
                    //            HapticFeedbackConstants.FLAG_IGNORE_GLOBAL_SETTING
                    //    );
                    //}
                } else {
                    if (mKey.getClick().getShiftLock().equals("double")) {
                        if (ModifierState.isShifted()) {
                            ModifierState.setShiftLock(true);
                        } else {
                            mTrime.setShifted(true);
                        }
                    } else {
                        boolean shifted = !ModifierState.isShifted();
                        if (mKey.isShiftLock())
                            ModifierState.setShiftLock(shifted);
                        mTrime.setShifted(shifted);
                    }
                }
                setSelected(ModifierState.isShifted());
                updateTextColor(mClick, ModifierState.isShiftLock(), mKeyStyle);
            } else {
                mTrime.onEvent(mKey.getEvent());
                if (ModifierState.isShifted() && !ModifierState.isShiftLock())
                    mTrime.setShifted(false);
            }
            return;
        }
        if (TextUtils.isEmpty(mClickText)) return;
        mTrime.onEvent(new Event(mClickText));
    }

    //@Override
    //public void setBackgroundColor(int color) {
    //    //super.setBackgroundColor(color);
    //    keyRoot.setBackground(createButtonBackground(color, 24f));
    //}

    /**
     * 设置背景颜色(带圆角)。
     *
     * @param color 背景颜色。
     * @param radius 圆角半径。
     */
    public void setBackgroundColor(int color, float radius) {
        //super.setBackgroundColor(color);
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
        boolean changed = mPressed != pressed;
        mPressed = pressed;
        super.setPressed(pressed);


        if (changed) {
            if (direction == SWIPE_NONE)
                showPreview(pressed, mClick.getText());
        }


        if (mKey != null && changed) {
            if (pressed) {
                setOnClickListener(this);
                if (mKey.getLongClick() != null || mKey.getEvent().isRepeatable() || (mKey.isShift())) {
                    postDelayed(mLongClickRunnable, mKeyStyle.getLongClickTime());
                }
            } else {
                removeCallbacks(mLongClickRunnable);
                removeCallbacks(mRepeatableRunnable);
            }
        }
        if (mSelected && !pressed) {
            mPressed = true;
            return;
        }
        // 触发重构后的轻量级动画
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

        // 1. 缩放与位移动画 (ViewPropertyAnimator 自动管理)
        float scaleX = isPressed ? mPressedStyle.getScaleX() : 1.0f;
        float scaleY = isPressed ? mPressedStyle.getScaleY() : 1.0f;
        float translationZ = isPressed ? mPressedStyle.getTranslationZ() : 0;
        float translationY = isPressed ? mPressedStyle.getTranslationY() : 0;
        float translationX = isPressed ? mPressedStyle.getTranslationX() : 0;

        keyRoot.animate()
                .scaleX(scaleX)
                .scaleY(scaleY)
                .translationZ(translationZ)
                .translationY(translationY)
                .translationX(translationX)
                .setDuration(50)
                .setInterpolator(FAST_OUT_SLOW_IN)
                .start();

        // 2. 背景过渡动画
        if (transition != null) {
            if (isPressed) {
                transition.startTransition(50);
            } else {
                transition.reverseTransition(50);
            }
        } else {
            Drawable bg = keyRoot.getBackground();
            if (bg instanceof GradientDrawable) {
                int dColor = isPressed ? mPressedStyle.getBackgroundColor(0xffaaaaaa)
                        : mKeyStyle.getBackgroundColor();
                ((GradientDrawable) bg).setColor(dColor);
            }
        }
        updateTextColor(mClick, isPressed, mKeyStyle);
        for (int i = 0; i < mHints.length; i++) {
            TextView hint = mHints[i];
            if (hint == null)
                continue;
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
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            int dShadowColor = mKeyStyle.getShadowColor();
            if (dShadowColor != 0) {
                if (isPressed) {
                    int pShadowColor = mPressedStyle.getShadowColor();
                    if (pShadowColor != 0) {
                        keyRoot.setOutlineAmbientShadowColor(pShadowColor);
                        keyRoot.setOutlineSpotShadowColor(pShadowColor);
                    }
                } else {
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
     * @param text 预览文本内容。
     */
    private void showPreview(boolean isPressed, CharSequence text) {
        if (mKey == null)
            return;
        if (keyPreview == null)
            return;
        if (!isPressed) {
            keyPreview.animate().cancel();
            keyPreview.setVisibility(GONE);
            return;
        }
        if (text != null) {
            keyPreview.setText(text);
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
        if (isPressed) {
            keyPreview.setAlpha(0);
            keyPreview.setScaleX(0.5f);
            keyPreview.setScaleY(0.5f);
            keyPreview.setTranslationY(0);
            keyPreview.setTranslationZ(1);
            keyPreview.setVisibility(VISIBLE);
        }
        KeyStyle previewStyle = mKeyStyle.getKeyStyle("preview", mKeyStyle);
        keyPreview.animate()
                .scaleX(previewStyle.getScaleX())
                .scaleY(previewStyle.getScaleY())
                .translationZ(1)
                .translationY(-getHeight() * previewStyle.getScaleY() * 1.01f)
                .translationX(0)
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
     * @param view TextView 视图。
     * @param isPressed 是否处于按下状态。
     * @param style 按键样式。
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
     * @param action 操作类型。
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
     * 当布局发生变化时调用。
     * 标记命中矩形失效,延迟到需要时再重新计算。
     *
     * @param changed 布局是否改变。
     * @param l 左边界。
     * @param t 上边界。
     * @param r 右边界。
     * @param b 下边界。
     */
    @Override
    protected void onLayout(boolean changed, int l, int t, int r, int b) {
        super.onLayout(changed, l, t, r, b);
        // 布局变动时，仅标记失效，不立即计算（因为父容器可能还在变）
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
        setVisibility(text != null ? VISIBLE : INVISIBLE);
        mClick.setText(mKeyStyle.getSpan(text));
        mClick.setSingleLine(false);
        mClick.setMaxLines(4);
        mClick.setEllipsize(TextUtils.TruncateAt.END);
        setContentDescription(text);
    }

    /**
     * 设置标签文本(单行显示)。
     *
     * @param text 标签文本。
     */
    public void setLabel(String text) {
        if (TextUtils.isEmpty(text)) return;
        setVisibility(VISIBLE);
        mClick.setText(mKeyStyle.getSpan(text));
        mClick.setSingleLine(false);
        setContentDescription(text);
    }

    /**
     * 设置点击文本。
     * 用于没有 Key 配置的按键,直接设置文本和点击事件。
     *
     * @param text 点击文本。
     */
    public void setClickText(String text) {
        mClickText = text;
        setVisibility(text != null ? VISIBLE : INVISIBLE);
        mClick.setText(mKeyStyle.getSpan(text));
        if (text != null)
            mClick.setSingleLine(!text.contains("/n"));
        setOnClickListener(this);
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
            LuaValue keyLongClickStyle = mKey.getMk().get("style_long_click");
            if (keyLongClickStyle.istable()) {
                // 按键级别配置存在，基于主题 long_click 样式创建新的样式对象
                mLongClickStyle = new KeyStyle(keyLongClickStyle, mKeyStyle.getLongClickKeyStyle());
            } else {
                // 使用主题全局的 key.long_click 配置
                mLongClickStyle = mKeyStyle.getLongClickKeyStyle();
            }
            
            mHintStyles[HINT_LONG] = mLongClickStyle;
            if (!mLongClickStyle.isShow())
                return;
            mLongClick = new TightTextView(getContext());
            //mLongClick.setBackgroundColor(0xff0000ff);
            mHints[HINT_LONG] = mLongClick;

            mLongClick.setIncludeFontPadding(true);
            mLongClick.setSingleLine(true);
            mLongClick.setVisibility(View.VISIBLE);
            mLongClick.setTextSize(TypedValue.COMPLEX_UNIT_DIP, mLongClickStyle.getTextSize(8));
            //if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            //    mLongClick.setLineHeight((int) mLongClickStyle.getTextSize(12));
            //}
            mLongClick.setTextColor(mLongClickStyle.getTextColor());
            mLongClick.setTypeface(mLongClickStyle.getFont());
            mLongClick.setTranslationX(mLongClickStyle.getSize("offset_x", 0));
            mLongClick.setTranslationY(mLongClickStyle.getSize("offset_y", 0));
            keyRoot.addView(mLongClick, new FrameLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT, mLongClickStyle.getGravity(Gravity.TOP | Gravity.CENTER)));
            mLongClick.setGravity(mLongClickStyle.getGravity(Gravity.TOP | Gravity.CENTER));
            //Paint paint = mLongClick.getPaint();
            //Rect bounds = new Rect();
            //paint.getTextBounds(text, 0, text.length(), bounds);
            //int actualHeight = bounds.height(); // 符号真实的像素高度
            //int actualWidth = bounds.width();
            //mLongClick.setFirstBaselineToTopHeight(actualHeight);
        }

        mLongClick.setVisibility(text != null ? VISIBLE : GONE);
        mLongClick.setText(mKeyStyle.getLongClickKeyStyle().getSpan(text));
        //mLongClick.postInvalidate();
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
            
            mHintStyles[HINT] = mHintStyle;
            if (!mHintStyle.isShow())
                return;
            if (!mHintStyle.isShow())
                return;
            mHint = new TightTextView(getContext());
            mHints[HINT] = mHint;
            mHint.setIncludeFontPadding(true);
            mHint.setSingleLine(true);
            mHint.setVisibility(View.VISIBLE);
            mHint.setTextSize(TypedValue.COMPLEX_UNIT_DIP, mHintStyle.getTextSize(8));
            mHint.setTextColor(mHintStyle.getTextColor());
            mHint.setTypeface(mHintStyle.getFont());
            mHint.setTranslationX(mHintStyle.getSize("offset_x", 0));
            mHint.setTranslationY(mHintStyle.getSize("offset_y", 0));
            keyRoot.addView(mHint, new FrameLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT, mHintStyle.getGravity(Gravity.BOTTOM | Gravity.CENTER)));
            mHint.setGravity(mHintStyle.getGravity(Gravity.BOTTOM | Gravity.CENTER));
        }

        mHint.setVisibility(text != null ? VISIBLE : GONE);
        mHint.setText(mKeyStyle.getHintKeyStyle().getSpan(text));
        //mHint.postInvalidate();
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
     * @param left 左边距。
     * @param top 上边距。
     * @param right 右边距。
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
            if (mAsciiKey != null) {
                mKey = Rime.isAsciiMode() ? mAsciiKey : mDefKey;
                initKey();
            } else {
                initKey();
                //String click = mKey.getLabel();
                //if (!TextUtils.isEmpty(click)) {
                //    setClickText(click);
                //    setContentDescription(mKey.getDescription());
                //}
                //String longClick = mKey.getSymbolLabel();
                //if (!TextUtils.isEmpty(longClick)) setLongClickText(longClick);
            }

            if (mKey.isShift()) {
                setSelected(ModifierState.isShifted());
            }
            
            // 更新 q-p 键位的动态长按助记显示
            updateDynamicLongClickHint();
        }
        if(Config.is_hide_key_hint()==_hide_key_hint)
            return;
        _hide_key_hint=Config.is_hide_key_hint();
        if(Config.is_hide_key_hint()){
            for (TextView hint : mHints) {
                if(hint!=null)
                    hint.setVisibility(GONE);
            }
        }else {
            for (TextView hint : mHints) {
                if(hint!=null)
                    hint.setVisibility(VISIBLE);
            }
        }
    }

    /**
     * 更新 q-p 键位的动态长按助记显示。
     * 根据当前 Shift 状态和 ASCII 模式，动态更新长按助记文本。
     */
    private void updateDynamicLongClickHint() {
        if (mKey == null || mLongClick == null) return;
        
        String label = mKey.getLabel();
        if (label == null || label.length() != 1) return;
        
        char ch = label.charAt(0);
        // 修正：支持小写(a-z)和大写(A-Z)字母键位
        // Shift 状态下 label 会变成大写，所以需要同时检查两种情况
        boolean isLetter = (ch >= 'a' && ch <= 'z') || (ch >= 'A' && ch <= 'Z');
        if (!isLetter) return;
        
        boolean isShifted = ModifierState.isShifted();
        boolean isAsciiMode = Rime.isAsciiMode();
        String dynamicLabel = mKey.getDynamicLongClickLabel(isShifted, isAsciiMode);
        
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
        //setBackgroundColor(0);
        setClipChildren(false);
        setClipToPadding(false);
        setClickable(true);
        setFocusable(true);
        //if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        //    setScreenReaderFocusable(true); // API 28+
        //    setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_YES);
        //}
        keyRoot = new FrameLayout(getContext());
        keyRoot.setClipChildren(false);
        keyRoot.setClipToPadding(false);

        // 阴影颜色 (P+)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            int dShadowColor = mKeyStyle.getShadowColor();
            if (dShadowColor != 0) {
                keyRoot.setOutlineAmbientShadowColor(dShadowColor);
                keyRoot.setOutlineSpotShadowColor(dShadowColor);
            }
        }
        // 背景
        //Drawable background = mKeyStyle.getBackground();
        //if (background instanceof GradientDrawable) {
        //    if (!mPressedStyle.hasKey("background"))
        //        keyRoot.setBackground(createButtonBackground(background));
        //    else
        //        keyRoot.setBackground(background);
        //    transition = null;
        //} else {
        Drawable[] layers = new Drawable[2];
        layers[0] = mKeyStyle.getBackground();
        layers[1] = mPressedStyle.getBackground();
        if (layers[0] instanceof LuaBitmapDrawable) {
            if (layers[1] instanceof GradientDrawable) {
                /*int targetColor = mPressedStyle.getBackgroundColor(); // 你的目标颜色（例如 Trime 主题色）
                float r = Color.red(targetColor) / 255f;
                float g = Color.green(targetColor) / 255f;
                float b = Color.blue(targetColor) / 255f;
                float a = Color.alpha(targetColor) / 255f;

                // 灰度转换系数（标准生理亮度公式）
                float lr = 0.213f;
                float lg = 0.715f;
                float lb = 0.072f;

                ColorMatrix cm = new ColorMatrix(new float[] {
                        lr * r, lg * r, lb * r, 0, 0,  // 新的 R = (原R*lr + 原G*lg + 原B*lb) * 目标R
                        lr * g, lg * g, lb * g, 0, 0,  // 新s G = (原R*lr + 原G*lg + 原B*lb) * 目标G
                        lr * b, lg * b, lb * b, 0, 0,  // 新的 B = (原R*lr + 原G*lg + 原B*lb) * 目标B
                        0,      0,      0,      a, 0   // 保持原图透明度并乘以目标Alpha
                });

                ColorFilter filter = new ColorMatrixColorFilter(cm);*/
                PorterDuffColorFilter colorFilter = new PorterDuffColorFilter(mPressedStyle.getBackgroundColor(), PorterDuff.Mode.MULTIPLY);
                LuaBitmapDrawable bg = ((LuaBitmapDrawable) mKeyStyle.getBackground());
                bg.setColorFilter(colorFilter);
                layers[1] = bg;
            }
        }
        transition = new TransitionDrawable(layers);
        keyRoot.setBackground(transition);
        //}

        // 初始化 Click TextView
        mClick = new TextView(getContext());
        mClick.setIncludeFontPadding(false);
        mClick.setSingleLine(true);
        mClick.setTextSize(TypedValue.COMPLEX_UNIT_DIP, mKeyStyle.getTextSize());
        mClick.setTextColor(mKeyStyle.getTextColor());
        mClick.setTypeface(mKeyStyle.getFont());

        setVisibility(View.INVISIBLE);

        // 布局添加
        keyRoot.addView(mClick, new FrameLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT, mKeyStyle.getGravity(Gravity.CENTER)));

        mClick.setTranslationX(mKeyStyle.getSize("offset_x", 0));
        mClick.setTranslationY(mKeyStyle.getSize("offset_y", 0));


        //createAnimator(keyRoot);

        int elevation = mKeyStyle.getElevation();
        keyRoot.setElevation(elevation);

        Style margins = mKeyStyle.getStyle("margins");
        LayoutParams params = new LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
        params.setMargins(margins.getSize("left", elevation / 3), margins.getSize("top", elevation / 3), margins.getSize("right", elevation / 3), margins.getSize("bottom", elevation / 3 * 2));
        Style padding = mKeyStyle.getStyle("padding");
        keyRoot.setPadding(padding.getSize("left", 0), padding.getSize("top", 0), padding.getSize("right", 0), padding.getSize("bottom", 0));
        addView(keyRoot, params);
        if (mKeyStyle.hasKey("preview")) {
            KeyStyle previewStyle = mKeyStyle.getKeyStyle("preview", mKeyStyle);
            keyPreview = new TextView(getContext());
            keyPreview.setIncludeFontPadding(true);

            // 关键修复 2：如果背景是自定义绘制的，强制设置轮廓提供者以产生阴影
            keyPreview.setOutlineProvider(ViewOutlineProvider.BACKGROUND);
            keyPreview.setClipToOutline(false); // 允许阴影溢出边界绘制
            keyPreview.setBackground(previewStyle.getBackground());
            keyPreview.setVisibility(GONE);
            keyPreview.setGravity(Gravity.CENTER);
            keyPreview.setElevation(previewStyle.getElevation());
            keyPreview.setTextSize(TypedValue.COMPLEX_UNIT_DIP, previewStyle.getTextSize());
            keyPreview.setTextColor(previewStyle.getTextColor());
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                int dShadowColor = previewStyle.getShadowColor();
                if (dShadowColor != 0) {
                    keyPreview.setOutlineAmbientShadowColor(dShadowColor);
                    keyPreview.setOutlineSpotShadowColor(dShadowColor);
                }
            }
            addView(keyPreview, new LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        }
        keyRoot.setOutlineProvider(ViewOutlineProvider.BACKGROUND);
        keyRoot.setClipToOutline(false); // 允许阴影溢出边界绘制
    }

    /**
     * 初始化按键内容。
     * 设置主文本、长按文本、提示文本和滑动提示文本。
     */
    private void initKey() {
        if (mKey == null) return;
        String click = mKey.getLabel();
        if (!TextUtils.isEmpty(click)) setClickText(click);

        String longClick = mKey.getLongClickLabel();
        if (!TextUtils.isEmpty(longClick)) setLongClickText(longClick);

        String hint = mKey.getHint();
        if (!TextUtils.isEmpty(hint)) setHintText(hint);
        setContentDescription(mKey.getDescription());
        KeyStyle mHintKeyStyle = mKeyStyle.getHintKeyStyle();

        String ev = mKey.getHint(SWIPE_UP);
        if (ev != null) {
            // 优先使用按键级别的 hint_up 配置，继承主题全局的 hint.up 配置
            KeyStyle ht;
            LuaValue keyHintUp = mKey.getMk().get("hint_up");
            if (keyHintUp.istable()) {
                // 按键级别配置存在，先获取主题的 hint.up 作为默认值
                KeyStyle defaultUpStyle = mHintKeyStyle.hasKey("up") 
                    ? mHintKeyStyle.getKeyStyle("up", mHintKeyStyle)
                    : mHintKeyStyle;
                // 基于主题的 hint.up 样式创建新的样式对象（实现继承覆盖）
                ht = new KeyStyle(keyHintUp, defaultUpStyle);
            } else if (mHintKeyStyle.hasKey("up")) {
                // 没有按键级别配置，使用主题全局的 hint.up 配置
                ht = mHintKeyStyle.getKeyStyle("up", mHintKeyStyle);
            } else {
                ht = mHintKeyStyle;
            }
            mHintStyles[SWIPE_UP] = ht;
            if (mHints[SWIPE_UP] != null)
                mHints[SWIPE_UP].setText(ht.getSpan(ev));
            else
                mHints[SWIPE_UP] = addHint(ev, Gravity.TOP, ht);
        }
        ev = mKey.getHint(SWIPE_DOWN);
        if (ev != null) {
            // 优先使用按键级别的 hint_down 配置，继承主题全局的 hint.down 配置
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
            if (mHints[SWIPE_DOWN] != null)
                mHints[SWIPE_DOWN].setText(ht.getSpan(ev));
            else
                mHints[SWIPE_DOWN] = addHint(ev, Gravity.BOTTOM, ht);
        }
        ev = mKey.getHint(SWIPE_LEFT);
        if (ev != null) {
            // 优先使用按键级别的 hint_left 配置，继承主题全局的 hint.left 配置
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
            if (mHints[SWIPE_LEFT] != null)
                mHints[SWIPE_LEFT].setText(ht.getSpan(ev));
            else
                mHints[SWIPE_LEFT] = addHint(ev, Gravity.LEFT, ht);
        }
        ev = mKey.getHint(SWIPE_RIGHT);
        if (ev != null) {
            // 优先使用按键级别的 hint_right 配置，继承主题全局的 hint.right 配置
            KeyStyle ht;
            LuaValue keyHintRight = mKey.getMk().get("hint_right");
            if (keyHintRight.istable()) {
                KeyStyle defaultRightStyle = mHintKeyStyle.hasKey("right") 
                    ? mHintKeyStyle.getKeyStyle("right", mHintKeyStyle)
                    : mHintKeyStyle;
                ht = new KeyStyle(keyHintRight, defaultRightStyle);
            } else if (mHintKeyStyle.hasKey("right")) {
                ht = mHintKeyStyle.getKeyStyle("right", mHintKeyStyle);
            } else {
                ht = mHintKeyStyle;
            }
            mHintStyles[SWIPE_RIGHT] = ht;
            if (mHints[SWIPE_RIGHT] != null)
                mHints[SWIPE_RIGHT].setText(ht.getSpan(ev));
            else
                mHints[SWIPE_RIGHT] = addHint(ev, Gravity.RIGHT, ht);
        }
    }

    /**
     * 添加提示文本。
     * 创建指定方向的提示 TextView,并设置样式和位置。
     *
     * @param label 提示文本。
     * @param g 对齐方式(Gravity)。
     * @param mHintStyle 提示样式。
     * @return 创建的 TextView,如果不需要显示则返回 null。
     */
    private TextView addHint(String label, int g, KeyStyle mHintStyle) {
        if (!mHintStyle.isShow())
            return null;
        TextView hint = (g == Gravity.TOP || g == Gravity.BOTTOM) ? new TightTextView(getContext()) : new TextView(getContext());
        hint.setIncludeFontPadding(true);
        hint.setText(mHintStyle.getSpan(label));
        hint.setSingleLine(true);
        hint.setVisibility(View.VISIBLE);
        hint.setTextSize(TypedValue.COMPLEX_UNIT_DIP, mHintStyle.getTextSize(8));
        hint.setTextColor(mHintStyle.getTextColor());
        hint.setTypeface(mHintStyle.getFont());
        keyRoot.addView(hint, new FrameLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT, g | Gravity.CENTER));
        hint.setTypeface(mHintStyle.getFont());
        hint.setTranslationX(mHintStyle.getSize("offset_x", 0));
        hint.setTranslationY(mHintStyle.getSize("offset_y", 0));
        if (mLongClick != null && hint.getGravity() == mLongClick.getGravity())
            mLongClick.setVisibility(GONE);
        if (mHint != null && hint.getGravity() == mHint.getGravity())
            mHint.setVisibility(GONE);
        return hint;
    }

    /**
     * 创建按钮背景(带圆角和波纹效果)。
     *
     * @param color 背景颜色。
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
            if (mKeyStyle.getLongClickKeyStyle().isSoundEnabled()) {
                // 获取自定义音效ID
                int sound = mKeyStyle.getLongClickKeyStyle().getSoundEffect();
                if (sound > 0) {
                    // 播放自定义音效
                    ThemeManager.play(sound);
                } else {
                    // 播放系统默认点击音效
                    playSoundEffect(SoundEffectConstants.CLICK);
                }
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
;       int[] point = new int[2];
        getLocationOnScreen(point);
        int x=point[0];
        int width = getWidth();
        addView(popupKeyboard, new FrameLayout.LayoutParams(popupKeyboard.getRawWidth(), popupKeyboard.getRawHeight(), Gravity.TOP | Gravity.LEFT));
        int dx = x + width / 2 - popupKeyboard.getRawWidth() / 2;
        TrimeService trime = TrimeService.getInstance();
        if (dx < 0)
            dx = 0;
        else if (dx + popupKeyboard.getRawWidth() > trime.getWidth())
            dx = trime.getWidth() - popupKeyboard.getRawWidth();
        popupKeyboard.setTranslationX(dx-x);
        popupKeyboard.setTranslationY(-popupKeyboard.getRawHeight());
        popupKeyboard.setOffsetX(x - dx);
    }

    /**
     * 重复按键 Runnable。
     * 在长按后持续触发点击事件,实现快速输入。
     */
    private final Runnable mRepeatableRunnable = new Runnable() {
        @Override
        public void run() {
            if (!isPressed()) return;
            if (mKeyStyle.getLongClickKeyStyle().isVibrationEnabled()) {
                VibrationEffect ve = mKeyStyle.getLongClickKeyStyle().getVibrationEffect();
                if (ve != null) {
                    ThemeManager.vibrate(ve);
                } else {
                    boolean ret = performHapticFeedback(
                            HapticFeedbackConstants.LONG_PRESS,
                            HapticFeedbackConstants.FLAG_IGNORE_GLOBAL_SETTING
                    );
                }
            }
            if (mKeyStyle.getLongClickKeyStyle().isSoundEnabled()) {
                int sound = mKeyStyle.getLongClickKeyStyle().getSoundEffect();
                if (sound > 0) {
                    ThemeManager.play(sound);
                } else {
                    playSoundEffect(SoundEffectConstants.CLICK);
                }
            }
            //performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP, HapticFeedbackConstants.FLAG_IGNORE_GLOBAL_SETTING);
            onClick(KeyView.this);
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
            removeCallbacks(this);
            if (mKeyStyle.getLongClickKeyStyle().isVibrationEnabled()) {
                VibrationEffect ve = mKeyStyle.getLongClickKeyStyle().getVibrationEffect();
                if (ve != null) {
                    ThemeManager.vibrate(ve);
                } else {
                    boolean ret = performHapticFeedback(
                            HapticFeedbackConstants.LONG_PRESS,
                            HapticFeedbackConstants.FLAG_IGNORE_GLOBAL_SETTING
                    );
                }
            }
            if (mKeyStyle.getLongClickKeyStyle().isSoundEnabled()) {
                int sound = mKeyStyle.getLongClickKeyStyle().getSoundEffect();
                if (sound > 0) {
                    ThemeManager.play(sound);
                } else {
                    playSoundEffect(SoundEffectConstants.CLICK);
                }
            }
            //performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP, HapticFeedbackConstants.FLAG_IGNORE_GLOBAL_SETTING);
            if (direction != SWIPE_NONE) {
                Event ev = mKey.getEvent(direction);
                if (ev != null) mTrime.onEvent(ev);
            }
            postDelayed(this, mKeyStyle.getRepeatClickTime());
        }
    };

    /**
     * 设置点击文本内边距。
     *
     * @param left 左边距。
     * @param top 上边距。
     * @param right 右边距。
     * @param bottom 下边距。
     */
    public void setClickPadding(int left, int top, int right, int bottom) {
        mClick.setPadding(left, top, right, bottom);
    }


    private Bitmap maskBitmap;
    private int lastWidth, lastHeight;
    /** 是否启用异形形状触摸检测(默认关闭,提升性能) */
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

        if (event.getAction() == MotionEvent.ACTION_DOWN) {
            if (mKeyStyle.isVibrationEnabled()) {
                VibrationEffect ve = mKeyStyle.getVibrationEffect();
                if (ve != null) {
                    ThemeManager.vibrate(ve);
                } else {
                    boolean ret = performHapticFeedback(
                            HapticFeedbackConstants.KEYBOARD_TAP,
                            HapticFeedbackConstants.FLAG_IGNORE_GLOBAL_SETTING
                    );
                }
            }
            if (mKeyStyle.isSoundEnabled()) {
                int sound = mKeyStyle.getSoundEffect();
                if (sound > 0) {
                    ThemeManager.play(sound);
                } else {
                    playSoundEffect(SoundEffectConstants.CLICK);
                }
            }
        }

        // A. 异形按键检测 (ACTION_DOWN)
        if (isShapeDetectionEnabled && event.getAction() == MotionEvent.ACTION_DOWN) {
            if (isPixelTransparent(event)) return false;
        }
        if (popupKeyboard != null) {
            if (event.getActionMasked() == MotionEvent.ACTION_MOVE) {
                popupKeyboard.dispatchTouchEvent(event);
                return true;
            }

            if (event.getAction() == MotionEvent.ACTION_UP || event.getAction() == MotionEvent.ACTION_CANCEL) {
                popupKeyboard.dispatchTouchEvent(event);
                removeView(popupKeyboard);
                popupKeyboard = null;
                return true;
            }
        }
        if (mKey == null || !mKey.hasSwipeEvent())
            return super.onTouchEvent(event);

        if (event.getAction() == MotionEvent.ACTION_DOWN) {
            // 请求父容器不要拦截后续的 MOVE 事件，哪怕我滑出了边界
            ViewParent parent = getParent();
            if (parent != null) {
                parent.requestDisallowInterceptTouchEvent(true);
            }
        }
        // B. 处理滑动逻辑
        if (event.getAction() == MotionEvent.ACTION_MOVE) {
            handleSwipeEvent(event);
        } else if (event.getAction() == MotionEvent.ACTION_UP || event.getAction() == MotionEvent.ACTION_CANCEL) {
            // C. 处理滑动后的上屏逻辑
            if (direction != SWIPE_NONE) {
                if (mKey.isSwipeRepeatable()) {
                    removeCallbacks(mSwipRepeatableRunnable);
                } else {
                    Event ev = mKey.getEvent(direction);
                    if (ev != null) mTrime.onEvent(ev);
                }
                // 重要：重置状态
                showPreview(false, null);
                direction = SWIPE_NONE;
                lastDirection = SWIPE_NONE;
                setPressed(false); // 手指是在外部抬起的，取消按键按下状态
                return true; // 拦截事件，防止 super 触发 onClick
            }
            // 重置状态
            lastDirection = SWIPE_NONE;
            showPreview(false, null);
            mTrime.onUp(0);
        }
        return super.onTouchEvent(event);
    }

    // 1. 定义方向常量
    /** 无滑动 */
    public static final int SWIPE_NONE = 0;
    /** 提示文本索引 */
    public static final int HINT = KeyEventType.CLICK.ordinal();
    /** 长按文本索引 */
    public static final int HINT_LONG = KeyEventType.LONG_CLICK.ordinal();
    /** 上滑索引 */
    public static final int SWIPE_UP = KeyEventType.SWIPE_UP.ordinal();
    /** 下滑索引 */
    public static final int SWIPE_DOWN = KeyEventType.SWIPE_DOWN.ordinal();
    /** 左滑索引 */
    public static final int SWIPE_LEFT = KeyEventType.SWIPE_LEFT.ordinal();
    /** 右滑索引 */
    public static final int SWIPE_RIGHT = KeyEventType.SWIPE_RIGHT.ordinal();
    /** 当前滑动方向 */
    private int direction = 0;
    /** 上一次滑动方向 */
    private int lastDirection = 0;
    /** 8个方向的提示样式数组 */
    private final KeyStyle[] mHintStyles = new KeyStyle[8];

    /**
     * 处理滑动事件。
     * 根据手指位置判断滑动方向,并触发对应的事件。
     *
     * @param event 触摸事件。
     */
    private void handleSwipeEvent(MotionEvent event) {
        float x = event.getX();
        float y = event.getY();
        float w = getWidth();
        float h = getHeight();

        // 1. 使用偏移量绝对值判断方向，解决“斜向滑动”的判定冲突
        float dx = (x < 0) ? -x : (x > w ? x - w : 0);
        float dy = (y < 0) ? -y : (y > h ? y - h : 0);

        if (dx == 0 && dy == 0) {
            direction = SWIPE_NONE;
        } else if (dx > dy) {
            direction = (x < 0) ? SWIPE_LEFT : SWIPE_RIGHT;
        } else {
            direction = (y < 0) ? SWIPE_UP : SWIPE_DOWN;
        }

        if (direction != lastDirection) {
            if (direction != SWIPE_NONE) {
                // 只要进入滑动状态，就移除长按和重复按键的回调，防止误触发
                removeCallbacks(mLongClickRunnable);
                removeCallbacks(mRepeatableRunnable);

                Event ev = mKey.getEvent(direction);
                if (ev != null) {
                    showPreview(true, mHintStyles[direction] != null ? mHintStyles[direction].getSpan(ev.getLabel()) : ev.getLabel());
                    if (mKey.isSwipeRepeatable())
                        postDelayed(mSwipRepeatableRunnable, mKeyStyle.getRepeatClickTime());
                } else {
                    showPreview(false, null);
                    removeCallbacks(mSwipRepeatableRunnable);
                }
            } else {
                // 回到按键中心，恢复普通预览
                showPreview(true, mKeyStyle.getPressedStyle().getSpan(mKey.getLabel()));
                removeCallbacks(mSwipRepeatableRunnable);
            }
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
        int x = (int) event.getX() - keyRoot.getLeft();
        int y = (int) event.getY() - keyRoot.getTop();
        int w = keyRoot.getWidth();
        int h = keyRoot.getHeight();

        // 1. 边界防御：如果触摸点在矩形外，或者 View 还没加载完，视为透明
        if (x < 0 || x >= w || y < 0 || y >= h || w <= 0 || h <= 0) {
            return true;
        }

        // 2. 只有尺寸变化时才重新绘制 Mask，节省性能
        if (maskBitmap == null || w != lastWidth || h != lastHeight) {
            // 释放旧资源
            if (maskBitmap != null) maskBitmap.recycle();

            lastWidth = w;
            lastHeight = h;

            // ALPHA_8 格式最省内存，每个像素仅占 1 字节
            maskBitmap = Bitmap.createBitmap(w, h, Bitmap.Config.ALPHA_8);
            Canvas maskCanvas = new Canvas(maskBitmap);

            Drawable bg = keyRoot.getBackground();
            if (bg instanceof TransitionDrawable) {
                Drawable tempBg = ((TransitionDrawable) bg).getDrawable(0).mutate();
                tempBg.setBounds(0, 0, w, h);
                tempBg.draw(maskCanvas);
            } else if (bg != null) {
                // 复制一份背景，防止修改原背景的 Bounds
                Drawable tempBg = bg.mutate();
                tempBg.setBounds(0, 0, w, h);
                tempBg.draw(maskCanvas);
            }
        }

        // 3. 检测像素。对于 ALPHA_8，getPixel 返回的是位移后的 Alpha 值
        // 允许一点点微弱的阴影/羽化边缘（阈值设为 10）
        return (maskBitmap.getPixel(x, y) >> 24 & 0xff) < 0x40;
    }

    /**
     * 设置文本大小。
     *
     * @param i 单位类型。
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
     * @param left 左边距。
     * @param top 上边距。
     * @param right 右边距。
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
