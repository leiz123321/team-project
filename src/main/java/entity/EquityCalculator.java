package entity;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Random;

/**
 * Works out the chances of winning, tying, and losing from the cards one
 * player can see: that player's hole cards and the revealed community cards.
 * Every other card is treated as unknown, and the opponent is assumed to hold
 * any two of the unknown cards with equal likelihood.
 *
 * <p>Once the flop is revealed, every possible outcome is counted, so the
 * result is exact: 1081 turn-and-river pairs with 990 opponent hands each on
 * the flop, 46 river cards with 990 opponent hands each on the turn, and 990
 * opponent hands on the river. Before the flop there are about two billion
 * outcomes, too many to count each time, so the result is looked up in
 * {@link PreflopEquityTable}.
 */
public class EquityCalculator {
    private static final int DECK_SIZE = 52;
    private static final int HOLE_SIZE = 2;
    private static final int BOARD_SIZE = 5;
    private static final int FLOP_SIZE = 3;
    private static final int TURN_SIZE = 4;
    private static final int SEVEN = 7;
    private static final int OUTCOMES = 3;
    private static final int WIN = 0;
    private static final int TIE = 1;
    private static final int LOSE = 2;

    private final HandEvaluator evaluator = new HandEvaluator();
    private final PreflopEquityTable preflopTable = new PreflopEquityTable();

    /**
     * Works out the chances of winning, tying, and losing against one opponent.
     *
     * @param holeCards the player's two hole cards
     * @param boardCards the revealed community cards: none, three, four, or five
     * @return the chances
     * @throws IllegalArgumentException if the number of cards is wrong, a card
     *         is null, or a card appears twice
     */
    public EquityResult calculate(List<Card> holeCards, List<Card> boardCards) {
        validate(holeCards, boardCards);
        if (boardCards.isEmpty()) {
            return this.preflopTable.lookup(holeCards.get(0), holeCards.get(1));
        }
        return countAll(holeCards, boardCards);
    }

    /**
     * Estimates the chances by dealing the unknown cards at random many times.
     * {@link #calculate(List, List)} does not use this; it is how the table of
     * chances before the flop was produced, and a way to cross-check the exact
     * results.
     *
     * @param holeCards the player's two hole cards
     * @param boardCards the revealed community cards: none, three, four, or five
     * @param random the source of randomness
     * @param iterations how many random deals to try
     * @return the estimated chances
     * @throws IllegalArgumentException if the cards are not valid, random is
     *         null, or iterations is not positive
     */
    public EquityResult simulate(List<Card> holeCards, List<Card> boardCards, Random random,
                                 int iterations) {
        validate(holeCards, boardCards);
        if (random == null || iterations <= 0) {
            throw new IllegalArgumentException("random must not be null and iterations must be positive");
        }
        int[] mine = new int[SEVEN];
        int[] theirs = new int[SEVEN];
        int[] unknown = prepare(holeCards, boardCards, mine, theirs);
        int firstMissing = HOLE_SIZE + boardCards.size();
        int drawn = HOLE_SIZE + SEVEN - firstMissing;

        long[] tally = new long[OUTCOMES];
        for (int i = 0; i < iterations; i++) {
            // Move a random selection of the unknown cards to the front.
            for (int k = 0; k < drawn; k++) {
                int other = k + random.nextInt(unknown.length - k);
                int swapped = unknown[k];
                unknown[k] = unknown[other];
                unknown[other] = swapped;
            }
            theirs[0] = unknown[0];
            theirs[1] = unknown[1];
            for (int k = firstMissing; k < SEVEN; k++) {
                int card = unknown[HOLE_SIZE + k - firstMissing];
                mine[k] = card;
                theirs[k] = card;
            }
            record(this.evaluator.score(mine, SEVEN), this.evaluator.score(theirs, SEVEN), tally);
        }
        return toResult(tally);
    }

