/*
 * SPDX-FileCopyrightText: 2015 - 2026 Rime community
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package com.osfans.trime.keyboard;

import android.content.Context;
import android.util.Log;
import android.view.ContextThemeWrapper;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import androidx.viewpager2.widget.ViewPager2;

import com.google.android.material.tabs.TabLayout;
import com.google.android.material.tabs.TabLayoutMediator;
import com.osfans.trime.Config;
import com.osfans.trime.Event;
import com.osfans.trime.Key;
import com.osfans.trime.TrimeService;
import com.osfans.trime.keyboard.adapter.ListPagerAdapter;
import com.osfans.trime.theme.KeyStyle;
import com.osfans.trime.theme.Style;
import com.osfans.trime.theme.ThemeManager;

import org.luaj.Globals;
import org.luaj.LuaTable;
import org.luaj.LuaValue;
import org.luaj.lib.ResourceFinder;
import org.luaj.lib.jse.JsePlatform;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;

/**
 * 符号键盘视图类。
 * 显示符号候选词列表,支持分页、Tab 切换、返回、删除等操作。
 * 实现 ResourceFinder 接口,用于 Lua 脚本加载资源文件。
 */
public class SymbolsKeyboardView extends LinearLayout implements ResourceFinder {
    // ==================== 成员变量 ====================
    /** Lua 按键映射表 */
    private final LuaTable mKeyMap;
    /** 按键样式 */
    private final KeyStyle mKeyStyle;
    /** TrimeService 实例 */
    private final TrimeService mTrime;
    /** 符号样式 */
    private final Style mSymbolStyle;
    /** ViewPager2 组件 */
    private ViewPager2 viewPager;
    /** 列表分页适配器 */
    private ListPagerAdapter mAdapter;

    /**
     * 构造函数(从 symbols.lua 加载配置)。
     *
     * @param context 上下文。
     */
    public SymbolsKeyboardView(@NonNull Context context) {
        super(context);
        long time=System.currentTimeMillis();
        Globals globals = JsePlatform.standardGlobals();
        globals.finder = this;
        globals.loadfile("symbols.lua").call();
        mKeyMap=globals.get("key_maps").checktable();
        mSymbolStyle = ThemeManager.getStyle().getStyle("symbol");
        mKeyStyle=mSymbolStyle.getKeyStyle("key",ThemeManager.getStyle().getKeyStyle("key"));
        mTrime=TrimeService.getInstance();
        setBackground(mSymbolStyle.getBackground(0x00000000));
        initView();
        Log.w("SymbolsKeyboardView", "init time: "+(System.currentTimeMillis()-time) );
    }

    /**
     * 构造函数(使用已有的 Lua 全局环境)。
     *
     * @param context 上下文。
     * @param globals Lua 全局环境。
     */
    public SymbolsKeyboardView(@NonNull Context context,Globals globals) {
        super(context);
        long time=System.currentTimeMillis();
        mKeyMap=globals.get("key_maps").checktable();
        mSymbolStyle = ThemeManager.getStyle().getStyle("symbol");
        mKeyStyle=mSymbolStyle.getKeyStyle("key",ThemeManager.getStyle().getKeyStyle("key"));
        mTrime=TrimeService.getInstance();
        setBackground(mSymbolStyle.getBackground(0x00000000));
        initView();
        Log.w("SymbolsKeyboardView", "init time: "+(System.currentTimeMillis()-time) );
    }

