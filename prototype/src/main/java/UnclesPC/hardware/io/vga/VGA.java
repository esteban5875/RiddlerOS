package UnclesPC.hardware.io.vga;

import UnclesPC.hardware.motherboard.error.ErrorCode;
import UnclesPC.hardware.ram.Memory;

enum dataType {
    COLOR(0x00),
    CHARACTER(0x01);

    private final int value;

    dataType(int value) {
        this.value = value;
    }

    public int value() {
        return value;
    }
}

public class VGA {
    private static final int STATUS_IDLE = 0x00;
    private static final int STATUS_BUSY = 0x01;

    private final int commandLoc;
    private final int statusLoc;
    private final int modeLoc;
    private final int xLoc;
    private final int yLoc;
    private final int dataLoc;
    private final int errorLoc;
    private final Memory memory;

    public VGA(Memory memory) {
        this.memory = memory;
        this.commandLoc = GraphicsMap.COMMAND.value();
        this.statusLoc = GraphicsMap.STATUS.value();
        this.modeLoc = GraphicsMap.MODE.value();
        this.xLoc = GraphicsMap.X.value();
        this.yLoc = GraphicsMap.Y.value();
        this.dataLoc = GraphicsMap.DATA.value();
        this.errorLoc = GraphicsMap.ERROR.value();

        memory.writeWord(statusLoc, STATUS_IDLE);
        memory.writeWord(errorLoc, ErrorCode.SUCCESS.code());
    }

    public void executeCMD() {
        //TODO: Implement command execution logic.
    }

    private void setError(ErrorCode errorCode) {
        memory.writeWord(errorLoc, errorCode.code());
    }

    private void setStatus(int status) {
        memory.writeWord(statusLoc, status);
    }

    private int getStatus() {
        return memory.readWord(statusLoc);
    }

    private int getMode() {
        return memory.readWord(modeLoc);
    }

    private int getX() {
        return memory.readWord(xLoc);
    }

    private int getY() {
        return memory.readWord(yLoc);
    }

    private void writeData(int data) {
        memory.writeWord(dataLoc, data);
    }
}
