package entity.strategy;

import entity.Action;
import entity.BotStrategy;
import entity.DecisionContext;

/**
 * A built-in opponent that plays few hands and hardly ever bluffs: the
 * reasoning of {@link Playbook}, with numbers that give up more hands than
 * the theory asks for and put chips in only with the good ones.
 *
 * <p>At the neutral aggression of 0.5:
 * <ul>
 *   <li>facing a bet it folds 1.3 times the share of hands the bet needs to
 *       fold: on the river the worst 0.43 of them against a bet of half the
 *       pot and the worst 0.65 against a bet of the pot, on earlier streets
 *       fewer. A small bet still gets a call: what it folds depends on the
 *       price;</li>
 *   <li>it raises the best 0.12 of the hands it goes on with, and hardly
 *       ever as a bluff;</li>
 *   <li>when nobody has bet it bets the best 0.22 of all hands for value and
 *       only the worst 0.02 as a bluff;</li>
 *   <li>its bets are half the pot.</li>
 * </ul>
 * The aggression moves these numbers as {@link Playbook} describes.
 */
public class TightStrategy implements BotStrategy {
    private static final double TIGHTNESS = 1.3;
    private static final double RAISES = 0.12;
    private static final double VALUES = 0.22;
    private static final double BLUFFS = 0.3;
    private static final double SIZE = 0.5;

    private final Playbook playbook;

    /**
     * Creates the strategy with the neutral aggression of 0.5.
     */
    public TightStrategy() {
        this(Aggression.NEUTRAL);
    }

    /**
     * Creates the strategy.
     *
     * @param aggression how readily it bets and raises, from 0 to 1
     * @throws IllegalArgumentException if aggression is not between 0 and 1
     */
    public TightStrategy(double aggression) {
        this.playbook = new Playbook(TIGHTNESS, RAISES, VALUES, BLUFFS, SIZE, new Aggression(aggression));
    }

    @Override
    public Action decide(DecisionContext context) {
        return this.playbook.decide(context);
    }
}
