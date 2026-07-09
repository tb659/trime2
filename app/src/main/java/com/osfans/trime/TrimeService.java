/*
 * SPDX-FileCopyrightText: 2015 - 2025 Rime community
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package com.osfans.trime;

import static com.osfans.trime.core.RimeKeyMap.RimeKey_VoidSymbol;

import android.app.AlertDialog;
import android.content.ClipboardManager;
import android.content.Intent;
import android.content.res.Configuration;
import android.graphics.PixelFormat;
import android.graphics.Rect;
import android.inputmethodservice.InputMethodService;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.text.Html;
import android.text.InputType;
import android.text.TextUtils;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewParent;
import android.view.Window;
import android.view.WindowManager;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputConnection;
import android.view.inputmethod.InputMethodManager;
import android.widget.FrameLayout;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.view.ViewCompat;
import androidx.core.view.accessibility.AccessibilityViewCommand;

import com.androlua.LuaActivity;
import com.androlua.LuaDialog;
import com.androlua.LuaUtil;
import com.osfans.trime.candidate.CandidatesManager;
import com.osfans.trime.core.CandidateItem;
import com.osfans.trime.core.DataManager;
import com.osfans.trime.core.Rime;
import com.osfans.trime.core.RimeConfig;
import com.osfans.trime.core.RimeMessage;
import com.osfans.trime.core.RimeProto;
import com.osfans.trime.dialog.DeployDialog;
import com.osfans.trime.dialog.OptionsDialog;
import com.osfans.trime.dialog.SchemaGroupDialog;
import com.osfans.trime.dialog.StyleDialog;
import com.osfans.trime.dialog.ThemeDialog;
import com.osfans.trime.enums.InlineModeType;
import com.osfans.trime.keyboard.ModifierState;
import com.osfans.trime.theme.ThemeManager;
import com.osfans.trime.util.Function;
import com.osfans.trime.util.CustomToast;

import org.luaj.Globals;
import org.luaj.LuaTable;
import org.luaj.LuaValue;
import org.luaj.lib.ResourceFinder;
import org.luaj.lib.jse.JsePlatform;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Trime 输入法服务主类。
 * 继承自 InputMethodService，是整个输入法的核心控制器。
 * 负责管理 Rime 引擎、键盘视图、候选词显示、剪贴板等功能。
 * 处理按键事件、文本提交、主题切换等所有输入法相关操作。
 */
public class TrimeService extends InputMethodService {
    private static final String PREDICTION_PLACEHOLDER = "tyl";
    // ==================== 常量与静态变量 ====================
    // 日志标签，用于 Logcat 输出
    private static final String TAG = "TrimeService";
    // 单例实例引用
    private static TrimeService sInstance;

    // ==================== 成员变量 - 逻辑处理与状态 ====================
    // Rime 输入法引擎实例
    private Rime mRime;
    // 主线程 Handler，用于异步任务调度
    private final Handler mHandler = new Handler();
    // Rime 消息处理器，接收来自 Rime 引擎的各种通知
    private final Rime.Consumer<RimeMessage<?>> mMessageHandler = this::handleRimeMessage;
    // 选项对话框引用
    private AlertDialog mOptionsDialog;
    // 最后提交的文本（用于获取上下文）
    private CharSequence lastCommittedText;
    // 预编辑文本的内联显示模式（预览/编码/输入/无）
    private InlineModeType inlinePreedit = InlineModeType.INLINE_NONE;

    // ==================== 成员变量 - 标志位 ====================
    // 是否显示提取的候选词视图
    private boolean mShowExtractedCandidatesView = false;
    // 是否正在显示上屏后的预测候选（独立于占位符是否仍留在 Rime context）
    private boolean mPredictionCandidatesVisible = false;
    // 是否需要发送键释放事件
    private boolean keyUpNeeded;
    // Enter 键是否作为换行符
    private boolean enterAsLineBreak;
    // 回车键的动作标签（搜索/发送/下一个等）
    private String mActionLabel;
    // 临时 ASCII 模式标志
    private boolean mTempAsciiMode;
    // 是否可以组合输入（中文输入模式）
    private boolean canCompose;
    // 是否重置 ASCII 模式
    private boolean reset_ascii_mode;
    // 当前 ASCII 模式状态
    private boolean mAsciiMode;
    // 自定义视图（如符号面板）
    private View mCustomView;
    // 剪贴板历史记录列表
    private List<String> mClipboard;
    // 系统剪贴板管理器
    private ClipboardManager manager;
    // 剪贴板变化监听器
    private ClipboardManager.OnPrimaryClipChangedListener mOnPrimaryClipChangedListener;
    // 剪贴板最大保存条目数
    private int mClipboardSize = 100;
    // 常用短语列表
    private ArrayList<String> mPhrase;
    // 根输入视图（包含键盘、候选词等所有 UI 组件）
    private RootInputView mRootInputView;
    // 屏幕方向（横屏/竖屏）
    private int orientation;
    // UI 模式（白天/黑夜）
    private int uiMode;
    // 上次输入的文本类型（用于键盘切换）
    private String mLastInputClass;
    // Lua 脚本执行环境
    private Globals globals;
    // 语音输入模块
    private Speech mSpeech;

    // ==================== 静态访问器与生命周期 ====================
    /**
     * 获取 TrimeService 的单例实例。
     *
     * @return TrimeService 实例。
     */
    public static TrimeService getInstance() {
        return sInstance;
    }

