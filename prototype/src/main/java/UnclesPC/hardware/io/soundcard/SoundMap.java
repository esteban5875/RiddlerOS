package UnclesPC.hardware.io.soundcard;

public enum SoundMap {
    STATUS(0x007F3004),
    FREQUENCY(0x007F3008),
    DURATION(0x007F300C),
    VOLUME(0x007F3010),
    ERROR(0x007F3014);

    private final int value;

    SoundMap(int value) {
        this.value = value;
    }

    public int value() {
        return value;
    }
}
