package entity.strategy;

import entity.Action;
import entity.ActionType;
import entity.DecisionContext;

/**
 * How the three thinking styles decide. They share one way of reasoning and
 * differ only in five numbers, which a style hands to its playbook.
 *
 * <p>The reasoning is the textbook one for a bet in poker, not an invention
 * of this program (see for example B. Chen and J. Ankenman, <i>The
 * Mathematics of Poker</i>, 2006, on how often to call and how often to
 * bluff). It rests on two results:
 * <ul>
 *   <li><b>How much to defend.</b> A bluff of {@code b} into a pot of
 *       {@code p} makes money if the other player folds more often than
 *       {@code b / (p + b)}. So a player facing a bet goes on with the best
 *       of their hands and folds no more than that share of the worst.
 *       Before the river a bluff that is called still wins some of the
 *       time, so it loses less than {@code b}: by what the worst hand there
 *       is still wins of the pot after the call. With that smaller loss
 *       {@code l} in place of {@code b}, the share is {@code l / (p + l)},
 *       and nothing at all when the loss is nothing: a small bet before the
 *       flop is never worth a fold.</li>
 *   <li><b>How much to bluff.</b> A caller is indifferent to a bet of
 *       {@code b} into {@code p} when {@code b / (p + 2b)} of the hands that
 *       make it are bluffs, which is {@code b / (p + b)} bluffs for every
 *       hand bet for value. Fewer bluffs, and nobody need pay the good
 *       hands; more, and calling always wins.</li>
 * </ul>
 *
 * <p>A playbook measures a hand by its {@link Standing}: the share of all
 * hands it is ahead of. With it:
 * <ul>
 *   <li><b>Facing a bet</b>, it folds the hands whose standing is below the
 *       share the bet needs to fold, times the <i>tightness</i> of the style
 *       (1 folds exactly that share; the styles fold more, because people
 *       bluff less often than the theory allows). It never asks for a better
 *       hand than a cap, so that betting everything every time does not
 *       push it out of every pot; the cap is higher when the call would cost
 *       most of its chips. Of the hands it goes on with, it raises the best
 *       share, <i>raises</i>, and calls with the rest; and for every hand
 *       raised for value it raises as a bluff a number of the hands just
 *       short of a call. The more of its chips a raise would put in, the
 *       fewer hands it raises, and it does not bluff with all of them.</li>
 *   <li><b>When nobody has bet</b>, it bets the best share of all hands,
 *       <i>values</i>, for value, bets a number of the very worst as a
 *       bluff, and checks everything between: a middling hand wins a
 *       showdown often enough and gains nothing from a bet.</li>
 * </ul>
 * How often it bluffs is the number the theory allows for the size of its
 * bets, times <i>bluffs</i> (1 is exactly that number; the styles bluff
 * less, because people call more often than the theory asks). Its bets are
 * a share of the pot, <i>size</i>; a raise is three quarters of that share,
 * counted on the pot as it will be after calling, above the smallest raise
 * allowed. A bet or a raise that would put in most of its chips puts in all
 * of them.
 *
 * <p>The aggression setting moves the five numbers: a bolder opponent folds
 * less, raises and bets more hands, bluffs more often and bets more.
 *
 * <p>What this does not do: it compares its hand with every hand there is,
 * not with the hands the other player would still hold after the betting so
 * far, and it does not learn how the other player plays.
 */
final class Playbook {
    /** How far the aggression moves the tightness at either end of the scale. */
    private static final double TIGHTNESS_SWING = 0.10;
    /** How far the aggression moves the share of hands raised, and the share bet for value. */
    private static final double SHARE_SWING = 0.08;
    /** By how much of itself the aggression moves the number of bluffs. */
    private static final double BLUFF_SWING = 0.5;
    /** How far the aggression moves the size of the bets, as a share of the pot. */
    private static final double SIZE_SWING = 0.15;

    /** The best standing it ever asks for before a call that costs few of its chips. */
    private static final double CAP = 0.62;
    /** How much higher the cap is for a call that costs all of its chips. */
    private static final double CAP_FOR_EVERYTHING = 0.18;
    /** The share of the raising hands given up when the raise would put in all of its chips. */
    private static final double FEWER_RAISES_FOR_EVERYTHING = 0.75;
    /** A raise is this share of a bet above the smallest raise allowed. */
    private static final double RAISE_SHARE_OF_BET = 0.75;
    /** A bet or a raise of at least this share of what it could put in becomes all of it. */
    private static final double COMMITTED = 0.75;

    private final double tightness;
    private final double raises;
    private final double values;
    private final double bluffsPerValueBet;
    private final double size;

