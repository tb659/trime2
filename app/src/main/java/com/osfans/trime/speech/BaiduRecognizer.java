package com.osfans.trime.speech;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Handler;
import android.util.Log;

import com.baidu.speech.EventListener;
import com.baidu.speech.EventManager;
import com.baidu.speech.EventManagerFactory;
import com.baidu.speech.asr.SpeechConstant;
import com.osfans.trime.BuildConfig;
import com.osfans.trime.TrimeService;
import com.osfans.trime.util.Function;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 百度语音识别器实现类,基于百度语音识别 SDK V3。
 * 实现了 Recognizer 接口。
 * 提供在线语音识别功能,支持 VAD(语音活动检测)和实时结果返回。
 */
public class BaiduRecognizer implements Recognizer {
    /** 日志标签 */
    private static final String TAG = "BaiduRecognizer";

    // ==================== 偏好设置键名常量 ====================
    /** 百度 API Key 的 SharedPreferences 键名 */
    public static final String KEY_BAIDU_API_KEY = "baidu_api_key";
    /** 百度 Secret Key 的 SharedPreferences 键名 */
    public static final String KEY_BAIDU_SECRET_KEY = "baidu_secret_key";

    // ==================== 语言常量映射 ====================
    /** 百度 SDK 支持的普通话语言代码 */
    private static final String BAIDU_LANG_MANDARIN = "cmn-Hans-CN";
    /** 百度 SDK 支持的粤语语言代码 */
    private static final String BAIDU_LANG_CANTONESE = "yue";
    /** 百度 SDK 支持的英语语言代码 */
    private static final String BAIDU_LANG_ENGLISH = "en";

    // TrimeService 实例,用于获取 Handler 进行线程切换
    private final TrimeService mService;
    // 识别结果监听器
    private final RecognizerListener mListener;
    // 上下文对象
    private final Context mContext;
    // 百度语音事件管理器实例
    private EventManager mEventManager;
    // 当前识别语言
    private String mCurrentLanguage = BAIDU_LANG_MANDARIN;
    // 百度 API 凭证
    private String mApiKey;
    private String mSecretKey;
    // 是否已初始化
    private boolean mInitialized = false;

    /**
     * 构造函数。
     *
     * @param service TrimeService 实例。
     * @param listener 识别结果监听器。
     */
    public BaiduRecognizer(TrimeService service, RecognizerListener listener) {
        mService = service;
        mListener = listener;
        mContext = service;
        init();
    }

    /**
     * 初始化百度语音识别器。
     * 优先从 BuildConfig 读取 API 凭证(来自 local.properties),其次从 SharedPreferences 读取。
     */
    private void init() {
        SharedPreferences pref = Function.getPref(mContext);
        
        // 优先从 BuildConfig 获取(编译时从 local.properties 注入)
        mApiKey = BuildConfig.API_KEY;
        mSecretKey = BuildConfig.SECRET_KEY;
        
        // 如果 BuildConfig 中没有,则从 SharedPreferences 读取(用户手动配置)
        if (mApiKey == null || mApiKey.isEmpty()) {
            mApiKey = pref.getString(KEY_BAIDU_API_KEY, "");
        }
        if (mSecretKey == null || mSecretKey.isEmpty()) {
            mSecretKey = pref.getString(KEY_BAIDU_SECRET_KEY, "");
        }
        
        Log.i(TAG, "init: mApiKey=" + (mApiKey.isEmpty() ? "[empty]" : "[configured]") + 
                   ", mSecretKey=" + (mSecretKey.isEmpty() ? "[empty]" : "[configured]"));

        if (mApiKey.isEmpty() || mSecretKey.isEmpty()) {
            Log.e(TAG, "init: Baidu API credentials not configured");
            mListener.onError("百度语音凭证未配置,请在设置中填写 API Key 和 Secret Key");
            return;
        }

        try {
            mEventManager = EventManagerFactory.create(mContext, "asr");
            EventListener eventListener = new EventListener() {
                @Override
                public void onEvent(String name, String params, byte[] data, int offset, int length) {
                    handleEvent(name, params, data, offset, length);
                }
            };
            mEventManager.registerListener(eventListener);
            mInitialized = true;
            Log.i(TAG, "init: Baidu EventManager created successfully");
        } catch (Exception e) {
            Log.e(TAG, "init: Failed to create Baidu EventManager", e);
            mListener.onError("百度语音初始化失败: " + e.getMessage());
        }
    }

