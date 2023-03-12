# PS5 FTP Server
This is a BD-J Xlet that launches an FTP server on port 1337.
The FTP server is based off of [ps4-ftp][ps4-ftp] and does not require
a kernel exploit.
The Xlet relies on a [privilege escalation vulnerability][h1] discovered
by [theflow][theflow], which was later [reproduced for the PS4][insp1] by
[sleirsgoevy][sleirsgoevy]. To escape the Java sandbox, the Xlet uses that
vulnerability to disable the security manager using a technique discovered
by [sleirsgoevy][insp2].

[h1]: https://hackerone.com/reports/1379975
[theflow]: https://github.com/TheOfficialFloW
[insp1]: https://github.com/sleirsgoevy/bd-jb
[sleirsgoevy]: https://github.com/sleirsgoevy
[insp2]: https://github.com/sleirsgoevy/bd-jb/tree/ps5
[ps4-ftp]: https://github.com/Scene-Collective/ps4-ftp
