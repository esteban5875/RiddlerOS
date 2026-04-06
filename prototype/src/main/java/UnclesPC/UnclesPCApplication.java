package UnclesPC;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Map;

import UnclesPC.app_level.emu_manager.DiskImgManager;
import UnclesPC.toolset.assembler.Assembler;
import UnclesPC.virtual_hardware.io.exception.VirtualHardwareException;
import UnclesPC.virtual_hardware.motherboard.Motherboard;
import UnclesPC.virtual_hardware.ram.modules.MemoryMap;

public final class UnclesPCApplication {
    private static final Path FIRMWARE_DIR = Path.of(
        "src", "main", "java", "UnclesPC", "virtual_hardware", "motherboard", "firmware"
    );

    private static final Path TEST_DIR = Path.of(
        "src", "main", "java", "UnclesPC", "tests", "bin"
    );

    private static final Map<String, Integer> FIRMWARE_IMAGES = Map.ofEntries(
        Map.entry("bootstrap.bin", 0x00000600),
        Map.entry("vector_divide_by_zero.bin", 0x00000800),
        Map.entry("vector_debug.bin", 0x00000820),
        Map.entry("vector_overflow.bin", 0x00000840),
        Map.entry("vector_bound_range.bin", 0x00000860),
        Map.entry("vector_invalid_opcode.bin", 0x00000880),
        Map.entry("vector_device_not_available.bin", 0x000008A0),
        Map.entry("vector_general_protection.bin", 0x000008C0),
        Map.entry("vector_bios_tty.bin", 0x00000900),
        Map.entry("vector_bios_disk.bin", 0x00000980),
        Map.entry("vector_bios_mem.bin", 0x00000A00)
    );

    public static void main(String[] args) throws IOException, VirtualHardwareException {
        // compileFirmware();

        DiskImgManager diskImgManager = new DiskImgManager();

        if (diskImgManager.exists()) {
            diskImgManager.open();
        } else {
            diskImgManager.create(false);
        }

        Motherboard motherboard = new Motherboard(diskImgManager.requireOpen());

        try {
            loadFirmware();
            motherboard.loadProgram(readBinFile(TEST_DIR, "bootloader.bin"));;
            System.out.println("'Uncle's PC' Java prototype ready. \n");

            motherboard.debug_tick();
        } catch (VirtualHardwareException e) {
            System.err.println("\n" + "Virtual hardware exception: " + e.getMessage() + " (code " + e.errorCode().code() + ")");
        } finally {
            diskImgManager.close();
        }
    }

    //Func to bootstrap firmware. Should be run whenever firmware source code is changed. Commented out to avoid accidental execution.

    /* private static void compileFirmware() throws IOException {
        Assembler.toBin(FIRMWARE_DIR.resolve("bootstrap.rasm"), FIRMWARE_DIR.resolve("bootstrap.bin"));
        Assembler.toBin(
            FIRMWARE_DIR.resolve("vector_bios_disk.rasm"),
            FIRMWARE_DIR.resolve("vector_bios_disk.bin")
        );
        Assembler.toBin(
            FIRMWARE_DIR.resolve("vector_bios_mem.rasm"),
            FIRMWARE_DIR.resolve("vector_bios_mem.bin")
        );
        Assembler.toBin(
            FIRMWARE_DIR.resolve("vector_bios_tty.rasm"),
            FIRMWARE_DIR.resolve("vector_bios_tty.bin")
        );
        Assembler.toBin(
            FIRMWARE_DIR.resolve("vector_bound_range.rasm"),
            FIRMWARE_DIR.resolve("vector_bound_range.bin")
        );
        Assembler.toBin(
            FIRMWARE_DIR.resolve("vector_debug.rasm"),
            FIRMWARE_DIR.resolve("vector_debug.bin")
        );
        Assembler.toBin(
            FIRMWARE_DIR.resolve("vector_device_not_available.rasm"),
            FIRMWARE_DIR.resolve("vector_device_not_available.bin")
        );
        Assembler.toBin(
            FIRMWARE_DIR.resolve("vector_divide_by_zero.rasm"),
            FIRMWARE_DIR.resolve("vector_divide_by_zero.bin")
        );
        Assembler.toBin(
            FIRMWARE_DIR.resolve("vector_general_protection.rasm"),
            FIRMWARE_DIR.resolve("vector_general_protection.bin")
        );
        Assembler.toBin(
            FIRMWARE_DIR.resolve("vector_invalid_opcode.rasm"),
            FIRMWARE_DIR.resolve("vector_invalid_opcode.bin")
        );
        Assembler.toBin(
            FIRMWARE_DIR.resolve("vector_overflow.rasm"),
            FIRMWARE_DIR.resolve("vector_overflow.bin")
        );
    } */

    private static byte[] readBinFile(Path directory, String fileName) throws IOException {
        return Assembler.toBytes(directory.resolve(fileName));
    }

    private static byte[] loadFirmware() throws IOException {
        int firmwareStart = MemoryMap.FIRMWARE_START.value();
        int firmwareSize = MemoryMap.FIRMWARE_END.value() - firmwareStart + 1;
        byte[] firmware = new byte[firmwareSize];

        for (Map.Entry<String, Integer> image : FIRMWARE_IMAGES.entrySet()) {
            byte[] bytes = readBinFile(FIRMWARE_DIR, image.getKey());
            int offset = image.getValue() - firmwareStart;

            if (offset < 0 || offset + bytes.length > firmware.length) {
                throw new IllegalStateException("firmware binary out of range: " + image.getKey());
            }

            System.arraycopy(bytes, 0, firmware, offset, bytes.length);
        }

        return firmware;
    }

}
