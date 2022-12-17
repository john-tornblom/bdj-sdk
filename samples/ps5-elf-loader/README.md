# PS5 ELF Loader
This is a BD-J Xlet that starts a socket server on port 9020, that reads and
executes ELF files transerved over TCP. The Xlet relies on a
[privilege escalation vulnerability][h1] discovered by [theflow][theflow],
which was later [reproduced for the PS4][insp1] by [sleirsgoevy][sleirsgoevy].
To escape the Java sandbox, the Xlet uses that vulnerability to disable the
security manager using a technique discovered by [sleirsgoevy][insp2].
Once we are out of the sandbox, we can start a socket server that implements
an ELF loader. To launch ELF files remotely, you may use netcat:
```console
john@localhost:~/bdj-sdk/samples/ps5-elf-loader$ make -C payloads
john@localhost:~/bdj-sdk/samples/ps5-elf-loader$ nc -q0 ps5 9020 < payloads/getpid.elf
```

[h1]: https://hackerone.com/reports/1379975
[insp1]: https://github.com/sleirsgoevy/bd-jb
[insp2]: https://github.com/sleirsgoevy/bd-jb/tree/ps5
[sleirsgoevy]: https://github.com/sleirsgoevy
[theflow]: https://github.com/TheOfficialFloW
