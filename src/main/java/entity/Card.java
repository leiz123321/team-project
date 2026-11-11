package entity;

/**
 * A simple entity representing a card. Cards have a rank and a suit.
 */
public class Card implements Comparable<Card>{

    private final int rank;
    private final int suit;

    /**
     * Creates a new user with the given rank and suit.
     * @param rank the rank of the card (11, 12, 13, 14) = (jack, queen, king, ace)
     * @param suit the suit of the card (1, 2, 3, 4) = (diamond, club, heart, spade)
     * @throws IllegalArgumentException if the rank or suit are out of the allowed range
     */
    public Card(int rank, int suit) {
        if (rank < 1 || rank > 14) {
            throw new IllegalArgumentException("This rank does not exist.");
        }
        if (suit < 1 || suit > 4) {
            throw new IllegalArgumentException("This suit does not exist.");
        }
        this.rank = rank;
        this.suit = suit;
    }

    public int getRank() {
        return rank;
    }

    public String getRankSymbol() {
        switch(rank) {
            case 11: return "J";
            case 12: return "Q";
            case 13: return "K";
            case 14: return "A";
            default: return Integer.toString(rank);
        }
    }

    public int getSuit() {
        return suit;
    }

    public String getSuitSymbol() {
        switch(suit) {
            case 1: return "♦";
            case 2: return "♣";
            case 3: return "♥";
            default: return "♠";
        }
    }

    @Override
    public String toString() {
        return getRankSymbol() + getSuitSymbol();
    }

    @Override
    public int compareTo(Card o) {
        return 4 * (this.rank - o.rank) + (this.suit - o.suit);
    }

}