package entity;

import java.util.Objects;

/**
 * One playing card. Its rank and suit never change; its state records where
 * the card currently is and changes as the card is dealt and revealed.
 */
public class Card {
    private final Rank rank;
    private final Suit suit;
    private CardState state;

    /**
     * Creates a card that starts in the deck.
     *
     * @param rank the rank of the card
     * @param suit the suit of the card
     * @throws IllegalArgumentException if rank or suit is null
     */
    public Card(Rank rank, Suit suit) {
        if (rank == null) {
            throw new IllegalArgumentException("rank must not be null");
        }
        if (suit == null) {
            throw new IllegalArgumentException("suit must not be null");
        }
        this.rank = rank;
        this.suit = suit;
        this.state = CardState.IN_DECK;
    }

    public Rank getRank() {
        return this.rank;
    }

    public Suit getSuit() {
        return this.suit;
    }

    public CardState getState() {
        return this.state;
    }

    /**
     * Deals this card from the deck to a player's hand.
     *
     * @throws IllegalStateException if the card is not in the deck
     */
    public void dealToHand() {
        requireState(CardState.IN_DECK);
        this.state = CardState.IN_HAND;
    }

    /**
     * Deals this card from the deck as a face-down community card.
     *
     * @throws IllegalStateException if the card is not in the deck
     */
    public void dealToBoard() {
        requireState(CardState.IN_DECK);
        this.state = CardState.COMMUNITY_HIDDEN;
    }

    /**
     * Turns this face-down community card face up.
     *
     * @throws IllegalStateException if the card is not a face-down community card
     */
    public void reveal() {
        requireState(CardState.COMMUNITY_HIDDEN);
        this.state = CardState.COMMUNITY_REVEALED;
    }

    /**
     * Puts this card back in the deck, whatever state it is in.
     */
    public void returnToDeck() {
        this.state = CardState.IN_DECK;
    }

    private void requireState(CardState expected) {
        if (this.state != expected) {
            throw new IllegalStateException(
                    this + " must be " + expected + " but is " + this.state);
        }
    }

    /**
     * Two cards are equal when they have the same rank and suit. The state is
     * not compared.
     */
    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof Card)) {
            return false;
        }
        Card other = (Card) o;
        return this.rank == other.rank && this.suit == other.suit;
    }

    @Override
    public int hashCode() {
        return Objects.hash(this.rank, this.suit);
    }

    /**
     * Returns the rank symbol followed by the suit symbol, for example "A♠".
     */
    @Override
    public String toString() {
        return this.rank.getSymbol() + this.suit.getSymbol();
    }
}
