local System = luajava.bindClass('java.lang.System')
local ver = System:getProperty('java.version')
print('Running Java version ' .. ver)
