package UnclesPC.hardware.cpu.modules;

import java.util.LinkedHashMap;
import java.util.Map;

public final class RegSet {
    private RegSet() {
    }

    public static Map<Integer, Integer> defaultGeneralRegisters() {
        Map<Integer, Integer> registers = new LinkedHashMap<>();
        for (int i = 0; i < 16; i++) {
            registers.put(i, 0);
        }
        return registers;
    }
}
