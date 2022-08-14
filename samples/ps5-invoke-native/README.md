# PS5 native execution
This is a proof of concept BD-J Xlet that executes native code in libkernel.
The Xlet relies on a [privilege escalation vulnerability][h1] discovered by
[theflow][theflow], which was later [reproduced for the PS4][insp1] by
[sleirsgoevy][sleirsgoevy]. To escape the Java sandbox, the Xlet uses that
vulnerability to disable the security manager using a technique discovered
by [sleirsgoevy][insp2].

To execute native code, the Xlet uses reflection to access a NativeLibrary class
burried within java.lang.ClassLoader, where we may obtain and alter symbols the
JVM uses to invoke native code. By obtaining symbols to the native functions
getcontext and setcontext, we can invoke arbritary functions loaded in memory.
Like the privilege escalation vulnerability mentioned above, the code
implementing the native code execution sample is derived from the works by
[sleirsgoevy][sleirsgoevy] and [theflow][theflow].

[h1]: https://hackerone.com/reports/1379975
[insp1]: https://github.com/sleirsgoevy/bd-jb
[insp2]: https://github.com/sleirsgoevy/bd-jb/tree/ps5
[sleirsgoevy]: https://github.com/sleirsgoevy
[theflow]: https://github.com/TheOfficialFloW
