/*
 * Copyright (C) 2015-present, osfans
 * waxaca@163.com https://github.com/osfans
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package com.osfans.trime;

import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Typeface;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.Build.VERSION;
import android.os.Build.VERSION_CODES;
import android.text.Html;
import android.text.Layout;
import android.text.SpannableStringBuilder;
import android.text.TextPaint;
import android.text.TextUtils;
import android.text.method.LinkMovementMethod;
import android.text.style.AbsoluteSizeSpan;
import android.text.style.AlignmentSpan;
import android.text.style.BackgroundColorSpan;
import android.text.style.ClickableSpan;
import android.text.style.ForegroundColorSpan;
import android.text.style.UnderlineSpan;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.widget.TextView;

import com.androlua.LuaBitmapDrawable;
import com.osfans.trime.candidate.CandidatesManager;
import com.osfans.trime.core.CandidateItem;
import com.osfans.trime.core.Rime;
import com.osfans.trime.core.RimeProto;
import com.osfans.trime.theme.KeyStyle;
import com.osfans.trime.theme.Style;
import com.osfans.trime.theme.ThemeManager;

import java.util.ArrayList;
import java.util.Map;


/**
 * 编码区组件，用于显示已输入的按键编码信息。
 * 支持使用方向键或触屏移动光标位置，同时可以显示候选词、云输入结果等内容。
 * 该组件继承自 TextView，通过 SpannableStringBuilder 实现富文本显示效果。
 */
@SuppressLint("AppCompatCustomView")
public class Composition extends TextView {
    // 日志标签，用于 Logcat 输出
    private static final String TAG = "Composition";
    private static final char SOFT_CURSOR = '‸';
    // 键盘文本尺寸相关属性（单位：像素）
    private int key_text_size, text_size, label_text_size, candidate_text_size, comment_text_size;
    // 文本颜色相关属性（ARGB 格式）
    private int key_text_color, text_color, label_color, candidate_text_color, comment_text_color;
    // 高亮状态下的文本颜色（选中项、候选词等）
    private int hilited_text_color, hilited_candidate_text_color, hilited_comment_text_color;
    // 背景颜色相关属性（普通状态和高亮状态）
    private int back_color, hilited_back_color, hilited_candidate_back_color;
    // 键盘背景颜色（可为空，表示使用默认值）
    private Integer key_back_color;
    // 字体类型定义（分别用于正文、标签、候选词、注释）
    private Typeface tfText, tfLabel, tfCandidate, tfComment;
    // 编码区的光标位置范围 [起始位置, 结束位置]
    private int composition_pos[] = new int[2];
    // 最大显示长度和粘性行数（粘性行指始终显示的固定行）
    private int max_length, sticky_lines;
    // 最大候选词条目数（-1 表示无限制）
    private int max_entries = 5;
    // 云输入最大条目数（0 表示禁用云输入）
    private int cloud_max_entries = 0;
    // 是否使用游标显示当前选中的候选词；是否显示注释信息
    private boolean candidate_use_cursor, show_comment=true;
    // 当前高亮的候选词索引（-1 表示无高亮）
    private int highlightIndex;
    // 主题样式配置对象，包含窗口布局的各个组件样式
    private Style components;
    // 可跨距的字符串构建器，用于构建富文本内容
    private SpannableStringBuilder ss;
    // Span 标志位，用于控制文本样式的生效范围（通常为 0 或 Spanned.SPAN_EXCLUSIVE_EXCLUSIVE）
    private int span = 0;
    // 可移动性配置（"false"/"once"/"true"），控制编码区是否可拖动
    private String movable;
    // 可移动区域的位置范围 [起始位置, 结束位置]
    private int move_pos[] = new int[2];
    // 是否为首次移动操作（用于初始化拖动起点）
    private boolean first_move = true;
    // 拖动时的偏移量（X 和 Y 方向）
    private float mDx, mDy;
    // 当前视图在屏幕上的位置坐标（X 和 Y）
    private int mCurrentX, mCurrentY;
    // 当前显示的候选词数量
    private int candidate_num;
    // 是否显示所有短语（包括长度不足的候选词）
    private boolean all_phrases;
    // 云输入词条计数器和每行最大长度限制
    private int cloud_num = 1;
    private int cloud_line_length;
    // 云输入词条之间的分隔符（默认为空格）
    private String cloudSep = " ";
    // 最小匹配长度（用于过滤候选词）
    private int min_length;
    // Alpha 透明度值（预留字段，当前未使用）
    private int mAlpha;
    // 高亮标签的颜色（可为空）
    private Integer hilited_label_color;
    // 是否强制单行显示模式
    private boolean mSingle;
    // 是否将末尾内容显示在顶部（反向排列模式）
    private boolean end_top;
    // Rime 输入法引擎的上下文数据，包含编码、候选词等信息
    private RimeProto.Context mRimeContext;

    /**
     * 构造函数，创建 Composition 视图实例并执行重置操作以应用初始样式。
     *
     * @param context Android 上下文环境，用于访问系统资源和主题配置。
     */
    public Composition(Context context) {
        super(context);
        reset();
    }
    /**
     * 添加云输入候选词到编码区显示。
     * 该方法会根据配置的最大条目数和最小长度限制来决定是否添加该词条。
     *
     * @param cloud 云输入候选词对象，包含文本内容和注释信息。
     */
    public void addCloud(CandidateItem cloud) {
        // 检查是否超过云输入最大条目数限制（如果设置了限制且不为 0）
        if (cloud_num > cloud_max_entries && cloud_max_entries != 0)
            return;
        // 如果云输入最大条目数小于 5 且候选词长度为 1，则跳过（避免显示过多单字）
        if (cloud_max_entries < 5 && cloud.getText().length() == 1)
            return;
        // 记录当前字符串的起始位置，用于后续设置 Span 样式范围
        int start = ss.length();
        // 第一个云输入词条需要特殊处理：添加序号前缀和换行符
        if (cloud_num == 1) {
            // 如果没有设置最小条目数或最小长度，使用数字序号；否则使用云朵符号 ☁
            if (max_entries < 1 || min_length < 1)
                ss.append("\n").append(String.valueOf(cloud_num)).append(".");
            else
                ss.append("\n☁").append(String.valueOf(cloud_num)).append(".");
            // 如果当前行长度加上候选词长度超过最大行长度限制，则换行并重新开始计数
        } else if (max_length > 1 && cloud_line_length + cloud.getText().length() > max_length) {
            ss.append("\n").append(String.valueOf(cloud_num)).append(".");
            cloud_line_length = 0;
            // 否则在同一行内添加分隔符和序号
        } else {
            ss.append(cloudSep).append(String.valueOf(cloud_num)).append(".");
        }
        // 更新当前行的累计长度（累加候选词的文本长度）
        cloud_line_length += cloud.getText().length();
        // 记录序号部分的结束位置，并设置对齐方式和字体大小样式
        int end = ss.length();
        ss.setSpan(new AlignmentSpan.Standard(Layout.Alignment.ALIGN_NORMAL), start, end, span);
        ss.setSpan(new AbsoluteSizeSpan(candidate_text_size), start, end, span);
        // 更新起始位置为候选词文本的开始处
        start = ss.length();
        // 追加候选词的文本内容
        ss.append(cloud.getText());
        // 记录候选词文本的结束位置
        end = ss.length();
        // 为候选词文本设置对齐方式、点击事件响应（CloudSpan）、以及字体大小样式
        ss.setSpan(new AlignmentSpan.Standard(Layout.Alignment.ALIGN_NORMAL), start, end, span);
        ss.setSpan(
                new CloudSpan(
                        cloud,
                        tfLabel,
                        hilited_candidate_text_color,
                        hilited_candidate_back_color,
                        label_color),
                start,
                end,
                span);
        ss.setSpan(new AbsoluteSizeSpan(candidate_text_size), start, end, span);
    }

