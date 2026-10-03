-- Publish only the most recent send reservation. An active registration must retain its proof.
-- KEYS: code, cooldown. ARGV: send token, HMAC proof, TTL seconds.
redis.replicate_commands()
if redis.call('GET', KEYS[2]) ~= ARGV[1] then return 0 end
local now = tonumber(redis.call('TIME')[1])
if tonumber(redis.call('HGET', KEYS[1], 'lockUntil') or '0') > now then return 0 end
redis.call('DEL', KEYS[1])
redis.call('HSET', KEYS[1], 'proof', ARGV[2], 'attempts', '0')
redis.call('EXPIRE', KEYS[1], ARGV[3])
return 1
