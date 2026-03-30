package UnclesPC.hardware.io.keyboard;

import UnclesPC.hardware.motherboard.error.ErrorCode;
import UnclesPC.hardware.ram.Memory;

public final class Keyboard {
    private final int statusLoc;
    private int writePtrLoc;
    private int readPtrLoc;
    private final Memory memory;

    public Keyboard(Memory memory) {
        this.memory = memory;
        this.statusLoc = KeyboardMap.STATUS.value();
        this.writePtrLoc = KeyboardMap.FIFO_START.value();
        this.readPtrLoc = KeyboardMap.FIFO_START.value();
    }

    public void pressKey(int keyCode) {
        int nextWritePtr = writePtrLoc + 4;
        
        if (nextWritePtr > KeyboardMap.FIFO_END.value()) {
            nextWritePtr = KeyboardMap.FIFO_START.value();
        }
        
        memory.write(writePtrLoc, keyCode);
        writePtrLoc = nextWritePtr;
        memory.write(statusLoc, 0); // Buffer not full
    }
}
