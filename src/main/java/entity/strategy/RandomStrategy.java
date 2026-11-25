package entity.strategy;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import entity.Action;
import entity.ActionType;
import entity.BotStrategy;
import entity.DecisionContext;

/**
 * A built-in opponent that picks among the legal actions at random. When it
 * bets or raises, it uses the smallest amount allowed. It does not fold when
 * it may check: giving up a pot that costs nothing to stay in is not a way of
 * playing, however unpredictable.
 */
public class RandomStrategy implements BotStrategy {
    private final Random random;

    /**
     * Creates the strategy.
     *
     * @param random the source of randomness; pass a seeded one in tests
     * @throws IllegalArgumentException if random is null
     */
    public RandomStrategy(Random random) {
        if (random == null) {
            throw new IllegalArgumentException("random must not be null");
        }
        this.random = random;
    }

    @Override
    public Action decide(DecisionContext context) {
        List<ActionType> choices = new ArrayList<>(context.getLegalActionTypes());
        if (context.isLegal(ActionType.CHECK)) {
            choices.remove(ActionType.FOLD);
        }
        ActionType type = choices.get(this.random.nextInt(choices.size()));
        return Action.of(type, context.getMinWager());
    }
}
