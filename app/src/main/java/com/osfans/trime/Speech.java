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

import android.accessibilityservice.AccessibilityServiceInfo;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.media.SoundPool;
import android.os.Bundle;
import android.os.Vibrator;
import android.speech.RecognitionListener;
import android.speech.RecognitionService;
import android.speech.RecognizerIntent;
import android.speech.SpeechRecognizer;
import android.text.TextUtils;
import android.util.Log;
import android.view.accessibility.AccessibilityManager;
import android.widget.Toast;

import com.osfans.trime.data.opencc.OpenCCDictManager;
import com.osfans.trime.speech.RecognizerListener;
import com.osfans.trime.speech.VivoRecognizer;
import com.osfans.trime.theme.ThemeManager;
import com.osfans.trime.util.Function;
import com.osfans.trime.core.Rime;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;

import static android.Manifest.permission.RECORD_AUDIO;
import static android.content.Context.VIBRATOR_SERVICE;
import static android.media.AudioManager.STREAM_MUSIC;

/**
 * 语音输入管理类，实现RecognitionListener和RecognizerListener接口。
 * 支持系统语音识别服务和Vivo语音识别服务两种引擎。
 * 提供语音识别的开始、停止、取消功能，并处理识别结果（支持简繁转换）。
 * 包含音效播放和震动反馈功能。
 */
class Speech implements RecognitionListener, RecognizerListener {
    /** 结果ID的键名（用于Intent传递） */
    public static final String RES_ID = "RES_ID";
    /** 名称的键名（用于Intent传递） */
    public static final String NAME = "NAME";
    /** 配置键名 */
    public static final String config = "config";

    /** 音效池，用于播放语音识别相关的音效 */
    private final SoundPool mSoundPool;
    /** Vivo语音识别器实例（如果使用Vivo引擎） */
    private VivoRecognizer vSpeech;
    /** 取消音效ID */
    private int mSoundCancel;
    /** 错误音效ID */
    private int mSoundError;
    /** 开始音效ID */
    private int mSoundStart;
    /** 成功音效ID */
    private int mSoundSuccess;
    /** 结束音效ID */
    private int mSoundEnd;
    /** 震动器实例，用于触觉反馈 */
    private final Vibrator mVibrator;
    /** 震动持续时间（毫秒） */
    private int duration = 10;
    /** Android系统语音识别器实例 */
    private SpeechRecognizer speech = null;
    /** 语音识别意图，包含识别参数 */
    private Intent recognizerIntent;
    /** 日志标签 */
    private String TAG = "Speech";
    /** 上下文对象 */
    private Context context;
    /** 当前识别状态 */
    private int state = STATE_CANCEL;
    /** 状态常量：完成 */
    public static final int STATE_DONE = -1;
    /** 状态常量：错误 */
    public static final int STATE_ERROR = -2;
    /** 状态常量：就绪 */
    public static final int STATE_READY = 0;
    /** 状态常量：开始说话 */
    public static final int STATE_BEGIN = 1;
    /** 状态常量：说话结束 */
    public static final int STATE_END = 2;
    /** 状态常量：开始识别 */
    public static final int STATE_START = 3;
    /** 状态常量：取消 */
    public static final int STATE_CANCEL = -3;

    /**
     * 构造语音输入管理器。
     * 根据配置初始化语音识别引擎（Vivo或系统识别器），设置音效和震动。
     * @param context TrimeService上下文
     */
    public Speech(TrimeService context) {
        this.context = context;
        // 从配置中读取识别服务名称（格式："vivo" 或 "包名/服务名"）
        String[] name = Function.getPref(context).getString("recognition_service", "vivo").split("/");
        if (name.length == 1) {
            // 单一名称：使用内置识别器或系统识别器
            if (name[0].equals("vivo")) {
                vSpeech = new VivoRecognizer(context, this); // 使用Vivo识别器
            } else {
                speech = SpeechRecognizer.createSpeechRecognizer(context);
                if (speech != null)
                    speech.setRecognitionListener(this); // 设置系统识别监听器
            }
        } else {
            // 指定组件名称：尝试创建指定服务的识别器
            try {
                Log.i(TAG, "Speech: " + name[1] + ":" + name[2]);
                speech = SpeechRecognizer.createSpeechRecognizer(context, new ComponentName(name[1], name[2]));
                if (speech != null)
                    speech.setRecognitionListener(this);
            } catch (Exception e) {
                e.printStackTrace();
                // 创建失败，回退到系统默认识别器
                speech = SpeechRecognizer.createSpeechRecognizer(context);
                if (speech != null)
                    speech.setRecognitionListener(this);
            }
        }
        // 初始化震动器
        mVibrator = (Vibrator) context.getSystemService(VIBRATOR_SERVICE);
        SharedPreferences pref = Function.getPref(context);
        duration = (int) (pref.getInt("key_vibrate_duration", duration) * 1.5); // 震动持续时间

        // 初始化音效池（最多同时播放4个音效）
        mSoundPool = new SoundPool(4, STREAM_MUSIC, 0);
        mSoundCancel = mSoundPool.load(context, R.raw.speech_recognition_cancel, 1);
        mSoundError = mSoundPool.load(context, R.raw.speech_recognition_error, 1);
        mSoundStart = mSoundPool.load(context, R.raw.speech_recognition_start, 1);
        mSoundSuccess = mSoundPool.load(context, R.raw.speech_recognition_success, 1);
        mSoundEnd = mSoundPool.load(context, R.raw.speech_speech_end, 1);
    }

