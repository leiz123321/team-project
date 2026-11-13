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
 * Every other card is treated as unknown, so the opponent's hole cards and the
 * cards still to come are drawn at random many times and the outcomes counted.
 */
public class EquityCalculator {
    /** How many random deals are tried when no number is given. */
    public static final int DEFAULT_ITERATIONS = 2000;

    private static final int HOLE_SIZE = 2;
    private static final int BOARD_SIZE = 5;
    private static final int FLOP_SIZE = 3;
    private static final int TURN_SIZE = 4;

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
     * @param iterations how many random deals to try
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
     * Estimates the chances of winning, tying, and losing against one opponent.
     *
     * @param holeCards the player's two hole cards
     * @param boardCards the revealed community cards: none, three, four, or five
     * @return the estimated chances
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

        int missing = BOARD_SIZE - boardCards.size();
        int wins = 0;
        int ties = 0;
        int losses = 0;
        for (int i = 0; i < this.iterations; i++) {
            Collections.shuffle(unknown, this.random);
            List<Card> restOfBoard = unknown.subList(HOLE_SIZE, HOLE_SIZE + missing);

            List<Card> mine = new ArrayList<>(holeCards);
            mine.addAll(boardCards);
            mine.addAll(restOfBoard);

            List<Card> theirs = new ArrayList<>(unknown.subList(0, HOLE_SIZE));
            theirs.addAll(boardCards);
            theirs.addAll(restOfBoard);

            int result = this.evaluator.evaluate(mine).compareTo(this.evaluator.evaluate(theirs));
            if (result > 0) {
                wins++;
            }
            else if (result == 0) {
                ties++;
            }
            else {
                losses++;
            }
        }
        double total = this.iterations;
        return new EquityResult(wins / total, ties / total, losses / total);
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
