/*
 * SPDX-FileCopyrightText: 2015 - 2026 Rime community
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package com.osfans.trime.util;

import static android.content.Intent.FLAG_ACTIVITY_NEW_TASK;

import android.annotation.TargetApi;
import android.app.SearchManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.icu.text.DateFormat;
import android.icu.util.Calendar;
import android.icu.util.ULocale;
import android.net.Uri;
import android.os.Build.*;
import android.preference.PreferenceManager;
import android.text.TextUtils;
import android.util.Log;
import android.util.SparseArray;
import android.view.KeyEvent;
import android.widget.Toast;

import com.osfans.trime.BuildConfig;
import com.osfans.trime.Config;
import com.osfans.trime.TrimeService;
import com.osfans.trime.core.DataManager;
import com.osfans.trime.dialog.DeployDialog;

import org.luaj.LuaTable;

import java.io.File;
import java.text.FieldPosition;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * 功能工具类,提供应用启动、日期格式化、偏好设置管理、Lua 脚本执行等功能。
 * 支持打开系统应用、执行 Intent、调用 Lua 脚本等高级功能。
 */
public class Function {
    private static String TAG = Function.class.getSimpleName();
    // 特殊按键码到应用类别的映射表
    private static SparseArray<String> sApplicationLaunchKeyCategories;

    static {
        sApplicationLaunchKeyCategories = new SparseArray<String>();
        // 浏览器键 -> 浏览器应用
        sApplicationLaunchKeyCategories.append(
                KeyEvent.KEYCODE_EXPLORER, "android.intent.category.APP_BROWSER");
        // 邮件键 -> 邮件应用
        sApplicationLaunchKeyCategories.append(
                KeyEvent.KEYCODE_ENVELOPE, "android.intent.category.APP_EMAIL");
        // 联系人键(207) -> 联系人应用
        sApplicationLaunchKeyCategories.append(207, "android.intent.category.APP_CONTACTS");
        // 日历键(208) -> 日历应用
        sApplicationLaunchKeyCategories.append(208, "android.intent.category.APP_CALENDAR");
        // 邮件键2(209) -> 邮件应用
        sApplicationLaunchKeyCategories.append(209, "android.intent.category.APP_EMAIL");
        // 计算器键(210) -> 计算器应用
        sApplicationLaunchKeyCategories.append(210, "android.intent.category.APP_CALCULATOR");
    }

    /**
     * 根据按键码打开对应的系统应用。
     * 支持浏览器、邮件、联系人、日历、计算器等系统应用。
     *
     * @param context Android 上下文对象。
     * @param keyCode 按键码。
     * @return 是否成功打开应用。
     */
    @TargetApi(VERSION_CODES.ICE_CREAM_SANDWICH_MR1)
    public static boolean openCategory(Context context, int keyCode) {
        String category = sApplicationLaunchKeyCategories.get(keyCode);
        if (category != null) {
            Intent intent = Intent.makeMainSelectorActivity(Intent.ACTION_MAIN, category);
            intent.setFlags(FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_NO_HISTORY);
            try {
                context.startActivity(intent);
            } catch (Exception ex) {
                Log.e(TAG, "Start Activity Exception" + ex);
            }
            return true;
        }
        return false;
    }

    /**
     * 根据参数启动 Intent。
     * 支持 URI、组件名、包名三种格式。
     *
     * @param context Android 上下文对象。
     * @param arg 启动参数(URI/组件名/包名)。
     */
    private static void startIntent(Context context, String arg) {
        Intent intent;
        try {
            if (arg.indexOf(':') >= 0) {
                // 参数是 URI,直接解析
                intent = Intent.parseUri(arg, Intent.URI_INTENT_SCHEME);
            } else if (arg.indexOf('/') >= 0) {
                // 参数是组件名,构建启动 Intent
                intent = new Intent(Intent.ACTION_MAIN);
                intent.addCategory(Intent.CATEGORY_LAUNCHER);
                intent.setComponent(ComponentName.unflattenFromString(arg));
            } else {
                // 假设参数是包名
                intent = context.getPackageManager().getLaunchIntentForPackage(arg);
            }
            intent.setFlags(FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_NO_HISTORY);
            context.startActivity(intent);
        } catch (Exception ex) {
            Log.e(TAG, "Start Activity Exception" + ex);
        }
    }

    /**
     * 根据 Action 和参数启动 Intent。
     * 支持搜索、分享等特殊 Action。
     *
     * @param context Android 上下文对象。
     * @param action Intent Action。
     * @param arg 参数字符串。
     */
    private static void startIntent(Context context, String action, String arg) {
        action = "android.intent.action." + action.toUpperCase(Locale.getDefault());
        try {
            Intent intent = new Intent(action);
            switch (action) {
                case Intent.ACTION_WEB_SEARCH:
                case Intent.ACTION_SEARCH:
                    if (arg.startsWith("http")) { // web_search 无法直接打开网址
                        startIntent(context, arg);
                        return;
                    }
                    intent.putExtra(SearchManager.QUERY, arg);
                    break;
                case Intent.ACTION_SEND: // 分享文本
                    intent.setType("text/plain");
                    intent.putExtra(Intent.EXTRA_TEXT, arg);
                    break;
                default:
                    if (!TextUtils.isEmpty(arg)) intent.setData(Uri.parse(arg));
                    break;
            }
            intent.setFlags(FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_NO_HISTORY);
            context.startActivity(intent);
        } catch (Exception ex) {
            Log.e(TAG, "Start Activity Exception" + ex);
        }
    }


