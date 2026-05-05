package com.osfans.trime;

import android.text.TextUtils;
import android.util.Base64;
import android.util.Log;

import com.androlua.AsyncTaskX;
import com.osfans.trime.util.HttpUtil;

import org.jetbrains.annotations.NotNull;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.Headers;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

/**
 * Vivo AI GPT API调用类
 * 
 * <p>提供与Vivo AI平台交互的功能，支持流式和非流式文本生成。
 * 包含API认证、签名生成、HTTP请求发送和响应处理等完整流程。</p>
 * 
 * <p>主要功能：
 * <ul>
 *   <li>生成随机字符串（用于nonce）</li>
 *   <li>生成规范查询字符串（用于签名）</li>
 *   <li>使用HmacSHA256算法生成请求签名</li>
 *   <li>生成API认证头（包含签名等信息）</li>
 *   <li>支持流式API调用（SSE，Server-Sent Events）</li>
 *   <li>支持非流式API调用（同步获取完整响应）</li>
 * </ul></p>
 * 
 * @author Rime社区
 * @version 1.0
 */
public class VivoGpt {

    /** UTF-8字符集常量 */
    private static final Charset UTF8 = StandardCharsets.UTF_8;
    
    /** 日志标签 */
    private static String TAG = "VivoGpt";
    
    /** API应用ID，从BuildConfig获取 */
    private static final String appId = BuildConfig.API_ID;
    
    /** API密钥，从BuildConfig获取 */
    private static final String appKey = BuildConfig.API_KEY;

    // ============================================================
    // 1. 辅助方法：随机字符串、签名生成
    // ============================================================

    /**
     * 生成指定长度的随机字符串
     * 
     * <p>使用大小写字母和数字生成随机字符串，常用于生成nonce（一次性随机数）。</p>
     * 
     * @param len 要生成的随机字符串长度
     * @return 随机字符串
     */
    private static String generateRandomString(int len) {
        String chars = "0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz";
        Random rnd = new Random();
        StringBuilder sb = new StringBuilder(len);
        for (int i = 0; i < len; i++)
            sb.append(chars.charAt(rnd.nextInt(chars.length())));
        return sb.toString();
    }

    /**
     * 生成规范查询字符串
     * 
     * <p>将查询参数按key排序后拼接成规范格式，用于API签名。</p>
     * 
     * @param queryParams 查询参数字符串（格式：key1=value1&key2=value2）
     * @return 规范查询字符串，如果输入为空则返回空字符串
     * @throws UnsupportedEncodingException URL编码失败
     */
    private static String generateCanonicalQueryString(String queryParams) throws UnsupportedEncodingException {
        if (queryParams == null || queryParams.length() <= 0) {
            return "";
        }

        // 解析查询参数为Map
        HashMap<String, String> params = new HashMap<>();
        String[] param = queryParams.split("&");
        for (String item : param) {
            String[] pair = item.split("=");
            if (pair.length == 2) {
                params.put(pair[0], pair[1]);
            } else {
                params.put(pair[0], "");
            }
        }
        // 按key排序（TreeSet自动排序）
        SortedSet<String> keys = new TreeSet<>(params.keySet());
        StringBuilder sb = new StringBuilder();
        boolean first = true;
        for (String key : keys) {
            if (!first) {
                sb.append("&");
            }
            // URL编码key和value
            String item = URLEncoder.encode(key) + "=" + URLEncoder.encode(params.get(key));
            sb.append(item);
            first = false;
        }

        return sb.toString();
    }

