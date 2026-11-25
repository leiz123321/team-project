package entity.strategy;

import entity.Action;
import entity.BotStrategy;
import entity.DecisionContext;

/**
 * A built-in opponent that keeps the pressure on: the reasoning of
 * {@link Playbook}, with numbers that give up few hands and bet and raise
 * many, for large amounts.
 *
 * <p>At the neutral aggression of 0.5:
 * <ul>
 *   <li>facing a bet it folds 1.1 times the share of hands the bet needs to
 *       fold: on the river the worst 0.37 of them against a bet of half the
 *       pot and the worst 0.55 against a bet of the pot, on earlier streets
 *       fewer;</li>
 *   <li>it raises the best 0.26 of the hands it goes on with, and as a bluff
 *       some of the hands just short of a call;</li>
 *   <li>when nobody has bet it bets the best 0.38 of all hands for value and
 *       the worst 0.11 as a bluff: half of its hands;</li>
 *   <li>its bets are 0.85 of the pot.</li>
 * </ul>
 * The aggression moves these numbers as {@link Playbook} describes.
 */
public class AggressiveStrategy implements BotStrategy {
    private static final double TIGHTNESS = 1.1;
    private static final double RAISES = 0.26;
    private static final double VALUES = 0.38;
    private static final double BLUFFS = 0.65;
    private static final double SIZE = 0.85;

    private final Playbook playbook;

    /**
     * Creates the strategy with the neutral aggression of 0.5.
     */
    public AggressiveStrategy() {
        this(Aggression.NEUTRAL);
    }

    /**
     * Creates the strategy.
     *
     * @param aggression how readily it bets and raises, from 0 to 1
     * @throws IllegalArgumentException if aggression is not between 0 and 1
     */
    public AggressiveStrategy(double aggression) {
        this.playbook = new Playbook(TIGHTNESS, RAISES, VALUES, BLUFFS, SIZE, new Aggression(aggression));
    }

    @Override
    public Action decide(DecisionContext context) {
        return this.playbook.decide(context);
    }
}
