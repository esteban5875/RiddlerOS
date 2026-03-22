package myibmgame.hardware.motherboard.error;

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
    DISK_ERROR(0x0A);

    private final int code;

    ErrorCode(int code) {
        this.code = code;
    }

    public int code() {
        return code;
    }
}
