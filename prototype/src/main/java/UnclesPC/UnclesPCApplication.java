package UnclesPC;

import UnclesPC.hardware.cpu.CPU;

public final class UnclesPCApplication {
    private UnclesPCApplication() {
    }

    public static void main(String[] args) {
        CPU cpu = new CPU();
        cpu.reset();

        //TODO: Code for hardware and .rasm instructions implementations.

        System.out.println("'Uncle's PC' Java prototype ready.");
        System.out.println("CPU snapshot after reset: " + cpu.snapshot());
    }
}
