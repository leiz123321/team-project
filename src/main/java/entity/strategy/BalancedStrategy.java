package entity.strategy;

import entity.Action;
import entity.BotStrategy;
import entity.DecisionContext;

/**
 * A built-in opponent that plays by the book: the reasoning of
 * {@link Playbook}, with numbers close to what the theory asks for.
 *
 * <p>At the neutral aggression of 0.5:
 * <ul>
 *   <li>facing a bet it folds 1.2 times the share of hands the bet needs to
 *       fold: on the river the worst 0.4 of them against a bet of half the
 *       pot and the worst 0.6 against a bet of the pot, on earlier streets
 *       fewer;</li>
 *   <li>it raises the best 0.22 of the hands it goes on with, and as a bluff
 *       some of the hands just short of a call;</li>
 *   <li>when nobody has bet it bets the best 0.28 of all hands for value and
 *       the worst 0.06 as a bluff, half as many as the theory allows, and
 *       checks the rest;</li>
 *   <li>its bets are two thirds of the pot.</li>
 * </ul>
 * The aggression moves these numbers as {@link Playbook} describes.
 */
public class BalancedStrategy implements BotStrategy {
    private static final double TIGHTNESS = 1.2;
    private static final double RAISES = 0.22;
    private static final double VALUES = 0.28;
    private static final double BLUFFS = 0.5;
    private static final double SIZE = 0.66;

    private final Playbook playbook;

    /**
     * Creates the strategy with the neutral aggression of 0.5.
     */
    public BalancedStrategy() {
        this(Aggression.NEUTRAL);
    }

    /**
     * Creates the strategy.
     *
     * @param aggression how readily it bets and raises, from 0 to 1
     * @throws IllegalArgumentException if aggression is not between 0 and 1
     */
    public BalancedStrategy(double aggression) {
        this.playbook = new Playbook(TIGHTNESS, RAISES, VALUES, BLUFFS, SIZE, new Aggression(aggression));
    }

    @Override
    public Action decide(DecisionContext context) {
        return this.playbook.decide(context);
    }
}
