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

import android.content.Context;
import android.content.SharedPreferences;
import android.media.SoundPool;
import android.os.Bundle;
import android.os.Vibrator;
import android.speech.RecognitionListener;
import android.util.Log;
import android.widget.Toast;

import com.osfans.trime.data.opencc.OpenCCDictManager;
import com.osfans.trime.speech.BaiduRecognizer;
import com.osfans.trime.speech.RecognizerListener;
import com.osfans.trime.theme.ThemeManager;
import com.osfans.trime.util.Function;
import com.osfans.trime.util.CustomToast;
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
 * 支持百度语音识别引擎。
 * 提供语音识别的开始、停止、取消功能，并处理识别结果（支持简繁转换）。
 * 包含音效播放和震动反馈功能。
 */
class Speech implements RecognizerListener {
    /** 结果ID的键名（用于Intent传递） */
    public static final String RES_ID = "RES_ID";
    /** 名称的键名（用于Intent传递） */
    public static final String NAME = "NAME";
    /** 配置键名 */
    public static final String config = "config";
    /** 日志标签 */
    private static final String TAG = "Speech";

    /** 音效池，用于播放语音识别相关的音效 */
    private final SoundPool mSoundPool;
    /** 百度语音识别器实例 */
    private BaiduRecognizer bSpeech;
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
     * 初始化百度语音识别引擎，设置音效和震动。
     * @param context TrimeService上下文
     */
    public Speech(TrimeService context) {
        this.context = context;
        // 使用百度语音识别引擎
        bSpeech = new BaiduRecognizer(context, this);
        
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
     * 播放指定音效并触发震动。
     * @param id 音效ID（mSoundCancel/mSoundError等）
     */
    private void playSound(int id) {
        if (mSoundPool == null) {
            Log.w(TAG, "playSound: SoundPool is null, skip");
            return;
        }
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
        CustomToast.show(context, text, Toast.LENGTH_LONG, true);
    }

    /**
     * 开始语音识别。
     * 根据当前状态决定是开始监听、取消还是停止。
     */
    public void start() {
        if (bSpeech == null) {
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
     * 调用百度识别器的startInputting方法。
     */
    private void startListening() {
        if (bSpeech != null) {
            bSpeech.startInputting(); // 百度识别器开始输入
            state = STATE_START;
        }
    }

    /**
     * 取消语音识别。
     * 播放取消音效，触发震动，并调用百度识别器的取消方法。
     */
    public void cancel() {
        playSound(mSoundCancel);
        state = STATE_CANCEL;
        vibrate();
        if (bSpeech != null) bSpeech.cancel();
    }

    /**
     * 停止语音识别（完成输入）。
     * 触发震动，并调用百度识别器的停止方法。
     */
    public void stop() {
        Log.i(TAG, "stop");
        vibrate();
        if (bSpeech != null) bSpeech.stop();
    }

    /**
     * 销毁语音识别器，释放资源。
     */
    public void destroy() {
        if (bSpeech != null) {
            bSpeech.cancel();
            bSpeech = null;
        }
    }

    public int getState() {
        return state;
    }

    // RecognizerListener 接口实现（百度识别器回调）

    @Override
    public void onReady() {
        state = STATE_READY;
        Log.i(TAG, "onReadyForSpeech");
        playSound(mSoundStart);
        vibrate();
    }

    @Override
    public void onBegin() {
        state = STATE_BEGIN; // 用户开始说话
    }

    @Override
    public void onEnd() {
        state = STATE_END; // 用户停止说话
    }

    @Override
    public void onResult(String result) {
        Log.i(TAG, "onResult:" + result);
        if (bSpeech == null)
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
