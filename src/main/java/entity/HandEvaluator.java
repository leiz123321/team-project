package entity;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

/**
 * Finds the best five-card poker hand among five to seven cards.
 *
 * <p>The strength of a hand is first worked out as a single number, so that
 * comparing two hands is comparing two numbers. {@link #evaluate(List)} turns
 * that number into a {@link HandValue}; code that compares a very large number
 * of hands can use {@link #score(int[], int)} directly.
 */
public class HandEvaluator {
    private static final int RANKS = 13;
    private static final int SUITS = 4;
    private static final int HAND_SIZE = 5;
    private static final int MAX_CARDS = 7;
    private static final int SLOTS = 5;
    private static final int SLOT_BITS = 4;
    private static final int SLOT_MASK = 0xF;
    private static final int CATEGORY_SHIFT = SLOTS * SLOT_BITS;
    private static final int STRAIGHT_MASK = 0x1F;
    private static final int WHEEL_MASK = 0x100F;
    private static final int FIVE_ORDINAL = Rank.FIVE.ordinal();
    private static final int FOUR = 4;
    private static final int THREE = 3;
    private static final int TWO = 2;

    /**
     * Returns the number that stands for a card in {@link #score(int[], int)}.
     *
     * @param card the card
     * @return a number from 0 to 51
     */
    public static int codeOf(Card card) {
        return card.getSuit().ordinal() * RANKS + card.getRank().ordinal();
    }

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
        int[] codes = new int[cards.size()];
        for (int i = 0; i < codes.length; i++) {
            codes[i] = codeOf(cards.get(i));
        }
        return toHandValue(score(codes, codes.length));
    }

    /**
     * Returns the strength of the best five-card hand among the given cards as
     * one number: a stronger hand always has a larger number, and hands that
     * tie have the same number. The cards are not checked.
     *
     * @param codes the cards, each as returned by {@link #codeOf(Card)}
     * @param count how many entries of codes to use, from 5 to 7
     * @return the strength of the best hand
     */
    public int score(int[] codes, int count) {
        int[] rankCounts = new int[RANKS];
        int[] suitCounts = new int[SUITS];
        int[] suitMasks = new int[SUITS];
        int rankMask = 0;
        for (int i = 0; i < count; i++) {
            int rank = codes[i] % RANKS;
            int suit = codes[i] / RANKS;
            rankCounts[rank]++;
            suitCounts[suit]++;
            suitMasks[suit] |= 1 << rank;
            rankMask |= 1 << rank;
        }

        int flushMask = 0;
        for (int suit = 0; suit < SUITS; suit++) {
            if (suitCounts[suit] >= HAND_SIZE) {
                flushMask = suitMasks[suit];
            }
        }
        if (flushMask != 0) {
            int high = straightHigh(flushMask);
            if (high >= 0) {
                return start(HandCategory.STRAIGHT_FLUSH) | slot(high, 0);
            }
        }

        // The highest rank held four, three, and two times, and the next highest.
        int quad = -1;
        int trip = -1;
        int secondTrip = -1;
        int pair = -1;
        int secondPair = -1;
        for (int rank = RANKS - 1; rank >= 0; rank--) {
            int held = rankCounts[rank];
            if (held == FOUR) {
                quad = rank;
            }
            else if (held == THREE) {
                if (trip < 0) {
                    trip = rank;
                }
                else if (secondTrip < 0) {
                    secondTrip = rank;
                }
            }
            else if (held == TWO) {
                if (pair < 0) {
                    pair = rank;
                }
                else if (secondPair < 0) {
                    secondPair = rank;
                }
            }
        }

        int result;
        if (quad >= 0) {
            result = start(HandCategory.FOUR_OF_A_KIND) | slot(quad, 0)
                    | kickers(rankMask & ~(1 << quad), 1, 1);
        }
        else if (trip >= 0 && (secondTrip >= 0 || pair >= 0)) {
            result = start(HandCategory.FULL_HOUSE) | slot(trip, 0)
                    | slot(Math.max(secondTrip, pair), 1);
        }
        else if (flushMask != 0) {
            result = start(HandCategory.FLUSH) | kickers(flushMask, HAND_SIZE, 0);
        }
        else if (straightHigh(rankMask) >= 0) {
            result = start(HandCategory.STRAIGHT) | slot(straightHigh(rankMask), 0);
        }
        else if (trip >= 0) {
            result = start(HandCategory.THREE_OF_A_KIND) | slot(trip, 0)
                    | kickers(rankMask & ~(1 << trip), 2, 1);
        }
        else if (secondPair >= 0) {
            result = start(HandCategory.TWO_PAIR) | slot(pair, 0) | slot(secondPair, 1)
                    | kickers(rankMask & ~(1 << pair) & ~(1 << secondPair), 1, 2);
        }
        else if (pair >= 0) {
            result = start(HandCategory.PAIR) | slot(pair, 0)
                    | kickers(rankMask & ~(1 << pair), THREE, 1);
        }
        else {
            result = start(HandCategory.HIGH_CARD) | kickers(rankMask, HAND_SIZE, 0);
        }
        return result;
    }

    /**
     * Returns the highest rank of the best straight among the ranks in the
     * mask, or -1 if there is none. In A-2-3-4-5 the ace is low, so the highest
     * rank is the five.
     */
    private int straightHigh(int rankMask) {
        for (int high = RANKS - 1; high >= HAND_SIZE - 1; high--) {
            if ((rankMask >> (high - HAND_SIZE + 1) & STRAIGHT_MASK) == STRAIGHT_MASK) {
                return high;
            }
        }
        if ((rankMask & WHEEL_MASK) == WHEEL_MASK) {
            return FIVE_ORDINAL;
        }
        return -1;
    }

    private int start(HandCategory category) {
        return category.ordinal() << CATEGORY_SHIFT;
    }

    /**
     * Places a rank in one of the five tie-break positions; position 0 is the
     * most important. A rank is stored as its ordinal plus one, so that an
     * empty position is 0.
     */
    private int slot(int rank, int position) {
        return (rank + 1) << (SLOTS - 1 - position) * SLOT_BITS;
    }

    /**
     * Places the highest ranks of the mask in consecutive tie-break positions.
     */
    private int kickers(int rankMask, int howMany, int firstPosition) {
        int bits = 0;
        int position = firstPosition;
        for (int rank = RANKS - 1; rank >= 0 && position < firstPosition + howMany; rank--) {
            if ((rankMask & 1 << rank) != 0) {
                bits |= slot(rank, position);
                position++;
            }
        }
        return bits;
    }

    private HandValue toHandValue(int score) {
        HandCategory category = HandCategory.values()[score >>> CATEGORY_SHIFT];
        List<Rank> tiebreakers = new ArrayList<>();
        for (int position = 0; position < SLOTS; position++) {
            int stored = score >>> (SLOTS - 1 - position) * SLOT_BITS & SLOT_MASK;
            if (stored != 0) {
                tiebreakers.add(Rank.values()[stored - 1]);
            }
        }
        return new HandValue(category, tiebreakers);
    }
}
