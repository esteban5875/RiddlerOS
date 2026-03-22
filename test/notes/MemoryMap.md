### Physical Address Space (First 1 MB)

``` 

0x000000 ─────────────────────────
           Interrupt Vector Table (IVT) — 1 KB
0x000400 ─────────────────────────
           BIOS Data Area (BDA) — 256 B
0x000500 ─────────────────────────
           Free low memory
...
0x007C00 ─────────────────────────
           Stage 1 Bootloader (512 B)
0x007E00 ─────────────────────────
           Free low memory
...
0x09FC00 ─────────────────────────
           EBDA (Extended BIOS Data Area)
0x0A0000 ─────────────────────────
           Video Memory (VGA)
0x0C0000 ─────────────────────────
           BIOS ROM / Option ROMs
0x100000 ─────────────────────────
           Extended Memory (Above 1 MB)

```

- 0x000000 – 0x09FFFF = 640 KB <- All memory usable in real mode

- 0x100000 = 1 MB

- A20 disabled -> <0x100000 -> 0 (Cannot exceed 1 MB) [physical = requested_address % 1MB]

- Must enable A20 to access above 1 MB. After switching modes and enabling A20 -> 0x0010000 (1 MB) Kernel Address 

- Kernel lives in protected mode.

- Enabling A20 -> Allowing the 21st physical address line (A20) to carry its real value instead of being forced to 0. (physical_address = requested_address)