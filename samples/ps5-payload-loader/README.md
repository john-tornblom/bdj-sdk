# PS5 ELF Loader
This is a BD-J Xlet which starts socket servers that launches payloads
transmitted via TCP (ELFs on port 9020, JARs on port 9025, and Lua scripts on
port 9938).

The Xlet relies on a [privilege escalation vulnerability][h1]
discovered by [theflow][theflow], which was later [reproduced for the PS4][insp1]
by [sleirsgoevy][sleirsgoevy]. To escape the Java sandbox, the Xlet uses that
vulnerability to disable the security manager using a technique discovered by
[sleirsgoevy][insp2]. 

[h1]: https://hackerone.com/reports/1379975
[insp1]: https://github.com/sleirsgoevy/bd-jb
[insp2]: https://github.com/sleirsgoevy/bd-jb/tree/ps5
[sleirsgoevy]: https://github.com/sleirsgoevy
[theflow]: https://github.com/TheOfficialFloW
