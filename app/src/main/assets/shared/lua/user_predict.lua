-- user_predict.lua — 上屏后预测（形码专用）
-- 为虎码等形码方案添加基于 Rime LevelDB 自训练的上屏后预测
-- 参考 amzxyz/rime-wanxiang (GPL-3.0) 的 user_predict.lua，适配 Trime Android
--
-- 架构分三层，通过 schema YAML 注册到 Rime 引擎：
--   Processor (P) — lua_processor@user_predict_processor    监听上屏/按键，维护记忆链
--   Translator (T) — lua_translator@user_predict_translator 输入占位符时生成预测候选
--   Filter (F) — lua_filter@user_predict_filter             输入过程中根据预测调频排序
--
-- 核心设计：
--   ① 瀑布流查询 S→2→1→P-Gram 逐级降级，命中即返回
--   ② 时间衰减权重 score = count × DECAY_RATE^age_days × multiplier
--   ③ 自训练写入 LevelDB（predict.userdb），无需预设数据
--   ④ 注入占位符 ››› 使候选栏在上屏后保持可见

--  ======================= 配置项 ========================
-- patch:
--   # 1. 注册 Lua 组件
--   'engine/processors/@before 0': lua_processor@user_predict_processor    # 监听上屏事件，管理记忆链，写入自训练数据库
--   'engine/translators/@before 0': lua_translator@user_predict_translator  # 注入占位符生成预测候选
--   'engine/filters/@before 0': lua_filter@user_predict_filter              # 输入过程中根据预测调频

--   # 2. 添加预测开关
--   'switches/+':
--     - name: prediction
--       reset: 1
--       states: ["🕳 关", "🔮 开"]

--   # 3. 预测配置
--     # 连续预测行为
--     # 上屏后候选栏显示最多 max_candidates 个预测候选
--     # 选择预测候选 → 该词上屏 → 基于累计文本触发新一轮预测
--     # 达到 max_predictions 次后停止预测
--     # 按任意键打断预测链，恢复正常输入
--     # 退格仅做数据库撤销，不负责回显预测候选
--   user_predict:
--     db_name: lua/predict                # LevelDB 数据库名称
--     max_candidates: 5                    # 每次最多显示几个预测候选
--     max_predictions: 3                   # 连续预测最大次数
--     expiry_days: 90                      # 条目过期天数
--     max_memory_branches: 15             # 查询分支上限
--     decay_rate: 0.85                     # 时间衰减系数 (每日)
--     context_timeout: 5000                # 上下文超时毫秒数
--     internal_first_min_len: 2            # 首字续写学习最短词长
--     internal_first_max_len: 10           # 首字续写学习最长词长
--     internal_first_weight: 0.35          # 首字续写学习权重
--     internal_pair_min_len: 4             # 前 2 字续写学习最短词长
--     internal_pair_max_len: 10            # 前 2 字续写学习最长词长
--     internal_pair_weight: 1.0            # 前 2 字续写学习权重
--     particle_whitelist: '吧,呢,吗,啦,嘛,呀,欸,哒,哈,哇,啊,哦,噢,咯,呗,哟,呦,哎,嗯,么,啥,谁,哪,里,儿,了,的,过,好,行,对,成'  # 语助词白名单（逗号分隔）

-- ======================== Lua 标准库简写 ========================
-- 高频调用局部化，略微提升性能
local insert = table.insert
local remove = table.remove
local sort = table.sort
local s_match = string.match
local s_sub = string.sub
local s_find = string.find
local s_gmatch = string.gmatch
local s_len = string.len
local s_format = string.format
local tonumber = tonumber
local math_max = math.max
local math_min = math.min
local os_time = os.time

-- ======================== 默认配置 ========================
-- 会被方案 YAML 中 user_predict: 下的同名配置覆盖
local CONFIG = {
    MAX_CANDIDATES      = 5,              -- 每次最多显示几个预测候选
    MAX_PREDICTIONS     = 3,              -- 连续预测最大次数（达到后停止）
    EXPIRY_SECONDS      = 90 * 24 * 3600, -- 1-Gram/2-Gram/S-Gram 过期秒数
    P_EXPIRY_SECONDS    = 30 * 24 * 3600, -- P-Gram 过期秒数（模糊匹配更短）
    MAX_MEMORY_BRANCHES = 15,             -- 查询分支上限，超过的置零不删除
    DECAY_RATE          = 0.85,           -- 每日时间衰减系数
    SCAN_LIMIT          = 80,             -- LevelDB 每次扫描上限
    CONTEXT_TIMEOUT_MS  = 5000,           -- 上下文超时毫秒数（两次上屏间隔超过此值重置记忆链）
    INTERNAL_FIRST_MIN_LEN = 2,           -- 首字续写学习最短词长
    INTERNAL_FIRST_MAX_LEN = 10,          -- 首字续写学习最长词长
    INTERNAL_FIRST_WEIGHT  = 0.35,        -- 首字续写学习权重
    INTERNAL_PAIR_MIN_LEN  = 4,           -- 前 2 字续写学习最短词长
    INTERNAL_PAIR_MAX_LEN  = 10,          -- 前 2 字续写学习最长词长
    INTERNAL_PAIR_WEIGHT   = 1.0,         -- 前 2 字续写学习权重
}
-- ======================== 全局状态变量 ========================
-- 这些变量在三个 Lua 组件（P/T/F）之间共享，用于传递记忆链和预测结果
local PH_CHAR = "tyl"       -- ASCII 占位符，避免与 schema 现有规则冲突
local HISTORY_MAX = 4        -- 历史记忆链深度；需覆盖连续预测链，供上下文学习与查询

