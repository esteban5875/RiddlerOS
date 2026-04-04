package UnclesPC.virtual_hardware.cpu.modules;

public record Instruction(int opcode, CPUData.InstMode mode, int operand1, int operand2) {
    public static final int OPERAND_SIZE = 4;
    public static final int BYTE_SIZE = 2 + (2 * OPERAND_SIZE);
    public static final int OPCODE_OFFSET = 0;
    public static final int MODE_OFFSET = 1;
    public static final int OPERAND1_OFFSET = 2;
    public static final int OPERAND2_OFFSET = OPERAND1_OFFSET + OPERAND_SIZE;
}
