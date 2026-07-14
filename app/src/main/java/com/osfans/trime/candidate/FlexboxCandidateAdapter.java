/*
 * SPDX-FileCopyrightText: 2015 - 2026 Rime community
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package com.osfans.trime.candidate;

import android.annotation.SuppressLint;
import android.content.Context;
import android.text.TextUtils;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.flexbox.FlexboxLayoutManager;
import com.osfans.trime.Config;
import com.osfans.trime.TrimeService;
import com.osfans.trime.core.CandidateItem;
import com.osfans.trime.theme.KeyStyle;
import com.osfans.trime.theme.ThemeManager;

import java.util.ArrayList;

/**
 * Flexbox 候选词适配器类。
 * 用于 FlexboxLayoutManager 的 RecyclerView,支持流式布局展示候选词。
 */
public class FlexboxCandidateAdapter extends RecyclerView.Adapter<FlexboxCandidateAdapter.CandidateViewHolder> {

    // ==================== 成员变量 ====================
    
    /** 候选词数据列表 */
    private final ArrayList<CandidateItem> mData;
    /** 候选词文本样式 */
    private final KeyStyle mCandidateStyle;
    /** 候选词注释样式 */
    private final KeyStyle mCommentStyle;
    /** 是否正在加载下一页 */
    private boolean mIsLoading;

    /**
     * 构造函数。
     *
     * @param data 候选词数据列表。
     */
    public FlexboxCandidateAdapter(ArrayList<CandidateItem> data) {
        this.mData = data;
        mCandidateStyle = ThemeManager.getStyle().getKeyStyle("candidate"); // 获取候选词样式
        mCommentStyle = mCandidateStyle.getKeyStyle("comment",mCandidateStyle); // 获取注释样式
    }

    /**
     * 创建 ViewHolder。
     * 构建候选词项的视图结构,使用 FlexboxLayoutParams 支持流式布局。
     *
     * @param parent 父容器。
     * @param viewType 视图类型。
     * @return 新创建的 CandidateViewHolder。
     */
    @NonNull
    @Override
    public FlexboxCandidateAdapter.CandidateViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        Context context = parent.getContext();
        // 1. 创建容器（相当于 XML 里的 LinearLayout）
        LinearLayout layout = new LinearLayout(context);
        // 在 onCreateViewHolder 里的 layout 设置之后添加
        TypedValue outValue = new TypedValue();
        context.getTheme().resolveAttribute(android.R.attr.selectableItemBackground, outValue, true);
        layout.setBackgroundResource(outValue.resourceId);
        layout.setClickable(true);
        layout.setGravity(Gravity.CENTER);
        layout.setOrientation(LinearLayout.VERTICAL);
        int dp = ThemeManager.dp2px(12);
        int dp2 = ThemeManager.dp2px(6);
        layout.setPadding(dp, dp2, dp, dp2);
        // 关键:必须使用 FlexboxLayoutManager.LayoutParams
        // 宽度设为 WRAP_CONTENT,高度设为 WRAP_CONTENT
        int candidateMinWidth = ThemeManager.getCandidateMinWidth();
        FlexboxLayoutManager.LayoutParams lp = new FlexboxLayoutManager.LayoutParams(
                candidateMinWidth > 0 ? candidateMinWidth : ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT);
        //lp.setFlexBasisPercent(0.2f);
        lp.setFlexGrow(1.0f); // 设置弹性增长系数,使项目均匀填充空间
        layout.setLayoutParams(lp);