    /**
     * 格式化日期字符串。
     * 支持 ICU 库( Android N+)和传统 SimpleDateFormat。
     *
     * @param option 日期格式字符串,可包含区域设置(如 "zh_CN@ yyyy-MM-dd")。
     * @return 格式化后的日期字符串。
     */
    public static String getDate(String option) {
        String s = "";
        String locale = "";
        if (option.contains("@")) {
            String[] ss = option.split(" ", 2);
            if (ss.length == 2 && ss[0].contains("@")) {
                locale = ss[0];
                option = ss[1];
            } else if (ss.length == 1) {
                locale = ss[0];
                option = "";
            }
        }
        if (VERSION.SDK_INT >= VERSION_CODES.N && !TextUtils.isEmpty(locale)) {
            ULocale ul = new ULocale(locale);
            Calendar cc = Calendar.getInstance(ul);
            DateFormat df;
            if (TextUtils.isEmpty(option)) {
                df = DateFormat.getDateInstance(DateFormat.LONG, ul);
            } else {
                df = new android.icu.text.SimpleDateFormat(option, ul);
            }
            s = df.format(cc, new StringBuffer(256), new FieldPosition(0)).toString();
        } else {
            s = new SimpleDateFormat(option, Locale.getDefault()).format(new Date()); // 时间
        }
        return s;
    }

    /**
     * 批量设置 SharedPreferences 的值。
     *
     * @param preferences SharedPreferences 对象。
     * @param map 键值对映射。
     * @return 是否成功提交。
     */
    public static boolean setAll(SharedPreferences preferences, Map<String, Object> map) {
        Set<Map.Entry<String, Object>> sets = map.entrySet();
        SharedPreferences.Editor editor = preferences.edit();
        for (Map.Entry<String, Object> entry : sets) {
            String key = entry.getKey();
            Object newValue = entry.getValue();
            if (newValue instanceof String)
                editor.putString(key, (String) newValue);
            else if (newValue instanceof Integer)
                editor.putInt(key, (Integer) newValue);
            else if (newValue instanceof Long)
                editor.putLong(key, (Long) newValue);
            else if (newValue instanceof Float)
                editor.putFloat(key, (Float) newValue);
            else if (newValue instanceof Set)
                editor.putStringSet(key, (Set<String>) newValue);
            else if (newValue instanceof Boolean)
                editor.putBoolean(key, (Boolean) newValue);
        }
        return editor.commit();
    }

