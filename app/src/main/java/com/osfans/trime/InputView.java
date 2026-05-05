/*
 * SPDX-FileCopyrightText: 2015 - 2025 Rime community
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package com.osfans.trime;

import android.content.Context;
import android.text.TextUtils;
import android.util.Log;
import android.view.TextureView;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewParent;
import android.widget.FrameLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.androlua.LuaUtil;
import com.osfans.trime.core.Rime;
import com.osfans.trime.keyboard.AbsKeyboardView;
import com.osfans.trime.keyboard.FlexboxKeyboardView;
import com.osfans.trime.keyboard.KeyboardView;
import com.osfans.trime.keyboard.ModifierState;
import com.osfans.trime.keyboard.RowKeyboardView;
import com.osfans.trime.keyboard.SymbolsKeyboardView;
import com.osfans.trime.theme.ThemeManager;
import com.osfans.trime.util.Function;

import androidx.annotation.NonNull;

import org.luaj.Globals;
import org.luaj.LuaValue;
import org.luaj.lib.ResourceFinder;
import org.luaj.lib.jse.JsePlatform;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

/**
 * 输入视图容器类，负责管理不同键盘视图的切换、缓存和生命周期。
 * 该类继承自FrameLayout，实现ResourceFinder接口以支持Lua脚本资源加载。
 * 支持根据Rime输入法方案（Schema）动态切换键盘布局，并缓存已加载的键盘视图以提高性能。
 */
public class InputView extends FrameLayout implements ResourceFinder {

    /** 当前显示的键盘视图实例 */
    private View mKeyboardView;
    /** Lua全局环境，用于执行键盘相关的Lua脚本 */
    private Globals globals;
    /** 上一个显示的键盘视图，用于切换时的过渡动画 */
    private View oldView;

    /**
     * 构造输入视图实例。
     * @param context 上下文对象
     */
    public InputView(@NonNull Context context) {
        super(context);
        setClipChildren(false); // 允许子View超出边界绘制（如按键阴影）
        setClipToPadding(false); // 允许内容绘制到padding区域
        // 获取当前Rime方案ID，若为空则从偏好设置中读取上次选择的方案
        String id = Rime.getRimeStatus().getSchemaId();
        if(TextUtils.isEmpty(id))
            id = Function.getPref(context).getString("select_schema_id","");
        setKeyboard(id); // 初始化加载对应方案的键盘
    }

