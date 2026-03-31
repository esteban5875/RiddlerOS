package UnclesPC.hardware.io.soundcard;

import UnclesPC.hardware.motherboard.error.ErrorCode;
import UnclesPC.hardware.ram.Memory;

public final class SoundCard {
    private static final int STATUS_FREE = 0;
    private static final int STATUS_OCCUPIED = 1;

    private final int statusLoc;
    private final int frequencyLoc;
    private final int durationLoc;
    private final int volumeLoc;
    private final int errorLoc;
    private final Memory memory;

    public SoundCard(Memory memory) {
        this.memory = memory;
        this.statusLoc = SoundMap.STATUS.value();
        this.frequencyLoc = SoundMap.FREQUENCY.value();
        this.durationLoc = SoundMap.DURATION.value();
        this.volumeLoc = SoundMap.VOLUME.value();
        this.errorLoc = SoundMap.ERROR.value();

        memory.write(statusLoc, STATUS_FREE);
        memory.write(frequencyLoc, 0);
        memory.write(durationLoc, 0);
        memory.write(volumeLoc, 0xFF);
        memory.write(errorLoc, ErrorCode.SUCCESS.code());
    }

    public int[] beep() {
        ErrorCode errorCode = ErrorCode.SUCCESS;

        if (memory.read(statusLoc) == STATUS_OCCUPIED) {
            memory.write(errorLoc, ErrorCode.SOUND_CARD_OCCUPIED.code());
            return null;
        }

        try {
            memory.write(statusLoc, STATUS_OCCUPIED);

            final int frequency = memory.read(frequencyLoc);
            final int durationMs = memory.read(durationLoc);
            final int volume = memory.read(volumeLoc);

            if (frequency < 20 || frequency > 20_000) {
                errorCode = ErrorCode.INVALID_SOUND_FREQUENCY;
                return null;
            }

            if (durationMs <= 0) {
                errorCode = ErrorCode.INVALID_SOUND_DURATION;
                return null;
            }

            if (volume < 0 || volume > 0xFF) {
                errorCode = ErrorCode.INVALID_SOUND_VOLUME;
                return null;
            }

            return new int[] { frequency, durationMs, volume };
        } finally {
            memory.write(errorLoc, errorCode.code());
            memory.write(statusLoc, STATUS_FREE);
        }
    }

    public void setFrequency(int frequency) {
        memory.write(frequencyLoc, frequency);
    }

    public void setDuration(int duration) {
        memory.write(durationLoc, duration);
    }

    public void setVolume(int volume) {
        memory.write(volumeLoc, volume);
    }
}
