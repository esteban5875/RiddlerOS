package UnclesPC.hardware.io.keyboard;

import UnclesPC.hardware.motherboard.error.ErrorCode;
import UnclesPC.hardware.ram.Memory;

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

    //TODO: Execute MMIO.
}
