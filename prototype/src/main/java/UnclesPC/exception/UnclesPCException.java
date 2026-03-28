package UnclesPC.exception;

import UnclesPC.hardware.motherboard.error.ErrorCode;

public final class UnclesPCException extends RuntimeException {
    private final ErrorCode errorCode;

    public UnclesPCException(ErrorCode errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
    }

    public ErrorCode errorCode() {
        return errorCode;
    }
}
