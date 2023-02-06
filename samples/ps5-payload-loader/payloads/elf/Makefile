PS5_HOST ?= ps5
PS5_PORT ?= 9020

CC     ?= cc
CFLAGS := -ffreestanding -fno-builtin -nostdlib -static

all: getpid.elf mntinfo.elf remount.elf reloc.elf

getpid.elf: getpid.c
	$(CC) $(CFLAGS) -o $@ $< -Xlinker -T linker.x

mntinfo.elf: mntinfo.c
	$(CC) $(CFLAGS) -o $@ $< -Xlinker -T linker.x

remount.elf: remount.c
	$(CC) $(CFLAGS) -o $@ $< -Xlinker -T linker.x

reloc.elf: reloc.c
	$(CC) -ffreestanding -fno-builtin -nostdlib -fPIC -fpie -o $@ $< -Xlinker -T linker.x

clean:
	rm -f getpid.elf mntinfo.elf remount.elf reloc.elf

getpid_test: getpid.elf
	nc -q0 $(PS5_HOST) $(PS5_PORT) < $^

mntinfo_test: mntinfo.elf
	nc -q0 $(PS5_HOST) $(PS5_PORT) < $^

remount_test: remount.elf
	nc -q0 $(PS5_HOST) $(PS5_PORT) < $^

reloc_test: reloc.elf
	nc -q0 $(PS5_HOST) $(PS5_PORT) < $^

