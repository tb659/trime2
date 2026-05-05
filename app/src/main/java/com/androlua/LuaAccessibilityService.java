package com.androlua;

import android.accessibilityservice.AccessibilityService;
import android.accessibilityservice.AccessibilityServiceInfo;
import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.app.WallpaperManager;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.ContentResolver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.FileProvider;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.database.Cursor;
import android.graphics.Rect;
import android.graphics.Region;
import android.media.AudioManager;
import android.media.session.MediaSession;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Parcelable;
import android.os.StrictMode;
import android.preference.PreferenceManager;
import android.provider.MediaStore;
import android.provider.Settings;
import android.speech.RecognitionListener;
import android.speech.RecognizerIntent;
import android.speech.SpeechRecognizer;
import android.speech.tts.TextToSpeech;
import android.speech.tts.UtteranceProgressListener;
import android.text.TextUtils;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.KeyEvent;
import android.view.WindowManager;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityNodeInfo;
import android.webkit.MimeTypeMap;
import android.widget.ArrayListAdapter;
import android.widget.Toast;

import org.luaj.Globals;
import org.luaj.LuaTable;
import org.luaj.LuaValue;
import com.osfans.trime.BuildConfig;
import com.osfans.trime.util.CustomToast;
import org.luaj.android.file;
import org.luaj.android.http;
import org.luaj.android.json;
import org.luaj.android.loadlayout;
import org.luaj.android.print;
import org.luaj.android.printf;
import org.luaj.android.res;
import org.luaj.android.task;
import org.luaj.android.thread;
import org.luaj.android.timer;
import org.luaj.lib.ResourceFinder;
import org.luaj.lib.jse.CoerceJavaToLua;
import org.luaj.lib.jse.JavaPackage;
import org.luaj.lib.jse.JsePlatform;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static android.accessibilityservice.AccessibilityServiceInfo.FLAG_REQUEST_TOUCH_EXPLORATION_MODE;
import static android.speech.tts.TextToSpeech.Engine.KEY_PARAM_STREAM;
import static android.speech.tts.TextToSpeech.Engine.KEY_PARAM_UTTERANCE_ID;
import static android.speech.tts.TextToSpeech.Engine.KEY_PARAM_VOLUME;
import static android.view.accessibility.AccessibilityNodeInfo.ACTION_ARGUMENT_EXTEND_SELECTION_BOOLEAN;

public class LuaAccessibilityService extends AccessibilityService implements ResourceFinder, LuaContext, LuaBroadcastReceiver.OnReceiveListener {
    private static String mLuaDir;
    private TextToSpeech mTts;
    private SpeechRecognizer mSpeech;
    private boolean mOk;
    private static LuaAccessibilityService sInstance = null;
    private int mScreenDensity;

