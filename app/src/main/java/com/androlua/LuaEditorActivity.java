package com.androlua;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.text.TextUtils;
import android.util.DisplayMetrics;
import android.util.Log;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.webkit.MimeTypeMap;
import android.widget.AdapterView;
import android.widget.ArrayListAdapter;
import android.widget.EditText;
import android.widget.GridLayout;
import android.widget.HorizontalScrollView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import android.content.FileProvider;

import com.myopicmobile.textwarrior.android.OnSelectionChangedListener;
import com.myopicmobile.textwarrior.common.AutoIndent;
import com.myopicmobile.textwarrior.common.DocumentProvider;
import com.myopicmobile.textwarrior.common.LuaParser;
import com.myopicmobile.textwarrior.common.PackageUtil;

import org.json.JSONObject;
import org.luaj.Globals;
import org.luaj.Lua;
import org.luaj.LuaClosure;
import org.luaj.LuaError;
import org.luaj.LuaTable;
import org.luaj.LuaValue;
import org.luaj.Varargs;
import com.osfans.trime.BuildConfig;
import com.osfans.trime.util.CustomToast;
import org.luaj.android.file;
import org.luaj.android.http;
import org.luaj.android.json;
import org.luaj.android.res;
import org.luaj.android.saf;
import org.luaj.compiler.DumpState;
import org.luaj.lib.ResourceFinder;
import org.luaj.lib.jse.JsePlatform;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Enumeration;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipOutputStream;


/**
 * Created by nirenr on 2019/10/12.
 */

public class LuaEditorActivity extends Activity implements ResourceFinder {
    private static String mHelp;
    private LuaEditor edit;
    private String path;
    private ArrayList<String> permissions;
    private File mDir;
    private String mName;
    private DisplayMetrics dm;
    private ArrayList<JsonUtil.HistoryData> history = new ArrayList<>();
    private String mHistoryPath;
    private TextView mTitle;
    private File mRootDir;
    private Globals mGlobals;
    private File mProjectsDir;
    private File mProjDir;
    private LinearLayout mMenu;
    private int _h;
    private ProgressDialog mDlg;
    private static LuaEditorActivity sInstance;

    public static LuaEditorActivity getInstance() {
        return sInstance;
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        sInstance = null;
    }

    public LinearLayout getMenu() {
        return mMenu;
    }

    public LuaEditor getEdit() {
        return edit;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        sInstance = this;
        try {
            InputStream f = getAssets().open("main.lua");
            if (f != null) {
                startActivity(new Intent(this, LuaActivity.class).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
                finish();
                return;
            }
        } catch (Exception e) {
            if (BuildConfig.DEBUG)
                e.printStackTrace();
        }
        ComponentName networkActivity = new ComponentName(getApplicationContext(), ImportProject.class);
        PackageManager packageManager = getApplicationContext().getPackageManager();
        int i = packageManager.getComponentEnabledSetting(networkActivity);
        Log.d("luaj", "ImportProject: " + i);
        packageManager.setComponentEnabledSetting(networkActivity, PackageManager.COMPONENT_ENABLED_STATE_ENABLED, PackageManager.DONT_KILL_APP);


        dm = getResources().getDisplayMetrics();
        getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE);
        initDir();
        mHistoryPath = new File(mDir, "history.json").getAbsolutePath();
        File f = new File(mDir, "android.json");
        history = JsonUtil.loadHistoryData(mHistoryPath);

        /*if (f.exists())
            PackageUtil.load(this, f.getAbsolutePath());
        else
            PackageUtil.load(this);*/
        initView();
        loadConfig();
        if (Build.VERSION.SDK_INT >= 23) {
            if (checkCallingOrSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                try {
                    permissions = new ArrayList<String>();
                    PackageManager pm = getPackageManager();
                    String[] ps2 = pm.getPackageInfo(getPackageName(), PackageManager.GET_PERMISSIONS).requestedPermissions;
                    for (String p : ps2) {
                        try {
                            if ((pm.getPermissionInfo(p, 0).protectionLevel & 1) != 0)
                                checkPermission(p);
                        } catch (Exception e) {
                            if (BuildConfig.DEBUG)
                                e.printStackTrace();
                        }
                    }
                    if (!permissions.isEmpty()) {
                        String[] ps = new String[permissions.size()];
                        permissions.toArray(ps);
                        requestPermissions(ps,
                                0);
                        return;
                    }
                } catch (Exception e) {
                    if (BuildConfig.DEBUG)
                        e.printStackTrace();
                }
            }
        }

        if (history.size() > 0 && readHistory(0))
            return;

        readFile();

    }

