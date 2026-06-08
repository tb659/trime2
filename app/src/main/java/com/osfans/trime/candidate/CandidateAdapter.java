/*
 * SPDX-FileCopyrightText: 2015 - 2026 Rime community
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package com.osfans.trime.candidate;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.os.Handler;
import android.text.TextUtils;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.osfans.trime.Config;
import com.osfans.trime.TrimeService;
import com.osfans.trime.core.CandidateItem;
import com.osfans.trime.core.Rime;
import com.osfans.trime.theme.KeyStyle;
import com.osfans.trime.theme.Style;
import com.osfans.trime.theme.ThemeManager;

import java.util.ArrayList;

/**
 * 候选词适配器类。
 * 用于 RecyclerView 展示候选词列表,支持动态加载、高亮选中、触摸反馈等功能。
 */
public class CandidateAdapter extends RecyclerView.Adapter<CandidateAdapter.CandidateViewHolder> {

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
    private final Handler mHandler=new Handler();

    /**
     * 构造函数。
     *
     * @param data 候选词数据列表。
     */
    public CandidateAdapter(ArrayList<CandidateItem> data) {
        this.mData = data;
        mCandidateStyle = ThemeManager.getStyle().getKeyStyle("candidate"); // 获取候选词样式
        mCandidatePressedStyle = mCandidateStyle.getKeyStyle("pressed", mCandidateStyle); // 获取按下状态样式
        mCommentStyle = mCandidateStyle.getKeyStyle("comment", mCandidateStyle); // 获取注释样式
        mCommentPressedStyle = mCommentStyle.getKeyStyle("pressed", mCommentStyle); // 获取注释按下状态样式
        mCandidatePressedBackground = mCandidatePressedStyle.getBackground(); // 获取按下状态背景
    }

    /**
     * 创建 ViewHolder。
     * 构建候选词项的视图结构:垂直布局包含注释文本和候选词文本。
     *
     * @param parent 父容器。
     * @param viewType 视图类型。
     * @return 新创建的 CandidateViewHolder。
     */
    @NonNull
    @Override
    public CandidateAdapter.CandidateViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        Context context = parent.getContext();
        // 1. 创建容器（相当于 XML 里的 LinearLayout）
        LinearLayout layout = new LinearLayout(context) {
            @Override
            public CharSequence getAccessibilityClassName() {
                return "com.nirenr.trime.candidate.CandidateItem";
            }
        };
        // 2. 设置容器的布局
        layout.setGravity(Gravity.CENTER);
        // 在 onCreateViewHolder 里的 layout 设置之后添加
        //TypedValue outValue = new TypedValue();
        //context.getTheme().resolveAttribute(android.R.attr.selectableItemBackground, outValue, true);
        //layout.setBackgroundResource(outValue.resourceId);
        // 3. 设置容器可点击
        layout.setClickable(true);
        // 4. 添加容器
        layout.setOrientation(LinearLayout.VERTICAL);
        // 内边距：左右 8dp, 上下 2dp
        int px6 = ThemeManager.dp2px(6);
        int px1 = ThemeManager.dp2px(1);
        layout.setPadding(px6, px1, px6, px1);

        // 设置容器 LayoutParams
        int candidateMinWidth = ThemeManager.getCandidateMinWidth();
        RecyclerView.LayoutParams lp = new RecyclerView.LayoutParams(
                candidateMinWidth > 0 ? candidateMinWidth : ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.MATCH_PARENT); // 横向列表高度建议 MATCH_PARENT
        layout.setLayoutParams(lp);

        TextView tvComment = new TextView(context);
        tvComment.setSingleLine(true);
        tvComment.setPadding(0, 0, 0, 0);
        tvComment.setLineSpacing(0, 0);
        tvComment.setGravity(Gravity.CENTER);
        tvComment.setTextColor(mCommentStyle.getTextColor(0xff444444));
        tvComment.setTextSize(TypedValue.COMPLEX_UNIT_DIP, mCommentStyle.getTextSize(12));
        tvComment.setIncludeFontPadding(true);
        tvComment.setTypeface(mCommentStyle.getFont());

