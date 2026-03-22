package myibmgame.assembler.parser;

import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import myibmgame.assembler.lexer.Lexer;
import myibmgame.hardware.cpu.modules.CPUData;
import myibmgame.hardware.cpu.modules.InstSet;

public final class Parser {
    private static final Map<String, Integer> GENERAL_REGISTERS = buildGeneralRegisters();
    private static final Map<String, Integer> INST_BYTES = InstSet.allOpcodes();
    private static final Map<String, Integer> INSTRUCTION_ARITY = buildInstructionArity();

    private final List<String> tokens;
    private final List<Statement> statements;
    private final Map<String, Integer> labels;
    private final Map<String, Integer> constants;

    public Parser(String sourceCode) {
        this.tokens = new Lexer(sourceCode).tokenize();
        this.statements = new ArrayList<>();
        this.labels = new LinkedHashMap<>();
        this.constants = new LinkedHashMap<>();
    }

    public byte[] assemble() {
        parseStatements();
        return mapToBytes();
    }

    private List<LineTokens> splitLines() {
        List<LineTokens> lines = new ArrayList<>();
        List<String> current = new ArrayList<>();
        int lineNumber = 1;

        for (String token : tokens) {
            if ("\n".equals(token)) {
                lines.add(new LineTokens(lineNumber, List.copyOf(current)));
                current = new ArrayList<>();
                lineNumber += 1;
            } else {
                current.add(token);
            }
        }

        if (!current.isEmpty()) {
            lines.add(new LineTokens(lineNumber, List.copyOf(current)));
        }

        return lines;
    }

    private void parseStatements() {
        statements.clear();
        labels.clear();
        constants.clear();

        for (LineTokens line : splitLines()) {
            statements.addAll(parseLine(line.tokens(), line.line()));
        }

        runFirstPass();
    }

    private List<Statement> parseLine(List<String> lineTokens, int line) {
        List<Statement> parsedStatements = new ArrayList<>();
        if (lineTokens.isEmpty()) {
            return parsedStatements;
        }

        int index = 0;
        if (lineTokens.size() >= 2 && ":".equals(lineTokens.get(1))) {
            String label = normalizeSymbol(lineTokens.get(0));
            parsedStatements.add(new Statement(Statement.Kind.LABEL, line, null, List.of(), label));
            index = 2;
            if (index >= lineTokens.size()) {
                return parsedStatements;
            }
        }

        if (index + 1 < lineTokens.size() && "EQU".equalsIgnoreCase(lineTokens.get(index + 1))) {
            String name = normalizeSymbol(lineTokens.get(index));
            List<Operand> operands = parseOperands(lineTokens.subList(index + 2, lineTokens.size()), line);
            if (operands.size() != 1) {
                throw lineError(line, "EQU requires exactly one value");
            }

            parsedStatements.add(
                new Statement(
                    Statement.Kind.DIRECTIVE,
                    line,
                    "EQU",
                    List.of(new Operand(Operand.Kind.SYMBOL, name), operands.get(0)),
                    null
                )
            );
            return parsedStatements;
        }

        String mnemonic = lineTokens.get(index).toUpperCase(Locale.ROOT);
        List<String> rawOperands = index + 1 < lineTokens.size()
            ? lineTokens.subList(index + 1, lineTokens.size())
            : List.of();

        if (InstSet.ASSEMBLER_DIRECTIVES.contains(mnemonic)) {
            parsedStatements.add(
                new Statement(
                    Statement.Kind.DIRECTIVE,
                    line,
                    mnemonic,
                    parseOperands(rawOperands, line),
                    null
                )
            );
            return parsedStatements;
        }

        if (INST_BYTES.containsKey(mnemonic)) {
            List<Operand> operands = parseOperands(rawOperands, line);
            Integer expected = INSTRUCTION_ARITY.get(mnemonic);
            if (expected != null && operands.size() != expected) {
                throw lineError(line, mnemonic + " expects " + expected + " operands, got " + operands.size());
            }

            parsedStatements.add(
                new Statement(Statement.Kind.INSTRUCTION, line, mnemonic, operands, null)
            );
            return parsedStatements;
        }

        throw lineError(line, "unknown mnemonic/directive: " + lineTokens.get(index));
    }

