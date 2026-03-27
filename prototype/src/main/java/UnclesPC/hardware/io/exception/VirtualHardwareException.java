package UnclesPC.hardware.io.exception;

public class VirtualHardwareException extends Exception {
    private final int error_code;

    public VirtualHardwareException(int error_code) {
        super();
        this.error_code = error_code;
    }

    public int getErrorCode(){
        return error_code;
    }
}
