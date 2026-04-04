package UnclesPC.virtual_hardware.io.vga;

public enum GraphicsMap {
    COMMAND(0x00700000),
    STATUS(0x00700004),
    MODE(0x00700008),
    X(0x0070000C),
    Y(0x00700010),
    DATA(0x00700014),
    ERROR(0x00700018);

    private final int value;

    GraphicsMap(int value) {
        this.value = value;
    }

    public int value() {
        return value;
    }
}
