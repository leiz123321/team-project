package entity;

/**
 * The category of a five-card poker hand, in increasing order of strength.
 */
public enum HandCategory {
    HIGH_CARD("High Card"),
    PAIR("Pair"),
    TWO_PAIR("Two Pair"),
    THREE_OF_A_KIND("Three of a Kind"),
    STRAIGHT("Straight"),
    FLUSH("Flush"),
    FULL_HOUSE("Full House"),
    FOUR_OF_A_KIND("Four of a Kind"),
    STRAIGHT_FLUSH("Straight Flush");

    private final String displayName;

    HandCategory(String displayName) {
        this.displayName = displayName;
    }

    /**
     * Returns the name of this category as it is shown to a person, for
     * example "Full House".
     */
    public String getDisplayName() {
        return this.displayName;
    }
}