    private List<Operand> parseOperands(List<String> operandTokens, int line) {
        if (operandTokens.isEmpty()) {
            return List.of();
        }

        List<List<String>> groups = splitOperands(operandTokens, line);
        List<Operand> operands = new ArrayList<>(groups.size());
        for (List<String> group : groups) {
            operands.add(parseOperandTokens(group, line));
        }
        return operands;
    }

    private List<List<String>> splitOperands(List<String> operandTokens, int line) {
        List<List<String>> chunks = new ArrayList<>();
        List<String> current = new ArrayList<>();
        int bracketDepth = 0;

        for (String token : operandTokens) {
            if ("[".equals(token)) {
                bracketDepth += 1;
                current.add(token);
                continue;
            }
            if ("]".equals(token)) {
                bracketDepth -= 1;
                if (bracketDepth < 0) {
                    throw lineError(line, "unexpected ']'");
                }
                current.add(token);
                continue;
            }
            if (",".equals(token) && bracketDepth == 0) {
                if (current.isEmpty()) {
                    throw lineError(line, "empty operand before ','");
                }
                chunks.add(List.copyOf(current));
                current = new ArrayList<>();
                continue;
            }
            current.add(token);
        }

        if (bracketDepth != 0) {
            throw lineError(line, "unclosed '[' in operand");
        }
        if (current.isEmpty()) {
            throw lineError(line, "trailing ',' without operand");
        }

        chunks.add(List.copyOf(current));
        return chunks;
    }

    private Operand parseOperandTokens(List<String> tokenGroup, int line) {
        if (tokenGroup.size() == 1) {
            String token = tokenGroup.get(0);
            Integer register = GENERAL_REGISTERS.get(token.toUpperCase(Locale.ROOT));
            if (register != null) {
                return new Operand(Operand.Kind.REG, register);
            }
            if (isNumber(token)) {
                return new Operand(Operand.Kind.IMM, parseNumber(token, line));
            }
            if (isStringLiteral(token)) {
                return new Operand(Operand.Kind.STRING, stripStringQuotes(token));
            }
            return new Operand(Operand.Kind.SYMBOL, normalizeSymbol(token));
        }

        if (tokenGroup.size() == 3 && "[".equals(tokenGroup.get(0)) && "]".equals(tokenGroup.get(2))) {
            String inner = tokenGroup.get(1);
            if (isNumber(inner)) {
                return new Operand(Operand.Kind.MEM, parseNumber(inner, line));
            }
            if (GENERAL_REGISTERS.containsKey(inner.toUpperCase(Locale.ROOT))) {
                throw lineError(line, "register-indirect memory is not implemented in this skeleton");
            }
            return new Operand(Operand.Kind.MEM, normalizeSymbol(inner));
        }

        throw lineError(line, "unsupported operand syntax: " + String.join(" ", tokenGroup));
    }

    private int statementSize(Statement statement) {
        if (statement.kind() == Statement.Kind.LABEL) {
            return 0;
        }

        if (statement.kind() == Statement.Kind.INSTRUCTION) {
            return 4;
        }

        if (statement.kind() != Statement.Kind.DIRECTIVE || statement.mnemonic() == null) {
            return 0;
        }

        String directive = statement.mnemonic();
        List<Operand> operands = statement.operands();

        return switch (directive) {
            case "ORG", "EQU" -> 0;
            case "INCLUDE" -> throw lineError(statement.line(), "INCLUDE is not implemented in this parser skeleton");
            case "DB" -> {
                int size = 0;
                for (Operand operand : operands) {
                    size += operand.kind() == Operand.Kind.STRING
                        ? operand.value().toString().length()
                        : 1;
                }
                yield size;
            }
            case "DW" -> 2 * operands.size();
            case "DS" -> {
                if (operands.size() != 1) {
                    throw lineError(statement.line(), "DS expects exactly one operand");
                }
                int count = resolveValue(operands.get(0), statement.line());
                if (count < 0) {
                    throw lineError(statement.line(), "DS size cannot be negative");
                }
                yield count;
            }
            default -> throw lineError(statement.line(), "unsupported directive: " + directive);
        };
    }

