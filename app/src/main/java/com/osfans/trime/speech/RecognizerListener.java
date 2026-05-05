package com.osfans.trime.speech;

/**
 * 语音识别监听器接口,用于接收语音识别过程中的各种事件回调。
 */
public interface RecognizerListener {
    /**
     * 当语音识别器准备就绪时调用。
     * 表示录音设备已准备好,可以开始录音。
     */
    public void onReady();

    /**
     * 当检测到语音开始时调用。
     * 表示用户已开始说话。
     */
    public void onBegin();

    /**
     * 当语音结束时调用。
     * 表示 VAD(语音活动检测)检测到用户停止说话。
     */
    public void onEnd();

    /**
     * 当识别出文本结果时调用。
     *
     * @param text 识别出的文本内容。
     */
    public void onResult(String text);

    /**
     * 当发生错误时调用。
     *
     * @param msg 错误信息。
     */
    public void onError(String msg);

}