    /**
     * 获取可用的语音识别服务列表。
     * @param context 上下文对象
     * @return 服务名称到组件名称的映射表
     */
    private HashMap<String, ComponentName> getService(Context context) {
        HashMap<String, ComponentName> appMap = new HashMap<>();
        ArrayList<String> list = new ArrayList<String>();
        PackageManager manager = context.getPackageManager();
        Intent mainIntent = new Intent(RecognitionService.SERVICE_INTERFACE); // 查询识别服务
        List<ResolveInfo> apps = manager.queryIntentServices(mainIntent, 0);
        Collections.sort(apps, new ResolveInfo.DisplayNameComparator(manager));

        int count = apps.size();
        for (int i = 0; i < count; i++) {
            ResolveInfo info = apps.get(i);
            CharSequence title = info.loadLabel(manager); // 服务显示名称
            ComponentName componentName = new ComponentName(
                    info.serviceInfo.applicationInfo.packageName,
                    info.serviceInfo.name);
            appMap.put(title.toString(), componentName); // 存入映射表
        }
        return appMap;
    }

    /**
     * 播放指定音效并触发震动。
     * @param id 音效ID（mSoundCancel/mSoundError等）
     */
    private void playSound(int id) {
        mSoundPool.play(id, 0.5f, 0.5f, 0, 0, 1); // 播放音效（左右声道各50%音量）
        vibrate(); // 同时触发震动
    }

    /**
     * 触发震动反馈。
     */
    public void vibrate() {
        if (mVibrator != null) {
            mVibrator.cancel(); // 取消之前的震动
            mVibrator.vibrate(duration); // 触发新震动
        }
    }

    /**
     * 显示提示消息（Toast）。
     * @param text 提示文本
     */
    private void alert(String text) {
        Toast.makeText(context, text, Toast.LENGTH_SHORT).show();
    }

    /**
     * 开始语音识别。
     * 根据当前状态决定是开始监听、取消还是停止。
     */
    public void start() {
        if (speech == null && vSpeech == null) {
            alert("未正确设置识别引擎");
            playSound(mSoundError);
            return;
        }
        Log.i(TAG, "start " + state);
        switch (state) {
            case STATE_BEGIN:
                stop(); // 正在开始，先停止
                break;
            case STATE_START:
            case STATE_READY:
            case STATE_END:
                cancel(); // 已开始或就绪，先取消
                break;
            default:
                startListening(); // 默认开始监听
        }
    }

