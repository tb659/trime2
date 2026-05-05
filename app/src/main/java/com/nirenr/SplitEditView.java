package com.nirenr;

import android.app.AlertDialog;
import android.app.Service;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Build;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.ArrayListAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.GridView;
import android.widget.LinearLayout;

import com.androlua.LuaEditor;

import java.util.ArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 分割编辑视图类。
 * 支持按全文、段落、行、句、字等不同模式分割和编辑文本。
 */

public class SplitEditView extends LinearLayout implements View.OnClickListener, AdapterView.OnItemClickListener {
    /** 全文模式 */
    private static final int ALL = 0;
    /** 段落模式 */
    private static final int CHUNK = 1;
    /** 行模式 */
    private static final int LINE = 2;
    /** 句模式 */
    private static final int ROW = 3;
    /** 字模式 */
    private static final int CHAR = 4;
    /** 保存按钮索引 */
    private static final int SAVE = 5;
    /** Android 上下文 */
    private final Context mContext;
    /** 根布局 */
    private LinearLayout mRoot;
    /** 列表视图(GridView) */
    private GridView mListView;
    /** 编辑器视图 */
    private LuaEditor mEditView;
    /** 当前分割模式 */
    private int mSplitMode=ALL;
    /** 原始文本 */
    private String mText = "";
    /** 分割后的文本数组 */
    private String[] mList=new String[]{""};
    /** 保存监听器 */
    private OnSaveListener mOnSaveListener;
    /** 按钮栏 */
    private LinearLayout mButtonBar;

    /**
     * 构造函数。
     *
     * @param context Android 上下文。
     */
    public SplitEditView(Context context) {
        super(context);
        initView(context); // 初始化视图
        mContext = context;
    }