        TextView tvText = new TextView(context);
        tvText.setSingleLine(true);
        tvText.setMarqueeRepeatLimit(-1);
        tvText.setPadding(0, 0, 0, 0);
        tvText.setLineSpacing(0, 0.1f);
        tvText.setGravity(Gravity.CENTER);
        tvText.setTextColor(mCandidateStyle.getTextColor(0xff000000));
        tvText.setTextSize(TypedValue.COMPLEX_UNIT_DIP, mCandidateStyle.getTextSize(22));
        tvText.setMaxLines(1); // 防止意外换行导致宽度测量错误
        tvText.setIncludeFontPadding(true); // 去除由于字体规范导致的额外内边距
        tvText.setTypeface(mCandidateStyle.getFont());
        // 1. 先加注释（上方）
        LinearLayout.LayoutParams commentLp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                0, tvComment.getTextSize());
        commentLp.gravity = Gravity.CENTER;
// 2. 后加正文（下方）
        LinearLayout.LayoutParams textLp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                0, tvText.getTextSize());
        textLp.gravity = Gravity.CENTER;

        layout.addView(tvComment, commentLp);
        layout.addView(tvText, textLp);

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
                CandidateItem item = mData.get(position);
                if (item.getIndex() == -1) {
                    // index 为 -1 表示直接提交文本
                    TrimeService.getInstance().commitText(item.getText());
                    TrimeService.getInstance().setCandidates(null);
                } else {
                    // 正常候选词,通知 Rime 引擎选择
                    TrimeService.getInstance().selectCandidate(item.getIndex());
                }
            }
        });

        return holder;
    }

    /**
     * 绑定数据到 ViewHolder。
     * 设置候选词文本、注释、样式,并处理自动加载下一页。
     *
     * @param holder ViewHolder 实例。
     * @param position 数据位置索引。
     */
    @Override
    public void onBindViewHolder(@NonNull CandidateAdapter.CandidateViewHolder holder, int position) {
        final CandidateItem data = mData.get(position);
        boolean hideComment = Config.is_hide_comment();
        // 处理注释为空的情况，隐藏 View 节省空间
        if (TextUtils.isEmpty(data.getComment())|| hideComment) {
            holder.tvComment.setVisibility(View.GONE);
        } else {
            holder.tvComment.setVisibility(View.VISIBLE);
            holder.tvComment.setText(data.getComment());
        }
        holder.tvText.setText(data.getText());
        int pl = holder.itemView.getPaddingLeft();
        int pr = holder.itemView.getPaddingRight();
        holder.itemView.setPadding(pl, hideComment ? 0 : ThemeManager.dp2px(1), pr, hideComment ? 0 : ThemeManager.dp2px(1));
        if(data.getText().length()>32){
            holder.tvText.setMaxWidth(TrimeService.getInstance().getWidth());
            holder.tvText.setEllipsize(TextUtils.TruncateAt.MARQUEE);
        } else {
            holder.tvText.setMaxWidth(Integer.MAX_VALUE);
            holder.tvText.setEllipsize(null);
        }
        holder.itemView.setContentDescription(data.getText());
        holder.itemView.setSelected(mIdx == position);
        holder.itemView.setBackground(mIdx == position ? mCandidatePressedBackground : null);
        boolean pressed = mIdx == position;
        {
            KeyStyle tc = pressed ? mCandidatePressedStyle : mCandidateStyle;
            holder.tvText.setTextColor(tc.getTextColor());
            holder.tvText.setTextSize(TypedValue.COMPLEX_UNIT_DIP, tc.getTextSize());
            holder.tvText.setTypeface(tc.getFont());
            holder.tvText.setGravity(tc.getGravity(Gravity.CENTER));
        }
        {
            KeyStyle cc = pressed ? mCommentPressedStyle : mCommentStyle;
            holder.tvComment.setTextColor(cc.getTextColor());
            holder.tvComment.setTextSize(TypedValue.COMPLEX_UNIT_DIP, cc.getTextSize());
            holder.tvComment.setTypeface(cc.getFont());
            holder.tvComment.setGravity(cc.getGravity(Gravity.CENTER));
        }
        {
            KeyStyle cs = pressed ? mCandidatePressedStyle : mCandidateStyle;
            ((LinearLayout) holder.itemView).setGravity(cs.getGravity(Gravity.CENTER));
            Style padding = cs.getStyle("padding");
            int paddingLeft = padding.getSize("left", -1);
            if (paddingLeft >= 0) {
                int paddingTop = padding.getSize("top", -1);
                int paddingRight = padding.getSize("right", -1);
                int paddingBottom = padding.getSize("bottom", -1);
                holder.itemView.setPadding(
                    paddingTop >= 0 ? paddingLeft : holder.itemView.getPaddingLeft(),
                    paddingTop >= 0 ? paddingTop : holder.itemView.getPaddingTop(),
                    paddingRight >= 0 ? paddingRight : holder.itemView.getPaddingRight(),
                    paddingBottom >= 0 ? paddingBottom : holder.itemView.getPaddingBottom());
            }
            Style margins = cs.getStyle("margins");
            int ml = margins.getSize("left", -1);
            if (ml >= 0) {
                ViewGroup.MarginLayoutParams mlp = (ViewGroup.MarginLayoutParams) holder.itemView.getLayoutParams();
                if (mlp != null) {
                    mlp.setMargins(ml, margins.getSize("top", -1),
                            margins.getSize("right", -1), margins.getSize("bottom", -1));
                }
            }
        }
        // 2. 强制处理宽度更新
        holder.itemView.post(() -> {
            ViewGroup.LayoutParams lp = holder.itemView.getLayoutParams();
            if (lp != null) {
                int minW = ThemeManager.getCandidateMinWidth();
                lp.width = minW > 0 ? minW : ViewGroup.LayoutParams.WRAP_CONTENT;
                lp.height = ViewGroup.LayoutParams.MATCH_PARENT;
                holder.itemView.setLayoutParams(lp);
                // 关键：强制要求父容器重新布局
                //holder.itemView.requestLayout();
            }
        });
        // 检查是否滑到了最后三项(提前加载,体验更好)
        if (position >= getItemCount() - 3) {
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
        if (cand != null && !cand.isEmpty()) {
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
     * 清空旧数据,加载新数据,并高亮第一个候选词。
     *
     * @param next 新的候选词列表。
     */
    public void setData(ArrayList<CandidateItem> next) {
        mIdx = 0; // 重置选中索引
        oldIdx = 0;
        mData.clear(); // 清空旧数据
        if (next.isEmpty() && Rime.isComposing() && !TextUtils.isEmpty(Rime.getRimeRawInput())) {
            // 无候选词且正在编码时，显示原始输入码作为候选（点击直接上屏）
            mData.add(new CandidateItem(Rime.getRimeRawInput()));
        } else {
            mData.addAll(next); // 添加新数据
            if (!next.isEmpty())
                Rime.highlightRimeCandidate(next.get(0).getIndex()); // 高亮第一个候选词
        }
        notifyDataSetChanged(); // 通知数据更新
    }

    /** RecyclerView 引用 */
    private RecyclerView mRecyclerView;

    /**
     * 当适配器附加到 RecyclerView 时调用。
     *
     * @param recyclerView RecyclerView 实例。
     */
    @Override
    public void onAttachedToRecyclerView(@NonNull RecyclerView recyclerView) {
        super.onAttachedToRecyclerView(recyclerView);
        this.mRecyclerView = recyclerView; // 获取列表控件引用
    }

    /**
     * 当适配器从 RecyclerView 分离时调用。
     *
     * @param recyclerView RecyclerView 实例。
     */
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
        if(idx<0||idx>=getItemCount()-1)
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
