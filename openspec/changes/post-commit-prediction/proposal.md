## Why

虎码等形码方案在打字上屏后，候选区立即清空，用户必须继续输入下一组编码才能看到候选词。百度、搜狗等输入法在用户上屏后会根据已上屏的内容自动预测后续文字并展示在候选区，大幅减少击键次数。Trime 目前缺少此能力，用户打完一句话需要逐字逐词编码，效率较低。

## What Changes

- 文字上屏后，自动将已上屏的文本送入预测引擎，获取后续候选词
- 预测候选项展示在现有候选区（与输入法候选同一位置），用户可直接点击上屏
- 选中预测候选后，该词上屏并触发基于累积文本的新一轮预测
- 新增配置开关，允许用户启用/禁用预测功能
- 支持在 `default.custom.yaml` 或主题中配置预测源（用户词典/语言模型）

## Capabilities

### New Capabilities
- `post-commit-prediction`: 文字上屏后自动预测后续候选词并展示，支持连续预测与配置开关

### Modified Capabilities

（无）

## Impact

- `app/src/main/java/com/osfans/trime/TrimeService.java` — 上屏后触发预测查询
- `app/src/main/java/com/osfans/trime/core/Rime.java` — 新增预测查询接口（JNI）
- `app/src/main/java/com/osfans/trime/candidate/CandidatesManager.java` — 展示预测候选
- `app/src/main/java/com/osfans/trime/candidate/CandidateView.java` — 预测候选交互
- `app/src/main/java/com/osfans/trime/core/RimeProto.java` — 预测相关的数据结构
- `app/src/main/jni/librime-predict/` — 确认预测引擎集成是否完整
- `app/src/main/jni/librime_jni/rime_jni.cc` — JNI 桥接预测接口
- `app/src/main/java/com/osfans/trime/theme/Style.java` — 新增预测功能配置项
