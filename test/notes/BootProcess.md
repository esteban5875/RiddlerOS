Power On Self Test -> When a computer is switched on or reset, it runs through this series of diagnostics.

BIOS looks for a boot signature in bootable devices (magic number), boot signature is in boot sector (sector number 0), contains byte sequence ending in 0x55 and 0xAA
offsets 510 and 511 respectively. 

Disk Sector -> Lowest avbailable storage unit (512 bytes).

Boot sector is then loaded into memory at 0x0000:0x7c00 (segment 0, address 0x7c00)

In the early execution env the CPU is in **Real Mode**.

Bootloader loads kernel and passes control to it.

When booting you only have 446 bytes available for boot record (Code executed on boot), however that is not much for the stuff that, before running the kernel, the bootloader must do:

- determine which partition to boot from (either by looking for the active partition, or by presenting the user with a selection of installed operating systems to chose from);
- determine where your kernel image is located on the boot partition (either by interpreting the file system, or by loading the image from a fixed position);
- load the kernel image into memory (requires basic disk I/O);
- enable protected mode;
- preparing the runtime environment for the kernel (e.g. setting up stack space);

Ways to approach this problem:

- Geek loading: Squeeze everything from the above list into the boot record. This is next to impossible, and does not leave room for any special-case handling or useful error messages.
- One-stage loading: Write a stub program for making the switch, and link that in front of your kernel image. Boot record loads kernel image (below the 1mb memory mark, because in real mode that's the upper memory limit!), jumps into the stub, stub makes the switch to Protected Mode and runtime preparations, jumps into kernel proper.
- Two-stage loading: Write a separate stub program which is loaded below the 1mb memory mark, and does everything from the above list.