    private void initView() {
        // 导入 android.view.ContextThemeWrapper
        Context themedContext = new ContextThemeWrapper(getContext(), androidx.appcompat.R.style.Theme_AppCompat_Light);
        // 使用 themedContext 来初始化 TabLayout 或 View
        TabLayout tabLayout = new TabLayout(themedContext);
        tabLayout.setTabTextColors(mKeyStyle.getTextColor(),mKeyStyle.getKeyStyle("pressed",mKeyStyle).getTextColor());
        tabLayout.setSelectedTabIndicatorColor(mSymbolStyle.getColor("indicator_color",mKeyStyle.getKeyStyle("pressed",mKeyStyle).getTextColor()));
        tabLayout.setTabMode(TabLayout.MODE_SCROLLABLE);
        viewPager = new ViewPager2(getContext());
        LinearLayout root = new LinearLayout(getContext());
        root.setOrientation(LinearLayout.VERTICAL);
        root.addView(tabLayout, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        root.addView(viewPager, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1));
        addView(root, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT,1));
        mAdapter = new ListPagerAdapter(mKeyMap);
        viewPager.setAdapter(mAdapter);
        // 2. 使用 TabLayoutMediator 连接 TabLayout 和 ViewPager2
        new TabLayoutMediator(tabLayout, viewPager, (tab, position) -> {
            tab.setText(mKeyMap.get(position+1).get("name").optjstring(String.valueOf(position+1)));
        }).attach();
        LinearLayout mButtonBar = new LinearLayout(getContext());
        mButtonBar.setOrientation(LinearLayout.VERTICAL);

