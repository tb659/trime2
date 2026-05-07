package com.osfans.trime.util;

import android.content.Context;
import android.graphics.Point;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.Display;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.PopupWindow;
import android.widget.TextView;

import com.osfans.trime.R;

import java.lang.ref.WeakReference;

/**
 * 自定义 Toast 提示工具类。
 * <p>
 * 该类使用 {@link PopupWindow} 实现类似 Android 原生 {@link android.widget.Toast} 的功能，
 * 但提供了更灵活的显示位置控制（例如显示在键盘上方）和自定义样式支持。
 * 它确保所有 UI 操作都在主线程中执行，并自动管理旧提示的消失以防止重叠。
 */
public class CustomToast {

    /**
     * 短提示持续时间（毫秒）。
     * 对应 {@link android.widget.Toast#LENGTH_SHORT} 的实际显示时长。
     */
    private static final int LENGTH_SHORT_DURATION = 3000;

    /**
     * 长提示持续时间（毫秒）。
     * 对应 {@link android.widget.Toast#LENGTH_LONG} 的实际显示时长。
     */
    private static final int LENGTH_LONG_DURATION = 5000;

    /**
     * 当前正在显示的 PopupWindow 的弱引用。
     * 使用弱引用防止内存泄漏，并允许垃圾回收器在必要时回收未使用的窗口对象。
     */
    private static WeakReference<PopupWindow> currentPopupRef;

    /**
     * 运行在主线程（UI 线程）的 Handler。
     * 用于确保所有视图操作和延迟任务都在正确的线程上下文中执行。
     */
    private static final Handler mainHandler = new Handler(Looper.getMainLooper());

    /**
     * 显示自定义 Toast 提示。
     * <p>
     * 默认显示在屏幕底部附近，不强制位于键盘上方。
     *
     * @param context  上下文环境，用于获取系统服务和加载资源
     * @param text     要显示的文本内容
     * @param duration 显示时长，通常传入 {@link android.widget.Toast#LENGTH_SHORT} 或 {@link android.widget.Toast#LENGTH_LONG}
     */
    public static void show(Context context, String text, int duration) {
        show(context, text, duration, false);
    }

    /**
     * 显示自定义 Toast 提示，支持指定是否显示在键盘上方。
     * <p>
     * 如果上下文是 {@link android.inputmethodservice.InputMethodService} 且 {@code aboveKeyboard} 为 true，
     * 则尝试将提示显示在输入法键盘的顶部。否则，显示在屏幕底部。
     *
     * @param context       上下文环境，可以是 Activity 或 InputMethodService
     * @param text          要显示的文本内容
     * @param duration      显示时长，通常传入 {@link android.widget.Toast#LENGTH_SHORT} 或 {@link android.widget.Toast#LENGTH_LONG}
     * @param aboveKeyboard 如果为 true 且上下文支持，则尝试显示在键盘上方；否则显示在屏幕底部
     */
    public static void show(Context context, String text, int duration, boolean aboveKeyboard) {
        // 参数有效性检查：如果上下文或文本为空，则直接返回，避免崩溃
        if (context == null || text == null) return;

        // 确保所有 UI 操作在主线程中执行
        mainHandler.post(() -> {
            // 在显示新提示之前，先关闭当前可能存在的旧提示
            dismissCurrent();

            // 加载自定义 Toast 布局文件
            View toastView = LayoutInflater.from(context).inflate(R.layout.toast_custom, null);
            // 查找文本视图并设置显示内容
            TextView textView = toastView.findViewById(R.id.toast_text);
            textView.setText(text);

            // 创建 PopupWindow 实例
            // 宽高设置为 WRAP_CONTENT，以便根据内容自动调整大小
            PopupWindow popupWindow = new PopupWindow(
                    toastView,
                    WindowManager.LayoutParams.WRAP_CONTENT,
                    WindowManager.LayoutParams.WRAP_CONTENT
            );

            // 配置 PopupWindow 的行为属性
            popupWindow.setOutsideTouchable(false); // 点击外部区域不关闭
            popupWindow.setFocusable(false);        // 不获取焦点，避免干扰输入法或其他控件
            popupWindow.setTouchable(false);        // 不可触摸，仅作为展示用途

            // 在 Android 5.0 (Lollipop) 及以上版本，设置高程为 0 以移除默认阴影，保持样式简洁
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                popupWindow.setElevation(0);
            }

            // 在指定位置显示 PopupWindow
            // 使用 Gravity.CENTER 让系统自动居中，然后应用偏移量
            popupWindow.showAtLocation(getContentView(context), Gravity.CENTER, 0, 0);

            // 保存当前 PopupWindow 的弱引用，以便后续可以关闭它
            currentPopupRef = new WeakReference<>(popupWindow);

            // 根据传入的 duration 常量确定实际延迟时间
            int delay;
            if (duration == android.widget.Toast.LENGTH_SHORT) {
                delay = LENGTH_SHORT_DURATION;
            } else if (duration == android.widget.Toast.LENGTH_LONG) {
                delay = LENGTH_LONG_DURATION;
            } else {
                // 如果传入的是具体毫秒数，直接使用
                delay = duration > 0 ? duration : LENGTH_SHORT_DURATION;
            }
            
            // 设置定时任务，在指定延迟后自动关闭提示
            mainHandler.postDelayed(CustomToast::dismissCurrent, delay);
        });
    }

    /**
     * 关闭当前正在显示的 Toast 提示。
     * <p>
     * 此方法是私有的，仅供内部调用。它会检查引用是否有效以及窗口是否正在显示，
     * 然后安全地关闭窗口并清理引用。
     */
    private static void dismissCurrent() {
        if (currentPopupRef != null) {
            PopupWindow popup = currentPopupRef.get();
            // 只有当 PopupWindow 对象存在且正在显示时才执行关闭操作
            if (popup != null && popup.isShowing()) {
                popup.dismiss();
            }
            // 清除引用，帮助垃圾回收
            currentPopupRef.clear();
        }
    }

    /**
     * 获取用于显示 Toast 的父视图（DecorView）。
     * <p>
     * 根据上下文类型返回合适的根视图：
     * - 如果是 Activity，返回其窗口装饰视图。
     * - 如果是 InputMethodService，返回其窗口装饰视图。
     * - 其他情况，创建一个临时视图作为占位符（虽然这种情况较少见且可能无法正确显示）。
     *
     * @param context 上下文环境
     * @return 用于挂载 PopupWindow 的根视图
     */
    private static View getContentView(Context context) {
        if (context instanceof android.app.Activity) {
            return ((android.app.Activity) context).getWindow().getDecorView();
        } else if (context instanceof android.inputmethodservice.InputMethodService) {
            return ((android.inputmethodservice.InputMethodService) context).getWindow().getWindow().getDecorView();
        } else {
            // 对于非 Activity 和非 IMS 的上下文，创建一个简单的 View 作为备用
            // 注意：在这种情况下显示 PopupWindow 可能会受到限制或行为不一致
            return new View(context);
        }
    }

    /**
     * 手动立即关闭当前显示的 Toast 提示。
     * <p>
     * 此方法可用于在用户交互或其他事件触发时提前隐藏提示。
     * 操作将在主线程中异步执行。
     */
    public static void dismiss() {
        mainHandler.post(CustomToast::dismissCurrent);
    }
}
