package UnclesPC.assembler.parser;

import java.util.List;

public record Statement(Kind kind, int line, String mnemonic, List<Operand> operands, String name) {
    public Statement {
        operands = operands == null ? List.of() : List.copyOf(operands);
    }

    public enum Kind {
        LABEL,
        INSTRUCTION,
        DIRECTIVE
    }
}
