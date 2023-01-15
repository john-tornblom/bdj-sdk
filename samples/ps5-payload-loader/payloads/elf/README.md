To list mount points:
```console
$ make mntinfo.elf
$ nc -q0 ps5 9020 < mntinfo.elf
```

To remount /system with write permissions:
```console
$ make remount.elf
$ nc -q0 ps5 9020 < remount.elf
```
