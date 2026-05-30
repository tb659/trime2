-- utils.lua
local utils = {}

function utils.clone(t)
  local result = {}
  for k, v in pairs(t) do
    result[k] = v
  end
  return result
end

function utils.merge(t1, t2)
  local result = utils.clone(t1)
  for k, v in pairs(t2) do
    result[k] = v
  end
  return result
end

function utils.deep_clone(t)
  if type(t) ~= "table" then return t end
  local result = {}
  for k, v in pairs(t) do
    result[k] = utils.deep_clone(v)
  end
  return result
end

function utils.deep_merge(t1, t2)
  local result = utils.clone(t1)
  for k, v in pairs(t2) do
    if type(result[k]) == "table" and type(v) == "table" then
      result[k] = utils.deep_merge(result[k], v)
    else
      result[k] = v
    end
  end
  return result
end

return utils