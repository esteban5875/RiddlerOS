package myibmgame.hardware.ram.modules;

public enum VectorTable {
    DIVIDE_BY_ZERO(0x00),
    DEBUG(0x01),
    OVERFLOW(0x04),
    BOUND_RANGE(0x05),
    INVALID_OPCODE(0x06),
    DEVICE_NOT_AVAILABLE(0x07),
    GENERAL_PROTECTION(0x0D),
    BIOS_TTY(0x10),
    BIOS_DISK(0x13),
    BIOS_MEM(0x15),
    BIOS_MODE(0x18);

    private final int value;

    VectorTable(int value) {
        this.value = value;
    }

    public int value() {
        return value;
    }
}
