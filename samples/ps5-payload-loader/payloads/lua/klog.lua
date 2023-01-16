local f = io.open('/dev/klog', 'r')

local line = ''
while true do
   local ch = f:read(1)
   if ch == nil then
      break
   end
   if ch == '\n' then
      print(line)
      line = ''
   else
      line = line .. ch
   end
end
f:close();
