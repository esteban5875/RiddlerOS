package myibmgame.hardware.io.soundcard;

import myibmgame.hardware.ram.Memory;

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

    public void beep(int frequencyHz, int durationMs, int volume) {
        enabled = true;
        this.frequencyHz = Math.max(1, frequencyHz);
        this.remainingMs = Math.max(0, durationMs);
        this.volume = Math.max(0, Math.min(255, volume));
        memory.write(SoundMap.FREQUENCY.value(), this.frequencyHz);
        memory.write(SoundMap.DURATION.value(), this.remainingMs);
        memory.write(SoundMap.VOLUME.value(), this.volume);
        memory.write(SoundMap.STATUS.value(), 1);
    }

    public void beep(int frequencyHz, int durationMs) {
        beep(frequencyHz, durationMs, 255);
    }

    public void stop() {
        enabled = false;
        remainingMs = 0;
        memory.write(SoundMap.STATUS.value(), 0);
    }

    public void tick(int deltaMs) {
        if (!enabled) {
            return;
        }

        remainingMs -= deltaMs;
        memory.write(SoundMap.DURATION.value(), Math.max(remainingMs, 0));
        if (remainingMs <= 0) {
            stop();
        }
    }

    public int isBusy() {
        return memory.read(SoundMap.STATUS.value());
    }
}
