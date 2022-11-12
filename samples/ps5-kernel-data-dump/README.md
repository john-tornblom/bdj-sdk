# PS5 kernel data dump
This is a BD-J Xlet that dumps contents from the .data segment of a running PS5
kernel. The Xlet relies on a [privilege escalation vulnerability][h1] discovered
by [theflow][theflow], which was later [reproduced for the PS4][insp1] by
[sleirsgoevy][sleirsgoevy]. To escape the Java sandbox, the Xlet uses that
vulnerability to disable the security manager using a technique discovered
by [sleirsgoevy][insp2].

To execute native code, the Xlet uses reflection to access a NativeLibrary class
burried within java.lang.ClassLoader, where we may obtain and alter symbols the
JVM uses to invoke native code. By obtaining symbols to the native functions
getcontext and setcontext, we can invoke arbitrary functions loaded in memory.
Like the privilege escalation vulnerability mentioned above, the code
implementing the native code execution sample is derived from the works by
[sleirsgoevy][sleirsgoevy] and [theflow][theflow].

To enable kernel R/W, the Xlet exploits an [IPv6 use after free bug][uaf],
also discovered by [theflow][theflow], and later ported to the PS5 by
[sleirsgoevy][insp2] and [SpecterDev][specterdev].

Usage:
```console
john@localhost:~$ echo 0x1000000 | nc PS5IP 5656 > kernel.data
```
where 0x1000000 is the number of bytes to dump.

[h1]: https://hackerone.com/reports/1379975
[insp1]: https://github.com/sleirsgoevy/bd-jb
[insp2]: https://github.com/sleirsgoevy/bd-jb/tree/ps5
[sleirsgoevy]: https://github.com/sleirsgoevy
[theflow]: https://github.com/TheOfficialFloW
[uaf]: https://hackerone.com/reports/826026
[specterdev]: https://github.com/Cryptogenic/PS5-IPV6-Kernel-Exploit

