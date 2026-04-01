package UnclesPC.hardware.io.exception;

public class VirtualHardwareException extends Exception {
    private final int error_code;

    public VirtualHardwareException(int error_code) {
        super();
        this.error_code = error_code & 0xFFFFFFFF; // Ensure error_code is treated as 32-bit unsigned
    }

    public int getErrorCode(){
        return error_code;
    }
}
