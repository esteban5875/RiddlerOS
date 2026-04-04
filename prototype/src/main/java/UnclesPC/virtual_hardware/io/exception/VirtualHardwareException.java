package UnclesPC.virtual_hardware.io.exception;

import UnclesPC.virtual_hardware.motherboard.error.ErrorCode;

public final class VirtualHardwareException extends Exception {
    private final ErrorCode errorCode;

    public VirtualHardwareException(ErrorCode errorCode) {
        super();
        this.errorCode = errorCode;
    }

    public VirtualHardwareException(ErrorCode errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
    }

    public ErrorCode errorCode() {
        return errorCode;
    }

    public int getErrorCode() {
        return errorCode.code();
    }
}
