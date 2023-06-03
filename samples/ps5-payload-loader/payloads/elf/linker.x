OUTPUT_FORMAT("elf64-x86-64")
OUTPUT_ARCH(i386:x86-64)
ENTRY(_start)

PHDRS {
	ph_text PT_LOAD FLAGS (0x1 | 0x4);
	ph_data PT_LOAD FLAGS (0x2 | 0x4);
}

SECTIONS {
	.text : { *(.text .text.*) } : ph_text
	.rodata : { *(.rodata .rodata.*) }
	. = ALIGN(0x4000);
	.data : { *(.data .data.*) } : ph_data
	.bss : {
	     __bss_start = .;
	     *(.bss .bss.*) *(COMMON)
	     __bss_end = .;
	}
}
