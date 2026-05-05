/*
 * SPDX-FileCopyrightText: 2015 - 2026 Rime community
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package com.osfans.trime.util;

import android.util.Log;

import org.json.JSONArray;
import org.json.JSONException;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 云端输入辅助类,提供拼音云预测和搜索建议功能。
 * 集成了百度、Google 等云输入法 API,用于增强本地输入的准确性。
 */
public class Cloud {
    // 百度云输入法 API URL
    private static final String url = "https://olime.baidu.com/py?inputtype=py&bg=0&ed=20&result=hanzi&resultcoding=utf-8&ch_en=0&clientinfo=web&version=1&input=";
    // Google 云输入法 API URL(国内版)
    private static final String url2 = "https://www.google.cn/inputtools/request?ime=pinyin&text=";
    // 百度搜索建议 API URL
    private static final String url3 = "http://suggestion.baidu.com/su?p=3&cb=window.bdsug.sug&wd=";
    // 正则表达式:匹配百度 API 返回的候选词
    private static final Pattern p = Pattern.compile("\\[\"([^\"]*)");
    // 正则表达式:匹配百度搜索建议返回的结果
    private static final Pattern p2 = Pattern.compile("\"([^\",:]*)\"");
    // HTTP GET 任务对象(用于取消之前的请求)
    private static HttpUtil.HttpTask sTask;
    // HTTP GET 任务对象2(用于取消之前的请求)
    private static HttpUtil.HttpTask sTask2;
    // 缓存已获取的云端结果,避免重复请求
    private static HashMap<String, String> cache = new HashMap<>();

    /**
     * 从云端获取拼音预测结果。
     * 同时调用 Google 和百度两个云输入法 API,合并结果后返回。
     *
     * @param py 拼音字符串。
     * @param callback 回调接口,接收云端返回的候选词列表。
     */
    public static void get(final String py, final CloudCallback callback) {
        // 取消之前未完成的请求,避免资源浪费
        if (sTask != null)
            sTask.cancel();
        if (sTask2 != null)
            sTask2.cancel();
        sTask = null;
        sTask2 = null;
        // 存储本地已有的候选词,用于去重
        final ArrayList<String> ss = new ArrayList<>();
        // 首先调用 Google 云输入法 API
        sTask = HttpUtil.get(url2 + py, new HttpUtil.HttpCallback() {
            @Override
            public void onDone(HttpUtil.HttpResult result) {
                final ArrayList<String> list = new ArrayList<>();
                if (result.code == 200) {
                    try {
                        // 解析 JSON 响应,提取第一个候选词
                        String sText = new JSONArray(result.text).getJSONArray(1).getJSONArray(0).getJSONArray(1).getString(0);
                        // 如果候选词长度大于1且不在本地列表中,则添加到结果
                        if (sText.length() > 1) {
                            if(!ss.contains(sText)) {
                                list.add(sText);
                            }
                         }
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                }
                sTask = null;
                // Google API 完成后,继续调用百度 API
                sTask2 = HttpUtil.get(url + py, new HttpUtil.HttpCallback() {
                    @Override
                    public void onDone(HttpUtil.HttpResult result) {
                        // 解析百度 API 返回的结果
                        if (result.code == 200) {
                            Matcher m = p.matcher(result.text);
                            while (m.find()) {
                                String g = m.group(1);
                                Log.i("rime", "onDone:3 "+g);
                                // 跳过已在本地候选词中的结果
                                if(ss.contains(g))
                                    continue;
                                // 去重后添加到结果列表
                                if (!list.contains(g))
                                    list.add(g);
                            }
                        }
                        // 返回合并后的云端候选词列表
                        callback.onDone(list);
                        sTask2 = null;
                    }
                });
            }
        });
    }

    /**
     * 从百度搜索建议 API 获取搜索关键词推荐。
     * 用于在用户输入时提供实时的搜索建议。
     *
     * @param py 搜索关键词或拼音。
     * @param callback 回调接口,接收搜索建议列表。
     */
    public static void sug(String py, final CloudCallback callback) {
        Log.i("rime", "onDone:sug1 " + py);
        sTask2 = HttpUtil.get(url3 + py, new HttpUtil.HttpCallback() {
            @Override
            public void onDone(HttpUtil.HttpResult result) {
                Log.i("rime", "onDone:sug " + result.text);
                final ArrayList<String> list = new ArrayList<>();
                if (result.code == 200) {
                    // 使用正则表达式提取搜索建议
                    Matcher m = p2.matcher(result.text);
                    while (m.find()) {
                        String g = m.group(1);
                        Log.i("rime", "onDone:3 "+g);
                        Log.i("rime", "onDone:4 "+list.contains(g));
                        // 去重处理
                        if(list.contains(g))
                            continue;
                        list.add(g);
                    }
                }
                callback.onDone(list);
                sTask2 = null;
            }
        });
    }

    /**
     * 云端回调接口,用于接收异步请求的结果。
     */
    public static interface CloudCallback {
        /**
         * 当云端请求完成时调用,返回候选词列表。
         *
         * @param list 候选词列表。
         */
        public void onDone(ArrayList<String> list);

        /**
         * 当云端请求完成时调用,返回单个文本结果。
         *
         * @param text 文本结果。
         */
        public void onDone(String text);
    }
}
