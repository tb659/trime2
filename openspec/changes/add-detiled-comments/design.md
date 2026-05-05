# Design: 为 com/osfans/trime 包添加详细注释

## 注释规范

### 语言要求
- 所有注释使用**简体中文**
- 将现有的繁体中文注释转换为简体中文
- 保留代码中的英文术语（如 API、class、method 等）

### 注释类型

#### 1. 文件头注释
每个文件开头添加文件说明注释：
```java
/*
 * SPDX-FileCopyrightText: 2015 - 2025 Rime community
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package com.osfans.trime.xxx;

/**
 * 文件描述：简要说明该文件的功能和用途
 * 
 * @author 原作者（如果已知）
 * @date 创建或修改日期
 */
```

#### 2. 类/接口注释
```java
/**
 * 类的功能描述
 * 
 * <p>详细描述类的用途、主要功能、使用场景等</p>
 * 
 * @see 相关类或方法
 */
public class Xxx {
    // ...
}
```

#### 3. 方法注释
```java
/**
 * 方法功能描述
 * 
 * @param 参数名 参数说明
 * @return 返回值说明
 * @throws 异常类型 异常说明
 */
public returnType methodName(paramType param) {
    // 方法逻辑说明
    int count = 0; // 计数器，用于记录...
    // ...
}
```

#### 4. 字段/变量注释
```java
/** 常量说明 */
private static final String TAG = "Xxx";

/** 成员变量说明 */
private int count; // 或者: // 成员变量说明
```

#### 5. 代码行注释
- 关键逻辑行：说明"为什么"这样做，不只是"做什么"
- 复杂算法：说明算法思路
- 特殊处理：说明处理的原因和场景
- 魔术数字：说明数字的含义

## 处理策略

### 处理顺序
按照目录结构顺序处理，先处理根目录文件，再按字母顺序处理子目录：

1. `com/osfans/trime/` 根目录文件
2. `com/osfans/trime/candidate/`
3. `com/osfans/trime/core/`
4. `com/osfans/trime/data/` (包括 opencc/ 和 userdict/)
5. `com/osfans/trime/dialog/`
6. `com/osfans/trime/enums/`
7. `com/osfans/trime/keyboard/` (包括 adapter/)
8. `com/osfans/trime/speech/`
9. `com/osfans/trime/theme/`
10. `com/osfans/trime/util/`

### 繁体转简体规则
- 使用标准的繁体到简体转换规则
- 保留专有名词（如 RIME、Trime 等）
- 保留代码中的英文技术术语
- 转换示例：
  - 繁："這是一個按鍵視圖" → 简："这是一个按键视图"
  - 繁："處理按鍵事件" → 简："处理按键事件"

### 注释添加原则
1. **不改变代码逻辑**：只添加注释，不修改任何代码实现
2. **保持原有格式**：保留代码的缩进、换行等格式
3. **注释位置**：
   - 类注释在 `class` 关键字之前
   - 方法注释在方法声明之前
   - 代码行注释在代码行的上方或右侧
4. **注释质量**：
   - 说明"为什么"，不只是"是什么"
   - 解释复杂逻辑和算法
   - 说明参数和返回值的含义
   - 标注特殊处理的原因

## 质量标准

### 完整性
- [ ] 所有类、接口、枚举都有注释
- [ ] 所有 public/protected 方法都有注释
- [ ] 所有字段都有注释
- [ ] 关键代码行都有注释说明

### 准确性
- [ ] 注释准确反映代码功能
- [ ] 没有错误的说明
- [ ] 参数和返回值说明与实际一致

### 可读性
- [ ] 注释语言通顺、易懂
- [ ] 使用规范的简体中文
- [ ] 技术术语准确

## 工具和方法

### 处理方式
- 使用文件读取工具读取源文件
- 分析代码结构（类、方法、字段等）
- 在适当位置插入注释
- 使用文件写入工具保存修改后的文件

### 验证方法
- 确保文件可以正常编译（注释不影响编译）
- 检查注释是否完整覆盖所有代码
- 检查繁体中文是否已转换为简体

## 注意事项

1. **大文件处理**：对于较大的文件（如 TrimeService.java 有 1571 行），需要分段处理，避免遗漏
2. **Kotlin 文件**：对于 .kt 文件，使用 Kotlin 的注释风格（/** */ 或 //）
3. **保留版权信息**：文件头部的 SPDX 版权信息保留不变
4. **不修改第三方代码**：只处理 com/osfans/trime 包下的文件
