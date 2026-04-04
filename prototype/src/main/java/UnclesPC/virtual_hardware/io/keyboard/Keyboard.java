package UnclesPC.virtual_hardware.io.keyboard;

import java.util.concurrent.locks.ReentrantLock;

import UnclesPC.app_level.exception.UnclesPCException;
import UnclesPC.virtual_hardware.io.exception.VirtualHardwareException;
import UnclesPC.virtual_hardware.motherboard.error.ErrorCode;
import UnclesPC.virtual_hardware.ram.Memory;

public final class Keyboard {
    private static final int FIFO_STRIDE_BYTES = 4;
    private static final int STATUS_IDLE = 0;
    private static final int STATUS_OCCUPIED = 1;

    private final int statusLoc;
    private final Memory memory;
    private final ReentrantLock deviceLock;
    private final int fifoStartLoc;
    private final int fifoEndLoc;

    private int writePtrLoc;

    public Keyboard(Memory memory) {
        this.memory = memory;
        this.statusLoc = KeyboardMap.STATUS.value();
        this.deviceLock = new ReentrantLock();
        this.fifoStartLoc = KeyboardMap.FIFO_START.value();
        this.fifoEndLoc = KeyboardMap.FIFO_END.value();
        this.writePtrLoc = fifoStartLoc;

        initializeDeviceState();
    }

    public ErrorCode pressKey(int keyCode) {
        deviceLock.lock();
        try {
            writeDeviceWord(statusLoc, STATUS_OCCUPIED);

            writeDeviceWord(writePtrLoc, keyCode);
            writePtrLoc = advancePointer(writePtrLoc);

            writeDeviceWord(KeyboardMap.CURRENT_KEY.value(), keyCode);
            return ErrorCode.SUCCESS;
        } finally {
            writeDeviceWord(statusLoc, STATUS_IDLE);
            deviceLock.unlock();
        }
    }

    private void initializeDeviceState() {
        writeDeviceWord(statusLoc, STATUS_IDLE);
        writeDeviceWord(KeyboardMap.CURRENT_KEY.value(), 0);

        for (int slot = fifoStartLoc; slot < fifoEndLoc; slot += FIFO_STRIDE_BYTES) {
            writeDeviceWord(slot, 0);
        }
    }

    private int advancePointer(int pointer) {
        int nextPointer = pointer + FIFO_STRIDE_BYTES;
        return nextPointer >= fifoEndLoc ? fifoStartLoc : nextPointer;
    }

    private void writeDeviceWord(int address, int value) {
        try {
            memory.writeWord(address, value);
        } catch (VirtualHardwareException e) {
            throw new UnclesPCException(
                ErrorCode.MEMORY_OUT_OF_BOUNDS,
                "keyboard MMIO address is invalid: " + address,
                e
            );
        }
    }
}
