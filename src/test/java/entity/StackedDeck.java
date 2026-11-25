package entity;

import java.util.List;
import java.util.Random;

/**
 * A deck for the tests that deals chosen cards in a chosen order, so that a
 * test decides who holds what. Shuffling it takes every card back without
 * mixing them. This is a helper for the tests, not a test.
 *
 * <p>{@link Game} deals the small blind's two hole cards first, then the big
 * blind's two, then the flop, the turn, and the river.
 */
public final class StackedDeck extends Deck {
    private final List<Card> order;
    private int next;

    /**
     * Creates a deck that deals the given cards and then has none left.
     *
     * @param texts the cards to deal, first to last, separated by spaces
     */
    public StackedDeck(String texts) {
        this.order = Cards.cards(texts);
    }

    @Override
    public void shuffle(Random random) {
        reset();
        this.next = 0;
    }

    @Override
    public Card dealToHand() {
        Card card = take();
        card.dealToHand();
        return card;
    }

    @Override
    public Card dealToBoard() {
        Card card = take();
        card.dealToBoard();
        return card;
    }

    // Finds this deck's own copy of the next chosen card, so that taking the
    // cards back later also takes back the ones dealt here.
    private Card take() {
        if (this.next >= this.order.size()) {
            throw new IllegalStateException("the stacked deck has no chosen cards left");
        }
        Card wanted = this.order.get(this.next);
        this.next++;
        for (Card card : cardsIn(CardState.IN_DECK)) {
            if (card.equals(wanted)) {
                return card;
            }
        }
        throw new IllegalStateException(wanted + " was chosen twice");
    }
}
