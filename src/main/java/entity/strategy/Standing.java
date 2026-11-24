package entity.strategy;

import entity.DecisionContext;

/**
 * Where a hand stands among all the hands a player could hold: the share of
 * them it is ahead of, from 0 (the worst hand there is) to 1 (the best).
 * The built-in strategies decide by this number, because the rules they
 * follow are about shares of hands: "fold the worst third", "raise the best
 * tenth".
 *
 * <p>What a strategy is told about its hand is its chance of winning against
 * a hand dealt at random, a tie counted as half a win. On the river, when
 * every card is out, that chance is the standing: a hand that wins 70 times
 * in 100 against a random hand is ahead of 70 in 100 of them.
 *
 * <p>Earlier it is not, because the chance still averages over the cards to
 * come. The chances then lie closer together: before the flop between 0.32
 * for the worst starting hand and 0.85 for a pair of aces, with half of all
 * hands below 0.50; on the flop no hand is below 0.12. For these three
 * streets the standing is read from a table: for a number of shares, the
 * chance of the hand that is ahead of that share of all hands on that
 * street. Between two entries the standing grows evenly. The table before
 * the flop was counted from the chances of all 1326 starting hands, the
 * tables for the flop and the turn from the exact chances of hands and
 * boards dealt at random (30,000 flops, 200,000 turns); a test counts all
 * three again.
 */
final class Standing {
    private static final int FLOP_CARDS = 3;
    private static final int TURN_CARDS = 4;

    /** The shares of all hands the tables have an entry for. */
    private static final double[] SHARES = {
        0.0, 0.1, 0.2, 0.3, 0.4, 0.5, 0.6, 0.7, 0.8, 0.9, 0.95, 0.975, 1.0,
    };

    /** Before the flop: the chance of winning of the hand that is ahead of each of those shares. */
    private static final double[] BEFORE_THE_FLOP = {
        0.3228, 0.3750, 0.4051, 0.4367, 0.4719, 0.4972, 0.5233, 0.5525, 0.5779, 0.6195, 0.6535, 0.7204, 0.8520,
    };

    /** The same on the flop. */
    private static final double[] ON_THE_FLOP = {
        0.1278, 0.2484, 0.3107, 0.3684, 0.4244, 0.4785, 0.5410, 0.6039, 0.6913, 0.7963, 0.8537, 0.9096, 1.0,
    };

    /** The same on the turn. */
    private static final double[] ON_THE_TURN = {
        0.0452, 0.1921, 0.2658, 0.3372, 0.4101, 0.4834, 0.5517, 0.6401, 0.7423, 0.8448, 0.9084, 0.9476, 1.0,
    };

    /** On the river the chance is the standing: the worst hand never wins. */
    private static final double[] ON_THE_RIVER = {};

    private Standing() {
    }

    /**
     * Returns where the hand of a decision stands.
     *
     * @param context the decision; its chance of winning is that of the hand
     *        against a hand dealt at random
     * @return the share of all hands the hand is ahead of, from 0 to 1
     */
    static double of(DecisionContext context) {
        return among(chances(context.getBoardCards().size()), context.getWinChance());
    }

    /**
     * Returns the chance of winning the worst hand there is still has on the
     * street of a decision, against a hand dealt at random.
     *
     * @param context the decision
     * @return the chance, from 0 to 1; 0 on the river
     */
    static double ofTheWorstHand(DecisionContext context) {
        double[] table = chances(context.getBoardCards().size());
        double chance = 0.0;
        if (table.length > 0) {
            chance = table[0];
        }
        return chance;
    }

    /**
     * Returns the share of all hands that a hand with the given chance of
     * winning is ahead of, by a table of chances.
     *
     * @param table the chances of the hands ahead of the shares of
     *        {@link #shares()}, or an empty table if the chance is the share
     * @param chance the chance of winning
     * @return the share, from 0 to 1
     */
    static double among(double[] table, double chance) {
        int last = table.length - 1;
        double share;
        if (table.length == 0) {
            share = chance;
        }
        else if (chance <= table[0]) {
            share = SHARES[0];
        }
        else if (chance >= table[last]) {
            share = SHARES[last];
        }
        else {
            int upper = 1;
            while (chance > table[upper]) {
                upper++;
            }
            double along = (chance - table[upper - 1]) / (table[upper] - table[upper - 1]);
            share = SHARES[upper - 1] + along * (SHARES[upper] - SHARES[upper - 1]);
        }
        return share;
    }

    /**
     * Returns the shares the tables have an entry for.
     *
     * @return a copy of the shares
     */
    static double[] shares() {
        return SHARES.clone();
    }

    /**
     * Returns the table of a street: the chances of the hands ahead of the
     * shares of {@link #shares()}.
     *
     * @param boardCards how many community cards are revealed: 0, 3, 4, or 5
     * @return a copy of the chances; empty for the river, where the chance
     *         is the share
     */
    static double[] chances(int boardCards) {
        double[] table = ON_THE_RIVER;
        if (boardCards == 0) {
            table = BEFORE_THE_FLOP;
        }
        else if (boardCards == FLOP_CARDS) {
            table = ON_THE_FLOP;
        }
        else if (boardCards == TURN_CARDS) {
            table = ON_THE_TURN;
        }
        return table.clone();
    }
}
