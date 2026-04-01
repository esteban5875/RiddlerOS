package UnclesPC.hardware.ram;

import UnclesPC.exception.UnclesPCException;
import UnclesPC.hardware.motherboard.error.ErrorCode;
import UnclesPC.hardware.ram.modules.MemoryMap;

public final class Memory { //TODO: Handle exceptions natively instead of using java exceptions.
    private static final int DEFAULT_SIZE = 500 * 1024 * 1024; // 500 MB
    private static final int STACK_LIMIT = MemoryMap.STACK_TOP.value() + 1;

    private final int size;
    private final byte[] data;
    private int sp = STACK_LIMIT;

    public Memory() {
        this(DEFAULT_SIZE);
    }

    public Memory(int size) {
        if (size < STACK_LIMIT) {
            throw new IllegalArgumentException("memory size must cover the full mapped address space");
        }

        this.size = size;
        this.data = new byte[size];
        this.sp = STACK_LIMIT;
    }

    public int size() {
        return size;
    }

    public int stackPointer() {
        return sp;
    }

    public void setStackPointer(int value) {
        validateStackPointer(value);
        sp = value;
    }

    public void resetStackPointer() {
        sp = STACK_LIMIT;
    }

    public int writeByte(int address, int value) {
        validateRange(address, 1);
        data[address] = (byte) (value & 0xFF);
        return ErrorCode.SUCCESS.code();
    }

    public int readByte(int address) {
        validateRange(address, 1);
        return data[address] & 0xFF;
    }

    public int pushWord(int value) {
        if (sp - 4 < MemoryMap.STACK_BOTTOM.value()) {
            throw new UnclesPCException(ErrorCode.STACK_OVERFLOW, "stack overflow");
        }

        sp -= 4;

        writeWord(sp, value);

        return sp;
    }

    public int popWord() {
        if (sp + 4 > STACK_LIMIT) {
            throw new UnclesPCException(ErrorCode.STACK_UNDERFLOW, "stack underflow");
        }

        int value = readWord(sp);

        sp += 4;

        return value;
    }

    public void setVector(int interruptNumber, int handlerAddress) {
        int base = MemoryMap.VECTOR_TABLE_START.value();
        int address = base + interruptNumber * 4;

        validateVectorAddress(address);
        writeWord(address, handlerAddress);
    }

    public int getVector(int interruptNumber) {
        int base = MemoryMap.VECTOR_TABLE_START.value();
        int address = base + interruptNumber * 4;
        validateVectorAddress(address);
        int value = readWord(address);

        return value;
    }

    public int readWord(int address) {
        validateRange(address, 4);
        int value = 0;

        for (int i = 0; i < 4; i++) {
            value |= (data[address + i] & 0xFF) << (8 * i);
        }

        return value;
    }

    public void writeWord(int address, int value) {
        validateRange(address, 4);
        for (int i = 0; i < 4; i++) {
            data[address + i] = (byte) ((value >>> (8 * i)) & 0xFF);
        }
    }

    private void validateRange(int address, int length) {
        if (address < 0 || address > size - length) {
            throw new UnclesPCException(
                ErrorCode.MEMORY_OUT_OF_BOUNDS,
                "memory address out of bounds: " + address
            );
        }
    }

    private void validateStackPointer(int value) {
        if (value < MemoryMap.STACK_BOTTOM.value() || value > STACK_LIMIT || (STACK_LIMIT - value) % 4 != 0) {
            throw new UnclesPCException(
                ErrorCode.MEMORY_OUT_OF_BOUNDS,
                "invalid stack pointer: " + value
            );
        }
    }

    private void validateVectorAddress(int address) {
        if (address < MemoryMap.VECTOR_TABLE_START.value() || address + 3 > MemoryMap.VECTOR_TABLE_END.value()) {
            throw new UnclesPCException(
                ErrorCode.MEMORY_OUT_OF_BOUNDS,
                "vector address out of bounds: " + address
            );
        }
    }
}