    /**
     * Counts every possible outcome: each way the community cards can be
     * completed, with each pair of unknown cards the opponent could hold.
     */
    private EquityResult countAll(List<Card> holeCards, List<Card> boardCards) {
        int[] mine = new int[SEVEN];
        int[] theirs = new int[SEVEN];
        int[] unknown = prepare(holeCards, boardCards, mine, theirs);
        long[] tally = new long[OUTCOMES];

        int missing = BOARD_SIZE - boardCards.size();
        if (missing == 0) {
            countOpponentHands(mine, theirs, unknown, -1, -1, tally);
        }
        else if (missing == 1) {
            for (int i = 0; i < unknown.length; i++) {
                mine[SEVEN - 1] = unknown[i];
                theirs[SEVEN - 1] = unknown[i];
                countOpponentHands(mine, theirs, unknown, i, -1, tally);
            }
        }
        else {
            for (int i = 0; i < unknown.length; i++) {
                for (int j = i + 1; j < unknown.length; j++) {
                    mine[SEVEN - 2] = unknown[i];
                    theirs[SEVEN - 2] = unknown[i];
                    mine[SEVEN - 1] = unknown[j];
                    theirs[SEVEN - 1] = unknown[j];
                    countOpponentHands(mine, theirs, unknown, i, j, tally);
                }
            }
        }
        return toResult(tally);
    }

    /**
     * For one complete set of community cards, compares the player's hand with
     * every pair of unknown cards the opponent could hold. The two skipped
     * positions are the unknown cards already used as community cards.
     */
    private void countOpponentHands(int[] mine, int[] theirs, int[] unknown,
                                    int skipFirst, int skipSecond, long[] tally) {
        int myScore = this.evaluator.score(mine, SEVEN);
        for (int a = 0; a < unknown.length; a++) {
            if (a == skipFirst || a == skipSecond) {
                continue;
            }
            theirs[0] = unknown[a];
            for (int b = a + 1; b < unknown.length; b++) {
                if (b == skipFirst || b == skipSecond) {
                    continue;
                }
                theirs[1] = unknown[b];
                record(myScore, this.evaluator.score(theirs, SEVEN), tally);
            }
        }
    }

    /**
     * Puts the known cards into the two seven-card hands (hole cards in the
     * first two places, community cards after them) and returns the unknown
     * cards.
     */
    private int[] prepare(List<Card> holeCards, List<Card> boardCards, int[] mine, int[] theirs) {
        boolean[] known = new boolean[DECK_SIZE];
        for (int i = 0; i < HOLE_SIZE; i++) {
            mine[i] = HandEvaluator.codeOf(holeCards.get(i));
            known[mine[i]] = true;
        }
        for (int i = 0; i < boardCards.size(); i++) {
            int code = HandEvaluator.codeOf(boardCards.get(i));
            mine[HOLE_SIZE + i] = code;
            theirs[HOLE_SIZE + i] = code;
            known[code] = true;
        }
        int[] unknown = new int[DECK_SIZE - HOLE_SIZE - boardCards.size()];
        int next = 0;
        for (int code = 0; code < DECK_SIZE; code++) {
            if (!known[code]) {
                unknown[next] = code;
                next++;
            }
        }
        return unknown;
    }

    private void record(int myScore, int theirScore, long[] tally) {
        if (myScore > theirScore) {
            tally[WIN]++;
        }
        else if (myScore == theirScore) {
            tally[TIE]++;
        }
        else {
            tally[LOSE]++;
        }
    }

    private EquityResult toResult(long[] tally) {
        double total = tally[WIN] + tally[TIE] + tally[LOSE];
        return new EquityResult(tally[WIN] / total, tally[TIE] / total, tally[LOSE] / total);
    }

    private void validate(List<Card> holeCards, List<Card> boardCards) {
        if (holeCards == null || holeCards.size() != HOLE_SIZE) {
            throw new IllegalArgumentException("need exactly two hole cards");
        }
        if (boardCards == null) {
            throw new IllegalArgumentException("board cards must not be null");
        }
        int size = boardCards.size();
        if (size != 0 && size != FLOP_SIZE && size != TURN_SIZE && size != BOARD_SIZE) {
            throw new IllegalArgumentException("need zero, three, four, or five board cards");
        }
        List<Card> all = new ArrayList<>(holeCards);
        all.addAll(boardCards);
        if (all.contains(null) || new HashSet<>(all).size() != all.size()) {
            throw new IllegalArgumentException("cards must be different and not null");
        }
    }
}