    /**
     * 设置单个云输入候选词到编码区。
     * 如果该候选词已存在于当前文本中，则不会重复添加。
     *
     * @param cloud 云输入候选词对象。
     */
    public void setCloud(CandidateItem cloud) {
        // 如果云输入功能被禁用（最大条目数为 0），则直接返回
        if (cloud_max_entries == 0) {
            return;
        }
        // 重置云输入计数器：从第 1 个词条开始，行长度清零
        cloud_num = 1;
        cloud_line_length = 0;
        // 如果 SpannableStringBuilder 尚未初始化，则创建新实例
        if (ss == null)
            ss = new SpannableStringBuilder();
        // 检查候选词是否已存在，避免重复添加
        if (ss.toString().contains(cloud.getText()))
            return;
        // 调用内部方法添加云输入词条到显示区域
        addCloud(cloud);
        // 递增云输入词条计数器，为下一个词条做准备
        cloud_num++;
        // 如果文本总长度超过最大长度限制，则取消单行显示模式，允许多行展示
        if (ss.length() > max_length) {
            setSingleLine(false); // 设置多行显示
        }/*else {
            measure(0,0);
            if(getMeasuredWidth()/2>getMaxWidth())
                setSingleLine(false);
        }*/
        setText(ss);
    }

    /**
     * 添加字符串类型的云输入候选词到编码区显示。
     * 与 addCloud(CandidateItem) 类似，但接受简单的字符串参数。
     *
     * @param cloud 云输入候选词文本字符串。
     */
    public void addCloud(String cloud) {
        // 检查是否超过云输入最大条目数限制
        if (cloud_num > cloud_max_entries && cloud_max_entries != 0)
            return;
        // 如果云输入最大条目数小于 5 且候选词长度为 1，则跳过
        if (cloud_max_entries < 5 && cloud.length() == 1)
            return;
        // 记录起始位置并构建带序号的显示文本
        int start = ss.length();
        if (cloud_num == 1) {
            if (max_entries < 1 || min_length < 1)
                ss.append("\n").append(String.valueOf(cloud_num)).append(".");
            else
                ss.append("\n☁").append(String.valueOf(cloud_num)).append(".");
        } else if (max_length > 1 && cloud_line_length + cloud.length() > max_length) {
            ss.append("\n").append(String.valueOf(cloud_num)).append(".");
            cloud_line_length = 0;
        } else {
            ss.append(cloudSep).append(String.valueOf(cloud_num)).append(".");
        }
        // 更新行长度并设置文本样式
        cloud_line_length += cloud.length();
        int end = ss.length();
        ss.setSpan(new AlignmentSpan.Standard(Layout.Alignment.ALIGN_NORMAL), start, end, span);
        ss.setSpan(new AbsoluteSizeSpan(candidate_text_size), start, end, span);
        start = ss.length();
        ss.append(cloud);
        end = ss.length();
        ss.setSpan(new AlignmentSpan.Standard(Layout.Alignment.ALIGN_NORMAL), start, end, span);
        ss.setSpan(
                new CloudSpan(
                        cloud,
                        tfLabel,
                        hilited_candidate_text_color,
                        hilited_candidate_back_color,
                        label_color),
                start,
                end,
                span);
        ss.setSpan(new AbsoluteSizeSpan(candidate_text_size), start, end, span);
    }

    /**
     * 设置字符串类型的云输入候选词到编码区。
     *
     * @param cloud 云输入候选词文本字符串。
     */
    public void setCloud(String cloud) {
        // 如果云输入功能被禁用，则直接返回
        if (cloud_max_entries == 0) {
            return;
        }
        // 重置计数器并初始化 SpannableStringBuilder
        cloud_num = 1;
        cloud_line_length = 0;
        if (ss == null)
            ss = new SpannableStringBuilder();
        // 检查是否已存在，避免重复
        if (ss.toString().contains(cloud))
            return;
        // 添加云输入词条
        addCloud(cloud);
        cloud_num++;
        // 根据长度决定是否启用多行显示
        if (ss.length() > max_length) {
            setSingleLine(false); // 设置多行显示
        }/*else {
            measure(0,0);
            if(getMeasuredWidth()/2>getMaxWidth())
                setSingleLine(false);
        }*/
        setText(ss);
    }

    /**
     * 批量设置云输入候选词列表到编码区。
     * 如果云输入被禁用，则将候选词列表传递给 TrimeService 处理。
     *
     * @param cloud 云输入候选词字符串列表。
     */
    public void setCloud(ArrayList<String> cloud) {
        // 如果云输入功能被禁用，则将候选词交给 CandidatesManager 处理
        if (cloud_max_entries == 0) {
            TrimeService.getInstance().setCandidates(cloud);
            return;
        }
        // 如果只允许显示一个云输入词条，则不进行处理
        if (cloud_max_entries == 1) {
            return;
        }
        // 获取当前文本内容用于去重检查
        String text = ss.toString();
        cloud_num = 1;
        cloud_line_length = 0;
        // 遍历云输入列表，逐个添加到显示区域
        for (String s : cloud) {
            if (text.contains(s))
                continue;
            addCloud(s);
            cloud_num++;
            // 如果达到最大条目数限制（且大于 0），则提前退出循环
            if (cloud_max_entries > 0 && cloud_num > 5)
                break;
        }
        // 根据文本长度决定是否启用多行显示
        if (ss.length() > max_length)
            setSingleLine(false); // 设置多行显示
        setText(ss);
    }

    /**
     * 添加组合输入词条到编码区显示。
     * 支持 HTML 格式的文本内容。
     *
     * @param cloud1 组合输入文本，可能包含 HTML 标签。
     */
    private void addComposition(String cloud1) {
        CharSequence cloud=cloud1;
        // 如果文本以 <html> 开头，则解析为 HTML 富文本
        if(cloud1.startsWith("<html>")){
            cloud = Html.fromHtml(cloud1);
        }
        // 构建带序号的显示文本
        int start = ss.length();
        if (cloud_num == 1) {
            ss.append("\n").append(String.valueOf(cloud_num)).append(".");
        } else if (max_length > 1 && cloud_line_length + cloud.length() > max_length) {
            ss.append("\n").append(String.valueOf(cloud_num)).append(".");
            cloud_line_length = 0;
        } else {
            ss.append(cloudSep).append(String.valueOf(cloud_num)).append(".");
        }
        // 更新行长度并设置文本样式
        cloud_line_length += cloud.length();
        int end = ss.length();
        ss.setSpan(new AlignmentSpan.Standard(Layout.Alignment.ALIGN_NORMAL), start, end, span);
        ss.setSpan(new AbsoluteSizeSpan(candidate_text_size), start, end, span);
        start = ss.length();
        ss.append(cloud);
        end = ss.length();
        ss.setSpan(new AlignmentSpan.Standard(Layout.Alignment.ALIGN_NORMAL), start, end, span);
        // 使用 CloudSpan2 处理可点击的组合输入词条
        ss.setSpan(
                new CloudSpan2(
                        cloud,
                        tfLabel,
                        hilited_candidate_text_color,
                        hilited_candidate_back_color,
                        label_color),
                start,
                end,
                span);
        ss.setSpan(new AbsoluteSizeSpan(candidate_text_size), start, end, span);
    }

    /**
     * 批量添加组合输入词条列表到编码区。
     * 如果当前没有正在编辑的文本，则先初始化 SpannableStringBuilder 并添加移动区域。
     *
     * @param list 组合输入文本字符串列表。
     */
    public void addCompositions(ArrayList<String> list) {
        // 如果 Rime 上下文中没有预编辑文本或 SpannableStringBuilder 为空，则进行初始化
        if (TextUtils.isEmpty(mRimeContext.getComposition().getPreedit()) || ss == null) {
            ss = new SpannableStringBuilder();
            int len = components.getLength();
            // 遍历窗口组件，添加可移动区域标记
            for (int i = 0; i < len; i++) {
                Style m = components.getStyle(i);
                if (m.hasKey("move")) appendMove(m);
            }
        }

        // 重置云输入计数器并逐个添加组合输入词条
        cloud_num = 1;
        for (String s : list) {
            addComposition(s);
            cloud_num++;
        }
        // 根据文本长度决定是否启用多行显示
        if (ss.length() > max_length)
            setSingleLine(false); // 设置多行显示
        setText(ss);
    }

    /**
     * 设置编码区是否为单行显示模式。
     *
     * @param single true 表示强制单行显示，false 表示允许多行显示。
     */
    public void setCompositionSingleLine(boolean single) {
        mSingle = single;
    }

