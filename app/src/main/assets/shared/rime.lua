charset_comment_filter = require("charset_comment_filter") --Unicode分区提示
core2022 = require("core2022_filter") --自定义字符集过滤（常用字集）
-- dz_ci = require("dz_ci_filter") --单字模式 这个别用，有问题的
number_translator = require("number")
lua_unicode_display_filter = require("unicode_display")  --Unicode编码提示
calculator_translator = require("calculator_translator")  --简易计算器
exe_processor = require("exe")  -- 网页启动器
shijian_translator = require("shijian2") -- 高级时间
local user_predict = require("user_predict") -- 上屏后预测模块

-- 显式包装一次，避免 librime-lua 在组件分发表上取不到 func 时出现 upvalue 'f' 为空。
user_predict_processor = {
  init = function(env)
    if user_predict.P and user_predict.P.init then return user_predict.P.init(env) end
  end,
  func = function(key, env)
    if user_predict.P and user_predict.P.func then return user_predict.P.func(key, env) end
  end,
  fini = function(env)
    if user_predict.P and user_predict.P.fini then return user_predict.P.fini(env) end
  end,
}

user_predict_translator = {
  init = function(env)
    if user_predict.T and user_predict.T.init then return user_predict.T.init(env) end
  end,
  func = function(input, seg, env)
    if user_predict.T and user_predict.T.func then return user_predict.T.func(input, seg, env) end
  end,
  fini = function(env)
    if user_predict.T and user_predict.T.fini then return user_predict.T.fini(env) end
  end,
}

user_predict_filter = {
  init = function(env)
    if user_predict.F and user_predict.F.init then return user_predict.F.init(env) end
  end,
  func = function(input, env)
    if user_predict.F and user_predict.F.func then return user_predict.F.func(input, env) end
  end,
  fini = function(env)
    if user_predict.F and user_predict.F.fini then return user_predict.F.fini(env) end
  end,
}
