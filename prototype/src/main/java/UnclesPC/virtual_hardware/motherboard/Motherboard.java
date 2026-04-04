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

    public void reset() {
        reset(new byte[0]);
    }

    public void reset(byte[] firmware) {
        Objects.requireNonNull(firmware, "firmware");

        cpu.reset();
        loadFirmware(firmware);
    }

    public TickResult tick() throws VirtualHardwareException {
        cpu.step();
        disk.executeCMD();
        return new TickResult(vga.executeCMD(), soundCard.beep());
    }

    public TickResult debug_tick() throws VirtualHardwareException {
        TickResult result = tick();

        while (!cpu.isHalted() && isFirmwarePc(cpu.pc())) {
            result = tick();
        }

        System.out.println("CPU snapshot: " + cpu.snapshot());
        System.out.println("MMIO snapshot: " + debug.snapshot(memory));
        System.out.println("Tick result: " + result);

        return result;
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

    private boolean isFirmwarePc(int address) {
        return address >= MemoryMap.FIRMWARE_START.value()
            && address <= MemoryMap.FIRMWARE_END.value();
    }
}
