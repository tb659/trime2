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

package com.osfans.trime.enums;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

/**
 * 窗口位置类型枚举。
 * 定义浮动窗口(如候选词窗口)的显示位置和定位方式。
 */
public enum WindowsPositionType {
  /** 左侧对齐 */
  LEFT,
  /** 左上角 */
  LEFT_UP,
  /** 右侧对齐 */
  RIGHT,
  /** 右上角 */
  RIGHT_UP,
  /** 可拖动模式 */
  DRAG,
  /** 固定位置 */
  FIXED,
  /** 左下角 */
  BOTTOM_LEFT,
  /** 右下角 */
  BOTTOM_RIGHT,
  /** 左上角 */
  TOP_LEFT,
  /** 右上角 */
  TOP_RIGHT;

  /** 字符串到枚举值的转换映射表 */
  private static final Map<String, WindowsPositionType> convertMap =
      new HashMap<String, WindowsPositionType>(WindowsPositionType.values().length);

  /**
   * 静态初始化块。
   * 将所有枚举值添加到转换映射表中,键为大写字符串。
   */
  static {
    for (WindowsPositionType type : WindowsPositionType.values()) {
      convertMap.put(type.toString(), type);
    }
  }

  /**
   * 从字符串转换为枚举值。
   * 支持大小写不敏感的匹配,如果找不到匹配项则返回 FIXED(固定位置)作为默认值。
   *
   * @param code 位置类型字符串(如 "LEFT", "RIGHT_UP" 等)。
   * @return 对应的枚举值,未找到则返回 FIXED。
   */
  public static WindowsPositionType fromString(String code) {
    WindowsPositionType type = convertMap.get(code.toUpperCase(Locale.getDefault()));
    if (null == type) {
      return FIXED;
    } else {
      return type;
    }
  }
}
