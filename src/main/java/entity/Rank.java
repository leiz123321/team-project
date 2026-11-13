package entity;

/**
 * The rank of a playing card, in increasing order of strength.
 */
public enum Rank {
    TWO("2"),
    THREE("3"),
    FOUR("4"),
    FIVE("5"),
    SIX("6"),
    SEVEN("7"),
    EIGHT("8"),
    NINE("9"),
    TEN("10"),
    JACK("J"),
    QUEEN("Q"),
    KING("K"),
    ACE("A");

    private final String symbol;

    Rank(String symbol) {
        this.symbol = symbol;
    }

    /**
     * Returns the text printed on a card of this rank, for example "10" or "K".
     */
    public String getSymbol() {
        return this.symbol;
    }
}
