package UnclesPC.virtual_hardware.motherboard.error;

public enum ErrorCode {
    SUCCESS(0x00),
    MEMORY_OUT_OF_BOUNDS(0x01),
    STACK_OVERFLOW(0x02),
    STACK_UNDERFLOW(0x03),
    INVALID_INSTRUCTION(0x04),
    DIVISION_BY_ZERO(0x05),
    CPU_MODE_ERROR(0x06),
    DISK_NOT_FOUND(0x07),
    DISK_IMAGE_ERROR(0x08),
    SECTOR_SIZE_ERROR(0x09),
    OCCUPIED_DISK_ERROR(0x0A),
    INVALID_DISK_COMMAND(0x0B),
    SECTOR_COUNT_ERROR(0x0C),
    DISK_TIMEOUT(0x0D),
    KEYBOARD_OCCUPIED(0x0E),
    KEYBOARD_BUFFER_FULL(0x0F),
    NO_KEY_AVAILABLE(0x10),
    SOUND_CARD_OCCUPIED(0x11),
    INVALID_SOUND_FREQUENCY(0x12),
    INVALID_SOUND_DURATION(0x13),
    INVALID_SOUND_VOLUME(0x14),
    DISK_NOT_OPEN(0x15);

    private final int code;

    ErrorCode(int code) {
        this.code = code & 0xFFFFFFFF; // Ensure code is treated as 32-bit unsigned
    }

    public int code() {
        return code;
    }
}
