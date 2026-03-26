package UnclesPC.hardware.ram.modules;

public enum MemoryMap {
    VECTOR_TABLE_START(0x00000000),
    VECTOR_TABLE_END(0x000003FF),
    BOOTLOADER_START(0x00000400),
    BOOTLOADER_END(0x000005FF),
    FIRMWARE_START(0x00000600),
    FIRMWARE_END(0x00000FFF),
    KERNEL_START(0x00001000),
    KERNEL_END(0x001FFFFF),
    KERNEL_HEAP_START(0x00200000),
    KERNEL_HEAP_END(0x002FFFFF),
    USER_START(0x00300000),
    USER_END(0x006FFFFF),
    MMIO_START(0x00700000),
    MMIO_END(0x007EFFFF),
    STACK_TOP(0x007FFFFF),
    STACK_BOTTOM(0x007F0000);

    private final int value;

    MemoryMap(int value) {
        this.value = value;
    }

    public int value() {
        return value;
    }
}