    /**
     * 设置编码区末尾内容是否显示在顶部（反向排列模式）。
     *
     * @param b true 表示末尾内容在顶部，false 表示正常顺序。
     */
    public void setCompositionEndTop(boolean b) {
        end_top = b;
    }

    /**
     * CloudSpan2 内部类：用于处理可点击的组合输入词条。
     * 继承自 ClickableSpan，实现点击提交文本的功能。
     */
    private class CloudSpan2 extends ClickableSpan {
        CharSequence index;  // 词条文本内容
        Typeface tf;         // 字体类型
        int hi_text, hi_back, text;  // 高亮文本颜色、高亮背景颜色、普通文本颜色

        /**
         * 构造函数，初始化 CloudSpan2 的各项属性。
         *
         * @param i       词条文本内容
         * @param _tf     字体类型
         * @param _hi_text 高亮文本颜色
         * @param _hi_back 高亮背景颜色
         * @param _text   普通文本颜色
         */
        public CloudSpan2(CharSequence i, Typeface _tf, int _hi_text, int _hi_back, int _text) {
            super();
            index = i;
            tf = _tf;
            hi_text = _hi_text;
            hi_back = _hi_back;
            text = _text;
        }

        /**
         * 点击事件处理：提交文本并清空编码区。
         *
         * @param tv 被点击的视图对象。
         */
        @Override
        public void onClick(View tv) {
            TrimeService.getInstance().commitTextAndClearComposition(index);
        }

        /**
         * 更新绘制状态：设置字体、颜色，禁用下划线。
         *
         * @param ds 文本绘制对象。
         */
        @Override
        public void updateDrawState(TextPaint ds) {
            ds.setUnderlineText(false);
            ds.setTypeface(tf);
            ds.setColor(text);
        }
    }

    /**
     * CloudSpan 内部类：用于处理可点击的云输入候选词。
     * 继承自 ClickableSpan，支持点击提交和添加到云输入历史。
     */
    private class CloudSpan extends ClickableSpan {
        String comment;  // 候选词注释
        String index;    // 候选词文本
        Typeface tf;     // 字体类型
        int hi_text, hi_back, text;  // 颜色属性

        /**
         * 构造函数，使用 CandidateItem 对象初始化。
         *
         * @param i        候选词对象
         * @param _tf      字体类型
         * @param _hi_text 高亮文本颜色
         * @param _hi_back 高亮背景颜色
         * @param _text    普通文本颜色
         */
        public CloudSpan(CandidateItem i, Typeface _tf, int _hi_text, int _hi_back, int _text) {
            super();
            index = i.getText();
            comment = i.getComment();
            tf = _tf;
            hi_text = _hi_text;
            hi_back = _hi_back;
            text = _text;
        }

        /**
         * 构造函数，使用字符串初始化。
         *
         * @param i        候选词文本
         * @param _tf      字体类型
         * @param _hi_text 高亮文本颜色
         * @param _hi_back 高亮背景颜色
         * @param _text    普通文本颜色
         */
        public CloudSpan(String i, Typeface _tf, int _hi_text, int _hi_back, int _text) {
            super();
            index = i;
            tf = _tf;
            hi_text = _hi_text;
            hi_back = _hi_back;
            text = _text;
        }

        /**
         * 点击事件处理：提交文本、清空编码区，并将该词条添加到云输入历史。
         *
         * @param tv 被点击的视图对象。
         */
        @Override
        public void onClick(View tv) {
            TrimeService.getInstance().commitTextAndClearComposition(index);
            TrimeService.getInstance().addCloud(index);
            if(!TextUtils.isEmpty(comment))
                TrimeService.getInstance().addCloud(index,comment);
        }

        /**
         * 更新绘制状态：设置字体和颜色，禁用下划线。
         *
         * @param ds 文本绘制对象。
         */
        @Override
        public void updateDrawState(TextPaint ds) {
            ds.setUnderlineText(false);
            ds.setTypeface(tf);
            ds.setColor(text);
        }
    }

    /**
     * CompositionSpan 内部类：用于显示编码区预编辑文本的样式。
     * 继承自 UnderlineSpan，提供下划线、字体和颜色设置。
     */
    private class CompositionSpan extends UnderlineSpan {
        public CompositionSpan() {
            super();
        }

        /**
         * 更新绘制状态：设置字体、文本颜色和背景颜色。
         *
         * @param ds 文本绘制对象。
         */
        @Override
        public void updateDrawState(TextPaint ds) {
            ds.setTypeface(tfText);
            ds.setColor(text_color);
            ds.bgColor = back_color;
        }
    }

    /**
     * CandidateSpan 内部类：用于显示候选词的样式和点击事件。
     * 继承自 ClickableSpan，支持高亮显示选中的候选词。
     */
    private class CandidateSpan extends ClickableSpan {
        int index;       // 候选词索引
        Typeface tf;     // 字体类型
        int hi_text, hi_back, text;  // 颜色属性

        /**
         * 构造函数，初始化候选词 Span 的各项属性。
         *
         * @param i        候选词索引
         * @param _tf      字体类型
         * @param _hi_text 高亮文本颜色
         * @param _hi_back 高亮背景颜色
         * @param _text    普通文本颜色
         */
        public CandidateSpan(int i, Typeface _tf, int _hi_text, int _hi_back, int _text) {
            super();
            index = i;
            tf = _tf;
            hi_text = _hi_text;
            hi_back = _hi_back;
            text = _text;
        }

        /**
         * 点击事件处理：选择分页中的指定候选词。
         *
         * @param tv 被点击的视图对象。
         */
        @Override
        public void onClick(View tv) {
            TrimeService.getInstance().selectPagedCandidateFromUi(index);
        }

        /**
         * 更新绘制状态：根据是否为高亮索引设置不同的颜色。
         *
         * @param ds 文本绘制对象。
         */
        @Override
        public void updateDrawState(TextPaint ds) {
            ds.setUnderlineText(false);
            ds.setTypeface(tf);
            if (index == highlightIndex) {
                ds.setColor(hi_text);
                ds.bgColor = hi_back;
            } else {
                ds.setColor(text);
            }
        }
    }

    /**
     * EventSpan 内部类：用于显示可点击的事件按钮。
     * 继承自 ClickableSpan，点击时触发相应的事件处理。
     */
    private class EventSpan extends ClickableSpan {
        Event event;  // 事件对象

        /**
         * 构造函数，初始化事件 Span。
         *
         * @param e 事件对象。
         */
        public EventSpan(Event e) {
            super();
            event = e;
        }

        /**
         * 点击事件处理：触发事件的执行。
         *
         * @param tv 被点击的视图对象。
         */
        @Override
        public void onClick(View tv) {
            TrimeService.getInstance().onEvent(event);
        }

        /**
         * 更新绘制状态：设置键盘文本颜色和背景颜色，禁用下划线。
         *
         * @param ds 文本绘制对象。
         */
        @Override
        public void updateDrawState(TextPaint ds) {
            ds.setUnderlineText(false);
            ds.setColor(key_text_color);
            if (key_back_color != null) ds.bgColor = key_back_color;
        }
    }

    /**
     * LetterSpacingSpan 内部类：用于设置字符间距。
     * 继承自 UnderlineSpan，通过 TextPaint 设置字母间距。
     */
    @TargetApi(21)
    public class LetterSpacingSpan extends UnderlineSpan {
        private float letterSpacing;  // 字符间距值

        /**
         * 构造函数，设置字符间距。
         *
         * @param letterSpacing 字符间距值（单位：em）。
         */
        public LetterSpacingSpan(float letterSpacing) {
            this.letterSpacing = letterSpacing;
        }

        /**
         * 更新绘制状态：应用字符间距设置。
         *
         * @param ds 文本绘制对象。
         */
        @Override
        public void updateDrawState(TextPaint ds) {
            ds.setLetterSpacing(letterSpacing);
        }
    }


