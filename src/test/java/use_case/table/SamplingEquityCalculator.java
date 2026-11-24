package use_case.table;

import java.util.List;
import java.util.Random;

import entity.Card;
import entity.EquityCalculator;
import entity.EquityResult;

/**
 * A calculator of chances for tests that play thousands of hands. On the flop
 * and the turn, where counting every outcome takes tens of milliseconds, it
 * estimates the chances from a small number of random deals instead; before
 * the flop and on the river it gives the same answer as the real calculator.
 * The estimate depends only on the cards, so a test that uses it stays
 * repeatable.
 */
public final class SamplingEquityCalculator extends EquityCalculator {
    private static final int RIVER_SIZE = 5;
    private static final int DEALS = 120;

    private int calls;

    @Override
    public EquityResult calculate(List<Card> holeCards, List<Card> boardCards) {
        this.calls++;
        if (boardCards.isEmpty() || boardCards.size() == RIVER_SIZE) {
            return super.calculate(holeCards, boardCards);
        }
        // The text of the cards, not their hash codes: an enum's hash code changes from run to run.
        long seed = (holeCards.toString() + boardCards.toString()).hashCode();
        return simulate(holeCards, boardCards, new Random(seed), DEALS);
    }

    /**
     * Returns how many times chances have been asked of this calculator.
     */
    public int getCalls() {
        return this.calls;
    }
}