    /**
     * 开始监听语音输入。
     * 根据使用的识别器类型（Vivo或系统）调用对应方法。
     */
    private void startListening() {
        if (vSpeech != null) {
            vSpeech.startInputting(); // Vivo识别器开始输入
            state = STATE_START;
        } else if (speech != null) {
            // 配置系统识别意图
            recognizerIntent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
            recognizerIntent.putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1); // 只返回最佳结果
            speech.startListening(recognizerIntent);
            state = STATE_START;
        }
    }

    /**
     * 取消语音识别。
     * 播放取消音效，触发震动，并调用对应识别器的取消方法。
     */
    public void cancel() {
        playSound(mSoundCancel);
        state = STATE_CANCEL;
        vibrate();
        if (vSpeech != null)
            vSpeech.cancel();
        if (speech != null)
            speech.cancel();
    }

    /**
     * 停止语音识别（完成输入）。
     * 触发震动，并调用对应识别器的停止方法。
     */
    public void stop() {
        Log.i(TAG, "stop");
        vibrate();
        if (vSpeech != null)
            vSpeech.stop();
        if (speech != null)
            speech.stopListening();
    }

    /**
     * 销毁语音识别器，释放资源。
     */
    public void destroy() {
        if (vSpeech != null)
            vSpeech.cancel();
        if (speech != null)
            speech.cancel();
        if (speech != null)
            speech.destroy();
        if (vSpeech != null)
            vSpeech.destroy();
        vSpeech = null;
        speech = null;
    }

    // RecognitionListener 接口实现

    @Override
    public void onBeginningOfSpeech() {
        state = STATE_BEGIN; // 用户开始说话
        Log.i(TAG, "onBeginningOfSpeech");
    }

    @Override
    public void onBufferReceived(byte[] buffer) {
        // 接收到音频数据缓冲区（暂未使用）
    }

    @Override
    public void onEndOfSpeech() {
        state = STATE_END; // 用户停止说话
        Log.i(TAG, "onEndOfSpeech");
        playSound(mSoundEnd); // 播放结束音效
    }

    @Override
    public void onError(int errorCode) {
        state = STATE_ERROR;
        String errorMessage = getErrorText(errorCode); // 获取错误信息
        alert(errorMessage);
        playSound(mSoundError); // 播放错误音效
    }

    @Override
    public void onEvent(int arg0, Bundle arg1) {
        Log.i(TAG, "onEvent");
    }

    @Override
    public void onPartialResults(Bundle arg0) {
        Log.i(TAG, "onPartialResults"); // 部分识别结果（暂未使用）
    }

    @Override
    public void onReadyForSpeech(Bundle arg0) {
        state = STATE_READY; // 准备好开始说话
        Log.i(TAG, "onReadyForSpeech");
        playSound(mSoundStart); // 播放开始音效
        vibrate(); // 触发震动
    }

    @Override
    public void onResults(Bundle results) {
        if (speech == null)
            return;
        playSound(mSoundSuccess); // 播放成功音效
        state = STATE_DONE;
        TrimeService trime = TrimeService.getInstance();
        if (trime == null) return;
        // 获取识别结果列表
        ArrayList<String> matches = results.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION);
        boolean s2t2 = Function.getPref(context).getBoolean("voice_input_s2t", false);
        boolean s2t = !Rime.getRimeOption("simplification"); // 是否简体中文模式
        if (matches != null) {
            for (String result : matches) {
                // 根据配置进行简繁转换
                if (s2t2) {
                    if (s2t)
                        result = OpenCCDictManager.convertLine(result, "s2t.json"); // 简体转繁体
                    else
                        result = OpenCCDictManager.convertLine(result, "t2s.json"); // 繁体转简体
                }
                // 调用主题管理器中的onSpeechResults函数（如果定义）
                Object ret = ThemeManager.callFunction("onSpeechResults", result);
                if(ret!=null){
                    if (ret.equals(Boolean.FALSE))
                        return; // 返回false则不提交
                    if (ret instanceof String)
                        result = ret.toString(); // 使用返回值作为新结果
                }
                trime.commitText(result); // 提交识别结果
            }
        }
    }

    @Override
    public void onRmsChanged(float rmsdB) {
        // 音量能量变化（暂未使用）
    }

    /**
     * 获取错误码对应的错误信息（简体中文）。
     * @param errorCode 错误码
     * @return 错误信息字符串
     */
    private static String getErrorText(int errorCode) {
        String message;
        switch (errorCode) {
            case SpeechRecognizer.ERROR_AUDIO:
                message = "音频错误";
                break;
            case SpeechRecognizer.ERROR_CLIENT:
                message = "客户端错误";
                break;
            case SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS:
                message = "权限不足";
                break;
            case SpeechRecognizer.ERROR_NETWORK:
                message = "网络错误";
                break;
            case SpeechRecognizer.ERROR_NETWORK_TIMEOUT:
                message = "网络超时";
                break;
            case SpeechRecognizer.ERROR_NO_MATCH:
                message = "未能识别";
                break;
            case SpeechRecognizer.ERROR_RECOGNIZER_BUSY:
                message = "识别服务忙";
                break;
            case SpeechRecognizer.ERROR_SERVER:
                message = "服务器错误";
                break;
            case SpeechRecognizer.ERROR_SPEECH_TIMEOUT:
                message = "无语音输入";
                break;
            default:
                message = "未知错误";
                break;
        }
        return message;
    }

    public int getState() {
        return state;
    }

    // RecognizerListener 接口实现（Vivo识别器回调）

    @Override
    public void onReady() {
        state = STATE_READY;
        Log.i(TAG, "onReadyForSpeech");
        playSound(mSoundStart);
        vibrate();
    }

    @Override
    public void onBegin() {
        onBeginningOfSpeech(); // 代理到系统识别器的回调
    }

    @Override
    public void onEnd() {
        onEndOfSpeech(); // 代理到系统识别器的回调
    }

    @Override
    public void onResult(String result) {
        if (vSpeech == null && speech == null)
            return;
        playSound(mSoundSuccess);
        state = STATE_DONE;
        TrimeService trime = TrimeService.getInstance();
        if (trime == null) return;
        boolean s2t2 = Function.getPref(context).getBoolean("voice_input_s2t", false);
        boolean s2t = !Rime.getRimeOption("simplification");
        if (result != null) {
            if (s2t2) {
                if (s2t)
                    result = OpenCCDictManager.convertLine(result, "s2t.json");
                else
                    result = OpenCCDictManager.convertLine(result, "t2s.json");
            }
            Object ret = ThemeManager.callFunction("onSpeechResults", result);
            if(ret!=null){
                if (ret.equals(Boolean.FALSE))
                    return;
                if (ret instanceof String)
                    result = ret.toString();
            }
            trime.commitText(result);
        }
    }

    @Override
    public void onError(String msg) {
        state = STATE_ERROR;
        alert(msg);
        playSound(mSoundError);
    }
}
