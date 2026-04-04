package UnclesPC.virtual_hardware.cpu;

import java.util.LinkedHashMap;
import java.util.Map;

import UnclesPC.app_level.exception.UnclesPCException;
import UnclesPC.virtual_hardware.cpu.modules.CPUData;
import UnclesPC.virtual_hardware.cpu.modules.CPUState;
import UnclesPC.virtual_hardware.cpu.modules.InstSet;
import UnclesPC.virtual_hardware.cpu.modules.Instruction;
import UnclesPC.virtual_hardware.cpu.modules.OpcodeDescriptor;
import UnclesPC.virtual_hardware.cpu.modules.OpcodeTable;
import UnclesPC.virtual_hardware.io.exception.VirtualHardwareException;
import UnclesPC.virtual_hardware.motherboard.error.ErrorCode;
import UnclesPC.virtual_hardware.ram.Memory;
import UnclesPC.virtual_hardware.ram.modules.MemoryMap;
import UnclesPC.virtual_hardware.ram.modules.VectorTable;

public final class CPU {
    private final Memory memory;
    private final CPUState state;
    private final Map<Integer, OpcodeDescriptor> opcodeTable;
    private boolean halted;

    public CPU() {
        this(new Memory());
    }

    public CPU(Memory memory) {
        this.memory = memory;
        this.state = new CPUState();
        this.opcodeTable = OpcodeTable.OPCODE_TABLE;
        this.halted = false;
        syncStackPointerFromMemory();
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
        return new LinkedHashMap<>(state.flags());
    }

    public int sp() {
        syncStackPointerFromMemory();
        return state.sp();
    }

    public boolean isHalted() {
        return halted;
    }

    public void executeJump(int address) throws VirtualHardwareException {
        validateExecutionTarget(address);
        setPc(address);
    }