    @SuppressLint("ResourceType")
    private void initView() {
        edit = new LuaEditor(this);
        mGlobals = JsePlatform.standardGlobals();
        edit.setGlobal(mGlobals);
        mGlobals.finder = this;
        mGlobals.load(new json());
        mGlobals.load(new file());
        mGlobals.load(new res(null));
        LuaValue key = LuaValue.NIL;
        Varargs next;
        ArrayList<String> ks = new ArrayList<>();
        String[] ss;
        while (!(next = mGlobals.next(key)).isnil(1)) {
            key = next.arg1();
            LuaValue val = next.arg(2);
            if (val.istable()) {
                Varargs n;
                LuaValue k = LuaValue.NIL;
                ArrayList<String> vs = new ArrayList<>();
                while (!(n = val.next(k)).isnil(1)) {
                    k = n.arg1();
                    vs.add(k.tojstring());
                }
                ss = new String[vs.size()];
                vs.toArray(ss);
                edit.addPackage(key.tojstring(), ss);
            }
            ks.add(key.tojstring());
        }
        ks.add("loadlayout");
        ks.add("printf");
        ks.add("task");
        ks.add("timer");
        ks.add("call");
        ks.add("thread");
        ks.add("http");
        ks.add("Http");

        ks.add("service");
        ks.add("accessibility");
        ks.add("notification");
        ks.add("saf");

        addPackage("activity", LuaActivity.class);
        addPackage("http", http.class);
        addPackage("Http", Http.class);
        addPackage("service", LuaService.class);
        addPackage("accessibility", LuaAccessibilityService.class);
        addPackage("notification", LuaNotificationListenerService.class);
        addPackage("saf", saf.class);

        ss = new String[ks.size()];
        ks.toArray(ss);
        edit.addNames(ss);
        LinearLayout hList = new LinearLayout(this);
        String[] btn = {"(", ")", "[", "]", "{", "}", "\"", "=", ":", ".", ",", "_", "+", "-", "*", "/", "\\", "%", "#", "^", "$", "?", "&", "|", "<", ">", "~", ";", "'"};
        for (String s : btn) {
            final TextView view = new TextView(this);
            view.setText(s);
            view.setGravity(Gravity.CENTER);
            view.setPadding(0, 0, 0, 0);
            view.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 16);
            view.setLayoutParams(new ViewGroup.LayoutParams(dp(36), dp(32)));
            view.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    edit.paste(view.getText().toString());
                    final ColorStateList tc = view.getTextColors();
                    view.setBackgroundColor(tc.getDefaultColor());
                    view.setTextColor(0);
                    new Handler().postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            view.setBackgroundColor(0);
                            view.setTextColor(tc);
                        }
                    }, 100);
                }
            });
            hList.addView(view);
        }

        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        //edit.setNonPrintingCharVisibility(true);
        //标题栏
        LinearLayout mLayout = new LinearLayout(this);
        mLayout.setOrientation(LinearLayout.VERTICAL);
        mTitle = new TextView(this);
        //mTitle.setGravity(Gravity.CENTER);
        mTitle.setEllipsize(TextUtils.TruncateAt.START);
        mTitle.setSingleLine(true);
        mTitle.setId(12);
        mTitle.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onOptionsItemSelected(v);
                final ColorStateList tc = mTitle.getTextColors();
                mTitle.setBackgroundColor(tc.getDefaultColor());
                mTitle.setTextColor(0);
                new Handler().postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        mTitle.setBackgroundColor(0);
                        mTitle.setTextColor(tc);
                    }
                }, 100);
            }
        });
        ;
        //mLayout.addView();
        mLayout.addView(mTitle, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        LinearLayout fList = new LinearLayout(this);
        fList.addView(createButton("最近", 12));
        fList.addView(createButton("保存", 6));
        fList.addView(createButton("新建文件", 5));

        LinearLayout pList = new LinearLayout(this);
        pList.addView(createButton("新建工程", 11));
        pList.addView(createButton("打开工程", 14));
        pList.addView(createButton("工程属性", 15));
        pList.addView(createButton("打包", 13));
        pList.addView(createButton("导出", 17));

        LinearLayout eList = new LinearLayout(this);
        eList.addView(createButton("格式化", 3));
        eList.addView(createButton("导入分析", 10));
        eList.addView(createButton("导航", 16));
        eList.addView(createButton("粘贴", 23));
        eList.addView(createButton("Java API", 18));
        mMenu = new LinearLayout(this);

        //菜单栏
        LinearLayout mList = new LinearLayout(this);
        mList.addView(createButton("撤消", 1));
        mList.addView(createButton("重做", 2));
        mList.addView(createButton("打开", 4));
        View sBtn = createButton("搜索", 7);
        mList.addView(sBtn);

        mList.addView(createButton("文件", new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mMenu.getChildCount() > 0) {
                    if (mMenu.getChildAt(0).equals(fList)) {
                        mMenu.removeAllViews();
                        return;
                    }
                }
                mMenu.removeAllViews();
                mMenu.addView(fList);
            }
        }));
        mList.addView(createButton("工程", new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mMenu.getChildCount() > 0) {
                    if (mMenu.getChildAt(0).equals(pList)) {
                        mMenu.removeAllViews();
                        return;
                    }
                }
                mMenu.removeAllViews();
                mMenu.addView(pList);
            }
        }));
        mList.addView(createButton("编辑", new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mMenu.getChildCount() > 0) {
                    if (mMenu.getChildAt(0).equals(eList)) {
                        mMenu.removeAllViews();
                        return;
                    }
                }
                mMenu.removeAllViews();
                mMenu.addView(eList);
            }
        }));
        mList.addView(createButton("插件", 19));
       /* mList.addView(createButton("最近", 12));
        mList.addView(createButton("打开", 4));
        mList.addView(createButton("保存", 6));
        mList.addView(createButton("格式化", 3));
        mList.addView(createButton("导入分析", 10));
        mList.addView(createButton("新建文件", 5));
        mList.addView(createButton("新建工程", 11));
        mList.addView(createButton("打开工程", 14));
        mList.addView(createButton("导航", 16));
        mList.addView(createButton("打包", 13));*/
        fList.addView(createButton("日志", 8));
        fList.addView(createButton("帮助", 9));


        HorizontalScrollView mScroll = new HorizontalScrollView(this);
        mScroll.addView(mList);
        mLayout.addView(mScroll, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        layout.addView(mLayout, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        mLayout.addView(mMenu, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));


        LinearLayout sList = new LinearLayout(this);
        @SuppressLint("AppCompatCustomView") EditText sEdit = new EditText(this) {
            @Override
            protected void onTextChanged(CharSequence text, int start, int lengthBefore, int lengthAfter) {
                super.onTextChanged(text, start, lengthBefore, lengthAfter);
                edit.findNext(text.toString());
            }
        };
        sList.addView(sEdit, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT, 1));
        sList.addView(createButton(" ▼ ", 28));
        sList.addView(createButton(" ▲ ", 29));
        sList.setVisibility(View.GONE);
        layout.addView(sList, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        sBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (sList.getVisibility() == View.GONE) {
                    sList.setVisibility(View.VISIBLE);
                    sEdit.setText(edit.getSelectedText());
                } else {
                    sList.setVisibility(View.GONE);
                }
            }
        });
        layout.addView(edit, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT, 1));

        //底部工具栏
        LinearLayout hLayout = new LinearLayout(this);
        hLayout.addView(createButton("运行", 0));

        ListView fxList = new ListView(this);
        ArrayListAdapter<String> adapter = new ArrayListAdapter<String>(this, android.R.layout.simple_list_item_1);
        fxList.setAdapter(adapter);
        fxList.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                DocumentProvider text = edit.getText();
                String[] ls = text.toString().split("\n");
                int offset = 0;
                for (String l : ls) {
                    if (!l.contains("import")) {
                        edit.insert(offset, String.format("import \"%s\"\n", adapter.getItem(position)));
                        break;
                    }
                    offset += l.length() + 1;
                }
            }
        });
        fxList.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {
            @Override
            public boolean onItemLongClick(AdapterView<?> parent, View view, int position, long id) {
                showJavaMethod(adapter.getItem(position));
                return true;
            }
        });
        layout.addView(fxList, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        //剪切板栏
        LinearLayout cList = new LinearLayout(this);
        cList.addView(createButton("全选", 20));
        cList.addView(createButton("剪切", 21));
        cList.addView(createButton("复制", 22));
        cList.addView(createButton("粘贴", 23));
        cList.addView(createButton("◀", 24));
        cList.addView(createButton("▶", 26));
        cList.addView(createButton("▲", 25));
        cList.addView(createButton("▼", 27));
        cList.setVisibility(View.GONE);
        HorizontalScrollView cScroll = new HorizontalScrollView(this);
        cScroll.addView(cList);
        hLayout.addView(cScroll);
        edit.setOnSelectionChangedListener(new OnSelectionChangedListener() {
            @Override
            public void onSelectionChanged(boolean active, int selStart, int selEnd) {
                if (active) {
                    cList.setVisibility(View.VISIBLE);
                } else {
                    cList.setVisibility(View.GONE);
                    fxList.setVisibility(View.GONE);
                    return;
                }

                ArrayList<String> cs = PackageUtil.fix(edit.getSelectedText());
                if (cs != null && !cs.isEmpty()) {
                    adapter.clear();
                    adapter.addAll(cs);
                    fxList.setVisibility(View.VISIBLE);
                    ViewGroup.LayoutParams lp = fxList.getLayoutParams();
                    lp.height = getItemHeight() * Math.min(cs.size(), 3);
                    fxList.setLayoutParams(lp);
                } else {
                    fxList.setVisibility(View.GONE);
                }
            }
        });

        //符号栏
        HorizontalScrollView hScroll = new HorizontalScrollView(this);
        hScroll.addView(hList);
        hLayout.addView(hScroll, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        layout.addView(hLayout, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        // layout.addView(hScroll, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        setContentView(layout);

    }

    private void addPackage(String s, Class<?> aClass) {
        Method[] ms = aClass.getMethods();
        ArrayList<String> vs = new ArrayList<>();
        for (Method m : ms) {
            String n = m.getName();
            if (!vs.contains(n))
                vs.add(n);
        }
        String[] ss = new String[vs.size()];
        vs.toArray(ss);
        edit.addPackage(s, ss);
    }

    public int getItemHeight() {
        if (_h != 0)
            return _h;

        LayoutInflater inflater = (LayoutInflater) getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        TextView item = (TextView) inflater.inflate(android.R.layout.simple_list_item_1, null);
        item.measure(0, 0);
        _h = item.getMeasuredHeight();
        return _h;
    }

    private View createButton(String s, int id) {
        final TextView view = new TextView(this);
        view.setId(id);
        view.setText(s);
        view.setGravity(Gravity.CENTER);
        view.setPadding(dp(8), 0, dp(8), 0);
        view.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 16);
        view.setLayoutParams(new ViewGroup.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, dp(32)));
        view.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onOptionsItemSelected(v);
                final ColorStateList tc = view.getTextColors();
                view.setBackgroundColor(tc.getDefaultColor());
                view.setTextColor(0);
                new Handler().postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        view.setBackgroundColor(0);
                        view.setTextColor(tc);
                        mMenu.removeAllViews();
                    }
                }, 100);
            }
        });
        return view;
    }

    private View createButton(String s, View.OnClickListener listener) {
        final TextView view = new TextView(this);
        view.setText(s);
        view.setGravity(Gravity.CENTER);
        view.setPadding(dp(8), 0, dp(8), 0);
        view.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 16);
        view.setLayoutParams(new ViewGroup.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, dp(32)));
        view.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                listener.onClick(v);
                final ColorStateList tc = view.getTextColors();
                view.setBackgroundColor(tc.getDefaultColor());
                view.setTextColor(0);
                new Handler().postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        view.setBackgroundColor(0);
                        view.setTextColor(tc);
                    }
                }, 100);
            }
        });
        return view;
    }

    private int dp(float n) {
        // TODO: Implement this method
        return (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, n, dm);
    }

    @Override
    protected void onStop() {
        super.onStop();
        save();
    }

    private void save() {
        try {
            if (edit.isEdited()) {
                edit.save();
            }
            JsonUtil.HistoryData c = null;
            for (JsonUtil.HistoryData data : history) {
                if (data.getPath().equals(path)) {
                    c = data;
                    break;
                }
            }
            if (c != null)
                history.remove(c);
            history.add(0, new JsonUtil.HistoryData(path, edit.getCaretPosition()));
            JsonUtil.saveHistoryData(mHistoryPath, history);
        } catch (IOException e) {
            if (BuildConfig.DEBUG)
                e.printStackTrace();
        }
    }

    private void loadConfig() {
        try {
            File path = new File(mDir, "config.json");
            if (!path.exists())
                return;
            InputStream stream = new FileInputStream(path);
            BufferedReader reader = new BufferedReader(new InputStreamReader(stream, "UTF-8"));
            StringBuilder stringBuilder = new StringBuilder(8196);
            String input;
            while ((input = reader.readLine()) != null) {
                stringBuilder.append(input);
            }
            stream.close();
            JSONObject colors = new JSONObject(stringBuilder.toString());
            if (colors.has("dark")) {
                if (colors.optBoolean("dark")) {
                    setTheme(android.R.style.Theme_DeviceDefault_NoActionBar);
                    edit.setDark(true);
                } else {
                    setTheme(android.R.style.Theme_DeviceDefault_Light_NoActionBar);
                    edit.setDark(false);
                }
            }

            if (colors.has("wrap"))
                edit.setWordWrap(colors.optBoolean("wrap"));

            if (colors.has("text"))
                edit.setTextColor(Color.parseColor(colors.optString("text")));
            if (colors.has("highlight"))
                edit.setTextHighlightColor(Color.parseColor(colors.optString("Highlight")));
            if (colors.has("background")) {
                String bg = colors.optString("background");
                if (new File(mDir, bg).exists())
                    edit.setBackground(new LuaBitmapDrawable(this, new File(mDir, bg).getAbsolutePath()));
                else
                    edit.setBackgroundColor(Color.parseColor(bg));
            }

            if (colors.has("line"))
                edit.setLineColor(Color.parseColor(colors.optString("line")));
            if (colors.has("keyword"))
                edit.setKeywordColor(Color.parseColor(colors.optString("keyword")));
            if (colors.has("package"))
                edit.setBasewordColor(Color.parseColor(colors.optString("package")));
            if (colors.has("number"))
                edit.setUserwordColor(Color.parseColor(colors.optString("number")));
            if (colors.has("global"))
                edit.setGlobalColor(Color.parseColor(colors.optString("global")));
            if (colors.has("local"))
                edit.setLocalColor(Color.parseColor(colors.optString("local")));
            if (colors.has("upval"))
                edit.setUpvalColor(Color.parseColor(colors.optString("upval")));
            if (colors.has("comment"))
                edit.setCommentColor(Color.parseColor(colors.optString("comment")));
            if (colors.has("string"))
                edit.setStringColor(Color.parseColor(colors.optString("string")));
            if (colors.has("panel")) {
                colors = colors.getJSONObject("panel");
                if (colors.has("background")) {
                    String bg = colors.optString("background");
                    if (new File(mDir, bg).exists())
                        edit.setPanelBackground(new LuaBitmapDrawable(this, new File(mDir, bg).getAbsolutePath()));
                    else
                        edit.setPanelBackgroundColor(Color.parseColor(bg));
                }
                //    edit.setPanelBackgroundColor(Color.parseColor(colors.optString("background")));
                if (colors.has("text"))
                    edit.setPanelTextColor(Color.parseColor(colors.optString("text")));
            } /*else {
                edit.setPanelTextColor(edit.getColorScheme().getColor(ColorScheme.Colorable.FOREGROUND));
                edit.setPanelBackgroundColor(edit.getColorScheme().getColor(ColorScheme.Colorable.BACKGROUND));
            }*/

        } catch (Exception e) {
            if (BuildConfig.DEBUG)
                e.printStackTrace();
        }
    }


    private void initDir() {
        mDir = new File(Environment.getExternalStorageDirectory(), "LuaJ");
        mRootDir = mDir;
        mProjectsDir = new File(mRootDir, "Projects");
        if (!mDir.exists())
            mDir.mkdirs();
        if (!mProjectsDir.exists())
            mProjectsDir.mkdirs();
    }

    private void checkPermission(String permission) {
        if (checkCallingOrSelfPermission(permission)
                != PackageManager.PERMISSION_GRANTED) {
            permissions.add(permission);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        readFile();
    }

    private void readFile() {
        try {
            path = new File(mDir, "main.lua").getAbsolutePath();
            edit.open(path);
            mName = "main.lua";
            //getActionBar().setSubtitle("main.lua");
            loadLibs();
            mTitle.setText(path.replace(Environment.getExternalStorageDirectory().getAbsolutePath(), ".."));
        } catch (IOException e) {
            CustomToast.show(this, "打开出错：" + e.toString(), Toast.LENGTH_SHORT, false);
            if (BuildConfig.DEBUG)
                e.printStackTrace();
        }
    }

    private boolean readHistory(int i) {
        try {
            path = history.get(i).getPath();
            Log.i("luaj", "readHistory: " + path);
            edit.open(path);
            mName = new File(path).getName();
            mDir = new File(path).getParentFile();
            edit.setSelection(history.get(i).getIdx());
            //getActionBar().setSubtitle(mName);
            loadLibs();
            mTitle.setText(path.replace(Environment.getExternalStorageDirectory().getAbsolutePath(), ".."));
            return true;
        } catch (IOException e) {
            if (BuildConfig.DEBUG)
                e.printStackTrace();
            return false;
        }
    }

    private void readFile(String p) {
        try {
            path = new File(mDir, p).getAbsolutePath();
            edit.open(path);
            mName = p;
            for (JsonUtil.HistoryData data : history) {
                if (data.getPath().equals(path))
                    edit.setSelection(data.getIdx());
            }
            //getActionBar().setSubtitle(p);
            loadLibs();
            mTitle.setText(path.replace(Environment.getExternalStorageDirectory().getAbsolutePath(), ".."));
            save();
        } catch (IOException e) {
            CustomToast.show(this, "打开出错：" + e.toString(), Toast.LENGTH_SHORT, false);
            if (BuildConfig.DEBUG)
                e.printStackTrace();
        }
    }

    private void loadLibs() {
        /** use func.env **/
        Lua.LUA_FUNC_ENV = false;

        /** use first local _ENV **/
        Lua.LUA_LOCAL_ENV = true;

        /** use t.filed **/
        Lua.LUA_JAVA_OO = true;

        /** use if(b){} **/
        Lua.LUA_BLOCK_CURLY = false;

        /** use t[1] **/
        Lua.LUA_JAVA_ARRAY_FIRST_INDEX = false;

        mProjDir = checkProjectDir(mDir);
        LuaDexLoader mLuaDexLoader = new LuaDexLoader(this, mProjDir.getAbsolutePath());
        mLuaDexLoader.loadLibs();
        mGlobals.luajavaLib.classLoaders = mLuaDexLoader.getClassLoaders();
        PackageUtil.load(this, new File(mProjDir, "libs").listFiles());
    }

    private File checkProjectDir(File dir) {
        if (dir == null)
            return mDir;
        if (new File(dir, "main.lua").exists() && new File(dir, "init.lua").exists()) {
            initENV(new File(dir, "init.lua").getAbsolutePath());
            return dir;
        }
        return checkProjectDir(dir.getParentFile());
    }

    private void initENV(String path) {

        try {
            LuaTable env = new LuaTable();
            mGlobals.loadfile(path, env).call();
            LuaValue b = env.get("LUA_JAVA_OO");
            if (b.isboolean())
                Lua.LUA_JAVA_OO = b.toboolean();
            b = env.get("LUA_JAVA_ARRAY_FIRST_INDEX");
            if (b.isboolean())
                Lua.LUA_JAVA_ARRAY_FIRST_INDEX = b.toboolean();
            b = env.get("LUA_FUNC_ENV");
            if (b.isboolean())
                Lua.LUA_FUNC_ENV = b.toboolean();
            b = env.get("LUA_LOCAL_ENV");
            if (b.isboolean())
                Lua.LUA_LOCAL_ENV = b.toboolean();
            b = env.get("LUA_BLOCK_CURLY");
            if (b.isboolean())
                Lua.LUA_BLOCK_CURLY = b.toboolean();
            b = env.get("LUA_JAVA_OO");
            if (b.isboolean())
                Lua.LUA_JAVA_OO = b.toboolean();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void onOptionsItemSelected(View item) {
        switch (item.getId()) {
            case 0:
                try {
                    edit.save();
                    startActivity(new Intent(this, LuaActivity.class).setData(Uri.fromFile(mProjDir)));
                } catch (IOException e) {
                    CustomToast.show(this, "保存出错：" + e.toString(), Toast.LENGTH_SHORT, false);
                    if (BuildConfig.DEBUG)
                        e.printStackTrace();
                }
                break;
            case 1:
                edit.undo();
                break;
            case 2:
                edit.redo();
                break;
            case 3:
                edit.format();
                break;
            case 4:
                save();
                openFile(mDir);
                break;
            case 5:
                save();
                new EditDialog(this, "输入文件名", "", new EditDialog.EditDialogCallback() {
                    @Override
                    public void onCallback(String text) {
                        if (TextUtils.isEmpty(text))
                            return;
                        if (!text.contains("."))
                            text = text + ".lua";
                        readFile(text);
                    }
                }).show();
                break;
            case 6:
                try {
                    edit.save();
                    CustomToast.show(this, "已保存", Toast.LENGTH_SHORT, false);
                } catch (IOException e) {
                    CustomToast.show(this, "保存出错：" + e.toString(), Toast.LENGTH_SHORT, false);
                    if (BuildConfig.DEBUG)
                        e.printStackTrace();
                }
                break;
            case 7:
                edit.search();
                break;
            case 8:
                String[] logs = new String[LuaActivity.logs.size()];
                LuaActivity.logs.toArray(logs);
                AlertDialog dlg1 = new AlertDialog.Builder(this).setTitle("日志")
                        .setItems(logs, null)
                        .setPositiveButton("清空", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                LuaActivity.logs.clear();
                            }
                        })
                        .setNegativeButton("取消", null)
                        .create();
                dlg1.show();
                dlg1.getListView().setFastScrollEnabled(true);
                break;
            case 9:
                new AlertDialog.Builder(this)
                        .setTitle("LuaJ++")
                        .setMessage(load())
                        .setPositiveButton("确定", null)
                        .create()
                        .show();
                break;
            case 10:
                String[] cls = AutoIndent.fix(edit.getText());
                Arrays.sort(cls, new LocaleComparator());
                boolean[] ss = new boolean[cls.length];
                StringBuilder buf = new StringBuilder();
                new AlertDialog.Builder(this)
                        .setTitle("导入")
                        .setMultiChoiceItems(cls, ss, new DialogInterface.OnMultiChoiceClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which, boolean isChecked) {
                                ss[which] = isChecked;
                            }
                        })
                        .setPositiveButton("复制", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                for (int i = 0; i < cls.length; i++) {
                                    if (ss[i])
                                        buf.append("import \"").append(cls[i]).append("\"\n");
                                }
                                ClipboardManager clp = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
                                clp.setText(buf.toString());
                            }
                        })
                        .setNegativeButton("取消", null)
                        .create()
                        .show();
                break;
            case 11:
                save();
                new EditDialog(this, "输入工程名", "", new EditDialog.EditDialogCallback() {
                    @Override
                    public void onCallback(String text) {
                        if (TextUtils.isEmpty(text))
                            return;
                        File d = new File(mProjectsDir, text);
                        if (!d.exists() && !d.mkdirs()) {
                            CustomToast.show(LuaEditorActivity.this, "创建出错", Toast.LENGTH_SHORT, false);
                            return;
                        }
                        mDir = d;
                        initProject(mDir, text);
                        readFile("main.lua");
                    }
                }).show();
                break;
            case 12:
                String[] hs = new String[history.size()];
                for (int i = 0; i < history.size(); i++) {
                    hs[i] = history.get(i).getPath();
                }
                AlertDialog dlg = new AlertDialog.Builder(this).setTitle("最近打开")
                        .setItems(hs, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                save();
                                if (readHistory(which)) {
                                    history.remove(which);
                                    save();
                                } else {
                                    history.remove(which);
                                }
                            }
                        })
                        .setNegativeButton("取消", null)
                        .create();
                dlg.show();
                dlg.getListView().setFastScrollEnabled(true);

                break;
            case 13:
                save();
                break;
            case 14:
                String[] ps = mProjectsDir.list(new FilenameFilter() {
                    @Override
                    public boolean accept(File dir, String name) {
                        return new File(dir, name).isDirectory();
                    }
                });
                Arrays.sort(ps, new LocaleComparator());
                AlertDialog dlg2 = new AlertDialog.Builder(this)
                        .setTitle("打开工程")
                        .setItems(ps, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                mDir = new File(mProjectsDir, ps[which]);
                                initProject(mDir, ps[which]);
                                readFile("main.lua");
                            }
                        })
                        .setNegativeButton("取消", null)
                        .create();
                dlg2.show();
                dlg2.getListView().setFastScrollEnabled(true);
                break;
            case 15:
                save();
                mDir = mProjDir;
                readFile("init.lua");
                break;
            case 16:
                ArrayList<LuaParser.Var> fs = LuaParser.getFuncList();
                String[] fn = new String[fs.size()];
                for (int i = 0; i < fs.size(); i++) {
                    LuaParser.Var f = fs.get(i);
                    fn[i] = f.name + f.type + " " + f.idx;
                }
                AlertDialog dlg3 = new AlertDialog.Builder(this)
                        .setTitle("导航")
                        .setItems(fn, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                edit.gotoLine(fs.get(which).idx);
                            }
                        })
                        .setPositiveButton("确定", null)
                        .create();
                dlg3.show();
                dlg3.getListView().setFastScrollEnabled(true);
                break;
            case 17:
                LuaApplication app = LuaApplication.getInstance();
                String name = LuaUtil.getFileName(mProjDir.getName(), ".zip");
                if (LuaUtil.zip(mProjDir.getAbsolutePath(), app.getLuaExtDir("backup"), name)) {
                    CustomToast.show(this, "备份完成" + app.getLuaExtPath("backup", name), Toast.LENGTH_SHORT, false);
                    shareFile(app.getLuaExtPath("backup", name));
                } else {
                    CustomToast.show(this, "备份失败", Toast.LENGTH_SHORT, false);
                }
                break;
            case 18:
                showJavaAPI();
                break;
            case 19:
                showPlugins();
                break;
            case 20:
                edit.selectAll();
                break;
            case 21:
                edit.cut();
                break;
            case 22:
                edit.copy();
                break;
            case 23:
                edit.paste();
                break;
            case 24:
                edit.moveCaretLeft();
                break;
            case 25:
                edit.moveCaretUp();
                break;
            case 26:
                edit.moveCaretRight();
                break;
            case 27:
                edit.moveCaretDown();
                break;
            case 28:
                edit.findNext();
                break;
            case 29:
                edit.findBack();
                break;

        }
    }

    private void showPlugins() {
        File pd = new File(mRootDir, "Plugins");
        if (!pd.exists())
            pd.mkdirs();
        String[] ps = pd.list(new FilenameFilter() {
            @Override
            public boolean accept(File dir, String name) {
                return new File(new File(dir, name), "main.lua").exists();
            }
        });
        Arrays.sort(ps, new LocaleComparator());
        AlertDialog dlg = new AlertDialog.Builder(this)
                .setTitle("Plugins")
                .setItems(ps, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        save();
                        startActivity(new Intent(LuaEditorActivity.this, LuaActivity.class)
                                .setData(Uri.fromFile(new File(pd, ps[which])))
                                .putExtra(LuaActivity.ARG, new Object[]{
                                        mDir.getAbsolutePath(),
                                        path,
                                        edit.getSelectedText()
                                }));
                    }
                })
                .setNegativeButton(android.R.string.cancel, null)
                .create();
        dlg.getListView().setFastScrollEnabled(true);
        dlg.show();

    }

    @Override
    protected void onStart() {
        super.onStart();
        try {
            String s = new String(LuaUtil.readAll(path));
            if (!s.trim().equals(edit.getText().toString().trim())) {
                new AlertDialog.Builder(this)
                        .setTitle("文件已更改")
                        .setMessage("是否重新加载")
                        .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                edit.setText(s, true);
                            }
                        })
                        .setNegativeButton(android.R.string.cancel, null)
                        .create()
                        .show();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void showJavaAPI() {
        Collections.sort(PackageUtil.getClassList());
        ArrayListAdapter<String> adapter = new ArrayListAdapter<String>(this, PackageUtil.getClassList());
        AlertDialog dlg = new AlertDialog.Builder(this, android.R.style.Theme_Material)
                .setTitle("Java API")
                .setAdapter(adapter, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        showJavaMethod(adapter.getItem(which));
                    }
                })
                .create();
        dlg.show();
        dlg.getListView().setFastScrollEnabled(true);
    }

    private void showJavaMethod(String item) {
        ArrayList<String> list = new ArrayList<>();
        ArrayList<String> list2 = new ArrayList<>();
        try {
            Class<?> cls = Class.forName(item);
            list.add("字段");
            list2.add("");
            Field[] fs = cls.getFields();
            for (Field f : fs) {
                list.add(f.getType().getSimpleName() + " " + f.getName());
                list2.add(f.getName());
            }
            list.add("方法");
            list2.add("");
            Method[] ms = cls.getMethods();
            for (Method m : ms) {
                list.add(m.getReturnType().getSimpleName() + " " + m.getName() + toString(m.getParameterTypes()));
                list2.add(m.getName());
            }
            ArrayListAdapter<String> adapter = new ArrayListAdapter<>(this, list);
            AlertDialog dlg = new AlertDialog.Builder(this, android.R.style.Theme_Material)
                    .setTitle(item)
                    .setAdapter(adapter, null)
                    .create();
            dlg.getListView().setFastScrollEnabled(true);
            dlg.getListView().setOnItemClickListener(new AdapterView.OnItemClickListener() {
                @Override
                public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                    copy(list2.get(position));
                    CustomToast.show(LuaEditorActivity.this, "已复制", Toast.LENGTH_SHORT, false);
                }
            });
            dlg.show();
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
            if (!item.contains("."))
                return;
            if (item.contains("$"))
                return;
            showJavaMethod(item.replaceAll("\\.([a-zA-Z0-9]*)$", "\\$$1"));
        }
    }

    private void copy(String n) {
        ClipboardManager cm = (ClipboardManager) getSystemService(CLIPBOARD_SERVICE);
        if (cm != null) {
            cm.setPrimaryClip(ClipData.newPlainText("label", n));
        }
    }

    public static String toString(Class<?>[] a) {
        if (a == null)
            return "null";

        int iMax = a.length - 1;
        if (iMax == -1)
            return "()";

        StringBuilder b = new StringBuilder();
        b.append('(');
        for (int i = 0; ; i++) {
            b.append(a[i].getSimpleName());
            if (i == iMax)
                return b.append(')').toString();
            b.append(", ");
        }
    }

    public void shareFile(String path) {
        Intent share = new Intent(Intent.ACTION_SEND);
        File file = new File(path);
        share.setType("*/*");
        share.setFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        share.putExtra(Intent.EXTRA_STREAM, getUriForFile(file));
        startActivity(Intent.createChooser(share, file.getName()).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
    }

    private void initProject(File mDir, String text) {
        if (!new File(mDir, "init.lua").exists()) {
            LuaUtil.save(new File(mDir, "init.lua").getAbsolutePath(), String.format("app_name=\"%s\"\n" +
                    "ver_name=\"1.0\"\n" +
                    "ver_code=\"1\"\n" +
                    "package_name=\"org.luaj.demo\"\n" +
                    "theme=\"Theme_DeviceDefault\"\n" +
                    "developer=\"\"\n" +
                    "description=\"\"\n" +
                    "debug_mode=true\n" +
                    "user_permission={\n" +
                    "  \"INTERNET\",\n" +
                    "  \"WRITE_EXTERNAL_STORAGE\"\n" +
                    "}", text));
            LuaUtil.save(new File(mDir, "res/string/init.lua").getAbsolutePath(), String.format("appname=\"%s\"\n", text));
            LuaUtil.save(new File(mDir, "res/layout/main.lua").getAbsolutePath(), "{\n" +
                    "  import \"android.widget.*\";\n" +
                    "  LinearLayout,\n" +
                    "  orientation=\"vertical\",\n" +
                    "  layout_width=\"fill\",\n" +
                    "  layout_height=\"fill\",\n" +
                    "  {\n" +
                    "    TextView,\n" +
                    "    id=\"tv\",\n" +
                    "    text=\"Hello LuaJ++\",\n" +
                    "    layout_width=\"fill\",\n" +
                    "  },\n" +
                    "}");
            new File(mDir, "res/drawable").mkdirs();
            if (!new File(mDir, "main.lua").exists()) {
                LuaUtil.save(new File(mDir, "main.lua").getAbsolutePath(), "import \"android.app.*\"\n" +
                        "import \"android.widget.*\"\n" +
                        "import \"com.androlua.*\"\n" +
                        "import \"java.lang.*\"\n" +
                        "import \"java.util.*\"\n" +
                        "import \"res\"\n" +
                        "--activity.setTitle(\"LuaJ++\")\n" +
                        "--activity.setTheme(android.R.style.Theme_DeviceDefault)\n" +
                        "activity.setContentView(res.layout.main)");
            }
        }
    }

    private void openFile(final File dir) {
        File[] ls = dir.listFiles();
        if (ls == null)
            return;
        ArrayList<String> ds = new ArrayList<>();
        ArrayList<String> fs = new ArrayList<>();
        for (File l : ls) {
            if (l.isDirectory())
                ds.add(l.getName());
            else
                fs.add(l.getName());
        }
        Collections.sort(ds, new LocaleComparator());
        Collections.sort(fs, new LocaleComparator());
        ds.add(0, "..");
        ds.addAll(fs);
        String[] list = new String[ds.size()];
        ds.toArray(list);
        AlertDialog dlg = new AlertDialog.Builder(this)
                .setTitle("打开 " + dir.getAbsolutePath())
                .setItems(list, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        Log.i("luaj", "onClick: " + list[which]);
                        if (which == 0) {
                            if (dir.getParentFile() != null)
                                openFile(dir.getParentFile());
                        } else if (new File(dir, list[which]).isDirectory()) {
                            openFile(new File(dir, list[which]));
                        } else {
                            LuaEditorActivity.this.mDir = dir;
                            readFile(list[which]);
                        }
                    }
                })
                .setNegativeButton("取消", null)
                .setPositiveButton("新建文件", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        new EditDialog(LuaEditorActivity.this, "输入文件名", "", new EditDialog.EditDialogCallback() {
                            @Override
                            public void onCallback(String text) {
                                if (TextUtils.isEmpty(text))
                                    return;
                                if (!text.contains("."))
                                    text = text + ".lua";
                                try {
                                    new File(dir, text).createNewFile();
                                } catch (IOException e) {
                                    e.printStackTrace();
                                }
                                openFile(dir);
                            }
                        }).show();
                    }
                })
                .setNeutralButton("新建文件夹", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        new EditDialog(LuaEditorActivity.this, "输入文件夹名", "", new EditDialog.EditDialogCallback() {
                            @Override
                            public void onCallback(String text) {
                                if (TextUtils.isEmpty(text))
                                    return;
                                //noinspection ResultOfMethodCallIgnored
                                new File(dir, text).mkdirs();
                                openFile(dir);
                            }
                        }).show();
                    }
                })
                .create();
        dlg.show();
        dlg.getListView().setFastScrollEnabled(true);
        dlg.getListView().setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {
            @Override
            public boolean onItemLongClick(AdapterView<?> parent, View view, int position, long id) {
                dlg.dismiss();
                new AlertDialog.Builder(LuaEditorActivity.this)
                        .setItems(new String[]{
                                "删除",
                                "重命名",
                                "取消"
                        }, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                switch (which) {
                                    case 0:
                                        new File(dir, list[position]).delete();
                                        if (list[position].equals(mName))
                                            readFile();
                                        openFile(dir);
                                        break;
                                    case 1:
                                        new EditDialog(LuaEditorActivity.this, "输入文件名", list[position], new EditDialog.EditDialogCallback() {
                                            @Override
                                            public void onCallback(String text) {
                                                if (TextUtils.isEmpty(text))
                                                    return;
                                                new File(dir, list[position]).renameTo(new File(dir, text));
                                                if (list[position].equals(mName))
                                                    readFile(list[position]);
                                                openFile(dir);
                                            }
                                        }).show();
                                        break;
                                }
                                dialog.dismiss();
                            }
                        })
                        .create()
                        .show();

                return true;
            }
        });
    }

    public String load() {
        if (mHelp != null)
            return mHelp;
        try {
            InputStream stream = getAssets().open("help.txt");
            BufferedReader reader = new BufferedReader(new InputStreamReader(stream, "UTF-8"));
            StringBuilder stringBuilder = new StringBuilder(8196);
            String input;
            while ((input = reader.readLine()) != null) {
                stringBuilder.append(input).append("\n");
            }
            stream.close();
            mHelp = stringBuilder.toString();
        } catch (Exception e) {
            if (BuildConfig.DEBUG)
                e.printStackTrace();
        }
        //Log.i("luaj", "load filter: " + packages);
        return mHelp;
    }
    private String getType(File file) {
        int lastDot = file.getName().lastIndexOf(46);
        if (lastDot >= 0) {
            String extension = file.getName().substring(lastDot + 1);
            String mime = MimeTypeMap.getSingleton().getMimeTypeFromExtension(extension);
            if (mime != null) {
                return mime;
            }
        }
        return "application/octet-stream";
    }

    public Uri getUriForFile(File path) {
        return FileProvider.getUriForFile(this, getPackageName(), path);
    }

    public void installApk(String path) {
        Intent share = new Intent(Intent.ACTION_VIEW);
        File file = new File(path);
        share.setFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        share.setDataAndType(getUriForFile(file), getType(file));
        share.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(share);
    }

    private void addZip(ZipOutputStream zip, File dir, String root) {
        Log.i("luaj", "addZip: " + root + ";" + dir);
        if (dir.getName().startsWith("."))
            return;
        if (mDlg != null) {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    mDlg.setMessage(dir.getName());
                }
            });
        }
        String name = root + "/" + dir.getName();
        if (name.endsWith(".apk"))
            return;
        if (dir.isDirectory()) {
            File[] fs = dir.listFiles();
            for (File f : fs) {
                addZip(zip, f, name);
            }
        } else {
            try {
                zip.putNextEntry(new ZipEntry(name));
            } catch (Exception e) {
                throw new LuaError(e);
            }

            if (name.endsWith(".lua")) {
                LuaValue args = mGlobals.loadfile(dir.getAbsolutePath());
                LuaValue f = args.checkfunction(1);
                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                try {
                    DumpState.dump(((LuaClosure) f).p, baos, true);
                    zip.write(baos.toByteArray());
                    zip.flush();
                } catch (Exception e) {
                    throw new LuaError(e);
                }
            } else {
                try {
                    /*byte[] b = LuaUtil.readAll(dir.getAbsolutePath());
                    zip.write(b, 0, b.length);
                    zip.flush();*/
                    FileInputStream in = new FileInputStream(dir);
                    LuaUtil.copyFile(in, zip);
                    in.close();
                } catch (Exception e) {
                    throw new LuaError(e);
                }
            }
        }
    }

    @Override
    public InputStream findResource(String name) {
        try {
            if (new File(name).exists())
                return new FileInputStream(name);
        } catch (Exception e) {
            try {
                return new FileInputStream(new File(mProjDir, name));
            } catch (Exception e2) {
                return null;
            }
        }
        return null;
    }

    @Override
    public String findFile(String filename) {
        if (filename.startsWith("/"))
            return filename;
        return new File(mDir, filename).getAbsolutePath();
    }
}
