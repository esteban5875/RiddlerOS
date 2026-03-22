package myibmgame.hardware.ram.modules;

public enum MMIOSub {
    VGA_START(0x007F0000),
    VGA_END(0x007F0FFF),
    DISK_START(0x007F1000),
    DISK_END(0x007F1FFF),
    KB_START(0x007F2000),
    KB_END(0x007F2FFF),
    SOUND_START(0x007F3000),
    SOUND_END(0x007F3FFF);

    private final int value;

    MMIOSub(int value) {
        this.value = value;
    }

    public int value() {
        return value;
    }
}
