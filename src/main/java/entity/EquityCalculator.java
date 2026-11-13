package entity;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;

/**
 * Works out the chances of winning, tying, and losing from the cards one
 * player can see: that player's hole cards and the revealed community cards.
 * Every other card is treated as unknown, and the opponent is assumed to hold
 * any two of the unknown cards with equal likelihood.
 *
 * <p>With four or five community cards revealed, every possible opponent hand
 * and remaining community card is counted, so the result is exact. With fewer
 * revealed there are too many possibilities to count, so the unknown cards are
 * drawn at random many times and the result is an estimate.
 */
public class EquityCalculator {
    /** How many random deals are tried before the turn when no number is given. */
    public static final int DEFAULT_ITERATIONS = 2000;

    private static final int HOLE_SIZE = 2;
    private static final int BOARD_SIZE = 5;
    private static final int FLOP_SIZE = 3;
    private static final int TURN_SIZE = 4;
    private static final int OUTCOMES = 3;
    private static final int WIN = 0;
    private static final int TIE = 1;
    private static final int LOSE = 2;

    private final Random random;
    private final int iterations;
    private final HandEvaluator evaluator = new HandEvaluator();

    /**
     * Creates a calculator with a new source of randomness and the default
     * number of random deals.
     */
    public EquityCalculator() {
        this(new Random(), DEFAULT_ITERATIONS);
    }

    /**
     * Creates a calculator.
     *
     * @param random the source of randomness; pass a seeded one in tests
     * @param iterations how many random deals to try when the result is estimated
     * @throws IllegalArgumentException if random is null or iterations is not positive
     */
    public EquityCalculator(Random random, int iterations) {
        if (random == null || iterations <= 0) {
            throw new IllegalArgumentException("random must not be null and iterations must be positive");
        }
        this.random = random;
        this.iterations = iterations;
    }

    /**
     * Works out the chances of winning, tying, and losing against one opponent:
     * exactly when four or five community cards are revealed, and as an
     * estimate otherwise.
     *
     * @param holeCards the player's two hole cards
     * @param boardCards the revealed community cards: none, three, four, or five
     * @return the chances
     * @throws IllegalArgumentException if the number of cards is wrong, a card
     *         is null, or a card appears twice
     */
    public EquityResult calculate(List<Card> holeCards, List<Card> boardCards) {
        validate(holeCards, boardCards);

        Set<Card> known = new HashSet<>(holeCards);
        known.addAll(boardCards);
        List<Card> unknown = new ArrayList<>();
        for (Suit suit : Suit.values()) {
            for (Rank rank : Rank.values()) {
                Card card = new Card(rank, suit);
                if (!known.contains(card)) {
                    unknown.add(card);
                }
            }
        }

        if (boardCards.size() >= TURN_SIZE) {
            return countAll(holeCards, boardCards, unknown);
        }
        return simulate(holeCards, boardCards, unknown);
    }

    /**
     * Counts every possible outcome: each remaining community card, if one is
     * still to come, with each pair of unknown cards the opponent could hold.
     */
    private EquityResult countAll(List<Card> holeCards, List<Card> boardCards, List<Card> unknown) {
        int[] tally = new int[OUTCOMES];
        if (boardCards.size() == BOARD_SIZE) {
            countOpponentHands(holeCards, boardCards, unknown, null, tally);
        }
        else {
            for (Card river : unknown) {
                List<Card> fullBoard = new ArrayList<>(boardCards);
                fullBoard.add(river);
                countOpponentHands(holeCards, fullBoard, unknown, river, tally);
            }
        }
        return toResult(tally);
    }

    private void countOpponentHands(List<Card> holeCards, List<Card> fullBoard, List<Card> unknown,
                                    Card excluded, int[] tally) {
        List<Card> mine = new ArrayList<>(holeCards);
        mine.addAll(fullBoard);
        HandValue myValue = this.evaluator.evaluate(mine);
        for (int i = 0; i < unknown.size(); i++) {
            for (int j = i + 1; j < unknown.size(); j++) {
                Card first = unknown.get(i);
                Card second = unknown.get(j);
                if (first.equals(excluded) || second.equals(excluded)) {
                    continue;
                }
                List<Card> theirs = new ArrayList<>(fullBoard);
                theirs.add(first);
                theirs.add(second);
                record(myValue.compareTo(this.evaluator.evaluate(theirs)), tally);
            }
        }
    }

    /**
     * Draws the opponent's hand and the remaining community cards at random
     * many times and counts the outcomes.
     */
    private EquityResult simulate(List<Card> holeCards, List<Card> boardCards, List<Card> unknown) {
        int missing = BOARD_SIZE - boardCards.size();
        int[] tally = new int[OUTCOMES];
        for (int i = 0; i < this.iterations; i++) {
            Collections.shuffle(unknown, this.random);
            List<Card> restOfBoard = unknown.subList(HOLE_SIZE, HOLE_SIZE + missing);

            List<Card> mine = new ArrayList<>(holeCards);
            mine.addAll(boardCards);
            mine.addAll(restOfBoard);

            List<Card> theirs = new ArrayList<>(unknown.subList(0, HOLE_SIZE));
            theirs.addAll(boardCards);
            theirs.addAll(restOfBoard);

            record(this.evaluator.evaluate(mine).compareTo(this.evaluator.evaluate(theirs)), tally);
        }
        return toResult(tally);
    }

    private void record(int comparison, int[] tally) {
        if (comparison > 0) {
            tally[WIN]++;
        }
        else if (comparison == 0) {
            tally[TIE]++;
        }
        else {
            tally[LOSE]++;
        }
    }

    private EquityResult toResult(int[] tally) {
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
