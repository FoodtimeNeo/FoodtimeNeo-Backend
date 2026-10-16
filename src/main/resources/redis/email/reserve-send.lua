-- KEYS: code, cooldown, account counter, IP counter. ARGV: token, cooldown, window, account limit, IP limit.
redis.replicate_commands()
local now = tonumber(redis.call('TIME')[1])
local lockUntil = tonumber(redis.call('HGET', KEYS[1], 'lockUntil') or '0')
if lockUntil > now then return lockUntil - now end
local cooldown = redis.call('TTL', KEYS[2])
if cooldown > 0 then return cooldown end
local a = tonumber(redis.call('GET', KEYS[3]) or '0')
local b = tonumber(redis.call('GET', KEYS[4]) or '0')
local retry = 0
if a >= tonumber(ARGV[4]) then retry = math.max(retry, redis.call('TTL', KEYS[3])) end
if b >= tonumber(ARGV[5]) then retry = math.max(retry, redis.call('TTL', KEYS[4])) end
if a >= tonumber(ARGV[4]) or b >= tonumber(ARGV[5]) then return math.max(1, retry) end
for i = 3, 4 do
  if redis.call('INCR', KEYS[i]) == 1 then redis.call('EXPIRE', KEYS[i], ARGV[3]) end
end
redis.call('SET', KEYS[2], ARGV[1], 'EX', ARGV[2])
return 0
