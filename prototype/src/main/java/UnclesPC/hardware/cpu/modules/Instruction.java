package UnclesPC.hardware.cpu.modules;

public record Instruction(int opcode, CPUData.InstMode mode, int operand1, int operand2) {
}
