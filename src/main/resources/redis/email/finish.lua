-- KEYS: code. ARGV: claim token, 'consume' or 'release'. Cannot touch a newer claim/code.
if redis.call('HGET', KEYS[1], 'lock') ~= ARGV[1] then return 0 end
if ARGV[2] == 'consume' then redis.call('DEL', KEYS[1])
else redis.call('HDEL', KEYS[1], 'lock', 'lockUntil') end
return 1
