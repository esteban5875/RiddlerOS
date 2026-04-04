package UnclesPC.virtual_hardware.io.vga;

import UnclesPC.app_level.exception.UnclesPCException;
import UnclesPC.toolset.types.VGAPayload;
import UnclesPC.virtual_hardware.io.exception.VirtualHardwareException;
import UnclesPC.virtual_hardware.motherboard.error.ErrorCode;
import UnclesPC.virtual_hardware.ram.Memory;

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
    private static final int COMMAND_NONE = 0x00;
    private static final int COMMAND_DRAW = 0x01;
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

        setCommand(COMMAND_NONE);
        writeDeviceWord(statusLoc, STATUS_IDLE);
        setMode(dataType.COLOR.value());
        setX(0);
        setY(0);
        setData(0);
        writeDeviceWord(errorLoc, ErrorCode.SUCCESS.code());
    }

    public VGAPayload executeCMD() {
        if (getCommand() == COMMAND_NONE) {
            return null;
        }

        while (getStatus() == STATUS_BUSY) {
            Thread.onSpinWait();
        }

        setStatus(STATUS_BUSY);

        try {
            if (getCommand() != COMMAND_DRAW) {
                return null;
            }

            return new VGAPayload(getMode(), getX(), getY(), getData());
        } finally {
            setCommand(COMMAND_NONE);
            setError(ErrorCode.SUCCESS);
            setStatus(STATUS_IDLE);
        }
    }

    public void setCommand(int command) {
        writeDeviceWord(commandLoc, command);
    }

    public void setMode(int mode) {
        writeDeviceWord(modeLoc, mode);
    }

    public void setX(int x) {
        writeDeviceWord(xLoc, x);
    }

    public void setY(int y) {
        writeDeviceWord(yLoc, y);
    }

    public void setData(int data) {
        writeDeviceWord(dataLoc, data);
    }

    private void setError(ErrorCode errorCode) {
        writeDeviceWord(errorLoc, errorCode.code());
    }

    private void setStatus(int status) {
        writeDeviceWord(statusLoc, status);
    }

    private int getStatus() {
        return readDeviceWord(statusLoc);
    }

    private int getCommand() {
        return readDeviceWord(commandLoc);
    }

    private int getMode() {
        return readDeviceWord(modeLoc);
    }

    private int getX() {
        return readDeviceWord(xLoc);
    }

    private int getY() {
        return readDeviceWord(yLoc);
    }

    private int getData() {
        return readDeviceWord(dataLoc);
    }

    private int readDeviceWord(int address) {
        try {
            return memory.readWord(address);
        } catch (VirtualHardwareException e) {
            throw new UnclesPCException(
                ErrorCode.MEMORY_OUT_OF_BOUNDS,
                "VGA MMIO address is invalid: " + address,
                e
            );
        }
    }

    private void writeDeviceWord(int address, int value) {
        try {
            memory.writeWord(address, value);
        } catch (VirtualHardwareException e) {
            throw new UnclesPCException(
                ErrorCode.MEMORY_OUT_OF_BOUNDS,
                "VGA MMIO address is invalid: " + address,
                e
            );
        }
    }
}