    /**
     * 触摸事件处理：支持点击编码区移动光标和拖动编码区位置。
     *
     * @param event 触摸事件对象。
     * @return true 表示事件已处理，false 表示传递给父类处理。
     */
    @Override
    public boolean onTouchEvent(MotionEvent event) {
        int action = event.getAction();
        // 处理手指抬起事件：点击编码区移动光标位置
        if (action == MotionEvent.ACTION_UP) {
            int n = getOffsetForPosition(event.getX(), event.getY());
            if (composition_pos[0] <= n && n <= composition_pos[1]) {
                String compositionText = getText().toString().substring(composition_pos[0], n);
                int cursor = countEffectiveCompositionChars(compositionText);
                int inputLength = Rime.getRimeRawInput().length();
                cursor = Math.max(0, Math.min(cursor, inputLength));
                TrimeService trime = TrimeService.getInstance();
                trime.setPendingCompositionCaret(cursor);
                trime.getRime().moveCursorPos(cursor);
                trime.updateComposing();
                return true;
            }
            // 处理拖动事件：如果允许移动且为移动或按下动作
        } else if (!movable.contentEquals("false")
                && (action == MotionEvent.ACTION_MOVE || action == MotionEvent.ACTION_DOWN)) {
            int n = getOffsetForPosition(event.getX(), event.getY());
            if (move_pos[0] <= n && n <= move_pos[1]) {
                // 处理按下事件：记录初始位置
                if (action == MotionEvent.ACTION_DOWN) {
                    if (first_move || movable.contentEquals("once")) {
                        first_move = false;
                        int location[] = TrimeService.getInstance().getLocationInWindow(this);
                        mCurrentX = location[0];
                        mCurrentY = location[1];
                    }
                    mDx = mCurrentX - event.getRawX();
                    mDy = mCurrentY - event.getRawY();
                } else { //MotionEvent.ACTION_MOVE - 处理移动事件
                    mCurrentX = (int) (event.getRawX() + mDx);
                    mCurrentY = (int) (event.getRawY() + mDy);
                    setTranslationX(mCurrentX);
                    setTranslationY(mCurrentY);
                }
                return true;
            }
        }

        return super.onTouchEvent(event);
    }

    private static int countEffectiveCompositionChars(String text) {
        if (TextUtils.isEmpty(text)) return 0;
        int count = 0;
        for (int i = 0; i < text.length(); i++) {
            char ch = text.charAt(i);
            if (ch == ' ' || ch == SOFT_CURSOR) continue;
            count++;
        }
        return count;
    }

    private static String withSoftCursor(String text, int caret) {
        if (text == null) return "";
        int safeCaret = Math.max(0, Math.min(caret, text.length()));
        return text.substring(0, safeCaret) + SOFT_CURSOR + text.substring(safeCaret);
    }

    /**
     * 设置是否显示候选词注释。
     *
     * @param value true 表示显示注释，false 表示隐藏注释。
     */
    public void setShowComment(boolean value) {
        show_comment = value;
    }

    /**
     * 设置最大候选词条目数。
     *
     * @param i 最大条目数（-1 表示无限制）。
     */
    public void setMaxEntries(int i) {
        max_entries = i;
    }

    /**
     * 重置编码区组件的样式和配置。
     * 该方法从主题管理器中读取最新的样式配置，并应用到当前视图。
     * 包括获取各种按键样式、设置布局参数、文字大小、颜色、字体等。
     */
    public void reset() {
        // 1. 获取所有相关的 KeyStyle 定义 (逻辑分层)
        // 获取全局主题样式对象
        Style theme = ThemeManager.getStyle();
        // 获取编码区（composition）的主样式
        KeyStyle style = theme.getKeyStyle("composition");
        // 获取编码区按下状态（pressed）的样式，若未定义则回退到主样式
        KeyStyle mPressedStyle = style.getKeyStyle("pressed", style);

        // 获取候选词（candidate）的主样式
        KeyStyle mCandidateStyle = theme.getKeyStyle("candidate");
        // 获取候选词按下状态（pressed）的样式，若未定义则回退到候选词主样式
        KeyStyle mCandidatePressedStyle = mCandidateStyle.getKeyStyle("pressed", mCandidateStyle);

        // 获取候选词注释（comment）的样式，基于候选词主样式
        KeyStyle mCommentStyle = mCandidateStyle.getKeyStyle("comment", mCandidateStyle);
        // 获取候选词注释按下状态（pressed）的样式，若未定义则回退到注释主样式
        KeyStyle mCommentPressedStyle = mCommentStyle.getKeyStyle("pressed", mCommentStyle);

        // 获取提示键（hint key）的样式，基于编码区中的 key 样式，再获取其 hintKeyStyle
        KeyStyle hintKeyStyle = style.getKeyStyle("key", ThemeManager.getStyle().getKeyStyle()).getHintKeyStyle();
        // 获取普通按键（key）的样式，基于编码区中的 key 样式
        KeyStyle keyStyle = style.getKeyStyle("key", ThemeManager.getStyle().getKeyStyle());

        // 2. 基础逻辑参数设置
        // 获取窗口组件的样式配置
        components = style.getStyle("window");
        // 获取最大候选词条目数，若未配置则使用默认值 max_entries
        max_entries = style.getInt("max_entries", max_entries);
        // 获取云输入最大条目数，若未配置则使用默认值 cloud_max_entries
        cloud_max_entries = style.getInt("cloud_max_entries", cloud_max_entries);
        // 获取最小匹配长度，无默认值
        min_length = style.getInt("min_length");
        // 获取最大显示长度，若未配置则默认为 5
        max_length = style.getInt("max_length", 5);
        // 获取粘性行数（始终显示的固定行数），无默认值
        sticky_lines = style.getInt("sticky_lines");
        // 获取是否显示所有短语（包括长度不足的候选词）
        all_phrases = style.getBoolean("all_phrases");
        // 获取是否使用游标显示当前选中的候选词，默认为 true
        candidate_use_cursor = style.getBoolean("use_cursor", true);
        // 获取编码区是否可移动的配置，默认为 "false"
        movable = style.getString("movable", "false");

        // 3. 尺寸与布局设置 (Size, Margin, Padding, Spacing)
        // 设置最小宽度，若未配置则默认为 10 像素
        setMinWidth(style.getSize("min_width", 10));
        // 设置最小高度，若未配置则默认为 10 像素
        setMinHeight(style.getSize("min_height", 10));
        // 设置最大宽度：取配置值与屏幕最大宽度的较小值，防止超出屏幕
        // 注意：setMaxWidth 被调用了两次，此处保留逻辑，先取限制值再取比例值
        setMaxWidth(Math.min(style.getSize("max_width", 10000), TrimeService.getInstance().getMaxWidth()));
        // 注释掉的代码：按屏幕宽度比例设置最大宽度
        //setMaxWidth((int) (getResources().getDisplayMetrics().widthPixels * style.getFloat("width", 0.8f)));
        // 设置最大高度，若未配置则默认为 1000 像素
        setMaxHeight(style.getSize("max_height", 1000));

        // 获取行间距倍数，若未配置则默认为 1.0
        float line_spacing_multiplier = style.getFloat("line_spacing_multiplier", 1f);
        // 如果行间距倍数为 0，则强制设为 1.0，避免无效值
        if (line_spacing_multiplier == 0f) line_spacing_multiplier = 1f;
        // 设置行间距：第一个参数为附加间距，第二个参数为倍数
        setLineSpacing(style.getFloat("line_spacing", 1f), line_spacing_multiplier);

        // 获取内边距（padding）样式配置
        Style padding = style.getStyle("padding");
        // 设置视图的内边距：左、上、右、下，若未配置则默认为 0
        setPadding(padding.getSize("left", 0), padding.getSize("top", 0), padding.getSize("right", 0), padding.getSize("bottom", 0));

        // 4. 文字尺寸设置 (Text Size)
        // 将 dp 值转换为 px，并设置正文文字大小，默认 18dp
        text_size = ThemeManager.dp2px(style.getTextSize(18));
        // 设置候选词文字大小，默认 18dp
        candidate_text_size = ThemeManager.dp2px(mCandidateStyle.getTextSize(18));
        // 设置注释文字大小，默认 12dp
        comment_text_size = ThemeManager.dp2px(mCommentStyle.getTextSize(12));
        // 设置标签（如候选词序号）文字大小，默认 12dp
        label_text_size = ThemeManager.dp2px(hintKeyStyle.getTextSize(12));
        // 设置按键文字大小，默认 18dp
        key_text_size = ThemeManager.dp2px(keyStyle.getTextSize(18));

        // 5. 颜色属性设置 (Colors)
        // 普通颜色
        // 获取正文文本颜色
        text_color = style.getTextColor();
        // 获取候选词文本颜色
        candidate_text_color = mCandidateStyle.getTextColor();
        // 获取注释文本颜色
        comment_text_color = mCommentStyle.getTextColor();
        // 获取标签文本颜色
        label_color = hintKeyStyle.getTextColor();
        // 获取按键文本颜色
        key_text_color = keyStyle.getTextColor();

        // 选中/高亮颜色
        // 获取编码区按下状态的文本颜色（高亮文本颜色）
        hilited_text_color = mPressedStyle.getTextColor();
        // 获取候选词按下状态的文本颜色（高亮候选词文本颜色）
        hilited_candidate_text_color = mCandidatePressedStyle.getTextColor();
        // 获取注释按下状态的文本颜色（高亮注释文本颜色）
        hilited_comment_text_color = mCommentPressedStyle.getTextColor();
        // 获取高亮标签的颜色：通过按键样式的按下状态下的 hintKeyStyle 获取
        hilited_label_color = style.getKeyStyle("key", ThemeManager.getStyle().getKeyStyle()).getPressedStyle().getHintKeyStyle().getTextColor();

        // 背景颜色
        // 获取编码区背景颜色，若未配置则尝试从 Drawable 获取
        back_color = getColor(style, "background");
        // 获取按键背景颜色
        key_back_color = keyStyle.getBackgroundColor();
        // 获取编码区按下状态的背景颜色（高亮背景颜色）
        hilited_back_color = style.getPressedStyle().getBackgroundColor();
        // 获取候选词按下状态的背景颜色（高亮候选词背景颜色）
        hilited_candidate_back_color = getColor(mCandidatePressedStyle, "background");

        // 6. 字体设置 (Fonts)
        // 设置正文字体
        tfText = style.getFont();
        // 设置候选词字体
        tfCandidate = mCandidateStyle.getFont();
        // 设置注释字体
        tfComment = mCommentStyle.getFont();
        // 设置标签字体
        tfLabel = hintKeyStyle.getFont();

        // 7. 初始状态
        // 清空当前文本内容
        setText("");
    }

