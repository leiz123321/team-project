package entity;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

/**
 * The 52 cards of a standard deck. The deck creates the cards once and keeps
 * all of them for the whole game; dealing a card changes that card's state
 * instead of removing it.
 */
public class Deck {
    private final List<Card> cards = new ArrayList<>();
    private int nextIndex;

    /**
     * Creates a deck of 52 distinct cards, all in the deck and not shuffled.
     */
    public Deck() {
        for (Suit suit : Suit.values()) {
            for (Rank rank : Rank.values()) {
                this.cards.add(new Card(rank, suit));
            }
        }
        this.nextIndex = 0;
    }

    /**
     * Returns every card to the deck and puts the cards in a random order.
     *
     * @param random the source of randomness; pass a seeded one in tests
     */
    public void shuffle(Random random) {
        reset();
        Collections.shuffle(this.cards, random);
    }

    /**
     * Returns every card to the deck without changing their order.
     */
    public void reset() {
        for (Card card : this.cards) {
            card.returnToDeck();
        }
        this.nextIndex = 0;
    }

    /**
     * Deals the top card to a player's hand.
     *
     * @return the card that was dealt
     * @throws IllegalStateException if no cards are left
     */
    public Card dealToHand() {
        Card card = nextCard();
        card.dealToHand();
        return card;
    }

    /**
     * Deals the top card as a face-down community card.
     *
     * @return the card that was dealt
     * @throws IllegalStateException if no cards are left
     */
    public Card dealToBoard() {
        Card card = nextCard();
        card.dealToBoard();
        return card;
    }

    private Card nextCard() {
        if (this.nextIndex >= this.cards.size()) {
            throw new IllegalStateException("no cards left in the deck");
        }
        Card card = this.cards.get(this.nextIndex);
        this.nextIndex++;
        return card;
    }

    /**
     * Returns how many cards have not been dealt yet.
     */
    public int remaining() {
        return this.cards.size() - this.nextIndex;
    }

    /**
     * Returns the cards that are currently in the given state.
     *
     * @param state the state to look for
     * @return a new list of the matching cards
     */
    public List<Card> cardsIn(CardState state) {
        List<Card> result = new ArrayList<>();
        for (Card card : this.cards) {
            if (card.getState() == state) {
                result.add(card);
            }
        }
        return result;
    }
}