    /**
     * 初始化视图结构。
     * 创建 GridView、编辑器和按钮栏。
     *
     * @param context Android 上下文。
     */
    private void initView(Context context) {
        mRoot = this;
        mRoot.setOrientation(LinearLayout.VERTICAL);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT, 1);
        LinearLayout.LayoutParams lp2 = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT, 1);
        LinearLayout.LayoutParams lp3 = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        mListView = new GridView(context);
        mEditView = new LuaEditor(context);
        mListView.setOnItemClickListener(this);
        mRoot.addView(mListView, lp);
        mRoot.addView(mEditView, lp);
        mButtonBar = new LinearLayout(context);
        String[] bts = new String[]{"全文", "按段", "按行", "按句", "按字", "确定"}; // 按钮文本

        for (int i = 0; i < bts.length; i++) {
            String b = bts[i];
            Button btn = new Button(context);
            btn.setText(b); // 设置按钮文本
            btn.setId(i); // 设置 ID 作为模式标识
            btn.setOnClickListener(this); // 设置点击监听
            mButtonBar.addView(btn, lp2);
        }

        mRoot.addView(mButtonBar, lp3);
        mListView.setVisibility(View.GONE);
        mEditView.setVisibility(View.VISIBLE);
        setText("");
        setOnSaveListener(null);
    }

    /**
     * 设置文本内容。
     *
     * @param text 要设置的文本。
     */
    public void setText(String text) {
        mText = text;
        if (mText == null)
            mText = ""; // null 转为空字符串
        mEditView.setText(mText); // 设置编辑器文本
        initText(); // 初始化分割
    }

    /**
     * 获取当前文本内容。
     * 如果在编辑模式则返回编辑器内容,否则拼接分割后的数组。
     *
     * @return 当前文本内容。
     */
    public String getText() {
        if(isShowEdit())
            return mEditView.getText().toString();
        StringBuilder buf = new StringBuilder();
        for (String s : mList){
             buf.append(s);
            if(mSplitMode==CHUNK)
                buf.append("\n\n");
        }
        if(mSplitMode==CHUNK)
            buf.delete(buf.length()-2,buf.length());
        return buf.toString();
    }

    /**
     * 初始化文本分割。
     * 根据当前分割模式对文本进行分割并显示。
     */
    private void initText() {
        mList = new String[]{mText};
        switch (mSplitMode) {
            case CHUNK:
                splitChunk();
                break;
            case LINE:
                splitLine();
                break;
            case ROW:
                splitRow();
                break;
            case CHAR:
                splitChar();
                break;
            default:
                setShowEdit(true);
        }
    }

    /**
     * 设置是否显示编辑模式。
     *
     * @param show true 显示编辑器,false 显示分割列表。
     */
    private void setShowEdit(boolean show) {
        if (isShowEdit() == show)
            return;
        if (!show) {
            mListView.setVisibility(View.VISIBLE);
            mEditView.setVisibility(View.GONE);
            mText = mEditView.getText().toString();
            mList=new String[]{mText};
        } else {
            mEditView.setText(getText());
            mListView.setVisibility(View.GONE);
            mEditView.setVisibility(View.VISIBLE);
        }
    }

    /**
     * 检查是否显示编辑模式。
     *
     * @return true 表示编辑器可见。
     */
    public boolean isShowEdit() {
        return mEditView.getVisibility() == View.VISIBLE;
    }

    /**
     * 点击事件处理。
     * 根据按钮 ID 切换分割模式或执行保存操作。
     *
     * @param v 被点击的视图。
     */
    @Override
    public void onClick(View v) {
        Button b = (Button) v;
        switch (v.getId()) {
            case ALL:
                setShowEdit(true);
                mSplitMode = ALL;
                break;
            case CHUNK:
                setShowEdit(false);
                splitChunk();
                break;
            case LINE:
                setShowEdit(false);
                splitLine();
                break;
            case ROW:
                setShowEdit(false);
                splitRow();
                break;
            case CHAR:
                setShowEdit(false);
                splitChar();
                break;
            case SAVE:
                if(mOnSaveListener!=null)
                    mOnSaveListener.onSave(getText());
                break;
        }
    }

    /**
     * 通用分割方法。
     * 使用正则表达式分割文本,支持不同的分割模式。
     *
     * @param text 要分割的文本。
     * @param reg 正则表达式。
     * @return 分割后的字符串数组。
     */
    private String[] split(String text, String reg) {
        ArrayList<String> list = new ArrayList<>();
        Pattern pattern = Pattern.compile(reg);
        Matcher matcher = pattern.matcher(text);
        int start = 0;
        while (matcher.find()) {
            int end = matcher.end();
            if(mSplitMode==CHUNK)
                end=matcher.start(); // 段落模式不包含分隔符
            list.add(text.substring(start, end));
            start = end;
            if(mSplitMode==CHUNK)
                start = matcher.end(); // 跳过段落分隔符
        }
        if (start != text.length())
            list.add(text.substring(start));
        String[] ret = new String[list.size()];
        list.toArray(ret);
        return ret;
    }

    /**
     * 按字分割。
     * 将文本拆分为单个字符,每行显示8个。
     */
    private void splitChar() {
        //mDlg.setTitle(mService.getString(R.string.split_edit_title) + " 按字");
        mText=getText();
        mSplitMode = CHAR;
        mList = new String[mText.length()];
        for (int i = 0; i < mText.length(); i++)
            mList[i] = String.valueOf(mText.charAt(i));
        if (mList.length == 0)
            mList = new String[]{""};
        mListView.setNumColumns(8);
        mListView.setAdapter(new ArrayListAdapter<>(mContext, android.R.layout.simple_list_item_1, mList));
    }

    /**
     * 按行分割。
     * 以换行符为分隔符拆分文本。
     */
    private void splitLine() {
        //mDlg.setTitle(mService.getString(R.string.split_edit_title) + " 按段");
        mText=getText();
        mSplitMode = LINE;
        mList = split(mText, "\n");
        if (mList.length == 0)
            mList = new String[]{""};
        mListView.setNumColumns(1);
        mListView.setAdapter(new ArrayListAdapter<>(mContext, android.R.layout.simple_list_item_1, mList));
    }

    /**
     * 按句分割。
     * 以标点符号(。？！，等)为分隔符拆分文本。
     */
    private void splitRow() {
        //mDlg.setTitle(mService.getString(R.string.split_edit_title) + " 按句");
        mText=getText();
        mSplitMode = ROW;
        //mList = mText.split("[。？！，\n “”]+");
        mList = split(mText, "\\. |[。？！，\n “”,：；;\\?!]+");
        if (mList.length == 0)
            mList = new String[]{""};
        mListView.setNumColumns(1);
        mListView.setAdapter(new ArrayListAdapter<>(mContext, android.R.layout.simple_list_item_1, mList));
    }

    /**
     * 按段落分割。
     * 以两个或更多换行符为分隔符拆分文本。
     */
    private void splitChunk() {
        //mDlg.setTitle(mService.getString(R.string.split_edit_title) + " 按句");
        mText=getText();
        mSplitMode = CHUNK;
        //mList = mText.split("[。？！，\n “”]+");
        mList = split(mText, "\\n{2,10}");
        if (mList.length == 0)
            mList = new String[]{""};
        mListView.setNumColumns(1);
        mListView.setAdapter(new ArrayListAdapter<>(mContext, android.R.layout.simple_list_item_1, mList));
    }

    /**
     * 更新分割显示。
     * 根据当前模式重新分割并刷新列表。
     */
    private void updateSplit() {
        switch (mSplitMode) {
            case CHUNK:
                splitChunk();
                break;
            case LINE:
                splitLine();
                break;
            case ROW:
                splitRow();
                break;
            case CHAR:
                splitChar();
                break;
        }
    }

    /**
     * 列表项点击事件。
     * 弹出编辑对话框修改选中项的内容。
     *
     * @param parent 父视图。
     * @param view 被点击的视图。
     * @param position 位置索引。
     * @param id 项 ID。
     */
    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        new EditDialog(position).show(); // 显示编辑对话框
    }

    /**
     * 设置保存监听器。
     *
     * @param listener 保存回调监听器。
     */
    public void setOnSaveListener(OnSaveListener listener){
        mOnSaveListener=listener;
        if(listener==null)
            mButtonBar.getChildAt(SAVE).setVisibility(GONE);
        else
            mButtonBar.getChildAt(SAVE).setVisibility(VISIBLE);
    }
    /**
     * 保存监听器接口。
     */
    public static interface OnSaveListener{
        /**
         * 保存回调方法。
         *
         * @param text 要保存的文本内容。
         */
        public void onSave(String text);
    }

    /**
     * 编辑对话框内部类。
     * 用于编辑分割后的单个文本项。
     */
    private class EditDialog implements DialogInterface.OnClickListener {
        private final int mIdx;
        private final EditText mEdit;
        private AlertDialog dlg;

        public EditDialog(int idx) {
            mIdx = idx;
            mEdit = new EditText(mContext);
            mEdit.setText(mList[idx]);
            mEdit.setSelection(mList[idx].length());
        }

        public void show() {
            dlg = new AlertDialog.Builder(mContext)
                    .setTitle("输入内容")
                    .setView(mEdit)
                    .setNegativeButton(android.R.string.cancel, null)
                    .setPositiveButton(android.R.string.ok, this)
                    .setCancelable(false)
                    .create();

            Window window = dlg.getWindow();
            if (window != null) {
                window.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_VISIBLE);
                if(mContext instanceof Service){
                    if (Build.VERSION.SDK_INT >= 22)
                        window.setType(WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY);
                    else
                        window.setType(WindowManager.LayoutParams.TYPE_SYSTEM_ERROR);
                }
                dlg.show();
            }
            mEdit.setFocusable(true);
            mEdit.requestFocus();
        }

        @Override
        public void onClick(DialogInterface dialog, int which) {
            mList[mIdx] = mEdit.getText().toString();
            updateSplit();
            mListView.smoothScrollToPosition(mIdx);
        }
    }

}
