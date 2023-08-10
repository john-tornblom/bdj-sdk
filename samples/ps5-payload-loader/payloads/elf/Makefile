PS5_HOST ?= ps5
PS5_PORT ?= 9020

CC      ?= cc
LD      ?= ld
CFLAGS  := -ffreestanding -fno-builtin -nostdlib -fPIC
LDFLAGS := -pie -T linker.x

all: getpid.elf mntinfo.elf remount.elf

%.o: %.c
	$(CC) -c $(CFLAGS) -o $@ $^

%.elf: %.o
	$(LD) $(LDFLAGS) -o $@ $<

clean:
	rm -f *.o *.elf

%_test: %.elf
	nc -q0 $(PS5_HOST) $(PS5_PORT) < $^

