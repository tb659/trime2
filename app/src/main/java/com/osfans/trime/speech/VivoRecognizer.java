package com.osfans.trime.speech;

import android.media.MediaRecorder;
import android.os.Bundle;
import android.util.Log;

import com.androlua.LuaApplication;
import com.osfans.trime.BuildConfig;
import com.osfans.trime.TrimeService;
import com.vivo.speechsdk.api.InitListener;
import com.vivo.speechsdk.api.SpeechConstants;
import com.vivo.speechsdk.api.SpeechError;
import com.vivo.speechsdk.api.SpeechEvent;
import com.vivo.speechsdk.api.SpeechSdk;
import com.vivo.speechsdk.asr.api.ASREngine;
import com.vivo.speechsdk.asr.api.IRecognizerListener;
import com.vivo.speechsdk.asr.api.IUpdateHotWordListener;

import org.json.JSONException;
import org.json.JSONObject;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

/**
 * Vivo 语音识别器实现类,基于 Vivo 语音 SDK。
 * 实现了 Recognizer 接口和 IRecognizerListener 监听器。
 * 提供在线语音识别功能,支持 VAD(语音活动检测)和实时结果返回。
 */
public class VivoRecognizer implements Recognizer, IRecognizerListener {

    // TrimeService 实例,用于获取 Handler 进行线程切换
    private final TrimeService mService;
    // 识别结果监听器
    private final RecognizerListener mListener;
    // Vivo ASR 引擎实例
    private ASREngine mEngine;

    /**
     * 构造函数。
     *
     * @param service TrimeService 实例。
     * @param listener 识别结果监听器。
     */
    public VivoRecognizer(TrimeService service, RecognizerListener listener) {
        mService = service;
        mListener = listener;
        init2();
    }

    /**
     * 初始化语音识别引擎。
     * 分两步初始化:先初始化 SDK,再初始化 ASR 引擎。
     */
    private void init2() {
        // 第一步: 检查 SDK 是否已初始化
        if(!SpeechSdk.isInit()){
            // SDK 未初始化,则异步初始化
            SpeechSdk.init(LuaApplication.getInstance(), UUID.randomUUID().toString(), new InitListener() {
                @Override
                public void onSuccess() {
                    Log.w("VivoSdk", "onSuccess: ");
                    // SDK 初始化成功后,再次调用 init2 初始化 ASR 引擎
                    init2();
                }

                @Override
                public void onError(SpeechError error) {
                    Log.w("VivoSdk", "onError: "+error.getDescription() );
                }
            });
            return;
        }

        // 第二步: 创建并初始化 ASR 引擎
        mEngine = ASREngine.createEngine();
        Bundle bundle = new Bundle();
        // 设置应用 ID 和密钥(从 BuildConfig 中读取)
        bundle.putString(SpeechConstants.KEY_APPID, BuildConfig.API_ID);
        bundle.putString(SpeechConstants.KEY_APPKEY, BuildConfig.API_KEY);
        // 设置为在线识别模式
        bundle.putInt(SpeechConstants.KEY_ENGINE_MODE, SpeechConstants.TYPE_ENGINE_MODE_ONLINE);
        // 设置引擎类型为短语音输入
        bundle.putString(SpeechConstants.KEY_ENGINE_TYPE, "shortasrinput");
        // 禁用预加载
        bundle.putBoolean(SpeechConstants.KEY_PRELOAD_ENABLE, false);
        // 启用连接复用,提高性能
        bundle.putBoolean(SpeechConstants.KEY_CONNECTION_REUSE_ENABLE, true);
        mEngine.init(bundle, new InitListener() {
            @Override
            public void onSuccess() {
                //do something
            }

            @Override
            public void onError(SpeechError error) {
                //do something
            }
        });
    }

    /**
     * 开始监听语音输入。
     * 配置 VAD 参数,启动语音识别引擎。
     */
    @Override
    public void startListening() {
        if(mEngine==null){
            init2();
            return;
        }
        Bundle bundle = new Bundle();
        // 启用内部录音
        bundle.putBoolean(SpeechConstants.KEY_INNER_RECORD, true);
        // 设置为 ASR 请求模式
        bundle.putInt(SpeechConstants.KEY_REQUEST_MODE, SpeechConstants.TYPE_REQUEST_MODE_ASR);
        bundle.putString(SpeechConstants.KEY_BUSINESS_INFO, "vivo");
        // 设置 VAD 模式为正常
        bundle.putString(SpeechConstants.KEY_VAD_MODE, "normal");
        // 设置静音保持计数(8个静音帧后结束)
        bundle.putInt(SpeechConstants.KEY_VAD_KEEP_SILENCE_COUNT, 8);
        // 设置音频源为麦克风
        bundle.putInt(SpeechConstants.KEY_AUDIO_SOURCE, MediaRecorder.AudioSource.MIC);
        // 设置标点符号模式(2=智能标点)
        bundle.putInt(SpeechConstants.KEY_PUNCTUATION, 2);
        // 设置前端静音超时时间(5000ms)
        bundle.putInt(SpeechConstants.KEY_VAD_FRONT_TIME, 5000);
        // 设置后端静音超时时间(1000ms)
        bundle.putInt(SpeechConstants.KEY_VAD_END_TIME, 1000);
        // 启用 VAD
        bundle.putBoolean(SpeechConstants.KEY_VAD_ENABLE, true);
        // 启用中文数字转换
        bundle.putBoolean(SpeechConstants.KEY_CHINESE_TO_DIGITAL, true);
        // 设置 ASR 超时时间(5000ms)
        bundle.putInt(SpeechConstants.KEY_ASR_TIME_OUT, 5000);
        // 启用编码
        bundle.putBoolean(SpeechConstants.KEY_ENCODE_ENABLE, true);
        int code = mEngine.start(bundle, this);
        if (code != 0) {
            mListener.onError(String.valueOf(code));
            //start failed do something
        }
    }

