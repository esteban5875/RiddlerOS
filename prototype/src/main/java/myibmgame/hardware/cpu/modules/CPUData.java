package myibmgame.hardware.cpu.modules;

import java.util.LinkedHashMap;
import java.util.Map;
import myibmgame.hardware.motherboard.error.ErrorCode;
import myibmgame.hardware.motherboard.error.MyIBMGameException;

public final class CPUData {
    public static final int ADDR_MASK = 0x7FFFFF;
    public static final long REG_MASK = 0xFFFFFFFFL;
    public static final int REAL_MODE_LIMIT = 0x100000;
    public static final int SIGN_BIT = 1 << 31;

    private CPUData() {
    }

    public static Map<String, Integer> defaultFlags() {
        Map<String, Integer> flags = new LinkedHashMap<>();
        flags.put("C", 0);
        flags.put("Z", 0);
        flags.put("N", 0);
        flags.put("V", 0);
        return flags;
    }

    public enum CpuMode {
        REAL(0x00),
        PROTECTED_USER(0x01),
        PROTECTED_KERNEL(0x02);

        private final int code;

        CpuMode(int code) {
            this.code = code;
        }

        public int code() {
            return code;
        }
    }

    public enum OperandType {
        NONE(0),
        REG(1),
        IMM(2),
        MEM(3);

        private final int code;

        OperandType(int code) {
            this.code = code;
        }

        public int code() {
            return code;
        }
    }

    public enum InstMode {
        REG_REG(0x00),
        REG_IMM(0x01),
        REG_MEM(0x02),
        NONE(0xFF);

        private final int code;

        InstMode(int code) {
            this.code = code;
        }

        public int code() {
            return code;
        }

        public static InstMode fromCode(int code) {
            for (InstMode mode : values()) {
                if (mode.code == code) {
                    return mode;
                }
            }
            throw new MyIBMGameException(
                ErrorCode.INVALID_INSTRUCTION,
                "unknown instruction mode: " + code
            );
        }
    }

    public enum RegType {
        SPECIAL(0x00),
        GENERAL(0x01);

        private final int code;

        RegType(int code) {
            this.code = code;
        }

        public int code() {
            return code;
        }
    }
}
