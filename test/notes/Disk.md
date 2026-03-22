Sector -> A sector is the smallest addressable storage unit on a disk.

LBA Addressing: 
    LBA 0 → first sector
    LBA 1 → second sector
    LBA 2 → third sector

CHS:
    Sector numbers start at 1, NOT 0
When BIOS boots:
    Reads LBA 0
    Loads it to 0x0000:0x7C00
    Checks bytes 510–511 for signature 0x55AA

If kernel bigger than 512 bytes: sectors_needed = ceil(kernel_size / 512)
Bootloader starts reading at -> LBA 1 | CHS 2

Real Mode Disk Read:
    AH = 02h  → CHS read
    AH = 42h  → Extended LBA read (modern)
    Destination -> ES:BX

