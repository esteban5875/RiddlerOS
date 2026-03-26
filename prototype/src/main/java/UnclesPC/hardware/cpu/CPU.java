package UnclesPC.hardware.cpu;

import java.util.LinkedHashMap;
import java.util.Map;

import UnclesPC.hardware.cpu.modules.CPUData;
import UnclesPC.hardware.cpu.modules.CPUState;
import UnclesPC.hardware.cpu.modules.InstSet;
import UnclesPC.hardware.cpu.modules.Instruction;
import UnclesPC.hardware.cpu.modules.OpcodeDescriptor;
import UnclesPC.hardware.cpu.modules.OpcodeTable;
import UnclesPC.hardware.motherboard.error.ErrorCode;
import UnclesPC.hardware.motherboard.error.MyIBMGameException;
import UnclesPC.hardware.ram.Memory;
import UnclesPC.hardware.ram.modules.MemoryMap;
import UnclesPC.hardware.ram.modules.VectorTable;

public final class CPU {
    private final Memory memory;
    private final CPUState state;
    private final Map<Integer, OpcodeDescriptor> opcodeTable;
    private boolean halted;

    public CPU() {
        this.memory = new Memory();
        this.state = new CPUState();
        this.opcodeTable = OpcodeTable.OPCODE_TABLE;
        this.halted = false;
    }

    public Memory memory() {
        return memory;
    }

    public int getReg(int idx) {
        return state.generalRegs().getOrDefault(idx, 0);
    }

    public void setReg(int idx, int value) {
        state.generalRegs().put(idx, mask32(value));
    }

    public CPUData.CpuMode mode() {
        return state.mode();
    }

    public int pc() {
        return state.pc();
    }

    public int acc() {
        return state.acc();
    }

    public int x() {
        return state.x();
    }

    public int y() {
        return state.y();
    }

    public Map<String, Integer> flags() {
        requireKernel();
        return new LinkedHashMap<>(state.flags());
    }

    public int sp() {
        requireKernel();
        return state.sp();
    }

    public boolean isHalted() {
        return halted;
    }

    public void executeJump(int address) {
        validateExecutionTarget(address);
        setPc(address);
    }

    public void conditionalJump(int address, String mnemonic) {
        boolean shouldJump = switch (mnemonic) {
            case "JZ" -> zeroFlagOn();
            case "JC" -> carryFlagOn();
            case "JNZ" -> !zeroFlagOn();
            case "JNC" -> !carryFlagOn();
            case "JGT" -> !zeroFlagOn() && negativeFlagOn() == overflowFlagOn();
            case "JLT" -> negativeFlagOn() != overflowFlagOn();
            default -> false;
        };

        if (shouldJump) {
            executeJump(address);
        }
    }

