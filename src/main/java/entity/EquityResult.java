package entity;

/**
 * The chances of winning, tying, and losing a hand, each between 0 and 1.
 */
public final class EquityResult {
    private final double win;
    private final double tie;
    private final double lose;

    /**
     * Creates a result.
     *
     * @param win the chance of winning
     * @param tie the chance of tying
     * @param lose the chance of losing
     * @throws IllegalArgumentException if a chance is not between 0 and 1
     */
    public EquityResult(double win, double tie, double lose) {
        if (!isChance(win) || !isChance(tie) || !isChance(lose)) {
            throw new IllegalArgumentException("each chance must be between 0 and 1");
        }
        this.win = win;
        this.tie = tie;
        this.lose = lose;
    }

    private static boolean isChance(double value) {
        return value >= 0.0 && value <= 1.0;
    }

    public double getWin() {
        return this.win;
    }

    public double getTie() {
        return this.tie;
    }

    public double getLose() {
        return this.lose;
    }

    /**
     * Returns the share of the pot this player expects: the chance of winning
     * plus half the chance of tying.
     */
    public double getEquity() {
        return this.win + this.tie / 2;
    }

    @Override
    public String toString() {
        return String.format("win %.1f%%, tie %.1f%%, lose %.1f%%",
                this.win * 100, this.tie * 100, this.lose * 100);
    }
}
