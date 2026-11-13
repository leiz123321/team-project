package entity;

/**
 * The computer opponent's chosen playing style and its settings.
 */
public final class BotProfile {
    private final String styleName;
    private final double aggression;

    /**
     * Creates a profile.
     *
     * @param styleName the name of the playing style
     * @param aggression how readily the opponent bets and raises, from 0
     *        (never) to 1 (whenever possible)
     * @throws IllegalArgumentException if styleName is null or blank, or
     *         aggression is not between 0 and 1
     */
    public BotProfile(String styleName, double aggression) {
        if (styleName == null || styleName.isBlank()) {
            throw new IllegalArgumentException("styleName must not be blank");
        }
        if (aggression < 0.0 || aggression > 1.0) {
            throw new IllegalArgumentException("aggression must be between 0 and 1");
        }
        this.styleName = styleName;
        this.aggression = aggression;
    }

    public String getStyleName() {
        return this.styleName;
    }

    public double getAggression() {
        return this.aggression;
    }
}
