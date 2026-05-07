/*
 * SPDX-FileCopyrightText: 2015 - 2025 Rime community
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package com.osfans.trime.dialog;

import android.app.AlertDialog;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.IBinder;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;

import com.osfans.trime.BuildConfig;
import com.osfans.trime.theme.ThemeManager;
import com.osfans.trime.util.Function;

/**
 * 语音识别设置对话框。
 * 允许用户选择识别引擎（vivo/百度/系统）并配置百度 API 凭证。
 */
public class SpeechDialog {
    /**
     * 日志标签
     */
    private String TAG = "SpeechDialog";

    // ==================== 常量 ====================
    /**
     * 百度 API Key 的 SharedPreferences 键名
     */
    private static final String PREF_BAIDU_API_KEY = "baidu_api_key";
    /**
     * 百度 Secret Key 的 SharedPreferences 键名
     */
    private static final String PREF_BAIDU_SECRET_KEY = "baidu_secret_key";

    // ==================== 成员变量 ====================
    /**
     * 对话框实例
     */
    private AlertDialog mDig;
    /**
     * SharedPreferences 实例
     */
    private final SharedPreferences mPref;
    /**
     * 引擎选择 Spinner
     */
    private Spinner mEngineSpinner;
    /**
     * 百度 API Key 输入框
     */
    private EditText mBaiduApiKeyInput;
    /**
     * 百度 Secret Key 输入框
     */
    private EditText mBaiduSecretKeyInput;
    /**
     * 百度凭证布局
     */
    private LinearLayout mBaiduCredentialsLayout;
    /**
     * 窗口 Token(用于依附于输入法窗口)
     */
    private IBinder mToken;

    /**
     * 构造函数。
     * 创建语音识别设置对话框，包含引擎选择和百度 API 凭证配置。
     *
     * @param context 上下文。
     */
    public SpeechDialog(Context context) {
        mPref = Function.getPref(context);

        // 创建主布局
        LinearLayout mainLayout = new LinearLayout(context);
        mainLayout.setOrientation(LinearLayout.VERTICAL);
        mainLayout.setPadding(32, 32, 32, 32);

        // 引擎选择标签
        TextView engineLabel = new TextView(context);
        engineLabel.setText("语音识别引擎：");
        engineLabel.setTextSize(16);
        mainLayout.addView(engineLabel);

        // 引擎选择 Spinner
        mEngineSpinner = new Spinner(context);
        String[] engines = new String[]{"vivo", "baidu", "system"};
        ArrayAdapter<String> adapter = new ArrayAdapter<>(context, android.R.layout.simple_spinner_item, engines);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        mEngineSpinner.setAdapter(adapter);

        // 设置当前选择的引擎
        String currentEngine = mPref.getString("recognition_service", "vivo");
        int engineIndex = 0;
        if (currentEngine.equals("baidu")) {
            engineIndex = 1;
        } else if (!currentEngine.equals("vivo")) {
            engineIndex = 2;
        }
        mEngineSpinner.setSelection(engineIndex);
        mainLayout.addView(mEngineSpinner);

        // 百度凭证区域（仅当选择百度时显示）
        mBaiduCredentialsLayout = new LinearLayout(context);
        mBaiduCredentialsLayout.setOrientation(LinearLayout.VERTICAL);
        mBaiduCredentialsLayout.setVisibility(engineIndex == 1 ? LinearLayout.VISIBLE : LinearLayout.GONE);

        // 百度 API Key 输入
        TextView apiKeyLabel = new TextView(context);
        apiKeyLabel.setText("百度 API Key：");
        apiKeyLabel.setTextSize(14);
        apiKeyLabel.setPadding(0, 16, 0, 4);
        mBaiduCredentialsLayout.addView(apiKeyLabel);

        mBaiduApiKeyInput = new EditText(context);
        mBaiduApiKeyInput.setHint("请输入百度 API Key");
        mBaiduApiKeyInput.setText(mPref.getString(PREF_BAIDU_API_KEY, ""));
        mBaiduCredentialsLayout.addView(mBaiduApiKeyInput);

        // 百度 Secret Key 输入
        TextView secretKeyLabel = new TextView(context);
        secretKeyLabel.setText("百度 Secret Key：");
        secretKeyLabel.setTextSize(14);
        secretKeyLabel.setPadding(0, 16, 0, 4);
        mBaiduCredentialsLayout.addView(secretKeyLabel);

        mBaiduSecretKeyInput = new EditText(context);
        mBaiduSecretKeyInput.setHint("请输入百度 Secret Key");
        mBaiduSecretKeyInput.setText(mPref.getString(PREF_BAIDU_SECRET_KEY, ""));
        mBaiduCredentialsLayout.addView(mBaiduSecretKeyInput);

        // 提示文本
        TextView hint = new TextView(context);
        hint.setText("请在百度开发者平台注册获取 API 凭证：https://ai.baidu.com/");
        hint.setTextSize(12);
        hint.setTextColor(0xFF888888);
        hint.setPadding(0, 8, 0, 0);
        mBaiduCredentialsLayout.addView(hint);

        mainLayout.addView(mBaiduCredentialsLayout);

        // 监听引擎选择变化
        mEngineSpinner.setOnItemSelectedListener(new android.widget.AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(android.widget.AdapterView<?> parent, View view, int position, long id) {
                mBaiduCredentialsLayout.setVisibility(position == 1 ? LinearLayout.VISIBLE : LinearLayout.GONE);
            }

            @Override
            public void onNothingSelected(android.widget.AdapterView<?> parent) {
            }
        });

        // 创建 AlertDialog
        AlertDialog.Builder builder = new AlertDialog.Builder(context, ThemeManager.getDialogTheme())
                .setTitle("语音识别设置")
                .setView(mainLayout)
                .setPositiveButton("确定", (dialog, which) -> saveSettings())
                .setNegativeButton("取消", null);

        mDig = builder.create();
    }

    /**
     * 显示对话框(无 Token)。
     */
    public void show() {
        if (mDig == null)
            return;
        mDig.show();
    }

    /**
     * 显示对话框(带 Token,依附于输入法窗口)。
     * 设置对话框类型为 TYPE_APPLICATION_ATTACHED_DIALOG,使其能依附于输入法窗口显示。
     *
     * @param token 窗口 Token。
     */
    public void show(IBinder token) {
        if (mDig == null)
            return;
        mToken = token;
        if (mToken == null) {
            show();
            return;
        }
        Window win = mDig.getWindow();
        WindowManager.LayoutParams attr = win.getAttributes();
        attr.type = WindowManager.LayoutParams.TYPE_APPLICATION_ATTACHED_DIALOG;
        win.addFlags(WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE);
        attr.token = token;
        win.setAttributes(attr);
        mDig.show();
    }

    /**
     * 保存设置到 SharedPreferences。
     */
    private void saveSettings() {
        String engine = (String) mEngineSpinner.getSelectedItem();
        SharedPreferences.Editor editor = mPref.edit();

        if (engine.equals("baidu")) {
            editor.putString("recognition_service", "baidu");
            editor.putString(PREF_BAIDU_API_KEY, mBaiduApiKeyInput.getText().toString().trim());
            editor.putString(PREF_BAIDU_SECRET_KEY, mBaiduSecretKeyInput.getText().toString().trim());
        } else if (engine.equals("vivo")) {
            editor.putString("recognition_service", "vivo");
        } else {
            editor.putString("recognition_service", "system");
        }

        editor.apply();
    }
}