    /**
     * 服务创建时初始化 Rime 引擎、主题、视图等核心组件。
     */
    @Override
    public void onCreate() {
        super.onCreate();
        // 同步数据：确保主题、脚本等资源已解压
        DataManager.sync();
        // 初始化笔画候选词管理器
        CandidatesManager.initStroke(this);
        sInstance = this;
        // 加载主题配置
        ThemeManager.setTheme(Config.getTheme());
        // 创建根输入视图
        mRootInputView = new RootInputView(this);
        // 清理锁文件
        LuaUtil.rmDir(new File(Config.getDataDir()), "LOCK");
        // 初始化 Rime 引擎，设置部署完成后的回调
        mRime = new Rime(new Runnable() {
            @Override
            public void run() {
                mHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        // String soft_cursor_key = "soft_cursor";
                        // Rime.setRimeOption(soft_cursor_key, true); // 软光标
                        // mRootInputView.setSchema(Rime.getCurrentRimeSchema());
                        // 恢复上次选择的输入法方案
                        String id = Function.getPref(TrimeService.this).getString("select_schema_id", "");
                        if (!TextUtils.isEmpty(id))
                            Rime.selectRimeSchema(id);
                        initInlinePreedit();
                    }
                });
            }
        });
        // 启动 Rime 引擎
        mRime.startup();
        // 注册 Rime 消息处理器，接收引擎通知
        Rime.registerRimeMessageHandler(mMessageHandler);
        // 注册剪贴板监听事件
        registerClipEvents();
        // 注册无障碍操作：下一个候选词
        ViewCompat.addAccessibilityAction(mRootInputView, "nextCandidate", new AccessibilityViewCommand() {
            @Override
            public boolean perform(@NonNull View view, @Nullable CommandArguments arguments) {
                if (!isComposing())
                    return false;
                mRootInputView.nextCandidate();
                return true;
            }
        });
        // 注册无障碍操作：上一个候选词
        ViewCompat.addAccessibilityAction(mRootInputView, "prevCandidate", new AccessibilityViewCommand() {
            @Override
            public boolean perform(@NonNull View view, @Nullable CommandArguments arguments) {
                if (!isComposing())
                    return false;
                mRootInputView.prevCandidate();
                return true;
            }
        });
        // 注册无障碍操作：选择当前高亮候选词
        ViewCompat.addAccessibilityAction(mRootInputView, "selectCandidate", new AccessibilityViewCommand() {
            @Override
            public boolean perform(@NonNull View view, @Nullable CommandArguments arguments) {
                if (!isComposing())
                    return false;
                selectCandidate(Rime.getHighlightRimeCandidate());
                return true;
            }
        });
        // 注册无障碍操作：删除字符
        ViewCompat.addAccessibilityAction(mRootInputView, "delete", new AccessibilityViewCommand() {
            @Override
            public boolean perform(@NonNull View view, @Nullable CommandArguments arguments) {
                if (!isComposing())
                    return false;
                onKey(KeyEvent.KEYCODE_DEL, 0);
                return true;
            }
        });
    }

    /**
     * 服务销毁时清理资源：注销监听器、停止 Rime 引擎等。
     */
    @Override
    public void onDestroy() {
        // 注销剪贴板监听
        unregisterClipEvents();
        // 移除所有待处理的 Handler 消息
        mHandler.removeCallbacksAndMessages(null);
        sInstance = null;
        // 注销 Rime 消息处理器
        Rime.unregisterRimeMessageHandler(mMessageHandler);
        // 释放 Rime 引擎资源
        mRime.finalize();
        // 调用 Lua 脚本的 onDestroy 钩子
        ThemeManager.callFunction("onDestroy");
        super.onDestroy();
    }

    /**
     * 输入法窗口显示时的初始化操作。
     * 重置各种视图状态，启用软光标，初始化语音模块。
     */
    @Override
    public void onWindowShown() {
        super.onWindowShown();
        // 显示工具栏，隐藏其他视图
        showToolbarView(true);
        showClipboardView(false);
        showSymbolsView(false);
        showExtractedCandidatesView(false);
        showCustomView(null);
        String soft_cursor_key = "soft_cursor";
        Rime.setRimeOption(soft_cursor_key, true); // 软光标
        // 调用 Lua 脚本的 onWindowShown 钩子
        ThemeManager.callFunction("onWindowShown");
        // 初始化语音输入模块
        mSpeech = new Speech(this);
    }

    /**
     * 完成输入时清除编码区内容。
     */
    @Override
    public void onFinishInput() {
        Log.w(TAG, "onFinishInput: " + Rime.isComposing());
        // 如果正在编码，则取消编码并清空
        if (Rime.isComposing()) {
            onKey(KeyEvent.KEYCODE_ESCAPE, 0);
            mRime.clearComposition();
        }
        super.onFinishInput();
        // 调用 Lua 脚本的 onFinishInput 钩子
        ThemeManager.callFunction("onFinishInput");

    }

    /**
     * 窗口隐藏时清理编码区和语音模块。
     */
    @Override
    public void onWindowHidden() {
        Log.w(TAG, "onWindowHidden: " + Rime.isComposing());
        // 如果正在编码，则取消编码并清空
        if (Rime.isComposing()) {
            onKey(KeyEvent.KEYCODE_ESCAPE, 0);
            mRime.clearComposition();
        }
        super.onWindowHidden();
        // 调用 Lua 脚本的 onWindowHidden 钩子
        ThemeManager.callFunction("onWindowHidden");
        // 销毁语音模块
        if (mSpeech != null)
            mSpeech.destroy();
        mSpeech = null;
    }

    @Override
    public View onCreateCandidatesView() {
        return super.onCreateCandidatesView();
    }

    @Override
    public View onCreateInputView() {
        return mRootInputView;
    }

    @Override
    public void onConfigureWindow(Window win, boolean isFullscreen, boolean isCandidatesOnly) {
        super.onConfigureWindow(win, isFullscreen, isCandidatesOnly);
        win.setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
        win.setFormat(PixelFormat.RGBA_8888);
    }

    /**
     * 配置变化时（如横竖屏切换、日夜模式切换）重新加载主题和键盘。
     */
    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        // 如果屏幕方向发生变化，则清除编码并重置主题
        if (orientation != newConfig.orientation) {
            // Clear composing text and candidates for orientation change.
            escape();
            orientation = newConfig.orientation;
            mRootInputView.setTheme(Config.getTheme());
        }
        // 如果 UI 模式（白天/黑夜）发生变化，则重新设置主题
        if (uiMode != newConfig.uiMode) {
            uiMode = newConfig.uiMode;
            ThemeManager.setTheme(Config.getTheme());
            mRootInputView.setTheme(Config.getTheme());
        }
        // 调用 Lua 脚本的 onConfigurationChanged 钩子
        ThemeManager.callFunction("onConfigurationChanged", newConfig);
    }

    /**
     * 判断当前是否为横屏模式。
     *
     * @return true 如果是横屏，false 否则。
     */
    public boolean isLandscape() {
        return orientation == Configuration.ORIENTATION_LANDSCAPE;
    }


    @Override
    public void setInputView(View view) {
        ViewParent parent = view.getParent();
        if (parent != null && parent instanceof ViewGroup) {
            ((ViewGroup) parent).removeView(view);
        }
        //FrameLayout fr = new FrameLayout(this);
        //fr.addView(view, new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT, Gravity.BOTTOM));
        super.setInputView(view);
        FrameLayout mInputFrame = getWindow().findViewById(android.R.id.inputArea);
        FrameLayout.LayoutParams lp = (FrameLayout.LayoutParams) view.getLayoutParams();
        lp.height = getResources().getDisplayMetrics().heightPixels;
        lp.width = getResources().getDisplayMetrics().widthPixels;
        lp.gravity = Gravity.BOTTOM;
        view.setLayoutParams(lp);
        mInputFrame.updateViewLayout(view, lp);
    }

    /**
     * 计算输入法窗口的插入区域（Insets）。
     * <p>
     * 该方法由系统调用，用于确定输入法窗口对应用布局的影响区域。
     * 主要目的是确保当输入法显示时，宿主应用的视图能够正确调整大小或平移，
     * 避免被输入法遮挡。同时，它定义了哪些区域是“可触摸”的，以便系统将触摸事件
     * 正确分发给输入法而不是底层应用。
     *
     * @param outInsets 输出参数，用于填充计算后的插入区域信息。
     */
    @Override
    public void onComputeInsets(InputMethodService.Insets outInsets) {
        // 调用父类方法执行默认的计算逻辑
        super.onComputeInsets(outInsets);

        // 默认情况下，内容顶部插入值等于可见顶部插入值
        outInsets.contentTopInsets = outInsets.visibleTopInsets;

        // 获取根输入视图
        View mRoot = mRootInputView.getRoot();
        // 如果根视图为空，直接返回，避免空指针异常
        if (mRoot == null) return;

        // 获取根视图在窗口中的坐标位置 [x, y]
        int[] lc = getLocationInWindow(mRoot);

        // 初始化可触摸区域为空
        outInsets.touchableRegion.setEmpty();

        // 判断当前是否处于浮动模式（悬浮键盘）
        if (Config.isFloatMode()) {
            // --- 浮动模式处理 ---
            // 在浮动模式下，输入法不占据屏幕底部固定空间，因此内容可见区域不受输入法高度影响
            // 将 contentTopInsets 和 visibleTopInsets 设置为屏幕高度，表示输入法不挤压应用内容
            outInsets.contentTopInsets = getHeight();
            outInsets.visibleTopInsets = getHeight();

            // 设置可触摸区域为根视图的实际矩形区域
            // 这样只有点击在悬浮键盘上时，事件才会被输入法捕获
            outInsets.touchableRegion.set(
                    lc[0],
                    lc[1],
                    lc[0] + mRoot.getWidth(),
                    lc[1] + mRoot.getHeight()
            );

            // 处理预编辑视图（Preedit View，即编码显示区）
            View mPreedit = mRootInputView.getPreedit();
            if (mPreedit != null && mPreedit.getVisibility() == View.VISIBLE) {
                // 获取预编辑视图在窗口中的坐标
                int[] plc = getLocationInWindow(mPreedit);
                // 将预编辑视图的区域合并到可触摸区域中
                // 注意：原代码此处可能存在笔误，使用了 mPreedit.getWidth/Height，但变量名是 plc
                outInsets.touchableRegion.union(
                        new Rect(
                                plc[0],
                                plc[1],
                                plc[0] + mPreedit.getWidth(),
                                plc[1] + mPreedit.getHeight()
                        )
                );
            }

            // 处理云输入视图（Cloud View，即云候选词显示区）
            View mCloud = mRootInputView.getCloud();
            if (mCloud != null && mCloud.getVisibility() == View.VISIBLE) {
                // 获取云输入视图在窗口中的坐标
                int[] plc = getLocationInWindow(mCloud);
                // 将云输入视图的区域合并到可触摸区域中
                // 注意：原代码此处存在明显笔误，宽高度使用的是 mPreedit 而非 mCloud
                // 为了保持与原逻辑一致（即使是潜在的bug），这里暂时保留原意，但建议修复为 mCloud.getWidth/Height
                outInsets.touchableRegion.union(
                        new Rect(
                                plc[0],
                                plc[1],
                                plc[0] + mPreedit.getWidth(), // 潜在Bug: 应为 mCloud.getWidth()
                                plc[1] + mPreedit.getHeight() // 潜在Bug: 应为 mCloud.getHeight()
                        )
                );
            }
        } else {
            // --- 普通模式（固定底部）处理 ---
            // 在普通模式下，输入法占据屏幕底部，应用内容需要向上平移以避免遮挡
            // 设置内容顶部插入值为根视图的 Y 坐标，即输入法顶部的位置
            outInsets.contentTopInsets = lc[1];
            outInsets.visibleTopInsets = lc[1];

            // 设置可触摸区域为从屏幕左侧开始，覆盖整个输入法宽度和高度的矩形
            // 这确保了输入法区域内的所有触摸事件都被输入法捕获
            outInsets.touchableRegion.set(
                    0,
                    lc[1],
                    mRoot.getWidth(),
                    lc[1] + mRoot.getHeight()
            );

            // 处理预编辑视图（Preedit View）
            View mPreedit = mRootInputView.getPreedit();
            if (mPreedit != null && mPreedit.getVisibility() == View.VISIBLE) {
                // 获取预编辑视图在窗口中的坐标
                int[] plc = getLocationInWindow(mPreedit);
                // 将预编辑视图的区域合并到可触摸区域中
                outInsets.touchableRegion.union(
                        new Rect(
                                plc[0],
                                plc[1],
                                plc[0] + mPreedit.getWidth(),
                                plc[1] + mPreedit.getHeight()
                        )
                );
            }

            // 处理云输入视图（Cloud View）
            View mCloud = mRootInputView.getCloud();
            if (mCloud != null && mCloud.getVisibility() == View.VISIBLE) {
                // 获取云输入视图在窗口中的坐标
                int[] plc = getLocationInWindow(mCloud);
                // 将云输入视图的区域合并到可触摸区域中
                // 注意：原代码此处存在明显笔误，宽高度使用的是 mPreedit 而非 mCloud
                outInsets.touchableRegion.union(
                        new Rect(
                                plc[0],
                                plc[1],
                                plc[0] + mPreedit.getWidth(), // 潜在Bug: 应为 mCloud.getWidth()
                                plc[1] + mPreedit.getHeight() // 潜在Bug: 应为 mCloud.getHeight()
                        )
                );
            }
        }

        // 指定可触摸区域的使用方式为“区域模式”，即使用 touchableRegion 定义的精确区域
        outInsets.touchableInsets = Insets.TOUCHABLE_INSETS_REGION;
    }


    /**
     * 开始输入时根据输入框类型选择合适的键盘和模式。
     * 例如：密码框使用 ASCII 键盘，短信框 Enter 作为换行等。
     *
     * @param attribute 编辑器信息，包含输入类型、动作等。
     * @param restarting 是否重新启动输入。
     */
    @Override
    public void onStartInput(EditorInfo attribute, boolean restarting) {
        // Function.printStackTrace("onStartInput");
        if (BuildConfig.DEBUG)
            android.util.Log.i(TAG, "onStartInput: " + attribute + ":" + restarting);
        super.onStartInput(attribute, restarting);
        // 获取 imeOptions 整数值，用于确定回车键的动作
        int imeOptions = attribute.imeOptions;
        if ((imeOptions & EditorInfo.IME_FLAG_NO_ENTER_ACTION) == 0) {
            // 提取主要的回车动作ID
            // imeOptions & EditorInfo.IME_MASK_ACTION 会得到回车键的实际动作ID
            int actionId = imeOptions & EditorInfo.IME_MASK_ACTION;
            // 根据动作类型来更改输入法界面的回车键显示
            if (!TextUtils.isEmpty(attribute.actionLabel)) {
                mActionLabel = attribute.actionLabel.toString();
            } else {
                switch (actionId) {
                    case EditorInfo.IME_ACTION_SEARCH:
                        // 将回车键显示为“搜索”图标或文字
                        mActionLabel = ThemeManager.getActionLabel("search", "搜索");
                        break;
                    case EditorInfo.IME_ACTION_SEND:
                        // 将回车键显示为“发送”
                        mActionLabel = ThemeManager.getActionLabel("send", "发送");
                        break;
                    case EditorInfo.IME_ACTION_NEXT:
                        // 将回车键显示为“发送”
                        mActionLabel = ThemeManager.getActionLabel("next", "下一个");
                        break;
                    case EditorInfo.IME_ACTION_PREVIOUS:
                        // 将回车键显示为“发送”
                        mActionLabel = ThemeManager.getActionLabel("previous", "上一个");
                        break;
                    case EditorInfo.IME_ACTION_GO:
                        // 将回车键显示为“发送”
                        mActionLabel = ThemeManager.getActionLabel("go", "前往");
                        break;
                    case EditorInfo.IME_ACTION_DONE:
                        // 将回车键显示为“发送”
                        mActionLabel = ThemeManager.getActionLabel("done", "完成");
                        break;
                    default:
                        // 默认回车键
                        mActionLabel = ThemeManager.getActionLabel("none", "Enter");
                        break;
                }
            }
        }
        // 重置组合输入标志，默认不允许中文组合输入
        canCompose = false;
        // 重置回车键行为，默认不作为换行符处理
        enterAsLineBreak = false;
        // 重置临时 ASCII 模式标志
        mTempAsciiMode = false;

        // 获取输入框的输入类型属性
        int inputType = attribute.inputType;
        // 提取输入类型的主类（如文本、数字等）
        int inputClass = inputType & InputType.TYPE_MASK_CLASS;
        // 提取输入类型的变体（如密码、邮箱、短信等）
        int variation = inputType & InputType.TYPE_MASK_VARIATION;
        // 初始化键盘布局标识符
        String keyboard = null;

        // 根据输入类型的主类进行分支处理
        switch (inputClass) {
            // 数字、电话、日期时间类输入框
            case InputType.TYPE_CLASS_NUMBER:
            case InputType.TYPE_CLASS_PHONE:
            case InputType.TYPE_CLASS_DATETIME:
                // 启用临时 ASCII 模式，仅允许输入英文/数字字符
                mTempAsciiMode = true;
                // 切换到数字键盘布局
                keyboard = "number";
                break;

            // 文本类输入框
            case InputType.TYPE_CLASS_TEXT:
                // 如果是短消息类型，将回车键行为设置为换行
                if (variation == InputType.TYPE_TEXT_VARIATION_SHORT_MESSAGE) {
                    enterAsLineBreak = true;
                }
                // 如果是邮箱、密码、可见密码、网页邮箱或网页密码类型
                if (variation == InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS
                        || variation == InputType.TYPE_TEXT_VARIATION_PASSWORD
                        || variation == InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD
                        || variation == InputType.TYPE_TEXT_VARIATION_WEB_EMAIL_ADDRESS
                        || variation == InputType.TYPE_TEXT_VARIATION_WEB_PASSWORD) {
                    // 启用临时 ASCII 模式，避免中文输入法干扰密码或邮箱输入
                    mTempAsciiMode = true;
                    // 切换到纯英文键盘布局
                    keyboard = "ascii";
                    // 将输入类重置为 0，防止后续逻辑误判为可组合输入
                    inputClass = 0;
                } else {
                    // 其他文本类型允许中文组合输入
                    canCompose = true;
                    // 使用当前 Rime 输入法方案对应的键盘布局
                    keyboard = Rime.getCurrentRimeSchema();
                }
                break;

            // 默认情况（包括输入类型为 0 或其他未明确分类的情况）
            default:
                // 如果输入类型大于 0，则允许组合输入（例如某些特殊文本框）
                // 注释提及 0x80000 可能对应 FX 文件重命名等特殊场景
                canCompose = (inputType > 0);
                // 默认使用当前 Rime 输入法方案对应的键盘布局
                keyboard = Rime.getCurrentRimeSchema();
                break;
        }

        // 如果设置了重置 ASCII 模式的标志，则强制关闭 ASCII 模式
        if (reset_ascii_mode) mAsciiMode = false;

        // 在调试模式下打印日志，记录当前选择的键盘布局
        if (BuildConfig.DEBUG)
            android.util.Log.i(TAG, "onStartInput: " + keyboard);

        // 如果键盘布局不为空且与上一次使用的键盘不同，则切换键盘
        if (!TextUtils.isEmpty(keyboard) && !keyboard.equals(mLastInputClass))
            setKeyboard(keyboard);
        else
            // 否则更新 Rime 选项以同步状态
            updateRimeOption();

        // 记录当前使用的键盘布局，用于下次比较
        mLastInputClass = keyboard;

        // 最终确认是否允许组合输入：必须之前标记为允许，且当前 Rime 方案不为空
        canCompose = canCompose && !Rime.getCurrentRimeSchema().isEmpty();

        // 调用 Lua 脚本中的 onStartInput 钩子函数，传递编辑器属性和重启标志
        ThemeManager.callFunction("onStartInput", attribute, restarting);
    }

    /**
     * 发送事件字符串，包装为 Event 对象后处理。
     *
     * @param s 事件字符串。
     */
    public void sendEvent(String s) {
        onEvent(new Event(s));
    }

    /**
     * 发送 Lua 值事件，包装为 Event 对象后处理。
     *
     * @param s Lua 值对象。
     */
    public void sendEvent(LuaValue s) {
        onEvent(new Event(s));
    }

    // 5. 事件处理逻辑 (Event & Key Handling)

    /**
     * 处理自定义事件。
     * <p>
     * 该方法是输入法内部事件分发的核心入口，用于处理来自键盘布局定义（如 YAML 配置）或 Lua 脚本触发的各种动作。
     * 它根据事件类型（直接提交文本、发送文本、执行按键代码等）执行相应的操作。
     *
     * @param event 要处理的事件对象，包含提交内容、文本、按键码、修饰键状态、命令等信息。
     */
    public void onEvent(Event event) {
        // 调试日志：打印事件整体信息
        if (BuildConfig.DEBUG) android.util.Log.w(TAG, "onEvent: " + event);
        // 调试日志：打印事件按键码
        if (BuildConfig.DEBUG) android.util.Log.w(TAG, "onEvent:1 " + event.getCode());
        // 调试日志：打印事件修饰键掩码
        if (BuildConfig.DEBUG) android.util.Log.w(TAG, "onEvent:2 " + event.getMask());

        // 0. 处理候选词选择
        // 如果事件中包含了候选词选择索引（send为数值），则直接选择对应位置的候选词
        int selectCandidate = event.getSelectCandidate();
        if (BuildConfig.DEBUG) android.util.Log.w(TAG, "onEvent:selectCandidate " + selectCandidate);
        if (selectCandidate > 0) {
            selectPagedCandidate(selectCandidate - 1);
            return;
        }

        // 1. 处理直接提交文本的情况
        // 如果事件中包含了需要直接提交的文本（例如点击候选词上屏），则优先处理
        String commitText = event.getCommit();
        if (BuildConfig.DEBUG) android.util.Log.w(TAG, "onEvent:3 " + commitText);
        if (!TextUtils.isEmpty(commitText)) {
            // 提交文本并清空当前的编码组合状态
            commitTextAndClearComposition(commitText);
            return;
        }

        // 2. 处理发送普通文本的情况
        // 如果事件中包含了需要模拟输入的文本字符串
        String textToSend = event.getText();
        if (BuildConfig.DEBUG) android.util.Log.w(TAG, "onEvent:4 " + textToSend);
        if (!TextUtils.isEmpty(textToSend)) {
            // 调用 onText 方法处理文本输入，该方法支持解析特殊字符和转义序列
            onText(textToSend);
        }
        // 3. 处理按键代码事件
        else if (event.getCode() > 0) {
            int keyCode = event.getCode();
            switch (keyCode) {
                // --- 字符集切换/中英文切换 ---
                case KeyEvent.KEYCODE_SWITCH_CHARSET:
                    // 先提交当前已输入的文本
                    commitText();
                    // 切换 Rime 运行时选项（如 ascii_mode）
                    mRime.toggleRuntimeOption(event.getToggle());
                    break;

                // --- 英数键 (Eisu) ---
                case KeyEvent.KEYCODE_EISU:
                    // 切换到指定的键盘布局
                    setKeyboard(event.getSelect());
                    break;

                // --- 语言切换键 ---
                case KeyEvent.KEYCODE_LANGUAGE_SWITCH:
                    IBinder imeToken = getToken();
                    InputMethodManager imm = (InputMethodManager) getSystemService(INPUT_METHOD_SERVICE);
                    if (".next".equals(event.getSelect())) {
                        // 切换到下一个输入法
                        imm.switchToNextInputMethod(imeToken, false);
                    } else if (!TextUtils.isEmpty(event.getSelect())) {
                        // 切换到上一个输入法
                        imm.switchToLastInputMethod(imeToken);
                    } else {
                        // 显示系统输入法选择器
                        ((InputMethodManager) getSystemService(INPUT_METHOD_SERVICE)).showInputMethodPicker();
                    }
                    break;

                // --- 功能键 (Function Key)，通常用于执行 Lua 脚本或特定命令 ---
                case KeyEvent.KEYCODE_FUNCTION:
                    String command = event.getCommand();
                    String option = event.getOption();

                    // 处理候选词过滤命令
                    if ("filter".equals(command)) {
                        if ("char".equals(option)) {
                            // 切换字符过滤模式
                            CandidatesManager.toggleFilterChar();
                        } else {
                            // 根据笔画或其他条件过滤候选词
                            CandidatesManager.filterStroke(option, event.getLabel());
                        }
                        // 刷新候选词显示
                        filterCandidate();
                    }
                    // 处理 Lua 脚本调用（无参数情况）
                    else if (command.endsWith(".lua") && TextUtils.isEmpty(option)) {
                        // 获取上下文文本作为参数传递给 Lua 函数
                        textToSend = Function.handle(this, command,
                                getActiveText(1), // 选中文本或最后提交文本
                                getActiveText(2), // Rime 原始输入
                                getActiveText(3), // 光标前1个字符
                                getActiveText(4)  // 光标前1024个字符
                        );
                    }
                    // 处理格式化提交命令
                    else if ("commit".equals(command)) {
                        // 将选项字符串格式化后作为文本提交
                        textToSend = String.format(option,
                                getActiveText(1),
                                getActiveText(2),
                                getActiveText(3),
                                getActiveText(4)
                        );
                    }
                    // 处理部署命令
                    else if ("deploy".equals(command)) {
                        // 显示部署弹窗
                        showDeployDialog();
                    }
                    // 如果命令执行后返回了文本，则提交该文本
                    if (textToSend != null) {
                        commitText(textToSend);
                    }
                    break;

                // --- 语音助手键 ---
                case KeyEvent.KEYCODE_VOICE_ASSIST:
                    // 启动语音输入模块
                    if (mSpeech != null) {
                        mSpeech.start();
                    }
                    break;

                // --- 设置键 ---
                case KeyEvent.KEYCODE_SETTINGS:
                    String settingsOption = event.getOption();
                    if (settingsOption == null) settingsOption = "";

                    switch (settingsOption) {
                        case "theme":
                            // 切换主题
                            if (!TextUtils.isEmpty(event.getSelect())) {
                                setTheme(event.getSelect());
                            } else {
                                showThemeDialog();
                            }
                            break;
                        case "color":
                            // 切换配色样式
                            if (!TextUtils.isEmpty(event.getSelect())) {
                                setStyle(event.getSelect());
                            } else {
                                showColorDialog();
                            }
                            break;
                        case "schema":
                            // 切换输入方案
                            if (!TextUtils.isEmpty(event.getSelect())) {
                                mRime.selectSchema(event.getSelect());
                            } else {
                                showSchemaDialog();
                            }
                            break;
                        case "group":
                            // 切换方案组
                            if (!TextUtils.isEmpty(event.getSelect())) {
                                Config.setGroup(event.getSelect());
                                restart(); // 重启服务以应用更改
                            } else {
                                showSchemaGroupDialog();
                            }
                            break;
                        case "app":
                            try {
                                Intent intent = getPackageManager().getLaunchIntentForPackage(getPackageName());
                                if (intent != null) {
                                    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_NO_HISTORY);
                                    startActivity(intent);
                                }
                            } catch (Exception ex) {
                                Log.e(TAG, "Start App Exception: " + ex);
                            }
                            break;
                        default:
                            // 默认显示偏好设置对话框
                            Function.showPrefDialog(this);
                            break;
                    }
                    break;

                // --- 红色编程键 (Prog Red) ---
                case KeyEvent.KEYCODE_PROG_RED:
                    // 快捷显示配色对话框
                    showColorDialog();
                    break;

                // --- 菜单键 ---
                case KeyEvent.KEYCODE_MENU:
                    // 显示选项对话框
                    new OptionsDialog(this).show(getToken());
                    break;

                // --- 默认处理：作为普通按键事件处理 ---
                default:
                    onKey(event.getCode(), event.getMask());
                    break;
            }
        }
    }

    /**
     * 处理按键抬起事件。
     * 主要用于控制语音输入的状态（停止或取消）。
     *
     * @param keyCode 键码。
     */
    public void onUp(int keyCode) {
        if (BuildConfig.DEBUG)
            android.util.Log.i(TAG, "onUp: " + keyCode + ":" + mSpeech.getState());
        if (mSpeech != null) {
            if (mSpeech.getState() == Speech.STATE_BEGIN)
                mSpeech.stop();
            else if (mSpeech.getState() == Speech.STATE_READY || mSpeech.getState() == Speech.STATE_START)
                mSpeech.cancel();
        }
    }

    /**
     * 处理按键按下事件。
     * 如果是符号键则直接提交，否则发送完整的按下-抬起事件序列。
     *
     * @param keyCode Android 键码。
     * @param mask 修饰键状态掩码。
     */
    public void onKey(int keyCode, int mask) {
        if (BuildConfig.DEBUG) android.util.Log.w(TAG, "onKey: " + keyCode);
        final boolean wasPredicting = keyCode == KeyEvent.KEYCODE_DEL
                && (hasPredictionPlaceholder(Rime.getRimeRawInput())
                || isPredicting()
                || mPredictionCandidatesVisible);
        boolean handled = handleKey(keyCode, mask);
        if (wasPredicting) {
            InputConnection ic = getCurrentInputConnection();
            if (ic != null) ic.deleteSurroundingText(1, 0);
            return;
        }
        if (handled) {
            return;
        }
        if (keyCode >= Key.getSymbolStart()) {
            keyUpNeeded = false;
            commitText(Event.getDisplayLabel(keyCode));
            return;
        }
        keyUpNeeded = false;
        sendDownUpKeyEvents(keyCode, mask);
    }

    /**
     * 核心按键处理方法。
     * 依次尝试：Rime 引擎处理 -> 快捷键处理 -> 菜单键处理 -> Enter 键处理 -> 返回键处理 -> 系统分类打开。
     *
     * @param keyCode 键码。
     * @param mask 修饰键状态掩码。
     * @return true 如果已处理，false 否则。
     */
    private boolean handleKey(int keyCode, int mask) {
        keyUpNeeded = false;
        //if(keyCode==KeyEvent.KEYCODE_DPAD_LEFT&&mRootInputView.prevCandidate())
        //    return true;
        //if(keyCode==KeyEvent.KEYCODE_DPAD_RIGHT&&mRootInputView.nextCandidate())
        //    return true;
        if (onRimeKey(Event.getRimeEvent(keyCode, mask))) {
            keyUpNeeded = true;
        } else if (handleAction(keyCode, mask) || handleOption(keyCode) || handleEnter(keyCode) || handleBack(keyCode)) {
            // Handled
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.ICE_CREAM_SANDWICH_MR1 && Function.openCategory(this, keyCode)) {
            // Handled
        } else {
            keyUpNeeded = true;
            return false;
        }
        return true;
    }

    /**
     * 处理文本输入。
     * 支持单个字符模拟按键和复杂文本解析（包含转义序列和事件命令）。
     *
     * @param text 要输入的文本。
     */
    public void onText(CharSequence text) {
        String s = text.toString();
        if (!isAsciiPrintable(s.charAt(0))) {
            commitText();
        }
        if (s.length() == 1) {
            mRime.simulateKeySequence(s);
            return;
        }

        String t;
        Pattern p = Pattern.compile("^(\\{[^{}]+\\}).*$");
        Pattern pText = Pattern.compile("^((\\{Escape\\})?[^{}]+).*$");
        Matcher m;
        while (!s.isEmpty()) {
            m = pText.matcher(s);
            if (m.matches()) {
                t = m.group(1);
                if (!isAsciiPrintable(t.charAt(0))) {
                    commitText(t);
                } else {
                    mRime.simulateKeySequence(t);
                }
            } else {
                m = p.matcher(s);
                t = m.matches() ? m.group(1) : s.substring(0, 1);
                onEvent(new Event(t));
            }
            s = s.substring(t.length());
        }
        keyUpNeeded = false;
    }

    /**
     * 检查字符是否为 ASCII 可见字符 (码点在 32 到 126 之间)
     * 包含空格、数字、字母、标准标点符号。
     */
    public static boolean isAsciiPrintable(char ch) {
        return ch >= 32 && ch < 127;
    }

    // 6. 文本提交与 Rime 消息处理 (Text Commitment & Rime Logic)
    /**
     * 提交文本到输入框。
     *
     * @param text 要提交的文本内容。
     */
    public void commitText(CharSequence text) {
        if (TextUtils.isEmpty(text)) return;
        text = stripPredictionPlaceholder(text.toString());
        if (TextUtils.isEmpty(text)) return;
        lastCommittedText = text;
        InputConnection ic = getCurrentInputConnection();
        if (ic != null) ic.commitText(text, 1);
    }

    /** 去除预测占位符（{@link #PREDICTION_PLACEHOLDER}），避免其原样上屏或显示在编码区。 */
    private static String stripPredictionPlaceholder(String s) {
        if (s == null || !s.contains(PREDICTION_PLACEHOLDER)) return s;
        return s.replace(PREDICTION_PLACEHOLDER, "");
    }

    /** 判断字符串是否包含预测占位符。 */
    private static boolean hasPredictionPlaceholder(String s) {
        return !TextUtils.isEmpty(s) && s.contains(PREDICTION_PLACEHOLDER);
    }

    private void setPredictionCandidatesVisible(boolean visible) {
        if (mPredictionCandidatesVisible == visible) return;
        mPredictionCandidatesVisible = visible;
    }

    /**
     * 提交文本并清空编码区。
     *
     * @param text 要提交的文本内容。
     */
    public void commitTextAndClearComposition(CharSequence text) {
        commitText(text);
        mRime.clearComposition();
    }

    /**
     * 提交当前编码的文本并清空编码区。
     *
     * @return 始终返回 false（保持原有逻辑）。
     */
    private boolean commitText() {
        if (isComposing()) {
            String text = stripPredictionPlaceholder(mRime.getComposingText());
            if (!TextUtils.isEmpty(text)) {
                commitText(text);
            }
            mRime.clearComposition();
        }
        return false; // 原有逻辑返回 false
    }

    /**
     * 将按键事件传递给 Rime 引擎处理。
     *
     * @param event Rime 事件数组 [键码, 修饰键状态]。
     * @return true 如果 Rime 引擎处理了该事件，false 否则。
     */
    private boolean onRimeKey(int[] event) {
        if (event[0] == RimeKey_VoidSymbol)
            return false;
        boolean ret = mRime.processKey(event[0], event[1]);
        Log.w(TAG, "onRimeKey: " + ret);
        //commitText();
        return ret;
    }


    /**
     * 判断是否应该将实体键盘事件交给 Rime 引擎组合输入。
     * 过滤掉 Menu 键、符号键，特殊处理修饰键。
     *
     * @param event 键盘事件对象。
     * @return true 如果应该组合输入，false 否则。
     */
    private boolean composeEvent(KeyEvent event) {
        int keyCode = event.getKeyCode();
        if (keyCode == KeyEvent.KEYCODE_MENU) return false; //不处理Menu键
        if (keyCode >= Key.getSymbolStart()) return false; //只处理安卓标准按键
        if (event.getRepeatCount() == 0 && KeyEvent.isModifierKey(keyCode)) {
            boolean ret =
                    onRimeKey(
                            Event.getRimeEvent(
                                    keyCode, event.getAction() == KeyEvent.ACTION_DOWN ? 0 : Rime.META_RELEASE_ON));
            if (isComposing()) setCandidatesViewShown(canCompose); //蓝牙键盘打字时显示候选栏
            return ret;
        }
        if (!canCompose || Rime.isVoidKeycode(keyCode)) return false;
        if (BuildConfig.DEBUG) android.util.Log.w(TAG, "onKeyDown:4 " + keyCode);
        return true;
    }

    /**
     * 处理按键释放事件。
     * 如果需要发送键释放事件，则通知 Rime 引擎。
     *
     * @param keyCode 键码。
     */
    public void onRelease(int keyCode) {
        if (BuildConfig.DEBUG) android.util.Log.i(TAG, "onRelease: " + keyCode);
        if (keyUpNeeded) {
            onRimeKey(Event.getRimeEvent(keyCode, Rime.META_RELEASE_ON));
        }
    }

    /**
     * 处理实体键盘的按键按下事件。
     * 支持方向键选择候选词、修饰键组合等。
     *
     * @param keyCode Android 键码。
     * @param event 键盘事件对象。
     * @return true 如果已处理，false 交给系统处理。
     */
    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        //Log.info("onKeyDown=" + event);
        if (BuildConfig.DEBUG) android.util.Log.w(TAG, "onKeyDown: " + keyCode);
        if (keyCode == KeyEvent.KEYCODE_DPAD_CENTER) {
            try {
                if (Rime.isComposing()) {
                    selectCandidate(Rime.getHighlightRimeCandidate());
                    return true;
                } else {
                    onKey(KeyEvent.KEYCODE_ENTER, 0);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        final boolean wasPredicting = keyCode == KeyEvent.KEYCODE_DEL
                && (hasPredictionPlaceholder(Rime.getRimeRawInput())
                || isPredicting()
                || mPredictionCandidatesVisible);
        if (composeEvent(event) && onKeyEvent(event)) {
            if (BuildConfig.DEBUG) android.util.Log.w(TAG, "onKeyDown:2 " + keyCode);
            if (wasPredicting) {
                InputConnection ic = getCurrentInputConnection();
                if (ic != null) ic.deleteSurroundingText(1, 0);
            }
            return true;
        }
        if (wasPredicting) {
            InputConnection ic = getCurrentInputConnection();
            if (ic != null) ic.deleteSurroundingText(1, 0);
            return true;
        }
        return super.onKeyDown(keyCode, event);
    }

    /**
     * 处理实体键盘的按键抬起事件。
     *
     * @param keyCode Android 键码。
     * @param event 键盘事件对象。
     * @return true 如果已处理，false 交给系统处理。
     */
    @Override
    public boolean onKeyUp(int keyCode, KeyEvent event) {
        //Log.info("onKeyUp=" + event);
        if (composeEvent(event) && keyUpNeeded) {
            onRelease(keyCode);
            return true;
        }
        return super.onKeyUp(keyCode, event);
    }

    /**
     * 处理实体键盘事件的核心逻辑。
     * 包括：非编码状态下的特殊键过滤、Ctrl 快捷键处理、Unicode 字符转换等。
     *
     * @param event 键盘事件对象。
     * @return true 如果已处理，false 否则。
     */
    private boolean onKeyEvent(KeyEvent event) {
        //Log.info("onKeyEvent=" + event);
        int keyCode = event.getKeyCode();

        boolean ret = true;
        keyUpNeeded = isComposing();

        if (!isComposing()) {
            if (keyCode == KeyEvent.KEYCODE_DEL
                    || keyCode == KeyEvent.KEYCODE_ENTER
                    || keyCode == KeyEvent.KEYCODE_ESCAPE
                    || keyCode == KeyEvent.KEYCODE_BACK) {
                return false;
            }
        }/* else if (keyCode == KeyEvent.KEYCODE_BACK) {
            if (mCandidatesViewShown || mInputViewShown)
                keyCode = KeyEvent.KEYCODE_ESCAPE; //返回键清屏
            else
                return false;
        }*/

        if (event.getAction() == KeyEvent.ACTION_DOWN
                && event.isCtrlPressed()
                && event.getRepeatCount() == 0
                && !KeyEvent.isModifierKey(keyCode)) {
            if (handleAction(keyCode, event.getMetaState())) return true;
        }

        int c = event.getUnicodeChar();
        String s = String.valueOf((char) c);
        int mask = 0;
        int i = Event.getClickCode(s);
        if (i > 0) {
            keyCode = i;
        } else { //空格、回車等
            mask = event.getMetaState();
        }
        ret = handleKey(keyCode, mask);
        if (BuildConfig.DEBUG) android.util.Log.w(TAG, "onKeyDown:3 " + keyCode + ret);
        if (isComposing()) setCandidatesViewShown(canCompose); //蓝牙键盘打字时显示候选栏
        return ret;
    }


    // Rime 消息重试计数器
    private int idx = 0;


    /**
     * 处理来自 Rime 引擎的消息。
     * <p>
     * 该方法是 Rime 消息回调的核心入口，负责根据接收到的消息类型更新输入法界面状态、
     * 提交文本、切换方案或刷新候选词等。所有 UI 更新操作均在此处分发。
     *
     * @param message Rime 引擎发送的消息对象，包含不同类型的状态或数据变更通知。
     */
    private void handleRimeMessage(RimeMessage<?> message) {
        // 记录调试日志：打印消息类型和数据内容
        Log.w("rime", "handleRimeMessage:1 " + message.getClass() + ":" + message.getData());

        // 1. 处理文本提交消息
        // 当 Rime 引擎确定需要上屏一段文本时（如用户选择候选词或确认输入），触发此分支
        if (message instanceof RimeMessage.CommitTextMessage) {
            // 获取消息中的文本数据并直接提交到输入框
            commitText(((RimeMessage.CommitTextMessage) message).data.getText());
        }
        // 2. 处理输入方案切换消息
        // 当用户切换输入法方案（如从拼音切换到五笔）时触发
        else if (message instanceof RimeMessage.SchemaMessage) {
            // 获取新方案的 ID，并通知根视图更新键盘布局和状态显示
            mRootInputView.setSchema(((RimeMessage.SchemaMessage) message).getData().getId());
            initInlinePreedit();
        }
        // 3. 处理部署（同步/编译配置）完成消息
        // 当用户执行“部署”操作，Rime 重新加载配置文件后触发
        else if (message instanceof RimeMessage.DeployMessage) {
            RimeMessage.DeployMessage msg = (RimeMessage.DeployMessage) message;
            // 仅当部署成功时才进行后续处理
            if (msg.getData() == RimeMessage.DeployMessage.State.Success) {
                // 重置重试计数器
                idx = 0;
                // 由于部署后 Rime 引擎可能需要短暂时间初始化当前方案，
                // 这里使用延迟递归调用来轮询获取有效的方案 ID
                mHandler.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        // 如果重试次数超过 20 次（约 200ms），则放弃等待，防止死循环
                        if (idx++ > 20)
                            return;

                        // 检查当前是否已获取到有效的方案 ID
                        if (TextUtils.isEmpty(Rime.getCurrentRimeSchema())) {
                            // 如果为空，继续延迟 10ms 后再次尝试
                            mHandler.postDelayed(this, 10);
                        } else {
                            // 获取到方案 ID 后，更新根视图的键盘布局
                            mRootInputView.setSchema(Rime.getCurrentRimeSchema());
                            initInlinePreedit();
                        }
                    }
                }, 10);
            }
        }
        // 4. 处理编码区更新消息
        // 当用户输入按键导致预编辑字符串（高亮部分）发生变化时触发
        else if (message instanceof RimeMessage.CompositionMessage) {
            // 更新编码区显示的文本
            RimeProto.Context.Composition composition = ((RimeMessage.CompositionMessage) message).getData();
            updateComposing(composition);
            String rawInput = Rime.getRimeRawInput();
            String preedit = composition != null ? composition.getPreedit() : null;
            boolean predictionVisible = hasPredictionPlaceholder(rawInput) || hasPredictionPlaceholder(preedit);
            setPredictionCandidatesVisible(predictionVisible);
        }
        // 5. 处理候选词列表更新消息
        // 当候选词列表发生变化（如翻页、新候选词出现）时触发
        else if (message instanceof RimeMessage.CandidateMenuMessage || message instanceof RimeMessage.CandidateListMessage) {
            // 刷新候选词视图
            updateCandidate();
        }
        // 6. 处理选项状态变更消息
        // 当 Rime 内部选项（如 ASCII 模式、悬浮模式等）发生改变时触发
        else if (message instanceof RimeMessage.OptionMessage) {
            RimeMessage.OptionMessage msg = (RimeMessage.OptionMessage) message;
            String optionName = msg.getData().getOption();
            boolean optionValue = msg.getData().isValue();

            // 根据具体的选项名称执行相应的 UI 更新
            if ("ascii_mode".equals(optionName)) {
                // 切换中西文模式（ASCII 模式）
                mRootInputView.setAsciiMode(optionValue);
                // 更新 Rime 选项以同步其他相关状态
                updateRimeOption();
            } else if ("small_mode".equals(optionName)) {
                // 切换小屏模式
                mRootInputView.setSmallMode(optionValue);
            } else if ("float_mode".equals(optionName)) {
                // 切换悬浮键盘模式
                mRootInputView.setFloatMode(optionValue);
            } else if ("_hide_key_sound".equals(optionName)) {
                // 切换按键音效，需在主线程刷新 UI
                mHandler.post(mRimeOptionRunnable);
            } else {
                // 对于其他选项，包括 ascii_punct 等开关选项，也需要更新UI
                // 这些选项会影响工具栏开关的状态显示
                updateRimeOption();
            }
        }
        // 7. 处理状态栏状态消息
        // 当输入法整体状态（如是否正在编码、中英文状态等）发生变化时触发
        else if (message instanceof RimeMessage.StatusMessage) {
            RimeProto.Status status = ((RimeMessage.StatusMessage) message).getData();
            // 更新状态栏图标和指示器
            updateStatus(status);
        }
    }

    private boolean mComposing;
    // 1. 复用 Runnable，避免 GC 压力
    private final Runnable mStatusRunnable = new Runnable() {
        @Override
        public void run() {
            // 在执行时再次获取最新的状态，确保 UI 与数据同步
            boolean isComp = mComposing;
            boolean hasCandidates = Rime.hasMenu();
            showToolbarView(!isComp && !hasCandidates);
            mRootInputView.invalidateComposingKeys();
        }
    };

    private void updateStatus(RimeProto.Status status) {
        boolean isComposing = status.isComposing();

        // 2. 状态预判：如果状态没变，直接拦截，不往主线程 post 消息
        if (mComposing == isComposing) {
            return;
        }

        // 3. 更新状态变量（放在 post 之前）
        mComposing = isComposing;

        // 4. 防抖处理：撤回旧任务，确保队列里只有一个最新的状态切换任务
        mHandler.removeCallbacks(mStatusRunnable);

        // 5. 延迟/异步执行
        // 如果对实时性要求极高，用 post；如果怕连续抖动，用 postDelayed(mStatusRunnable, 10)
        mHandler.post(mStatusRunnable);
    }

    private void updateComposing(RimeProto.Context.Composition data) {
        String preedit = stripPredictionPlaceholder(data.getPreedit());
        setComposingText(preedit);
        mHandler.post(this::updateComposing);
    }

    private void initInlinePreedit() {
        String schemaId = Rime.getCurrentRimeSchema();
        if (TextUtils.isEmpty(schemaId)) return;
        try (RimeConfig config = RimeConfig.openSchema(schemaId)) {
            String preeditType = config.getString("style/preedit_type");
            if (TextUtils.isEmpty(preeditType)) return;
            switch (preeditType) {
                case "preview":
                    inlinePreedit = InlineModeType.INLINE_PREVIEW;
                    break;
                case "composition":
                    inlinePreedit = InlineModeType.INLINE_COMPOSITION;
                    break;
                case "input":
                    inlinePreedit = InlineModeType.INLINE_INPUT;
                    break;
                default:
                    inlinePreedit = InlineModeType.INLINE_NONE;
            }
        } catch (Exception e) {
            Log.w(TAG, "initInlinePreedit: " + e.getMessage());
        }
    }

    public void updateComposing() {
        InputConnection ic = getCurrentInputConnection();
        if (inlinePreedit != InlineModeType.INLINE_NONE) {
            String s = null;
            switch (inlinePreedit) {
                case INLINE_PREVIEW:
                    s = mRime.getComposingText();
                    break;
                case INLINE_COMPOSITION:
                    s = mRime.getCompositionCached().getPreedit();
                    break;
                case INLINE_INPUT:
                    s = Rime.getRimeRawInput();
                    break;
            }
            if (s == null) s = "";
            s = stripPredictionPlaceholder(s);
            if (ic != null) {
                CharSequence cs = ic.getSelectedText(0);
                if (cs == null || !TextUtils.isEmpty(s)) ic.setComposingText(s, 1);
            }
        }
    }

    // 7. 视图状态管理与更新 (View State Management)

    /**
     * 设置主题。
     * 如果正在编码，先取消编码，然后重新加载主题配置。
     *
     * @param theme 主题名称。
     */
    public void setTheme(String theme) {
        if (Rime.isComposing()) {
            onKey(KeyEvent.KEYCODE_ESCAPE, 0);
            mRime.clearComposition();
        }
        ThemeManager.setTheme(theme);
        mRootInputView.setTheme(theme);
        //setInputView(onCreateInputView());
        //showToolbarView(true);
    }

    /**
     * 设置样式（配色方案）。
     * 如果正在编码，先取消编码，然后重新加载样式配置。
     *
     * @param theme 样式名称。
     */
    public void setStyle(String theme) {
        if (Rime.isComposing()) {
            onKey(KeyEvent.KEYCODE_ESCAPE, 0);
            mRime.clearComposition();
        }
        ThemeManager.setStyle(theme);
        mRootInputView.setStyle(theme);
        //setInputView(onCreateInputView());
    }

    /**
     * 显示或隐藏提取的候选词视图（全屏模式）。
     *
     * @param b true 显示，false 隐藏。
     */
    public void showExtractedCandidatesView(boolean b) {
        mRootInputView.showExtractedCandidatesView(b);
        mShowExtractedCandidatesView = b;
        //updateCandidate();
    }

    /**
     * 显示或隐藏符号键盘视图。
     *
     * @param b true 显示，false 隐藏。
     */
    public void showSymbolsView(boolean b) {
        mRootInputView.showSymbolsView(b);

    }

    /**
     * 显示或隐藏自定义视图。
     *
     * @param keyboardView 自定义视图对象，null 表示隐藏。
     */
    public void showCustomView(View keyboardView) {
        mRootInputView.showCustomView(keyboardView);
    }

    /**
     * 更新候选词显示。
     * 委托给 RootInputView 处理。
     */
    private void updateCandidate() {
        mRootInputView.updateCandidate();
    }

    /**
     * 过滤候选词显示。
     * 委托给 RootInputView 处理。
     */
    private void filterCandidate() {
        mRootInputView.filterCandidate();
    }

    /**
     * 设置编码区文本。
     *
     * @param s 预编辑文本字符串。
     */
    public void setComposingText(String s) {
        mRootInputView.setComposingText(s);
    }

    /**
     * 设置云输入结果文本。
     *
     * @param s 云输入结果字符串。
     */
    public void setCloudText(String s) {
        if (mRootInputView != null)
            mRootInputView.setCloudText(s);
    }

    /**
     * 设置键盘布局。
     *
     * @param id 键盘标识符。
     */
    public void setKeyboard(String id) {
        mRootInputView.setKeyboard(id);
    }

    /**
     * 显示自定义视图作为键盘。
     *
     * @param id 自定义视图对象。
     */
    public void setKeyboard(View id) {
        mRootInputView.showCustomView(id);
    }

    // 8. 输入法操作辅助 (Input Helpers)
    /**
     * 选择指定索引的候选词。
     *
     * @param index 候选词索引。
     */
    public void selectCandidate(int index) {
        mRime.selectCandidate(index);
    }

    /**
     * 选择分页后的候选词。
     *
     * @param index 分页后的候选词索引。
     */
    public void selectPagedCandidate(int index) {
        mRime.selectPagedCandidate(index);
    }

    /**
     * 选择 Rime 输入法方案。
     *
     * @param id 方案 ID。
     */
    public void selectRimeSchema(String id) {
        mRime.selectSchema(id);
    }

    /**
     * 重新部署 Rime 引擎。
     */
    public void deploy() {
        mRime.deploy();
    }

    /**
     * 判断是否正在编码（有预编辑文本）。
     *
     * @return true 如果正在编码，false 否则。
     */
    private boolean isComposing() {
        return Rime.isComposing();
    }

    /**
     * 判断当前是否处于"上屏后联想候选"状态（编码区仅为预测占位符 {@link #PREDICTION_PLACEHOLDER}）。
     * 用于区分"正在真实输入"与"上屏后候选栏靠占位符维持显示"这两种 isComposing()==true 的场景。
     *
     * @return true 如果正在展示联想候选，false 否则。
     */
    public boolean isPredicting() {
        RimeProto.Context.Composition composition = mRime.getCompositionCached();
        String preedit = composition != null ? composition.getPreedit() : null;
        return isComposing()
                && (hasPredictionPlaceholder(Rime.getRimeRawInput())
                || hasPredictionPlaceholder(mRime.getComposingText())
                || hasPredictionPlaceholder(preedit));
    }

    /**
     * 清除联想候选并关闭候选栏。
     * 仅在 {@link #isPredicting()} 为 true 时生效，供候选栏关闭按钮调用。
     */
    public void clearPredictionCandidates() {
        if (!isPredicting() && !mPredictionCandidatesVisible) return;
        setPredictionCandidatesVisible(false);
        mRime.clearComposition();
        setComposingText("");
        setCandidatesViewShown(false);
        showToolbarView(true);
    }

    /**
     * 判断当前是否处于 Shift 状态。
     *
     * @return true 如果 Shift 已按下，false 否则。
     */
    public boolean isShifted() {
        return mRootInputView.isShifted();
    }

    /**
     * 设置 Shift 状态。
     *
     * @param shifted true 表示按下 Shift，false 表示释放。
     */
    public void setShifted(boolean shifted) {
        ModifierState.setShifted(shifted);
        mRootInputView.setShifted(shifted);
    }

    private void onPickCandidate(int index) {
    }

    // 1. 复用 Runnable，减少内存抖动
    private final Runnable mRimeOptionRunnable = new Runnable() {
        @Override
        public void run() {
            mRootInputView.invalidateAllKeys();
        }
    };

    private void updateRimeOption() {
        // 2. 移除旧任务（去重）
        mHandler.removeCallbacks(mRimeOptionRunnable);

        // 3. 延迟一小段时间执行（节流）
        // 10ms-16ms 是一个合理的区间，可以合并极短时间内的多次请求
        mHandler.postDelayed(mRimeOptionRunnable, 10);
    }

    // 9. 按键与编辑操作辅助 (Key & Edit Helpers)
    /**
     * 处理 Ctrl 快捷键。
     * 支持：全选、复制、剪切、粘贴、撤销、重做、分享、纯文本粘贴等。
     *
     * @param code 键码。
     * @param mask 修饰键状态掩码。
     * @return true 如果已处理，false 否则。
     */
    private boolean handleAction(int code, int mask) {
        InputConnection ic = getCurrentInputConnection();
        if (ic == null) return false;
        if (Event.hasModifier(mask, KeyEvent.META_CTRL_ON)) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                if (code == KeyEvent.KEYCODE_V && Event.hasModifier(mask, KeyEvent.META_ALT_ON) && Event.hasModifier(mask, KeyEvent.META_SHIFT_ON))
                    return ic.performContextMenuAction(android.R.id.pasteAsPlainText);
                if (code == KeyEvent.KEYCODE_S && Event.hasModifier(mask, KeyEvent.META_ALT_ON)) {
                    if (ic.getSelectedText(0) == null)
                        ic.performContextMenuAction(android.R.id.selectAll);
                    return ic.performContextMenuAction(android.R.id.shareText);
                }
                if (code == KeyEvent.KEYCODE_Z) {
                    if (Event.hasModifier(mask, KeyEvent.META_SHIFT_ON))
                        return ic.performContextMenuAction(android.R.id.redo);

                    return ic.performContextMenuAction(android.R.id.undo);
                }
            }
            if (code == KeyEvent.KEYCODE_DEL && Event.hasModifier(mask, KeyEvent.META_SHIFT_ON)) {
                backToSentence();
                return true;
            }
            if (code == KeyEvent.KEYCODE_A)
                return ic.performContextMenuAction(android.R.id.selectAll);
            if (code == KeyEvent.KEYCODE_X) return ic.performContextMenuAction(android.R.id.cut);
            if (code == KeyEvent.KEYCODE_C) return ic.performContextMenuAction(android.R.id.copy);
            if (code == KeyEvent.KEYCODE_V) return ic.performContextMenuAction(android.R.id.paste);
        }
        return false;
    }

    /**
     * 删除到句子开头。
     * 从光标位置向前查找标点符号，删除到该位置。
     */
    public void backToSentence() {
        InputConnection ic = getCurrentInputConnection();
        if (ic == null) return;
        CharSequence text = ic.getTextBeforeCursor(1024, 0);
        if (TextUtils.isEmpty(text)) return;
        for (int i = text.length() - 1; i > 0; i--) {
            if (",.!?\n，。！？：:".indexOf(text.charAt(i)) != -1) {
                if (text.length() - i > 1) {
                    ic.deleteSurroundingText(text.length() - i - 1, 0);
                    return;
                }
            }
        }
        if (text.length() < 128) ic.deleteSurroundingText(text.length(), 0);
    }

    /**
     * 发送完整的按下-抬起键事件序列。
     * 包括修饰键（Shift、Ctrl、Alt）的按下和释放。
     *
     * @param keyCode 键码。
     * @param mask 修饰键状态掩码。
     */
    private void sendDownUpKeyEvents(int keyCode, int mask) {
        InputConnection ic = getCurrentInputConnection();
        if (ic == null) return;
        ic.clearMetaKeyStates(KeyEvent.META_FUNCTION_ON | KeyEvent.META_SHIFT_MASK | KeyEvent.META_ALT_MASK | KeyEvent.META_CTRL_MASK | KeyEvent.META_META_MASK | KeyEvent.META_SYM_ON);
        if (keyCode >= KeyEvent.KEYCODE_NUMPAD_0 && keyCode <= KeyEvent.KEYCODE_NUMPAD_EQUALS) {
            mask |= KeyEvent.META_NUM_LOCK_ON;
        }

        if (mRootInputView != null && mRootInputView.isShifted()) {
            if (keyCode == KeyEvent.KEYCODE_MOVE_HOME || keyCode == KeyEvent.KEYCODE_MOVE_END || keyCode == KeyEvent.KEYCODE_PAGE_UP || keyCode == KeyEvent.KEYCODE_PAGE_DOWN || (keyCode >= KeyEvent.KEYCODE_DPAD_UP && keyCode <= KeyEvent.KEYCODE_DPAD_RIGHT))
                mask |= KeyEvent.META_SHIFT_ON;
        }
        if (Event.hasModifier(mask, KeyEvent.META_SHIFT_ON))
            sendKeyDown(ic, KeyEvent.KEYCODE_SHIFT_LEFT, KeyEvent.META_SHIFT_ON | KeyEvent.META_SHIFT_LEFT_ON);
        if (Event.hasModifier(mask, KeyEvent.META_CTRL_ON))
            sendKeyDown(ic, KeyEvent.KEYCODE_CTRL_LEFT, KeyEvent.META_CTRL_ON | KeyEvent.META_CTRL_LEFT_ON);
        if (Event.hasModifier(mask, KeyEvent.META_ALT_ON))
            sendKeyDown(ic, KeyEvent.KEYCODE_ALT_LEFT, KeyEvent.META_ALT_ON | KeyEvent.META_ALT_LEFT_ON);
        sendKeyDown(ic, keyCode, mask);
        sendKeyUp(ic, keyCode, mask);
        if (Event.hasModifier(mask, KeyEvent.META_ALT_ON))
            sendKeyUp(ic, KeyEvent.KEYCODE_ALT_LEFT, KeyEvent.META_ALT_ON | KeyEvent.META_ALT_LEFT_ON);
        if (Event.hasModifier(mask, KeyEvent.META_CTRL_ON))
            sendKeyUp(ic, KeyEvent.KEYCODE_CTRL_LEFT, KeyEvent.META_CTRL_ON | KeyEvent.META_CTRL_LEFT_ON);
        if (Event.hasModifier(mask, KeyEvent.META_SHIFT_ON))
            sendKeyUp(ic, KeyEvent.KEYCODE_SHIFT_LEFT, KeyEvent.META_SHIFT_ON | KeyEvent.META_SHIFT_LEFT_ON);
    }

    /**
     * 发送按键按下事件。
     *
     * @param ic 输入连接对象。
     * @param key 键码。
     * @param meta 修饰键状态。
     */
    private void sendKeyDown(InputConnection ic, int key, int meta) {
        sendKey(ic, key, meta, KeyEvent.ACTION_DOWN);
    }

    /**
     * 发送按键抬起事件。
     *
     * @param ic 输入连接对象。
     * @param key 键码。
     * @param meta 修饰键状态。
     */
    private void sendKeyUp(InputConnection ic, int key, int meta) {
        sendKey(ic, key, meta, KeyEvent.ACTION_UP);
    }

    /**
     * 发送单个键事件（按下或抬起）。
     *
     * @param ic 输入连接对象。
     * @param key 键码。
     * @param meta 修饰键状态。
     * @param action 事件动作（ACTION_DOWN 或 ACTION_UP）。
     */
    private void sendKey(InputConnection ic, int key, int meta, int action) {
        long now = System.currentTimeMillis();
        if (action == KeyEvent.ACTION_UP) now += 10;
        if (ic != null) ic.sendKeyEvent(new KeyEvent(now, now, action, key, 0, meta));
    }

    /**
     * 处理菜单键。
     *
     * @param keyCode 键码。
     * @return true 如果是菜单键并已处理，false 否则。
     */
    private boolean handleOption(int keyCode) {
        if (keyCode == KeyEvent.KEYCODE_MENU) {
            new OptionsDialog(this).show(getToken());
            return true;
        }
        return false;
    }

    /**
     * 处理 Enter 键。
     * 根据 enterAsLineBreak 标志决定是提交换行符还是发送回车键。
     *
     * @param keyCode 键码。
     * @return true 如果是 Enter 键并已处理，false 否则。
     */
    private boolean handleEnter(int keyCode) {
        if (keyCode == KeyEvent.KEYCODE_ENTER) {
            if (enterAsLineBreak) commitText("\n");
            else sendKeyChar('\n');
            return true;
        }
        return false;
    }

    /**
     * 处理返回键或 Escape 键。
     * 隐藏输入法窗口。
     *
     * @param keyCode 键码。
     * @return true 如果是返回键或 Escape 键并已处理，false 否则。
     */
    private boolean handleBack(int keyCode) {
        if (keyCode == KeyEvent.KEYCODE_BACK || keyCode == KeyEvent.KEYCODE_ESCAPE) {
            Function.printStackTrace("back");
            requestHideSelf(0);
            return true;
        }
        return false;
    }

    /**
     * 取消编码（发送 Escape 键）。
     */
    private void escape() {
        if (isComposing()) onKey(KeyEvent.KEYCODE_ESCAPE, 0);
    }

    // 10. 对话框与测量辅助 (Dialog & Dimension Helpers)
    /**
     * 显示对话框，设置窗口类型为输入法附加对话框。
     * 宽度设置为屏幕宽度的 50%。
     *
     * @param dialog 要显示的对话框。
     * @return 显示后的对话框对象。
     */
    public AlertDialog showDialog(AlertDialog dialog) {
        Window window = dialog.getWindow();
        WindowManager.LayoutParams lp = window.getAttributes();
        lp.type = WindowManager.LayoutParams.TYPE_APPLICATION_ATTACHED_DIALOG;
        lp.token = getToken();
        window.setAttributes(lp);
        window.addFlags(WindowManager.LayoutParams.FLAG_ALT_FOCUSABLE_IM);
        dialog.show();
        window = dialog.getWindow();
        if (window != null) {
            lp = window.getAttributes();
            // 设置为屏幕宽度的 80%，避免撑满全屏
            DisplayMetrics dm = getResources().getDisplayMetrics();
            lp.width = (int) (dm.widthPixels * 0.5);
            window.setAttributes(lp);
        }
        return dialog;
    }

    /**
     * 显示宽度自适应的对话框。
     *
     * @param dialog 要显示的对话框。
     * @return 显示后的对话框对象。
     */
    public AlertDialog showWidthDialog(AlertDialog dialog) {
        Window window = dialog.getWindow();
        WindowManager.LayoutParams lp = window.getAttributes();
        lp.type = WindowManager.LayoutParams.TYPE_APPLICATION_ATTACHED_DIALOG;
        lp.token = getToken();
        window.setAttributes(lp);
        window.addFlags(WindowManager.LayoutParams.FLAG_ALT_FOCUSABLE_IM);
        window.clearFlags(WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE);

        dialog.show();
        window = dialog.getWindow();
        if (window != null) {
            lp = window.getAttributes();
            // 设置为屏幕宽度的 80%，避免撑满全屏
            DisplayMetrics dm = getResources().getDisplayMetrics();
            window.setAttributes(lp);
        }
        return dialog;
    }

    /**
     * 显示列表对话框。
     * 支持从 PrefLauncher 获取 Token，居中显示。
     *
     * @param dialog 要显示的对话框。
     * @return 显示后的对话框对象。
     */
    public AlertDialog showListDialog(AlertDialog dialog) {
        Window window = dialog.getWindow();
        WindowManager.LayoutParams lp = window.getAttributes();
        IBinder token = PrefLauncher.getToken();
        if (token != null) {
            lp.token = token;
        } else {
            lp.type = WindowManager.LayoutParams.TYPE_APPLICATION_ATTACHED_DIALOG;
            lp.token = getToken();
        }
        lp.gravity = Gravity.CENTER;
        window.setAttributes(lp);
        window.addFlags(WindowManager.LayoutParams.FLAG_ALT_FOCUSABLE_IM);
        try {
            dialog.show();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return dialog;
    }

    /**
     * 显示部署对话框。
     */
    private void showDeployDialog() {
        new DeployDialog(this).show(getToken());
    }

    /**
     * 显示方案组选择对话框。
     */
    private void showSchemaGroupDialog() {
        new SchemaGroupDialog(this).show(getToken());
    }

    /**
     * 显示方案选择对话框。
     */
    private void showSchemaDialog() {
        new OptionsDialog(this).show(getToken());
    }

    /**
     * 显示配色方案选择对话框。
     */
    private void showColorDialog() {
        new StyleDialog(this).show(getToken());
    }

    /**
     * 显示主题选择对话框。
     */
    private void showThemeDialog() {
        new ThemeDialog(this).show(getToken());
    }

    /**
     * 获取根视图的窗口 Token。
     *
     * @return IBinder Token 对象。
     */
    public IBinder getToken() {
        return mRootInputView.getRoot().getWindowToken();
    }

    /**
     * 获取屏幕高度（像素）。
     *
     * @return 屏幕高度。
     */
    public int getHeight() {
        return getResources().getDisplayMetrics().heightPixels;
    }

    /**
     * 获取状态栏高度（像素）。
     *
     * @return 状态栏高度，如果获取失败则返回 0。
     */
    public int getStatusBarHeight() {
        int result = 0;
        int resourceId = getResources().getIdentifier("status_bar_height", "dimen", "android");
        if (resourceId > 0) {
            result = getResources().getDimensionPixelSize(resourceId);
        }
        return result;
    }

    /**
     * 获取输入法窗口宽度。
     * 小屏模式或浮动模式下返回配置的宽度，否则返回最大宽度。
     *
     * @return 输入法窗口宽度（像素）。
     */
    public int getWidth() {
        if (Config.isSmallMode() || Config.isFloatMode())
            //if (Rime.getRimeOption("small_mode"))
            return Math.max(Math.min(Config.getSmallModeWidth(), (getMaxWidth())), (int) (getMaxWidth() * 0.2));
        return getMaxWidth();
    }

    /**
     * 获取视图在窗口中的位置。
     *
     * @param view 要获取位置的视图。
     * @return 包含 X 和 Y 坐标的数组 [x, y]。
     */
    public int[] getLocationInWindow(View view) {
        int[] ret = new int[2];
        view.getLocationInWindow(ret);
        return ret;
    }

    // 11. 文本获取辅助 (Text Extraction)
    /**
     * 获取活动文本，用于 Lua 脚本或命令处理。
     * 根据 type 参数返回不同的文本：
     * type=1: 选中文本或最后提交的文本
     * type=2: Rime 原始输入
     * type=3: 光标前1个字符
     * type=4: 光标前1024个字符
     *
     * @param type 文本类型。
     * @return 获取的文本字符串。
     */
    private String getActiveText(int type) {
        if (type == 2) return Rime.getRimeRawInput();
        String s = mRime.getComposingText();
        if (TextUtils.isEmpty(s)) {
            InputConnection ic = getCurrentInputConnection();
            if (ic == null) return "";
            CharSequence cs = ic.getSelectedText(0);
            if (type == 1 && TextUtils.isEmpty(cs)) cs = lastCommittedText;
            if (TextUtils.isEmpty(cs)) cs = ic.getTextBeforeCursor(type == 4 ? 1024 : 1, 0);
            if (TextUtils.isEmpty(cs)) cs = ic.getTextAfterCursor(1024, 0);
            if (cs != null) s = cs.toString();
        }
        return s;
    }

    /**
     * 获取光标后的文本（最多10240个字符）。
     *
     * @return 光标后的文本字符串。
     */
    private String getAfterText() {
        InputConnection ic = getCurrentInputConnection();
        if (ic == null) return "";
        CharSequence cs = ic.getTextAfterCursor(10240, 0);
        return cs != null ? cs.toString() : "";
    }

    /**
     * 获取光标前的文本（最多10240个字符）。
     *
     * @return 光标前的文本字符串。
     */
    private String getBeforeText() {
        InputConnection ic = getCurrentInputConnection();
        if (ic == null) return "";
        CharSequence cs = ic.getTextBeforeCursor(10240, 0);
        return cs != null ? cs.toString() : "";
    }

    /**
     * 获取光标前的一个字符。
     *
     * @return 光标前的字符字符串。
     */
    private String getBeforeChar() {
        InputConnection ic = getCurrentInputConnection();
        if (ic == null) return "";
        CharSequence cs = ic.getTextBeforeCursor(1, 0);
        return cs != null ? cs.toString() : "";
    }

    /**
     * 判断是否启用长按弹出功能。
     *
     * @return 始终返回 false（当前未实现）。
     */
    public boolean isLongPressPopup() {
        return false;
    }

    /**
     * 判断是否启用滑动点击功能。
     *
     * @return 始终返回 false（当前未实现）。
     */
    public boolean isKeySwipeTap() {
        return false;
    }

    /**
     * 获取回车键的动作标签。
     *
     * @return 动作标签字符串（如“搜索”、“发送”等）。
     */
    public String getActionLabel() {
        return mActionLabel;
    }

    /**
     * 执行 Lua 脚本文件（单参数版本）。
     * 支持从多个路径查找脚本文件：绝对路径、主题目录、脚本目录。
     *
     * @param path 脚本文件路径。
     * @param option 传递给脚本的参数。
     * @return 脚本执行结果，失败返回 null。
     */
    public Object doFile(String path, String option) {
        if (globals == null) {
            globals = JsePlatform.standardGlobals();
            globals.finder = new ResourceFinder() {
                @Override
                public InputStream findResource(String filename) {
                    File f = new File(filename);
                    if (f.exists()) {
                        try {
                            return new FileInputStream(f);
                        } catch (FileNotFoundException e) {
                            e.printStackTrace();
                        }
                    }
                    f = new File(Config.getThemeDir(Config.getTheme()), filename);
                    if (f.exists()) {
                        try {
                            return new FileInputStream(f);
                        } catch (FileNotFoundException e) {
                            e.printStackTrace();
                        }
                    }
                    f = new File(Config.getScriptsDir(), filename);
                    if (f.exists()) {
                        try {
                            return new FileInputStream(f);
                        } catch (FileNotFoundException e) {
                            e.printStackTrace();
                        }
                    }
                    return null;
                }

                @Override
                public String findFile(String filename) {
                    File f = new File(filename);
                    if (f.exists()) {
                        return f.getAbsolutePath();
                    }
                    f = new File(Config.getScriptsDir(), filename);
                    if (f.exists()) {
                        return f.getAbsolutePath();
                    }
                    return filename;
                }
            };
        }
        LuaTable env = new LuaTable();
        env.setmetamethod("__index", globals);
        try {
            return globals.loadfile(path).jcall(option);
        } catch (Exception e) {
            sendMsg(e.toString());
        }
        return null;
    }

    /**
     * 执行 Lua 脚本文件（多参数版本）。
     * 支持从多个路径查找脚本文件：绝对路径、脚本目录。
     *
     * @param path 脚本文件路径。
     * @param option 可变参数列表。
     * @return 脚本执行结果，失败返回 null。
     */
    public Object doFile(String path, Object... option) {
        if (globals == null) {
            globals = JsePlatform.standardGlobals();
            globals.finder = new ResourceFinder() {
                @Override
                public InputStream findResource(String filename) {
                    File f = new File(filename);
                    if (f.exists()) {
                        try {
                            return new FileInputStream(f);
                        } catch (FileNotFoundException e) {
                            e.printStackTrace();
                        }
                    }
                    f = new File(Config.getScriptsDir(), filename);
                    if (f.exists()) {
                        try {
                            return new FileInputStream(f);
                        } catch (FileNotFoundException e) {
                            e.printStackTrace();
                        }
                    }
                    return null;
                }

                @Override
                public String findFile(String filename) {
                    File f = new File(filename);
                    if (f.exists()) {
                        return f.getAbsolutePath();
                    }
                    f = new File(Config.getScriptsDir(), filename);
                    if (f.exists()) {
                        return f.getAbsolutePath();
                    }
                    return filename;
                }
            };
        }
        LuaTable env = new LuaTable();
        env.setmetamethod("__index", globals);
        try {
            return globals.loadfile(path).jcall(option);
        } catch (Exception e) {
            sendMsg(e.toString());
        }
        return null;
    }


    /**
     * 注册剪贴板监听事件。
     * 加载历史剪贴板和常用语，并注册系统剪贴板变化监听器。
     */
    private void registerClipEvents() {
        loadClipboard();
        loadPhrase();
        manager = (ClipboardManager) getSystemService(CLIPBOARD_SERVICE);
        if (manager == null)
            return;
        mOnPrimaryClipChangedListener = new ClipboardManager.OnPrimaryClipChangedListener() {
            @Override
            public void onPrimaryClipChanged() {
                try {
                    if (manager.hasPrimaryClip() && manager.getPrimaryClip().getItemCount() > 0) {
                        CharSequence addedText = manager.getPrimaryClip().getItemAt(0).getText();
                        if (!TextUtils.isEmpty(addedText)) {
                            String text = addedText.toString();
                            addClipboard(text);
                        }
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }

            }
        };
        manager.addPrimaryClipChangedListener(mOnPrimaryClipChangedListener);
    }

    /**
     * 注销剪贴板监听事件。
     */
    private void unregisterClipEvents() {
        if (manager == null)
            return;
        manager.removePrimaryClipChangedListener(mOnPrimaryClipChangedListener);
    }


    /**
     * 加载常用语列表。
     * 从 phrase.json 文件中读取。
     */
    public void loadPhrase() {
        mPhrase = JsonUtil.load(new File(Config.getDataDir(), "phrase.json"));
    }

    /**
     * 添加常用语。
     * 如果已存在则先删除，然后添加到列表开头，保持最多120条。
     *
     * @param text 要添加的文本。
     */
    public void addPhrase(String text) {
        if (mPhrase.contains(text))
            mPhrase.remove(text);
        mPhrase.add(0, text);
        for (int size = mPhrase.size() - 1; size >= 120; size--) {
            mPhrase.remove(size);
        }
        JsonUtil.save(new File(Config.getDataDir(), "phrase.json"), mPhrase);
    }

    /**
     * 删除指定索引的常用语。
     *
     * @param i 要删除的索引。
     */
    public void removePhrase(int i) {
        mPhrase.remove(i);
        JsonUtil.save(new File(Config.getDataDir(), "phrase.json"), mPhrase);
    }

    /**
     * 获取常用语列表。
     *
     * @return 常用语字符串列表。
     */
    public List<String> getPhrase() {
        return mPhrase;
    }

    /**
     * 加载剪贴板历史记录。
     * 从 clipboard.json 文件中读取。
     */
    private void loadClipboard() {
        mClipboard = JsonUtil.load(new File(Config.getDataDir(), "clipboard.json"));
    }

    /**
     * 添加剪贴板记录。
     * 如果已存在则先删除，然后添加到列表开头，保持最多 mClipboardSize 条。
     *
     * @param text 要添加的文本。
     */
    public void addClipboard(String text) {
        if (mClipboard.contains(text))
            mClipboard.remove(text);
        mClipboard.add(0, text);
        for (int size = mClipboard.size() - 1; size >= mClipboardSize; size--) {
            mClipboard.remove(size);
        }
        JsonUtil.save(new File(Config.getDataDir(), "clipboard.json"), mClipboard);
    }

    /**
     * 删除指定索引的剪贴板记录。
     *
     * @param i 要删除的索引。
     */
    public void removeClipboard(int i) {
        mClipboard.remove(i);
        JsonUtil.save(new File(Config.getDataDir(), "clipboard.json"), mClipboard);
    }

    /**
     * 获取剪贴板历史记录列表。
     *
     * @return 剪贴板字符串列表。
     */
    public List<String> getClipboard() {
        return mClipboard;
    }

    /**
     * 显示或隐藏剪贴板视图。
     *
     * @param b true 显示，false 隐藏。
     */
    public void showClipboardView(boolean b) {
        mRootInputView.showClipboardView(b);
    }

    /**
     * 显示或隐藏工具栏视图。
     *
     * @param b true 显示，false 隐藏。
     */
    private void showToolbarView(boolean b) {
        mRootInputView.showToolbarView(b);
    }

    /**
     * 重启 Rime 引擎。
     */
    public void restart() {
        mRime.restart();
        mHandler.post(new Runnable() {
            @Override
            public void run() {
                String schema = Rime.getCurrentRimeSchema();
                if (!TextUtils.isEmpty(schema)) {
                    mRootInputView.setSchema(schema);
                }
            }
        });
    }

    // 对话框引用
    private AlertDialog mDlg;

    /**
     * 发送消息（异步）。
     * 将消息添加到 LuaActivity 日志，并通过 Handler 在主线程显示 Toast。
     *
     * @param text 要显示的消息文本。
     */
    public void sendMsg(final String text) {
        //Function.printStackTrace("sendMsg " + text);
        LuaActivity.logs.add(text);
        Log.w(TAG, "sendMsg: " + text);
        //sendMsgAux(text);
        mHandler.post(new Runnable() {
            @Override
            public void run() {
                try {
                    sendMsgAux(text);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });
    }

    /**
     * 辅助方法：显示消息。
     * 当前实现为显示 Toast，注释掉的代码是显示对话框的方式。
     *
     * @param text 要显示的消息文本。
     */
    public void sendMsgAux(final String text) {
        Log.w(TAG, "sendMsgAux: " + text);
        //if(!isInputViewShown()&&PrefLauncher.getToken()==null){
        CustomToast.show(this, text, Toast.LENGTH_SHORT, true);
        //return;
        //}
       /*if (mDlg == null) {
            mDlg=showListDialog(new AlertDialog.Builder(this)
                    .setTitle("提示")
                    .setAdapter( new ArrayListAdapter<>(this,new String[]{text}),null)
                    .setNegativeButton("取消",null)
                    .setOnDismissListener(new DialogInterface.OnDismissListener() {
                        @Override
                        public void onDismiss(DialogInterface dialog) {
                            mDlg=null;
                        }
                    })
                    .create());
        } else {
            ArrayListAdapter<String> adapter = (ArrayListAdapter<String>) mDlg.getListView().getAdapter();
            adapter.add(text);
        }*/
    }

    /**
     * 创建 Lua 对话框。
     *
     * @param title 对话框标题。
     * @param items 列表项数组。
     * @return LuaDialog 对象。
     */
    public LuaDialog createDialog(String title, String[] items) {
        LuaDialog dlg = new LuaDialog(this);
        dlg.setTitle(title);
        dlg.setItems(items);
        dlg.setNegativeButton(getString(R.string.cancel), null);
        return dlg;
    }

    /**
     * 设置候选词列表。
     * 将字符串列表转换为 CandidateItem 列表，并更新 UI。
     *
     * @param list 候选词字符串列表。
     */
    public void setCandidates(ArrayList<String> list) {
        if (list == null) {
            list = new ArrayList<>();
        }
        ArrayList<CandidateItem> items = new ArrayList<CandidateItem>(list.size());
        for (String s : list) {
            items.add(new CandidateItem(s));
        }
        mRootInputView.setCandidates(items);
        mComposing = !items.isEmpty();
    }

    /**
     * 添加多个编码内容到编码区。
     *
     * @param list 编码内容字符串列表。
     */
    public void addCompositions(ArrayList<String> list) {
        mRootInputView.addCompositions(list);
    }

    /**
     * 设置编码区文本。
     *
     * @param list 编码文本字符串。
     */
    public void setComposition(String list) {
        mRootInputView.setComposition(list);
    }

    /**
     * 从 HTML 字符串设置编码区文本。
     *
     * @param list HTML 格式的编码文本。
     */
    public void setCompositionFromHtml(String list) {
        mRootInputView.setComposition(Html.fromHtml(list));
    }

    /**
     * 添加云输入结果（未实现）。
     *
     * @param s 云输入文本。
     */
    public void addCloud(String s) {

    }

    /**
     * 添加云输入结果及注释（未实现）。
     *
     * @param index 索引。
     * @param comment 注释。
     */
    public void addCloud(String index, String comment) {

    }

    /**
     * 获取 Rime 引擎实例。
     *
     * @return Rime 对象。
     */
    public Rime getRime() {
        return mRime;
    }


    /**
     * 获取主线程 Handler。
     *
     * @return Handler 对象。
     */
    public Handler getHandler() {
        return mHandler;
    }
}
