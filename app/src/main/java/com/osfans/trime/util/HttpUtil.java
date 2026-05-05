package com.osfans.trime.util;

import android.util.Log;

import com.androlua.LuaUtil;
import com.androlua.AsyncTaskX;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.Charset;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * HTTP 网络请求工具类,提供 GET、POST、PUT、DELETE 等常用 HTTP 方法。
 * 支持异步请求、文件下载、表单提交、Cookie 管理等功能。
 */
public class HttpUtil {

    // 全局 HTTP 请求头(应用于所有请求)
    private static HashMap<String, String> sHeader;

    /**
     * 设置全局 HTTP 请求头。
     *
     * @param header 请求头键值对映射。
     */
    public static void setHeader(HashMap<String, String> header) {
        sHeader = header;
    }

    /**
     * 获取全局 HTTP 请求头。
     *
     * @return 请求头键值对映射。
     */
    public static HashMap<String, String> getHeader() {
        return sHeader;
    }

    /**
     * 发送 HTTP GET 请求。
     *
     * @param url 请求 URL。
     * @param callback 回调接口,接收请求结果。
     * @return HttpTask 任务对象,可用于取消请求。
     */
    public static HttpTask get(String url, HttpCallback callback) {
        HttpTask task = new HttpTask(url, "GET", null, null, null, callback);
        task.executeOnExecutor(AsyncTaskX.THREAD_POOL_EXECUTOR);
        return task;
    }

    /**
     * 发送 HTTP GET 请求(带自定义请求头)。
     *
     * @param url 请求 URL。
     * @param header 自定义请求头。
     * @param callback 回调接口,接收请求结果。
     * @return HttpTask 任务对象。
     */
    public static HttpTask get(String url, HashMap<String, String> header, HttpCallback callback) {
        HttpTask task = new HttpTask(url, "GET", null, null, header, callback);
        task.executeOnExecutor(AsyncTaskX.THREAD_POOL_EXECUTOR);
        return task;
    }

    public static HttpTask get(String url, String cookie, HashMap<String, String> header, HttpCallback callback) {
        HttpTask task = cookie.matches("[\\w\\-]+") && Charset.isSupported(cookie) ? new HttpTask(url, "GET", null, cookie, header, callback) : new HttpTask(url, "GET", cookie, null, header, callback);
        task.executeOnExecutor(AsyncTaskX.THREAD_POOL_EXECUTOR);
        return task;
    }

    public static HttpTask get(String url, String cookie, HttpCallback callback) {
        HttpTask task = cookie.matches("[\\w\\-]+") && Charset.isSupported(cookie) ? new HttpTask(url, "GET", null, cookie, null, callback) : new HttpTask(url, "GET", cookie, null, null, callback);
        task.executeOnExecutor(AsyncTaskX.THREAD_POOL_EXECUTOR);
        return task;
    }

    public static HttpTask get(String url, String cookie, String charset, HttpCallback callback) {
        HttpTask task = new HttpTask(url, "GET", cookie, charset, null, callback);
        task.executeOnExecutor(AsyncTaskX.THREAD_POOL_EXECUTOR);
        return task;
    }

    public static HttpTask get(String url, String cookie, String charset, HashMap<String, String> header, HttpCallback callback) {
        HttpTask task = new HttpTask(url, "GET", cookie, charset, header, callback);
        task.executeOnExecutor(AsyncTaskX.THREAD_POOL_EXECUTOR);
        return task;
    }

    public static HttpTask download(String url, String data, HttpCallback callback) {
        HttpTask task = new HttpTask(url, "GET", null, null, null, callback);
        task.executeOnExecutor(AsyncTaskX.THREAD_POOL_EXECUTOR, data);
        return task;
    }

    public static HttpTask download(String url, String data, HashMap<String, String> header, HttpCallback callback) {
        HttpTask task = new HttpTask(url, "GET", null, null, header, callback);
        task.executeOnExecutor(AsyncTaskX.THREAD_POOL_EXECUTOR, data);
        return task;
    }