local history = {}           -- 上屏历史文本数组，最多 HISTORY_MAX 个元素
local last_commit = ""       -- 最近一次上屏文本
local last_commit_time = 0   -- 最近一次上屏的毫秒时间戳
local predict_count = 0      -- 当前连续预测轮次计数（从 1 开始）
local is_predicting = false  -- 是否处于预测状态
local prediction_visible = false -- 是否正在显示上屏后的预测候选
local pending_cands = nil    -- 缓存的预测候选列表，由 commit_cb 填充，Translator 读取
local get_predictions        -- 前向声明，供预测查询与过滤逻辑复用
local last_external_request_revision = 0 -- 最近一次消费的外部删后重预测请求版本
local set_prediction_visible -- 前向声明，供外部重预测入口复用

-- ======================== 字头词表（兜底 fallback） ========================
-- char_words.lua 由 script/generate_char_words.py 从虎码词库生成
-- 映射：字符 → 以该字开头的常见词语列表
-- 在 LevelDB 无数据时作为兜底查询源
-- 仅对单字符上屏生效（取上屏文本的第一个 CJK 字查询）
local _char_words_tbl = nil
local function ensure_char_words()
    if not _char_words_tbl then
        local ok, result = pcall(require, "char_words")
        if ok then _char_words_tbl = result else _char_words_tbl = {} end
    end
    return _char_words_tbl
end

-- 删后重预测走共享请求文件桥接：Java 会同时写 shared/build/user 三处，这里按脚本所在目录向上回溯多级兜底读取。
local function get_request_file_paths()
    local src = debug and debug.getinfo and debug.getinfo(1, "S").source or ""
    if s_sub(src, 1, 1) == "@" then src = s_sub(src, 2) end
    local dir = s_match(src, "^(.*[\\/])") or ""
    if dir == "" then return { "user_predict_request.txt" } end
    local paths = {
        dir .. "user_predict_request.txt",
        dir .. "../user_predict_request.txt",
        dir .. "../../user_predict_request.txt",
        dir .. "../../../user_predict_request.txt",
        dir .. "../../../lua/user_predict_request.txt",
        dir .. "../lua/user_predict_request.txt",
        dir .. "../../lua/user_predict_request.txt",
    }
    local unique = {}
    local result = {}
    for _, path in ipairs(paths) do
        if not unique[path] then
            unique[path] = true
            insert(result, path)
        end
    end
    return result
end

-- ======================== 语气助词白名单 ========================
-- 标点断句时，如果上屏文本的上文以这些字结尾，不重置记忆链
-- 例如 "好的。" → "的"在白名单中，认为这是合法结束，不打断预测链
-- 可通过 schema YAML user_predict/particle_whitelist 覆盖（逗号分隔字符串）
local PARTICLE_WHITELIST = {
    ["吧"] = true, ["呢"] = true, ["吗"] = true, ["啦"] = true,
    ["嘛"] = true, ["呀"] = true, ["欸"] = true, ["哒"] = true,
    ["哈"] = true, ["哇"] = true, ["啊"] = true, ["哦"] = true,
    ["噢"] = true, ["咯"] = true, ["呗"] = true, ["哟"] = true,
    ["呦"] = true, ["哎"] = true, ["嗯"] = true, ["么"] = true,
    ["啥"] = true, ["谁"] = true, ["哪"] = true, ["了"] = true,
    ["的"] = true, ["过"] = true, ["好"] = true, ["行"] = true,
    ["对"] = true, ["成"] = true,
}

-- ======================== 工具函数 ========================

-- 判断单字符是否是终结标点（句号/问号/感叹号/逗号/波浪号等）
-- 用于标点断句检测：遇到这些标点上屏，通常表示一句话结束
local function is_terminal_char(c)
    return s_match(c, "^[！？，。～.!?,]$") ~= nil
end

-- UTF-8 长度计算（兼容不支持 utf8.len 的 LuaJ）
local utf8_len = utf8 and utf8.len or function(str)
    local _, count = string.gsub(str, "[^\128-\191]", "")
    return count
end

