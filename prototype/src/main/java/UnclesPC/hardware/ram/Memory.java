package UnclesPC.hardware.ram;

import java.util.HashMap;
import java.util.Map;

import UnclesPC.exception.UnclesPCException;
import UnclesPC.hardware.motherboard.error.ErrorCode;
import UnclesPC.hardware.ram.modules.MemoryMap;

public final class Memory {
    private static final int DEFAULT_SIZE = 500 * 1024 * 1024;

    private final int size;
    private final Map<Integer, Integer> data;

    public Memory() {
        this(DEFAULT_SIZE);
    }

    public Memory(int size) {
        this.size = size;
        this.data = new HashMap<>();
    }

    public int size() {
        return size;
    }

    public int pushWord(int value, int sp) {
        if (sp - 4 < MemoryMap.STACK_BOTTOM.value()) {
            throw new UnclesPCException(ErrorCode.STACK_UNDERFLOW, "stack underflow");
        }

        int newSp = sp - 4;
        for (int i = 0; i < 4; i++) {
            write(newSp + i, (value >>> (8 * i)) & 0xFF);
        }
        return newSp;
    }

    public PopResult popWord(int sp) {
        if (sp + 4 > MemoryMap.STACK_TOP.value()) {
            throw new UnclesPCException(ErrorCode.STACK_OVERFLOW, "stack overflow");
        }

        int value = 0;
        for (int i = 0; i < 4; i++) {
            value |= read(sp + i) << (8 * i);
        }
        return new PopResult(value, sp + 4);
    }

    public void setVector(int interruptNumber, int handlerAddress) {
        int base = MemoryMap.VECTOR_TABLE_START.value();
        int address = base + interruptNumber * 4;

        for (int i = 0; i < 4; i++) {
            write(address + i, (handlerAddress >>> (8 * i)) & 0xFF);
        }
    }

    public int getVector(int interruptNumber) {
        int base = MemoryMap.VECTOR_TABLE_START.value();
        int address = base + interruptNumber * 4;
        int value = 0;

        for (int i = 0; i < 4; i++) {
            value |= read(address + i) << (8 * i);
        }

        return value;
    }

    public int read(int address) {
        validateAddress(address);
        return data.getOrDefault(address, 0);
    }

    public void write(int address, int value) {
        validateAddress(address);
        data.put(address, value & 0xFF);
    }

    private void validateAddress(int address) {
        if (address < 0 || address >= size) {
            throw new UnclesPCException(
                ErrorCode.MEMORY_OUT_OF_BOUNDS,
                "memory address out of bounds: " + address
            );
        }
    }

    public record PopResult(int value, int newSp) {
    }
}
