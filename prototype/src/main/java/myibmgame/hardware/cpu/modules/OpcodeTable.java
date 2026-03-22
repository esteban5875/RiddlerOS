package myibmgame.hardware.cpu.modules;

import java.util.LinkedHashMap;
import java.util.Map;

public final class OpcodeTable {
    public static final Map<Integer, OpcodeDescriptor> OPCODE_TABLE = buildOpcodeTable();

    private OpcodeTable() {
    }

    private static Map<Integer, OpcodeDescriptor> buildOpcodeTable() {
        Map<Integer, OpcodeDescriptor> table = new LinkedHashMap<>();
        table.put(InstSet.ADVANCE.get("NOP"), new OpcodeDescriptor("NOP", 0, "noop"));
        table.put(InstSet.ALU.get("ADD"), new OpcodeDescriptor("ADD", 2, "executeALU"));
        table.put(InstSet.ALU.get("SUB"), new OpcodeDescriptor("SUB", 2, "executeALU"));
        table.put(InstSet.ALU.get("MUL"), new OpcodeDescriptor("MUL", 2, "executeALU"));
        table.put(InstSet.ALU.get("DIV"), new OpcodeDescriptor("DIV", 2, "executeALU"));
        table.put(InstSet.ALU.get("INC"), new OpcodeDescriptor("INC", 1, "executeALU"));
        table.put(InstSet.ALU.get("DEC"), new OpcodeDescriptor("DEC", 1, "executeALU"));
        table.put(InstSet.ALU.get("AND"), new OpcodeDescriptor("AND", 2, "executeALU"));
        table.put(InstSet.ALU.get("OR"), new OpcodeDescriptor("OR", 2, "executeALU"));
        table.put(InstSet.ALU.get("XOR"), new OpcodeDescriptor("XOR", 2, "executeALU"));
        table.put(InstSet.ALU.get("NOT"), new OpcodeDescriptor("NOT", 1, "executeALU"));
        table.put(InstSet.ALU.get("CMP"), new OpcodeDescriptor("CMP", 2, "executeALU"));
        table.put(InstSet.DATA_MOVEMENT.get("MOV"), new OpcodeDescriptor("MOV", 2, "execute_MOV"));
        table.put(InstSet.DATA_MOVEMENT.get("LDACC"), new OpcodeDescriptor("LDACC", 1, "execute_LDACC"));
        table.put(InstSet.DATA_MOVEMENT.get("CLACC"), new OpcodeDescriptor("CLACC", 0, "execute_CLACC"));
        table.put(InstSet.DATA_MOVEMENT.get("LDX"), new OpcodeDescriptor("LDX", 1, "execute_LDX"));
        table.put(InstSet.DATA_MOVEMENT.get("LDY"), new OpcodeDescriptor("LDY", 1, "execute_LDY"));
        table.put(InstSet.DATA_MOVEMENT.get("STX"), new OpcodeDescriptor("STX", 1, "execute_STX"));
        table.put(InstSet.CONTROL_FLOW.get("JMP"), new OpcodeDescriptor("JMP", 1, "execute_jump"));
        table.put(InstSet.CONTROL_FLOW.get("JZ"), new OpcodeDescriptor("JZ", 1, "conditional_jump"));
        table.put(InstSet.CONTROL_FLOW.get("JNZ"), new OpcodeDescriptor("JNZ", 1, "conditional_jump"));
        table.put(InstSet.CONTROL_FLOW.get("JC"), new OpcodeDescriptor("JC", 1, "conditional_jump"));
        table.put(InstSet.CONTROL_FLOW.get("JNC"), new OpcodeDescriptor("JNC", 1, "conditional_jump"));
        table.put(InstSet.CONTROL_FLOW.get("JGT"), new OpcodeDescriptor("JGT", 1, "conditional_jump"));
        table.put(InstSet.CONTROL_FLOW.get("JLT"), new OpcodeDescriptor("JLT", 1, "conditional_jump"));
        table.put(InstSet.CONTROL_FLOW.get("CALL"), new OpcodeDescriptor("CALL", 1, "execute_call"));
        table.put(InstSet.CONTROL_FLOW.get("RET"), new OpcodeDescriptor("RET", 0, "execute_ret"));
        table.put(InstSet.CONTROL_FLOW.get("INT"), new OpcodeDescriptor("INT", 0, "trigger_interrupt"));
        table.put(InstSet.STACK.get("PUSHY"), new OpcodeDescriptor("PUSHY", 0, "execute_pushy"));
        table.put(InstSet.STACK.get("POPY"), new OpcodeDescriptor("POPY", 0, "execute_popy"));
        table.put(InstSet.EOF_OPS.get("HALT"), new OpcodeDescriptor("HALT", 0, "halt"));
        return Map.copyOf(table);
    }
}
