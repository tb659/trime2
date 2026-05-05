package com.osfans.trime;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * JSON工具类，提供JSON文件的读写、解析和保存功能。
 * 支持字符串列表、JSON数组、JSON对象以及历史数据（HistoryData）的持久化操作。
 * 所有读写操作均使用UTF-8编码，确保中文等多字节字符的正确处理。
 */
public class JsonUtil {
    /**
     * 从JSON文件加载字符串列表。
     * 读取指定路径的JSON文件，解析为JSON数组，并将所有字符串元素存入列表返回。
     * @param path JSON文件路径
     * @return 包含JSON数组中所有字符串的列表，如果文件不存在或解析失败则返回空列表
     */
    public static ArrayList<String> load(File path) {
        ArrayList<String> list = new ArrayList<>();
        try {
            InputStream stream = new FileInputStream(path);
            BufferedReader reader = new BufferedReader(new InputStreamReader(stream, "UTF-8"));
            StringBuilder stringBuilder = new StringBuilder();
            String input;
            while ((input = reader.readLine()) != null) {
                stringBuilder.append(input);
            }
            stream.close();
            JSONArray letters = new JSONArray(stringBuilder.toString());
            int len = letters.length();
            for (int i = 0; i < len; i++) {
                list.add(letters.getString(i));
            }
        } catch (java.io.IOException | JSONException ignored) {
        }
        return list;
    }

    /**
     * 将字符串列表保存为JSON文件。
     * 将列表转换为JSON数组格式，并以缩进4个空格的格式写入指定文件。
     * @param path 保存的目标文件路径
     * @param map 要保存的字符串列表
     */
    public static void save(File path, List<String> map) {
        JSONArray json = new JSONArray(map);
        try {
            BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(path), "UTF-8"));
            writer.write(json.toString(4));
            writer.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * 从JSON文件加载JSON数组。
     * 读取指定路径的JSON文件并解析为JSONArray对象。
     * @param path JSON文件路径
     * @return 解析得到的JSON数组，如果文件不存在或解析失败则返回空的JSON数组
     */
    public static JSONArray loadArray(File path) {
        ArrayList<String> list = new ArrayList<>();
        try {
            InputStream stream = new FileInputStream(path);
            BufferedReader reader = new BufferedReader(new InputStreamReader(stream, "UTF-8"));
            StringBuilder stringBuilder = new StringBuilder();
            String input;
            while ((input = reader.readLine()) != null) {
                stringBuilder.append(input);
            }
            stream.close();
            return new JSONArray(stringBuilder.toString());

        } catch (java.io.IOException | JSONException ignored) {
        }
        return new JSONArray();
    }

    /**
     * 将JSON对象保存到指定路径（字符串路径版本）。
     * @param path 保存的目标文件路径（字符串形式）
     * @param json 要保存的JSON对象
     */
    public static void save(String path, JSONObject json) {
        save(new File(path), json);
    }

    /**
     * 将JSON对象保存到指定文件。
     * 以缩进4个空格的格式写入JSON对象到文件。
     * @param path 保存的目标文件
     * @param json 要保存的JSON对象
     */
    public static void save(File path, JSONObject json) {
        try {
            BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(path), "UTF-8"));
            writer.write(json.toString(4));
            writer.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * 将JSON数组保存到指定文件。
     * 以缩进4个空格的格式写入JSON数组到文件。
     * @param path 保存的目标文件
     * @param json 要保存的JSON数组
     */
    public static void save(File path, JSONArray json) {
        try {
            BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(path), "UTF-8"));
            writer.write(json.toString(4));
            writer.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * 从JSON文件加载历史数据列表。
     * 文件格式应为JSON对象，包含一个"history"字段，其值为历史数据的JSON数组。
     * @param path JSON文件路径
     * @return 历史数据列表，如果文件不存在或解析失败则返回空列表
     */
    public static ArrayList<HistoryData> loadHistoryData(String path) {
        ArrayList<HistoryData> list = new ArrayList<>();
        if (!new File(path).exists())
            return list;
        try {
            InputStream stream = new FileInputStream(new File(path));
            BufferedReader reader = new BufferedReader(new InputStreamReader(stream, "UTF-8"));
            StringBuilder stringBuilder = new StringBuilder();
            String input;
            while ((input = reader.readLine()) != null) {
                stringBuilder.append(input);
            }
            stream.close();
            try {
                JSONArray json = new JSONObject(stringBuilder.toString()).getJSONArray("history");
                int len = json.length();
                for (int i = 0; i < len; i++) {
                    list.add(new HistoryData(json.getJSONObject(i)));
                }
            } catch (JSONException e) {
                e.printStackTrace();
            }
        } catch (java.io.IOException e) {
            e.printStackTrace();
        }
        return list;
    }