    /**
     * 从样式配置中获取颜色值。
     * 优先尝试直接获取颜色配置，如果未配置颜色，则尝试获取背景图片并取中心像素的颜色作为替代。
     *
     * @param style 样式配置对象。
     * @param s     配置键名（如 "background"）。
     * @return 颜色整数值（ARGB），如果无法获取则返回 0（透明黑）。
     */
    private int getColor(Style style, String s) {
        // 尝试直接从样式中获取颜色值
        Integer clr = style.getColor(s);
        // 如果颜色值为空（未配置颜色）
        if (clr == null) {
            // 尝试获取对应的 Drawable 对象（通常是背景图）
            Drawable cd = style.getDrawable(s);
            // 如果 Drawable 是 BitmapDrawable 类型
            if (cd instanceof BitmapDrawable) {
                // 获取位图对象
                Bitmap bmp = ((BitmapDrawable) cd).getBitmap();
                // 取位图中心点的像素颜色作为颜色值
                clr = bmp.getPixel(bmp.getWidth() / 2, bmp.getHeight() / 2);
            } else {
                // 如果不是 BitmapDrawable 或无法获取，默认返回 0（透明）
                clr = 0;
            }
        }
        // 返回最终确定的颜色值
        return clr;
    }

    /**
     * 根据样式配置获取文本对齐方式的 Span 对象。
     * 支持左对齐（normal/left）、右对齐（opposite/right）和居中对齐（center）。
     *
     * @param m 样式配置对象。
     * @return AlignmentSpan.Standard 对象，包含指定的对齐方式。
     */
    private Object getAlign(Style m) {
        // 默认对齐方式为正常对齐（左对齐，取决于文本方向）
        Layout.Alignment i = Layout.Alignment.ALIGN_NORMAL;
        // 检查样式中是否配置了 "align" 键
        if (m.hasKey("align")) {
            // 获取对齐方式的字符串配置
            String align = m.getString("align");
            // 根据配置字符串设置对应的对齐枚举值
            switch (align) {
                case "left":
                case "normal":
                    // 左对齐或正常对齐
                    i = Layout.Alignment.ALIGN_NORMAL;
                    break;
                case "right":
                case "opposite":
                    // 右对齐或反向对齐
                    i = Layout.Alignment.ALIGN_OPPOSITE;
                    break;
                case "center":
                    // 居中对齐
                    i = Layout.Alignment.ALIGN_CENTER;
                    break;
            }
        }
        // 返回封装了对齐方式的 Span 对象
        return new AlignmentSpan.Standard(i);
    }

    /**
     * 将预编辑文本（Composition）追加到 SpannableStringBuilder 中。
     * 处理文本的对齐、HTML 解析、字体大小、字间距以及选中部分的高亮显示。
     *
     * @param m 当前窗口组件的样式配置对象。
     */
    private void appendComposition(Style m) {
        // 如果配置为末尾内容置顶且当前缓冲区已有内容，先添加换行符
        if (end_top && ss.length() > 2)
            ss.append("\n");
        TrimeService trime = TrimeService.getInstance();
        if (trime != null && trime.shouldHideCompositionDuringPrediction()) return;
        
        // 获取 Rime 上下文中的预编辑信息
        RimeProto.Context.Composition r = mRimeContext.getComposition();
        // 获取预编辑文本字符串
        String s = r.getPreedit();
        int selectionStart = r.getSelStart();
        int selectionEnd = r.getSelEnd();
        String input = mRimeContext != null ? mRimeContext.getInput() : null;
        if (TrimeService.isPredictionPlaceholderOnly(s, input)) return;
        String visibleText = TrimeService.resolveVisibleCompositionText(s, input);
        if (TrimeService.shouldPreferRawInputForComposition(input) || TextUtils.isEmpty(s)) {
            if (!TextUtils.isEmpty(input)) {
                int caret = Math.max(0, Math.min(mRimeContext.getCaretPos(), input.length()));
                s = withSoftCursor(input, caret);
                selectionStart = caret;
                selectionEnd = caret;
            }
        } else {
            s = visibleText;
        }
        if (TextUtils.isEmpty(s)) return;
        
        // 定义起始和结束位置变量
        int start, end;
        // 获取配置的前缀分隔符
        String sep = m.getString("start");
        // 如果前缀分隔符不为空
        if (!TextUtils.isEmpty(sep)) {
            // 记录当前长度作为起始位置
            start = ss.length();
            // 追加前缀分隔符
            ss.append(sep);
            // 记录追加后的长度作为结束位置
            end = ss.length();
            // 为前缀设置对齐方式 Span
            ss.setSpan(getAlign(m), start, end, span);
        }
        
        // 记录预编辑文本的起始位置
        start = ss.length();
        // 检查预编辑文本是否为 HTML 格式
        if (s.startsWith("<html>")) {
            // 如果是 HTML，解析并追加富文本内容
            ss.append(Html.fromHtml(s));
            // 更新结束位置
            end = ss.length();
        } else {
            // 如果不是 HTML，直接追加纯文本
            ss.append(s);
            // 更新结束位置
            end = ss.length();
            // 为纯文本设置对齐方式 Span
            ss.setSpan(getAlign(m), start, end, span);
        }
        
        // 记录预编辑文本在缓冲区中的起始和结束索引，用于后续光标定位
        composition_pos[0] = start;
        composition_pos[1] = end;
        int compositionLength = composition_pos[1] - composition_pos[0];
        selectionStart = Math.max(0, Math.min(selectionStart, compositionLength));
        selectionEnd = Math.max(selectionStart, Math.min(selectionEnd, compositionLength));

        // 设置预编辑文本的基础样式（如下划线、颜色等，由 CompositionSpan 定义）
        ss.setSpan(new CompositionSpan(), start, end, span);
        // 设置预编辑文本的字体大小
        ss.setSpan(new AbsoluteSizeSpan(text_size), start, end, span);
        
        // 检查是否配置了字符间距
        if (m.hasKey("letter_spacing")) {
            // 获取字符间距值，默认为 0
            float size = m.getFloat("letter_spacing", 0);
            // 如果间距不为 0
            if (size != 0f)
                // 应用字符间距 Span
                ss.setSpan(new LetterSpacingSpan(size), start, end, span);
        }
        
        // 计算选中部分的起始位置：预编辑文本起始位置 + Rime 选区起始偏移
        start = composition_pos[0] + selectionStart;
        // 计算选中部分的结束位置：预编辑文本起始位置 + Rime 选区结束偏移
        end = composition_pos[0] + selectionEnd;
        
        // 为选中部分设置高亮前景色
        ss.setSpan(new ForegroundColorSpan(hilited_text_color), start, end, span);
        // 为选中部分设置高亮背景色
        ss.setSpan(new BackgroundColorSpan(hilited_back_color), start, end, span);
        
        // 获取配置的后缀分隔符
        sep = m.getString("end");
        // 如果后缀分隔符不为空，则追加到缓冲区
        if (!TextUtils.isEmpty(sep)) ss.append(sep);
    }

