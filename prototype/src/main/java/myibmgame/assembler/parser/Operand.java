package myibmgame.assembler.parser;

public record Operand(Kind kind, Object value) {
    public enum Kind {
        REG,
        IMM,
        STRING,
        SYMBOL,
        MEM,
        INVALID
    }
}
