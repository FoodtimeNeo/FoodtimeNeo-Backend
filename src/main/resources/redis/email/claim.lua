-- KEYS: code. ARGV: proof, claim token, claim lease, max attempts.
-- 0 invalid/expired, 1 claimed, 2 already being registered. Never extend code expiration.
redis.replicate_commands()
if redis.call('EXISTS', KEYS[1]) == 0 then return 0 end
local now = tonumber(redis.call('TIME')[1])
if tonumber(redis.call('HGET', KEYS[1], 'lockUntil') or '0') > now then return 2 end
if redis.call('HGET', KEYS[1], 'proof') ~= ARGV[1] then
  if redis.call('HINCRBY', KEYS[1], 'attempts', 1) >= tonumber(ARGV[4]) then redis.call('DEL', KEYS[1]) end
  return 0
end
redis.call('HSET', KEYS[1], 'lock', ARGV[2], 'lockUntil', tostring(now + tonumber(ARGV[3])))
return 1
