package UnclesPC.virtual_hardware.io.soundcard;

import UnclesPC.app_level.exception.UnclesPCException;
import UnclesPC.toolset.types.SoundCardPayload;
import UnclesPC.virtual_hardware.io.exception.VirtualHardwareException;
import UnclesPC.virtual_hardware.motherboard.error.ErrorCode;
import UnclesPC.virtual_hardware.ram.Memory;

public final class SoundCard {
    private static final int COMMAND_NONE = 0;
    private static final int COMMAND_BEEP = 1;
    private static final int STATUS_FREE = 0;
    private static final int STATUS_OCCUPIED = 1;

    private final int commandLoc;
    private final int statusLoc;
    private final int frequencyLoc;
    private final int durationLoc;
    private final int volumeLoc;
    private final int errorLoc;
    private final Memory memory;

    public SoundCard(Memory memory) {
        this.memory = memory;
        this.commandLoc = SoundMap.COMMAND.value();
        this.statusLoc = SoundMap.STATUS.value();
        this.frequencyLoc = SoundMap.FREQUENCY.value();
        this.durationLoc = SoundMap.DURATION.value();
        this.volumeLoc = SoundMap.VOLUME.value();
        this.errorLoc = SoundMap.ERROR.value();

        writeDeviceWord(commandLoc, COMMAND_NONE);
        writeDeviceWord(statusLoc, STATUS_FREE);
        writeDeviceWord(frequencyLoc, 0);
        writeDeviceWord(durationLoc, 0);
        writeDeviceWord(volumeLoc, 0xFF);
        writeDeviceWord(errorLoc, ErrorCode.SUCCESS.code());
    }

    public SoundCardPayload beep() {
        ErrorCode errorCode = ErrorCode.SUCCESS;

        if (readDeviceWord(commandLoc) != COMMAND_BEEP) {
            return null;
        }

        if (readDeviceWord(statusLoc) == STATUS_OCCUPIED) {
            writeDeviceWord(errorLoc, ErrorCode.SOUND_CARD_OCCUPIED.code());
            return null;
        }

        try {
            writeDeviceWord(statusLoc, STATUS_OCCUPIED);

            final int frequency = readDeviceWord(frequencyLoc);
            final int durationMs = readDeviceWord(durationLoc);
            final int volume = readDeviceWord(volumeLoc);

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

            return new SoundCardPayload(frequency, durationMs, volume);
        } finally {
            writeDeviceWord(commandLoc, COMMAND_NONE);
            writeDeviceWord(errorLoc, errorCode.code());
            writeDeviceWord(statusLoc, STATUS_FREE);
        }
    }

    public void setCommand(int command) {
        writeDeviceWord(commandLoc, command);
    }

    public void setFrequency(int frequency) {
        writeDeviceWord(frequencyLoc, frequency);
    }

    public void setDuration(int duration) {
        writeDeviceWord(durationLoc, duration);
    }

    public void setVolume(int volume) {
        writeDeviceWord(volumeLoc, volume);
    }

    private int readDeviceWord(int address) {
        try {
            return memory.readWord(address);
        } catch (VirtualHardwareException e) {
            throw new UnclesPCException(
                ErrorCode.MEMORY_OUT_OF_BOUNDS,
                "sound card MMIO address is invalid: " + address,
                e
            );
        }
    }

    private void writeDeviceWord(int address, int value) {
        try {
            memory.writeWord(address, value);
        } catch (VirtualHardwareException e) {
            throw new UnclesPCException(
                ErrorCode.MEMORY_OUT_OF_BOUNDS,
                "sound card MMIO address is invalid: " + address,
                e
            );
        }
    }
}
