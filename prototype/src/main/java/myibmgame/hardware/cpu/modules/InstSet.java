package myibmgame.hardware.cpu.modules;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

public final class InstSet {
    public static final Map<String, Integer> ADVANCE = Map.of(
        "NOP", 0x00
    );

    public static final Map<String, Integer> ALU = Map.ofEntries(
        Map.entry("ADD", 0x01),
        Map.entry("SUB", 0x02),
        Map.entry("MUL", 0x03),
        Map.entry("DIV", 0x04),
        Map.entry("INC", 0x05),
        Map.entry("DEC", 0x06),
        Map.entry("AND", 0x07),
        Map.entry("OR", 0x08),
        Map.entry("XOR", 0x09),
        Map.entry("NOT", 0x10),
        Map.entry("CMP", 0x11)
    );

    public static final Map<String, Integer> DATA_MOVEMENT = Map.ofEntries(
        Map.entry("MOV", 0x12),
        Map.entry("LDACC", 0x13),
        Map.entry("CLACC", 0x14),
        Map.entry("LDX", 0x15),
        Map.entry("STX", 0x16),
        Map.entry("LDY", 0x17)
    );

    public static final Map<String, Integer> CONTROL_FLOW = Map.ofEntries(
        Map.entry("JMP", 0x22),
        Map.entry("JZ", 0x23),
        Map.entry("JNZ", 0x24),
        Map.entry("JC", 0x25),
        Map.entry("JNC", 0x26),
        Map.entry("JGT", 0x27),
        Map.entry("JLT", 0x28),
        Map.entry("CALL", 0x29),
        Map.entry("RET", 0x30),
        Map.entry("INT", 0x31)
    );

    public static final Map<String, Integer> STACK = Map.of(
        "PUSHY", 0x32,
        "POPY", 0x33
    );

    public static final Set<String> ASSEMBLER_DIRECTIVES = Set.of(
        "DB",
        "INCLUDE",
        "ORG",
        "EQU",
        "DW",
        "DS"
    );

    public static final Map<String, Integer> EOF_OPS = Map.of(
        "HALT", 0xFF
    );

    private InstSet() {
    }

    public static Map<String, Integer> allOpcodes() {
        Map<String, Integer> opcodes = new LinkedHashMap<>();
        opcodes.putAll(ADVANCE);
        opcodes.putAll(ALU);
        opcodes.putAll(DATA_MOVEMENT);
        opcodes.putAll(CONTROL_FLOW);
        opcodes.putAll(STACK);
        opcodes.putAll(EOF_OPS);
        return opcodes;
    }
}
