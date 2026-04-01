package UnclesPC.hardware.io.keyboard;

import java.util.OptionalInt;
import java.util.concurrent.locks.ReentrantLock;

import UnclesPC.hardware.motherboard.error.ErrorCode;
import UnclesPC.hardware.ram.Memory;

public final class Keyboard {
    private static final int FIFO_STRIDE_BYTES = 4;
    private static final int STATUS_IDLE = 0;
    private static final int STATUS_OCCUPIED = 1;

    private final int statusLoc;
    private final Memory memory;
    private final ReentrantLock deviceLock;
    private final int fifoStartLoc;
    private final int fifoEndLoc;
    private final int fifoCapacity;

    private int writePtrLoc;
    private int readPtrLoc;
    private int queuedKeyCount;

    public Keyboard(Memory memory) {
        this.memory = memory;
        this.statusLoc = KeyboardMap.STATUS.value();
        this.deviceLock = new ReentrantLock();
        this.fifoStartLoc = KeyboardMap.FIFO_START.value();
        this.fifoEndLoc = KeyboardMap.FIFO_END.value();
        this.fifoCapacity = (fifoEndLoc - fifoStartLoc) / FIFO_STRIDE_BYTES;
        this.writePtrLoc = fifoStartLoc;
        this.readPtrLoc = fifoStartLoc;
        this.queuedKeyCount = 0;

        initializeDeviceState();
    }

    public ErrorCode pressKey(int keyCode) {
        deviceLock.lock();
        try {
            memory.writeWord(statusLoc, STATUS_OCCUPIED);

            if (queuedKeyCount == fifoCapacity) {
                return ErrorCode.KEYBOARD_BUFFER_FULL;
            }

            memory.writeWord(writePtrLoc, keyCode);
            writePtrLoc = advancePointer(writePtrLoc);
            queuedKeyCount++;

            memory.writeWord(KeyboardMap.CURRENT_KEY.value(), keyCode);
            return ErrorCode.SUCCESS;
        } finally {
            memory.writeWord(statusLoc, STATUS_IDLE);
            deviceLock.unlock();
        }
    }

    public OptionalInt readKey() {
        deviceLock.lock();
        try {
            memory.writeWord(statusLoc, STATUS_OCCUPIED);

            if (queuedKeyCount == 0) {
                return OptionalInt.empty();
            }

            int keyCode = memory.readWord(readPtrLoc);
            memory.writeWord(readPtrLoc, 0);
            readPtrLoc = advancePointer(readPtrLoc);
            queuedKeyCount--;
            return OptionalInt.of(keyCode);
        } finally {
            memory.writeWord(statusLoc, STATUS_IDLE);
            deviceLock.unlock();
        }
    }

    private void initializeDeviceState() {
        memory.writeWord(statusLoc, STATUS_IDLE);
        memory.writeWord(KeyboardMap.CURRENT_KEY.value(), 0);

        for (int slot = fifoStartLoc; slot < fifoEndLoc; slot += FIFO_STRIDE_BYTES) {
            memory.writeWord(slot, 0);
        }
    }

    private int advancePointer(int pointer) {
        int nextPointer = pointer + FIFO_STRIDE_BYTES;
        return nextPointer >= fifoEndLoc ? fifoStartLoc : nextPointer;
    }
}
