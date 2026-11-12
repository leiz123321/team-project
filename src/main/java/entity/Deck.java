package entity;

/**
 * A simple entity representing a standard deck of cards
 */
public class Deck {

    private int size;
    private Card[] cards = new Card[52];

    /**
     * Creates a new deck.
     */
    public Deck() {
        reset();
    }

    public int getSize() {
        return size;
    }

    public Card[] getCards() {
        return cards;
    }

    public void reset() {
        this.size = 52;
        for (int i = 0; i <= 51; i++) {
            cards[i] = new Card(i % 13 + 2, i / 13 + 1);
        }
    }

    public void shuffle() {
        //TODO: implement
    }

}
