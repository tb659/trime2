// SPDX-FileCopyrightText: 2015 - 2024 Rime community
//
// SPDX-License-Identifier: GPL-3.0-or-later

package com.osfans.trime.data.opencc.dict;

import java.io.File;

public abstract class Dictionary {

    public enum Type {
        /** OpenCC 旧版二进制格式 */
        OCD("ocd"),
        /** OpenCC 新版二进制格式 */
        OCD2("ocd2"),
        /** 文本格式 */
        Text("txt");

        private final String ext;

        Type(String ext) {
            this.ext = ext;
        }

        public String getExt() {
            return ext;
        }

        /**
         * 根据文件名推断词典类型。
         *
         * @param name 文件名。
         * @return 对应的词典类型,无法识别则返回 null。
         */
        public static Type fromFileName(String name) {
            if (name == null) return null;
            if (name.endsWith(".ocd2")) return OCD2;
            if (name.endsWith(".ocd")) return OCD;
            if (name.endsWith(".txt")) return Text;
            return null;
        }
    }

    // Kotlin 的 abstract val 转换为 Java 的抽象 getter 方法
    public abstract File getFile();

    public abstract Type getType();

    public abstract TextDictionary toTextDictionary(File dest);

    public abstract OpenCCDictionary toOpenCCDictionary(File dest);

    /**
     * 词典名称(不含扩展名)。
     */
    public String getName() {
        String name = getFile().getName();
        int dotIndex = name.lastIndexOf('.');
        return (dotIndex > 0) ? name.substring(0, dotIndex) : name;
    }

    /**
     * 转换为文本词典(自动生成目标文件名)。
     * 目标文件与源文件在同一目录,扩展名为 .txt。
     *
     * @return 转换后的文本词典实例。
     */
    public TextDictionary toTextDictionary() {
        File dest = new File(getFile().getParentFile(), getName() + "." + Type.Text.getExt());
        return toTextDictionary(dest);
    }

    /**
     * 转换为 OpenCC 二进制词典(自动生成目标文件名)。
     * 目标文件与源文件在同一目录,扩展名为 .ocd2。
     *
     * @return 转换后的 OpenCC 词典实例。
     */
    public OpenCCDictionary toOpenCCDictionary() {
        File dest = new File(getFile().getParentFile(), getName() + "." + Type.OCD2.getExt());
        return toOpenCCDictionary(dest);
    }

    /**
     * 确保文件存在,不存在则抛出异常。
     */
    protected void ensureFileExists() {
        if (!getFile().exists()) {
            throw new IllegalStateException("File " + getFile().getAbsolutePath() + " does not exist");
        }
    }

    /**
     * 确保目标文件是文本格式(.txt),并删除已存在的文件。
     *
     * @param dest 目标文件。
     */
    protected void ensureTxt(File dest) {
        if (!getFileExtension(dest).equals(Type.Text.getExt())) {
            throw new IllegalArgumentException("Dest file name must end with ." + Type.Text.getExt());
        }
        dest.delete();
    }

    /**
     * 确保目标文件是二进制格式(.ocd 或 .ocd2),并删除已存在的文件。
     *
     * @param dest 目标文件。
     */
    protected void ensureBin(File dest) {
        String ext = getFileExtension(dest);
        if (!ext.equals(Type.OCD.getExt()) && !ext.equals(Type.OCD2.getExt())) {
            throw new IllegalArgumentException("Dest file name must end with ." + Type.OCD.getExt() + " or ." + Type.OCD2.getExt());
        }
        dest.delete();
    }

    /**
     * 返回词典的字符串表示。
     * 格式: "类名[名称 -> 文件路径]"。
     */
    @Override
    public String toString() {
        return getClass().getSimpleName() + "[" + getName() + " -> " + getFile().getPath() + "]";
    }

    public static Dictionary newDictionary(File it) {
        if (it == null) return null;
        Type type = Type.fromFileName(it.getName());
        if (type == null) return null;

        switch (type) {
            case OCD:
            case OCD2:
                return new OpenCCDictionary(it);
            case Text:
                return new TextDictionary(it);
            default:
                return null;
        }
    }

    // 辅助方法：获取文件后缀名（不带点）
    private String getFileExtension(File file) {
        String name = file.getName();
        int dotIndex = name.lastIndexOf('.');
        return (dotIndex > 0) ? name.substring(dotIndex + 1) : "";
    }
}
