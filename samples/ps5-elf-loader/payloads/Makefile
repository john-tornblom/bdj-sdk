PS5_HOST ?= ps5
PS5_PORT ?= 9020

CC     ?= cc
CFLAGS := -ffreestanding -fno-builtin -nostdlib -static

all: getpid.elf mntinfo.elf

getpid.elf: getpid.c
	$(CC) $(CFLAGS) -o $@ $< -Xlinker -T linker.x

mntinfo.elf: mntinfo.c
	$(CC) $(CFLAGS) -o $@ $< -Xlinker -T linker.x

clean:
	rm -f getpid.elf mntinfo.elf

getpid_test: getpid.elf
	nc -q0 $(PS5_HOST) $(PS5_PORT) < $^

mntinfo_test: mntinfo.elf
	nc -q0 $(PS5_HOST) $(PS5_PORT) < $^
