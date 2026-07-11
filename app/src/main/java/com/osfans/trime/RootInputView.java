/*
 * SPDX-FileCopyrightText: 2015 - 2026 Rime community
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package com.osfans.trime;

import android.annotation.SuppressLint;
import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.text.TextUtils;
import android.util.Log;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowInsets;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;

import com.osfans.trime.candidate.CandidateView;
import com.osfans.trime.candidate.CandidatesManager;
import com.osfans.trime.candidate.ExpandedCandidateView;
import com.osfans.trime.core.CandidateItem;
import com.osfans.trime.core.Rime;
import com.osfans.trime.core.RimeProto;
import com.osfans.trime.enums.InlineModeType;
import com.osfans.trime.keyboard.ClipboardKeyboardView;
import com.osfans.trime.keyboard.FloatKeyboard;
import com.osfans.trime.keyboard.KeyView;
import com.osfans.trime.keyboard.SymbolsKeyboardView;
import com.osfans.trime.theme.Style;
import com.osfans.trime.theme.ThemeManager;
import com.osfans.trime.util.Function;

import java.util.ArrayList;

/**
 * 根输入视图类，管理输入法的所有 UI 组件。
 * 包含键盘、候选词栏、编码区、工具栏等核心界面元素。
 * 支持小屏模式、浮动模式等特殊显示方式。
 */
public class RootInputView extends FrameLayout {

    // ==================== 成员变量 - UI 根布局与框架 ====================
    // 根布局容器（水平方向）
    private LinearLayout mRoot;
    // 输入视图的根布局
    private LinearLayout mInputViewRoot;
    // 左侧布局（小屏模式用）
    private FrameLayout mLeftLayout;
    // 右侧布局（小屏模式用）
    private FrameLayout mRightLayout;
    // 左侧按钮（切换位置）
    private KeyView mLeftButton;
    // 右侧按钮（切换位置）
    private KeyView mRightButton;

    // ==================== 成员变量 - 核心组件视图 ====================
    // 候选词视图（普通模式）
    private CandidateView mCandidateView;
    // 键盘输入视图
    private InputView mInputView;
    // 扩展候选词视图（全屏模式）
    private ExpandedCandidateView mExpandedCandidateView;
    // 符号键盘视图
    private SymbolsKeyboardView mSymbolsKeyboardView;
    // 编码区视图（显示预编辑文本）
    private Composition mPreedit;
    // 云输入结果显示区
    private TextView mCloud;
    // 中心布局容器
    private FrameLayout mCenterLayout;
    // 自定义视图（如 Lua 脚本创建的视图）
    private View mCustomView;
    // 是否显示提取的候选词视图
    private boolean mShowExtractedCandidatesView;
    // 主线程 Handler
    private final Handler mHandler = new Handler(Looper.getMainLooper());
    // 剪贴板键盘视图
    private ClipboardKeyboardView mClipboardKeyboardView;
    // 候选词起始索引（用于分页）
    private int mStartIdx = 0;
    // 编码区最小长度过滤条件
    private int mCompositionMinLength;

    /**
     * 构造函数，创建根输入视图并初始化所有子组件。
     *
     * @param context Android 上下文环境。
     */
    public RootInputView(@NonNull Context context) {
        super(context);
        initView(context);
        //setFitsSystemWindows(false);
    }

