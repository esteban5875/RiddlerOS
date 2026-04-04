package UnclesPC.virtual_hardware.io.soundcard;

public enum SoundMap {
    COMMAND(0x00703000),
    STATUS(0x00703004),
    FREQUENCY(0x00703008),
    DURATION(0x0070300C),
    VOLUME(0x00703010),
    ERROR(0x00703014);

    private final int value;

    SoundMap(int value) {
        this.value = value;
    }

    public int value() {
        return value;
    }
}