    private int resolveValue(Operand operand, int line) {
        if (operand.kind() == Operand.Kind.IMM || operand.kind() == Operand.Kind.REG) {
            return (Integer) operand.value();
        }

        if ((operand.kind() == Operand.Kind.SYMBOL || operand.kind() == Operand.Kind.MEM)
            && operand.value() instanceof String symbol) {
            String key = normalizeSymbol(symbol);
            if (constants.containsKey(key)) {
                return constants.get(key);
            }
            if (labels.containsKey(key)) {
                return labels.get(key);
            }
            throw lineError(line, "undefined symbol: " + symbol);
        }

        if (operand.kind() == Operand.Kind.MEM && operand.value() instanceof Integer value) {
            return value;
        }

        throw lineError(line, "cannot resolve operand " + operand.kind());
    }

    private int resolveByte(Operand operand, int line, String context) {
        int value = resolveValue(operand, line);
        if (value < 0 || value > 0xFF) {
            throw lineError(line, context + " must fit in one byte (0..255), got " + value);
        }
        return value;
    }

    private void runFirstPass() {
        int locationCounter = 0;

        for (Statement statement : statements) {
            if (statement.kind() == Statement.Kind.LABEL) {
                String name = statement.name();
                if (labels.containsKey(name) || constants.containsKey(name)) {
                    throw lineError(statement.line(), "duplicate symbol: " + name);
                }
                labels.put(name, locationCounter);
                continue;
            }

            if (statement.kind() == Statement.Kind.DIRECTIVE && "EQU".equals(statement.mnemonic())) {
                Operand symbol = statement.operands().get(0);
                Operand valueOperand = statement.operands().get(1);
                if (symbol.kind() != Operand.Kind.SYMBOL) {
                    throw lineError(statement.line(), "EQU first argument must be a symbol");
                }

                String key = normalizeSymbol(symbol.value().toString());
                if (labels.containsKey(key) || constants.containsKey(key)) {
                    throw lineError(statement.line(), "duplicate symbol: " + key);
                }
                if ((valueOperand.kind() == Operand.Kind.SYMBOL || valueOperand.kind() == Operand.Kind.MEM)
                    && valueOperand.value() instanceof String) {
                    throw lineError(statement.line(), "EQU value must be absolute in this skeleton");
                }

                constants.put(key, resolveValue(valueOperand, statement.line()));
                continue;
            }

            if (statement.kind() == Statement.Kind.DIRECTIVE && "ORG".equals(statement.mnemonic())) {
                if (statement.operands().size() != 1) {
                    throw lineError(statement.line(), "ORG expects exactly one operand");
                }
                int origin = resolveValue(statement.operands().get(0), statement.line());
                if (origin < 0) {
                    throw lineError(statement.line(), "ORG cannot set a negative location");
                }
                locationCounter = origin;
                continue;
            }

            locationCounter += statementSize(statement);
        }
    }