    public void conditionalJump(int address, String mnemonic) throws VirtualHardwareException {
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

    public void executeALU(String op, int src, CPUData.InstMode mode, int dest) throws VirtualHardwareException {
        if (!InstSet.ALU.containsKey(op)) {
            throw new UnclesPCException("Unknown ALU operation: " + op);
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
            default -> throw new UnclesPCException("Unsupported ALU op: " + op);
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

    public void executePushy() throws VirtualHardwareException {
        memory.pushWord(y());
        syncStackPointerFromMemory();
    }

    public void executePopy() throws VirtualHardwareException {
        int value = memory.popWord();
        syncStackPointerFromMemory();
        setY(value);
    }

    public void triggerInterrupt(Integer interruptNumber) throws VirtualHardwareException {
        if (state.mode() == CPUData.CpuMode.PROTECTED) {
            throw new VirtualHardwareException(
                ErrorCode.CPU_MODE_ERROR,
                "software interrupts are unavailable in protected mode; use MMIO"
            );
        }

        int actualInterrupt = interruptNumber == null ? acc() & 0xFF : interruptNumber & 0xFF;

        if (actualInterrupt == VectorTable.BIOS_MODE.value()) {
            switchToProtected();
            return;
        }

        int handler = memory.getVector(actualInterrupt);
        memory.pushWord(pc());
        syncStackPointerFromMemory();
        validateInterruptTarget(handler);
        setPc(handler);
    }

    public void executeSTX(int address) throws VirtualHardwareException {
        validateDataAccess(address, 1);
        memory.writeByte(address, x());
    }

    public void executeSTW(int address) throws VirtualHardwareException {
        if (isFirmwareInstruction() && isFirmwareWriteTarget(address, 4)) {
            memory.writeWord(address, x());
            return;
        }

        validateDataAccess(address, 4);
        memory.writeWord(address, x());
    }

    public void executeMOV(CPUData.InstMode mode, int dest, int src) throws VirtualHardwareException {
        switch (mode) {
            case REG_REG -> setReg(dest, getReg(src));
            case REG_IMM -> setReg(dest, src);
            case REG_MEM -> {
                validateDataAccess(src, 1);
                setReg(dest, memory.readByte(src));
            }
            default -> throw new VirtualHardwareException(
                ErrorCode.INVALID_INSTRUCTION,
                "unsupported MOV mode"
            );
        }
    }

    public void executeCall(int address) throws VirtualHardwareException {
        memory.pushWord(pc());
        syncStackPointerFromMemory();
        executeJump(address);
    }

    public void executeRet() throws VirtualHardwareException {
        int value = memory.popWord();
        syncStackPointerFromMemory();
        executeJump(value);
    }

    public void reset() {
        setMode(CPUData.CpuMode.REAL);
        setPc(MemoryMap.FIRMWARE_START.value());
        memory.resetStackPointer();
        syncStackPointerFromMemory();
        halted = false;

        for (int address = MemoryMap.VECTOR_TABLE_START.value(); address <= MemoryMap.VECTOR_TABLE_END.value(); address++) {
            try {
                memory.writeByte(address, 0);
            } catch (VirtualHardwareException e) {
                throw new UnclesPCException(
                    ErrorCode.MEMORY_OUT_OF_BOUNDS,
                    "unable to reset CPU vector table",
                    e
                );
            }
        }
    }

    public void switchToProtected() throws VirtualHardwareException {
        if (state.mode() != CPUData.CpuMode.REAL) {
            throw new VirtualHardwareException(ErrorCode.CPU_MODE_ERROR, "CPU is not in real mode");
        }

        setMode(CPUData.CpuMode.PROTECTED);
        setPc(MemoryMap.PROGRAM_START.value());
    }

    public Map<String, Object> snapshot() {
        syncStackPointerFromMemory();
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

    public int[] fetch() throws VirtualHardwareException {
        if (halted) {
            throw new UnclesPCException("CPU is halted");
        }

        int currentPc = pc();

        validateFetchAddress(currentPc);

        int[] instruction = new int[Instruction.BYTE_SIZE];
        for (int i = 0; i < Instruction.BYTE_SIZE; i++) {
            instruction[i] = memory.readByte(currentPc + i);
        }

        setPc(currentPc + Instruction.BYTE_SIZE);
        return instruction;
    }

    public Instruction decode(int[] instruction) throws VirtualHardwareException {
        if (instruction == null || instruction.length != Instruction.BYTE_SIZE) {
            throw new UnclesPCException(
                "Instruction must contain " + Instruction.BYTE_SIZE + " bytes"
            );
        }

        int opcode = instruction[Instruction.OPCODE_OFFSET] & 0xFF;
        if (!opcodeTable.containsKey(opcode)) {
            throw new VirtualHardwareException(
                ErrorCode.INVALID_INSTRUCTION,
                "unknown opcode: " + opcode
            );
        }

        CPUData.InstMode mode = CPUData.InstMode.fromCode(
            instruction[Instruction.MODE_OFFSET] & 0xFF
        );

        return new Instruction(
            opcode,
            mode,
            decodeOperand(instruction, Instruction.OPERAND1_OFFSET),
            decodeOperand(instruction, Instruction.OPERAND2_OFFSET)
        );
    }

    public void execute(Instruction instruction) throws VirtualHardwareException {
        OpcodeDescriptor descriptor = opcodeTable.get(instruction.opcode());
        if (descriptor == null) {
            throw new UnclesPCException("Opcode is not registered");
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
            case "execute_STX" -> executeSTX(resolveAddressOperand(instruction.mode(), instruction.operand1()));
            case "execute_STW" -> executeSTW(resolveAddressOperand(instruction.mode(), instruction.operand1()));
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
            default -> throw new UnclesPCException("Unknown handler: " + descriptor.handler());
        }
    }

    public void step() throws VirtualHardwareException {
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

    private void divZero() throws VirtualHardwareException {
        throw new VirtualHardwareException(ErrorCode.DIVISION_BY_ZERO, "division by zero");
    }

    private int resolveBinaryOperand(CPUData.InstMode mode, int src) throws VirtualHardwareException {
        return switch (mode) {
            case REG_REG -> getReg(src);
            case REG_IMM -> src;
            case REG_MEM -> {
                validateDataAccess(src, 1);
                yield memory.readByte(src);
            }
            case NONE -> throw new VirtualHardwareException(
                ErrorCode.INVALID_INSTRUCTION,
                "instruction mode NONE is invalid here"
            );
        };
    }

    private int resolveUnaryOperand(CPUData.InstMode mode, int operand) throws VirtualHardwareException {
        return switch (mode) {
            case REG_REG -> getReg(operand);
            case REG_IMM -> operand;
            case REG_MEM -> {
                validateDataAccess(operand, 1);
                yield memory.readByte(operand);
            }
            case NONE -> operand;
        };
    }

    private int resolveAddressOperand(CPUData.InstMode mode, int operand) {
        return switch (mode) {
            case REG_REG -> getReg(operand);
            case REG_IMM, REG_MEM, NONE -> operand;
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

    private void syncStackPointerFromMemory() {
        state.setSp(memory.stackPointer());
    }

    private void validateExecutionTarget(int address) throws VirtualHardwareException {
        switch (state.mode()) {
            case PROTECTED -> {
                if (address < MemoryMap.PROGRAM_START.value() || address > MemoryMap.PROGRAM_END.value()) {
                    throw new VirtualHardwareException(
                        ErrorCode.CPU_MODE_ERROR,
                        "jump target is outside program space"
                    );
                }
            }
            case REAL -> {
                if (address < MemoryMap.BOOTLOADER_START.value() || address > MemoryMap.BOOTLOADER_END.value()) {
                    throw new VirtualHardwareException(
                        ErrorCode.CPU_MODE_ERROR,
                        "jump target is outside bootloader space"
                    );
                }
            }
        }
    }

    private void validateFetchAddress(int address) throws VirtualHardwareException {
        switch (state.mode()) {
            case PROTECTED -> {
                if (address < MemoryMap.PROGRAM_START.value()
                    || address + Instruction.BYTE_SIZE - 1 > MemoryMap.PROGRAM_END.value()) {
                    throw new VirtualHardwareException(
                        ErrorCode.CPU_MODE_ERROR,
                        "PC is outside program space"
                    );
                }
            }
            case REAL -> {
                boolean inBootloader = address >= MemoryMap.BOOTLOADER_START.value()
                    && address + Instruction.BYTE_SIZE - 1 <= MemoryMap.BOOTLOADER_END.value();
                boolean inFirmware = address >= MemoryMap.FIRMWARE_START.value()
                    && address + Instruction.BYTE_SIZE - 1 <= MemoryMap.FIRMWARE_END.value();

                if (!inBootloader && !inFirmware) {
                    throw new VirtualHardwareException(
                        ErrorCode.CPU_MODE_ERROR,
                        "PC is outside real-mode bootloader/firmware space"
                    );
                }
            }
        }
    }

    private void validateDataAccess(int address, int length) throws VirtualHardwareException {
        int lastAddress = address + length - 1;

        switch (state.mode()) {
            case PROTECTED -> {
                boolean inProgram = address >= MemoryMap.PROGRAM_START.value()
                    && lastAddress <= MemoryMap.PROGRAM_END.value();
                boolean inMmio = address >= MemoryMap.MMIO_START.value()
                    && lastAddress <= MemoryMap.MMIO_END.value();

                if (!inProgram && !inMmio) {
                    throw new VirtualHardwareException(
                        ErrorCode.MEMORY_OUT_OF_BOUNDS,
                        "protected-mode access outside program/MMIO space: " + address
                    );
                }
            }
            case REAL -> {
                if (address < MemoryMap.BOOTLOADER_START.value()
                    || lastAddress > MemoryMap.BOOTLOADER_END.value()) {
                    throw new VirtualHardwareException(
                        ErrorCode.MEMORY_OUT_OF_BOUNDS,
                        "real-mode access outside bootloader memory: " + address
                    );
                }
            }
        }
    }

    private void validateInterruptTarget(int address) throws VirtualHardwareException {
        boolean inBootloader = address >= MemoryMap.BOOTLOADER_START.value()
            && address <= MemoryMap.BOOTLOADER_END.value();
        boolean inFirmware = address >= MemoryMap.FIRMWARE_START.value()
            && address <= MemoryMap.FIRMWARE_END.value();

        if (!inBootloader && !inFirmware) {
            throw new VirtualHardwareException(
                ErrorCode.CPU_MODE_ERROR,
                "interrupt handler is outside bootloader/firmware space"
            );
        }
    }

    private boolean isFirmwareInstruction() {
        if (state.mode() != CPUData.CpuMode.REAL) {
            return false;
        }

        int instructionAddress = pc() - Instruction.BYTE_SIZE;
        return instructionAddress >= MemoryMap.FIRMWARE_START.value()
            && instructionAddress + Instruction.BYTE_SIZE - 1 <= MemoryMap.FIRMWARE_END.value();
    }

    private boolean isFirmwareWriteTarget(int address, int length) {
        int lastAddress = address + length - 1;

        boolean inVectorTable = address >= MemoryMap.VECTOR_TABLE_START.value()
            && lastAddress <= MemoryMap.VECTOR_TABLE_END.value();
        boolean inMmio = address >= MemoryMap.MMIO_START.value()
            && lastAddress <= MemoryMap.MMIO_END.value();

        return inVectorTable || inMmio;
    }

    private int mask32(long value) {
        return (int) (value & CPUData.REG_MASK);
    }

    private int decodeOperand(int[] instruction, int offset) {
        int value = 0;
        for (int i = 0; i < Instruction.OPERAND_SIZE; i++) {
            value |= (instruction[offset + i] & 0xFF) << (8 * i);
        }
        return value;
    }
}
