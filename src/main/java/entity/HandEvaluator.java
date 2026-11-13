package entity;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

/**
 * Finds the best five-card poker hand among five to seven cards.
 */
public class HandEvaluator {
    private static final int HAND_SIZE = 5;
    private static final int MAX_CARDS = 7;
    private static final int FOUR = 4;
    private static final int THREE = 3;
    private static final int TWO = 2;

    /**
     * Returns the value of the best five-card hand that can be made from the
     * given cards. The state of the cards is ignored.
     *
     * @param cards five, six, or seven different cards
     * @return the value of the best hand
     * @throws IllegalArgumentException if the number of cards is wrong, a card
     *         is null, or a card appears twice
     */
    public HandValue evaluate(List<Card> cards) {
        if (cards == null || cards.size() < HAND_SIZE || cards.size() > MAX_CARDS) {
            throw new IllegalArgumentException("need between 5 and 7 cards");
        }
        if (cards.contains(null) || new HashSet<>(cards).size() != cards.size()) {
            throw new IllegalArgumentException("cards must be different and not null");
        }
        HandValue best = null;
        int n = cards.size();
        for (int a = 0; a < n; a++) {
            for (int b = a + 1; b < n; b++) {
                for (int c = b + 1; c < n; c++) {
                    for (int d = c + 1; d < n; d++) {
                        for (int e = d + 1; e < n; e++) {
                            HandValue value = evaluateFive(List.of(
                                    cards.get(a), cards.get(b), cards.get(c), cards.get(d), cards.get(e)));
                            if (best == null || value.compareTo(best) > 0) {
                                best = value;
                            }
                        }
                    }
                }
            }
        }
        return best;
    }

    private HandValue evaluateFive(List<Card> five) {
        Rank[] ranks = Rank.values();
        int[] counts = new int[ranks.length];
        boolean flush = true;
        Suit firstSuit = five.get(0).getSuit();
        for (Card card : five) {
            counts[card.getRank().ordinal()]++;
            if (card.getSuit() != firstSuit) {
                flush = false;
            }
        }

        // Ranks that appear more often come first; equal counts go from high to low.
        List<Rank> ordered = new ArrayList<>();
        for (int count = FOUR; count >= 1; count--) {
            for (int i = ranks.length - 1; i >= 0; i--) {
                if (counts[i] == count) {
                    ordered.add(ranks[i]);
                }
            }
        }
        int mostCommon = counts[ordered.get(0).ordinal()];
        int distinct = ordered.size();
        Rank straightHigh = straightHigh(ordered);

        HandValue value;
        if (straightHigh != null && flush) {
            value = new HandValue(HandCategory.STRAIGHT_FLUSH, List.of(straightHigh));
        }
        else if (mostCommon == FOUR) {
            value = new HandValue(HandCategory.FOUR_OF_A_KIND, ordered);
        }
        else if (mostCommon == THREE && distinct == TWO) {
            value = new HandValue(HandCategory.FULL_HOUSE, ordered);
        }
        else if (flush) {
            value = new HandValue(HandCategory.FLUSH, ordered);
        }
        else if (straightHigh != null) {
            value = new HandValue(HandCategory.STRAIGHT, List.of(straightHigh));
        }
        else if (mostCommon == THREE) {
            value = new HandValue(HandCategory.THREE_OF_A_KIND, ordered);
        }
        else if (mostCommon == TWO && distinct == THREE) {
            value = new HandValue(HandCategory.TWO_PAIR, ordered);
        }
        else if (mostCommon == TWO) {
            value = new HandValue(HandCategory.PAIR, ordered);
        }
        else {
            value = new HandValue(HandCategory.HIGH_CARD, ordered);
        }
        return value;
    }

    /**
     * Returns the highest rank of the straight made by these ranks, or null if
     * they do not make a straight. In A-2-3-4-5 the ace is low, so the highest
     * rank is the five.
     */
    private Rank straightHigh(List<Rank> ordered) {
        if (ordered.size() != HAND_SIZE) {
            return null;
        }
        Rank highest = ordered.get(0);
        Rank lowest = ordered.get(HAND_SIZE - 1);
        if (highest.ordinal() - lowest.ordinal() == HAND_SIZE - 1) {
            return highest;
        }
        if (highest == Rank.ACE && ordered.get(1) == Rank.FIVE) {
            return Rank.FIVE;
        }
        return null;
    }
}
