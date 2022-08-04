# PS5 sandbox filedump
This is a proof of concept BD-J Xlet that dumps the PS5 random word sandbox
mounted when running the BD-J player. The Xlet relies on a
[privilege escalation vulnerability][h1] discovered by [theflow][theflow],
which was later [reproduced for the PS4][insp1] by [sleirsgoevy][sleirsgoevy].
To escape the Java sandbox, the Xlet uses that vulnerability to disable
the security manager using a technique discovered by [sleirsgoevy][insp2].

[h1]: https://hackerone.com/reports/1379975
[insp1]: https://github.com/sleirsgoevy/bd-jb
[insp2]: https://github.com/sleirsgoevy/bd-jb/tree/ps5
[sleirsgoevy]: https://github.com/sleirsgoevy
[theflow]: https://github.com/TheOfficialFloW
