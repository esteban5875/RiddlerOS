package myibmgame.hardware.motherboard;

import myibmgame.hardware.cpu.CPU;

public final class Bios {
    private final CPU cpu;

    public Bios(CPU cpu) {
        this.cpu = cpu;
    }

    public CPU cpu() {
        return cpu;
    }
}
