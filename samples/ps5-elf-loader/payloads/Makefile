PS5_HOST ?= ps5
PS5_PORT ?= 9020

CC     := clang
CFLAGS := -target x86_64-none-elf -fno-builtin -nostdlib -fPIC -static

all: getpid.elf

getpid.elf: getpid.c
	$(CC) $(CFLAGS) -o $@ $< -Xlinker -T linker.x -Wl,--build-id=none

clean:
	rm -f getpid.elf

test: getpid.elf
	nc -q0 $(PS5_HOST) $(PS5_PORT) < $^

