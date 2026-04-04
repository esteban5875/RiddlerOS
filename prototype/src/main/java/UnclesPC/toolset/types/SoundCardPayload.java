package UnclesPC.toolset.types;

public record SoundCardPayload(
    int frequency,
    int durationMs,
    int volume
) {
    public boolean isMuted() {
        return volume == 0;
    }
}