    /**
     * 构建识别参数 Map。
     *
     * @param vadEndTime 后端静音超时时间(ms)
     * @param punctuationMode 标点模式: 1=简单标点, 2=智能标点
     * @return 配置好的参数 Map
     */
    private Map<String, Object> buildParams(int vadEndTime, int punctuationMode) {
        Map<String, Object> params = new LinkedHashMap<>();
        // API 凭证
        params.put(SpeechConstant.APP_KEY, mApiKey);
        params.put(SpeechConstant.SECRET, mSecretKey);
        // 识别参数
        params.put(SpeechConstant.PROP, punctuationMode == 1 ? "10060" : "1537"); // 10060=输入法, 1537=普通话
        params.put(SpeechConstant.LANGUAGE, mCurrentLanguage);
        // VAD 参数
        params.put(SpeechConstant.VAD_ENDPOINT_TIMEOUT, vadEndTime);
        params.put(SpeechConstant.VAD, SpeechConstant.VAD_DNN);
        // 音频参数
        params.put(SpeechConstant.AUDIO_SOURCE, android.media.MediaRecorder.AudioSource.MIC);
        params.put(SpeechConstant.SAMPLE_RATE, 16000);
        // 禁用标点
        if (punctuationMode == 0) {
            params.put(SpeechConstant.DISABLE_PUNCTUATION, true);
        }
        return params;
    }

    /**
     * 开始监听语音输入。
     * 配置 VAD 参数:前端 5000ms、后端 1000ms、智能标点。
     */
    @Override
    public void startListening() {
        if (!mInitialized || mEventManager == null) {
            Log.e(TAG, "startListening: not initialized");
            return;
        }
        try {
            Map<String, Object> params = buildParams(1000, 2);
            String jsonParams = new JSONObject(params).toString();
            mEventManager.send(SpeechConstant.ASR_START, jsonParams, null, 0, 0);
            Log.i(TAG, "startListening: started with VAD end=1000ms, punctuation=smart");
        } catch (Exception e) {
            Log.e(TAG, "startListening: failed", e);
            mListener.onError("启动识别失败: " + e.getMessage());
        }
    }

    /**
     * 开始语音输入模式。
     * 使用较长的后端静音超时(5000ms)和简单标点模式。
     */
    @Override
    public void startInputting() {
        if (!mInitialized || mEventManager == null) {
            Log.e(TAG, "startInputting: not initialized");
            return;
        }
        try {
            Map<String, Object> params = buildParams(5000, 1);
            String jsonParams = new JSONObject(params).toString();
            mEventManager.send(SpeechConstant.ASR_START, jsonParams, null, 0, 0);
            Log.i(TAG, "startInputting: started with VAD end=5000ms, punctuation=simple");
        } catch (Exception e) {
            Log.e(TAG, "startInputting: failed", e);
            mListener.onError("启动识别失败: " + e.getMessage());
        }
    }

    /**
     * 停止语音识别。
     */
    @Override
    public void stop() {
        if (mEventManager != null) {
            mEventManager.send(SpeechConstant.ASR_STOP, "{}", null, 0, 0);
            Log.i(TAG, "stop: recognition stopped");
        }
    }

    /**
     * 取消语音识别。
     */
    @Override
    public void cancel() {
        if (mEventManager != null) {
            mEventManager.send(SpeechConstant.ASR_CANCEL, "{}", null, 0, 0);
            Log.i(TAG, "cancel: recognition cancelled");
        }
    }

    /**
     * 销毁识别器资源。
     */
    @Override
    public void destroy() {
        if (mEventManager != null) {
            cancel();
            mEventManager = null;
            mInitialized = false;
            Log.i(TAG, "destroy: resources released");
        }
    }

    /**
     * 更新用户数据(暂未实现)。
     */
    @Override
    public void updateUserData() {
        // 预留:可调用百度热词更新接口
        Log.i(TAG, "updateUserData: not implemented");
    }

    /**
     * 设置识别语言。
     *
     * @param language 语言代码,如 zh_CN、zh_GD、en_GB。
     */
    @Override
    public void setLanguage(String language) {
        if (Recognizer.zh_CN.equals(language)) {
            mCurrentLanguage = BAIDU_LANG_MANDARIN;
        } else if (Recognizer.zh_GD.equals(language)) {
            mCurrentLanguage = BAIDU_LANG_CANTONESE;
        } else if (Recognizer.en_GB.equals(language)) {
            mCurrentLanguage = BAIDU_LANG_ENGLISH;
        } else {
            Log.w(TAG, "setLanguage: unsupported language " + language + ", using default");
        }
        Log.i(TAG, "setLanguage: " + language + " -> " + mCurrentLanguage);
    }

    // ==================== 百度 SDK 事件处理 ====================

    /**
     * 处理百度 SDK 事件回调。
     * 将百度 SDK 的回调桥接到 RecognizerListener 接口。
     *
     * @param name 事件名称
     * @param params JSON 格式的参数
     * @param data 音频数据
     * @param offset 数据偏移
     * @param length 数据长度
     */
    private void handleEvent(String name, String params, byte[] data, int offset, int length) {
        Log.d(TAG, "onEvent: name=" + name + ", params=" + params);

        switch (name) {
            case SpeechConstant.CALLBACK_EVENT_ASR_READY: // asr.ready
                // 录音设备准备好
                mListener.onReady();
                break;

            case SpeechConstant.CALLBACK_EVENT_ASR_BEGIN: // asr.begin
                // 检测到用户开始说话
                mListener.onBegin();
                break;

            case SpeechConstant.CALLBACK_EVENT_ASR_END: // asr.end
                // 检测到用户停止说话
                mListener.onEnd();
                break;

            case SpeechConstant.CALLBACK_EVENT_ASR_PARTIAL: // asr.partial
                // 部分识别结果
                handlePartialResult(params);
                break;

            case SpeechConstant.CALLBACK_EVENT_ASR_FINISH: // asr.finish
                // 识别完成
                handleFinishResult(params);
                break;

            case SpeechConstant.CALLBACK_EVENT_ASR_ERROR: // asr.error
                // 识别错误
                handleError(params);
                break;

            case SpeechConstant.CALLBACK_EVENT_ASR_VOLUME: // asr.volume
                // 音量变化
                handleVolume(params);
                break;

            default:
                break;
        }
    }