    public void executeALU(String op, int src, CPUData.InstMode mode, int dest) {
        if (!InstSet.ALU.containsKey(op)) {
            throw new MyIBMGameException(ErrorCode.INVALID_INSTRUCTION, "unknown ALU operation: " + op);
        }

        int destValue = getReg(dest);
        int operand = resolveBinaryOperand(mode, src);
        long rawResult;
        int maskedResult;

        switch (op) {
            case "ADD" -> {
                rawResult = Integer.toUnsignedLong(destValue) + Integer.toUnsignedLong(operand);
                maskedResult = mask32(rawResult);
                setReg(dest, maskedResult);
                updateFlags(rawResult, maskedResult, op, destValue, operand);
            }
            case "SUB" -> {
                rawResult = Integer.toUnsignedLong(destValue) - Integer.toUnsignedLong(operand);
                maskedResult = mask32(rawResult);
                setReg(dest, maskedResult);
                updateFlags(rawResult, maskedResult, op, destValue, operand);
            }
            case "MUL" -> {
                rawResult = Integer.toUnsignedLong(destValue) * Integer.toUnsignedLong(operand);
                maskedResult = mask32(rawResult);
                setReg(dest, maskedResult);
                updateFlags(rawResult, maskedResult, op, destValue, operand);
            }
            case "DIV" -> {
                if (operand == 0) {
                    divZero();
                }
                rawResult = Integer.toUnsignedLong(Integer.divideUnsigned(destValue, operand));
                maskedResult = mask32(rawResult);
                setReg(dest, maskedResult);
                updateFlags(rawResult, maskedResult, op, destValue, operand);
            }
            case "AND" -> {
                rawResult = destValue & operand;
                maskedResult = mask32(rawResult);
                setReg(dest, maskedResult);
                updateFlags(rawResult, maskedResult, op, destValue, operand);
            }
            case "OR" -> {
                rawResult = destValue | operand;
                maskedResult = mask32(rawResult);
                setReg(dest, maskedResult);
                updateFlags(rawResult, maskedResult, op, destValue, operand);
            }
            case "XOR" -> {
                rawResult = destValue ^ operand;
                maskedResult = mask32(rawResult);
                setReg(dest, maskedResult);
                updateFlags(rawResult, maskedResult, op, destValue, operand);
            }
            case "NOT" -> {
                rawResult = ~destValue;
                maskedResult = mask32(rawResult);
                setReg(dest, maskedResult);
                updateFlags(rawResult, maskedResult, op, destValue, operand);
            }
            case "INC" -> {
                rawResult = Integer.toUnsignedLong(destValue) + 1;
                maskedResult = mask32(rawResult);
                setReg(dest, maskedResult);
                updateFlags(rawResult, maskedResult, "ADD", destValue, 1);
            }
            case "DEC" -> {
                rawResult = Integer.toUnsignedLong(destValue) - 1;
                maskedResult = mask32(rawResult);
                setReg(dest, maskedResult);
                updateFlags(rawResult, maskedResult, "SUB", destValue, 1);
            }
            case "CMP" -> {
                rawResult = Integer.toUnsignedLong(destValue) - Integer.toUnsignedLong(operand);
                maskedResult = mask32(rawResult);
                updateFlags(rawResult, maskedResult, "SUB", destValue, operand);
            }
            default -> throw new MyIBMGameException(ErrorCode.INVALID_INSTRUCTION, "unsupported ALU op: " + op);
        }
    }

    public void executeLDACC(int value) {
        setAcc(value);
    }

    public void executeCLACC() {
        setAcc(0);
    }

    public void executeLDX(int value) {
        setX(value);
    }

    public void executeLDY(int value) {
        setY(value);
    }

    public void executePushy() {
        setSp(memory.pushWord(y(), sp()));
    }

    public void executePopy() {
        Memory.PopResult result = memory.popWord(sp());
        setSp(result.newSp());
        setY(result.value());
    }

    public void triggerInterrupt(Integer interruptNumber) {
        int actualInterrupt = interruptNumber == null ? acc() & 0xFF : interruptNumber & 0xFF;

        if (actualInterrupt == VectorTable.BIOS_MODE.value()) {
            switchToProtectedKernel();
            return;
        }

        int handler = memory.getVector(actualInterrupt);
        setSp(memory.pushWord(pc(), sp()));
        executeJump(handler);
    }

    public void executeSTX(int address) {
        memory.write(address, x());
    }

    public void executeMOV(CPUData.InstMode mode, int dest, int src) {
        switch (mode) {
            case REG_REG -> setReg(dest, getReg(src));
            case REG_IMM -> setReg(dest, src);
            case REG_MEM -> setReg(dest, memory.read(src));
            default -> throw new MyIBMGameException(ErrorCode.INVALID_INSTRUCTION, "unsupported MOV mode");
        }
    }

    public void executeCall(int address) {
        setSp(memory.pushWord(pc(), sp()));
        executeJump(address);
    }

    public void executeRet() {
        Memory.PopResult result = memory.popWord(sp());
        setSp(result.newSp());
        executeJump(result.value());
    }

    public void reset() {
        setMode(CPUData.CpuMode.REAL);
        setPc(MemoryMap.BOOTLOADER_START.value());
        setSp(MemoryMap.STACK_TOP.value());
        halted = false;

        for (int address = MemoryMap.VECTOR_TABLE_START.value(); address <= MemoryMap.VECTOR_TABLE_END.value(); address++) {
            memory.write(address, 0);
        }
    }

    public void switchToProtected() {
        if (state.mode() != CPUData.CpuMode.REAL) {
            throw new MyIBMGameException(ErrorCode.CPU_MODE_ERROR, "CPU is not in real mode");
        }

        setMode(CPUData.CpuMode.PROTECTED_USER);
        setPc(MemoryMap.USER_START.value());
    }

