package fr.dinar.client;

public final class MoneyGainEvent {
    private static final long DURATION_MS = 2800;
    private static final long FADE_IN_MS = 130;
    private static final long FADE_OUT_START_MS = DURATION_MS - 500;
    private static final long SLIDE_MS = 550;

    public final double amount;
    public final String label;
    public final long startMs;

    public MoneyGainEvent(double amount, String label) {
        this.amount = amount;
        this.label = label;
        this.startMs = System.currentTimeMillis();
    }

    public long elapsed() {
        return System.currentTimeMillis() - startMs;
    }

    public boolean finished() {
        return elapsed() >= DURATION_MS;
    }

    public float alpha() {
        long e = elapsed();
        if (e < FADE_IN_MS) return e / (float) FADE_IN_MS;
        if (e > FADE_OUT_START_MS) {
            return Math.max(0f, 1f - (e - FADE_OUT_START_MS) / (float) (DURATION_MS - FADE_OUT_START_MS));
        }
        return 1f;
    }

    public float slide() {
        long e = elapsed();
        if (e >= SLIDE_MS) return 1f;
        return e / (float) SLIDE_MS;
    }
}