    /**
     * 处理部分识别结果。
     * 包括中间结果(partial_result)和最终结果(final_result)。
     */
    private void handlePartialResult(String params) {
        try {
            JSONObject json = new JSONObject(params);
            if (json.has("results_recognition")) {
                Object results = json.get("results_recognition");
                String text = null;
                if (results instanceof org.json.JSONArray) {
                    org.json.JSONArray arr = (org.json.JSONArray) results;
                    if (arr.length() > 0) {
                        text = arr.getString(0);
                    }
                } else {
                    text = results.toString();
                }

                if (text != null) {
                    String resultType = json.optString("result_type", "partial_result");
                    Log.d(TAG, resultType + ": " + text);

                    // 如果是最终结果，通过 Handler 回调到主线程
                    if ("final_result".equals(resultType)) {
                        final String finalText = text;
                        Handler handler = mService.getHandler();
                        if (handler != null) {
                            handler.post(new Runnable() {
                                @Override
                                public void run() {
                                    mListener.onEnd();
                                    mListener.onResult(finalText);
                                }
                            });
                        }
                    }
                }
            }
        } catch (JSONException e) {
            Log.w(TAG, "handlePartialResult: failed to parse JSON", e);
        }
    }

    /**
     * 处理识别完成结果。
     * asr.finish 事件只包含状态信息，不包含识别文本。
     * 最终结果已在 handlePartialResult 中处理。
     */
    private void handleFinishResult(String params) {
        Log.d(TAG, "handleFinishResult: " + params);
        try {
            JSONObject json = new JSONObject(params);
            int error = json.optInt("error", -1);

            if (error != 0) {
                String desc = json.optString("desc", "未知错误");
                handleErrorWithDesc(error, desc);
            }
            // 成功时不做处理，最终结果已在 asr.partial 事件中处理
        } catch (JSONException e) {
            Log.w(TAG, "handleFinishResult: failed to parse JSON", e);
        }
    }

    /**
     * 处理识别错误。
     */
    private void handleError(String params) {
        try {
            JSONObject json = new JSONObject(params);
            int subError = json.optInt("sub_error", 0);
            String desc = json.optString("desc", "识别错误");
            handleErrorWithDesc(subError, desc);
        } catch (JSONException e) {
            Log.w(TAG, "handleError: failed to parse JSON", e);
            mListener.onError("识别错误: " + params);
        }
    }

    /**
     * 处理错误描述。
     */
    private void handleErrorWithDesc(int errorCode, String desc) {
        Log.e(TAG, "handleErrorWithDesc: code=" + errorCode + ", desc=" + desc);
        String chineseMsg = mapErrorCodeToChinese(errorCode);
        mListener.onError(chineseMsg + ": " + desc);
    }

    /**
     * 处理音量变化。
     */
    private void handleVolume(String params) {
        // 暂未使用
    }

    /**
     * 将百度 SDK 错误码映射为中文提示。
     *
     * @param errorCode 百度 SDK 错误码。
     * @return 中文错误提示。
     */
    private String mapErrorCodeToChinese(int errorCode) {
        switch (errorCode) {
            case 1:
            case 2:
            case 3:
            case 4:
            case 5:
            case 6:
                return "音频错误";
            case 7:
                return "网络错误";
            case 8:
                return "服务器错误";
            case 9:
                return "客户端错误";
            case 10:
                return "权限不足";
            case 11:
                return "未能识别";
            case 12:
                return "网络超时";
            case 13:
                return "识别服务忙";
            case 14:
                return "无语音输入";
            case 3300:
                return "输入参数不正确";
            case 3301:
                return "音频质量过差";
            case 3302:
                return "鉴权失败";
            case 3303:
                return "音频过长";
            case 3304:
                return "音频格式错误";
            case 3305:
                return "请求过快";
            case 3306:
                return "不支持的采样率";
            case 3307:
                return "不支持的协议";
            case 3308:
                return "不支持的音频编码";
            case 3309:
                return "不支持的语种";
            case 3310:
                return "未找到识别结果";
            default:
                if (errorCode >= 3000 && errorCode < 4000) {
                    return "百度服务器错误(" + errorCode + ")";
                }
                return "未知错误(" + errorCode + ")";
        }
    }
}
