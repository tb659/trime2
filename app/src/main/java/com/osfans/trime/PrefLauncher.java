/*
 * SPDX-FileCopyrightText: 2015 - 2025 Rime community
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package com.osfans.trime;

import android.app.Activity; // Android 活动基类
import android.graphics.Color; // 颜色处理类
import android.graphics.Insets; // 屏幕边距 insets 类
import android.os.Build; // 系统版本信息类
import android.os.Bundle; // 数据包类，用于保存和恢复状态
import android.os.IBinder; // 接口绑定器，用于跨进程通信
import android.text.util.Linkify; // 自动链接识别工具（如网址、电话）
import android.util.TypedValue; // 类型化数值工具类
import android.view.Menu; // 菜单类
import android.view.MenuItem; // 菜单项类
import android.view.View; // 视图基类
import android.view.ViewGroup; // 视图组基类
import android.view.WindowInsets; // 窗口边距信息类
import android.view.WindowManager; // 窗口管理器
import android.view.inputmethod.InputMethodManager; // 输入法管理器
import android.widget.AdapterView; // 适配器视图监听接口
import android.widget.ArrayListAdapter; // 数组列表适配器
import android.widget.Button; // 按钮控件
import android.widget.EditText; // 文本输入框控件
import android.widget.LinearLayout; // 线性布局容器
import android.widget.ListView; // 列表视图控件
import android.widget.TextView; // 文本显示控件
import android.widget.Toast; // 短提示消息控件

import androidx.annotation.NonNull; // 非空注解
import androidx.annotation.Nullable; // 可空注解

import com.androlua.LuaUtil; // Lua 工具类（同文输入法支持 Lua 脚本）
import com.osfans.trime.core.DataManager; // 数据管理器，负责同步用户数据
import com.osfans.trime.dialog.DeployDialog; // 部署对话框
import com.osfans.trime.dialog.KeyboardDialog; // 键盘选择对话框
import com.osfans.trime.dialog.OptionsDialog; // 选项设置对话框
import com.osfans.trime.dialog.SchemaDialog; // 输入方案管理对话框
import com.osfans.trime.dialog.SchemaGroupDialog; // 方案组管理对话框
import com.osfans.trime.dialog.SpeechDialog; // 语音识别设置对话框
import com.osfans.trime.dialog.StyleDialog; // 样式选择对话框
import com.osfans.trime.dialog.ThemeDialog; // 主题选择对话框
import com.osfans.trime.util.CustomToast;

/**
 * 偏好设置启动器活动类。
 * <p>
 * 该类作为输入法的一个独立入口 Activity，允许用户在未激活输入法或需要快速配置时，
 * 通过桌面图标直接进入设置界面。它提供了一个简单的列表菜单，用于切换输入法、
 * 管理方案、选择主题等核心功能。
 */
public class PrefLauncher extends Activity implements AdapterView.OnItemClickListener {

    /**
     * 当前窗口的令牌（Token）。
     * <p>
     * 该 Token 用于在需要窗口上下文的操作中（如弹出对话框或输入法交互）提供身份验证。
     */
    private static IBinder mToken;

    /**
     * 获取当前活动的窗口令牌。
     *
     * @return 窗口令牌 IBinder 对象，如果活动未启动则返回 null
     */
    public static IBinder getToken() {
        return mToken;
    }

