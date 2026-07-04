## 1. 创建 Lua 预测脚本 — Processor 层

- [x] 1.1 在 `app/src/main/assets/shared/lua/trime/` 下创建 `user_predict.lua`，实现 Processor 组件 (`P.init`, `P.func`, `P.fini`)
- [x] 1.2 实现 `commit_notifier` 回调：监听上屏事件，维护 `history[]` 数组和 `last_commit` 状态
- [x] 1.3 实现自训练写入逻辑：将上屏文本按 S-Gram / 2-Gram / 1-Gram / P-Gram 格式写入 LevelDB
- [x] 1.4 实现 ABA 防折返检测：比较 `last_commit == text` 和 `text == history[#history-1]` 跳过自我循环
- [x] 1.5 实现语境超时隔离：记录 `last_commit_time`，超时（默认 5000ms）时调用 `reset_memory_chain()`
- [x] 1.6 实现标点断句检测：识别终结符（。！？等）并重置记忆链；对语气助词白名单放行
- [x] 1.7 实现瀑布流查询函数 `get_predictions()`：S → 2 → 1 → P 四级降级，带时间衰减权重计算
- [x] 1.8 实现数据淘汰：`P.init()` 中定期扫描过期条目（90天），物理删除超期数据
- [x] 1.9 实现回滚机制：`P.func()` 中拦截 BackSpace 键，撤销最近 3 次数据库写入

## 2. 创建 Lua 预测脚本 — Translator 和 Filter 层

- [x] 2.1 在 `user_predict.lua` 中实现 Translator 组件 (`T.init`, `T.func`, `T.fini`)：当输入为占位符 `›››` 时生成预测候选
- [x] 2.2 实现 `update_notifier` 回调：预测候选就绪后调用 `ctx:push_input("›››")` 注入占位符触发 Translator
- [x] 2.3 在 `user_predict.lua` 中实现 Filter 组件 (`F.init`, `F.func`)：根据 `f_reorder_map` 调频候选排序
- [x] 2.4 实现 YAML 配置加载函数 `load_config()`：读取 `user_predict/*` 配置项
- [x] 2.5 从 `user_predict.lua` 导出三个组件：`trime.user_predict*P` / `*T` / `*F`

## 3. Schema 配置示例

- [x] 3.1 创建 `app/data/rime/prediction.custom.yaml` 示例配置模板，包含 processor/translator/filter 注册和 `user_predict` 配置块
- [x] 3.2 创建 `docs/prediction-config.md` 文档，说明如何启用预测功能

## 4. Java 侧：候选栏上屏后保持显示

- [x] 4.1 通过 Lua `update_notifier` + `ctx:push_input("›››")` 自动保持 isComposing=true，**无需 Java 修改**
- [x] 4.2 Translator 在引擎层面直接产出类型为 `"predict"` 的候选词，通过标准 `CandidateMenuMessage` 消息流显示

## 5. 预测开关集成

- [x] 5.1 标准 Rime option 机制处理 `{Option: prediction}` 键，Lua 代码通过 `ctx:get_option("prediction")` 判断
- [x] 5.2 示例键盘按键定义已在 `prediction-config.md` 中提供

## 6. 编译与集成测试

- [ ] 6.1 构建 APK 并安装到 Android 设备
- [ ] 6.2 在虎码方案中添加预测 Lua 组件配置，测试上屏后预测候选显示
- [ ] 6.3 测试连续预测（选预测 → 新预测 → 选预测 → 停止）
- [ ] 6.4 测试防干扰：快速退格撤销、超时断句、ABA 防折返
- [ ] 6.5 测试开关切换：关闭 prediction 后恢复正常输入行为