    /**
     * 将历史数据列表保存到JSON文件。
     * 保存格式为JSON对象，包含一个"history"字段，其值为历史数据的JSON数组。
     * 使用缩进2个空格的格式写入。
     * @param path 保存的目标文件路径
     * @param history 要保存的历史数据列表
     */
    public static void saveHistoryData(String path, ArrayList<HistoryData> history) {
        JSONObject json = new JSONObject();
        JSONArray list = new JSONArray();
        try {
            for (HistoryData data : history) {
                list.put(data.toJson());
            }
            json.put("history", list);
        } catch (JSONException e) {
            e.printStackTrace();
        }
        try {
            BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(path), "UTF-8"));
            writer.write(json.toString(2));
            writer.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * 从指定路径读取JSON对象。
     * @param name JSON文件路径
     * @return 解析得到的JSON对象，如果文件不存在或解析失败则返回空的JSON对象
     */
    public static JSONObject read(String name) {
        File file = new File(name);
        try {
            InputStream stream = new FileInputStream(file);
            BufferedReader reader = new BufferedReader(new InputStreamReader(stream, "UTF-8"));
            StringBuilder stringBuilder = new StringBuilder();
            String input;
            while ((input = reader.readLine()) != null) {
                stringBuilder.append(input);
            }
            stream.close();
            return new JSONObject(stringBuilder.toString());

        } catch (Exception ignored) {
        }
        return new JSONObject();
    }

    /**
     * 从指定文件读取JSON并转换为Map。
     * 将JSON对象的所有键值对转换为Map<String, Object>。
     * @param file JSON文件
     * @return 包含JSON所有键值对的Map，如果文件不存在或解析失败则返回空Map
     */
    public static Map<String, Object> read(File file) {
        Map<String, Object> map = new HashMap<>();
        try {
            InputStream stream = new FileInputStream(file);
            BufferedReader reader = new BufferedReader(new InputStreamReader(stream, "UTF-8"));
            StringBuilder stringBuilder = new StringBuilder();
            String input;
            while ((input = reader.readLine()) != null) {
                stringBuilder.append(input);
            }
            stream.close();
            JSONObject json = new JSONObject(stringBuilder.toString());
            Iterator<String> ks = json.keys();
            while (ks.hasNext()) {
                String k = ks.next();
                map.put(k, json.get(k));
            }
        } catch (java.io.IOException | JSONException ignored) {
        }
        return map;
    }

    /**
     * 从JSON对象中获取指定键的JSONObject，如果不存在则创建空对象并放入原JSON。
     * @param json 源JSON对象
     * @param key 要获取的键
     * @return 对应键的JSONObject，如果不存在则创建新的空JSONObject
     */
    public static JSONObject getJSONObject(JSONObject json, String key) {
        if(json.has(key))
            return json.optJSONObject(key);
        JSONObject j = new JSONObject();
        try {
            json.put(key,j);
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return j;
    }

    /**
     * 历史数据封装类，用于存储和恢复文件的访问历史记录。
     * 包含文件路径和索引位置两个字段。
     */
    public static class HistoryData {
        /** 文件路径 */
        private String mPath;
        /** 文件内的索引位置 */
        private int mIdx;

        /**
         * 从JSON对象构造历史数据。
         * @param json 包含"path"和"idx"字段的JSON对象
         */
        public HistoryData(JSONObject json) {
            mPath = json.optString("path");
            mIdx = json.optInt("idx");
        }

        /**
         * 使用指定参数构造历史数据。
         * @param path 文件路径
         * @param idx 索引位置
         */
        public HistoryData(String path, int idx) {
            mPath = path;
            mIdx = idx;
        }

        /**
         * 获取文件路径。
         * @return 文件路径字符串
         */
        public String getPath() {
            return mPath;
        }

        /**
         * 获取索引位置。
         * @return 索引位置整数值
         */
        public int getIdx() {
            return mIdx;
        }

        /**
         * 转换为JSON对象。
         * @return 包含"path"和"idx"字段的JSON对象
         */
        public JSONObject toJson() {
            JSONObject j = new JSONObject();
            try {
                j.put("path", mPath);
                j.put("idx", mIdx);
            } catch (JSONException e) {
                e.printStackTrace();
            }
            return j;
        }
    }
}
