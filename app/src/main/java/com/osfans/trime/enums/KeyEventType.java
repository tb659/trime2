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

/**
 * 按键事件类型枚举。
 * 定义按键支持的各种交互事件:
 * - CLICK: 单击事件
 * - LONG_CLICK: 长按事件
 * - SWIPE_LEFT/RIGHT/UP/DOWN: 四个方向的滑动事件
 * - COMBO: 组合键事件
 */
public enum KeyEventType {
  /** 单击事件 */
  CLICK,
  /** 长按事件 */
  LONG_CLICK,
  /** 左滑事件 */
  SWIPE_LEFT,
  /** 右滑事件 */
  SWIPE_RIGHT,
  /** 上滑事件 */
  SWIPE_UP,
  /** 下滑事件 */
  SWIPE_DOWN,
  /** 组合键事件 */
  COMBO
}