    /**
     * 使用HmacSHA256算法生成签名
     * 
     * <p>将签名字符串使用API密钥进行HmacSHA256加密，然后Base64编码。</p>
     * 
     * @param appKey API密钥
     * @param signingString 要签名的字符串
     * @return Base64编码的签名字符串，如果失败则返回空字符串
     */
    private static String generateSignature(String appKey, String signingString) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            SecretKeySpec secret = new SecretKeySpec(appKey.getBytes(UTF8), mac.getAlgorithm());
            mac.init(secret);
            return android.util.Base64.encodeToString(mac.doFinal(signingString.getBytes()), Base64.DEFAULT);
        } catch (Exception err) {
            Log.w(TAG, "create sign exception", err);
            return "";
        }
    }

    // ============================================================
    // 2. 认证头生成
    // ============================================================

    /**
     * 生成API认证头
     * 
     * <p>生成Vivo AI网关所需的认证头，包含：
     * <ul>
     *   <li>X-AI-GATEWAY-APP-ID: 应用ID</li>
     *   <li>X-AI-GATEWAY-TIMESTAMP: 时间戳</li>
     *   <li>X-AI-GATEWAY-NONCE: 随机数</li>
     *   <li>X-AI-GATEWAY-SIGNED-HEADERS: 已签名的头列表</li>
     *   <li>X-AI-GATEWAY-SIGNATURE: 签名</li>
     * </ul></p>
     * 
     * @param appId 应用ID
     * @param appKey API密钥
     * @param method HTTP方法（如POST）
     * @param uri 请求URI
     * @param queryParams 查询参数
     * @return 包含认证信息的头Map
     * @throws UnsupportedEncodingException URL编码失败
     */
    public static HashMap<String,String> generateAuthHeaders(String appId, String appKey, String method, String uri, String queryParams)
            throws UnsupportedEncodingException {
        // 生成随机数和时间戳
        String nonce = generateRandomString(8);
        String timestamp = String.valueOf(System.currentTimeMillis() / 1000);
        String canonicalQueryString = generateCanonicalQueryString(queryParams);
        // 构建待签名的头字符串
        String signedHeadersString = String.format("x-ai-gateway-app-id:%s\n" +
                "x-ai-gateway-timestamp:%s\nx-ai-gateway-nonce:%s", appId, timestamp, nonce);
        
        // 构建完整的签名字符串（包含方法、URI、查询字符串、应用ID、时间戳、随机数、已签名头）
        String[] fields = {
                method,
                uri,
                canonicalQueryString,
                appId,
                timestamp,
                nonce,
                signedHeadersString
        };
        final StringBuilder buf = new StringBuilder(fields.length * 16);
        for (int i = 0; i < fields.length; i++) {
            if (i > 0) {
                buf.append("\n");
            }
            if (fields[i] != null) {
                buf.append(fields[i]);
            }
        }
        
        // 生成认证头
        HashMap<String,String> headers = new HashMap<>();
        headers.put("X-AI-GATEWAY-APP-ID", appId.toString());
        headers.put("X-AI-GATEWAY-TIMESTAMP", timestamp.toString());
        headers.put("X-AI-GATEWAY-NONCE", nonce.toString());
        headers.put("X-AI-GATEWAY-SIGNED-HEADERS", "x-ai-gateway-app-id;x-ai-gateway-timestamp;x-ai-gateway-nonce");
        headers.put("X-AI-GATEWAY-SIGNATURE", generateSignature(appKey, buf.toString()));
        return headers;
    }

    // ============================================================
    // 3. 流式API调用（SSE - Server-Sent Events）
    // ============================================================

    /**
     * 调用Vivo GPT（流式，异步）
     * 
     * <p>使用流式API进行文本生成，通过回调返回结果。
     * 内部调用vivogpt方法发送HTTP请求。</p>
     * 
     * @param s 输入提示文本
     * @param callback HTTP回调接口
     */
    public static void gpt(String s, HttpUtil.HttpCallback callback){
        try {
            vivogpt(s, callback);
        } catch (Exception e) {
            e.printStackTrace();
            callback.onDone(new HttpUtil.HttpResult(e.toString()));
        }
    }

    /**
     * Vivo流式GPT API调用实现
     * 
     * <p>发送POST请求到Vivo AI的流式补全接口，使用SSE（Server-Sent Events）接收流式响应。
     * 每行以"data:"开头，解析JSON获取生成的文本。</p>
     * 
     * @param s 输入提示文本
     * @param callback HTTP回调接口
     * @return 始终返回null（结果通过回调返回）
     * @throws Exception 请求或解析异常
     */
    public static String vivogpt(String s, HttpUtil.HttpCallback callback) throws Exception {

        String URI = "/vivogpt/completions/stream";
        String DOMAIN = "api-ai.vivo.com.cn";
        String METHOD = "POST";
        UUID requestId = UUID.randomUUID();
        Log.w("vivogpt", "vivogpt: "+requestId );

        // 构建请求参数
        Map<String, Object> map = new HashMap<>();
        map.put("requestId", requestId.toString());
        String queryStr = mapToQueryString(map);

        // 构建请求体
        Map<String, String> data = new HashMap<>();
        data.put("prompt", s);
        data.put("model", "vivo-BlueLM-TB-Pro");
        UUID sessionId = UUID.randomUUID();
        data.put("sessionId", sessionId.toString());

        // 生成认证头
        HashMap<String,String> headers = generateAuthHeaders(appId, appKey, METHOD, URI, queryStr);
        headers.put("Content-Type", "application/json");
        String url = String.format("https://%s%s?%s", DOMAIN, URI, queryStr);

        // 创建HTTP客户端（设置超时5分钟）
        OkHttpClient client = new OkHttpClient().newBuilder()
                .connectTimeout(5, TimeUnit.MINUTES)
                .readTimeout(5, TimeUnit.MINUTES)
                .build();
        Request request = new Request.Builder()
                .url(url)
                .post(RequestBody.create(okhttp3.MediaType.parse("application/json"), new JSONObject(data).toString()))
                .headers(Headers.of(headers))
                .build();
        
        // 异步发送请求（使用队列）
        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(@NotNull Call call, @NotNull IOException e) {
                callback.onDone(new HttpUtil.HttpResult(e.toString()));
                callback.onDone(null);
            }

            @Override
            public void onResponse(@NotNull Call call, @NotNull Response response) throws IOException {
                BufferedReader stream = new BufferedReader(new InputStreamReader(response.body().byteStream()));
                String line = stream.readLine();
                while (line!=null){
                    try {
                        if(!TextUtils.isEmpty(line)) {
                            if(line.startsWith("event")) {
                                callback.onDone(null); // 事件行，忽略
                                break;
                            }
                            line = line.substring(5); // 去掉"data:"前缀
                            Log.w(TAG, "onResponse:line " + line);
                            String r = new JSONObject(line).getString("message");
                            callback.onDone(new HttpUtil.HttpResult(r));
                        }
                    } catch (JSONException e) {
                        callback.onDone(null);
                        break;
                    }
                    line = stream.readLine();
                }
            }
        });
        return null;
    }

    // ============================================================
    // 4. 非流式API调用（同步获取完整响应）
    // ============================================================

    /**
     * 调用Vivo GPT（非流式，异步）
     * 
     * <p>使用非流式API进行文本生成，通过回调返回完整结果。
     * 内部使用AsyncTaskX执行后台请求。</p>
     * 
     * @param s 输入提示文本
     * @param callback HTTP回调接口
     */
    public static void gpt1(String s, HttpUtil.HttpCallback callback){
        new AsyncTaskX<String,String,String>(){
            @Override
            protected String doInBackground(String... strings) {
                try {
                    return vivogpt1(s);
                } catch (Exception e) {
                    return e.toString();
                }
            }

            @Override
            protected void onPostExecute(String s) {
                super.onPostExecute(s);
                Log.w(TAG, "onPostExecute: "+s );
                try {
                    s=new JSONObject(s).getJSONObject("data").getString("content");
                } catch (JSONException e) {
                }
                callback.onDone(new HttpUtil.HttpResult(s));
            }
        }.execute();
    }

    /**
     * Vivo非流式GPT API调用实现
     * 
     * <p>发送POST请求到Vivo AI的补全接口，同步获取完整响应。
     * 响应包含在JSON的data.content字段中。</p>
     * 
     * @param s 输入提示文本
     * @return 响应字符串（JSON格式）
     * @throws Exception 请求或解析异常
     */
    public static String vivogpt1(String s) throws Exception {
        String URI = "/vivogpt/completions";
        String DOMAIN = "api-ai.vivo.com.cn";
        String METHOD = "POST";
        UUID requestId = UUID.randomUUID();
        Log.w("vivogpt", "vivogpt: "+requestId );

        Map<String, Object> map = new HashMap<>();
        map.put("requestId", requestId.toString());
        String queryStr = mapToQueryString(map);

        // 构建请求体
        Map<String, String> data = new HashMap<>();
        data.put("prompt", s);
        data.put("model", "vivo-BlueLM-TB-Pro");
        UUID sessionId = UUID.randomUUID();
        data.put("sessionId", sessionId.toString());

        HashMap<String,String> headers = generateAuthHeaders(appId, appKey, METHOD, URI, queryStr);
        headers.put("Content-Type", "application/json");
        String url = String.format("https://%s%s?%s", DOMAIN, URI, queryStr);

        OkHttpClient client = new OkHttpClient().newBuilder()
                .connectTimeout(5, TimeUnit.MINUTES)
                .readTimeout(5, TimeUnit.MINUTES)
                .build();
        Request request = new Request.Builder()
                .url(url)
                .post(RequestBody.create(okhttp3.MediaType.parse("application/json"), new JSONObject(data).toString()))
                .headers(Headers.of(headers))
                .build();
        
        // 同步发送请求
        Response response = client.newCall(request).execute();
        if (response.isSuccessful()) {
            return response.body().string();
        } else {
            throw new IOException("Unexpected code " + response);
        }
    }

    /**
     * 调用Vivo GPT（非流式，多轮对话，异步）
     * 
     * <p>支持多轮对话的API调用，传入对话历史列表。
     * 使用AsyncTaskX执行后台请求。</p>
     * 
     * @param s 对话历史列表（每项为一条消息）
     * @param callback HTTP回调接口
     */
    public static void gpt1(ArrayList<String> s, HttpUtil.HttpCallback callback){
        new AsyncTaskX<String,String,String>(){
            @Override
            protected String doInBackground(String... strings) {
                try {
                    return vivogpt1(s);
                } catch (Exception e) {
                    return e.toString();
                }
            }

            @Override
            protected void onPostExecute(String s) {
                super.onPostExecute(s);
                Log.w(TAG, "onPostExecute: "+s );
                try {
                    s=new JSONObject(s).getJSONObject("data").getString("content");
                } catch (JSONException e) {
                }
                callback.onDone(new HttpUtil.HttpResult(s));
            }
        }.execute();
    }

    /**
     * Vivo非流式GPT API调用实现（多轮对话版本）
     * 
     * <p>支持多轮对话，传入消息列表（包含role和content）。
     * 使用messages字段而非单个prompt。</p>
     * 
     * @param s 对话历史列表（每项为一条消息文本）
     * @return 响应字符串（JSON格式）
     * @throws Exception 请求或解析异常
     */
    public static String vivogpt1(ArrayList<String> s) throws Exception {
        String URI = "/vivogpt/completions";
        String DOMAIN = "api-ai.vivo.com.cn";
        String METHOD = "POST";
        UUID requestId = UUID.randomUUID();
        Log.w("vivogpt", "vivogpt: "+requestId );

        Map<String, Object> map = new HashMap<>();
        map.put("requestId", requestId.toString());
        String queryStr = mapToQueryString(map);

        // 构建请求体（包含多轮对话消息）
        Map<String, Object> data = new HashMap<>();
        ArrayList<Map<String, String>> msg = new ArrayList<>();
        for (int i = 0; i < s.size(); i++) {
            Map<String, String> m = new HashMap<>();
            m.put("role", i%2==0?"user":"assistant"); // 交替设置角色
            m.put("content", s.get(i));
            msg.add(m);
        }
        data.put("messages", msg);
        data.put("model", "vivo-BlueLM-TB-Pro");
        UUID sessionId = UUID.randomUUID();
        data.put("sessionId", sessionId.toString());

        HashMap<String,String> headers = generateAuthHeaders(appId, appKey, METHOD, URI, queryStr);
        headers.put("Content-Type", "application/json");
        String url = String.format("https://%s%s?%s", DOMAIN, URI, queryStr);

        OkHttpClient client = new OkHttpClient().newBuilder()
                .connectTimeout(5, TimeUnit.MINUTES)
                .readTimeout(5, TimeUnit.MINUTES)
                .build();
        Request request = new Request.Builder()
                .url(url)
                .post(RequestBody.create(okhttp3.MediaType.parse("application/json"), new JSONObject(data).toString()))
                .headers(Headers.of(headers))
                .build();
        
        // 同步发送请求
        Response response = client.newCall(request).execute();
        if (response.isSuccessful()) {
            return response.body().string();
        } else {
            throw new IOException("Unexpected code " + response);
        }
    }

    // ============================================================
    // 5. 工具方法：Map转查询字符串
    // ============================================================

    /**
     * 将Map转换为查询字符串
     * 
     * <p>将Map中的键值对拼接成URL查询字符串格式（key1=value1&key2=value2）。</p>
     * 
     * @param map 要转换的Map
     * @return 查询字符串，如果Map为空则返回空字符串
     */
    public static String mapToQueryString(Map<String, Object> map) {
        if (map.isEmpty()) {
            return "";
        }
        StringBuilder queryStringBuilder = new StringBuilder();
        for (Map.Entry<String, Object> entry : map.entrySet()) {
            if (queryStringBuilder.length() > 0) {
                queryStringBuilder.append("&");
            }
            queryStringBuilder.append(entry.getKey());
            queryStringBuilder.append("=");
            queryStringBuilder.append(entry.getValue());
        }
        return queryStringBuilder.toString();
    }
}
