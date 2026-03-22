**Real Mode** -> Simplistic 16-bit mode present on all x86 processors. They begin execution in this mode.

- Less than 1 MB of ram available for use.
- No virtual memory.
- Enables access to BIOS interrupts.

- **All of the 32-bit registers (EAX, ...) are still usable, by simply adding the "Operand Size Override Prefix" (0x66) to the beginning of any instruction. Your assembler is likely to do this for you, if you simply try to use a 32-bit register.**

**Segmentation**:
    - Memory divided into different variable-sized segments (code, data, stack, etc)
    - Segment -> A 16-bit value that gets shifted left by 4 bits; That gives you a base address aligned to 16 bytes.
    - The offset is just a 16-bit value added to that base.
    - Segment consists of:
        - Base address (Where is starts in physical memory)
        - Limit (Size)
        - Attributes (Exec, writable, etc.)
    - Logical Address -> Segment selector + Offset 
    - Base Address = selector x 16
    - Physical Address = Segment * 16 + Offset (Address Notation Segment:Offset)
    - In real mode segments are 64 KB Max
    - Protected mode:
        - sSegments can be up to 4 GB (or 1 MB with 16-bit granularity)
        - Descriptors stored in tables (GDT/LDT)
        - Full protection: privilege rings, access checks, limit enforcement
        - Required for any serious OS
    Segment Descriptor (8 bytes in GDT/LDT):
        - Bits 0–15, 48–63: Base (32 bits total, split across fields)
        - Bits 16–39, 56–63: Limit (20 bits, can be in bytes or 4 KB pages)
        - sType field (bits 40–43): Code/data, conforming, readable/writable, accessed bit
        - S flag: 1 = user segment (code/data), 0 = system (TSS, LDT, gates)
        - DPL (Descriptor Privilege Level): 0–3 (ring)
        - Present (P): 1 = segment is in memory
        - Granularity (G): 0 = limit in bytes, 1 = limit in 4 KB pages
        - Default operand size (D/B): 32-bit vs 16-bit
        - Available (AVL): Free bit for OS use
    Segment Selector (16 bits, loaded into CS/DS/ES/FS/GS/SS):
        - Bits 0–1: Requested Privilege Level (RPL)
        - Bit 2: TI (Table Indicator): 0 = GDT, 1 = LDT
        - Bits 3–15: Index into GDT or LDT (multiplied by 8 to get byte offset)

- Six 16-bit segment registers: CS, DS, ES, FS, GS, SS