    /**
     * 测量视图尺寸时的异常处理。
     * 如果测量失败，打印堆栈跟踪信息。
     *
     * @param widthMeasureSpec 宽度测量规范。
     * @param heightMeasureSpec 高度测量规范。
     */
    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        try {
            super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        } catch (Exception e) {
            e.printStackTrace();
            //setMeasuredDimension(TrimeService.getInstance().getWidth(),TrimeService.getInstance().getHeight());
        }
    }

    /**
     * 初始化根输入视图的所有子组件。
     * 创建键盘、候选词栏、编码区、工具栏等 UI 元素，并设置布局和事件监听器。
     * 支持小屏模式和浮动模式的特殊布局。
     *
     * @param context Android 上下文环境。
     */
    @SuppressLint({"ClickableViewAccessibility", "AppCompatCustomView"})
    private void initView(@NonNull Context context) {
        // 禁用子视图裁剪，允许溢出显示（用于弹出窗口）
        setClipChildren(false);
        setClipToPadding(false);
        // 从主题配置中读取是否有编码区配置和最小长度过滤条件
        Style compositionStyle = ThemeManager.getStyle().getStyle("composition");
        mHasComposition = ThemeManager.getStyle().hasKey("composition");
        mHideComposition = "hide".equals(compositionStyle.getString("position"));
        mCompositionMinLength = compositionStyle.getInt("min_length");

        // 初始化各个视图引用为空
        mShowExtractedCandidatesView = false;
        mExpandedCandidateView = null;
        mClipboardKeyboardView = null;
        mCustomView = null;
        mSymbolsKeyboardView = null;
        TrimeService trime = TrimeService.getInstance();

        // 创建根布局容器（水平方向）
        mRoot = new LinearLayout(context);

        mRoot.setClipChildren(false);
        mRoot.setClipToPadding(false);
        mRoot.setOrientation(LinearLayout.HORIZONTAL);
        // 如果是小屏模式，创建左右侧布局用于切换位置
        if (Config.isSmallMode()) {
            mLeftLayout = new FrameLayout(context);
            mLeftLayout.setVisibility(View.GONE);
            mRightLayout = new FrameLayout(context);
            mRightLayout.setVisibility(View.GONE);

            // 创建左侧按钮（◀），点击后显示在左侧
            mLeftButton = new KeyView(context, ThemeManager.getStyle().getStyle("toolbar").getKeyStyle());
            mLeftButton.setText("◀");
            mLeftButton.setContentDescription("显示在左侧");
            mLeftLayout.addView(mLeftButton, new FrameLayout.LayoutParams(ThemeManager.getCandidateHeight(), ThemeManager.getCandidateHeight(), Gravity.CENTER | Gravity.RIGHT));
            mLeftButton.setOnClickListener(new OnClickListener() {
                @Override
                public void onClick(View v) {
                    mLeftLayout.setVisibility(GONE);
                    mRightLayout.setVisibility(VISIBLE);
                    Config.setSmallModeGravity(Gravity.LEFT);
                }
            });
            mLeftButton.setOnLongClickListener(new OnLongClickListener() {
                @Override
                public boolean onLongClick(View v) {
                    setSelected(true);
                    return true;
                }
            });
            // 左侧按钮触摸事件：长按拖动调整宽度
            mLeftButton.setOnTouchListener(new OnTouchListener() {
                private float mWidth;
                private int mLastW;
                private float mLastX;

                @Override
                public boolean onTouch(View v, MotionEvent event) {
                    switch (event.getActionMasked()) {
                        case MotionEvent.ACTION_DOWN:
                            mLastX = event.getRawX();
                            mLastW = mCenterLayout.getWidth();
                            break;
                        case MotionEvent.ACTION_MOVE:
                            if (isSelected()) {
                                float x = event.getRawX();
                                mWidth = (mLastW - (x - mLastX));
                                mCenterLayout.setScaleX(mWidth / mLastW);
                                mCenterLayout.setTranslationX(-(mWidth - mLastW) / 2);
                                mLeftButton.setTranslationX(-(mWidth - mLastW));
                            }
                            break;
                        case MotionEvent.ACTION_UP:
                        case MotionEvent.ACTION_CANCEL:
                            if (isSelected()) {
                                Config.setSmallModeWidth((int) mWidth);
                                setSelected(false);
                                setTheme(Config.getTheme());
                            }
                            break;
                    }
                    return false;
                }
            });

            // 创建右侧按钮（▶），点击后显示在右侧
            mRightButton = new KeyView(context, ThemeManager.getStyle().getStyle("toolbar").getKeyStyle());
            mRightButton.setText("▶");
            mRightButton.setContentDescription("显示在右侧");
            mRightLayout.addView(mRightButton, new FrameLayout.LayoutParams(ThemeManager.getCandidateHeight(), ThemeManager.getCandidateHeight(), Gravity.CENTER | Gravity.LEFT));
            mRightButton.setOnClickListener(new OnClickListener() {
                @Override
                public void onClick(View v) {
                    mRightLayout.setVisibility(GONE);
                    mLeftLayout.setVisibility(VISIBLE);
                    Config.setSmallModeGravity(Gravity.RIGHT);
                }
            });

            mRightButton.setOnLongClickListener(new OnLongClickListener() {
                @Override
                public boolean onLongClick(View v) {
                    setSelected(true);
                    return true;
                }
            });
            // 右侧按钮触摸事件：长按拖动调整宽度
            mRightButton.setOnTouchListener(new OnTouchListener() {
                private float mWidth;
                private int mLastW;
                private float mLastX;

                @Override
                public boolean onTouch(View v, MotionEvent event) {
                    switch (event.getActionMasked()) {
                        case MotionEvent.ACTION_DOWN:
                            mLastX = event.getRawX();
                            mLastW = mCenterLayout.getWidth();
                            break;
                        case MotionEvent.ACTION_MOVE:
                            if (isSelected()) {
                                float x = event.getRawX();
                                mWidth = (mLastW + (x - mLastX));
                                mCenterLayout.setScaleX(mWidth / mLastW);
                                mCenterLayout.setTranslationX((mWidth - mLastW) / 2);
                                mRightButton.setTranslationX((mWidth - mLastW));
                            }
                            break;
                        case MotionEvent.ACTION_UP:
                        case MotionEvent.ACTION_CANCEL:
                            if (isSelected()) {
                                Config.setSmallModeWidth((int) mWidth);
                                setSelected(false);
                                setTheme(Config.getTheme());
                            }
                            break;
                    }
                    return false;
                }
            });
        }


        // 先创建 InputView,触发键盘视图创建和高度计算,使 ThemeManager 获得动态高度
        mInputView = new InputView(context);

        // 创建中心布局容器，用于放置键盘和候选词栏
        mCenterLayout = new FrameLayout(context);
        mCenterLayout.setClipChildren(false);
        mCenterLayout.setClipToPadding(false);
        // 根据小屏模式添加左右侧布局和中心布局到根布局
        if (Config.isSmallMode())
            mRoot.addView(mLeftLayout, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ThemeManager.getContentHeight(), 1));
        mRoot.addView(mCenterLayout, new LinearLayout.LayoutParams(trime.getWidth(), ThemeManager.getContentHeight()));
        if (Config.isSmallMode())
            mRoot.addView(mRightLayout, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ThemeManager.getContentHeight(), 1));

        // 创建输入视图的根布局（垂直方向）
        mInputViewRoot = new LinearLayout(context);
        mInputViewRoot.setClipChildren(false);
        mInputViewRoot.setClipToPadding(false);
        mInputViewRoot.setOrientation(LinearLayout.VERTICAL);
        mCenterLayout.addView(mInputViewRoot, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        // 创建候选词视图
        mCandidateView = new CandidateView(context);
        mInputViewRoot.addView(mCandidateView, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ThemeManager.getCandidateHeight()));
        mInputViewRoot.addView(mInputView, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ThemeManager.getKeyboardHeight()));
        // 设置根布局的背景
        Style color = ThemeManager.getStyle();
        mRoot.setBackground(color.getBackground(0xffdddddd));

        // 创建编码区视图（显示预编辑文本）
        mPreedit = new Composition(context) {
            @Override
            protected void onSizeChanged(int w, int h, int oldw, int oldh) {
                super.onSizeChanged(w, h, oldw, oldh);
                if (isSelected())
                    return;
                // 根据浮动模式调整编码区位置
                if (Config.isFloatMode()) {
                    float targetY = mRoot.getY() - mPreedit.getHeight();
                    mPreedit.setTranslationY(targetY);
                    mPreedit.setTranslationX(mRoot.getX());
                } else {
                    float targetY = mRoot.getTop() - mPreedit.getHeight();
                    mPreedit.setTranslationY(targetY);
                    mPreedit.setTranslationX(mCenterLayout.getX());
                }
            }
        };
        // 设置编码区的样式（颜色、背景、字体大小、边距）
        Style preeditColor = ThemeManager.getStyle().getStyle("preedit");
        mPreedit.setTextColor(preeditColor.getTextColor(0xffaaaaaa));
        mPreedit.setBackground(preeditColor.getBackground(0xff888888));
        mPreedit.setTextSize(TypedValue.COMPLEX_UNIT_DIP, preeditColor.getTextSize(18));
        int pd = ThemeManager.dp2px(4);
        mPreedit.setPadding(pd, pd, pd, pd);
        mPreedit.setVisibility(View.INVISIBLE);
        //mPreedit.setText(" ");

        // 创建云输入结果显示区
        mCloud = new TextView(context);
        mCloud.setTextColor(preeditColor.getTextColor(0xffaaaaaa));
        mCloud.setBackground(preeditColor.getBackground(0xff888888));
        mCloud.setTextSize(TypedValue.COMPLEX_UNIT_DIP, preeditColor.getTextSize(18));
        mCloud.setPadding(pd, pd, pd, pd);
        mCloud.setVisibility(View.INVISIBLE);
        mCloud.setText(" ");
        // 将根布局、编码区、云输入添加到视图中
        addView(mRoot, new FrameLayout.LayoutParams(Config.isFloatMode() ? ViewGroup.LayoutParams.WRAP_CONTENT : ViewGroup.LayoutParams.MATCH_PARENT, ThemeManager.getHeight(), Gravity.BOTTOM));
        addView(mPreedit, new FrameLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT, Gravity.TOP | Gravity.LEFT));
        addView(mCloud, new FrameLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT, Gravity.TOP | Gravity.RIGHT));
        showToolbarView(true);

        mRoot.setOnApplyWindowInsetsListener(new View.OnApplyWindowInsetsListener() {
            @NonNull
            @Override
            public WindowInsets onApplyWindowInsets(@NonNull View v, @NonNull WindowInsets insets) {
                // 将系统栏占用的空间转化为 ListView 的 Padding
                /*v.setPadding(
                        insets.getSystemWindowInsetLeft(),
                        insets.getSystemWindowInsetTop(),
                        insets.getSystemWindowInsetRight(),
                        insets.getSystemWindowInsetBottom()
                );*/
                if (!Config.isFloatMode() && mRoot != null) {
                    ViewGroup.LayoutParams lp = mRoot.getLayoutParams();
                    lp.height = ThemeManager.getHeight() + insets.getSystemWindowInsetBottom();
                    mRoot.setLayoutParams(lp);
                }
                if (insets.getSystemWindowInsetBottom() > 1) {
                    Window win = trime.getWindow().getWindow();
                    if (win != null) {
                        //win.addFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_NAVIGATION);
                        //win.setDecorFitsSystemWindows(false);

                        win.setNavigationBarColor(ThemeManager.getStyle().getBackgroundColor(0));
                    }
                } else {
                    Window win = trime.getWindow().getWindow();
                    if (win != null) {
                        //win.addFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_NAVIGATION);
                        //win.setDecorFitsSystemWindows(false);
                        win.setNavigationBarColor(ThemeManager.getStyle().getBackgroundColor(0));
                    }
                }
                return insets.consumeSystemWindowInsets();
            }
        });
        mRoot.requestApplyInsets();
        Window win = trime.getWindow().getWindow();
        if (win != null) {
            //win.addFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_NAVIGATION);
            //win.setDecorFitsSystemWindows(false);
            win.setNavigationBarColor(ThemeManager.getStyle().getBackgroundColor(0));
        }
        if (Config.isSmallMode()) {
            if (Config.getSmallModeGravity() == Gravity.LEFT) {
                mLeftLayout.setVisibility(GONE);
                mRightLayout.setVisibility(VISIBLE);
            } else {
                mLeftLayout.setVisibility(VISIBLE);
                mRightLayout.setVisibility(GONE);
            }
        } else {
            if (mLeftLayout != null)
                mLeftLayout.setVisibility(GONE);
            if (mRightLayout != null)
                mRightLayout.setVisibility(GONE);
        }
        if (Config.isFloatMode()) {
            KeyView hide = mCandidateView.getToolbar().getHide();
            hide.setOnLongClickListener(new OnLongClickListener() {
                @Override
                public boolean onLongClick(View v) {
                    setSelected(true);
                    if (!Config.isFloatMode()) {
                        mPreedit.setVisibility(View.VISIBLE);
                        mPreedit.setText("默认高度");
                        mPreedit.setY(getHeight() - ThemeManager.getRawContentHeight() - mPreedit.getHeight());
                    }
                    return true;
                }
            });
            hide.setOnTouchListener(new OnTouchListener() {
                private float mWidth;
                private int mLastW;
                private int mLastH;
                private float mX;
                private float mY;
                private float mLastX;
                private float mLastY;

                @Override
                public boolean onTouch(View v, MotionEvent event) {
                    switch (event.getActionMasked()) {
                        case MotionEvent.ACTION_DOWN:
                            mLastX = event.getRawX();
                            mLastY = event.getRawY();
                            mLastH = mInputView.getHeight();
                            mLastW = mCenterLayout.getWidth();
                            mX = mRoot.getTranslationX();
                            mY = mRoot.getTranslationY();
                            break;
                        case MotionEvent.ACTION_MOVE:
                            if (isSelected()) {
                                if (Config.isSmallMode()) {
                                    float y = event.getRawY();
                                    float mHeight = (mLastH + (mLastY - y));
                                    mInputView.setScaleY(mHeight / mLastH);
                                    mInputView.setTranslationY((mLastH - mHeight) / 2);
                                    mCandidateView.setTranslationY((mLastH - mHeight));
                                    float x = event.getRawX();
                                    mWidth = (mLastW + (x - mLastX));
                                    mCenterLayout.setScaleX(mWidth / mLastW);
                                    mCenterLayout.setTranslationX((mWidth - mLastW) / 2);
                                    mRightButton.setTranslationX((mWidth - mLastW));
                                } else {
                                    mRoot.setTranslationX(mX + event.getRawX() - mLastX);
                                    mRoot.setTranslationY(mY + event.getRawY() - mLastY);
                                }
                            }
                            break;
                        case MotionEvent.ACTION_UP:
                        case MotionEvent.ACTION_CANCEL:
                            if (isSelected()) {
                                if (Config.isSmallMode()) {
                                    Config.setKeyboardHeightScale(mInputView.getScaleY());
                                    Config.setSmallModeWidth((int) mWidth);
                                } else {
                                    Config.setFloatModeX((int) mRoot.getTranslationX());
                                    Config.setFloatModeY((int) mRoot.getTranslationY());
                                }
                                setSelected(false);
                                setTheme(Config.getTheme());
                            }
                            break;
                    }
                    return false;
                }
            });
            mRoot.setX(Config.getFloatModeX());
            if (mRoot.getX() < 0) {
                mRoot.setX(0);
            }
            if (mRoot.getX() + trime.getWidth() > trime.getMaxWidth()) {
                mRoot.setX(trime.getMaxWidth() - trime.getWidth());
            }

            mRoot.setY(Config.getFloatModeY());
            if (mRoot.getY() < ThemeManager.getHeight() - trime.getHeight() + ThemeManager.getCandidateHeight()) {
                mRoot.setY(ThemeManager.getHeight() - trime.getHeight() + ThemeManager.getCandidateHeight());
            }
            if (mRoot.getY() > 0) {
                mRoot.setY(0);
            }
        } else {
            KeyView hide = mCandidateView.getToolbar().getHide();
            hide.setOnLongClickListener(new OnLongClickListener() {
                @Override
                public boolean onLongClick(View v) {
                    setSelected(true);
                    if (!Config.isFloatMode()) {
                        mPreedit.setVisibility(View.VISIBLE);
                        mPreedit.setText("默认高度");
                        mPreedit.setY(getHeight() - ThemeManager.getRawContentHeight() - mPreedit.getHeight());
                    }
                    return true;
                }
            });
            hide.setOnTouchListener(new OnTouchListener() {
                private float mLastX;
                private float mWidth;
                private int mLastW;
                private int mLastH;
                private float mLastY;

                @Override
                public boolean onTouch(View v, MotionEvent event) {
                    switch (event.getActionMasked()) {
                        case MotionEvent.ACTION_DOWN:
                            mLastX = event.getRawX();
                            mLastY = event.getRawY();
                            mLastH = mInputView.getHeight();
                            mLastW = mCenterLayout.getWidth();
                            break;
                        case MotionEvent.ACTION_MOVE:
                            if (isSelected()) {
                                float y = event.getRawY();
                                float mHeight = (mLastH + (mLastY - y));
                                mInputView.setScaleY(mHeight / mLastH);
                                mInputView.setTranslationY((mLastH - mHeight) / 2);
                                mCandidateView.setTranslationY((mLastH - mHeight));
                                if (Config.isSmallMode()) {
                                    float x = event.getRawX();
                                    mWidth = (mLastW + (x - mLastX));
                                    mCenterLayout.setScaleX(mWidth / mLastW);
                                    mCenterLayout.setTranslationX((mWidth - mLastW) / 2);
                                    mRightButton.setTranslationX((mWidth - mLastW));
                                }
                            }
                            break;
                        case MotionEvent.ACTION_UP:
                        case MotionEvent.ACTION_CANCEL:
                            if (isSelected()) {
                                if (Config.isSmallMode()) {
                                    Config.setSmallModeWidth((int) mWidth);
                                }
                                Config.setKeyboardHeightScale(mInputView.getScaleY());
                                setSelected(false);
                                setTheme(Config.getTheme());
                            }
                            break;
                    }
                    return false;
                }
            });
        }

        // 根据 Rime 选项决定是否隐藏候选词栏
        if(Rime.getRimeOption("_hide_candidate"))
            mCandidateView.setVisibility(GONE);
        else
            mCandidateView.setVisibility(VISIBLE);
        // 根据 Rime 选项决定是否隐藏按键提示和候选词注释
        Config.set_hide_comment(Rime.getRimeOption("_hide_comment"));
        Config.set_hide_key_hint(Rime.getRimeOption("_hide_key_hint"));
        Config.set_hide_key_sound(Rime.getRimeOption("_hide_key_sound"));

    }

    /**
     * 获取根布局视图。
     *
     * @return 根布局 LinearLayout 对象。
     */
    public View getRoot() {
        return mRoot;
    }

    /**
     * 获取编码区视图。
     *
     * @return 编码区 Composition 对象。
     */
    public View getPreedit() {
        return mPreedit;
    }

    /**
     * 获取云输入结果显示区。
     *
     * @return 云输入 TextView 对象。
     */
    public TextView getCloud() {
        return mCloud;
    }

    /**
     * 刷新所有编码区按键的显示状态。
     */
    public void invalidateComposingKeys() {
        if (mInputView != null)
            mInputView.invalidateComposingKeys();
    }

    /**
     * 重新加载主题配置，重建所有 UI 组件。
     *
     * @param theme 主题名称。
     */
    public void setTheme(String theme) {
        removeAllViews();
        initView(getContext());
    }

    /**
     * 重新加载样式配置，重建所有 UI 组件。
     *
     * @param theme 样式名称。
     */
    public void setStyle(String theme) {
        removeAllViews();
        initView(getContext());
    }

    /**
     * 显示或隐藏自定义视图（如 Lua 脚本创建的视图）。
     * 如果 keyboardView 为 null，则显示正常的键盘和候选词栏。
     *
     * @param keyboardView 自定义视图对象，null 表示恢复正常视图。
     */
    public void showCustomView(View keyboardView) {
        if (mCustomView != null) {
            mCenterLayout.removeView(mCustomView);
        }
        mCustomView = keyboardView;
        if (keyboardView == null) {
            mInputViewRoot.setVisibility(View.VISIBLE);
            return;
        }
        mCenterLayout.addView(mCustomView, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        mInputViewRoot.setVisibility(View.GONE);
    }

    /**
     * 显示或隐藏提取的候选词视图（全屏模式）。
     * 切换时会同步候选词数据和索引。
     *
     * @param b true 显示全屏候选词视图，false 显示普通候选词栏。
     */
    public void showExtractedCandidatesView(boolean b) {
        showCustomView(null);
        if (mExpandedCandidateView == null) {
            if (!b)
                return;
            mExpandedCandidateView = new ExpandedCandidateView(getContext());
            mExpandedCandidateView.setVisibility(View.GONE);
            mCenterLayout.addView(mExpandedCandidateView, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        }
        mExpandedCandidateView.setVisibility(b ? View.VISIBLE : View.GONE);
        mInputViewRoot.setVisibility(!b ? View.VISIBLE : View.GONE);
        mShowExtractedCandidatesView = b;
        if (b) {
            mExpandedCandidateView.setData(mCandidateView.getData());
            mExpandedCandidateView.setIdx(mCandidateView.getIdx());
        } else {
            if (mExpandedCandidateView != null) {
                mCandidateView.setData(mExpandedCandidateView.getData());
                mCandidateView.setIdx(mExpandedCandidateView.getIdx());
            }
        }
    }

    /**
     * 显示或隐藏符号键盘视图。
     *
     * @param b true 显示符号键盘，false 显示正常键盘。
     */
    public void showSymbolsView(boolean b) {
        showCustomView(null);
        if (mSymbolsKeyboardView == null) {
            if (!b)
                return;
            mSymbolsKeyboardView = new SymbolsKeyboardView(getContext());
            mSymbolsKeyboardView.setVisibility(View.GONE);
            mCenterLayout.addView(mSymbolsKeyboardView, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        }
        mInputViewRoot.setVisibility(!b ? View.VISIBLE : View.GONE);
        mSymbolsKeyboardView.setVisibility(b ? View.VISIBLE : View.GONE);
    }

    // 候选词更新的 Runnable，用于防抖处理
    private final Runnable mUpdateCandidateRunnable = () -> {
        if (mShowExtractedCandidatesView) mExpandedCandidateView.show();
        else mCandidateView.show(mStartIdx);
    };

    /**
     * 更新候选词显示。
     * 使用防抖机制，避免频繁更新导致性能问题。
     */
    public void updateCandidate() {
        // 必须有延迟（如 10-20ms），才能在 Handler 队列中起到去重效果
        mHandler.removeCallbacks(mUpdateCandidateRunnable);
        mHandler.postDelayed(mUpdateCandidateRunnable, 10);
    }

    // 候选词过滤的 Runnable，用于防抖处理
    private final Runnable mFilterCandidateRunnable = () -> {
        if (mShowExtractedCandidatesView) mExpandedCandidateView.update();
        else mCandidateView.update();
    };

    /**
     * 过滤候选词显示。
     * 使用防抖机制，避免频繁更新导致性能问题。
     */
    public void filterCandidate() {
        // 必须有延迟（如 10-20ms），才能在 Handler 队列中起到去重效果
        mHandler.removeCallbacks(mFilterCandidateRunnable);
        mHandler.postDelayed(mFilterCandidateRunnable, 10);
    }

    // 1. 定义一个成员变量保存上一次的内容，用于比对去重
    private String mLastComposingText = "";

    private boolean mHasComposition;
    private boolean mHideComposition;
    private boolean mHideCompositionByInlinePreedit;

    private String getVisibleCompositionText() {
        if (!TextUtils.isEmpty(mLastComposingText)) {
            return mLastComposingText;
        }
        RimeProto.Context context = Rime.getRimeContext();
        if (context == null) {
            return "";
        }
        String input = context.getInput();
        return input == null ? "" : input;
    }

    // 2. 复用 Runnable，避免频繁 GC 产生内存抖动
    private final Runnable mComposingRunnable = new Runnable() {
        @Override
        public void run() {
            String s = getVisibleCompositionText();

            if (TextUtils.isEmpty(s) || mHideComposition || mHideCompositionByInlinePreedit) {
                mPreedit.setVisibility(View.INVISIBLE);
            } else {
                mPreedit.setVisibility(View.VISIBLE);

                // 核心优化：使用 setTranslationY 代替 LayoutParams
                // 这只会触发重绘（Repaint），不会触发重布局（Relayout），性能提升巨大
                 /*if(Config.isFloatMode()) {
                    float targetY = mRoot.getY() - mPreedit.getHeight();
                    mPreedit.setTranslationY(targetY);
                    mPreedit.setTranslationX(mRoot.getX());
                } else {
                     float targetY = mRoot.getTop() - mPreedit.getHeight();
                     mPreedit.setTranslationY(targetY);
                     mPreedit.setTranslationX(mCenterLayout.getX());
                 }*/
            }
            mStartIdx = 0;
            if (mHasComposition)
                mStartIdx = mPreedit.setWindow(mCompositionMinLength);
            else
                mPreedit.setText(s);
        }
    };

    /**
     * 设置编码区文本。
     * 使用防抖机制和去重判断，避免频繁更新导致性能问题。
     *
     * @param s 预编辑文本字符串。
     */
    public void setComposingText(String s) {
        // 3. 在非 UI 线程预判：如果内容没变，直接拦截，不向主线程发消息
        if (s == null) s = "";
        //if (s.equals(mLastComposingText)) return;

        mLastComposingText = s;

        // 4. 防抖处理：移除旧任务，确保主线程只处理最后一次更新
        mHandler.removeCallbacks(mComposingRunnable);
        mHandler.post(mComposingRunnable);
    }

    public void setInlinePreeditMode(InlineModeType mode) {
        mHideCompositionByInlinePreedit = mode != InlineModeType.INLINE_NONE;
        if (mode == InlineModeType.INLINE_NONE) {
            mHideComposition = false;
        }
        mHandler.removeCallbacks(mComposingRunnable);
        mHandler.post(mComposingRunnable);
    }

    // 1. 成员变量复用，减少 GC 压力
    private String mLastCloudText = "";

    private final Runnable mCloudRunnable = new Runnable() {
        @Override
        public void run() {
            // 这里的 s 直接取最新的 mLastCloudText
            String s = mLastCloudText;
            mCloud.setText(s);

            if (TextUtils.isEmpty(s)) {
                mCloud.setVisibility(View.INVISIBLE);
            } else {
                mCloud.setVisibility(View.VISIBLE);

                // 2. 性能核心：使用 TranslationY 避开 RequestLayout
                // 这样只会触发重绘（Invalidate），不会导致主线程计算整棵布局树
                if (Config.isFloatMode()) {
                    float targetY = mRoot.getTop() - mRoot.getY() - mCloud.getHeight();
                    mCloud.setTranslationY(targetY);
                    mCloud.setTranslationX(-(TrimeService.getInstance().getMaxWidth() - mRoot.getX() - mRoot.getWidth()));
                } else {
                    float targetY = mRoot.getTop() - mCloud.getHeight();
                    mCloud.setTranslationY(targetY);
                    mCloud.setTranslationX(-(TrimeService.getInstance().getMaxWidth() - mCenterLayout.getX() - mCenterLayout.getWidth()));
                }
            }
        }
    };

    /**
     * 设置云输入结果显示文本。
     * 使用防抖机制和内容去重判断，避免频繁更新导致性能问题。
     *
     * @param s 云输入结果文本字符串。
     */
    public void setCloudText(String s) {
        // 3. 内容一致性拦截
        if (s == null) s = "";
        if (s.equals(mLastCloudText)) return;

        mLastCloudText = s;

        // 4. 防抖：撤回尚未执行的任务，确保消息队列始终只有一个最新任务
        mHandler.removeCallbacks(mCloudRunnable);
        mHandler.post(mCloudRunnable);
    }

    /**
     * 设置键盘布局。
     * 根据 ID 切换到不同的键盘类型：候选词、剪贴板、常用语、符号或普通键盘。
     *
     * @param id 键盘标识符。
     */
    public void setKeyboard(String id) {
        mHandler.post(() -> {
            if (mCustomView != null) {
                mCenterLayout.removeView(mCustomView);
                mCustomView = null;
            }
            switch (id) {
                //case "symbols_ext":
                //    showSymbolsView(true);
                //    return;
                case "candidate":
                    showExtractedCandidatesView(true);
                    return;
                case "clipboard":
                    showClipboardView(true);
                    return;
                case "phrase":
                    showClipboardView(true);
                    mClipboardKeyboardView.showPhrase();
                    return;
                default:
                    showSymbolsView(false);
                    mInputView.setKeyboard(id);
                    // 更新 mCenterLayout 高度以匹配新键盘的动态高度
                    ViewGroup.LayoutParams lp = mCenterLayout.getLayoutParams();
                    if (lp != null) {
                        lp.height = ThemeManager.getContentHeight();
                        mCenterLayout.setLayoutParams(lp);
                    }
                    // 更新 mRoot 高度以匹配新键盘的动态高度，并触发窗口插入区重算
                    ViewGroup.LayoutParams rlp = mRoot.getLayoutParams();
                    if (rlp != null) {
                        rlp.height = ThemeManager.getHeight();
                        mRoot.setLayoutParams(rlp);
                    }
                    mRoot.requestApplyInsets();
            }
        });
    }

    /**
     * 判断当前是否处于 Shift 状态。
     *
     * @return true 如果 Shift 已按下，false 否则。
     */
    public boolean isShifted() {
        return mInputView.isShifted();
    }

    /**
     * 设置 Shift 状态。
     *
     * @param shifted true 表示按下 Shift，false 表示释放。
     */
    public void setShifted(boolean shifted) {
        mInputView.setShifted(shifted);
    }

    /**
     * 刷新所有按键的显示状态。
     * 包括键盘按键和候选词栏，同时更新 Rime 选项配置。
     */
    public void invalidateAllKeys() {
        boolean hideCommentChanged = Config.is_hide_comment() != Rime.getRimeOption("_hide_comment");
        Config.set_hide_comment(Rime.getRimeOption("_hide_comment"));
        Config.set_hide_key_hint(Rime.getRimeOption("_hide_key_hint"));
        Config.set_hide_key_sound(Rime.getRimeOption("_hide_key_sound"));
        if (mInputView != null) mInputView.invalidateAllKeys();
        if (mCandidateView != null){
            if(Rime.getRimeOption("_hide_candidate"))
                mCandidateView.setVisibility(GONE);
            else
                mCandidateView.setVisibility(VISIBLE);
            mCandidateView.invalidateAllKeys();
        }
        // 更新容器高度以匹配候选栏和键盘的最新动态高度
        ViewGroup.LayoutParams clp = mCenterLayout.getLayoutParams();
        if (clp != null) {
            clp.height = ThemeManager.getContentHeight();
            mCenterLayout.setLayoutParams(clp);
        }
        ViewGroup.LayoutParams rlp = mRoot.getLayoutParams();
        if (rlp != null) {
            rlp.height = ThemeManager.getHeight();
            mRoot.setLayoutParams(rlp);
        }
        mRoot.requestApplyInsets();
        if (hideCommentChanged) {
            int candidateAreaHeight = ThemeManager.getCandidateHeight();
            ViewGroup.LayoutParams cvlp = mCandidateView.getLayoutParams();
            if (cvlp != null) {
                cvlp.height = candidateAreaHeight;
                mCandidateView.setLayoutParams(cvlp);
            }
        }
    }

    /**
     * 显示或隐藏剪贴板键盘视图。
     *
     * @param b true 显示剪贴板，false 隐藏。
     */
    public void showClipboardView(boolean b) {
        showCustomView(null);
        if (mClipboardKeyboardView == null) {
            if (!b)
                return;
            mClipboardKeyboardView = new ClipboardKeyboardView(getContext());
            mClipboardKeyboardView.setVisibility(View.GONE);
            mCenterLayout.addView(mClipboardKeyboardView, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        }
        mClipboardKeyboardView.setVisibility(b ? View.VISIBLE : View.GONE);
        mInputViewRoot.setVisibility(!b ? View.VISIBLE : View.GONE);
        if (!b)
            return;
        mClipboardKeyboardView.show();

    }

    /**
     * 显示或隐藏工具栏视图。
     *
     * @param b true 显示工具栏，false 隐藏。
     */
    public void showToolbarView(boolean b) {
        mCandidateView.showToolbarView(b);
    }

    /**
     * 设置 ASCII 模式状态。
     *
     * @param asciiMode true 表示 ASCII 模式，false 表示中文模式。
     */
    public void setAsciiMode(boolean asciiMode) {
        mInputView.setAsciiMode(asciiMode);
    }

    /**
     * 设置输入法方案。
     * 保存选择的方案 ID，启用软光标，并重新加载主题。
     *
     * @param id 方案 ID。
     */
    public void setSchema(String id) {
        mHandler.post(new Runnable() {
            @Override
            public void run() {
                Function.saveString(getContext(), "select_schema_id", id);
                String soft_cursor_key = "soft_cursor";
                Rime.setRimeOption(soft_cursor_key, true); //软光标
                setTheme(Config.getTheme());
                setInlinePreeditMode(TrimeService.getInstance().getInlinePreeditMode());
                //mInputView.setKeyboard(id);
                //mCandidateView.setSchema(id);
            }
        });
    }

    /**
     * 切换到上一个候选词。
     *
     * @return true 如果切换成功，false 否则。
     */
    public boolean prevCandidate() {
        return mCandidateView.prevCandidate();
    }

    /**
     * 切换到下一个候选词。
     *
     * @return true 如果切换成功，false 否则。
     */
    public boolean nextCandidate() {
        return mCandidateView.nextCandidate();
    }

    /**
     * 设置候选词列表数据。
     *
     * @param items 候选词项列表。
     */
    public void setCandidates(final ArrayList<CandidateItem> items) {
        mHandler.post(new Runnable() {
            @Override
            public void run() {
                mCandidateView.setData(items);
                showToolbarView(items.isEmpty());
            }
        });
    }

    /**
     * 设置小屏模式状态。
     *
     * @param value true 启用小屏模式，false 禁用。
     */
    public void setSmallMode(boolean value) {
        Config.setSmallMode(value);
        mHandler.post(new Runnable() {
            @Override
            public void run() {
                setTheme(Config.getTheme());
            }
        });
    }

    /**
     * 设置浮动模式状态。
     *
     * @param value true 启用浮动模式，false 禁用。
     */
    public void setFloatMode(boolean value) {
        Config.setFloatMode(value);
        mHandler.post(new Runnable() {
            @Override
            public void run() {
                setTheme(Config.getTheme());
            }
        });
    }

    /**
     * 设置编码区文本（CharSequence 版本）。
     *
     * @param c 预编辑文本。
     */
    public void setComposition(CharSequence c) {
        mHandler.post(new Runnable() {
            @Override
            public void run() {
                mPreedit.setText(c);
            }
        });
    }

    /**
     * 添加多个编码内容到编码区。
     *
     * @param list 编码内容字符串列表。
     */
    public void addCompositions(ArrayList<String> list) {
        mHandler.post(new Runnable() {
            @Override
            public void run() {
                mPreedit.addCompositions(list);
            }
        });
    }

}
