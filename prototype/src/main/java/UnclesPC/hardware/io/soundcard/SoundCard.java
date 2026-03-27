package UnclesPC.hardware.io.soundcard;

import UnclesPC.hardware.ram.Memory;

public final class SoundCard {
    private final Memory memory;
    private boolean enabled;
    private int frequencyHz;
    private int remainingMs;
    private int volume;

    public SoundCard(Memory memory) {
        this.memory = memory;
        this.enabled = false;
        this.frequencyHz = 440;
        this.remainingMs = 0;
        this.volume = 255;
    }

    //TODO: Execute MMIO.
}
