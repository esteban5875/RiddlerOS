package UnclesPC.virtual_hardware.motherboard;

import java.io.RandomAccessFile;
import java.util.Objects;

import UnclesPC.app_level.exception.UnclesPCException;
import UnclesPC.toolset.debug;
import UnclesPC.toolset.types.SoundCardPayload;
import UnclesPC.toolset.types.VGAPayload;
import UnclesPC.virtual_hardware.cpu.CPU;
import UnclesPC.virtual_hardware.io.disk.Disk;
import UnclesPC.virtual_hardware.io.exception.VirtualHardwareException;
import UnclesPC.virtual_hardware.io.keyboard.Keyboard;
import UnclesPC.virtual_hardware.io.soundcard.SoundCard;
import UnclesPC.virtual_hardware.io.vga.VGA;
import UnclesPC.virtual_hardware.motherboard.error.ErrorCode;
import UnclesPC.virtual_hardware.ram.Memory;
import UnclesPC.virtual_hardware.ram.modules.MemoryMap;

public final class Motherboard {
    private final Memory memory;
    private final CPU cpu;
    private final Disk disk;
    private final Keyboard keyboard;
    private final VGA vga;
    private final SoundCard soundCard;

    public Motherboard(RandomAccessFile diskImage) {
        this(new Memory(), diskImage);
    }

    public Motherboard(Memory memory, RandomAccessFile diskImage) {
        this.memory = Objects.requireNonNull(memory, "memory");
        this.cpu = new CPU(this.memory);
        this.disk = new Disk(this.memory, Objects.requireNonNull(diskImage, "diskImage"));
        this.keyboard = new Keyboard(this.memory);
        this.vga = new VGA(this.memory);
        this.soundCard = new SoundCard(this.memory);
    }

    public Memory memory() {
        return memory;
    }

    public CPU cpu() {
        return cpu;
    }

    public Disk disk() {
        return disk;
    }

    public Keyboard keyboard() {
        return keyboard;
    }

    public VGA vga() {
        return vga;
    }

    public SoundCard soundCard() {
        return soundCard;
    }

    private void clearRegion(int start, int end) {
        for (int addr = start; addr <= end; addr++) {
            try {
                memory.writeByte(addr, 0);
            } catch (VirtualHardwareException e) {
                throw new UnclesPCException(ErrorCode.MEMORY_OUT_OF_BOUNDS, "unable to clear memory region", e);
            }
        }
    }

    public void reset(byte[] firmware, byte[] bootloader) {
        cpu.reset();
        clearRegion(MemoryMap.BOOTLOADER_START.value(), MemoryMap.BOOTLOADER_END.value());
        clearRegion(MemoryMap.FIRMWARE_START.value(), MemoryMap.FIRMWARE_END.value());
        loadImage(firmware, MemoryMap.FIRMWARE_START.value(), MemoryMap.FIRMWARE_END.value(), "firmware");
        loadImage(bootloader, MemoryMap.BOOTLOADER_START.value(), MemoryMap.BOOTLOADER_END.value(), "bootloader");
    }

    public void loadProgram(byte[] program) {
        clearRegion(MemoryMap.PROGRAM_START.value(), MemoryMap.PROGRAM_END.value());
        loadImage(program, MemoryMap.PROGRAM_START.value(), MemoryMap.PROGRAM_END.value(), "program");
    }

    private void loadImage(byte[] image, int start, int end, String name) {
        int regionSize = end - start + 1;
        
        if (image.length > regionSize) {
            throw new UnclesPCException(ErrorCode.MEMORY_OUT_OF_BOUNDS, name + " image does not fit");
        }

        for (int i = 0; i < image.length; i++) {
            try {
                memory.writeByte(start + i, image[i] & 0xFF);
            } catch (VirtualHardwareException e) {
                throw new UnclesPCException(ErrorCode.MEMORY_OUT_OF_BOUNDS, "unable to load " + name, e);
            }
        }
    }

    public TickResult tick() throws VirtualHardwareException {
        cpu.step();
        disk.executeCMD();
        return new TickResult(vga.executeCMD(), soundCard.beep());
    }

    public TickResult debug_tick() throws VirtualHardwareException {
        TickResult result = tick();

        while (!cpu.isHalted()) {
            result = tick();
            debugSnapshot();
        }

        return result;
    }

    public void debugSnapshot() {
        System.out.println("CPU snapshot: " + cpu.snapshot() + "\n");
        System.out.println("MMIO snapshot: " + debug.snapshot(memory) + "\n");
    }

    public void run() throws VirtualHardwareException {
        while (!cpu.isHalted()) {
            tick();
        }
    }

    public ErrorCode pressKey(int keyCode) {
        return keyboard.pressKey(keyCode);
    }

    public record TickResult(
        VGAPayload vgaPayload,
        SoundCardPayload soundCardPayload
    ) {
    }

    private void loadFirmware(byte[] firmware) {
        int firmwareStart = MemoryMap.FIRMWARE_START.value();
        int firmwareSize = MemoryMap.FIRMWARE_END.value() - firmwareStart + 1;

        if (firmware.length > firmwareSize) {
            throw new UnclesPCException(
                ErrorCode.MEMORY_OUT_OF_BOUNDS,
                "firmware image does not fit in firmware memory"
            );
        }

        for (int offset = 0; offset < firmware.length; offset++) {
            try {
                memory.writeByte(firmwareStart + offset, firmware[offset] & 0xFF);
            } catch (VirtualHardwareException e) {
                throw new UnclesPCException(
                    ErrorCode.MEMORY_OUT_OF_BOUNDS,
                    "unable to load firmware image",
                    e
                );
            }
        }
    }
}
