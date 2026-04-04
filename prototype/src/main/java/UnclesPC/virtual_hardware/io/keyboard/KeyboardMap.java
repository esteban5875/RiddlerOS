package UnclesPC.virtual_hardware.io.keyboard;

public enum KeyboardMap {
    STATUS(0x00702004),
    FIFO_START(0x0070200C),
    FIFO_END(0x0070201C),
    CURRENT_KEY(0x00702020);

    private final int value;

    KeyboardMap(int value) {
        this.value = value;
    }

    public int value() {
        return value;
    }
}
