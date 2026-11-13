package entity;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

/**
 * The five community cards of one hand. All five are dealt face down at the
 * start and are turned face up in three steps: the flop (three cards), the
 * turn (one card), and the river (one card).
 */
public class Board {
    private static final int SIZE = 5;
    private static final int FLOP_SIZE = 3;
    private static final int TURN_SIZE = 4;

    private final List<Card> cards;

    /**
     * Creates a board from five cards that have been dealt face down.
     *
     * @param cards the five community cards, in the order they will be revealed
     * @throws IllegalArgumentException if there are not exactly five different
     *         cards, or a card is not in the {@link CardState#COMMUNITY_HIDDEN} state
     */
    public Board(List<Card> cards) {
        if (cards == null || cards.size() != SIZE) {
            throw new IllegalArgumentException("a board needs exactly " + SIZE + " cards");
        }
        if (cards.contains(null) || new HashSet<>(cards).size() != SIZE) {
            throw new IllegalArgumentException("board cards must be " + SIZE + " different cards");
        }
        for (Card card : cards) {
            if (card.getState() != CardState.COMMUNITY_HIDDEN) {
                throw new IllegalArgumentException("board cards must have been dealt face down");
            }
        }
        this.cards = new ArrayList<>(cards);
    }

    /**
     * Turns the first three cards face up.
     *
     * @throws IllegalStateException if any card has already been revealed
     */
    public void revealFlop() {
        requireRevealed(0);
        for (int i = 0; i < FLOP_SIZE; i++) {
            this.cards.get(i).reveal();
        }
    }

    /**
     * Turns the fourth card face up.
     *
     * @throws IllegalStateException if the flop has not been revealed, or the
     *         turn already has
     */
    public void revealTurn() {
        requireRevealed(FLOP_SIZE);
        this.cards.get(FLOP_SIZE).reveal();
    }

    /**
     * Turns the fifth card face up.
     *
     * @throws IllegalStateException if the turn has not been revealed, or the
     *         river already has
     */
    public void revealRiver() {
        requireRevealed(TURN_SIZE);
        this.cards.get(TURN_SIZE).reveal();
    }

    private void requireRevealed(int expected) {
        int actual = getRevealedCount();
        if (actual != expected) {
            throw new IllegalStateException(
                    "expected " + expected + " revealed cards but there are " + actual);
        }
    }

    /**
     * Returns the cards that are face up, in the order they were revealed.
     *
     * @return a new list of the revealed cards
     */
    public List<Card> getRevealedCards() {
        List<Card> revealed = new ArrayList<>();
        for (Card card : this.cards) {
            if (card.getState() == CardState.COMMUNITY_REVEALED) {
                revealed.add(card);
            }
        }
        return revealed;
    }

    /**
     * Returns how many cards are face up: 0, 3, 4, or 5.
     */
    public int getRevealedCount() {
        return getRevealedCards().size();
    }

    @Override
    public String toString() {
        return getRevealedCards().toString();
    }
}
