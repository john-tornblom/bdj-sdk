# BD-J Linux SDK
This is a set of tools to simplify building BD-J ISO images on GNU/Linux systems.
It is an adaptation of the Win32 [minimal BD-J toolkit for the PS3][ps3],
with an updated authoring tool from [the HD Cookbook][hdc]. For creating ISO
images, we use a [Linux port][makefs_termux] of [NetBSD makefs][makefs] ported
by [Andrew Randrianasulu][Randrianasulu].

## Building
On Debian-flavored operating systems, you can invoke the following commands to
install dependencies, and compile the source code.

```console
john@localhost:~$ sudo apt-get install build-essential libbsd-dev git pkg-config openjdk-8-jdk-headless openjdk-11-jdk-headless
john@localhost:~$ git clone --recurse-submodules https://github.com/john-tornblom/bdj-sdk
john@localhost:~$ ln -s /usr/lib/jvm/java-8-openjdk-amd64 bdj-sdk/host/jdk8
john@localhost:~$ ln -s /usr/lib/jvm/java-11-openjdk-amd64 bdj-sdk/host/jdk11
john@localhost:~$ make -C bdj-sdk/host/src/makefs_termux
john@localhost:~$ make -C bdj-sdk/host/src/makefs_termux install DESTDIR=$PWD/bdj-sdk/host
john@localhost:~$ make -C bdj-sdk/target
```

## Usage example
```console
john@localhost:~$ make -C bdj-sdk/samples/helloworld
```
If everything was built successfully, you will find an BD-RE iso file
`bdj-sdk/samples/helloworld/helloworld.iso`

[ps3]: https://ps3.brewology.com/downloads/download.php?id=2171&mcid=4
[hdc]: http://oliverlietz.github.io/bd-j/hdcookbook.html
[makefs_termux]: https://github.com/Randrianasulu/makefs_termux
[makefs]: https://man.netbsd.org/makefs.8
[Randrianasulu]: https://github.com/Randrianasulu