    public static HttpTask download(String url, String data, String cookie, HttpCallback callback) {
        HttpTask task = new HttpTask(url, "GET", cookie, null, null, callback);
        task.executeOnExecutor(AsyncTaskX.THREAD_POOL_EXECUTOR, data);
        return task;
    }

    public static HttpTask download(String url, String data, String cookie, HashMap<String, String> header, HttpCallback callback) {
        HttpTask task = new HttpTask(url, "GET", cookie, null, header, callback);
        task.executeOnExecutor(AsyncTaskX.THREAD_POOL_EXECUTOR, data);
        return task;
    }


    public static HttpTask delete(String url, HttpCallback callback) {
        HttpTask task = new HttpTask(url, "DELETE", null, null, null, callback);
        task.executeOnExecutor(AsyncTaskX.THREAD_POOL_EXECUTOR);
        return task;
    }

    public static HttpTask delete(String url, HashMap<String, String> header, HttpCallback callback) {
        HttpTask task = new HttpTask(url, "DELETE", null, null, header, callback);
        task.executeOnExecutor(AsyncTaskX.THREAD_POOL_EXECUTOR);
        return task;
    }

    public static HttpTask delete(String url, String cookie, HashMap<String, String> header, HttpCallback callback) {
        HttpTask task = cookie.matches("[\\w\\-]+") && Charset.isSupported(cookie) ? new HttpTask(url, "DELETE", null, cookie, header, callback) : new HttpTask(url, "DELETE", cookie, null, header, callback);
        task.executeOnExecutor(AsyncTaskX.THREAD_POOL_EXECUTOR);
        return task;
    }

    public static HttpTask delete(String url, String cookie, HttpCallback callback) {
        HttpTask task = cookie.matches("[\\w\\-]+") && Charset.isSupported(cookie) ? new HttpTask(url, "DELETE", null, cookie, null, callback) : new HttpTask(url, "DELETE", cookie, null, null, callback);
        task.executeOnExecutor(AsyncTaskX.THREAD_POOL_EXECUTOR);
        return task;
    }

    public static HttpTask delete(String url, String cookie, String charset, HttpCallback callback) {
        HttpTask task = new HttpTask(url, "DELETE", cookie, charset, null, callback);
        task.executeOnExecutor(AsyncTaskX.THREAD_POOL_EXECUTOR);
        return task;
    }

    public static HttpTask delete(String url, String cookie, String charset, HashMap<String, String> header, HttpCallback callback) {
        HttpTask task = new HttpTask(url, "DELETE", cookie, charset, header, callback);
        task.executeOnExecutor(AsyncTaskX.THREAD_POOL_EXECUTOR);
        return task;
    }


    public static HttpTask post(String url, String data, HttpCallback callback) {
        HttpTask task = new HttpTask(url, "POST", null, null, null, callback);
        task.executeOnExecutor(AsyncTaskX.THREAD_POOL_EXECUTOR, data);
        return task;
    }

    public static HttpTask post(String url, String data, HashMap<String, String> header, HttpCallback callback) {
        HttpTask task = new HttpTask(url, "POST", null, null, header, callback);
        task.executeOnExecutor(AsyncTaskX.THREAD_POOL_EXECUTOR, data);
        return task;
    }

    public static HttpTask post(String url, String data, String cookie, HttpCallback callback) {
        HttpTask task = cookie.matches("[\\w\\-]+") && Charset.isSupported(cookie) ? new HttpTask(url, "POST", null, cookie, null, callback) : new HttpTask(url, "POST", cookie, null, null, callback);
        task.executeOnExecutor(AsyncTaskX.THREAD_POOL_EXECUTOR, data);
        return task;
    }

    public static HttpTask post(String url, String data, String cookie, HashMap<String, String> header, HttpCallback callback) {
        HttpTask task = cookie.matches("[\\w\\-]+") && Charset.isSupported(cookie) ? new HttpTask(url, "POST", null, cookie, header, callback) : new HttpTask(url, "POST", cookie, null, header, callback);
        task.executeOnExecutor(AsyncTaskX.THREAD_POOL_EXECUTOR, data);
        return task;
    }

