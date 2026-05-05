/*
 * SPDX-FileCopyrightText: 2015 - 2026 Rime community
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package com.osfans.trime.keyboard.adapter;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.res.Resources;
import android.util.Log;
import android.util.TypedValue;
import android.view.Menu;
import android.view.View;
import android.view.ViewGroup;
import android.widget.PopupMenu;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.osfans.trime.Key;
import com.osfans.trime.TrimeService;
import com.osfans.trime.keyboard.KeyView;
import com.osfans.trime.theme.KeyStyle;
import com.osfans.trime.theme.Style;
import com.osfans.trime.theme.ThemeManager;
import com.osfans.trime.util.Function;

import java.util.ArrayList;
import java.util.List;

/**
 * 瀑布流适配器。
 * 用于显示剪贴板和常用语列表,支持长按操作(删除、置顶、收藏)。
 */
public class WaterfallAdapter extends RecyclerView.Adapter<WaterfallAdapter.ViewHolder> {

    // ==================== 成员变量 ====================
    /** 项目样式 */
    private final KeyStyle mItemStyle;
    /** 数据列表 */
    private final List<String> mData;
    /** 剪贴板样式 */
    private final Style mStyle;
    /** 是否为常用语模式 */
    private boolean mIsPhrase;

    /**
     * 构造函数。
     *
     * @param data 数据列表。
     */
    public WaterfallAdapter(List<String> data) {
        this.mData = new ArrayList<>(data);
        mStyle=ThemeManager.getStyle().getStyle("clipboard");
        mItemStyle=mStyle.getKeyStyle("item");
    }

    /**
     * 创建 ViewHolder。
     * 创建 KeyView 并设置点击和长按事件。
     *
     * @param parent 父视图组。
     * @param viewType 视图类型。
     * @return ViewHolder 实例。
     */
    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        KeyView view = new KeyView(parent.getContext(), mItemStyle);
        view.setLayoutParams(new ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT));
        ViewHolder holder = new ViewHolder(view);
        holder.itemView.setOnClickListener(v -> {
            int position = holder.getBindingAdapterPosition(); // 获取当前实时位置
            if (position != RecyclerView.NO_POSITION && mData != null) {
                String item = mData.get(position);
                TrimeService.getInstance().commitText(item);
            }
        });
        holder.itemView.setOnLongClickListener(v -> {
            int position = holder.getBindingAdapterPosition(); // 获取当前实时位置
            if (position != RecyclerView.NO_POSITION && mData != null) {
                if(mIsPhrase)
                    showPhraseDialog(position);
                else
                    showClipboardDialog(position);
            }
            return true;
        });
        return holder;
   }

    /**
     * 显示剪贴板操作对话框。
     *
     * @param position 数据位置。
     */
    private void showClipboardDialog(int position) {
        TrimeService trime = TrimeService.getInstance();
        trime.showDialog(new AlertDialog.Builder(trime, ThemeManager.getDialogTheme())
                .setItems(new String[]{
                        "删除",
                        "置顶",
                        "收藏",
                        "取消"
                }, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        switch (which){
                            case 0:
                                trime.removeClipboard(position);
                                setData(mIsPhrase);
                                break;
                            case 1:
                                trime.addClipboard(trime.getClipboard().get(position));
                                setData(mIsPhrase);
                                break;
                            case 2:
                                trime.addPhrase(trime.getClipboard().get(position));
                                break;
                        }
                    }
                }).create());
    }

    /**
     * 显示常用语操作对话框。
     *
     * @param position 数据位置。
     */
    private void showPhraseDialog(int position) {
        TrimeService trime = TrimeService.getInstance();
        trime.showDialog(new AlertDialog.Builder(trime, ThemeManager.getDialogTheme())
                .setItems(new String[]{
                        "删除",
                        "置顶",
                        "取消"
                }, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        switch (which){
                            case 0:
                                trime.removePhrase(position);
                                setData(mIsPhrase);
                                break;
                            case 1:
                                trime.addPhrase(trime.getPhrase().get(position));
                                setData(mIsPhrase);
                                break;
                        }
                    }
                }).create());
    }

    /**
     * 绑定数据到 ViewHolder。
     * 限制文本长度最多100个字符。
     *
     * @param holder ViewHolder 实例。
     * @param position 数据位置。
     */
    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        String item = mData.get(position);
        // 动态修改 LayoutParams
        ViewGroup.LayoutParams lp = holder.textView.getLayoutParams();
        lp.width = ViewGroup.LayoutParams.MATCH_PARENT;
        lp.height = ViewGroup.LayoutParams.WRAP_CONTENT;
        holder.textView.setLayoutParams(lp);
        // 加载图片和文字
        if(item.length()>100)
            item=item.substring(0,100);
        holder.textView.setText(item);
    }

    /**
     * 获取数据项数量。
     *
     * @return 数据项总数。
     */
    @Override
    public int getItemCount() {
        return mData == null ? 0 : mData.size();
    }

    /**
     * 设置数据列表。
     * 根据模式(常用语/剪贴板)加载不同的数据。
     *
     * @param b true 表示常用语模式,false 表示剪贴板模式。
     */
    public void setData(boolean b) {
        mIsPhrase=b;
        mData.clear();
        TrimeService trime = TrimeService.getInstance();
        mData.addAll(b?trime.getPhrase():trime.getClipboard());
        notifyDataSetChanged();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        KeyView textView;
         public ViewHolder(@NonNull View itemView) {
            super(itemView);
            textView = (KeyView) itemView;
        }
    }
}