    private boolean isPredictionPlaceholderComposition() {
        if (mRimeContext == null || mRimeContext.getComposition() == null) return false;
        return TrimeService.isPredictionPlaceholderOnly(
                mRimeContext.getComposition().getPreedit(),
                mRimeContext.getInput());
    }

    /**
     * 将候选词列表追加到 SpannableStringBuilder 中。
     * 根据配置格式化候选词、标签和注释，并应用相应的样式（如颜色、字体、对齐方式）。
     * 支持正向显示（从上到下）和反向显示（从下到上，即 end_top 模式）。
     *
     * @param m      当前窗口组件的样式配置对象。
     * @param length 最小候选词长度限制，小于此长度的候选词可能被过滤。
     * @return 起始候选词的索引编号，用于分页或状态追踪。
     */
    private int appendCandidates(Style m, int length) {
        // 定义 Span 的起始和结束位置变量
        int start, end;
        // 初始化起始候选词索引为 0
        int start_num = 0;
        // 从 Rime 上下文菜单中获取候选词数组
        RimeProto.Candidate[] candidates = mRimeContext.getMenu().getCandidates();
        // 如果候选词数组为空或长度为 0，直接返回起始索引 0
        if (candidates == null || candidates.length == 0) return start_num;
        // 获取配置的前缀分隔符（例如候选词列表前的符号或空格）
        String sep = m.getString("start");
        // 根据配置决定是否使用游标高亮：如果使用，则获取高亮候选词的索引；否则设为 -1
        highlightIndex = candidate_use_cursor ? mRimeContext.getMenu().getHighlightedCandidateIndex() : -1;
        // 获取标签格式化字符串（用于格式化候选词序号或标签）
        String label_format = m.getString("label");
        // 获取候选词格式化字符串（用于格式化候选词文本本身）
        String candidate_format = m.getString("candidate");
        // 获取注释格式化字符串（用于格式化候选词的注释信息）
        String comment_format = m.getString("comment");
        // 获取候选词之间的行内分隔符（例如空格或特定符号）
        String line = m.getString("sep");
        // 将行内分隔符赋值给全局变量 cloudSep，供其他方法使用
        cloudSep = line;
        // 如果最小长度限制小于 1，则不显示任何候选词，直接返回 0
        if (length < 1)
            return 0;
        // 定义最后一个候选词的长度变量（当前代码中未实际使用）
        int last_cand_length = 0;
        // 初始化当前行的累计长度计数器
        int line_length = 0;
        // 从 Rime 上下文菜单中获取选择标签数组（如 1, 2, 3... 或 a, b, c...）
        String[] labels = mRimeContext.getMenu().getSelectLabels();
        
        // 判断是否为正常顺序显示（非末尾置顶模式）
        if (!end_top) {
            // 初始化候选词索引计数器 i
            int i = -1;
            // 重置当前显示的候选词数量为 0
            candidate_num = 0;
            // 初始化遍历计数器 n
            int n = -1;
            // 遍历所有候选词
            for (RimeProto.Candidate o : candidates) {
                // 递增遍历计数器
                n++;
                // 获取当前候选词的文本内容
                String cand = o.getText();
                // 如果文本为空，则设为空字符串，避免后续处理出错
                if (TextUtils.isEmpty(cand)) cand = "";
                // 递增候选词索引计数器
                i++;
                
                // 检查是否超过最大显示条目数限制（如果 max_entries > -1 表示有限制）
                if (candidate_num >= max_entries && max_entries > -1) {
                    // 如果这是第一个被跳过的候选词，记录其索引作为起始编号
                    if (start_num == 0 && candidate_num == i)
                        start_num = candidate_num;
                    // 超出限制，跳出循环
                    break;
                }
                
                // 检查候选词长度是否小于最小长度限制
                if (cand.length() < length) {
                    // 如果这是第一个被跳过的候选词，记录其索引作为起始编号
                    if (start_num == 0 && candidate_num == i)
                        start_num = candidate_num;
                    // 如果配置为显示所有短语（包括短词），则跳过当前词继续下一个
                    if (all_phrases)
                        continue;
                    else
                        // 否则，遇到短词即停止显示后续候选词，跳出循环
                        break;
                }
                
                // 根据配置的格式字符串格式化候选词文本
                cand = String.format(candidate_format, cand);
                // 定义行分隔符变量
                String line_sep;
                
                // 确定行分隔符：
                if (candidate_num == 0) {
                    // 如果是第一个候选词，使用前缀分隔符
                    line_sep = sep;
                } 
                // 注释掉的代码：每 5 个候选词换行（已禁用）
                /*else if (n % 5 == 0) {
                    line_sep = "\n";
                    line_length = 0;
                }*/ 
                else if ((sticky_lines > 0 && sticky_lines >= i)
                        || (max_length > 0 && line_length + cand.length() > max_length)) {
                    // 如果当前行是粘性行（固定显示的行）或者加上当前词后超过最大行长度，则换行
                    line_sep = "\n";
                    // 重置行长度计数器
                    line_length = 0;
                } else {
                    // 否则，使用配置的行内分隔符（如空格）
                    line_sep = line;
                }
                
                // 如果行分隔符不为空，则添加分隔符并设置对齐样式
                if (!TextUtils.isEmpty(line_sep)) {
                    start = ss.length();
                    ss.append(line_sep);
                    end = ss.length();
                    ss.setSpan(getAlign(m), start, end, span);
                }
                
                // 处理候选词标签（如序号）
                if (!TextUtils.isEmpty(label_format) && labels != null && labels.length > i) {
                    // 根据格式字符串和标签数组生成标签文本
                    String label = String.format(label_format, labels[i]);
                    start = ss.length();
                    ss.append(label);
                    end = ss.length();
                    // 为标签设置点击事件、颜色、字体等样式（使用 CandidateSpan）
                    ss.setSpan(
                            new CandidateSpan(
                                    i,
                                    tfLabel,
                                    hilited_label_color,
                                    hilited_candidate_back_color,
                                    label_color),
                            start,
                            end,
                            span);
                    // 设置标签的字体大小
                    ss.setSpan(new AbsoluteSizeSpan(label_text_size), start, end, span);
                }
                
                // 处理候选词主体文本
                start = ss.length();
                ss.append(cand);
                end = ss.length();
                // 累加当前行的长度
                line_length += cand.length();
                // 设置候选词的对齐方式
                ss.setSpan(getAlign(m), start, end, span);
                // 为候选词设置点击事件、颜色、字体等样式
                ss.setSpan(
                        new CandidateSpan(
                                i,
                                tfCandidate,
                                hilited_candidate_text_color,
                                hilited_candidate_back_color,
                                candidate_text_color),
                        start,
                        end,
                        span);
                // 设置候选词的字体大小
                ss.setSpan(new AbsoluteSizeSpan(candidate_text_size), start, end, span);
                
                // 处理候选词注释
                String comment = o.getComment();
                // 如果未隐藏注释、配置了注释格式且注释不为空，则处理注释
                if (!Config.is_hide_comment() && !TextUtils.isEmpty(comment_format) && !TextUtils.isEmpty(comment)) {
                    // 根据格式字符串格式化注释文本
                    comment = String.format(comment_format, comment);
                    start = ss.length();
                    // 检查注释是否包含 HTML 标签
                    if (comment.contains("<html>")) {
                        // 如果是 HTML，解析富文本并支持图片加载（使用 LuaBitmapDrawable）
                        ss.append(Html.fromHtml(comment, new Html.ImageGetter() {
                            @Override
                            public Drawable getDrawable(String source) {
                                return new LuaBitmapDrawable(TrimeService.getInstance(), source);
                            }
                        }, null));
                        end = ss.length();
                    } else {
                        // 如果是纯文本，直接追加
                        ss.append(comment);
                        end = ss.length();
                        // 设置注释的对齐方式
                        ss.setSpan(getAlign(m), start, end, span);
                        // 为注释设置点击事件、颜色、字体等样式
                        ss.setSpan(
                                new CandidateSpan(
                                        i,
                                        tfComment,
                                        hilited_comment_text_color,
                                        hilited_candidate_back_color,
                                        comment_text_color),
                                start,
                                end,
                                span);
                        // 设置注释的字体大小
                        ss.setSpan(new AbsoluteSizeSpan(comment_text_size), start, end, span);
                    }
                    // 注释掉的代码：累加注释长度到行长度（已禁用，通常注释不计入换行计算）
                    //line_length += comment.length();
                }
                
                // 递增已显示的候选词数量
                candidate_num++;
            }

            // 如果所有候选词都显示完毕且没有因长度限制提前截断，更新起始编号为总数量
            if (start_num == 0 && candidate_num == i + 1) start_num = candidate_num;
            
        } else {
            // 如果是末尾置顶模式（反向显示），则从后往前遍历候选词
            // 重置候选词数量为 0
            candidate_num = 0;
            // 初始化遍历计数器
            int n = -1;
            // 初始化索引计数器
            int i = -1;
            // 确定最大显示条目数
            int max = max_entries;
            // 如果无限制（-1），则设为候选词总数
            if (max == -1)
                max = candidates.length;
            // 如果计算出的最大值超过实际候选词数量，则修正为实际数量
            if (max > candidates.length)
                max = candidates.length;
            
            // 从最后一个候选词开始向前遍历
            for (i = max - 1; i >= 0; i--) {
                // 获取当前索引的候选词对象
                RimeProto.Candidate o = candidates[i];
                // 递增遍历计数器
                n++;
                // 获取候选词文本
                String cand = o.getText();
                // 如果文本为空，设为空字符串
                if (TextUtils.isEmpty(cand)) cand = "";
                
                // 注释掉的代码：在反向模式下也进行长度和数量检查（当前已禁用，全部显示）
                /*if (candidate_num >= max_entries && max_entries > -1) {
                    if (start_num == 0 && candidate_num == i)
                        start_num = candidate_num;
                    break;
                }
                if (cand.length() < length) {
                    if (start_num == 0 && candidate_num == i)
                        start_num = candidate_num;
                    continue;
                }*/
                
                // 格式化候选词文本
                cand = String.format(candidate_format, cand);
                // 定义行分隔符
                String line_sep;
                
                // 确定行分隔符：
                if (candidate_num == 0) {
                    // 第一个处理的候选词（实际上是列表中的最后一个）使用前缀分隔符
                    line_sep = sep;
                } else {
                    // 后续每个候选词都强制换行，实现反向堆叠效果
                    line_sep = "\n";
                    // 重置行长度
                    line_length = 0;
                }
                
                // 如果行分隔符不为空，添加并设置对齐样式
                if (!TextUtils.isEmpty(line_sep)) {
                    start = ss.length();
                    ss.append(line_sep);
                    end = ss.length();
                    ss.setSpan(getAlign(m), start, end, span);
                }
                
                // 处理标签
                if (!TextUtils.isEmpty(label_format) && labels != null) {
                    String label = String.format(label_format, labels[i]);
                    start = ss.length();
                    ss.append(label);
                    end = ss.length();
                    // 设置标签样式
                    ss.setSpan(
                            new CandidateSpan(
                                    i,
                                    tfLabel,
                                    hilited_label_color,
                                    hilited_candidate_back_color,
                                    label_color),
                            start,
                            end,
                            span);
                    ss.setSpan(new AbsoluteSizeSpan(label_text_size), start, end, span);
                }
                
                // 处理候选词主体
                start = ss.length();
                ss.append(cand);
                end = ss.length();
                line_length += cand.length();
                ss.setSpan(getAlign(m), start, end, span);
                // 设置候选词样式
                ss.setSpan(
                        new CandidateSpan(
                                i,
                                tfCandidate,
                                hilited_candidate_text_color,
                                hilited_candidate_back_color,
                                candidate_text_color),
                        start,
                        end,
                        span);
                ss.setSpan(new AbsoluteSizeSpan(candidate_text_size), start, end, span);
                
                // 处理注释
                String comment = o.getComment();
                if (!Config.is_hide_comment() && !TextUtils.isEmpty(comment_format) && !TextUtils.isEmpty(comment)) {
                    comment = String.format(comment_format, comment);
                    start = ss.length();
                    if (comment.contains("<html>")) {
                        // 解析 HTML 注释
                        ss.append(Html.fromHtml(comment, new Html.ImageGetter() {
                            @Override
                            public Drawable getDrawable(String source) {
                                return new LuaBitmapDrawable(TrimeService.getInstance(), source);
                            }
                        }, null));
                        end = ss.length();
                    } else {
                        ss.append(comment);
                        end = ss.length();
                        ss.setSpan(getAlign(m), start, end, span);
                        // 设置注释样式
                        ss.setSpan(
                                new CandidateSpan(
                                        i,
                                        tfComment,
                                        hilited_comment_text_color,
                                        hilited_candidate_back_color,
                                        comment_text_color),
                                start,
                                end,
                                span);
                        ss.setSpan(new AbsoluteSizeSpan(comment_text_size), start, end, span);
                    }
                    //line_length += comment.length();
                }
                
                // 递增显示数量
                candidate_num++;
            }

            // 在反向模式下，起始编号设为最大显示条目数
            start_num = max;
        }

        // 获取配置的后缀分隔符
        sep = m.getString("end");
        // 如果后缀分隔符不为空，则追加到文本末尾
        if (!TextUtils.isEmpty(sep)) ss.append(sep);
        
        // 返回起始候选词索引编号
        return start_num;
    }

