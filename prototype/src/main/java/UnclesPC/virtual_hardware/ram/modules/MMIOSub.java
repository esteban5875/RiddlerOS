package UnclesPC.virtual_hardware.ram.modules;

public enum MMIOSub {
    VGA_START(0x00700000),
    VGA_END(0x00700FFF),
    DISK_START(0x00701000),
    DISK_END(0x00701FFF),
    KB_START(0x00702000),
    KB_END(0x00702FFF),
    SOUND_START(0x00703000),
    SOUND_END(0x00703FFF);

    private final int value;

    MMIOSub(int value) {
        this.value = value;
    }

    public int value() {
        return value;
    }
}
