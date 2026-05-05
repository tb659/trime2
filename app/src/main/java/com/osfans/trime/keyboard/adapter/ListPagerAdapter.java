/*
 * SPDX-FileCopyrightText: 2015 - 2026 Rime community
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package com.osfans.trime.keyboard.adapter;

import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.flexbox.AlignItems;
import com.google.android.flexbox.FlexDirection;
import com.google.android.flexbox.FlexWrap;
import com.google.android.flexbox.FlexboxLayoutManager;
import com.google.android.flexbox.JustifyContent;

import org.luaj.LuaValue;

/**
 * 列表分页适配器。
 * 用于 ViewPager,每一页显示一个 RecyclerView,使用 FlexboxLayoutManager 实现流式布局。
 */
public class ListPagerAdapter extends RecyclerView.Adapter<ListPagerAdapter.ListViewHolder> {

    // ==================== 成员变量 ====================
    /** 数据映射表(Lua 值): Key 为页面索引, Value 为该页的数据列表 */
    private LuaValue mDataMap;
    /** 当前显示的 RecyclerView */
    private RecyclerView mListView;

    /**
     * 构造函数。
     *
     * @param dataMap Lua 数据映射表。
     */
    public ListPagerAdapter(LuaValue dataMap) {
        this.mDataMap = dataMap;
    }

    /**
     * 创建 ViewHolder。
     * 动态创建一个 RecyclerView 作为 ViewPager 的每一页,使用 FlexboxLayoutManager 实现流式布局。
     *
     * @param parent 父视图组。
     * @param viewType 视图类型。
     * @return ListViewHolder 实例。
     */
    @NonNull
    @Override
    public ListViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        // 动态创建一个 RecyclerView 作为 ViewPager 的每一页
        RecyclerView recyclerView = new RecyclerView(parent.getContext());
        recyclerView.setLayoutParams(new ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT));
        recyclerView.setClipChildren(false);
        recyclerView.setClipToPadding(false);

        FlexboxLayoutManager flexManager = new FlexboxLayoutManager(parent.getContext()){
            @Override
            public void onLayoutChildren(RecyclerView.Recycler recycler, RecyclerView.State state) {
                try {
                    super.onLayoutChildren(recycler, state);
                } catch (IndexOutOfBoundsException e) {
                    // 仅记录日志，不让应用崩溃
                    android.util.Log.e("ExpandedCandidate", "Catching Flexbox layout crash", e);
                }
            }
        };
        // 1. 主轴设为横向 (ROW)
        flexManager.setFlexDirection(FlexDirection.ROW);
        flexManager.setFlexWrap(FlexWrap.WRAP);             // 开启换行（换列）
        // 4. 设置对齐方式
        // 1. 改为左对齐，配合 Adapter 里的 flexGrow 实现整齐填充
        flexManager.setJustifyContent(JustifyContent.FLEX_START);
        //flexManager.setAlignItems(AlignItems.CENTER);
        flexManager.setAlignItems(AlignItems.STRETCH);
        recyclerView.setLayoutManager(flexManager);
        return new ListViewHolder(recyclerView);
    }

    /**
     * 绑定数据到 ViewHolder。
     *
     * @param holder ViewHolder 实例。
     * @param position 页面位置。
     */
    @Override
    public void onBindViewHolder(@NonNull ListViewHolder holder, int position) {
        // 获取当前页面的数据
        LuaValue pageData = mDataMap.get(position+1).get("keys");
        // 这里需要你写一个普通的 RecyclerView 适配器来展示具体行数据
        LuaValueListAdapter itemAdapter = new LuaValueListAdapter(pageData);
        holder.recyclerView.setAdapter(itemAdapter);
        mListView=holder.recyclerView;
    }

    /**
     * 获取页面数量。
     *
     * @return 页面总数。
     */
    @Override
    public int getItemCount() {
        return mDataMap.length();
    }

    /**
     * 获取当前显示的 RecyclerView。
     *
     * @return RecyclerView 实例。
     */
    public RecyclerView getListView() {
        return mListView;
    }

    public static class ListViewHolder extends RecyclerView.ViewHolder {
        public final RecyclerView recyclerView;
        ListViewHolder(@NonNull View itemView) {
            super(itemView);
            recyclerView = (RecyclerView) itemView;
        }
    }
}