    public static HttpTask post(String url, String data, String cookie, String charset, HttpCallback callback) {
        HttpTask task = new HttpTask(url, "POST", cookie, charset, null, callback);
        task.executeOnExecutor(AsyncTaskX.THREAD_POOL_EXECUTOR, data);
        return task;
    }

    public static HttpTask post(String url, String data, String cookie, String charset, HashMap<String, String> header, HttpCallback callback) {
        HttpTask task = new HttpTask(url, "POST", cookie, charset, header, callback);
        task.executeOnExecutor(AsyncTaskX.THREAD_POOL_EXECUTOR, data);
        return task;
    }

    public static HttpTask post(String url, HashMap<String, String> data, HttpCallback callback) {
        return post(url, formatMap(data), callback);
    }

    public static HttpTask post(String url, HashMap<String, String> data, String cookie, HttpCallback callback) {
        return post(url, formatMap(data), cookie, callback);
    }

    public static HttpTask post(String url, HashMap<String, String> data, String cookie, HashMap<String, String> header, HttpCallback callback) {
        return post(url, formatMap(data), cookie, header, callback);
    }

    public static HttpTask post(String url, HashMap<String, String> data, String cookie, String charset, HttpCallback callback) {
        return post(url, formatMap(data), cookie, charset, callback);
    }

    public static HttpTask post(String url, HashMap<String, String> data, String cookie, String charset, HashMap<String, String> header, HttpCallback callback) {
        return post(url, formatMap(data), cookie, charset, header, callback);
    }

    /**
     * 将 HashMap 格式化为 URL 编码的表单数据。
     * 格式: key1=value1&key2=value2
     *
     * @param data 键值对数据。
     * @return 格式化后的字符串。
     */
    private static String formatMap(HashMap<String, String> data) {
        StringBuilder buf = new StringBuilder();
        for (Map.Entry<String, String> entry : data.entrySet()) {
            buf.append(entry.getKey()).append("=").append(entry.getValue()).append("&");
        }
        if (!data.isEmpty())
            buf.deleteCharAt(buf.length() - 1);
        return buf.toString();
    }

    // multipart/form-data 请求的分隔符
    private final static String boundary = "----q1w2e3r4t5y6u7i8o9p0a1s2d3f4g5h6j7k8l9z0x1c2v3b4n5m6";

    /**
     * 发送带文件上传的 POST 请求(multipart/form-data)。
     *
     * @param url 请求 URL。
     * @param data 表单数据。
     * @param file 文件映射(key: 字段名, value: 文件路径)。
     * @param cookie Cookie 字符串或字符集名称。
     * @param charset 字符集编码。
     * @param header 自定义请求头。
     * @param callback 回调接口。
     * @return HttpTask 任务对象。
     */
    public static HttpTask post(String url, HashMap<String, String> data, HashMap<String, String> file, String cookie, String charset, HashMap<String, String> header, HttpCallback callback) {
        if (header == null)
            header = new HashMap<>();
        // 设置 Content-Type 为 multipart/form-data
        header.put("Content-Type", "multipart/form-data;boundary=" + boundary);
        HttpTask task = new HttpTask(url, "POST", cookie, charset, header, callback);
        task.execute(new Object[]{formatMultiDate(data, file, charset)});
        return task;
    }

