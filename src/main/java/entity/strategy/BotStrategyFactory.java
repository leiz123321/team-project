package entity.strategy;

import java.util.List;
import java.util.Random;

import entity.BotProfile;
import entity.BotStrategy;

/**
 * Turns a {@link BotProfile} into the built-in strategy it describes. This is
 * the one place that knows which playing styles exist and which class plays
 * each of them.
 */
public final class BotStrategyFactory {

    /** The style that plays few hands. */
    public static final String STYLE_TIGHT = "Tight";

    /** The style that bets and raises often. */
    public static final String STYLE_AGGRESSIVE = "Aggressive";

    /** The style that plays by the book. It is used when a style is not known. */
    public static final String STYLE_BALANCED = "Balanced";

    /** The style that picks among the legal actions at random. */
    public static final String STYLE_RANDOM = "Random";

    private BotStrategyFactory() {
    }

    /**
     * Returns the names of the playing styles that exist, in the order they
     * should be offered to a person.
     *
     * @return a list that cannot be changed
     */
    public static List<String> styleNames() {
        return List.of(STYLE_TIGHT, STYLE_AGGRESSIVE, STYLE_BALANCED, STYLE_RANDOM);
    }

    /**
     * Creates the built-in strategy for a profile. The profile's aggression is
     * passed on to the strategy; the random style ignores it. A style name is
     * matched without regard to upper and lower case, and a name that matches
     * no style gives the balanced strategy.
     *
     * @param profile the chosen style and its aggression
     * @param random the source of randomness for the random style; pass a
     *        seeded one in tests
     * @return the strategy
     * @throws IllegalArgumentException if profile or random is null
     */
    public static BotStrategy create(BotProfile profile, Random random) {
        if (profile == null || random == null) {
            throw new IllegalArgumentException("profile and random must not be null");
        }
        String styleName = profile.getStyleName();
        double aggression = profile.getAggression();
        BotStrategy strategy;
        if (STYLE_TIGHT.equalsIgnoreCase(styleName)) {
            strategy = new TightStrategy(aggression);
        }
        else if (STYLE_AGGRESSIVE.equalsIgnoreCase(styleName)) {
            strategy = new AggressiveStrategy(aggression);
        }
        else if (STYLE_RANDOM.equalsIgnoreCase(styleName)) {
            strategy = new RandomStrategy(random);
        }
        else {
            strategy = new BalancedStrategy(aggression);
        }
        return strategy;
    }
}
