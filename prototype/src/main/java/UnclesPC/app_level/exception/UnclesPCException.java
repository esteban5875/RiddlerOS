package UnclesPC.app_level.exception;

import UnclesPC.virtual_hardware.motherboard.error.ErrorCode;

public final class UnclesPCException extends RuntimeException {
    private final ErrorCode errorCode;

    public UnclesPCException(String message) {
        super(message);
        this.errorCode = null;
    }

    public UnclesPCException(String message, Throwable cause) {
        super(message, cause);
        this.errorCode = null;
    }

    public UnclesPCException(ErrorCode errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
    }

    public UnclesPCException(ErrorCode errorCode, String message, Throwable cause) {
        super(message, cause);
        this.errorCode = errorCode;
    }

    public ErrorCode errorCode() {
        return errorCode;
    }
}
