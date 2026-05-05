/*
 * SPDX-FileCopyrightText: 2015 - 2026 Rime community
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package com.osfans.trime.dialog;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.os.IBinder;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Toast;

import com.osfans.trime.TrimeService;
import com.osfans.trime.core.DataManager;
import com.osfans.trime.core.Rime;
import com.osfans.trime.core.SchemaItem;
import com.osfans.trime.theme.ThemeManager;
import com.osfans.trime.util.CustomToast;

import java.util.ArrayList;
import java.util.Arrays;

/**
 * 方案管理对话框。
 * 用于选择和管理 Rime 输入法方案,支持多选和部署新方案。
 */
public class SchemaDialog {
    // ==================== 成员变量 ====================
    /** 对话框实例 */
    private AlertDialog mDialog;
    /** 窗口 Token(用于依附于输入法窗口) */
    private IBinder mWindowToken;

    /**
     * 构造函数。
     * 加载所有可用方案,显示多选对话框供用户选择。
     *
     * @param context 上下文。
     */
    public SchemaDialog(Context context) {
        DataManager.sync();
        if (TrimeService.getInstance() == null) {
            CustomToast.show(context, "请先启用输入法", Toast.LENGTH_SHORT, true);
            return;
        }

        // 获取所有可用方案和已选方案
        SchemaItem[] availableSchemas = Rime.getAvailableRimeSchemaList();
        SchemaItem[] selectedSchemas = Rime.getSelectedRimeSchemaList();
        Arrays.sort(availableSchemas, new OptionsDialog.SortByName());

        int schemaCount = availableSchemas.length;
        String[] schemaNames = new String[schemaCount];
        boolean[] checkedStates = new boolean[schemaCount];

        // 存储当前选中的 Schema ID 列表
        ArrayList<String> currentSelectedIds = new ArrayList<>();

        for (int i = 0; i < schemaCount; i++) {
            SchemaItem item = availableSchemas[i];
            schemaNames[i] = item.getName();

            if (isSchemaSelected(selectedSchemas, item)) {
                checkedStates[i] = true;
                currentSelectedIds.add(item.getId());
            }
        }

        mDialog = new AlertDialog.Builder(context, ThemeManager.getDialogTheme())
                .setTitle("管理方案")
                .setMultiChoiceItems(schemaNames, checkedStates, new DialogInterface.OnMultiChoiceClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which, boolean isChecked) {
                        String schemaId = availableSchemas[which].getId();
                        if (isChecked) {
                            currentSelectedIds.add(schemaId);
                        } else {
                            currentSelectedIds.remove(schemaId);
                        }
                    }
                })
                .setPositiveButton("确定", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        // 将 List 转换为 Array 提交给 Rime 核心
                        String[] selectedIdsArray = currentSelectedIds.toArray(new String[0]);
                        Rime.selectRimeSchemas(selectedIdsArray);

                        // 部署新方案
                        new DeployDialog(mDialog.getContext()).show(mWindowToken);
                    }
                })
                .setNegativeButton("取消", null)
                .create();
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

        // 设置对话框类型，使其能依附于输入法窗口
        layoutParams.type = WindowManager.LayoutParams.TYPE_APPLICATION_ATTACHED_DIALOG;
        window.addFlags(WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE);
        layoutParams.token = token;

        window.setAttributes(layoutParams);
        mDialog.show();
    }

    /**
     * 检查某个方案是否在已选列表中。
     *
     * @param selectedList 已选方案列表。
     * @param targetItem 目标方案。
     * @return true 表示方案已在选中列表中。
     */
    private boolean isSchemaSelected(SchemaItem[] selectedList, SchemaItem targetItem) {
        for (SchemaItem selectedItem : selectedList) {
            if (selectedItem.getId().equals(targetItem.getId())) {
                return true;
            }
        }
        return false;
    }
}