    /**
     * 活动创建时的回调方法。
     * <p>
     * 在此方法中初始化界面布局、设置主题、适配系统边距（如刘海屏、导航栏）
     * 并加载功能菜单列表。
     *
     * @param savedInstanceState 之前保存的实例状态包，首次创建时为 null
     */
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        // 根据系统版本设置合适的默认主题
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            // Android 10 及以上支持深色模式自适应
            setTheme(android.R.style.Theme_DeviceDefault_DayNight);
        } else {
            // 旧版本使用默认设备主题
            setTheme(android.R.style.Theme_DeviceDefault);
        }
        super.onCreate(savedInstanceState);
        
        // 设置软键盘模式为平移模式，防止布局被遮挡
        getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_PAN);
        
        // 同步数据管理器，确保读取到最新的配置信息
        DataManager.sync();
        
        // Android 15 (VANILLA_ICE_CREAM) 及以上版本处理全屏边距
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.VANILLA_ICE_CREAM) {
            getWindow().setDecorFitsSystemWindows(false);
        }
        
        // 创建功能菜单列表适配器
        ArrayListAdapter<String> adapter = new ArrayListAdapter<>(this, new String[]{
                "切换输入法",   // 调用系统输入法选择器
                "方案组",       // 管理不同的方案组合
                "管理方案",     // 增删改查具体的输入方案（如拼音、五笔）
                "输入方案",     // 快速切换当前启用的方案
                "键盘主题",     // 更换键盘皮肤
                "颜色样式",     // 调整配色方案
                "默认键盘",      // 设置不同场景下的默认键盘布局
                "语音识别"       // 语音识别引擎设置
        });
        
        ListView mListView = new ListView(this); // 创建列表视图
        // 解决内容被导航栏遮挡的关键：应用 WindowInsets
        mListView.setAdapter(adapter); // 将适配器绑定到列表
        
        // 修改后的布局结构逻辑：使用线性布局垂直排列
        LinearLayout root = new LinearLayout(this);
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.VANILLA_ICE_CREAM) {
            // 监听系统边距变化，动态调整内边距以避开状态栏和导航栏
            root.setOnApplyWindowInsetsListener((v, insets) -> {
                // 获取系统状态栏和导航栏的 Insets，但不包含键盘 (ime)
                Insets systemBars = insets.getInsets(WindowInsets.Type.systemBars());
                // 获取键盘高度
                Insets ime = insets.getInsets(WindowInsets.Type.ime());
                v.setPadding(
                        systemBars.left,
                        systemBars.top,
                        systemBars.right,
                        systemBars.bottom
                );
                return WindowInsets.CONSUMED; // 标记边距已处理
            });
        }
        root.setOrientation(LinearLayout.VERTICAL); // 设置为垂直方向布局
        
        EditText editText = new EditText(this); // 创建一个底部的输入框（可能用于调试或搜索）
        root.addView(mListView, new LinearLayout.LayoutParams(-1, -2)); // ListView 占据剩余空间
        root.addView(editText, new LinearLayout.LayoutParams(-1, -2)); // EditText 固定在底部
        
        TextView tv = new TextView(this); // 创建提示信息的文本视图
        tv.setAutoLinkMask(Linkify.ALL); // 开启自动识别网址、邮箱等链接
        tv.setText("下载更多版本：https://github.com/nirenr/trime2/releases"); // 设置提示文本
        root.addView(tv, new LinearLayout.LayoutParams(-1, -2)); // 添加到底部
        
        setContentView(root); // 设置根布局
        mListView.setOnItemClickListener(this); // 注册列表项点击监听器
     }

    /**
     * 创建选项菜单。
     * <p>
     * 在右上角添加一个“部署”按钮，用于重新编译和加载 Rime 配置。
     *
     * @param menu 要填充的菜单对象
     * @return 返回 true 表示菜单已显示
     */
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // 添加“部署”菜单项，并设置为始终显示在动作栏
        menu.add(0, 1, 0, "部署").setShowAsActionFlags(MenuItem.SHOW_AS_ACTION_ALWAYS);
        return super.onCreateOptionsMenu(menu);
    }

    /**
     * 处理菜单项点击事件。
     *
     * @param item 被点击的菜单项
     * @return 返回 true 表示事件已处理
     */
    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        // 检查输入法服务是否已启动
        if (TrimeService.getInstance() == null) {
            CustomToast.show(this, "请先启用输入法", Toast.LENGTH_SHORT, true);
            return super.onOptionsItemSelected(item);
        }
        // 如果点击的是“部署”按钮（ID 为 1）
        if(item.getItemId() == 1){
            new DeployDialog(this).show(); // 显示部署进度对话框
        }
        return super.onOptionsItemSelected(item);
    }

    /**
     * 活动恢复时的回调。
     * <p>
     * 在此检查并请求必要的存储权限，确保输入法能正常读写配置文件。
     */
    @Override
    protected void onResume() {
        super.onResume();
        LuaUtil.checkStorage(this); // 检查存储权限
   }

    /**
     * 活动开始时的回调。
     * <p>
     * 获取当前窗口的 Token 并保存到静态变量，供其他组件使用。
     */
    @Override
    protected void onStart() {
        super.onStart();
        mToken = getWindow().getDecorView().getWindowToken(); // 获取窗口令牌
    }

    /**
     * 活动停止时的回调。
     * <p>
     * 清空窗口 Token，防止内存泄漏或在后台误用无效的 Token。
     */
    @Override
    protected void onStop() {
        mToken = null; // 释放令牌
        super.onStop();
    }

    /**
     * 处理列表项点击事件。
     * <p>
     * 根据用户点击的位置，弹出相应的设置对话框或执行系统操作。
     *
     * @param parent 被点击的 AdapterView
     * @param view   被点击的具体视图
     * @param position 被点击项的索引位置
     * @param id     被点击项的行 ID
     */
    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        switch (position) {
            case 0:
                // 显示系统输入法选择器，允许用户切换到其他输入法
                ((InputMethodManager) getSystemService(INPUT_METHOD_SERVICE)).showInputMethodPicker();
                break;
            case 1:
                // 显示方案组管理对话框
                new SchemaGroupDialog(PrefLauncher.this).show();
                break;
            case 2:
                // 显示方案管理对话框（增删改查）
                new SchemaDialog(this).show();
                break;
            case 3:
                // 显示输入方案切换对话框
                new OptionsDialog(this).show();
                break;
            case 4:
                // 显示键盘主题选择对话框
                new ThemeDialog(this).show();
                break;
            case 5:
                // 显示颜色样式选择对话框
                new StyleDialog(this).show();
                break;
            case 6:
                // 显示默认键盘布局设置对话框
                new KeyboardDialog(this).show();
                break;
            case 7:
                // 显示语音识别设置对话框
                new SpeechDialog(this).show();
                break;
        }
    }
}
