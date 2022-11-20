# PS5 JAR Loader
This is a BD-J Xlet that launches JAR files transmitted via TCP to a PS5.
The Xlet relies on a [privilege escalation vulnerability][h1] discovered
by [theflow][theflow], which was later [reproduced for the PS4][insp1] by
[sleirsgoevy][sleirsgoevy]. To escape the Java sandbox, the Xlet uses that
vulnerability to disable the security manager using a technique discovered
by [sleirsgoevy][insp2]. The JAR loading mechanism is heavily insired by
the works of [Hammer 83][h83].

Usage example:
```console
john@localhost:~/bdj-sdk/samples/ps5-jar-loader$ make -C hello-jar-loader
john@localhost:~/bdj-sdk/samples/ps5-jar-loader$ export PS5_HOST=<ps5-host>
john@localhost:~/bdj-sdk/samples/ps5-jar-loader$ make -C hello-jar-loader test
```

[h1]: https://hackerone.com/reports/1379975
[theflow]: https://github.com/TheOfficialFloW
[insp1]: https://github.com/sleirsgoevy/bd-jb
[sleirsgoevy]: https://github.com/sleirsgoevy
[insp2]: https://github.com/sleirsgoevy/bd-jb/tree/ps5
[h83]: https://github.com/hammer-83/ps5-jar-loader
