package myibmgame.hardware.io.keyboard;

import myibmgame.hardware.motherboard.error.ErrorCode;
import myibmgame.hardware.ram.Memory;

public final class Keyboard {
    private final int status;
    private int writePtr;
    private int readPtr;
    private final Memory memory;

    public Keyboard(Memory memory) {
        this.memory = memory;
        this.status = KeyboardMap.STATUS.value();
        this.writePtr = KeyboardMap.FIFO_START.value();
        this.readPtr = KeyboardMap.FIFO_START.value();
    }

    public void insertCurrent(int scanCode) {
        memory.write(writePtr, scanCode);

        writePtr += 4;
        if (writePtr >= KeyboardMap.FIFO_END.value()) {
            writePtr = KeyboardMap.FIFO_START.value();
        }

        memory.write(status, 1);
    }

    public int readCurrent() {
        if (readPtr == writePtr) {
            memory.write(status, 0);
            return ErrorCode.SUCCESS.code();
        }

        int current = memory.read(readPtr);

        readPtr += 4;
        if (readPtr >= KeyboardMap.FIFO_END.value()) {
            readPtr = KeyboardMap.FIFO_START.value();
        }

        if (readPtr == writePtr) {
            memory.write(status, 0);
        }

        return current;
    }

    public int getStatus() {
        return memory.read(status);
    }
}