    /**
     * 重写测量方法，捕获可能的异常避免崩溃。
     */
    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        try {
            super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        }catch (Exception e){
            e.printStackTrace(); // 调试阶段打印异常，正式环境可替换为日志系统
        }
    }

    /**
     * 设置当前显示的键盘视图（针对KeyboardView类型），执行切换动画并更新相关状态。
     * @param keyboardView 要设置的键盘视图实例
     */
    private void setKeyboardView(KeyboardView keyboardView) {
        if (keyboardView.equals(mKeyboardView))
            return; // 相同视图无需重复设置
        oldView = mKeyboardView; // 保存旧视图用于过渡
        mKeyboardView = keyboardView;
        // 若新视图已有父布局，先移除避免布局冲突
        ViewParent parent = keyboardView.getParent();
        if (parent != null && parent instanceof ViewGroup) {
            ((ViewGroup) parent).removeView(keyboardView);
        }
        // 同步ASCII模式状态到Rime引擎
        Rime.setRimeOption("ascii_mode", keyboardView.isAsciiMode());
        setShifted(false); // 重置Shift状态
        ModifierState.setShifted(false);
        ModifierState.setShiftLock(false);
        // 添加新键盘视图到容器最底层
        addView(keyboardView, 0, new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        if (oldView != null) {
            // 旧键盘执行淡出动画，动画结束后移除
            oldView.animate()
                    .alpha(0f)
                    .setDuration(100)
                    .withLayer() // 动画期间启用硬件层加速提升性能
                    .withEndAction(() -> {
                        removeView(oldView);
                        oldView.setAlpha(1.0f); // 恢复透明度以备复用
                    })
                    .start();
        }
    }

    /**
     * 设置当前显示的键盘视图（针对通用View类型），执行切换动画。
     * @param keyboardView 要设置的键盘视图实例
     */
    private void setKeyboardView(View keyboardView) {
        oldView = mKeyboardView;
        mKeyboardView = keyboardView;
        ViewParent parent = keyboardView.getParent();
        if (parent != null && parent instanceof ViewGroup) {
            ((ViewGroup) parent).removeView(keyboardView);
        }
        addView(keyboardView, 0, new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        if (oldView != null) {
            oldView.animate()
                    .alpha(0f)
                    .setDuration(100)
                    .withLayer()
                    .withEndAction(() -> {
                        removeView(oldView);
                        oldView.setAlpha(1.0f);
                    })
                    .start();
        }
    }

    /** 当前加载的Rime方案ID */
    private String mCurrentSchemaId;

    // 2. 视图缓存：SchemaId -> KeyboardView 实例，避免重复创建相同方案的键盘
    private final Map<String, KeyboardView> mViewCache = new HashMap<>();
    /** 符号键盘视图缓存：SchemaId -> SymbolsKeyboardView 实例 */
    private final Map<String, SymbolsKeyboardView> mSymbolsViewCache = new HashMap<>();

    /**
     * 根据方案ID切换当前显示的键盘。
     * 支持切换逻辑：缓存查找 -> Lua脚本加载 -> 多类型键盘创建 -> 默认键盘回退。
     * @param id 目标Rime方案ID，若为".last"则切换到上一个键盘
     */
    public void setKeyboard(String id) {
        Log.w("TAG", "setKeyboard:s " + id);
        if(".last".equals(id)){
            if(oldView!=null)
                setKeyboardView(oldView); // 切换到上一个键盘
            return;
        }
        if (id == null || id.equals(mCurrentSchemaId)) return; // 相同方案无需重复加载
        mCurrentSchemaId = id;
        id=getKeyboardId(id); // 解析实际的键盘ID
        Log.w("TAG", "setKeyboard:e " + id);
        // 优先从缓存获取键盘视图
        KeyboardView targetView = mViewCache.get(id);
        if (targetView == null) {
            // 初始化Lua运行环境
            Globals globals = JsePlatform.standardGlobals();
            globals.finder = this; // 设置资源查找器为当前实例
            LuaValue func = globals.loadfilex(id + ".lua"); // 加载对应方案的Lua脚本
            try {
                if (func.isfunction()) {
                    LuaValue ret = func.call(); // 执行Lua脚本
                    if (ret.isuserdata(View.class)) {
                        // 如果Lua返回自定义View，直接设置
                        setKeyboardView(ret.touserdata(View.class));
                        return;
                    }
                } else {
                    // Lua脚本不是函数，发送错误信息
                    ThemeManager.sendMsg("setKeyboard " + func.tojstring());
                }
            } catch (Exception e) {
                ThemeManager.sendMsg("setKeyboard " + e); // 捕获Lua执行异常并通知主题管理器
            }
            // 根据Lua配置创建对应的键盘视图类型
            if (globals.get("rows").istable()) {
                targetView = new RowKeyboardView(getContext(), globals); // 行布局键盘
            } else if (globals.get("flex_box").istable()) {
                targetView = new FlexboxKeyboardView(getContext(), globals); // Flexbox布局键盘
            } else if (globals.get("keys").istable()) {
                targetView = new AbsKeyboardView(getContext(), globals); // 绝对布局键盘
            } else if (globals.get("key_maps").istable()) {
                // 符号键盘特殊处理，显示在自定义视图区域
                SymbolsKeyboardView symbolsView = mSymbolsViewCache.get(id);
                if(symbolsView==null){
                    symbolsView=new SymbolsKeyboardView(getContext(), globals);
                    mSymbolsViewCache.put(id,symbolsView);
                }
                TrimeService.getInstance().showCustomView(symbolsView);
                mCurrentSchemaId=null; // 符号键盘不记录为当前方案
                return;
            } else {
                // 未知布局类型，回退到默认qwerty36键盘
                func = globals.loadfilex("themes/default/keyboards/qwerty36.lua");
                if (func.isfunction())
                    func.call();
                targetView = new RowKeyboardView(getContext(), globals);
            }
            mViewCache.put(id, targetView); // 存入缓存
            if (targetView.isLock()) {
                mViewCache.put(".default", targetView); // 锁定类型的键盘设为默认
            }
        }
        setKeyboardView(targetView); // 设置最终找到的键盘视图
    }

    /**
     * 解析并返回实际可用的键盘ID，包含多级回退逻辑。
     * 回退顺序：缓存检查 -> 文件存在性检查 -> 主题管理器映射 -> 默认方案 -> 固定默认键盘。
     * @param id 初始传入的键盘ID
     * @return 实际可用的键盘ID
     */
    private String getKeyboardId(String id) {
        if (id.isEmpty()) {
            // ID为空时，先尝试获取默认键盘配置
            String k = Config.getKeyboard(".default");
            if (!TextUtils.isEmpty(k)) {
                if(mViewCache.containsKey(id))
                    return id;
                if (new File(findFile(id + ".lua")).exists())
                    return id;
            }
            // 回退到当前Rime方案ID
            id = Rime.getRimeStatus().getSchemaId();
            if(TextUtils.isEmpty(id))
                id = Function.getPref(getContext()).getString("select_schema_id","");
        }
        Log.w("TAG", "setKeyboard:2 " + id);
        if(mViewCache.containsKey(id))
            return id; // 缓存中已有，直接返回
        if (new File(findFile(id + ".lua")).exists())
            return id; // 键盘文件存在，返回

        id = ThemeManager.getKeyboard(id); // 通过主题管理器解析键盘ID
        Log.w("TAG", "setKeyboard:4 " + id);
        if(mViewCache.containsKey(id))
            return id;
        if (new File(findFile(id + ".lua")).exists())
            return id;

        id = Rime.getRimeStatus().getSchemaId(); // 再次回退到当前Rime方案
        if(TextUtils.isEmpty(id))
            id = Function.getPref(getContext()).getString("select_schema_id","");
        Log.w("TAG", "setKeyboard:5 " + id);
        if(mViewCache.containsKey(id))
            return id;
        if (new File(findFile(id + ".lua")).exists())
            return id;

        id = ThemeManager.getKeyboard(id); // 再次通过主题管理器解析
        Log.w("TAG", "setKeyboard:6 " + id);
        if(mViewCache.containsKey(id))
            return id;
        if (new File(findFile(id + ".lua")).exists())
            return id;

        return "qwerty36"; // 所有回退都失败，返回固定默认键盘
    }

    /**
     * 实现ResourceFinder接口，查找Lua脚本资源。
     * 查找顺序：绝对路径 -> 键盘目录 -> 应用assets目录。
     * @param name 资源文件名
     * @return 资源输入流，未找到返回null
     */
    @Override
    public InputStream findResource(String name) {
        if (TextUtils.isEmpty(name))
            return null;
        // 1. 尝试绝对路径
        try {
            if (new File(name).exists())
                return new FileInputStream(name);
        } catch (Exception e) {
            if (BuildConfig.DEBUG)
                e.printStackTrace();
        }
        // 2. 尝试键盘目录下的文件
        try {
            return new FileInputStream(new File(Config.getKeyboardDir(), name));
        } catch (Exception e) {
            if (BuildConfig.DEBUG)
                e.printStackTrace();
        }
        if (!name.endsWith(".lua"))
            return null; // 非Lua文件不再尝试assets
        // 3. 尝试assets下的默认键盘目录
        try {
            return getContext().getAssets().open("themes/default/keyboards/" + name);
        } catch (Exception e) {
            if (BuildConfig.DEBUG)
                e.printStackTrace();
        }
        // 4. 尝试assets根目录
        try {
            return getContext().getAssets().open(name);
        } catch (Exception e) {
            if (BuildConfig.DEBUG)
                e.printStackTrace();
        }
        return null;
    }

    /**
     * 返回键盘文件的绝对路径。
     * @param filename 键盘文件名
     * @return 绝对路径，若以/开头则直接返回，否则拼接键盘目录
     */
    @Override
    public String findFile(String filename) {
        if (TextUtils.isEmpty(filename))
            return null;
        if (filename.startsWith("/"))
            return filename; // 绝对路径直接返回
        return new File(Config.getKeyboardDir(), filename).getAbsolutePath();
    }

    /**
     * 使当前键盘的 composing 按键无效，触发重绘。
     */
    public void invalidateComposingKeys() {
        if (mKeyboardView instanceof KeyboardView)
            ((KeyboardView) mKeyboardView).invalidateComposingKeys();
    }

    /**
     * 使当前键盘的所有按键无效，触发重绘。
     */
    public void invalidateAllKeys() {
        if (mKeyboardView instanceof KeyboardView)
            ((KeyboardView) mKeyboardView).invalidateAllKeys();
    }

    /**
     * 检查当前是否处于Shift按下状态。
     * @return true表示Shift激活，false表示未激活
     */
    public boolean isShifted() {
        return ModifierState.isShifted();
    }

    /**
     * 设置当前键盘的Shift状态。
     * @param shifted 是否按下Shift
     */
    public void setShifted(boolean shifted) {
        if (mKeyboardView instanceof KeyboardView)
            ((KeyboardView) mKeyboardView).setShifted(shifted);
    }

    /**
     * 设置当前键盘的ASCII模式状态。
     * @param asciiMode 是否启用ASCII模式
     */
    public void setAsciiMode(boolean asciiMode) {
        if (mKeyboardView instanceof KeyboardView)
            ((KeyboardView) mKeyboardView).setAsciiMode(asciiMode);
    }
}
