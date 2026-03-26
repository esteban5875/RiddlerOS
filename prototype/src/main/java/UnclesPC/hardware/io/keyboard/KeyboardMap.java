package UnclesPC.hardware.io.keyboard;

public enum KeyboardMap {
    STATUS(0x007F2004),
    FIFO_START(0x007F200C),
    FIFO_END(0x007F201C);

    private final int value;

    KeyboardMap(int value) {
        this.value = value;
    }

    public int value() {
        return value;
    }
}
