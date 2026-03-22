package myibmgame.hardware.motherboard.error;

public final class MyIBMGameException extends RuntimeException {
    private final ErrorCode errorCode;

    public MyIBMGameException(ErrorCode errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
    }

    public ErrorCode errorCode() {
        return errorCode;
    }
}