    private int[] encodeInstruction(Statement statement) {
        String mnemonic = statement.mnemonic();
        List<Operand> operands = statement.operands();
        int opcode = INST_BYTES.get(mnemonic);

        if (operands.isEmpty()) {
            return new int[] {opcode, CPUData.InstMode.NONE.code(), 0x00, 0x00};
        }

        if (operands.size() == 1) {
            Operand operand = operands.get(0);
            CPUData.InstMode mode;
            int op1;

            if (operand.kind() == Operand.Kind.REG) {
                mode = CPUData.InstMode.REG_REG;
                op1 = resolveByte(operand, statement.line(), mnemonic + " operand");
            } else if (operand.kind() == Operand.Kind.MEM) {
                mode = CPUData.InstMode.REG_MEM;
                op1 = resolveByte(operand, statement.line(), mnemonic + " address");
            } else {
                mode = CPUData.InstMode.REG_IMM;
                op1 = resolveByte(operand, statement.line(), mnemonic + " operand");
            }

            return new int[] {opcode, mode.code(), op1, 0x00};
        }

        if (operands.size() == 2) {
            Operand destination = operands.get(0);
            Operand source = operands.get(1);

            if (destination.kind() != Operand.Kind.REG) {
                throw lineError(statement.line(), mnemonic + " destination must be a register");
            }

            int op1 = resolveByte(destination, statement.line(), mnemonic + " destination");
            CPUData.InstMode mode = switch (source.kind()) {
                case REG -> CPUData.InstMode.REG_REG;
                case MEM -> CPUData.InstMode.REG_MEM;
                default -> CPUData.InstMode.REG_IMM;
            };
            int op2 = resolveByte(source, statement.line(), mnemonic + " source");
            return new int[] {opcode, mode.code(), op1, op2};
        }

        throw lineError(statement.line(), mnemonic + " operand count is unsupported");
    }

    private byte[] emitDirectiveBytes(Statement statement) {
        String mnemonic = statement.mnemonic();
        List<Operand> operands = statement.operands();
        ByteArrayOutputStream output = new ByteArrayOutputStream();

        switch (mnemonic) {
            case "ORG" -> throw lineError(statement.line(), "ORG is handled by the assembly address counter");
            case "EQU" -> {
                return output.toByteArray();
            }
            case "DB" -> {
                for (Operand operand : operands) {
                    if (operand.kind() == Operand.Kind.STRING) {
                        output.writeBytes(operand.value().toString().getBytes(StandardCharsets.US_ASCII));
                    } else {
                        output.write(resolveByte(operand, statement.line(), "DB value"));
                    }
                }
                return output.toByteArray();
            }
            case "DW" -> {
                for (Operand operand : operands) {
                    int value = resolveValue(operand, statement.line());
                    if (value < 0 || value > 0xFFFF) {
                        throw lineError(statement.line(), "DW value must fit in 16 bits, got " + value);
                    }
                    output.write(value & 0xFF);
                    output.write((value >>> 8) & 0xFF);
                }
                return output.toByteArray();
            }
            case "DS" -> {
                if (operands.size() != 1) {
                    throw lineError(statement.line(), "DS expects exactly one operand");
                }
                int size = resolveValue(operands.get(0), statement.line());
                if (size < 0) {
                    throw lineError(statement.line(), "DS size cannot be negative");
                }
                output.writeBytes(new byte[size]);
                return output.toByteArray();
            }
            case "INCLUDE" -> throw lineError(statement.line(), "INCLUDE is not implemented in this parser skeleton");
            default -> throw lineError(statement.line(), "unsupported directive: " + mnemonic);
        }
    }

