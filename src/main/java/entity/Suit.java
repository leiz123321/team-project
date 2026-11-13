package entity;

/**
 * The suit of a playing card. Suits have no order of strength.
 */
public enum Suit {
    DIAMOND("♦"),
    CLUB("♣"),
    HEART("♥"),
    SPADE("♠");

    private final String symbol;

    Suit(String symbol) {
        this.symbol = symbol;
    }

    /**
     * Returns the symbol of this suit, for example "♠".
     */
    public String getSymbol() {
        return this.symbol;
    }
}
