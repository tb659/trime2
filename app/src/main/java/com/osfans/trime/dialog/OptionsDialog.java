/*
 * SPDX-FileCopyrightText: 2015 - 2026 Rime community
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package com.osfans.trime.dialog;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.os.IBinder;
import android.util.Log;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Toast;

import com.osfans.trime.theme.ThemeManager;
import com.osfans.trime.util.Function;
import com.osfans.trime.TrimeService;
import com.osfans.trime.util.CustomToast;
import com.osfans.trime.core.Rime;
import com.osfans.trime.core.SchemaItem;

import java.lang.reflect.Array;
import java.text.Collator;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.Map;

/**
 * 方案选择对话框。
 * 用于切换 Rime 输入法方案,支持设置、管理方案和切换当前方案。
 */
public class OptionsDialog {
    // ==================== 成员变量 ====================
    /** 对话框实例 */
    private AlertDialog mDialog;
    /** 是否需要更新 Rime 选项 */
    private boolean mNeedUpdateRimeOption;
    /** 窗口 Token(用于依附于输入法窗口) */
    private IBinder mWindowToken;

    /**
     * 构造函数。
     * 加载所有可用方案,显示单选对话框供用户选择,支持设置和管理方案。
     *
     * @param context 上下文。
     */
    public OptionsDialog(Context context) {
        if (TrimeService.getInstance() == null) {
            CustomToast.show(context, "请先启用输入法", Toast.LENGTH_SHORT, true);
            return;
        }

        AlertDialog.Builder builder =
                new AlertDialog.Builder(context, ThemeManager.getDialogTheme())
                        .setTitle("选择方案")
                        .setPositiveButton(
                                "设置",
                                new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialog, int id) {
                                        Function.showPrefDialog(context); // 全局设置
                                        dialog.dismiss();
                                    }
                                })
                        .setNegativeButton(android.R.string.cancel, null);

        // 获取当前正在使用的方案 ID
        String currentSchemaId = Rime.getCurrentRimeSchema();

        if (currentSchemaId.equals(".default")) {
            builder.setMessage("没有方案，请先添加启用方案");
            mDialog = builder.create();
            return;
        }
        // 获取所有可用的方案列表
        SchemaItem[] availableSchemas = Rime.getRimeSchemaList();
        if (availableSchemas == null) {
            CustomToast.show(context, "请先启用输入法", Toast.LENGTH_SHORT, true);
            return;
        }
        Arrays.sort(availableSchemas, new SortByName());

        int schemaCount = availableSchemas.length;
        String[] schemaNames = new String[schemaCount];
        int currentSelectedIndex = 0;

        for (int i = 0; i < schemaCount; i++) {
            schemaNames[i] = availableSchemas[i].getName();
            // 匹配当前方案，用于设置单选框的初始选中项
            if (availableSchemas[i].getId().equals(currentSchemaId)) {
                currentSelectedIndex = i;
            }
        }

        builder.setNeutralButton(
                "管理方案",
                new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int id) {
                        // 启动部署/管理方案对话框
                        new SchemaDialog(context).show(mWindowToken);
                        dialog.dismiss();
                    }
                });

        builder.setSingleChoiceItems(
                schemaNames,
                currentSelectedIndex,
                new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int index) {
                        dialog.dismiss();
                        String selectedId = availableSchemas[index].getId();
                        Rime.selectRimeSchema(selectedId); // 切换方案
                        mNeedUpdateRimeOption = true;
                        Function.saveString(context, "select_schema_id", selectedId);
                    }
                });
        mDialog = builder.create();
    }

    /**
     * 显示对话框(无 Token)。
     */
    public void show() {
        if (mDialog == null) return;
        mDialog.show();
    }

    /**
     * 显示对话框(带 Token,依附于输入法窗口)。
     * 设置对话框类型为 TYPE_APPLICATION_ATTACHED_DIALOG,使其能依附于输入法窗口显示。
     *
     * @param token 窗口 Token。
     */
    public void show(IBinder token) {
        if (mDialog == null) return;
        mWindowToken = token;

        if (mWindowToken == null) {
            show();
            return;
        }

        Window window = mDialog.getWindow();
        WindowManager.LayoutParams layoutParams = window.getAttributes();

        // 设置为输入法附着的对话框类型，确保其能显示在输入法窗口之上
        layoutParams.type = WindowManager.LayoutParams.TYPE_APPLICATION_ATTACHED_DIALOG;
        window.addFlags(WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE);
        layoutParams.token = token;

        window.setAttributes(layoutParams);
        mDialog.show();
    }

    /**
     * 按名称排序方案项的比较器。
     * 优先比较方案名称,名称为空则比较 ID,支持区域敏感的字符串比较。
     */
    public static class SortByName implements Comparator<SchemaItem> {
        /** 区域敏感的比较器 */
        private final LocaleComparator localeComp = new LocaleComparator();

        @Override
        public int compare(SchemaItem item1, SchemaItem item2) {
            String name1 = item1.getName();
            String name2 = item2.getName();

            // 优先比较名称，名称为空则比较 ID
            if (name1 != null && name2 != null) {
                return localeComp.compare(name1, name2);
            }

            String id1 = item1.getId();
            String id2 = item2.getId();
            return localeComp.compare(id1, id2);
        }
    }

    /**
     * 区域敏感的字符串比较器。
     * 使用 Collator 实现符合当前语言环境的字符串比较。
     */
    public static class LocaleComparator implements Comparator<String> {
        /** Collator 实例,用于区域敏感的比较 */
        private final Collator collator = Collator.getInstance(java.util.Locale.getDefault());

        @Override
        public int compare(String s1, String s2) {
            return collator.compare(s1, s2);
        }
    }
}