    private byte[] mapToBytes() {
        if (statements.isEmpty()) {
            parseStatements();
        }

        ByteArrayOutputStream output = new ByteArrayOutputStream();
        int currentAddress = 0;
        Integer imageBase = null;

        for (Statement statement : statements) {
            if (statement.kind() == Statement.Kind.LABEL) {
                continue;
            }

            if (statement.kind() == Statement.Kind.DIRECTIVE) {
                if ("ORG".equals(statement.mnemonic())) {
                    if (statement.operands().size() != 1) {
                        throw lineError(statement.line(), "ORG expects exactly one operand");
                    }
                    int target = resolveValue(statement.operands().get(0), statement.line());
                    if (target < 0) {
                        throw lineError(statement.line(), "ORG cannot set a negative location");
                    }
                    currentAddress = target;
                    if (imageBase == null) {
                        imageBase = target;
                    }
                    continue;
                }

                byte[] chunk = emitDirectiveBytes(statement);
                if (chunk.length == 0) {
                    continue;
                }

                if (imageBase == null) {
                    imageBase = currentAddress;
                }

                int offset = currentAddress - imageBase;
                if (offset < output.size()) {
                    throw lineError(statement.line(), "output overlap at address " + String.format("0x%x", currentAddress));
                }
                if (offset > output.size()) {
                    output.writeBytes(new byte[offset - output.size()]);
                }

                output.writeBytes(chunk);
                currentAddress += chunk.length;
                continue;
            }

            if (statement.kind() == Statement.Kind.INSTRUCTION) {
                int[] encoded = encodeInstruction(statement);
                byte[] chunk = new byte[] {
                    (byte) encoded[0],
                    (byte) encoded[1],
                    (byte) encoded[2],
                    (byte) encoded[3]
                };

                if (imageBase == null) {
                    imageBase = currentAddress;
                }

                int offset = currentAddress - imageBase;
                if (offset < output.size()) {
                    throw lineError(statement.line(), "output overlap at address " + String.format("0x%x", currentAddress));
                }
                if (offset > output.size()) {
                    output.writeBytes(new byte[offset - output.size()]);
                }

                output.writeBytes(chunk);
                currentAddress += chunk.length;
                continue;
            }

            throw lineError(statement.line(), "unknown statement kind: " + statement.kind());
        }

        return output.toByteArray();
    }

    private boolean isNumber(String token) {
        try {
            Integer.decode(token);
            return true;
        } catch (NumberFormatException exception) {
            return false;
        }
    }

    private int parseNumber(String token, int line) {
        try {
            return Integer.decode(token);
        } catch (NumberFormatException exception) {
            throw lineError(line, "invalid numeric literal: " + token);
        }
    }

    private boolean isStringLiteral(String token) {
        return token.length() >= 2 && token.startsWith("\"") && token.endsWith("\"");
    }

    private String stripStringQuotes(String token) {
        return token.substring(1, token.length() - 1);
    }

    private String normalizeSymbol(String symbol) {
        return symbol.toUpperCase(Locale.ROOT);
    }

    private IllegalArgumentException lineError(int line, String message) {
        return new IllegalArgumentException("Line " + line + ": " + message);
    }

    private static Map<String, Integer> buildGeneralRegisters() {
        Map<String, Integer> registers = new LinkedHashMap<>();
        for (int i = 0; i < 16; i++) {
            registers.put("R" + i, i);
        }
        return Map.copyOf(registers);
    }

    private static Map<String, Integer> buildInstructionArity() {
        Map<String, Integer> arity = new LinkedHashMap<>();
        arity.put("NOP", 0);
        arity.put("HALT", 0);
        arity.put("CLACC", 0);
        arity.put("RET", 0);
        arity.put("PUSHY", 0);
        arity.put("POPY", 0);
        arity.put("INT", 0);
        arity.put("INC", 1);
        arity.put("DEC", 1);
        arity.put("NOT", 1);
        arity.put("LDACC", 1);
        arity.put("LDX", 1);
        arity.put("LDY", 1);
        arity.put("STX", 1);
        arity.put("JMP", 1);
        arity.put("JZ", 1);
        arity.put("JNZ", 1);
        arity.put("JC", 1);
        arity.put("JNC", 1);
        arity.put("JGT", 1);
        arity.put("JLT", 1);
        arity.put("CALL", 1);
        arity.put("ADD", 2);
        arity.put("SUB", 2);
        arity.put("MUL", 2);
        arity.put("DIV", 2);
        arity.put("AND", 2);
        arity.put("OR", 2);
        arity.put("XOR", 2);
        arity.put("CMP", 2);
        arity.put("MOV", 2);
        return Map.copyOf(arity);
    }

    private record LineTokens(int line, List<String> tokens) {
    }
}
