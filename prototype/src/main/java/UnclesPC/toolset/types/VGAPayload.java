package UnclesPC.toolset.types;

public record VGAPayload(
    int mode,
    int x,
    int y,
    int data
) {
    public static final int COLOR_MODE = 0x00;
    public static final int CHARACTER_MODE = 0x01;

    public boolean isColorMode() {
        return mode == COLOR_MODE;
    }

    public boolean isCharacterMode() {
        return mode == CHARACTER_MODE;
    }

    public int color() {
        return data;
    }

    public char character() {
        return (char) (data & 0xFFFF);
    }
}
