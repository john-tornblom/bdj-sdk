# BD-J Linux SDK

## Building
On Debian-flavored operating systems, you can invoke the following commands to
install dependencies, and compile the source code.

```console
john@localhost:~$ sudo apt-get install build-essential libbsd-dev git pkg-config openjdk-8-jdk-headless
john@localhost:~$ git clone --recurse-submodules https://github.com/john-tornblom/bdj-sdk
john@localhost:~$ ln -s /usr/lib/jvm/java-8-openjdk-amd64 bdj-sdk/host/jdk
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

