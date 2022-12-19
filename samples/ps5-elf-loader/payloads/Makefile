PS5_HOST ?= ps5
PS5_PORT ?= 9020

CC     ?= cc
CFLAGS := -ffreestanding -fno-builtin -nostdlib -static

all: getpid.elf

getpid.elf: getpid.c
	$(CC) $(CFLAGS) -o $@ $< -Xlinker -T linker.x

clean:
	rm -f getpid.elf

test: getpid.elf
	nc -q0 $(PS5_HOST) $(PS5_PORT) < $^

