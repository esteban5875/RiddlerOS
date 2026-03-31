package UnclesPC.hardware.io.vga;

public enum GraphicsMap {
    COMMAND(0x007F0000),
    STATUS(0x007F0004),
    MODE(0x007F0008),
    X(0x007F000C),
    Y(0x007F0010),
    DATA(0x007F0014),
    ERROR(0x007F0018);

    private final int value;

    GraphicsMap(int value) {
        this.value = value;
    }

    public int value() {
        return value;
    }
}
