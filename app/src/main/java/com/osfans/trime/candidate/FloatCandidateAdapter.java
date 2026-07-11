/*
 * SPDX-FileCopyrightText: 2015 - 2026 Rime community
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package com.osfans.trime.candidate;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.os.Handler;
import android.text.TextPaint;
import android.text.TextUtils;
import android.util.Log;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.osfans.trime.TrimeService;
import com.osfans.trime.core.CandidateItem;
import com.osfans.trime.core.Rime;
import com.osfans.trime.theme.KeyStyle;
import com.osfans.trime.theme.ThemeManager;

import java.util.ArrayList;

/**
 * 浮动候选词适配器类。
 * 用于浮动候选词窗口的 RecyclerView,支持水平/垂直自适应布局。
 */
public class FloatCandidateAdapter extends RecyclerView.Adapter<FloatCandidateAdapter.CandidateViewHolder> {

    // ==================== 成员变量 ====================
    
    /** 候选词数据列表 */
    private final ArrayList<CandidateItem> mData;
    /** 候选词文本样式 */
    private final KeyStyle mCandidateStyle;
    /** 候选词注释样式 */
    private final KeyStyle mCommentStyle;
    /** 候选词按下状态样式 */
    private final KeyStyle mCandidatePressedStyle;
    /** 注释按下状态样式 */
    private final KeyStyle mCommentPressedStyle;
    /** 是否正在加载下一页 */
    private boolean mIsLoading;
    /** 当前选中的候选词索引 */
    private int mIdx;
    /** 之前选中的候选词索引 */
    private int oldIdx;
    /** 按下状态的背景 drawable */
    private final Drawable mCandidatePressedBackground;
    /** Handler,用于延迟任务 */
    private final Handler mHandler = new Handler();
    /** 最大项宽度 */
    private int mMaxWidth;

    /**
     * 构造函数。
     *
     * @param data 候选词数据列表。
     */
    public FloatCandidateAdapter(ArrayList<CandidateItem> data) {
        this.mData = data;
        mCandidateStyle = ThemeManager.getStyle().getKeyStyle("candidate"); // 获取候选词样式
        mCandidatePressedStyle = mCandidateStyle.getKeyStyle("pressed", mCandidateStyle); // 获取按下状态样式
        mCommentStyle = mCandidateStyle.getKeyStyle("comment", mCandidateStyle); // 获取注释样式
        mCommentPressedStyle = mCommentStyle.getKeyStyle("pressed", mCommentStyle); // 获取注释按下状态样式
        mCandidatePressedBackground = mCandidatePressedStyle.getBackground(); // 获取按下状态背景
    }

    @NonNull
    @Override
    public CandidateViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        Context context = parent.getContext();
        // 1. 创建容器（相当于 XML 里的 LinearLayout）
        LinearLayout layout = new LinearLayout(context) {
            @Override
            public CharSequence getAccessibilityClassName() {
                return "com.nirenr.trime.candidate.CandidateItem";
            }
        };
        layout.setGravity(Gravity.START);
        // 在 onCreateViewHolder 里的 layout 设置之后添加
        //TypedValue outValue = new TypedValue();
        //context.getTheme().resolveAttribute(android.R.attr.selectableItemBackground, outValue, true);
        //layout.setBackgroundResource(outValue.resourceId);
        layout.setClickable(true);
        layout.setOrientation(LinearLayout.HORIZONTAL);
        // 内边距：左右 8dp, 上下 2dp
        int px8 = ThemeManager.dp2px(12);
        int px1 = ThemeManager.dp2px(1);
        layout.setPadding(px8, px1, px8, px1);

        // 设置容器 LayoutParams
        int candidateMinWidth = ThemeManager.getCandidateMinWidth();
        RecyclerView.LayoutParams lp = new RecyclerView.LayoutParams(
                candidateMinWidth > 0 ? candidateMinWidth : ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT); // 横向列表高度建议 MATCH_PARENT
        layout.setLayoutParams(lp);

        TextView tvComment = new TextView(context);
        tvComment.setPadding(0, 0, 0, 0);
        tvComment.setLineSpacing(0, 0);
        tvComment.setGravity(Gravity.START);
        tvComment.setTextColor(mCommentStyle.getTextColor(0xff444444));
        tvComment.setTextSize(TypedValue.COMPLEX_UNIT_DIP, mCommentStyle.getTextSize(12));
        tvComment.setIncludeFontPadding(true);
        tvComment.setTypeface(mCommentStyle.getFont());

