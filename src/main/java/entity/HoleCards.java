package entity;

import java.util.List;

/**
 * The two private cards held by one player.
 */
public class HoleCards {
    private final Card first;
    private final Card second;

    /**
     * Creates a player's hole cards from two cards that have been dealt to a hand.
     *
     * @param first one of the two cards
     * @param second the other card
     * @throws IllegalArgumentException if a card is null, the two cards are the
     *         same card, or a card is not in the {@link CardState#IN_HAND} state
     */
    public HoleCards(Card first, Card second) {
        if (first == null || second == null) {
            throw new IllegalArgumentException("hole cards must not be null");
        }
        if (first.equals(second)) {
            throw new IllegalArgumentException("hole cards must be two different cards");
        }
        if (first.getState() != CardState.IN_HAND || second.getState() != CardState.IN_HAND) {
            throw new IllegalArgumentException("hole cards must have been dealt to a hand");
        }
        this.first = first;
        this.second = second;
    }

    /**
     * Returns the two cards as a list that cannot be changed.
     */
    public List<Card> getCards() {
        return List.of(this.first, this.second);
    }

    @Override
    public String toString() {
        return this.first + " " + this.second;
    }
}
