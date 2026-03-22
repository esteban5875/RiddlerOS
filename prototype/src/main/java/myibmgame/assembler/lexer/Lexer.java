package myibmgame.assembler.lexer;

import java.util.ArrayList;
import java.util.List;

public final class Lexer {
    private final String sourceCode;
    private final List<String> tokens;

    public Lexer(String sourceCode) {
        this.sourceCode = sourceCode;
        this.tokens = new ArrayList<>();
    }

    public List<String> tokenize() {
        tokens.clear();

        for (String line : splitLines()) {
            List<String> lineTokens = tokenizeLine(line);
            if (!lineTokens.isEmpty()) {
                tokens.addAll(lineTokens);
            }
            tokens.add("\n");
        }

        return List.copyOf(tokens);
    }

    private List<String> splitLines() {
        return sourceCode.lines().toList();
    }

    private String stripComment(String line) {
        boolean inString = false;
        StringBuilder result = new StringBuilder();

        for (int i = 0; i < line.length(); i++) {
            char character = line.charAt(i);
            if (character == '"') {
                inString = !inString;
                result.append(character);
            } else if (character == ';' && !inString) {
                break;
            } else {
                result.append(character);
            }
        }

        return result.toString();
    }

    private List<String> tokenizeLine(String line) {
        String stripped = stripComment(line);
        List<String> result = new ArrayList<>();
        boolean inString = false;
        StringBuilder current = new StringBuilder();

        for (int i = 0; i < stripped.length(); i++) {
            char character = stripped.charAt(i);

            if (character == '"') {
                if (!inString) {
                    if (current.length() > 0) {
                        result.add(current.toString());
                        current.setLength(0);
                    }
                    inString = true;
                    current.append('"');
                } else {
                    current.append('"');
                    result.add(current.toString());
                    current.setLength(0);
                    inString = false;
                }
                continue;
            }

            if (inString) {
                current.append(character);
                continue;
            }

            if (Character.isWhitespace(character)) {
                if (current.length() > 0) {
                    result.add(current.toString());
                    current.setLength(0);
                }
                continue;
            }

            if (character == ',' || character == '[' || character == ']' || character == ':') {
                if (current.length() > 0) {
                    result.add(current.toString());
                    current.setLength(0);
                }
                result.add(String.valueOf(character));
                continue;
            }

            current.append(character);
        }

        if (inString) {
            throw new IllegalArgumentException("Unterminated string literal");
        }

        if (current.length() > 0) {
            result.add(current.toString());
        }

        return result;
    }
}
