## Context

Trime 已完整编译了 `librime-predict` 原生 C++ 插件（通过 `rime_require_module_predict()` 注册），但存在几个问题：

- `librime-predict` 需要预先构建的 `predict.db`（Darts 双数组 Trie + mmap），对形码方案（如虎码）不友好
- 社区方案 **Rime万象拼音**（`amzxyz/rime-wanxiang`）提供了一个纯 Lua 的预测处理器 `user_predict.lua`，基于 Rime 的 LevelDB（userdb）机制自训练，已获广泛验证
- 虎码是形码方案，无内置语言模型。Lua 方案通过自训练动态学习用户习惯，无需预设数据，更适合虎码场景
- Trime 内置 AndroLua (LuaJ)，可直接运行 Lua 脚本

## Goals / Non-Goals

**Goals:**
- 用户上屏后，候选区展示基于已上屏内容的预测后续文字
- 点击预测候选 → 该预测词上屏 → 触发新一轮预测（支持最多 3 层连续预测）
- 采用纯 Lua 方案，自训练无需预设数据，自动适应用户习惯
- 支持两种模式：上屏后预测（post-predict）和输入中调频（context reorder）
- 提供防干扰机制：ABA 防折返、语境超时隔离、标点断句

**Non-Goals:**
- 不修改 C++ 原生层
- 不修改 JNI 桥接层
- 不依赖 `librime-predict` 的 C++ 实现（可使用，但不是强依赖）
- 不预先提供训练数据（通过自训练积累）

## Decisions

### Decision 1: 预测实现方式 — 参考 rime-wanxiang 的纯 Lua 方案

采用与 `amzxyz/rime-wanxiang` 的 `user_predict.lua` 类似的架构，由三层组件构成：

**Processor (P)** — `lua_processor@*trime.user_predict*P`
- 通过 `commit_notifier` 监听上屏事件，跟踪用户输入历史
- 通过 `update_notifier` 注入占位符字符（`›`），触发 Translator 生成预测候选
- 通过 `delete_notifier` 处理候选删除
- 管理记忆链：历史记录、预测次数、上下文超时
- 实现 ABA 防折返：拦截 `"你好"→"你好"` 的自我循环

**Translator (T)** — `lua_translator@*trime.user_predict*T`
- 当输入为占位符 `›››` 时，从 LevelDB 查询预测候选
- 候选类型标记为 `"predict"`，可被 Filter/UI 区分
- 瀑布流查询模型：S-Gram → 2-Gram → 1-Gram → P-Gram

**Filter (F)** — `lua_filter@*trime.user_predict*F`
- 在常规输入中，根据上下文预测提升匹配候选的排序位置
- 支持量词调频（如 `"1"` + 量词候选的自动匹配）

### Decision 2: 瀑布流查询模型

`get_predictions()` 依次尝试以下查询级别，命中即返回：

| 级别 | Key 格式 | 权重乘数 | 说明 |
|------|---------|---------|------|
| S-Gram | `S\t<上文>\t` | x1000000 | 句子级精确匹配 |
| 2-Gram | `2\t<前词>\t<后词>\t` | x10000 | 二元组精确匹配 |
| 1-Gram | `1\t<后缀>\t` | x100 | 一元组降级匹配 |
| P-Gram | `P\t<后缀>\t` | x1 | 模糊后缀抗抖动 |

权重公式：`score = count × DECAY_RATE ^ age_days × multiplier`

### Decision 3: 自训练与数据管理

- 使用 Rime LevelDB 储存（`predict.userdb` 目录）
- Key 格式：`<gram_type>\t<prefix>\t<suffix>\t`
- Value 格式：`<count>|<timestamp>`
- 自动清理：超过 90 天未命中的条目物理销毁
- 双重衰减：时间指数衰减 + 频次基础权重
- 回滚机制：连按退格时撤销最近 3 次写入操作

### Decision 4: 防干扰机制

| 机制 | 实现方式 |
|------|---------|
| ABA 防折返 | 检查 `last_commit == current_text` 或历史中存在回环时跳过记录 |
| 语境超时 | 两次上屏间隔超过 5 秒时重置记忆链 |
| 标点断句 | 检测到句号/问号/感叹号等终结符时清空历史 |
| 语气助词白名单 | "吧呢吗啦"等助词接标点时允许正常结束 |
| 最大预测次数 | `max_predictions=3`，超过后停止预测 |
| 单字过滤 | 上屏文本超过 4 个字不记录 |

### Decision 5: 候选栏可见性

上屏后 `isComposing` 变为 `false`，候选栏会隐藏。需要修改：

- `Processor` 的 `commit_cb` 在生成预测候选后设置 `env.need_push = true`
- `update_cb` 检测到 `input == ""` 且 `need_push` 时，调用 `ctx:push_input("›››")`
- 占位符 `›››` 作为输入注入引擎，使候选栏保持显示
- `Translator` 在输入为 `›››` 时产出预测候选
- 用户敲任意非预测键时，占位符被清除，回到正常输入状态

### Decision 6: 配置方式

参考 rime-wanxiang 的配置结构，在 `schema_name.custom.yaml` 中添加：

```yaml
patch:
  'engine/processors/@before 0': lua_processor@*trime.user_predict*P
  'engine/translators/@before 0': lua_translator@*trime.user_predict*T
  'engine/filters/@before 0': lua_filter@*trime.user_predict*F
  'switches/+':
    - name: prediction
      reset: 1
  user_predict:
    db_name: lua/predict
    enable_post_predict: true
    enable_context_reorder: true
    max_candidates: 5
    max_predictions: 3
    expiry_days: 90
    max_memory_branches: 15
    decay_rate: 0.85
    context_timeout: 5000
```

## Risks / Trade-offs

- [低] Lua 性能：查询涉及 LevelDB 迭代扫描（scan_limit=80），在低端设备首次预测可能有几十毫秒延迟。可通过 `max_candidates` 和 `scan_limit` 控制。
- [低] 数据库膨胀：长期使用后 LevelDB 可能增长到数 MB。通过过期清理和 `activation_days` 冷数据淘汰控制。
- [低] 学习曲线：用户初次使用时预测效果较差，需要一段时间积累。这是自训练方案的固有特性。
- [中] Trime LuaJ 兼容性：`user_predict.lua` 使用 `LevelDb()`、`rime_api`、`Candidate()` 等 API，需确认 Trime 的 AndroLua 环境是否完整支持。