    public static LuaAccessibilityService getInstance() {
        return sInstance;
    }
    public static void setEnabled(Context context){
        ComponentName networkActivity = new ComponentName(context.getApplicationContext(), LuaAccessibilityService.class);
        PackageManager packageManager = context.getApplicationContext().getPackageManager();
        int i = packageManager.getComponentEnabledSetting(networkActivity);
        Log.d("luaj", "LuaAccessibilityService: " + i);
        packageManager.setComponentEnabledSetting(networkActivity, PackageManager.COMPONENT_ENABLED_STATE_ENABLED, PackageManager.DONT_KILL_APP);
        context.startActivity( new Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS));
    }

    public static void enabled(Context context){
        setEnabled(context);
    }

    public static void disabled(Context context){
        ComponentName networkActivity = new ComponentName(context.getApplicationContext(), LuaAccessibilityService.class);
        PackageManager packageManager = context.getApplicationContext().getPackageManager();
        packageManager.setComponentEnabledSetting(networkActivity, PackageManager.COMPONENT_ENABLED_STATE_DISABLED, PackageManager.DONT_KILL_APP);
        int i = packageManager.getComponentEnabledSetting(networkActivity);
        Log.d("luaj", "LuaAccessibilityService: " + i);
        if(sInstance!=null)
            sInstance.stopSelf();
    }
    public static void setLuaDir(String path){
        mLuaDir=path;
    }
    @CallLuaFunction
    @Override
    public void onCreate() {
        /*MediaSession mMediaSession = new MediaSession(this, "mbr");
        mMediaSession.setFlags(MediaSession.FLAG_HANDLES_MEDIA_BUTTONS |
                MediaSession.FLAG_HANDLES_TRANSPORT_CONTROLS);
        mMediaSession.setCallback(new MediaSession.Callback() {
            @Override
            public boolean onMediaButtonEvent(Intent intent) {
                //在这里就可以接收到（线控、蓝牙耳机的按键事件了）
                Parcelable key = intent.getParcelableExtra(Intent.EXTRA_KEY_EVENT);
                //获取按下的按键实现自己对应功能
                //返回true表示不让别的程序继续处理这个广播
                return true;
            }
        });
        if (!mMediaSession.isActive()) {
            mMediaSession.setActive(true);
        }*/




    StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder().permitAll().build();
        StrictMode.setThreadPolicy(policy);
        setTheme(android.R.style.Theme_DeviceDefault);
        super.onCreate();
        sInstance = this;
        reStart();
        ContentResolver mResolver = getContentResolver();
        String mDefEngine = Settings.Secure.getString(mResolver, Settings.Secure.TTS_DEFAULT_SYNTH);
        setTTSEngine(mDefEngine);

        mSpeech = SpeechRecognizer.createSpeechRecognizer(this);
        mSpeech.setRecognitionListener(new RecognitionListener() {
            @CallLuaFunction
            @Override
            public void onReadyForSpeech(Bundle params) {
                runFunc("onASRReady", params);
            }

            @CallLuaFunction
            @Override
            public void onBeginningOfSpeech() {
                runFunc("onASRStart");
            }

            @Override
            public void onRmsChanged(float rmsdB) {

            }

            @Override
            public void onBufferReceived(byte[] buffer) {

            }

            @CallLuaFunction
            @Override
            public void onEndOfSpeech() {
                runFunc("onASREnd");
            }

            @CallLuaFunction
            @Override
            public void onError(int error) {
                runFunc("onASRError", error);
            }

            @CallLuaFunction
            @Override
            public void onResults(Bundle results) {
                ArrayList<String> matches = results.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION);
                runFunc("onASRDone", matches);
            }

            @Override
            public void onPartialResults(Bundle partialResults) {

            }

            @Override
            public void onEvent(int eventType, Bundle params) {

            }
        });

    }

    private void setTTSEngine(String engine) {
        if (mTts != null)
            mTts.shutdown();
        mTts = new TextToSpeech(this, new TextToSpeech.OnInitListener() {
            @Override
            public void onInit(int status) {
                Log.i("luaj", "onCreate:onInit " + status);
            }
        });
        mTts.setOnUtteranceProgressListener(new UtteranceProgressListener() {
            @CallLuaFunction
            @Override
            public void onStart(String utteranceId) {
                runFunc("onTTSStart", utteranceId);
            }

            @CallLuaFunction
            @Override
            public void onDone(String utteranceId) {
                runFunc("onTTSDone", utteranceId);
            }

            @CallLuaFunction
            @Override
            public void onError(String utteranceId) {
                runFunc("onTTSError", utteranceId);
            }
        });
    }

    private String getCurrentEngine(TextToSpeech tts) {
        try {
            Method m = TextToSpeech.class.getMethod("getCurrentEngine");
            m.setAccessible(true);
            Object r = m.invoke(tts);
            if (r != null)
                return r.toString();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    @CallLuaFunction
    @Override
    protected void onServiceConnected() {
        super.onServiceConnected();
        runFunc("onServiceConnected");
    }

    @CallLuaFunction
    @Override
    protected boolean onGesture(int gestureId) {
        switch (gestureId) {
            case GESTURE_SWIPE_UP:
                if (runBooleanFunc("onUp"))
                    return true;
            case GESTURE_SWIPE_DOWN:
                if (runBooleanFunc("onDown"))
                    return true;
            case GESTURE_SWIPE_LEFT:
                if (runBooleanFunc("onLeft"))
                    break;
            case GESTURE_SWIPE_RIGHT:
                if (runBooleanFunc("onRight"))
                    return true;

            case GESTURE_SWIPE_LEFT_AND_RIGHT:
                if (runBooleanFunc("onLeftRight"))
                    return true;
            case GESTURE_SWIPE_RIGHT_AND_LEFT:
                if (runBooleanFunc("onRightLeft"))
                    return true;
            case GESTURE_SWIPE_UP_AND_DOWN:
                if (runBooleanFunc("onUpDown"))
                    return true;
            case GESTURE_SWIPE_DOWN_AND_UP:
                if (runBooleanFunc("onDownUp"))
                    return true;
            case GESTURE_SWIPE_LEFT_AND_UP:
                if (runBooleanFunc("onLeftUp"))
                    return true;
            case GESTURE_SWIPE_LEFT_AND_DOWN:
                if (runBooleanFunc("onLeftDown"))
                    return true;
            case GESTURE_SWIPE_RIGHT_AND_UP:
                if (runBooleanFunc("onRightUp"))
                    return true;
            case GESTURE_SWIPE_RIGHT_AND_DOWN:
                if (runBooleanFunc("onRightDown"))
                    return true;
            case GESTURE_SWIPE_UP_AND_LEFT:
                if (runBooleanFunc("onUpLeft"))
                    return true;
            case GESTURE_SWIPE_UP_AND_RIGHT:
                if (runBooleanFunc("onUpRight"))
                    return true;
            case GESTURE_SWIPE_DOWN_AND_LEFT:
                if (runBooleanFunc("onDownLeft"))
                    return true;
            case GESTURE_SWIPE_DOWN_AND_RIGHT:
                if (runBooleanFunc("onDownRight"))
                    return true;
        }
        return runBooleanFunc("onGesture", gestureId);
    }

    @CallLuaFunction
    @Override
    protected boolean onKeyEvent(KeyEvent event) {
        return runBooleanFunc("onKeyEvent", event);
    }

    @CallLuaFunction
    @Override
    public void onAccessibilityEvent(AccessibilityEvent event) {
        /*if (!mOk) {
            if (event.getEventType() == AccessibilityEvent.TYPE_VIEW_HOVER_ENTER) {
                onHoverEnter(event);
            } else {
                speak(getEventText(event));
            }
            return;
        }*/
        if (runBooleanFunc("onAccessibilityEvent", event))
            return;

        switch (event.getEventType()) {
            case AccessibilityEvent.TYPE_VIEW_CLICKED:
                runFunc("onViewClicked", event);
                break;
            case AccessibilityEvent.TYPE_VIEW_LONG_CLICKED:
                runFunc("onViewLongClicked", event);
                break;
            case AccessibilityEvent.TYPE_VIEW_SELECTED:
                runFunc("onViewSelected", event);
                break;
            case AccessibilityEvent.TYPE_VIEW_FOCUSED:
                runFunc("onViewFocused", event);
                break;
            case AccessibilityEvent.TYPE_VIEW_TEXT_CHANGED:
                runFunc("onViewTextChanged", event);
                break;
            case AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED:
                runFunc("onWindowStateChanged", event);
                break;
            case AccessibilityEvent.TYPE_NOTIFICATION_STATE_CHANGED:
                runFunc("onNotificationStateChanged", event);
                break;
            case AccessibilityEvent.TYPE_VIEW_HOVER_ENTER:
                runFunc("onViewHoverEnter", event);
                //onHoverEnter(event);
                break;
            case AccessibilityEvent.TYPE_VIEW_HOVER_EXIT:
                runFunc("onViewHoverExit", event);
                break;
            case AccessibilityEvent.TYPE_TOUCH_EXPLORATION_GESTURE_START:
                runFunc("onTouchExplorationGestureStart", event);
                break;
            case AccessibilityEvent.TYPE_TOUCH_EXPLORATION_GESTURE_END:
                runFunc("onTouchExplorationGestureEnd", event);
                break;
            case AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED:
                runFunc("onWindowContentChanged", event);
                break;
            case AccessibilityEvent.TYPE_VIEW_SCROLLED:
                runFunc("onViewScrolled", event);
                break;
            case AccessibilityEvent.TYPE_VIEW_TEXT_SELECTION_CHANGED:
                runFunc("onViewTextSelectionChanged", event);
                break;
            case AccessibilityEvent.TYPE_ANNOUNCEMENT:
                runFunc("onAnnouncement", event);
                break;
            case AccessibilityEvent.TYPE_VIEW_ACCESSIBILITY_FOCUSED:
                runFunc("onViewAccessibilityFocused", event);
                break;
            case AccessibilityEvent.TYPE_VIEW_ACCESSIBILITY_FOCUS_CLEARED:
                runFunc("onViewAccessibilityFocusCleared", event);
                break;
            case AccessibilityEvent.TYPE_VIEW_TEXT_TRAVERSED_AT_MOVEMENT_GRANULARITY:
                runFunc("onViewTextTraversedAtMovementGranularity", event);
                break;
            case AccessibilityEvent.TYPE_GESTURE_DETECTION_START:
                runFunc("onGestureDetectionStart", event);
                break;
            case AccessibilityEvent.TYPE_GESTURE_DETECTION_END:
                runFunc("onGestureDetectionEnd", event);
                break;
            case AccessibilityEvent.TYPE_TOUCH_INTERACTION_START:
                runFunc("onTouchInteractionStart", event);
                break;
            case AccessibilityEvent.TYPE_TOUCH_INTERACTION_END:
                runFunc("onTouchInteractionEnd", event);
                break;
            case AccessibilityEvent.TYPE_WINDOWS_CHANGED:
                runFunc("onWindowsChanged", event);
                break;
            case AccessibilityEvent.TYPE_VIEW_CONTEXT_CLICKED:
                runFunc("onViewContextClicked", event);
                break;
            case AccessibilityEvent.TYPE_ASSIST_READING_CONTEXT:
                runFunc("onAssistReadingContext", event);
                break;
        }
    }

    private void onHoverEnter(AccessibilityEvent event) {
        AccessibilityNodeInfo source = event.getSource();
        accessibilityFocus(source);
        String text = getNodeInfoText(source);
        speak(text);
    }

    public String getNodeInfoText(AccessibilityNodeInfo source) {
        CharSequence cd = source.getContentDescription();
        if (!TextUtils.isEmpty(cd)) {
            return cd.toString();
        }
        CharSequence text = source.getText();
        if (!TextUtils.isEmpty(text)) {
            return text.toString();
        }
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            CharSequence hint = source.getHintText();
            if (!TextUtils.isEmpty(hint)) {
                return hint.toString();
            }
        }
        return null;
    }


    public String getEventText(AccessibilityEvent source) {
        CharSequence cd = source.getContentDescription();
        if (!TextUtils.isEmpty(cd)) {
            return cd.toString();
        }
        List<CharSequence> text = source.getText();
        if (!text.isEmpty()) {
            for (CharSequence c : text) {
                if (c != null)
                    return c.toString();
            }
        }
        return null;
    }

    public String getText(AccessibilityNodeInfo source) {
        return getNodeInfoText(source);
    }

    public String getText(AccessibilityEvent source) {
        return getEventText(source);
    }

    public void setTouchMode(boolean enable) {
        AccessibilityServiceInfo info = getServiceInfo();
        if (info == null)
            return;
        if (enable) {
            info.flags |= FLAG_REQUEST_TOUCH_EXPLORATION_MODE;
        } else {
            info.flags &= ~FLAG_REQUEST_TOUCH_EXPLORATION_MODE;
        }
        setServiceInfo(info);
    }

    @Override
    public void onInterrupt() {

    }

    public void speak(String text) {
        if (TextUtils.isEmpty(text))
            return;
        Bundle bundle = new Bundle();
        int ret = mTts.speak(text, TextToSpeech.QUEUE_FLUSH, bundle, "");
    }

    public void stop() {
        mTts.stop();
    }

    public boolean isSpeaking() {
        return mTts.isSpeaking();
    }

    public void startListening() {
        if (mSpeech == null) {
            return;
        }
        Intent recognizerIntent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        recognizerIntent.putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1);
        mSpeech.startListening(recognizerIntent);
    }

    public void stopListening() {
        if (mSpeech == null) {
            return;
        }
        mSpeech.stopListening();
    }

    public void toHome() {
        performGlobalAction(AccessibilityService.GLOBAL_ACTION_HOME);
    }

    public void toSplitScreen() {
        performGlobalAction(AccessibilityService.GLOBAL_ACTION_TOGGLE_SPLIT_SCREEN);
    }

    public void toRecents() {
        performGlobalAction(AccessibilityService.GLOBAL_ACTION_RECENTS);
    }


    public void toNotifications() {
        performGlobalAction(AccessibilityService.GLOBAL_ACTION_NOTIFICATIONS);
    }

    public boolean focus(AccessibilityNodeInfo node) {
        if (node == null)
            return false;
        return node.performAction(AccessibilityNodeInfo.ACTION_FOCUS);
    }

    public boolean clearFocus(AccessibilityNodeInfo node) {
        if (node == null)
            return false;
        return node.performAction(AccessibilityNodeInfo.ACTION_CLEAR_FOCUS);
    }

    public boolean select(AccessibilityNodeInfo node) {
        if (node == null)
            return false;
        return node.performAction(AccessibilityNodeInfo.ACTION_SELECT);
    }

    public boolean clearSelection(AccessibilityNodeInfo node) {
        if (node == null)
            return false;
        return node.performAction(AccessibilityNodeInfo.ACTION_CLEAR_SELECTION);
    }

    public boolean click(AccessibilityNodeInfo node) {
        if (node == null)
            return false;
        return node.performAction(AccessibilityNodeInfo.ACTION_CLICK);
    }

    public boolean longClick(AccessibilityNodeInfo node) {
        if (node == null)
            return false;
        return node.performAction(AccessibilityNodeInfo.ACTION_LONG_CLICK);
    }

    public boolean accessibilityFocus(AccessibilityNodeInfo node) {
        if (node == null)
            return false;
        return node.performAction(AccessibilityNodeInfo.ACTION_ACCESSIBILITY_FOCUS);
    }

    public boolean clearAccessibilityFocus(AccessibilityNodeInfo node) {
        if (node == null)
            return false;
        return node.performAction(AccessibilityNodeInfo.ACTION_CLEAR_ACCESSIBILITY_FOCUS);
    }

    public boolean nextAtMovementGranularity(AccessibilityNodeInfo node, int type) {
        if (node == null)
            return false;
        Bundle args = new Bundle();
        args.putInt(AccessibilityNodeInfo.ACTION_ARGUMENT_MOVEMENT_GRANULARITY_INT,
                type);
        args.putBoolean(ACTION_ARGUMENT_EXTEND_SELECTION_BOOLEAN,
                false);
        return node.performAction(AccessibilityNodeInfo.ACTION_NEXT_AT_MOVEMENT_GRANULARITY, args);
    }

    public boolean previousAtMovementGranularity(AccessibilityNodeInfo node, int type) {
        if (node == null)
            return false;
        Bundle args = new Bundle();
        args.putInt(AccessibilityNodeInfo.ACTION_ARGUMENT_MOVEMENT_GRANULARITY_INT,
                type);
        args.putBoolean(ACTION_ARGUMENT_EXTEND_SELECTION_BOOLEAN,
                false);
        return node.performAction(AccessibilityNodeInfo.ACTION_PREVIOUS_AT_MOVEMENT_GRANULARITY, args);
    }

    public boolean nextChar(AccessibilityNodeInfo node) {
        return nextAtMovementGranularity(node, AccessibilityNodeInfo.MOVEMENT_GRANULARITY_CHARACTER);
    }

    public boolean nextWord(AccessibilityNodeInfo node) {
        return nextAtMovementGranularity(node, AccessibilityNodeInfo.MOVEMENT_GRANULARITY_WORD);
    }

    public boolean nextLine(AccessibilityNodeInfo node) {
        return nextAtMovementGranularity(node, AccessibilityNodeInfo.MOVEMENT_GRANULARITY_LINE);
    }

    public boolean nextParagraph(AccessibilityNodeInfo node) {
        return nextAtMovementGranularity(node, AccessibilityNodeInfo.MOVEMENT_GRANULARITY_PARAGRAPH);
    }

    public boolean nextPage(AccessibilityNodeInfo node) {
        return nextAtMovementGranularity(node, AccessibilityNodeInfo.MOVEMENT_GRANULARITY_PAGE);
    }

    public boolean previousChar(AccessibilityNodeInfo node) {
        return previousAtMovementGranularity(node, AccessibilityNodeInfo.MOVEMENT_GRANULARITY_CHARACTER);
    }

    public boolean previousWord(AccessibilityNodeInfo node) {
        return previousAtMovementGranularity(node, AccessibilityNodeInfo.MOVEMENT_GRANULARITY_WORD);
    }

    public boolean previousLine(AccessibilityNodeInfo node) {
        return previousAtMovementGranularity(node, AccessibilityNodeInfo.MOVEMENT_GRANULARITY_LINE);
    }

    public boolean previousParagraph(AccessibilityNodeInfo node) {
        return previousAtMovementGranularity(node, AccessibilityNodeInfo.MOVEMENT_GRANULARITY_PARAGRAPH);
    }

    public boolean previousPage(AccessibilityNodeInfo node) {
        return previousAtMovementGranularity(node, AccessibilityNodeInfo.MOVEMENT_GRANULARITY_PAGE);
    }

    public boolean nextHtmlElement(AccessibilityNodeInfo node, String type) {
        if (node == null)
            return false;
        final Bundle args = new Bundle();
        args.putString(
                AccessibilityNodeInfo.ACTION_ARGUMENT_HTML_ELEMENT_STRING, type);
        return node.performAction(AccessibilityNodeInfo.ACTION_NEXT_HTML_ELEMENT, args);
    }

    public boolean previousHtmlElement(AccessibilityNodeInfo node, String type) {
        if (node == null)
            return false;
        final Bundle args = new Bundle();
        args.putString(
                AccessibilityNodeInfo.ACTION_ARGUMENT_HTML_ELEMENT_STRING, type);
        return node.performAction(AccessibilityNodeInfo.ACTION_PREVIOUS_HTML_ELEMENT);
    }

    public String[] getHtmlElements() {
        return new String[]{"", "HEADING", "LINK", "BUTTON", "LANDMARK", "TEXT_FIELD", "FOCUSABLE", "CONTROL", "GRAPHIC", "CHECKBOX", "COMBOBOX", "TABLE", "LIST", "LIST_ITEM"};

    }

    public boolean scrollForward(AccessibilityNodeInfo node) {
        if (node == null)
            return false;
        return node.performAction(AccessibilityNodeInfo.ACTION_SCROLL_FORWARD);
    }

    public boolean scrollBackward(AccessibilityNodeInfo node) {
        if (node == null)
            return false;
        return node.performAction(AccessibilityNodeInfo.ACTION_SCROLL_BACKWARD);
    }

    public boolean copy(AccessibilityNodeInfo node) {
        if (node == null)
            return false;
        return node.performAction(AccessibilityNodeInfo.ACTION_COPY);
    }

    public boolean paste(AccessibilityNodeInfo node) {
        if (node == null)
            return false;
        return node.performAction(AccessibilityNodeInfo.ACTION_PASTE);
    }

    public boolean cut(AccessibilityNodeInfo node) {
        if (node == null)
            return false;
        return node.performAction(AccessibilityNodeInfo.ACTION_CUT);
    }

    public boolean setSelection(AccessibilityNodeInfo node, int start, int end) {
        if (node == null)
            return false;
        Bundle args = new Bundle();
        args.putInt(AccessibilityNodeInfo.ACTION_ARGUMENT_SELECTION_START_INT, start);
        args.putInt(AccessibilityNodeInfo.ACTION_ARGUMENT_SELECTION_END_INT, end);
        return node.performAction(AccessibilityNodeInfo.ACTION_SET_SELECTION, args);
    }

    public boolean setText(AccessibilityNodeInfo node, CharSequence text) {
        if (node == null)
            return false;
        Bundle args = new Bundle();
        args.putCharSequence(AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE, text);
        return node.performAction(AccessibilityNodeInfo.ACTION_SET_TEXT, args);
    }

    private final static String ARG = "arg";
    private final static String DATA = "data";
    private final static String NAME = "name";

    private Globals globals;
    private ArrayListAdapter<String> adapter;
    private String mExtDir;
    private int mWidth;
    private int mHeight;
    private boolean debug;
    private String luaDir;
    private String luaFile = "accessibility.lua";
    private ArrayList<String> permissions;
    private boolean isSetViewed;
    private LuaDexLoader mLuaDexLoader;
    private ArrayList<LuaGcable> mGc = new ArrayList<>();
    private LuaBroadcastReceiver mReceiver;
    private String pageName = "main";

    @CallLuaFunction
    public void reStart() {
        if(mLuaDir!=null)
            luaDir=mLuaDir;
        else
            luaDir = getFilesDir().getAbsolutePath();
        /*if (d != null) {
            String p = d.getPath();
            if (!TextUtils.isEmpty(p)) {
                File f = new File(p);
                if (f.isFile()) {
                    p = f.getParent();
                    luaFile = f.getAbsolutePath();
                }
                luaDir = p;
            }
        }*/
        //luaDir = checkProjectDir(new File(luaDir)).getAbsolutePath();
        initSize();
        pageName = new File(luaFile).getName();
        int idx = pageName.lastIndexOf(".");
        if (idx > 0)
            pageName = pageName.substring(0, idx);

        mLuaDexLoader = new LuaDexLoader(this, luaDir);
        mLuaDexLoader.loadLibs();
        globals = JsePlatform.standardGlobals();
        globals.finder = this;
        initENV();
        globals.luajavaLib.classLoaders = mLuaDexLoader.getClassLoaders();
        try {
            globals.jset("accessibility", this);
            globals.jset("service", this);
            globals.jset("this", this);
            globals.set("print", new print(this));
            globals.set("printf", new printf(this));
            globals.set("loadlayout", new loadlayout(this));
            globals.set("task", new task(this));
            globals.set("thread", new thread(this));
            globals.set("timer", new timer(this));
            globals.load(new res(this));
            globals.load(new json());
            globals.load(new file());
            globals.jset("Http", Http.class);
            globals.jset("http", http.class);
            globals.set("android", new JavaPackage("android"));
            globals.set("java", new JavaPackage("java"));
            globals.set("com", new JavaPackage("com"));
            globals.set("org", new JavaPackage("org"));
            globals.loadfile(luaFile).jcall();
            runFunc("onCreate");
        } catch (final Exception e) {
            sendError("Error", e);
            if (BuildConfig.DEBUG)
                e.printStackTrace();
            Intent res = new Intent();
            res.putExtra(DATA, e.toString());
        }
    }

    private File checkProjectDir(File dir) {
        if (dir == null)
            return new File(luaDir);
        if (new File(dir, "main.lua").exists() && new File(dir, "init.lua").exists())
            return dir;
        return checkProjectDir(dir.getParentFile());
    }

    private void initENV() {
        if (!new File(luaDir + "/init.lua").exists())
            return;

        try {
            LuaTable env = new LuaTable();
            globals.loadfile("init.lua", env).call();

            LuaValue debug = env.get("debugmode");
            if (debug.isboolean())
                setDebug(debug.toboolean());
            debug = env.get("debug_mode");
            if (debug.isboolean())
                setDebug(debug.toboolean());
            LuaValue theme = env.get("theme");
            if (theme.isint())
                setTheme((int) theme.toint());
            else if (theme.isstring())
                setTheme(android.R.style.class.getField(theme.tojstring()).getInt(null));
        } catch (Exception e) {
            sendMsg(e.getMessage());
        }
    }


    public void showLogs() {
        LuaActivity sActivity = LuaActivity.sActivity;
        if (sActivity != null) {
            sActivity.showLogs();
        }
    }

    public void setDebug(boolean bool) {
        debug = bool;
    }

    private void initSize() {
        WindowManager wm = (WindowManager) getSystemService(Context.WINDOW_SERVICE);
        DisplayMetrics outMetrics = new DisplayMetrics();
        wm.getDefaultDisplay().getMetrics(outMetrics);
        mWidth = outMetrics.widthPixels;
        mHeight = outMetrics.heightPixels;
        mScreenDensity= outMetrics.densityDpi;
    }

    public Object runFunc(String name, Object... arg) {
        try {
            LuaValue func = globals.get(name);
            if (func.isfunction())
                return func.jcall(arg);
        } catch (Exception e) {
            sendError(name, e);
        }
        return null;
    }

    public boolean runBooleanFunc(String name, Object... args) {
        try {
            LuaValue func = globals.get(name);
            if (args == null)
                args = new Object[0];
            int l = args.length;
            LuaValue[] as = new LuaValue[l];
            for (int i = 0; i < l; i++) {
                as[i] = CoerceJavaToLua.coerce(args[i]);
            }
            if (func.isfunction())
                return func.invoke(as).toboolean(1);
        } catch (Exception e) {
            sendError(name, e);
        }
        return false;
    }

    @Override
    public InputStream findResource(String name) {
        try {
            if (new File(name).exists())
                return new FileInputStream(name);
        } catch (Exception e) {
           /*if (BuildConfig.DEBUG)
             e.printStackTrace();*/
        }
        try {
            return new FileInputStream(new File(getLuaPath(name)));
        } catch (Exception e) {
            /*if (BuildConfig.DEBUG)
             e.printStackTrace();*/
        }

        try {
            return getAssets().open(name);
        } catch (Exception ioe) {
           /*if (BuildConfig.DEBUG)
             e.printStackTrace();*/
        }
        return null;
    }

    @Override
    public String findFile(String filename) {
        if (filename.startsWith("/"))
            return filename;
        return getLuaPath(filename);
    }

    //显示toast
    public void showToast(String text) {
        if (!debug)
            return;
        CustomToast.show(this, text, android.widget.Toast.LENGTH_LONG, false);
    }

    @Override
    public ArrayList<ClassLoader> getClassLoaders() {
        return null;
    }

    @Override
    public void call(String func, Object... args) {
        globals.get(func).jcall(args);
    }

    @Override
    public void set(String name, Object value) {
        globals.jset(name, value);
    }

    @Override
    public String getLuaPath() {
        return luaFile;
    }

    @Override
    public String getLuaPath(String path) {
        return new File(getLuaDir(), path).getAbsolutePath();
    }

    @Override
    public String getLuaPath(String dir, String name) {
        return new File(getLuaDir(dir), name).getAbsolutePath();
    }

    @Override
    public String getLuaDir() {
        return luaDir;
    }

    @Override
    public String getLuaDir(String dir) {
        return new File(getLuaDir(), dir).getAbsolutePath();
    }

    @Override
    public String getLuaExtDir() {
        if (mExtDir != null)
            return mExtDir;
        File d = new File(Environment.getExternalStorageDirectory(), "LuaJ");
        if (!d.exists())
            d.mkdirs();
        mExtDir = d.getAbsolutePath();
        return mExtDir;
    }

    @Override
    public String getLuaExtDir(String dir) {
        File d = new File(getLuaExtDir(), dir);
        if (!d.exists())
            d.mkdirs();
        return d.getAbsolutePath();
    }

    @Override
    public void setLuaExtDir(String dir) {
        mExtDir = dir;
    }

    @Override
    public String getLuaExtPath(String path) {
        return new File(getLuaExtDir(), path).getAbsolutePath();
    }

    @Override
    public String getLuaExtPath(String dir, String name) {
        return new File(getLuaExtDir(dir), name).getAbsolutePath();
    }

    @Override
    public Context getContext() {
        return this;
    }

    @Override
    public Globals getLuaState() {
        return globals;
    }

    @Override
    public Object doFile(String path, Object... arg) {
        return globals.loadfile(path).jcall(arg);
    }

    @Override
    public void sendMsg(final String msg) {
        LuaActivity sActivity=LuaActivity.sActivity;
        if (sActivity != null) {
            sActivity.sendMsg(msg);
        } else {
            LuaActivity.logs.add(msg);
        }
    }

    @Override
    public void sendError(String title, Exception msg) {
        sendMsg(title + ": " + msg.getMessage());
    }

    public static void logError(String title, Exception msg) {
        LuaActivity.logs.add(title+":"+msg);
    }


    public Intent registerReceiver(LuaBroadcastReceiver receiver, IntentFilter filter) {
        // TODO: Implement this method
        return super.registerReceiver(receiver, filter);
    }

    public Intent registerReceiver(LuaBroadcastReceiver.OnReceiveListener ltr, IntentFilter filter) {
        // TODO: Implement this method
        LuaBroadcastReceiver receiver = new LuaBroadcastReceiver(ltr);
        return super.registerReceiver(receiver, filter);
    }

    public Intent registerReceiver(IntentFilter filter) {
        // TODO: Implement this method
        if (mReceiver != null)
            unregisterReceiver(mReceiver);
        mReceiver = new LuaBroadcastReceiver(this);
        return super.registerReceiver(mReceiver, filter);
    }

    @Override
    public void unregisterReceiver(BroadcastReceiver receiver) {
        try {
            super.unregisterReceiver(receiver);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @CallLuaFunction
    @Override
    public void onReceive(Context context, Intent intent) {
        // TODO: Implement this method
        runFunc("onReceive", context, intent);
    }

    public static LuaActivity getActivity(String name) {
        return LuaActivity.getActivity(name);
    }

    @CallLuaFunction
    @Override
    public void onDestroy() {
        runFunc("onDestroy");
        for (LuaGcable g : mGc) {
            try {
                g.gc();
            } catch (Exception e) {
                //nothing
            }
        }
        mGc.clear();
        try {
            if (mReceiver != null)
                unregisterReceiver(mReceiver);
            mTts.shutdown();
            mSpeech.destroy();
        } catch (Exception e) {
            e.printStackTrace();
        }
        sInstance = null;
        super.onDestroy();
    }


    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        // TODO: Implement this method
        super.onConfigurationChanged(newConfig);
        initSize();
    }


    public int getWidth() {
        return mWidth;
    }

    public int getHeight() {
        return mHeight;
    }

    public int getDensity() {
        return mScreenDensity;
    }

    @Override
    public Map getGlobalData() {
        return LuaApplication.getInstance().getGlobalData();
    }


    @Override
    public Map<String, ?> getSharedData() {
        return PreferenceManager.getDefaultSharedPreferences(this).getAll();
    }

    @Override
    public Object getSharedData(String key) {
        return PreferenceManager.getDefaultSharedPreferences(this).getAll().get(key);
    }

    @Override
    public Object getSharedData(String key, Object def) {
        Object ret = PreferenceManager.getDefaultSharedPreferences(this).getAll().get(key);
        if (ret != null)
            return ret;
        return def;
    }

    @Override
    public boolean setSharedData(String key, Object value) {
        SharedPreferences.Editor edit = PreferenceManager.getDefaultSharedPreferences(this).edit();
        if (value == null)
            edit.remove(key);
        else if (value instanceof String)
            edit.putString(key, value.toString());
        else if (value instanceof Long)
            edit.putLong(key, (Long) value);
        else if (value instanceof Integer)
            edit.putInt(key, (Integer) value);
        else if (value instanceof Float)
            edit.putFloat(key, (Float) value);
        else if (value instanceof LuaTable)
            edit.putStringSet(key, new HashSet(((LuaTable) value).values()));
        else if (value instanceof Set)
            edit.putStringSet(key, (Set<String>) value);
        else if (value instanceof Boolean)
            edit.putBoolean(key, (Boolean) value);
        else
            return false;
        return edit.commit();
    }

    @Override
    public void regGc(LuaGcable obj) {
        mGc.add(obj);
    }

    public void newActivity(String path, boolean newDocument) throws FileNotFoundException {
        newActivity(1, path, null, newDocument);
    }

    public void newActivity(String path, Object[] arg, boolean newDocument) throws FileNotFoundException {
        newActivity(1, path, arg, newDocument);
    }

    public void newActivity(int req, String path, boolean newDocument) throws FileNotFoundException {
        newActivity(req, path, null, newDocument);
    }

    public void newActivity(String path) throws FileNotFoundException {
        newActivity(1, path, new Object[0]);
    }

    public void newActivity(String path, Object[] arg) throws FileNotFoundException {
        newActivity(1, path, arg);
    }

    public void newActivity(int req, String path) throws FileNotFoundException {
        newActivity(req, path, new Object[0]);
    }

    public void newActivity(int req, String path, Object[] arg) throws FileNotFoundException {
        newActivity(req, path, arg, false);
    }

    public void newActivity(int req, String path, Object[] arg, boolean newDocument) throws FileNotFoundException {
        //Log.i("luaj", "newActivity: "+path+ Arrays.toString(arg));
        Intent intent = new Intent(this, LuaActivity.class);
        if (newDocument)
            intent = new Intent(this, LuaActivityX.class);

        intent.putExtra(NAME, path);
        if (path.charAt(0) != '/' && luaDir != null)
            path = luaDir + "/" + path;
        File f = new File(path);
        if (f.isDirectory() && new File(path + "/main.lua").exists())
            path += "/main.lua";
        else if ((f.isDirectory() || !f.exists()) && !path.endsWith(".lua"))
            path += ".lua";
        if (!new File(path).exists())
            throw new FileNotFoundException(path);

        if (newDocument) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_DOCUMENT);
                intent.addFlags(Intent.FLAG_ACTIVITY_MULTIPLE_TASK);
            }
        }

        intent.setData(Uri.parse("file://" + path));

        if (arg != null)
            intent.putExtra(ARG, arg);
        if (newDocument)
            startActivity(intent);
        else
            startActivity(intent);
    }

    public void newActivity(String path, int in, int out, boolean newDocument) throws FileNotFoundException {
        newActivity(1, path, in, out, null, newDocument);
    }

    public void newActivity(String path, int in, int out, Object[] arg, boolean newDocument) throws FileNotFoundException {
        newActivity(1, path, in, out, arg, newDocument);
    }

    public void newActivity(int req, String path, int in, int out, boolean newDocument) throws FileNotFoundException {
        newActivity(req, path, in, out, null, newDocument);
    }

    public void newActivity(String path, int in, int out) throws FileNotFoundException {
        newActivity(1, path, in, out, new Object[0]);
    }

    public void newActivity(String path, int in, int out, Object[] arg) throws FileNotFoundException {
        newActivity(1, path, in, out, arg);
    }

    public void newActivity(int req, String path, int in, int out) throws FileNotFoundException {
        newActivity(req, path, in, out, new Object[0]);
    }

    public void newActivity(int req, String path, int in, int out, Object[] arg) throws FileNotFoundException {
        newActivity(req, path, in, out, arg, false);
    }

    public void newActivity(int req, String path, int in, int out, Object[] arg, boolean newDocument) throws FileNotFoundException {
        Intent intent = new Intent(this, LuaActivity.class);
        if (newDocument)
            intent = new Intent(this, LuaActivityX.class);
        intent.putExtra(NAME, path);
        if (path.charAt(0) != '/' && luaDir != null)
            path = luaDir + "/" + path;
        File f = new File(path);
        if (f.isDirectory() && new File(path + "/main.lua").exists())
            path += "/main.lua";
        else if ((f.isDirectory() || !f.exists()) && !path.endsWith(".lua"))
            path += ".lua";
        if (!new File(path).exists())
            throw new FileNotFoundException(path);

        intent.setData(Uri.parse("file://" + path));

        if (newDocument) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_DOCUMENT);
                intent.addFlags(Intent.FLAG_ACTIVITY_MULTIPLE_TASK);
            }
        }


        if (arg != null)
            intent.putExtra(ARG, arg);
        if (newDocument)
            startActivity(intent);
        else
            startActivity(intent);

    }

    @Override
    public void startActivity(Intent intent) {
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        super.startActivity(intent);
    }

    public Uri getUriForPath(String path) {
        return FileProvider.getUriForFile(this, getPackageName(), new File(path));
    }

    public Uri getUriForFile(File path) {
        return FileProvider.getUriForFile(this, getPackageName(), path);
    }

    public String getPathFromUri(Uri uri) {
        String path = null;
        if (uri != null) {
            String[] p = {
                    MediaStore.Images.Media.DATA
            };
            switch (uri.getScheme()) {
                case "content":
                    /*try {
                        InputStream in = getContentResolver().openInputStream(uri);
					} catch (IOException e) {
						e.printStackTrace();
					}*/

                    Cursor cursor = getContentResolver().query(uri, p, null, null, null);
                    if (cursor != null) {
                        int idx = cursor.getColumnIndexOrThrow(getPackageName());
                        if (idx < 0)
                            break;
                        path = cursor.getString(idx);
                        cursor.moveToFirst();
                        cursor.close();
                    }
                    break;
                case "file":
                    path = uri.getPath();
                    break;
            }
        }
        return path;
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

    public void installApk(String path) {
        Intent share = new Intent(Intent.ACTION_VIEW);
        File file = new File(path);
        share.setFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        share.setDataAndType(getUriForFile(file), getType(file));
        share.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(share);
    }

    public void openFile(String path) {
        Intent share = new Intent(Intent.ACTION_VIEW);
        File file = new File(path);
        share.setFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        share.addFlags(Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
        share.setDataAndType(getUriForFile(file), getType(file));
        share.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(share);
    }

    public void shareFile(String path) {
        Intent share = new Intent(Intent.ACTION_SEND);
        File file = new File(path);
        share.setType("*/*");
        share.setFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        share.putExtra(Intent.EXTRA_STREAM, getUriForFile(file));
        startActivity(Intent.createChooser(share, file.getName()).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
    }
}