    /**
     * Creates the playbook of a style.
     *
     * @param tightness how much of the share a bet needs to fold the style
     *        does fold: 1 is exactly that share, more is more
     * @param raises the share it raises of the hands it goes on with
     *        against a bet
     * @param values the share of all hands it bets for value when nobody
     *        has bet
     * @param bluffs how often it bluffs, where 1 is as often as the size of
     *        its bets allows
     * @param size its bets as a share of the pot
     * @param aggression the setting that moves these numbers
     */
    Playbook(double tightness, double raises, double values, double bluffs, double size, Aggression aggression) {
        this.tightness = aggression.lowered(tightness, TIGHTNESS_SWING);
        this.raises = aggression.raised(raises, SHARE_SWING);
        this.values = aggression.raised(values, SHARE_SWING);
        this.size = aggression.raised(size, SIZE_SWING);
        this.bluffsPerValueBet = aggression.scaled(bluffs, BLUFF_SWING) * this.size / (1 + this.size);
    }

    /**
     * Decides what to do.
     *
     * @param context what the player to act can see
     * @return a legal action
     */
    Action decide(DecisionContext context) {
        double standing = Standing.of(context);
        Action action;
        if (context.getAmountToCall() == 0) {
            action = unbet(context, standing);
        }
        else {
            action = facingBet(context, standing);
        }
        return action;
    }

    /** Nobody has bet: bet the best hands for value and some of the worst as a bluff, check the rest. */
    private Action unbet(DecisionContext context, double standing) {
        boolean forValue = standing >= 1 - this.values;
        boolean asBluff = standing < this.values * this.bluffsPerValueBet;
        Action action = Action.check();
        if (context.canWager() && (forValue || asBluff)) {
            action = context.wager(total(context));
        }
        return action;
    }

    /** Facing a bet: fold the share of the worst hands the bet asks for, raise the best, call with the rest. */
    private Action facingBet(DecisionContext context, double standing) {
        double needed = Math.min(cap(context), this.tightness * shareTheBetNeedsToFold(context));
        Action action = Action.fold();
        if (standing >= needed) {
            action = Action.call();
        }
        if (context.canWager()) {
            int total = total(context);
            double risked = (double) total / context.getMaxWager();
            double raised = this.raises * (1 - needed) * (1 - FEWER_RAISES_FOR_EVERYTHING * risked);
            double bluffed = raised * this.bluffsPerValueBet * (1 - risked);
            boolean forValue = standing >= 1 - raised;
            boolean asBluff = standing < needed && standing >= needed - bluffed;
            if (forValue || asBluff) {
                action = context.wager(total);
            }
        }
        return action;
    }

    /**
     * The share of hands the bet in front of it needs to fold so that a
     * bluff makes money: what the bluff loses when it is called, against the
     * pot as it was before the bet plus that loss. The loss is the bet, less
     * what the worst hand there is still wins of the pot after the call.
     */
    private static double shareTheBetNeedsToFold(DecisionContext context) {
        int bet = context.getAmountToCall();
        long potAfterCalling = (long) context.getPot() + bet;
        double loss = Math.max(0.0, bet - Standing.ofTheWorstHand(context) * potAfterCalling);
        double share = 0.0;
        if (loss > 0.0) {
            share = loss / (context.getPot() - bet + loss);
        }
        return share;
    }

    /** The best standing it ever asks for before a call: higher the more of its chips the call costs. */
    private static double cap(DecisionContext context) {
        double stake = 1.0;
        if (context.getMaxWager() > context.getAmountToCall()) {
            stake = (double) context.getAmountToCall() / context.getMaxWager();
        }
        return CAP + CAP_FOR_EVERYTHING * stake;
    }

    /**
     * The total it bets or raises to: a share of the pot for a bet; for a
     * raise, the smallest raise allowed and on top of it three quarters of
     * that share of the pot as it will be after calling. Never below the
     * smallest amount allowed and never above its chips, and all of its
     * chips once most of them would go in anyway.
     */
    private int total(DecisionContext context) {
        long target;
        if (context.isLegal(ActionType.BET)) {
            target = Math.round(this.size * context.getPot());
        }
        else {
            long potAfterCalling = (long) context.getPot() + context.getAmountToCall();
            target = context.getMinWager() + Math.round(RAISE_SHARE_OF_BET * this.size * potAfterCalling);
        }
        int most = context.getMaxWager();
        int total = (int) Math.min(Math.max(target, context.getMinWager()), most);
        if (total >= COMMITTED * most) {
            total = most;
        }
        return total;
    }
}