        // 2. 创建并配置 TextView (关键：必须给子 View 设置 LayoutParams)
        LinearLayout.LayoutParams childLp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT);

        TextView tvComment = new TextView(context);
        tvComment.setIncludeFontPadding(false);
        tvComment.setTextColor(mCommentStyle.getTextColor(0xff444444));
        tvComment.setTextSize(TypedValue.COMPLEX_UNIT_DIP,mCommentStyle.getTextSize(12));
        tvComment.setLayoutParams(childLp);
        tvComment.setTypeface(mCommentStyle.getFont());

        TextView tvText = new TextView(context);
        tvText.setIncludeFontPadding(false);
        tvText.setTextColor(mCandidateStyle.getTextColor(0xff000000));
        tvText.setTextSize(TypedValue.COMPLEX_UNIT_DIP,mCandidateStyle.getTextSize(22));
        tvText.setLayoutParams(childLp);
        tvText.setTypeface(mCandidateStyle.getFont());

        layout.addView(tvComment);
        layout.addView(tvText);

        FlexboxCandidateAdapter.CandidateViewHolder holder = new FlexboxCandidateAdapter.CandidateViewHolder(layout, tvComment, tvText);

        // 在这里只设置一次监听器
        holder.itemView.setOnClickListener(v -> {
            int position = holder.getBindingAdapterPosition(); // 获取当前实时位置
            if (position != RecyclerView.NO_POSITION && mData != null) {
                TrimeService.getInstance().selectCandidateItem(mData.get(position));
            }
        });

        return holder;
    }

    /**
     * 绑定数据到 ViewHolder。
     * 设置候选词文本、注释,并处理自动加载下一页。
     *
     * @param holder ViewHolder 实例。
     * @param position 数据位置索引。
     */
    @Override
    public void onBindViewHolder(@NonNull FlexboxCandidateAdapter.CandidateViewHolder holder, int position) {
        final CandidateItem data = mData.get(position);
        boolean hideComment = Config.is_hide_comment();
        boolean showComment = !TextUtils.isEmpty(data.getComment()) && (!hideComment || data.isSelfCreated());
        if (!showComment) {
            holder.tvComment.setVisibility(View.GONE);
        } else {
            holder.tvComment.setVisibility(View.VISIBLE);
            holder.tvComment.setText(data.getComment());
        }

        holder.tvText.setText(data.getText());
        int pl = holder.itemView.getPaddingLeft();
        int pr = holder.itemView.getPaddingRight();
        holder.itemView.setPadding(pl, showComment ? ThemeManager.dp2px(6) : 0, pr, showComment ? ThemeManager.dp2px(6) : 0);
        holder.itemView.setContentDescription(data.getText());
        // 2. 强制处理宽度更新
        holder.itemView.post(() -> {
            ViewGroup.LayoutParams lp = holder.itemView.getLayoutParams();
            if (lp != null) {
                int minW = ThemeManager.getCandidateMinWidth();
                lp.width = minW > 0 ? minW : ViewGroup.LayoutParams.WRAP_CONTENT;
                holder.itemView.setLayoutParams(lp);
                // 关键：强制要求父容器重新布局
                //holder.itemView.requestLayout();
            }
        });
        // 检查是否滑到了最后十项(提前加载,体验更好)
        if (position >= getItemCount() - 10) {
            // 使用 post 避免在布局阶段刷新
            holder.itemView.post(this::loadNextPage); // 触发加载下一页
        }
    }

    /**
     * 加载下一页候选词。
     * 从 CandidatesManager 获取下一页数据并追加到列表末尾。
     */
    private void loadNextPage() {
        if (mIsLoading) return; // 防止重复加载
        mIsLoading = true;
        ArrayList<CandidateItem> cand = CandidatesManager.next(); // 获取下一页数据
        if (!cand.isEmpty()) {
            int startPos = mData.size(); // 记录起始位置
            mData.addAll(cand); // 添加到数据列表
            // 不要 notifyDataSetChanged(),只通知新增的部分,性能更好
            notifyItemRangeInserted(startPos, cand.size()); // 通知插入新项
        }
        mIsLoading = false; // 重置加载标志
    }

    /**
     * 获取数据项数量。
     *
     * @return 候选词列表大小。
     */
    @Override
    public int getItemCount() {
        return mData == null ? 0 : mData.size();
    }

    /**
     * 设置新的候选词数据。
     * 清空旧数据,加载新数据。
     *
     * @param next 新的候选词列表。
     */
    @SuppressLint("NotifyDataSetChanged")
    public void setData(ArrayList<CandidateItem> next) {
        mData.clear(); // 清空旧数据
        String rawInput = com.osfans.trime.core.Rime.getRimeRawInput();
        ArrayList<CandidateItem> visibleItems = TrimeService.filterVisibleCandidateItems(rawInput, next);
        mData.addAll(TrimeService.getLearnedRawInputCandidates(rawInput, visibleItems));
        if (TrimeService.shouldInjectRawInputCandidate(rawInput, visibleItems)) {
            mData.add(new CandidateItem(rawInput));
        }
        CandidateItem preferredMixedCandidate = !mData.isEmpty() ? mData.get(0) : null;
        TrimeService.getInstance().setPreferredRawInputCandidate(
                preferredMixedCandidate != null ? preferredMixedCandidate.getText() : "");
        mData.addAll(visibleItems); // 添加过滤后的候选数据
        notifyDataSetChanged(); // 通知数据更新
    }

    /**
     * 获取所有候选词数据。
     *
     * @return 候选词列表。
     */
    public ArrayList<CandidateItem> getData() {
        return mData;
    }

    /**
     * 候选词 ViewHolder 类。
     * 持有候选词项的视图引用,包括注释文本和候选词文本。
     */
    public static class CandidateViewHolder extends RecyclerView.ViewHolder {

        /** 注释文本视图 */
        public final TextView tvComment;
        /** 候选词文本视图 */
        public final TextView tvText;

        /**
         * 构造函数。
         *
         * @param itemView 根视图。
         * @param tvComment 注释文本视图。
         * @param tvText 候选词文本视图。
         */
        public CandidateViewHolder(@NonNull View itemView, TextView tvComment, TextView tvText) {
            super(itemView);
            this.tvComment = tvComment;
            this.tvText = tvText;
        }
    }
}