        TextView tvText = new TextView(context);
        tvText.setMarqueeRepeatLimit(-1);
        tvText.setPadding(0, 0, 0, 0);
        tvText.setLineSpacing(0, 0.1f);
        tvText.setGravity(Gravity.START);
        tvText.setTextColor(mCandidateStyle.getTextColor(0xff000000));
        tvText.setTextSize(TypedValue.COMPLEX_UNIT_DIP, mCandidateStyle.getTextSize(22));
        tvText.setIncludeFontPadding(true); // 去除由于字体规范导致的额外内边距
        tvText.setTypeface(mCandidateStyle.getFont());
        // 1. 先加注释（上方）
        LinearLayout.LayoutParams commentLp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT);
        commentLp.gravity = Gravity.START;
// 2. 后加正文（下方）
        LinearLayout.LayoutParams textLp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT);
        textLp.gravity = Gravity.START;

        layout.addView(tvText, textLp);
        layout.addView(tvComment, commentLp);
        CandidateViewHolder holder = new CandidateViewHolder(layout, tvComment, tvText);
        // 设置触摸监听,记录选中索引并滚动
        holder.itemView.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                if (event.getActionMasked() == MotionEvent.ACTION_DOWN) {
                    mIdx = holder.getBindingAdapterPosition(); // 记录当前触摸位置
                    //Rime.highlightRimeCandidate(mIdx);
                    scrollToIdx(); // 滚动到选中位置
                }
                return false;
            }
        });
        // 设置点击监听,处理候选词选择
        holder.itemView.setOnClickListener(v -> {
            int position = holder.getBindingAdapterPosition(); // 获取当前实时位置
            if (position != RecyclerView.NO_POSITION && mData != null) {
                TrimeService.getInstance().selectCandidateItem(mData.get(position));
            }
        });
        return holder;
    }

    @Override
    public void onBindViewHolder(@NonNull CandidateViewHolder holder, int position) {
        final CandidateItem data = mData.get(position);
        // 处理注释为空的情况，隐藏 View 节省空间
        if (TextUtils.isEmpty(data.getComment())) {
            holder.tvComment.setVisibility(View.GONE);
            holder.tvComment.setText(data.getComment());
        } else {
            holder.tvComment.setVisibility(View.VISIBLE);
            holder.tvComment.setText(data.getComment());
        }
        holder.tvText.setText(data.getText());

        // 根据文本宽度自动调整布局方向
        int tvCommentWidth = (int) holder.tvComment.getPaint().measureText(data.getComment()); // 测量注释宽度
        int tvTextWidth = (int) holder.tvText.getPaint().measureText(data.getText()); // 测量文本宽度
        int sWidth = holder.tvText.getResources().getDisplayMetrics().widthPixels; // 获取屏幕宽度
        if(tvCommentWidth+tvTextWidth>sWidth){
            // 如果总宽度超过屏幕,使用垂直布局
            ((LinearLayout)holder.itemView).setOrientation(LinearLayout.VERTICAL);
            mMaxWidth=Math.max(mMaxWidth,Math.max(tvCommentWidth,tvTextWidth)); // 更新最大宽度
            holder.tvComment.setMaxWidth(sWidth);
            holder.tvText.setMaxWidth(sWidth);
        }else {
            // 否则使用水平布局
            ((LinearLayout)holder.itemView).setOrientation(LinearLayout.HORIZONTAL);
            mMaxWidth=Math.max(mMaxWidth,tvCommentWidth+tvTextWidth); // 更新最大宽度
            holder.tvComment.setMaxWidth(sWidth);
            holder.tvText.setMaxWidth(sWidth);
        }
        Log.w("TAG", "onBindViewHolder:1 "+tvCommentWidth );
        Log.w("TAG", "onBindViewHolder:2 "+tvTextWidth );
        Log.w("TAG", "onBindViewHolder:3 "+mMaxWidth );
        holder.itemView.setContentDescription(data.getText());
        holder.itemView.setSelected(mIdx == position);
        holder.itemView.setBackground(mIdx == position ? mCandidatePressedBackground : null);
        holder.tvText.setTextColor(mIdx == position ? mCandidatePressedStyle.getTextColor() : mCandidateStyle.getTextColor());
        holder.tvComment.setTextColor(mIdx == position ? mCommentPressedStyle.getTextColor() : mCommentStyle.getTextColor());
        //holder.itemView.setElevation(mIdx == position? mCandidatePressedStyle.getElevation():0);
        // 2. 强制处理宽度更新
        holder.itemView.post(() -> {
            ViewGroup.LayoutParams lp = holder.itemView.getLayoutParams();
            if (lp != null) {
                int minW = ThemeManager.getCandidateMinWidth();
                lp.height = ViewGroup.LayoutParams.WRAP_CONTENT;
                lp.width = minW > 0 ? minW : ViewGroup.LayoutParams.WRAP_CONTENT;
                holder.itemView.setLayoutParams(lp);
                // 关键：强制要求父容器重新布局
                holder.itemView.requestLayout();
            }
        });
        // 检查是否滑到了最后三项（提前加载，体验更好）
        //if (position >= getItemCount() - 3) {
        //    // 使用 post 避免在布局阶段刷新
        //    holder.itemView.post(this::loadNextPage);
        //}
    }

    private void loadNextPage() {
        if (mIsLoading) return;
        mIsLoading = true;
        ArrayList<CandidateItem> cand = CandidatesManager.next();
        if (cand != null && !cand.isEmpty()) {
            int startPos = mData.size();
            mData.addAll(cand);
            // 不要 notifyDataSetChanged()，只通知新增的部分，性能更好
            notifyItemRangeInserted(startPos, cand.size());
        }
        mIsLoading = false;
    }

    @Override
    public int getItemCount() {
        return mData == null ? 0 : mData.size();
    }

    /**
     * 设置新的候选词数据。
     * 清空旧数据,加载新数据,并高亮第一个候选词。
     *
     * @param next 新的候选词列表。
     */
    public void setData(ArrayList<CandidateItem> next) {
        mIdx = 0; // 重置选中索引
        oldIdx = 0;
        mMaxWidth=0; // 重置最大宽度
        mData.clear(); // 清空旧数据
        mData.addAll(next); // 添加新数据
        if (!next.isEmpty())
            Rime.highlightRimeCandidate(next.get(0).getIndex()); // 高亮第一个候选词
        notifyDataSetChanged(); // 通知数据更新
    }

    private RecyclerView mRecyclerView;

    @Override
    public void onAttachedToRecyclerView(@NonNull RecyclerView recyclerView) {
        super.onAttachedToRecyclerView(recyclerView);
        this.mRecyclerView = recyclerView; // 获取列表控件引用
    }

    @Override
    public void onDetachedFromRecyclerView(@NonNull RecyclerView recyclerView) {
        super.onDetachedFromRecyclerView(recyclerView);
        this.mRecyclerView = null; // 防止内存泄漏
    }

    /**
     * 选中上一个候选词。
     *
     * @return true 表示成功移动,false 表示已在第一个位置。
     */
    public boolean prevCandidate() {
        if (mIdx <= 0) return false; // 已在第一个,无法再向前
        mIdx--;
        scrollToIdx(); // 滚动到选中位置
        return true;
    }

    /**
     * 选中下一个候选词。
     *
     * @return true 表示成功移动或已在最后一个位置。
     */
    public boolean nextCandidate() {
        if (mData.isEmpty())
            return false; // 数据为空,无法移动
        if (mData.size() - 1 == mIdx) return true; // 已在最后一个
        mIdx++;
        scrollToIdx(); // 滚动到选中位置
        return true;
    }

    /**
     * 滚动到当前选中的候选词位置。
     * 更新 UI 高亮状态并通知 Rime 引擎高亮对应候选词。
     */
    private void scrollToIdx() {
        if (mRecyclerView != null) {
            // 自动滚动到 mIdx 所在位置
            mRecyclerView.smoothScrollToPosition(mIdx); // 平滑滚动
        }
        notifyItemChanged(oldIdx); // 刷新旧选中项(取消高亮)
        oldIdx = mIdx; // 更新旧索引
        notifyItemChanged(mIdx); // 刷新新选中项(设置高亮)
        /*mHandler.postDelayed(new Runnable() {
            @Override
            public void run() {
                notifyItemChanged(mIdx);
            }
        },50);*/
        Rime.highlightRimeCandidate(mData.get(mIdx).getIndex()); // 通知 Rime 高亮
    }

    /**
     * 设置选中索引。
     *
     * @param idx 目标索引。
     */
    public void setIdx(int idx) {
        if (idx < 0 || idx >= getItemCount() - 1)
            return; // 索引越界,直接返回
        mIdx = idx;
        scrollToIdx(); // 滚动到指定位置
    }

    /**
     * 获取指定位置的候选词项。
     *
     * @param index 位置索引。
     * @return 候选词对象。
     */
    public CandidateItem getItem(int index) {
        return mData.get(index);
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
     * 获取最大项宽度。
     *
     * @param context Android 上下文。
     * @return 最大项宽度(包含边距)。
     */
    public int getMaxItemWidth(Context context) {
        Log.w("TAG", "onBindViewHolder:4 "+mMaxWidth );
        return mMaxWidth + ThemeManager.dp2px(32); // 加上左右边距
    }

    public static class CandidateViewHolder extends RecyclerView.ViewHolder {

        public final TextView tvComment;
        public final TextView tvText;

        public CandidateViewHolder(@NonNull View itemView, TextView tvComment, TextView tvText) {
            super(itemView);
            this.tvComment = tvComment;
            this.tvText = tvText;
        }
    }
}