    /**
     * 格式化 multipart/form-data 请求体。
     * 将表单数据和文件数据组合成符合 HTTP 规范的 multipart 格式。
     *
     * @param data 表单数据。
     * @param file 文件映射。
     * @param charset 字符集编码。
     * @return 格式化后的字节数组。
     */
    private static byte[] formatMultiDate(HashMap<String, String> data, HashMap<String, String> file, String charset) {
        if (charset == null)
            charset = "UTF-8";
        ByteArrayOutputStream buff = new ByteArrayOutputStream();
        // 添加表单字段
        for (Map.Entry<String, String> entry : data.entrySet()) {
            try {
                buff.write(String.format("--%s\r\nContent-Disposition:form-data;name=\"%s\"\r\n\r\n%s\r\n", boundary, entry.getKey(), entry.getValue()).getBytes(charset));
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        // 添加文件字段
        for (Map.Entry<String, String> entry : file.entrySet()) {
            try {
                buff.write(String.format("--%s\r\nContent-Disposition:form-data;name=\"%s\";filename=\"%s\"\r\nContent-Type:application/octet-stream\r\n\r\n", boundary, entry.getKey(), entry.getValue()).getBytes(charset));
                buff.write(LuaUtil.readAll(new FileInputStream(entry.getValue())));
                buff.write("\r\n".getBytes(charset));
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        // 添加结束标记
        try {
            buff.write(String.format("--%s--\r\n", boundary).getBytes(charset));
        } catch (IOException e) {
            e.printStackTrace();
        }

        return buff.toByteArray();
    }

    public static HttpTask put(String url, String data, HttpCallback callback) {
        HttpTask task = new HttpTask(url, "PUT", null, null, null, callback);
        task.executeOnExecutor(AsyncTaskX.THREAD_POOL_EXECUTOR, data);
        return task;
    }

    public static HttpTask put(String url, String data, HashMap<String, String> header, HttpCallback callback) {
        HttpTask task = new HttpTask(url, "PUT", null, null, header, callback);
        task.executeOnExecutor(AsyncTaskX.THREAD_POOL_EXECUTOR, data);
        return task;
    }

    public static HttpTask put(String url, String data, String cookie, HttpCallback callback) {
        HttpTask task = cookie.matches("[\\w\\-]+") && Charset.isSupported(cookie) ? new HttpTask(url, "PUT", null, cookie, null, callback) : new HttpTask(url, "PUT", cookie, null, null, callback);
        task.executeOnExecutor(AsyncTaskX.THREAD_POOL_EXECUTOR, data);
        return task;
    }

    public static HttpTask put(String url, String data, String cookie, HashMap<String, String> header, HttpCallback callback) {
        HttpTask task = cookie.matches("[\\w\\-]+") && Charset.isSupported(cookie) ? new HttpTask(url, "PUT", null, cookie, header, callback) : new HttpTask(url, "PUT", cookie, null, header, callback);
        task.executeOnExecutor(AsyncTaskX.THREAD_POOL_EXECUTOR, data);
        return task;
    }

    public static HttpTask put(String url, String data, String cookie, String charset, HttpCallback callback) {
        HttpTask task = new HttpTask(url, "PUT", cookie, charset, null, callback);
        task.executeOnExecutor(AsyncTaskX.THREAD_POOL_EXECUTOR, data);
        return task;
    }

    public static HttpTask put(String url, String data, String cookie, String charset, HashMap<String, String> header, HttpCallback callback) {
        HttpTask task = new HttpTask(url, "PUT", cookie, charset, header, callback);
        task.executeOnExecutor(AsyncTaskX.THREAD_POOL_EXECUTOR, data);
        return task;
    }

    /**
     * HTTP 请求进度更新监听器接口。
     */
    public static interface UpdateListener {
        /**
         * 当请求进度更新时调用。
         *
         * @param values 进度信息数组。
         */
        public void onUpdate(String[] values);
    }

    /**
     * HTTP 异步任务类,继承自 AsyncTaskX。
     * 在后台线程执行 HTTP 请求,并在主线程回调结果。
     */
    public static class HttpTask extends AsyncTaskX<Object, String, HttpResult> {

        // 请求 URL
        private String mUrl;
        // 回调接口
        private HttpCallback mCallback;
        // 请求数据(POST/PUT 时使用)
        private byte[] mData;
        // 字符集编码
        private String mCharset;
        // Cookie 字符串
        private String mCookie;
        // 请求头
        private HashMap<String, String> mHeader;
        // HTTP 方法(GET/POST/PUT/DELETE)
        private String mMethod;
        // 进度更新监听器
        private UpdateListener mUpdateListener;

        /**
         * 构造函数。
         *
         * @param url 请求 URL。
         * @param method HTTP 方法。
         * @param cookie Cookie 字符串。
         * @param charset 字符集编码。
         * @param header 请求头。
         * @param callback 回调接口。
         */
        public HttpTask(String url, String method, String cookie, String charset, HashMap<String, String> header, HttpCallback callback) {
            mUrl = url;
            mMethod = method;
            mCookie = cookie;
            mCharset = charset;
            mHeader = header;
            mCallback = callback;
        }

        /**
         * 设置进度更新监听器。
         *
         * @param listener 监听器对象。
         */
        public void setUpdateListerer(UpdateListener listener) {
            mUpdateListener = listener;
        }

        @Override
        protected void onProgressUpdate(String... values) {
            super.onProgressUpdate(values);
            // 通知监听器进度更新
            if(mUpdateListener!=null) mUpdateListener.onUpdate(values);
        }

        @Override
        protected HttpResult doInBackground(Object[] p1) {
            try {
                URL url = new URL(mUrl);

                // 打开 HTTP 连接
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setConnectTimeout(6000);
                HttpURLConnection.setFollowRedirects(true);
                conn.setDoInput(true);
                conn.setRequestProperty("Accept-Language", "zh-cn,zh;q=0.5");

                conn.setRequestProperty("Accept-Charset", mCharset != null ? mCharset : "UTF-8");

                // 设置 Cookie
                if (mCookie != null)
                    conn.setRequestProperty("Cookie", mCookie);

                // 设置全局请求头
                if (sHeader != null) {
                    Set<Map.Entry<String, String>> entries = sHeader.entrySet();
                    for (Map.Entry<String, String> entry : entries) {
                        conn.setRequestProperty(entry.getKey(), entry.getValue());
                    }
                }

                // 设置自定义请求头
                if (mHeader != null) {
                    Set<Map.Entry<String, String>> entries = mHeader.entrySet();
                    for (Map.Entry<String, String> entry : entries) {
                        conn.setRequestProperty(entry.getKey(), entry.getValue());
                    }
                }

                if (mMethod != null)
                    conn.setRequestMethod(mMethod);

                // 如果不是 GET 请求且有数据,则设置输出流
                if (!"GET".equals(mMethod) && p1.length != 0) {
                    mData = formatData(p1);

                    conn.setDoOutput(true);
                    conn.setRequestProperty("Content-length", "" + mData.length);
                }

                conn.connect();

                // ==================== 文件下载逻辑 ====================
                if ("GET".equals(mMethod) && p1.length != 0) {
                    File f = new File((String) p1[0]);
                    if (!f.getParentFile().exists())
                        f.getParentFile().mkdirs();
                    if (f.exists()) {
                        try {
                            f.delete();
                        } catch (Exception e) {
                        }
                    }
                    FileOutputStream out = new FileOutputStream(f);
                    InputStream in = conn.getInputStream();
                    long len = conn.getContentLength();
                    long off = 0;
                    try {
                        int byteread = 0;
                        byte[] buffer = new byte[128 * 1024];
                        while ((byteread = in.read(buffer)) != -1) {
                            out.write(buffer, 0, byteread);
                            off += byteread;
                            // 发布下载进度(百分比)
                            publishProgress(String.format(Locale.getDefault(), "%d", off * 100 / len));
                        }
                    } catch (Exception e) {
                    }
                    return new HttpResult(conn.getResponseCode(), f.getAbsolutePath(), null, conn.getHeaderFields());
                }

                // ==================== POST/PUT 上传数据逻辑 ====================
                if (p1.length != 0) {
                    OutputStream os = conn.getOutputStream();
                    os.write(mData);
                }

                // 获取响应码和响应头
                int code = conn.getResponseCode();
                Map<String, List<String>> hs = conn.getHeaderFields();
                String encoding = conn.getContentEncoding();

                // 提取 Set-Cookie 响应头
                List<String> cs = hs.get("Set-Cookie");
                StringBuilder cok = new StringBuilder();
                if (cs != null) {
                    for (String s : cs) {
                        cok.append(s).append(";");
                    }
                }
                // 从 Content-Type 中提取字符集编码
                List<String> ct = hs.get("Content-Type");
                if (ct != null) {
                    for (String s : ct) {
                        int idx = s.indexOf("charset");
                        if (idx != -1) {
                            idx = s.indexOf("=", idx);
                            if (idx != -1) {
                                int idx2 = s.indexOf(";", idx);
                                if (idx2 == -1)
                                    idx2 = s.length();
                                mCharset = s.substring(idx + 1, idx2);
                                break;
                            }
                        }
                    }
                }

                if (mCharset == null) {
                    mCharset = "UTF-8";
                }

                // 读取响应体内容
                StringBuilder buf = new StringBuilder();
                try {
                    InputStream is = conn.getInputStream();
                    BufferedReader reader = new BufferedReader(new InputStreamReader(is, mCharset));
                    String line = reader.readLine();
                    if (line != null)
                        buf.append(line);
                    while ((line = reader.readLine()) != null && !isCancelled())
                        buf.append('\n').append(line);
                    is.close();
                } catch (Exception e) {
                    e.printStackTrace();
                }
                // 如果正常流读取失败,尝试从错误流中读取
                InputStream is = conn.getErrorStream();
                if (is != null) {
                    BufferedReader reader = new BufferedReader(new InputStreamReader(is, mCharset));
                    String line = reader.readLine();
                    if (line != null)
                        buf.append(line);
                    while ((line = reader.readLine()) != null && !isCancelled())
                        buf.append('\n').append(line);
                    is.close();
                }
                return new HttpResult(code, new String(buf), cok.toString(), hs);
            } catch (Exception e) {
                Log.i("lua", "doInBackground: " + mUrl);
                e.printStackTrace();
                return new HttpResult(-1, e.getMessage(), null, null);
            }

        }

        /**
         * 格式化请求数据。
         * 支持 String、byte[]、File、Map 等多种数据类型。
         *
         * @param p1 数据对象数组。
         * @return 格式化后的字节数组。
         */
        private byte[] formatData(Object[] p1) throws UnsupportedEncodingException, IOException {
            byte[] bs = null;
            if (p1.length == 1) {
                Object obj = p1[0];
                if (obj instanceof String)
                    bs = ((String) obj).getBytes(mCharset != null ? mCharset : "UTF-8");
                else if (obj.getClass().getComponentType() == byte.class)
                    bs = (byte[]) obj;
                else if (obj instanceof File)
                    bs = LuaUtil.readAll(new FileInputStream((File) obj));
                else if (obj instanceof Map)
                    bs = formatData((Map) obj);
            }
            return bs;
        }

        private byte[] formatData(Map obj) throws UnsupportedEncodingException {
            // TODO: Implement this method
            StringBuilder buf = new StringBuilder();
            Set<Map.Entry<String, String>> entries = mHeader.entrySet();
            for (Map.Entry<String, String> entry : entries) {
                buf.append(entry.getKey()).append("=").append(entry.getValue()).append("&");
            }
            return buf.toString().getBytes(mCharset != null ? mCharset : "UTF-8");
        }

        /**
         * 取消 HTTP 请求任务。
         *
         * @return 是否成功取消。
         */
        public boolean cancel() {
            return super.cancel(true);
        }


        @Override
        protected void onPostExecute(HttpResult result) {
            // 如果任务已取消,则不回调
            if (isCancelled())
                return;
            // 调用回调接口,返回请求结果
            mCallback.onDone(result);
        }
    }

    /**
     * HTTP 请求回调接口。
     */
    public interface HttpCallback {
        /**
         * 当请求完成时调用。
         *
         * @param result 请求结果对象。
         */
        public void onDone(HttpResult result);
    }

    /**
     * HTTP 请求结果类,封装响应码、响应体、Cookie 和响应头。
     */
    public static class HttpResult {
        // HTTP 响应码(200 表示成功)
        public int code;
        // 响应体文本内容
        public String text;
        // Cookie 字符串
        public String cookie;
        // 响应头映射
        public Map<String, List<String>> header;

        /**
         * 构造函数。
         *
         * @param code 响应码。
         * @param text 响应体文本。
         * @param cookie Cookie 字符串。
         * @param header 响应头映射。
         */
        public HttpResult(int code,
                          String text,
                          String cookie,
                          Map<String, List<String>> header) {

            this.code = code;
            this.text = text;
            this.cookie = cookie;
            this.header = header;
        }

        /**
         * 简化构造函数,默认响应码为 200。
         *
         * @param s 响应体文本。
         */
        public HttpResult(String s) {
            this(200,s,null,null);
        }
    }
}
