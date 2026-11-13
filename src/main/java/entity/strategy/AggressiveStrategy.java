package entity.strategy;

import entity.Action;
import entity.BotStrategy;
import entity.DecisionContext;

/**
 * A built-in opponent that bets and raises often: with a fair chance of
 * winning it bets or raises to about the size of the pot, and it calls with
 * hands a tighter player would fold.
 */
public class AggressiveStrategy implements BotStrategy {
    private static final double WAGER_THRESHOLD = 0.45;
    private static final double CALL_THRESHOLD = 0.3;

    @Override
    public Action decide(DecisionContext context) {
        double winChance = context.getWinChance();
        Action action;
        if (winChance >= WAGER_THRESHOLD && context.canWager()) {
            int potSized = Math.max(context.getMinWager(), context.getPot());
            action = context.wager(Math.min(potSized, context.getMaxWager()));
        }
        else if (context.getAmountToCall() == 0) {
            action = Action.check();
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