    /**
     * 开始语音输入模式。
     * 与 startListening 类似,但使用不同的 VAD 参数(更短的静音检测时间)。
     */
    @Override
    public void startInputting() {
        if(mEngine==null){
            init2();
            return;
        }
        Bundle bundle = new Bundle();
        bundle.putBoolean(SpeechConstants.KEY_INNER_RECORD, true);
        bundle.putInt(SpeechConstants.KEY_REQUEST_MODE, SpeechConstants.TYPE_REQUEST_MODE_ASR);
        bundle.putString(SpeechConstants.KEY_BUSINESS_INFO, "vivo");
        bundle.putString(SpeechConstants.KEY_VAD_MODE, "normal");
        // 设置静音保持计数(4个静音帧后结束,比 startListening 更短)
        bundle.putInt(SpeechConstants.KEY_VAD_KEEP_SILENCE_COUNT, 4);
        bundle.putInt(SpeechConstants.KEY_AUDIO_SOURCE, MediaRecorder.AudioSource.MIC);
        // 设置标点符号模式(1=简单标点)
        bundle.putInt(SpeechConstants.KEY_PUNCTUATION, 1);
        bundle.putInt(SpeechConstants.KEY_VAD_FRONT_TIME, 5000);
        // 设置后端静音超时时间(5000ms,比 startListening 更长)
        bundle.putInt(SpeechConstants.KEY_VAD_END_TIME, 5000);
        bundle.putBoolean(SpeechConstants.KEY_VAD_ENABLE, true);
        bundle.putBoolean(SpeechConstants.KEY_CHINESE_TO_DIGITAL, true);
        bundle.putInt(SpeechConstants.KEY_ASR_TIME_OUT, 5000);
        bundle.putBoolean(SpeechConstants.KEY_ENCODE_ENABLE, true);
        int code = mEngine.start(bundle, this);
        if (code != 0) {
            mListener.onError(String.valueOf(code));
            //start failed do something
        }
    }

    /**
     * 停止语音识别。
     */
    @Override
    public void stop() {
        if(mEngine==null){
            return;
        }
        mEngine.stop();
    }

    /**
     * 取消语音识别。
     */
    @Override
    public void cancel() {
        if(mEngine==null){
            return;
        }
        mEngine.cancel();
    }

    /**
     * 销毁识别器资源。
     */
    @Override
    public void destroy() {
        if(mEngine==null){
            return;
        }
        mEngine.destroy();
    }

    /**
     * 更新用户数据(暂未实现)。
     */
    @Override
    public void updateUserData() {
    }

    /**
     * 设置识别语言(暂未实现)。
     *
     * @param language 语言代码。
     */
    @Override
    public void setLanguage(String language) {

    }

    /**
     * 当识别结果返回时调用。
     * 解析 JSON 格式的识别结果,如果是最后一帧则回调给监听器。
     *
     * @param i 状态码。
     * @param s JSON 格式的识别结果字符串。
     */
    @Override
    public void onResult(int i, String s) {
        try {
            JSONObject json = new JSONObject(s);
            // 检查是否为最后一帧
            if(json.getBoolean("is_last")) {
                // 通知语音结束
                mListener.onEnd();
                // 切换到主线程回调结果
                mService.getHandler().post(new Runnable() {
                    @Override
                    public void run() {
                        try {
                            mListener.onResult(json.getString("text"));
                        } catch (Exception e) {
                            mListener.onError(e.toString());
                        }
                    }
                });
            }
        } catch (Exception e) {
            // 如果解析失败,直接返回原始字符串
            mListener.onResult(s);
        }
    }

    /**
     * 当语音开始时调用。
     */
    @Override
    public void onSpeechStart() {
        mListener.onBegin();
    }

    /**
     * 当语音结束时调用(暂未使用)。
     */
    @Override
    public void onSpeechEnd() {
    }

    /**
     * 当录音开始时调用。
     * 通知监听器录音设备已准备好。
     */
    @Override
    public void onRecordStart() {
        mListener.onReady();
    }

    /**
     * 当录音结束时调用(暂未使用)。
     */
    @Override
    public void onRecordEnd() {

    }

    /**
     * 当音量变化时调用(暂未使用)。
     *
     * @param i 音量值。
     * @param bytes 音频数据。
     */
    @Override
    public void onVolumeChanged(int i, byte[] bytes) {

    }

    /**
     * 当识别会话结束时调用(暂未使用)。
     */
    @Override
    public void onEnd() {
    }

    /**
     * 当事件发生时调用(暂未使用)。
     *
     * @param i 事件 ID。
     * @param bundle 事件参数。
     */
    @Override
    public void onEvent(int i, Bundle bundle) {
        //Log.w("TAG", "onEvent: "+i +bundle);
    }

    /**
     * 当发生错误时调用。
     *
     * @param speechError 错误对象。
     */
    @Override
    public void onError(SpeechError speechError) {
        mListener.onError(speechError.getDescription());
    }
}