    public void switchToProtectedKernel() {
        if (state.mode() != CPUData.CpuMode.REAL) {
            throw new MyIBMGameException(ErrorCode.CPU_MODE_ERROR, "CPU is not in real mode");
        }

        setMode(CPUData.CpuMode.PROTECTED_KERNEL);
        setPc(MemoryMap.KERNEL_START.value());
    }

    public Map<String, Object> snapshot() {
        Map<String, Object> snapshot = new LinkedHashMap<>();
        snapshot.put("PC", state.pc());
        snapshot.put("SP", state.sp());
        snapshot.put("Mode", state.mode().name());
        snapshot.put("ACC", state.acc());
        snapshot.put("X", state.x());
        snapshot.put("Y", state.y());
        snapshot.put("FLAGS", new LinkedHashMap<>(state.flags()));
        snapshot.put("REGS", new LinkedHashMap<>(state.generalRegs()));
        snapshot.put("HALTED", halted);
        return snapshot;
    }

    public int[] fetch() {
        if (halted) {
            throw new IllegalStateException("CPU is halted");
        }

        int currentPc = pc();

        if (state.mode() == CPUData.CpuMode.PROTECTED_USER
            && (currentPc < MemoryMap.USER_START.value() || currentPc > MemoryMap.USER_END.value())) {
            throw new MyIBMGameException(ErrorCode.CPU_MODE_ERROR, "PC is outside user space");
        }

        int[] instruction = {
            memory.read(currentPc),
            memory.read(currentPc + 1),
            memory.read(currentPc + 2),
            memory.read(currentPc + 3)
        };

        setPc(currentPc + 4);
        return instruction;
    }

    public Instruction decode(int[] instruction) {
        if (instruction == null || instruction.length != 4) {
            throw new MyIBMGameException(ErrorCode.INVALID_INSTRUCTION, "instruction must contain four bytes");
        }

        int opcode = instruction[0] & 0xFF;
        if (!opcodeTable.containsKey(opcode)) {
            throw new MyIBMGameException(ErrorCode.INVALID_INSTRUCTION, "unknown opcode: " + opcode);
        }

        CPUData.InstMode mode = CPUData.InstMode.fromCode(instruction[1] & 0xFF);
        return new Instruction(opcode, mode, instruction[2] & 0xFF, instruction[3] & 0xFF);
    }

    public void execute(Instruction instruction) {
        OpcodeDescriptor descriptor = opcodeTable.get(instruction.opcode());
        if (descriptor == null) {
            throw new MyIBMGameException(ErrorCode.INVALID_INSTRUCTION, "opcode is not registered");
        }

        switch (descriptor.handler()) {
            case "noop" -> {
            }
            case "halt" -> halted = true;
            case "executeALU" -> executeALU(
                descriptor.opcode(),
                instruction.operand2(),
                instruction.mode(),
                instruction.operand1()
            );
            case "execute_MOV" -> executeMOV(instruction.mode(), instruction.operand1(), instruction.operand2());
            case "execute_LDACC" -> executeLDACC(resolveUnaryOperand(instruction.mode(), instruction.operand1()));
            case "execute_CLACC" -> executeCLACC();
            case "execute_LDX" -> executeLDX(resolveUnaryOperand(instruction.mode(), instruction.operand1()));
            case "execute_LDY" -> executeLDY(resolveUnaryOperand(instruction.mode(), instruction.operand1()));
            case "execute_STX" -> executeSTX(resolveUnaryOperand(instruction.mode(), instruction.operand1()));
            case "execute_jump" -> executeJump(resolveUnaryOperand(instruction.mode(), instruction.operand1()));
            case "conditional_jump" -> conditionalJump(
                resolveUnaryOperand(instruction.mode(), instruction.operand1()),
                descriptor.opcode()
            );
            case "execute_call" -> executeCall(resolveUnaryOperand(instruction.mode(), instruction.operand1()));
            case "execute_ret" -> executeRet();
            case "trigger_interrupt" -> triggerInterrupt(null);
            case "execute_pushy" -> executePushy();
            case "execute_popy" -> executePopy();
            default -> throw new MyIBMGameException(ErrorCode.INVALID_INSTRUCTION, "unknown handler: " + descriptor.handler());
        }
    }

