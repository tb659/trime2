package com.osfans.trime.speech;

/**
 * 语音识别器接口,定义语音识别的基本操作。
 * 支持多种语言(普通话、粤语、英语)的语音输入和识别。
 */
public interface Recognizer {
    // ==================== 语言常量 ====================
    /** 简体中文(中国大陆) */
    public String zh_CN="zh_CN";
    /** 粤语(广东话) */
    public String zh_GD="zh_GD";
    /** 英语(英国) */
    public String en_GB="en_GB";

    // ==================== 核心方法 ====================
    /**
     * 开始监听语音输入。
     * 启动语音识别引擎,持续监听直到检测到语音结束或超时。
     */
    public void startListening();

    /**
     * 开始语音输入模式。
     * 与 startListening 类似,但可能使用不同的 VAD(语音活动检测)参数。
     */
    public void startInputting();

    /**
     * 停止语音识别。
     * 正常结束当前的语音识别会话。
     */
    public void stop();

    /**
     * 取消语音识别。
     * 立即中止当前的语音识别会话,不返回结果。
     */
    public void cancel();

    /**
     * 销毁识别器资源。
     * 释放语音识别引擎占用的所有资源。
     */
    public void destroy();

    /**
     * 更新用户数据。
     * 用于更新个性化词库或热更新热词。
     */
    public void updateUserData();

    /**
     * 设置识别语言。
     *
     * @param language 语言代码,如 zh_CN、zh_GD、en_GB。
     */
    public void setLanguage(String language);

}
