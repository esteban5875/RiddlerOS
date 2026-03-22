package myibmgame;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import myibmgame.assembler.parser.Parser;
import myibmgame.hardware.cpu.CPU;

public final class MyIBMGameApplication {
    private MyIBMGameApplication() {
    }

    public static void main(String[] args) throws IOException {
        CPU cpu = new CPU();
        cpu.reset();

        if (args.length > 0) {
            Path sourcePath = Path.of(args[0]);
            String source = Files.readString(sourcePath);
            Parser parser = new Parser(source);
            byte[] image = parser.assemble();
            System.out.printf("Assembled %d bytes from %s%n", image.length, sourcePath);
            System.out.println("CPU snapshot after reset: " + cpu.snapshot());
            return;
        }

        System.out.println("MyIBM-Game Java prototype ready.");
        System.out.println("Pass a .rasm file path to assemble it.");
        System.out.println("CPU snapshot after reset: " + cpu.snapshot());
    }
}