    public void step() {
        execute(decode(fetch()));
    }

    private void updateFlags(long rawResult, int maskedResult, String op, int a, int b) {
        state.flags().put("Z", maskedResult == 0 ? 1 : 0);

        int carry = switch (op) {
            case "ADD", "MUL" -> rawResult > CPUData.REG_MASK ? 1 : 0;
            case "SUB" -> Integer.compareUnsigned(a, b) < 0 ? 1 : 0;
            default -> 0;
        };
        state.flags().put("C", carry);
        state.flags().put("N", (maskedResult & CPUData.SIGN_BIT) != 0 ? 1 : 0);

        boolean signA = (a & CPUData.SIGN_BIT) != 0;
        boolean signB = (b & CPUData.SIGN_BIT) != 0;
        boolean signR = (maskedResult & CPUData.SIGN_BIT) != 0;

        boolean overflow = switch (op) {
            case "ADD" -> signA == signB && signR != signA;
            case "SUB" -> signA != signB && signR != signA;
            default -> false;
        };
        state.flags().put("V", overflow ? 1 : 0);
    }

    private void divZero() {
        throw new MyIBMGameException(ErrorCode.DIVISION_BY_ZERO, "division by zero");
    }

    private int resolveBinaryOperand(CPUData.InstMode mode, int src) {
        return switch (mode) {
            case REG_REG -> getReg(src);
            case REG_IMM -> src;
            case REG_MEM -> memory.read(src);
            case NONE -> throw new MyIBMGameException(ErrorCode.INVALID_INSTRUCTION, "instruction mode NONE is invalid here");
        };
    }

    private int resolveUnaryOperand(CPUData.InstMode mode, int operand) {
        return switch (mode) {
            case REG_REG -> getReg(operand);
            case REG_IMM -> operand;
            case REG_MEM -> memory.read(operand);
            case NONE -> operand;
        };
    }

    private boolean zeroFlagOn() {
        return state.flags().get("Z") == 1;
    }

    private boolean carryFlagOn() {
        return state.flags().get("C") == 1;
    }

    private boolean negativeFlagOn() {
        return state.flags().get("N") == 1;
    }

    private boolean overflowFlagOn() {
        return state.flags().get("V") == 1;
    }

    private void requireKernel() {
        if (state.mode() == CPUData.CpuMode.PROTECTED_USER) {
            throw new MyIBMGameException(ErrorCode.CPU_MODE_ERROR, "operation requires kernel privileges");
        }
    }

    private void setMode(CPUData.CpuMode mode) {
        state.setMode(mode);
    }

    private void setPc(int value) {
        state.setPc(value & CPUData.ADDR_MASK);
    }

    private void setAcc(int value) {
        state.setAcc(mask32(value));
    }

    private void setX(int value) {
        state.setX(mask32(value));
    }

    private void setY(int value) {
        state.setY(mask32(value));
    }

    private void setSp(int value) {
        requireKernel();
        state.setSp(value & CPUData.ADDR_MASK);
    }

    private void validateExecutionTarget(int address) {
        switch (state.mode()) {
            case PROTECTED_USER -> {
                if (address < MemoryMap.USER_START.value() || address > MemoryMap.USER_END.value()) {
                    throw new MyIBMGameException(ErrorCode.CPU_MODE_ERROR, "jump target is outside user space");
                }
            }
            case PROTECTED_KERNEL -> {
                if (address < MemoryMap.KERNEL_START.value() || address > MemoryMap.KERNEL_END.value()) {
                    throw new MyIBMGameException(ErrorCode.CPU_MODE_ERROR, "jump target is outside kernel space");
                }
            }
            case REAL -> {
                if (address < MemoryMap.BOOTLOADER_START.value() || address > MemoryMap.BOOTLOADER_END.value()) {
                    throw new MyIBMGameException(ErrorCode.CPU_MODE_ERROR, "jump target is outside bootloader space");
                }
            }
        }
    }

    private int mask32(long value) {
        return (int) (value & CPUData.REG_MASK);
    }
}
