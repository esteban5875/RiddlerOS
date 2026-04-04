package UnclesPC.virtual_hardware.cpu.modules;

import java.util.Map;

public final class CPUState {
    private int acc;
    private int sp;
    private int pc;
    private CPUData.CpuMode mode;
    private final Map<String, Integer> flags;
    private int fpu;
    private int x;
    private int y;
    private final Map<Integer, Integer> generalRegs;

    public CPUState() {
        this.acc = 0x00;
        this.sp = 0x00;
        this.pc = 0x00;
        this.mode = CPUData.CpuMode.REAL;
        this.flags = CPUData.defaultFlags();
        this.fpu = 0x00;
        this.x = 0x00;
        this.y = 0x00;
        this.generalRegs = RegSet.defaultGeneralRegisters();
    }

    public int acc() {
        return acc;
    }

    public void setAcc(int acc) {
        this.acc = acc;
    }

    public int sp() {
        return sp;
    }

    public void setSp(int sp) {
        this.sp = sp;
    }

    public int pc() {
        return pc;
    }

    public void setPc(int pc) {
        this.pc = pc;
    }

    public CPUData.CpuMode mode() {
        return mode;
    }

    public void setMode(CPUData.CpuMode mode) {
        this.mode = mode;
    }

    public Map<String, Integer> flags() {
        return flags;
    }

    public int fpu() {
        return fpu;
    }

    public void setFpu(int fpu) {
        this.fpu = fpu;
    }

    public int x() {
        return x;
    }

    public void setX(int x) {
        this.x = x;
    }

    public int y() {
        return y;
    }

    public void setY(int y) {
        this.y = y;
    }

    public Map<Integer, Integer> generalRegs() {
        return generalRegs;
    }
}