    /**
     * 添加功能按钮到编码区显示。
     * 根据样式配置决定是否显示按钮，并设置按钮的标签、点击事件及样式。
     *
     * @param m 当前窗口组件的样式配置对象。
     */
    private void appendButton(Style m) {
        // 检查是否配置了显示条件 "when"
        if (m.hasKey("when")) {
            // 获取显示条件字符串
            String when = m.getString("when");
            // 如果条件是 "paging"（分页时）且当前不在分页状态，则不显示
            if (when.contentEquals("paging") && !Rime.isPaging()) return;
            // 如果条件是 "has_menu"（有候选菜单时）且当前没有候选菜单，则不显示
            if (when.contentEquals("has_menu") && !Rime.hasMenu()) return;
        }
        // 定义按钮标签变量
        String label;
        // 创建点击事件对象，从配置中获取 "click" 动作
        Event e = new Event(m.getString("click"));
        // 如果配置了自定义标签 "label"，则使用自定义标签
        if (m.hasKey("label")) label = m.getString("label");
        // 否则，使用事件对象默认的标签
        else label = e.getLabel();
        // 定义 Span 的起始和结束位置变量
        int start, end;
        // 定义前缀分隔符变量
        String sep = null;
        // 如果配置了前缀 "start"，则获取前缀字符串
        if (m.hasKey("start")) sep = m.getString("start");
        // 如果前缀不为空
        if (!TextUtils.isEmpty(sep)) {
            // 记录当前文本长度作为起始位置
            start = ss.length();
            // 追加前缀字符串
            ss.append(sep);
            // 记录追加后的长度作为结束位置
            end = ss.length();
            // 为前缀设置对齐方式 Span
            ss.setSpan(getAlign(m), start, end, span);
        }
        // 记录按钮标签的起始位置
        start = ss.length();
        // 追加按钮标签文本
        ss.append(label);
        // 记录按钮标签的结束位置
        end = ss.length();
        // 为按钮标签设置对齐方式 Span
        ss.setSpan(getAlign(m), start, end, span);
        // 为按钮标签设置点击事件 Span (EventSpan)
        ss.setSpan(new EventSpan(e), start, end, span);
        // 为按钮标签设置字体大小 Span
        ss.setSpan(new AbsoluteSizeSpan(key_text_size), start, end, span);
        // 获取配置的后缀 "end"
        sep = m.getString("end");
        // 如果后缀不为空，则追加后缀字符串
        if (!TextUtils.isEmpty(sep)) ss.append(sep);
    }