        KeyView mHide = new KeyView(getContext(),mKeyStyle);
        mHide.setText("△");
        mHide.setContentDescription("返回");
        mHide.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                mTrime.showSymbolsView(false);
            }
        });
        KeyView mPrev = new KeyView(getContext(),mKeyStyle);
        mPrev.setText("⇑");
        mPrev.setContentDescription("上一页");
        mPrev.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                getListView(viewPager,mAdapter).smoothScrollBy(0,-ThemeManager.getContentHeight());
            }
        });
        KeyView mNext = new KeyView(getContext(),mKeyStyle);
        mNext.setText("⇓");
        mNext.setContentDescription("下一页");
        mNext.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                getListView(viewPager,mAdapter).smoothScrollBy(0,ThemeManager.getContentHeight());
            }
        });
        KeyView backSpace = new KeyView(getContext(), mKeyStyle);
        backSpace.setText(" ⌫");
        backSpace.setContentDescription("删除");
        backSpace.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                mTrime.onKey(KeyEvent.KEYCODE_DEL, 0);
            }
        });

        LuaValue keys = mSymbolStyle.getKeyStyle("tool_bar").get("keys");
        if (keys.istable()) {
            int len = keys.length();
            for (int i = 0; i < len; i++) {
                String k = keys.get(i + 1).tojstring();
                switch (k) {
                    case "hide":
                        mButtonBar.addView(mHide, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT, 1));
                        break;
                    case "page_up":
                        mButtonBar.addView(mPrev, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT, 1));
                        break;
                    case "page_down":
                        mButtonBar.addView(mNext, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT, 1));
                        break;
                    default:
                        KeyView key = new KeyView(getContext(), new Key(new Event(k)), mKeyStyle);
                        mButtonBar.addView(key, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT, 1));
                }
            }
        } else{
            mButtonBar.addView(mHide,new LinearLayout.LayoutParams(ThemeManager.getCandidateHeight(), ThemeManager.getCandidateHeight()));
            mButtonBar.addView(mPrev,new LinearLayout.LayoutParams(ThemeManager.getCandidateHeight(), ViewGroup.LayoutParams.MATCH_PARENT,1));
            mButtonBar.addView(mNext,new LinearLayout.LayoutParams(ThemeManager.getCandidateHeight(), ViewGroup.LayoutParams.MATCH_PARENT,1));
            mButtonBar.addView(backSpace,new LinearLayout.LayoutParams(ThemeManager.getCandidateHeight(), ThemeManager.getCandidateHeight()));
        }
        switch (mSymbolStyle.getKeyStyle("tool_bar").getGravity(Gravity.RIGHT)) {
            case Gravity.LEFT:
                setOrientation(HORIZONTAL);
                mButtonBar.setOrientation(VERTICAL);
                addView(mButtonBar, 0, new LinearLayout.LayoutParams(ThemeManager.getCandidateHeight(), ViewGroup.LayoutParams.MATCH_PARENT));
                break;
            case Gravity.RIGHT:
                setOrientation(HORIZONTAL);
                mButtonBar.setOrientation(VERTICAL);
                addView(mButtonBar, new LinearLayout.LayoutParams(ThemeManager.getCandidateHeight(), ViewGroup.LayoutParams.MATCH_PARENT));
                break;
            case Gravity.TOP:
                setOrientation(VERTICAL);
                mButtonBar.setOrientation(HORIZONTAL);
                addView(mButtonBar, 0, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ThemeManager.getCandidateHeight()));
                break;
            case Gravity.BOTTOM:
                setOrientation(VERTICAL);
                mButtonBar.setOrientation(HORIZONTAL);
                addView(mButtonBar, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ThemeManager.getCandidateHeight()));
                break;
        }
        //addView(mButtonBar, new LinearLayout.LayoutParams(ThemeManager.getCandidateHeight(), ViewGroup.LayoutParams.MATCH_PARENT));
    }
    /**
     * 向下翻页。
     *
     * @return true 表示操作成功。
     */
    public boolean pageDown() {
        getListView(viewPager,mAdapter).smoothScrollBy(0,ThemeManager.getContentHeight());
        return true;
    }

    /**
     * 向上翻页。
     *
     * @return true 表示操作成功。
     */
    public boolean pageUp() {
        getListView(viewPager,mAdapter).smoothScrollBy(0,-ThemeManager.getContentHeight());
        return true;
    }

    /**
     * 获取当前页的 RecyclerView。
     * 从 ViewPager2 中查找当前页对应的 ViewHolder,并返回其内部的 RecyclerView。
     *
     * @param viewPager2 ViewPager2 组件。
     * @param mAdapter 列表分页适配器。
     * @return 当前页的 RecyclerView 实例。
     */
    private RecyclerView getListView(ViewPager2 viewPager2, ListPagerAdapter mAdapter){
        // 1. 获取当前页码
        int currentPos = viewPager2.getCurrentItem();

// 2. 访问 ViewPager2 内部的 RecyclerView
        RecyclerView internalRecyclerView = (RecyclerView) viewPager2.getChildAt(0);

// 3. 找到当前位置对应的 ViewHolder
        RecyclerView.ViewHolder viewHolder = internalRecyclerView.findViewHolderForAdapterPosition(currentPos);

        if (viewHolder instanceof ListPagerAdapter.ListViewHolder) {
            // 4. 获取你在 ViewHolder 中定义的 recyclerView
            return  ((ListPagerAdapter.ListViewHolder) viewHolder).recyclerView;
            // 现在你可以对当前页的 RecyclerView 进行操作了
        }
        return mAdapter.getListView();
    }

    /**
     * 查找资源文件(实现 ResourceFinder 接口)。
     * 按优先级查找:绝对路径 -> 键盘目录 -> assets/themes/default/keyboards/ -> symbols.lua。
     *
     * @param name 资源文件名。
     * @return 输入流,未找到则返回 null。
     */
    @Override
    public InputStream findResource(String name) {
        try {
            if (new File(name).exists())
                return new FileInputStream(name);
        } catch (Exception e) {
        }
        try {
            return new FileInputStream(new File(Config.getKeyboardDir(), name));
        } catch (Exception e) {
        }

        try {
            return getContext().getAssets().open("themes/default/keyboards/"+name);
        } catch (Exception ioe) {
           /*if (BuildConfig.DEBUG)
             e.printStackTrace();*/
        }
        try {
            return getContext().getAssets().open("themes/default/keyboards/symbols.lua");
        } catch (Exception ioe) {
           /*if (BuildConfig.DEBUG)
             e.printStackTrace();*/
        }
        return null;
    }

    /**
     * 查找文件路径(实现 ResourceFinder 接口)。
     * 如果是绝对路径则直接返回,否则返回键盘目录下的完整路径。
     *
     * @param filename 文件名。
     * @return 文件的绝对路径。
     */
    @Override
    public String findFile(String filename) {
        if (filename.startsWith("/"))
            return filename;
        return new File(Config.getKeyboardDir(), filename).getAbsolutePath();
    }

}
