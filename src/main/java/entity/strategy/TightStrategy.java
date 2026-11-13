package entity.strategy;

import entity.Action;
import entity.BotStrategy;
import entity.DecisionContext;

/**
 * A built-in opponent that plays few hands: it folds to a bet unless its
 * chance of winning is good, and only puts in extra chips with a strong hand.
 */
public class TightStrategy implements BotStrategy {
    private static final double CALL_THRESHOLD = 0.55;
    private static final double BET_THRESHOLD = 0.75;
    private static final double RAISE_THRESHOLD = 0.85;

    @Override
    public Action decide(DecisionContext context) {
        double winChance = context.getWinChance();
        Action action;
        if (context.getAmountToCall() == 0) {
            if (winChance >= BET_THRESHOLD && context.canWager()) {
                action = context.wager(context.getMinWager());
            }
            else {
                action = Action.check();
            }
        }
        else if (winChance >= RAISE_THRESHOLD && context.canWager()) {
            action = context.wager(context.getMinWager());
        }
        else if (winChance >= CALL_THRESHOLD) {
            action = Action.call();
        }
        else {
            action = Action.fold();
        }
        return action;
    }
}