    /**
     * 获取应用版本号。
     *
     * @param context Android 上下文对象。
     * @return 版本号字符串,失败则返回 null。
     */
    public static String getVersion(Context context) {
        try {
            return context.getPackageManager().getPackageInfo(context.getPackageName(), 0).versionName;
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * 检查指定应用是否已安装。
     *
     * @param context Android 上下文对象。
     * @param app 应用包名。
     * @return 是否已安装。
     */
    public static boolean isAppAvailable(Context context, String app) {
        final PackageManager packageManager = context.getPackageManager();
        List<PackageInfo> pinfo = packageManager.getInstalledPackages(0);
        if (pinfo != null) {
            for (int i = 0; i < pinfo.size(); i++) {
                String pn = pinfo.get(i).packageName;
                if (pn.equals(app)) {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * 获取默认 SharedPreferences 对象。
     *
     * @param context Android 上下文对象。
     * @return SharedPreferences 对象。
     */
    public static SharedPreferences getPref(Context context) {
        return PreferenceManager.getDefaultSharedPreferences(context);
    }

    /**
     * 检查应用版本是否发生变化。
     *
     * @param context Android 上下文对象。
     * @return 版本是否变化。
     */
    public static boolean isDiffVer(Context context) {
        String version = getVersion(context);
        SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences(context);
        String pref_ver = pref.getString("version_name", "");
        boolean isDiff = !version.contentEquals(pref_ver);
        if (isDiff) {
            SharedPreferences.Editor edit = pref.edit();
            edit.putString("version_name", version);
            edit.apply();
        }
        return isDiff;
    }

    /**
     * 打印堆栈跟踪信息(仅 Debug 模式)。
     *
     * @param text 提示信息。
     */
    public static void printStackTrace(String text) {
        if (!BuildConfig.DEBUG)
            return;
        try {
            throw new RuntimeException(text + "");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * 获取用户数据目录路径。
     *
     * @param context Android 上下文对象。
     * @return 用户数据目录绝对路径。
     */
    public static String getUserDataDir(Context context) {
        return DataManager.getUserDataDir().getAbsolutePath();
    }

    /**
     * 处理命令,支持 Lua 脚本和内置命令。
     * 如果存在对应的 Lua 脚本文件,则执行脚本;否则执行内置命令。
     *
     * @param context TrimeService 实例。
     * @param command 命令名称。
     * @param option 命令参数(可变参数)。
     * @return 命令执行结果字符串。
     */
    public static String handle(TrimeService context, String command, Object... option) {
        String s = null;
        if (command == null)
            return s;
        // 尝试查找 Lua 脚本文件
        String path = Config.getScriptsPath(command);
        Log.w(TAG, "handle: "+path );
        if (new File(path).exists()) {
            Object ret = context.doFile(path, option);
            if (ret == null)
                return null;
            // 如果返回 LuaTable,则设置为候选词列表
            if (ret instanceof LuaTable) {
                context.setCandidates(new ArrayList<String>((Collection<? extends String>) ((LuaTable) ret).checktable().values()));
                return null;
            }
            return ret.toString();
        }
        return handle(context, command, "");
    }

    /**
     * 处理命令(字符串参数版本)。
     * 支持 GPT AI 生成、日期格式化、应用启动、部署、广播等多种功能。
     *
     * @param context TrimeService 实例。
     * @param command 命令名称。
     * @param option 命令参数字符串。
     * @return 命令执行结果字符串。
     */
    public static String handle(TrimeService context, String command, String option) {
        String s = null;
        if (command == null)
            return s;
        // 尝试查找 Lua 脚本文件
        String path = Config.getScriptsPath(command);
        Log.w(TAG, "handle: "+path );
        if (new File(path).exists()) {
            Object ret = context.doFile(path, option);
            if (ret == null)
                return null;
            if (ret instanceof LuaTable) {
                return null;
            }
            return ret.toString();
        }
        // 执行内置命令
        switch (command) {
            // ==================== 日期格式化 ====================
            case "date":
                s = getDate(option);
                break;
            // ==================== 启动应用或打开网址 ====================
            case "run":
                if (option.startsWith("http")) {
                    try {
                        context.startActivity(new Intent(Intent.ACTION_VIEW).addFlags(FLAG_ACTIVITY_NEW_TASK).setData(Uri.parse(option)));
                    }catch (Exception e){
                        Log.w(TAG, "handle: "+option );
                        e.printStackTrace();
                        CustomToast.show(context,e.toString(),Toast.LENGTH_SHORT, true);
                    }
                    break;
                }
                startIntent(context, option); // 启动程序
                break;
            // ==================== Rime 部署 ====================
            case "deploy":
                new DeployDialog(context).show(context.getToken());
                break;
            // ==================== 发送广播 ====================
            case "broadcast":
                if(option.equals("com.osfans.trime.action.DEPLOY"))
                    new DeployDialog(context).show(context.getToken());
                else if (option.equals("com.osfans.trime.action.SYNC_USER_DATA")) {
                    boolean ok = TrimeService.getInstance() != null
                            && TrimeService.getInstance().syncUserData();
                    if (TrimeService.getInstance() != null) {
                        TrimeService.getInstance().showStatusDialog(ok ? "同步完成" : "同步失败");
                    }
                }
                else
                    context.sendBroadcast(new Intent(option)); // 广播
                break;
            // ==================== 添加短语 ====================
            case "add_phrase":
                TrimeService.getInstance().addPhrase(option); // 新建短语
                CustomToast.show(context,"已添加到短语 "+option,Toast.LENGTH_SHORT, true);
                break;
            // ==================== 直接提交文本 ====================
            case "commit":
                s = option;
                break;
            // ==================== 其他 Intent ====================
            default:
                startIntent(context, command, option); // 其他intent
                break;
        }
        return s;
    }


    /**
     * 显示偏好设置对话框(启动 PrefLauncher Activity)。
     * 用于从输入法服务中打开设置界面。
     *
     * @param TrimeService TimeService。
     */
    public static void showPrefDialog(Context TrimeService) {}

    /**
     * 保存字符串到 SharedPreferences。
     *
     * @param context Android 上下文对象。
     * @param id 键名。
     * @param s 字符串值。
     */
    public static void saveString(Context context, String id, String s) {
        getPref(context).edit().putString(id,s).apply();
    }

    /**
     * 从 SharedPreferences 加载字符串。
     *
     * @param context Android 上下文对象。
     * @param id 键名。
     * @param def 默认值。
     * @return 字符串值。
     */
    public static String loadString(Context context, String id, String def) {
        return getPref(context).getString(id,def);
    }

    /**
     * 保存布尔值到 SharedPreferences。
     *
     * @param context Android 上下文对象。
     * @param id 键名。
     * @param s 布尔值。
     */
    public static void saveBoolean(Context context, String id, boolean s) {
        getPref(context).edit().putBoolean(id,s).apply();
    }

    /**
     * 从 SharedPreferences 加载布尔值。
     *
     * @param context Android 上下文对象。
     * @param id 键名。
     * @param def 默认值。
     * @return 布尔值。
     */
    public static boolean loadBoolean(Context context, String id, boolean def) {
        return getPref(context).getBoolean(id,def);
    }
}
