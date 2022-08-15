# PS5 Lua server
This is a BD-J Xlet that starts a socket server on port 9938 that interprets
data from incomming connections as Lua code. The Xlet relies on a 
[privilege escalation vulnerability][h1] discovered by [theflow][theflow],
which was later [reproduced for the PS4][insp1] by [sleirsgoevy][sleirsgoevy].
To escape the Java sandbox, the Xlet uses that vulnerability to disable the
security manager using a technique discovered by [sleirsgoevy][insp2].
Once we are out of the sandbox, we can start a socket server use [LuaJ][luaj]
to interpret the code inside the JRE. To launch scripts remotely, you may use
netcat:
```console
john@localhost:~/bdj-sdk/samples/ps5-lua-server$ nc -q0 PS5IP 9938 < scripts/helloworld.lua
```

[h1]: https://hackerone.com/reports/1379975
[insp1]: https://github.com/sleirsgoevy/bd-jb
[insp2]: https://github.com/sleirsgoevy/bd-jb/tree/ps5
[sleirsgoevy]: https://github.com/sleirsgoevy
[theflow]: https://github.com/TheOfficialFloW
[luaj]: https://github.com/luaj/luaj