    /**
     * 添加可移动区域标记到编码区显示。
     * 该区域用于支持触摸拖动以移动编码区窗口位置。
     *
     * @param m 当前窗口组件的样式配置对象。
     */
    private void appendMove(Style m) {
        // 获取移动区域的标识字符串（通常是一个空格或特定符号）
        String s = m.getString("move");
        // 定义 Span 的起始和结束位置变量
        int start, end;
        // 获取配置的前缀 "start"
        String sep = m.getString("start");
        // 如果前缀不为空
        if (!TextUtils.isEmpty(sep)) {
            // 记录当前文本长度作为起始位置
            start = ss.length();
            // 追加前缀字符串
            ss.append(sep);
            // 记录追加后的长度作为结束位置
            end = ss.length();
            // 为前缀设置对齐方式 Span
            ss.setSpan(getAlign(m), start, end, span);
        }
        // 记录移动区域标识的起始位置
        start = ss.length();
        // 追加移动区域标识字符串
        ss.append(s);
        // 记录移动区域标识的结束位置
        end = ss.length();
        // 为移动区域标识设置对齐方式 Span
        ss.setSpan(getAlign(m), start, end, span);
        // 记录移动区域在文本中的起始索引，用于触摸事件判断
        move_pos[0] = start;
        // 记录移动区域在文本中的结束索引，用于触摸事件判断
        move_pos[1] = end;
        // 为移动区域标识设置字体大小 Span
        ss.setSpan(new AbsoluteSizeSpan(key_text_size), start, end, span);
        // 为移动区域标识设置前景色 Span
        ss.setSpan(new ForegroundColorSpan(key_text_color), start, end, span);
        // 获取配置的后缀 "end"
        sep = m.getString("end");
        // 如果后缀不为空，则追加后缀字符串
        if (!TextUtils.isEmpty(sep)) ss.append(sep);
    }

    /**
     * 构建并显示编码区窗口内容。
     * 根据主题配置组装预编辑文本、候选词、功能按钮和移动区域，并应用相应的样式。
     *
     * @param length 最小候选词长度限制，小于此长度的候选词可能被过滤。
     * @return 起始候选词的索引编号，用于分页或状态追踪。
     */
    public int setWindow(int length) {
        // 如果视图不可见，则直接返回 0，不进行任何操作
        if (getVisibility() != View.VISIBLE) return 0;
        TrimeService trime = TrimeService.getInstance();
        if (trime != null && trime.shouldHideCompositionDuringPrediction()) return 0;
        // 获取当前的 Rime 输入法上下文数据
        mRimeContext = Rime.getRimeContext();
        // 从上下文中获取预编辑信息对象
        RimeProto.Context.Composition r = mRimeContext.getComposition();
        // 如果预编辑信息为空，则返回 0
        if (r == null) return 0;
        // 获取预编辑文本字符串
        String s = r.getPreedit();
        String input = mRimeContext.getInput();
        if (TrimeService.isPredictionPlaceholderOnly(s, input)) return 0;
        if (TrimeService.shouldPreferRawInputForComposition(input) || TextUtils.isEmpty(s)) {
            s = input;
        } else {
            s = TrimeService.resolveVisibleCompositionText(s, input);
        }
        // 如果既没有预编辑文本，也不是预测占位符态，则无需显示编码区
        if (TextUtils.isEmpty(s) && !isPredictionPlaceholderComposition()) return 0;
        // 暂时设置为单行显示，后续根据内容长度可能调整
        setSingleLine(true); // 设置单行
        // 初始化 SpannableStringBuilder 用于构建富文本
        ss = new SpannableStringBuilder();
        // 初始化起始候选词索引为 0
        int start_num = 0;
        // 如果不是末尾置顶模式（正常顺序显示）
        if (!end_top) {
            // 获取窗口组件配置的总数
            int len = components.getLength();
            // 从头到尾遍历所有组件配置
            for (int i = 0; i < len; i++) {
                // 获取第 i 个组件的样式配置
                Style m = components.getStyle(i);
                // 如果配置包含 "composition"，则添加预编辑文本
                if (m.hasKey("composition")) appendComposition(m);
                // 如果配置包含 "candidate"，则添加候选词列表，并更新起始索引
                else if (m.hasKey("candidate")) start_num = appendCandidates(m, length);
                // 如果配置包含 "click"，则添加功能按钮
                else if (m.hasKey("click")) appendButton(m);
                // 如果配置包含 "move"，则添加移动区域标记
                else if (m.hasKey("move")) appendMove(m);
            }
        } else {
            // 如果是末尾置顶模式（反向顺序显示）
            // 获取窗口组件配置的总数
            int len = components.getLength();
            // 从尾到头遍历所有组件配置
            for (int i = len - 1; i >= 0; i--) {
                // 获取第 i 个组件的样式配置
                Style m = components.getStyle(i);
                // 如果配置包含 "composition"，则添加预编辑文本
                if (m.hasKey("composition")) appendComposition(m);
                // 如果配置包含 "candidate"，则添加候选词列表，并更新起始索引
                else if (m.hasKey("candidate")) start_num = appendCandidates(m, length);
                // 如果配置包含 "click"，则添加功能按钮
                else if (m.hasKey("click")) appendButton(m);
                // 如果配置包含 "move"，则添加移动区域标记
                else if (m.hasKey("move")) appendMove(m);
            }
        }

        // 将构建好的富文本设置到 TextView 中
        setText(ss);
        // 如果配置为强制单行模式
        if (mSingle) {
            // 确保单行显示
            setSingleLine();
            // 滚动到顶部
            scrollTo(0, 0);
            // 设置最大宽度为屏幕宽度的 10 倍，防止换行
            setMaxWidth(TrimeService.getInstance().getWidth() * 10);
        } else {
            // 如果不是强制单行模式，根据内容长度动态判断是否换行
            // 如果文本长度超过 8 个字符
            if (ss.length() > 8) {
                // 测量视图宽度
                measure(0, 0);
                // 如果测量宽度大于等于最大允许宽度，则设置为多行显示
                if (getMeasuredWidth() >= getMaxWidth())
                    setSingleLine(false);
                // 注释掉的代码：重新测量或设置最小宽高
                //measure(0, 0);
                //setMinWidth(getMeasuredWidth());
                //setMinHeight(getMeasuredHeight());
            }
            if(BuildConfig.DEBUG){
                Log.i(TAG, "setWindow:s "+ss);
                Log.i(TAG, "setWindow:w "+getWidth());
                Log.i(TAG, "setWindow:m "+getMaxWidth());
                Log.i(TAG, "setWindow:w2 "+getMeasuredWidth());
            }
            /*if(BuildConfig.DEBUG){
                Log.i(TAG, "setWindow:s "+ss);
                Log.i(TAG, "setWindow:w "+getWidth());
                Log.i(TAG, "setWindow:m "+getMaxWidth());
                Log.i(TAG, "setWindow:w2 "+getMeasuredWidth());
            }*/
            // 如果有候选词分页（start_num > 0），则设置为多行显示以展示更多候选词
            if (start_num > 0)
                setSingleLine(false);
        }
        // 启用链接点击处理，使得 ClickableSpan 能够响应点击事件
        setMovementMethod(LinkMovementMethod.getInstance());
        // 返回起始候选词索引编号
        return start_num;
    }


}