-- 从 schema YAML 加载 user_predict/* 配置项
-- 在 P.init() 和 T.init() 时调用
local function load_config(env)
    local config = env.engine.schema.config
    if not config then return end

    CONFIG.MAX_CANDIDATES      = config:get_int("user_predict/max_candidates") or 5
    CONFIG.MAX_PREDICTIONS     = config:get_int("user_predict/max_predictions") or 3
    CONFIG.EXPIRY_SECONDS      = (config:get_int("user_predict/expiry_days") or 90) * 86400
    CONFIG.MAX_MEMORY_BRANCHES = config:get_int("user_predict/max_memory_branches") or 15
    CONFIG.DECAY_RATE          = config:get_double("user_predict/decay_rate") or 0.85

    CONFIG.INTERNAL_FIRST_MIN_LEN = config:get_int("user_predict/internal_first_min_len") or 2
    CONFIG.INTERNAL_FIRST_MAX_LEN = config:get_int("user_predict/internal_first_max_len") or 10
    CONFIG.INTERNAL_FIRST_WEIGHT = config:get_double("user_predict/internal_first_weight") or 0.35

    CONFIG.INTERNAL_PAIR_MIN_LEN = config:get_int("user_predict/internal_pair_min_len") or 4
    CONFIG.INTERNAL_PAIR_MAX_LEN = config:get_int("user_predict/internal_pair_max_len") or 10
    CONFIG.INTERNAL_PAIR_WEIGHT = config:get_double("user_predict/internal_pair_weight") or 1.0

    local timeout_val = config:get_int("user_predict/context_timeout")
    if timeout_val ~= nil then CONFIG.CONTEXT_TIMEOUT_MS = timeout_val end
    local history_depth = config:get_int("user_predict/history_depth")
    if history_depth ~= nil and history_depth > 0 then
        HISTORY_MAX = history_depth
    else
        HISTORY_MAX = math_max(4, CONFIG.MAX_PREDICTIONS + 1)
    end
    local whitelist_str = config:get_string("user_predict/particle_whitelist")
    if whitelist_str and whitelist_str ~= "" then
        local t = {}
        for w in s_gmatch(whitelist_str, "[^,]+") do t[w] = true end
        PARTICLE_WHITELIST = t
    end
end

-- 读取并消费一条外部重预测请求；revision 用来忽略旧请求或重复请求。
local function read_external_prediction_request()
    for _, path in ipairs(get_request_file_paths()) do
        local file = io.open(path, "r")
        if file then
            local revision_line = file:read("*l")
            local anchor = file:read("*a")
            file:close()
            local revision = tonumber(revision_line)
            if revision and revision > last_external_request_revision and anchor then
                anchor = string.gsub(anchor, "^%s+", "")
                anchor = string.gsub(anchor, "%s+$", "")
                if anchor ~= "" then
                    last_external_request_revision = revision
                    os.remove(path)
                    return anchor
                end
            end
        end
    end
    return nil
end

-- 用 Java 提供的删后锚点临时重建 history/last_commit，再复用现有 get_predictions 流程。
local function activate_external_prediction(env, anchor)
    if not anchor or anchor == "" then return false end
    for i = 1, #history do history[i] = nil end
    insert(history, anchor)
    last_commit = anchor
    last_commit_time = rime_api and rime_api.get_time_ms and rime_api.get_time_ms() or (os_time() * 1000)
    predict_count = 1
    pending_cands = get_predictions(env, anchor)
    if pending_cands then
        is_predicting = true
        set_prediction_visible(env, true)
        return true
    end
    predict_count = 0
    is_predicting = false
    pending_cands = nil
    set_prediction_visible(env, false)
    return false
end

-- 重置记忆链：清空所有上下文状态
-- 在语境超时、标点断句、外部打断等场景调用
local function reset_memory_chain(env, reason)
    for i = 1, #history do history[i] = nil end
    last_commit = ""
    last_commit_time = 0
    predict_count = 0
    is_predicting = false
    prediction_visible = false
    pending_cands = nil
    env.need_push = false
end

-- schema deploy/reload 后显式清空文件级共享状态，避免旧 env 的预测链残留到新一轮输入。
local function reset_runtime_state(env)
    reset_memory_chain(env, "runtime init")
    last_external_request_revision = 0
end

-- 提交完成后优先立即注入占位符，减少 deploy/reload 后首轮 update_notifier
-- 时序不稳定导致的预测丢失；若当前上下文还不能安全注入，则退回到 update_cb 兜底。
local function push_prediction_placeholder(ctx, env)
    if not ctx or not pending_cands then return false end
    local input = ctx.input or ""
    if input ~= "" then return false end
    local expected_len = utf8_len(PH_CHAR) or 1
    env.need_push = false
    ctx:push_input(PH_CHAR)
    ctx.caret_pos = expected_len
    return true
end

set_prediction_visible = function(env, visible)
    prediction_visible = visible
end

-- 获取 LevelDB 实例（带连接池）
-- db_name 由 user_predict/db_name 配置（默认 "lua/predict"）
local _db_pool = {}
local function get_db(env)
    local config = env.engine.schema.config
    local db_name = config:get_string("user_predict/db_name") or "lua/predict"
    if not _db_pool[db_name] then _db_pool[db_name] = LevelDb(db_name) end
    local db = _db_pool[db_name]
    if db and not db:loaded() then db:open() end
    return db
end

-- 判断是否是 CJK 汉字（包括扩展区 A~I）
local function is_chinese_char(char)
    local cp = utf8 and utf8.codepoint(char) or 0
    if not cp or cp == 0 then return false end
    return (cp >= 0x4E00 and cp <= 0x9FFF)
        or (cp >= 0x3400 and cp <= 0x4DBF)
        or (cp >= 0x20000 and cp <= 0x2A6DF)
        or (cp >= 0x2A700 and cp <= 0x2B73F)
        or (cp >= 0x2B740 and cp <= 0x2B81F)
        or (cp >= 0x2B820 and cp <= 0x2CEAF)
        or (cp >= 0x2CEB0 and cp <= 0x2EBEF)
        or (cp >= 0x30000 and cp <= 0x3134F)
        or (cp >= 0x31350 and cp <= 0x323AF)
        or (cp >= 0x2EBF0 and cp <= 0x2EE5F)
        or (cp >= 0xF900 and cp <= 0xFAFF)
        or (cp >= 0x2F800 and cp <= 0x2FA1F)
        or (cp >= 0x2E80 and cp <= 0x2EFF)
        or (cp >= 0x2F00 and cp <= 0x2FDF)
end

-- 判断是否为可记录的上屏文本
-- 只记录纯汉字或终结标点，过滤英文/数字/符号
local function is_valid_commit_text(text)
    if not text or text == "" then return false end
    for c in s_gmatch(text, "[%z\1-\127\194-\244][\128-\191]*") do
        if not is_chinese_char(c) and not is_terminal_char(c) then return false end
    end
    return true
end

-- 将 UTF-8 字符串拆分为单字符列表
-- 用于 P-Gram 后缀截取
local function get_utf8_chars(str)
    if not str or str == "" then return {} end
    local chars = {}
    if utf8 and utf8.codes then
        for _, c in utf8.codes(str) do insert(chars, utf8.char(c)) end
    else
        for c in s_gmatch(str, "[%z\1-\127\194-\244][\128-\191]*") do insert(chars, c) end
    end
    return chars
end

-- 根据字符串长度返回需要学习的后缀长度列表
-- len=4: {4,3,2}  len=3: {3,2}  len=2: {2}  len=1: {1}
-- 用于 P-Gram（模糊后缀匹配）的写入和查询
local function get_suffix_lengths(len)
    if len >= 4 then return { 4, 3, 2 }
    elseif len == 3 then return { 3, 2 }
    elseif len == 2 then return { 2 }
    elseif len == 1 then return { 1 } end
    return {}
end

-- ======================== 瀑布流预测核心 ========================
-- get_predictions(env, prev_commit) → {word, weight, db_key}[]
-- 四级瀑布流查询，逐级降级：
--   S-Gram (句子级精确匹配, ×1000000)
--   → 2-Gram (二元精确匹配, ×10000)
--   → 1-Gram (一元匹配, ×100)
--   → P-Gram (模糊后缀匹配, ×1)
-- 权重 = count × DECAY_RATE^age_days × multiplier
-- 同时负责过期数据清理（查询时发现过期则删除）
get_predictions = function(env, prev_commit)
    if not prev_commit or prev_commit == "" then return nil end
    local db = get_db(env)
    if not db then return nil end
    local cands = {}
    local seen = {}
    -- 排除刚上屏的词本身，避免原地重复
    seen[prev_commit] = true
    local now = os_time()

    -- 内部函数：查询指定前缀并清洗过期数据
    local function fetch_and_clean(query_key, multiplier)
        local da = db:query(query_key)
        if not da then return end
        local scan_count = 0
        local prefix_cands = {}
        for k, v in da:iter() do
            if scan_count >= CONFIG.SCAN_LIMIT then break end
            if s_sub(k, 1, 1) ~= "\1" then
                -- 从 key 中提取后续词 word
                local word = s_sub(k, s_len(query_key) + 1)
                -- value 格式：count|timestamp
                local c_str, ts_str = s_match(v, "^([^|]+)|?(.*)$")
                local count = tonumber(c_str) or 0
                local ts = tonumber(ts_str) or 0
                local is_p_gram = (s_sub(k, 1, 2) == "P\t")
                local limit = is_p_gram and CONFIG.P_EXPIRY_SECONDS or CONFIG.EXPIRY_SECONDS
                if ts == 0 then ts = now - limit - 1 end
                if (now - ts) > limit then
                    -- 过期：物理删除
                    if db.erase then db:erase(k) else db:update(k, "") end
                else
                    if count > 0 then
                        local age_days = (now - ts) / 86400.0
                        local score = count * (CONFIG.DECAY_RATE ^ age_days) * multiplier
                        if score > 0.05 and word ~= "" then
                            insert(prefix_cands, { word = word, weight = score, db_key = k })
                        end
                    end
                end
            end
            scan_count = scan_count + 1
        end
        da = nil
        if #prefix_cands > 0 then
            sort(prefix_cands, function(a, b) return a.weight > b.weight end)
            for i, c in ipairs(prefix_cands) do
                if i <= CONFIG.MAX_MEMORY_BRANCHES then
                    if not seen[c.word] then insert(cands, c); seen[c.word] = true end
                else
                    -- 超过分支上限的候选置零（保留 key 但清零权重）
                    db:update(c.db_key, "0|" .. tostring(now))
                end
            end
        end
    end

    -- 第一级：S-Gram — 句子级精确匹配
    -- key 格式：S\t<上文全文>\t<后续词>
    if #history >= 1 then
        fetch_and_clean("S\t" .. history[#history] .. "\t", 1000000)
    end

    -- 第二级：2-Gram — 二元组精确匹配
    -- key 格式：2\t<前词>\t<后词>\t<后续词>
    -- 条件：后词[≤4字] 且 (前词+后词)[≤5字]
    if #cands < CONFIG.MAX_CANDIDATES and #history >= 2 then
        local u0 = history[#history - 1]
        local u1 = history[#history]
        local len_u0 = u0 and utf8_len(u0) or 0
        local len_u1 = u1 and utf8_len(u1) or 0
        if len_u1 <= 4 and (len_u0 + len_u1) <= 5 then
            fetch_and_clean("2\t" .. u0 .. "\t" .. u1 .. "\t", 10000)
        end
    end

    -- 第三级：1-Gram — 一元组匹配
    -- key 格式：1\t<前词后缀>\t<后续词>
    -- 取前词的后2-4字做降级匹配
    if #cands < CONFIG.MAX_CANDIDATES and #history >= 1 then
        local u1 = history[#history]
        local chars = get_utf8_chars(u1)
        local len_u1 = #chars
        local max_len = math_min(len_u1, 4)
        local min_len = (len_u1 >= 2) and 2 or 1
        for l = max_len, min_len, -1 do
            local lookup = table.concat(chars, "", len_u1 - l + 1, len_u1)
            fetch_and_clean("1\t" .. lookup .. "\t", 100)
            if #cands > 0 then break end
        end
    end

    -- 第四级：P-Gram — 模糊后缀匹配（抗抖动）
    -- key 格式：P\t<上文后缀>\t<后续词>
    -- 用当前上屏文本的后缀做模糊匹配
    if #cands < CONFIG.MAX_CANDIDATES then
        local chars = get_utf8_chars(prev_commit)
        local lengths = get_suffix_lengths(#chars)
        for _, l in ipairs(lengths) do
            fetch_and_clean("P\t" .. table.concat(chars, "", #chars - l + 1, #chars) .. "\t", 1)
            if #cands > 0 then break end
        end
    end

    -- 第五级：F-Gram — 字尾→词语静态映射（兜底）
    -- 当 LevelDB 中没有任何匹配数据时，从预生成字头词表中查询
    -- char_words.lua 由 script/generate_char_words.py 从虎码词库生成
    -- 取上屏文本的末 1~5 字逐级查表（长前缀优先），返回去掉首字的"后缀"
    -- 例如 明→晚(明晚)  明晚→上(晚上)  人生自古→谁无死(人生自古谁无死)
    -- 后缀上屏后自然与前文组成完整词语
    if #cands < CONFIG.MAX_CANDIDATES then
        local chars = get_utf8_chars(prev_commit)
        local n_chars = #chars
        local fw = ensure_char_words()
        -- 从最长前缀开始尝试（最多 5 字）
        local max_f_prefix = math_min(n_chars, 5)
        for plen = max_f_prefix, 1, -1 do
            local prefix = table.concat(chars, "", n_chars - plen + 1, n_chars)
            -- 逐个字符检查是否全中文
            local pchars = get_utf8_chars(prefix)
            local all_cjk = true
            for _, pc in ipairs(pchars) do
                if not is_chinese_char(pc) then all_cjk = false; break end
            end
            if all_cjk then
                local fallback_list = fw[prefix]
                if fallback_list then
                    for _, w in ipairs(fallback_list) do
                        local w_chars = get_utf8_chars(w)
                        local w_len = #w_chars
                        if w_len > plen then
                            local suffix = table.concat(w_chars, "", plen + 1, w_len)
                            if not seen[suffix] then
                                insert(cands, { word = suffix, weight = 0.05, db_key = "F\t" .. w })
                                seen[suffix] = true
                                if #cands >= CONFIG.MAX_CANDIDATES then break end
                            end
                        end
                    end
                    if #cands > 0 then break end  -- 命中即停止
                end
            end
        end
    end

    if #cands > 0 then
        sort(cands, function(a, b) return a.weight > b.weight end)
        return cands
    end
    return nil
end

-- ======================== 删除预测候选 ========================
-- 从前端删除/手势删词时同步清除 LevelDB 中的对应条目
local function remove_predict_candidate(env, word)
    local db = get_db(env)
    local exact_key = nil
    if pending_cands then
        for _, c in ipairs(pending_cands) do
            if c.word == word then exact_key = c.db_key; break end
        end
    end
    if exact_key then
        if db.erase then db:erase(exact_key) else db:update(exact_key, "") end
    end
    -- 同时删除 P-Gram 关联
    local chars = get_utf8_chars(last_commit)
    local lengths = get_suffix_lengths(#chars)
    for _, l in ipairs(lengths) do
        local p_key = "P\t" .. table.concat(chars, "", #chars - l + 1, #chars) .. "\t" .. word
        if db.erase then db:erase(p_key) else db:update(p_key, "") end
    end
end

-- ======================== 过期数据批量清理 ========================
-- 每 3 天扫描一次全库，物理删除超期条目
local function clean_expired(env)
    local db = get_db(env)
    if not db then return end
    local now = os_time()
    local CLEAN_INTERVAL = 259200 -- 3 天
    local last_clean_str = db:fetch("\0last_clean_time")
    local last_clean_time = tonumber(last_clean_str) or 0
    if (now - last_clean_time) > CLEAN_INTERVAL then
        local deleted = 0
        for k, v in db:query(""):iter() do
            if s_sub(k, 1, 1) ~= "\1" and s_sub(k, 1, 1) ~= "\0" then
                local _, ts_str = s_match(v, "^([^|]+)|?(.*)$")
                local ts = tonumber(ts_str) or 0
                local is_p_gram = (s_sub(k, 1, 2) == "P\t")
                local limit = is_p_gram and CONFIG.P_EXPIRY_SECONDS or CONFIG.EXPIRY_SECONDS
                if ts == 0 then ts = now - limit - 1 end
                if (now - ts) > limit then
                    if db.erase then db:erase(k) else db:update(k, "") end
                    deleted = deleted + 1
                end
            end
        end
        db:update("\0last_clean_time", tostring(now))
        if deleted > 0 then
        end
    end
end

-- ====================================================================
--   Processor (P) — 物理按键截取与逻辑分发
--   注册名：lua_processor@user_predict_processor
--   功能：
--     ① commit_notifier → 监听上屏事件，自训练写入 LevelDB
--     ② update_notifier → 注入占位符 ››› 触发 Translator
--     ③ delete_notifier → 处理前端删词同步
--     ④ P.func → 监听按键并维护数据库撤销栈
-- ====================================================================
local P = {}

-- P.init: 处理器初始化
-- 在 Rime 引擎加载 schema 时调用
function P.init(env)
    load_config(env)
    local db = get_db(env)
    clean_expired(env)
    reset_runtime_state(env)
    env.need_push = false         -- 是否需要注入占位符
    env.last_written_keys = {}    -- 最近一次写库的 key-value 快照（用于回滚）
    env.just_committed = false    -- 是否刚上屏

    -- ============ commit_notifier 回调 ============
    -- 当 Rime 引擎产生 commit（上屏文本）时触发
    -- 在此回调中：记录上屏文本 → 更新记忆链 → 自训练写入 LevelDB → 查询预测候选
    env.commit_cb = function(ctx)
        local text = ctx:get_commit_text()
        -- 过滤非汉字/非标点文本（如英文、数字、编码等不记录）
        if not is_valid_commit_text(text) then
            reset_memory_chain(env, "non-Chinese text")
            return
        end

        -- 语境超时检测：两次上屏间隔超过 CONTEXT_TIMEOUT_MS 则重置记忆链
        local current_time = rime_api and rime_api.get_time_ms and rime_api.get_time_ms() or (os_time() * 1000)
        if last_commit ~= "" and (current_time - last_commit_time) > CONFIG.CONTEXT_TIMEOUT_MS then
            reset_memory_chain(env, "context timeout")
        end

        -- 预测轮次计数：第一次上屏为预测开始
        if not is_predicting then
            is_predicting = true
            predict_count = 1
        else
            predict_count = predict_count + 1
        end

        -- 达到最大预测次数后停止
        if predict_count > CONFIG.MAX_PREDICTIONS then
            is_predicting = false
            predict_count = 0
            pending_cands = nil
            return
        end

        -- ============ 自训练写入 LevelDB ============
        -- update_memory(key, is_tone, delta): 写入或更新一条 n-gram 记录
        -- key 格式：<gram_type>\t<前缀>\t<后续词>
        -- value 格式：count|timestamp
        env.last_written_keys = {}
        local function update_memory(key, is_tone, delta)
            local now = os_time()
            delta = delta or 1
            local val = db:fetch(key)
            env.last_written_keys[key] = val or ""
            if not val or val == "" then
                db:update(key, tostring(delta) .. "|" .. tostring(now))
            else
                local c_str, ts_str = s_match(val, "^([^|]+)|?(.*)$")
                local count = tonumber(c_str) or 0
                local ts = tonumber(ts_str) or 0
                local age = now - ts
                if age > CONFIG.EXPIRY_SECONDS then
                    db:update(key, tostring(delta) .. "|" .. tostring(now))
                else
                    db:update(key, tostring(count + delta) .. "|" .. tostring(now))
                end
            end
        end

        -- ============ 防干扰检查 ============
        current_time = rime_api and rime_api.get_time_ms and rime_api.get_time_ms() or (os_time() * 1000)
        local should_record = true
        local is_terminal = false
        local text_chars = get_utf8_chars(text)
        local len_text = #text_chars

        -- 规则1：单次上屏超过 4 个字不记录
        if len_text > 4 then should_record = false end

        -- 规则2：标点断句 — 终结符上屏时，检查上文是否为语助词白名单
        -- 语助词接终结符 → 正常结束，放行
        -- 非语助词接终结符 → 重置记忆链（一句话结束了）
        if should_record and is_terminal_char(text) then
            local prev_chars = get_utf8_chars(last_commit)
            local last_char = prev_chars[#prev_chars] or ""
            if not PARTICLE_WHITELIST[last_char] then
                should_record = false
                reset_memory_chain(env, "terminal punctuation")
            else
                is_terminal = true
            end
        end

        -- 规则3：ABA 防折返 — 连续上屏相同文本不记录
        if should_record and last_commit == text then should_record = false end
        -- 规则3a：A→B→A 回环检测
        if should_record and #history >= 2 then
            if text == history[#history - 1] then
                should_record = false
                remove(history, #history)
                last_commit = history[#history] or ""
            end
        end

        -- ============ 核心录入逻辑 ============
        -- 按 S-Gram / 2-Gram / 1-Gram / P-Gram 写入数据库
        if should_record then
            if last_commit ~= "" then
                local u1_chars = get_utf8_chars(last_commit)
                local len_u1 = #u1_chars

                -- P-Gram：以上文后缀为前缀
                local lengths_to_learn = get_suffix_lengths(len_u1)
                for _, l in ipairs(lengths_to_learn) do
                    if l < len_u1 or len_u1 >= 4 then
                        update_memory("P\t" .. table.concat(u1_chars, "", len_u1 - l + 1, len_u1) .. "\t" .. text, false)
                    end
                end

                -- 1-Gram：以上文全文为前缀
                if len_u1 <= 4 and #history >= 1 then
                    update_memory("1\t" .. last_commit .. "\t" .. text, false)
                end

                -- 2-Gram：以上文 + 上上文为前缀
                if len_u1 <= 4 and #history >= 2 then
                    local u0 = history[#history - 1]
                    local len_u0 = u0 and #get_utf8_chars(u0) or 0
                    if (len_u0 + len_u1) <= 5 then
                        update_memory("2\t" .. u0 .. "\t" .. last_commit .. "\t" .. text, false)
                    end
                end
            end

            -- 词内续写学习：只学习“首字 -> 剩余部分”，并降低权重，避免压过正常上下文预测。
            -- 例如：好笑 -> 好 => 笑， 好半天 -> 好 => 半天。
            if len_text >= CONFIG.INTERNAL_FIRST_MIN_LEN and len_text <= CONFIG.INTERNAL_FIRST_MAX_LEN then
                local prefix = text_chars[1] or ""
                local suffix = table.concat(text_chars, "", 2, len_text)
                if prefix ~= "" and suffix ~= "" then
                    update_memory("1\t" .. prefix .. "\t" .. suffix, false, CONFIG.INTERNAL_FIRST_WEIGHT)
                end
            end

            -- 前 2 字续写学习：对较长词，若前 2 字已存在于 1-Gram 或 P-Gram，
            -- 额外学习“前 2 字 -> 剩余部分”，帮助补足如“好半天”这类更长续写。
            if len_text >= CONFIG.INTERNAL_PAIR_MIN_LEN and len_text <= CONFIG.INTERNAL_PAIR_MAX_LEN then
                local part1 = text_chars[1] .. text_chars[2]
                local part2 = table.concat(text_chars, "", 3, len_text)
                local is_known_prefix = false
                for _, prefix in ipairs({ "1", "P" }) do
                    local query_key = prefix .. "\t" .. part1 .. "\t"
                    local da = db:query(query_key)
                    if da then
                        for k, _ in da:iter() do
                            if s_find(k, query_key, 1, true) then is_known_prefix = true; break end
                        end
                    end
                    if is_known_prefix then break end
                end
                if is_known_prefix then
                    update_memory("1\t" .. part1 .. "\t" .. part2, false, CONFIG.INTERNAL_PAIR_WEIGHT)
                end
            end
        end

        -- ============ 更新记忆链 ============
        if should_record then
            if is_terminal then
                -- 语助词接终结符 → 结束当前预测链，不记录到 history
                reset_memory_chain(env, "terminal committed")
            else
                insert(history, text)
                if #history > HISTORY_MAX then remove(history, 1) end
                last_commit = text
            end
        end

        -- ============ 撤销栈 ============
        -- 每次写库后保存快照到 undo_stack，最多 3 级
        env.undo_stack = env.undo_stack or {}
        if next(env.last_written_keys) then
            insert(env.undo_stack, env.last_written_keys)
            if #env.undo_stack > 3 then remove(env.undo_stack, 1) end
        end

        last_commit_time = current_time
        env.last_action_time = current_time
        env.just_committed = true

        -- ============ 查询预测候选 ============
        if predict_count <= CONFIG.MAX_PREDICTIONS and ctx:get_option("prediction") then
            pending_cands = get_predictions(env, last_commit)
            if pending_cands then
                env.need_push = true  -- 优先立即注入；若首轮时序不对，再由 update_notifier 兜底
                set_prediction_visible(env, true)
                push_prediction_placeholder(ctx, env)
            else
                predict_count = 0; is_predicting = false; pending_cands = nil
                set_prediction_visible(env, false)
            end
        else
            predict_count = 0; is_predicting = false; pending_cands = nil
            set_prediction_visible(env, false)
        end
    end

    -- ============ update_notifier 回调 ============
    -- 当 Rime 引擎的 Context 更新时触发
    -- 核心职责：在 commit 完成后立即注入占位符 ››› 触发 Translator 生成预测候选
    -- 同时负责：清理被用户操作打断的预测状态
    env.update_cb = function(ctx)
        local input = ctx.input or ""
        local expected_ph = PH_CHAR
        local expected_len = utf8_len(PH_CHAR) or 1
        if input == PH_CHAR then
            -- 删后重预测优先消费外部锚点；没有外部请求时再按原来的 pending_cands 逻辑显示预测。
            local external_anchor = read_external_prediction_request()
            if external_anchor then
                if not activate_external_prediction(env, external_anchor) then
                    ctx:clear()
                    reset_memory_chain(env, "external prediction empty")
                    return
                end
                -- Java 先注入占位符，Lua 再读取请求文件并填充 pending_cands；
                -- 这里需要主动重放一次占位符更新，确保 Translator 在候选已就绪后重新运行。
                ctx:clear()
                ctx:push_input(expected_ph)
                ctx.caret_pos = expected_len
                return
            elseif pending_cands then
                is_predicting = true
                set_prediction_visible(env, true)
            end
        end

        -- deploy 后首轮预测里，Rime 可能在内部刷新时短暂上报一个空 input；
        -- 这不是用户真的打断预测，不能把刚生成的候选链直接清掉。
        -- 这里只把“出现了非空且不是占位符的真实输入”视为外部打断。
        if is_predicting and input ~= "" and input ~= PH_CHAR and not env.need_push then
            reset_memory_chain(env, "external input clear")
            ctx:clear()
        end

        -- ============ 注入占位符 ››› ============
        -- 核心机制：commit_cb 中查询到预测候选后设置 need_push=true
        -- update_cb 检测到 input 为空且 need_push → 调用 ctx:push_input("›››")
        -- push_input 会触发 Rime Context::Update()，重新运行翻译器
        -- Translator 看到输入为 ››› 时从 pending_cands 生成预测候选
        -- 这样候选栏在上屏后保持显示 isComposing=true
        if env.need_push and input == "" then
            if push_prediction_placeholder(ctx, env) then return end
        end

        -- ============ 占位符清理 ============
        -- 如果输入包含占位符但不是预期的形式（被外部修改过）→ 清理
        if s_find(input, PH_CHAR, 1, true) then
            if input ~= expected_ph then
                local clean_text = string.gsub(input, PH_CHAR, "")
                ctx:clear()
                predict_count = 0
                is_predicting = false
                pending_cands = nil
                set_prediction_visible(env, false)
                if clean_text ~= "" then ctx:push_input(clean_text) end
                return
            else
                -- 光标准确位置保护：占位符不能参与实际输入
                if ctx.caret_pos < expected_len then
                    ctx:clear()
                    predict_count = 0
                    is_predicting = false
                    pending_cands = nil
                    set_prediction_visible(env, false)
                    return
                end
            end
        end
    end

    -- ============ delete_notifier 回调 ============
    -- 前端删除/手势删词时同步清除数据库条目
    env.delete_cb = function(ctx)
        local comp = ctx.composition
        if not comp or comp:empty() then return end
        local seg = comp:back()
        local idx = seg.selected_index
        local cand = seg:get_candidate_at(idx)
        if cand and cand.type == "predict" then
            remove_predict_candidate(env, cand.text)
            ctx:clear()
            reset_memory_chain(env, "delete predict candidate")
        end
    end

    -- ============ 注册 Notifier ============
    env.commit_connection = env.engine.context.commit_notifier:connect(env.commit_cb)
    env.update_connection = env.engine.context.update_notifier:connect(env.update_cb)
    env.delete_connection = env.engine.context.delete_notifier:connect(env.delete_cb)
end

-- P.func: 处理器按键事件
-- 每次按键时调用，返回值：
--   0 = kRejected（不处理）
--   1 = kAccepted（按键被消费，不再传递）
--   2 = kNoop（不处理，继续传递）
function P.func(key, env)
    local ctx = env.engine.context
    local input = ctx.input
    if not input then return 2 end
    if key:release() then return 2 end
    local repr = key:repr()

    -- 刚上屏后按非修饰键 → 清除 just_committed 标记
    if env.just_committed and repr ~= "BackSpace"
        and not s_find(repr, "Shift", 1, true)
        and not s_find(repr, "Control", 1, true)
        and not s_find(repr, "Alt", 1, true) then
        env.just_committed = false
    end

    -- ============ BackSpace 撤销 ============
    -- 在 CONTEXT_TIMEOUT_MS 内快速连按退格时，可撤销最近 3 次数据库写入。
    if repr == "BackSpace" then
        local current_time = rime_api and rime_api.get_time_ms and rime_api.get_time_ms() or (os_time() * 1000)
        local is_safe_to_undo = (not ctx:is_composing() or is_predicting)
        if is_safe_to_undo and env.undo_stack and #env.undo_stack > 0 then
            if (current_time - (env.last_action_time or 0)) <= CONFIG.CONTEXT_TIMEOUT_MS then
                local keys_to_undo = remove(env.undo_stack)
                local db = get_db(env)
                for k, v in pairs(keys_to_undo) do
                    if v == "" then
                        if db.erase then db:erase(k) else db:update(k, "") end
                    else
                        db:update(k, v)
                    end
                end
                env.last_action_time = current_time
            else
                -- 超时则清空回滚栈
                env.undo_stack = {}
            end
        end
        env.just_committed = false
    end

    -- ============ 预测状态下按键处理 ============
    -- Space/Enter → 仅当存在已选中的预测候选时放行给 selector 处理
    -- 否则说明输入区只剩占位符本身，绝不能让其原样上屏
    if is_predicting then
        if repr == "Return" or repr == "KP_Enter" or key.keycode == 0x20 then
            local comp = ctx.composition
            local has_predict_cand = false
            if comp and not comp:empty() then
                local seg = comp:back()
                local cand = seg:get_candidate_at(seg.selected_index)
                has_predict_cand = cand ~= nil and cand.type == "predict"
            end
            if has_predict_cand then
                return 2  -- 放行给 express_editor/selector 处理
            end
            ctx:clear()
            reset_memory_chain(env, "enter/space with no predict candidate")
            return 1
        end
        -- 其他键 → 打断预测状态
        ctx:clear()
        reset_memory_chain(env, "keypress breaks prediction")
        return 2
    end

    return 2
end

-- P.fini: 处理器析构
-- 解除所有 notifier 连接，防止内存泄漏
function P.fini(env)
    if env.commit_connection then env.commit_connection:disconnect(); env.commit_connection = nil end
    if env.update_connection then env.update_connection:disconnect(); env.update_connection = nil end
    if env.delete_connection then env.delete_connection:disconnect(); env.delete_connection = nil end
end

-- ====================================================================
--   Translator (T) — 预测候选生成
--   注册名：lua_translator@user_predict_translator
--   当 Processor 注入占位符 ››› 后，此翻译器从 pending_cands 生成候选
-- ====================================================================
local T = {}

function T.init(env)
    load_config(env)
    get_db(env)
end

-- T.func: 翻译器主函数
    -- input 为占位符时，从 pending_cands 生成 Candidate 类型为 "predict"
function T.func(input, seg, env)
    -- 受总开关控制
    if not env.engine.context:get_option("prediction") then return end
    -- 删后重预测有时会错过 update_cb 的消费时机；这里在 Translator 入口兜底再读一次请求文件。
    if input == PH_CHAR and not pending_cands then
        local external_anchor = read_external_prediction_request()
        if external_anchor then
            activate_external_prediction(env, external_anchor)
        end
    end
    -- 只有输入为精确占位符时才产出预测候选
    if input == PH_CHAR and pending_cands then
        is_predicting = true
        set_prediction_visible(env, true)
        local count = 0
        for _, c in ipairs(pending_cands) do
            if count >= CONFIG.MAX_CANDIDATES then break end
            -- Candidate(type, start, end, text, comment)
            -- type="predict" 可在 UI 层区分
            local cand = Candidate("predict", seg.start, seg._end, c.word, "")
            yield(cand)
            count = count + 1
        end
    end
end

function T.fini(env) end

-- ====================================================================
--   Filter (F) — 输入过程调频
--   注册名：lua_filter@user_predict_filter
--   在常规输入中，根据上下文预测提升匹配候选的排序位置
-- ====================================================================
local F = {}

function F.init(env)
    load_config(env)
end

-- 辅助函数：先出提升候选，再出普通候选
local function flush_yield(b_list, b_cnt, n_list, n_cnt)
    for i = 1, b_cnt do yield(b_list[i].cand) end
    for i = 1, n_cnt do yield(n_list[i]) end
end

-- F.func: 过滤器主函数
-- 在用户输入编码过程中，根据最近上屏文本预测后续词
-- 如果候选词在预测列表中出现，将其提升到前面
function F.func(input, env)
    local ctx = env.engine.context
    local current_input = ctx.input or ""

    -- 占位符阶段只保留预测候选，彻底屏蔽普通码表对 PH_CHAR 的翻译结果。
    if current_input == PH_CHAR then
        for cand in input:iter() do
            if cand.type == "predict" then yield(cand) end
        end
        return
    end

    -- 非预测占位符阶段不执行调频（由 Translator 全权处理）
    if not ctx:get_option("prediction") then
        for cand in input:iter() do yield(cand) end
        return
    end

    -- 获取预测映射表：word → rank
    local f_reorder_map = {}
    local preds = get_predictions(env, last_commit)
    if preds then
        for rank, p in ipairs(preds) do
            f_reorder_map[p.word] = rank
        end
    end

    -- 无预测数据 → 透传
    if not next(f_reorder_map) then
        for cand in input:iter() do yield(cand) end
        return
    end

    -- 遍历候选列表，将匹配预测的候选提升
    local b_list = {}
    local n_list = {}
    local b_cnt = 0
    local n_cnt = 0
    local count = 0
    local target_len = 0
    local target_end = 0

    for cand in input:iter() do
        count = count + 1
        local text = cand.text or ""
        local current_len = utf8_len(text) or 0
        if count == 1 then
            target_len = current_len
            target_end = cand._end
        end
        -- 不同长度的候选中止调频（只对同长度候选排序）
        if cand._end ~= target_end or current_len ~= target_len then
            flush_yield(b_list, b_cnt, n_list, n_cnt)
            yield(cand)
            for rest in input:iter() do yield(rest) end
            return
        end
        local rank = f_reorder_map[text]
        if rank then
            b_cnt = b_cnt + 1
            b_list[b_cnt] = { cand = cand, rank = rank, index = count }
        else
            n_cnt = n_cnt + 1
            n_list[n_cnt] = cand
        end
    end

    -- 按排名排序（稳定排序，同排名保持原顺序）
    sort(b_list, function(a, b)
        if a.rank == b.rank then return a.index < b.index end
        return a.rank < b.rank
    end)
    flush_yield(b_list, b_cnt, n_list, n_cnt)
end

function F.fini(env) end

-- ======================== 模块导出 ========================
-- 直接把组件挂到全局，避免某些加载顺序下仅靠 rime.lua 中转赋值时拿不到对应组件。
user_predict_processor = P
user_predict_translator = T
user_predict_filter = F

-- librime-lua 自动根据注册名加载对应组件
--   lua_processor@user_predict_processor    → P (Processor)
--   lua_translator@user_predict_translator  → T (Translator)
--   lua_filter@user_predict_filter          → F (Filter)
return { P = P, T = T, F = F }
