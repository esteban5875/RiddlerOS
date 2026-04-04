package UnclesPC.toolset.assembler;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import UnclesPC.toolset.assembler.parser.Parser;

public final class Assembler {
    private Assembler() {
    }

    public static void toBin(Path sourcePath, Path binPath) throws IOException {
        byte[] machineCode = new Parser(Files.readString(sourcePath)).assemble();

        Path parent = binPath.getParent();
        if (parent != null) {
            Files.createDirectories(parent);
        }

        Files.write(binPath, machineCode);
    }

    public static byte[] toBytes(Path binPath) throws IOException {
        return Files.readAllBytes(binPath);
    }
}
