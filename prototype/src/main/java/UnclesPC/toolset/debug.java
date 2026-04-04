package UnclesPC.toolset;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import UnclesPC.app_level.exception.UnclesPCException;
import UnclesPC.virtual_hardware.io.disk.DiskMap;
import UnclesPC.virtual_hardware.io.exception.VirtualHardwareException;
import UnclesPC.virtual_hardware.io.keyboard.KeyboardMap;
import UnclesPC.virtual_hardware.io.soundcard.SoundMap;
import UnclesPC.virtual_hardware.io.vga.GraphicsMap;
import UnclesPC.virtual_hardware.motherboard.error.ErrorCode;
import UnclesPC.virtual_hardware.ram.Memory;

public final class debug {
    private static final int KEYBOARD_FIFO_STRIDE_BYTES = 4;

    private debug() {
    }

    public static Map<String, Object> snapshot(Memory memory) {
        Map<String, Object> snapshot = new LinkedHashMap<>();
        snapshot.put("disk", diskSnapshot(memory));
        snapshot.put("keyboard", keyboardSnapshot(memory));
        snapshot.put("vga", vgaSnapshot(memory));
        snapshot.put("sound_card", soundCardSnapshot(memory));
        return snapshot;
    }

    private static Map<String, Object> diskSnapshot(Memory memory) {
        Map<String, Object> disk = new LinkedHashMap<>();
        disk.put("command", readMmioWord(memory, DiskMap.COMMAND.address));
        disk.put("status", readMmioWord(memory, DiskMap.STATUS.address));
        disk.put("error", readMmioWord(memory, DiskMap.ERROR.address));
        disk.put("sector_count", readMmioWord(memory, DiskMap.SECTOR_COUNT.address));
        disk.put("lba", readMmioWord(memory, DiskMap.LBA.address));
        disk.put("buffer_ptr", readMmioWord(memory, DiskMap.BUFFER_PTR.address));
        return disk;
    }

    private static Map<String, Object> keyboardSnapshot(Memory memory) {
        Map<String, Object> keyboard = new LinkedHashMap<>();
        keyboard.put("status", readMmioWord(memory, KeyboardMap.STATUS.value()));
        keyboard.put("current_key", readMmioWord(memory, KeyboardMap.CURRENT_KEY.value()));

        List<Integer> fifo = new ArrayList<>();
        for (int address = KeyboardMap.FIFO_START.value();
            address < KeyboardMap.FIFO_END.value();
            address += KEYBOARD_FIFO_STRIDE_BYTES) {
            fifo.add(readMmioWord(memory, address));
        }
        keyboard.put("fifo", fifo);

        return keyboard;
    }

    private static Map<String, Object> vgaSnapshot(Memory memory) {
        Map<String, Object> vga = new LinkedHashMap<>();
        vga.put("command", readMmioWord(memory, GraphicsMap.COMMAND.value()));
        vga.put("status", readMmioWord(memory, GraphicsMap.STATUS.value()));
        vga.put("mode", readMmioWord(memory, GraphicsMap.MODE.value()));
        vga.put("x", readMmioWord(memory, GraphicsMap.X.value()));
        vga.put("y", readMmioWord(memory, GraphicsMap.Y.value()));
        vga.put("data", readMmioWord(memory, GraphicsMap.DATA.value()));
        vga.put("error", readMmioWord(memory, GraphicsMap.ERROR.value()));
        return vga;
    }

    private static Map<String, Object> soundCardSnapshot(Memory memory) {
        Map<String, Object> soundCard = new LinkedHashMap<>();
        soundCard.put("command", readMmioWord(memory, SoundMap.COMMAND.value()));
        soundCard.put("status", readMmioWord(memory, SoundMap.STATUS.value()));
        soundCard.put("frequency", readMmioWord(memory, SoundMap.FREQUENCY.value()));
        soundCard.put("duration", readMmioWord(memory, SoundMap.DURATION.value()));
        soundCard.put("volume", readMmioWord(memory, SoundMap.VOLUME.value()));
        soundCard.put("error", readMmioWord(memory, SoundMap.ERROR.value()));
        return soundCard;
    }

    private static int readMmioWord(Memory memory, int address) {
        try {
            return memory.readWord(address);
        } catch (VirtualHardwareException e) {
            throw new UnclesPCException(
                ErrorCode.MEMORY_OUT_OF_BOUNDS,
                "unable to read MMIO debug snapshot at address: " + address,
                e
            );
        }
    }
}
