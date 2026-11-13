package entity;

/**
 * A participant in the game: a name, a chip count, hole cards, and whether
 * they have folded the current hand.
 */
public class Player {
    private final String name;
    private int chips;
    private HoleCards holeCards;
    private boolean folded;

    /**
     * Creates a player with no hole cards.
     *
     * @param name the player's name
     * @param chips the chips the player starts with
     * @throws IllegalArgumentException if the name is null or blank, or chips is negative
     */
    public Player(String name, int chips) {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("name must not be blank");
        }
        if (chips < 0) {
            throw new IllegalArgumentException("chips must not be negative");
        }
        this.name = name;
        this.chips = chips;
    }

    public String getName() {
        return this.name;
    }

    public int getChips() {
        return this.chips;
    }

    public boolean isFolded() {
        return this.folded;
    }

    public boolean hasHoleCards() {
        return this.holeCards != null;
    }

    /**
     * Returns this player's hole cards.
     *
     * @throws IllegalStateException if the player has not been dealt any
     */
    public HoleCards getHoleCards() {
        if (this.holeCards == null) {
            throw new IllegalStateException(this.name + " has no hole cards");
        }
        return this.holeCards;
    }

    /**
     * Gives this player their hole cards for a new hand.
     *
     * @param cards the hole cards
     * @throws IllegalArgumentException if cards is null
     */
    public void receiveHoleCards(HoleCards cards) {
        if (cards == null) {
            throw new IllegalArgumentException("hole cards must not be null");
        }
        this.holeCards = cards;
    }

    /**
     * Gets this player ready for a new hand: no hole cards and not folded.
     */
    public void clearHand() {
        this.holeCards = null;
        this.folded = false;
    }

    /**
     * Marks this player as having folded the current hand.
     */
    public void fold() {
        this.folded = true;
    }

    /**
     * Moves chips from this player into the pot.
     *
     * @param amount how many chips to put in
     * @throws IllegalArgumentException if amount is not positive or is more
     *         than the player has
     */
    public void bet(int amount) {
        if (amount <= 0 || amount > this.chips) {
            throw new IllegalArgumentException(
                    this.name + " cannot bet " + amount + " with " + this.chips + " chips");
        }
        this.chips -= amount;
    }

    /**
     * Gives chips to this player.
     *
     * @param amount how many chips the player receives
     * @throws IllegalArgumentException if amount is negative
     */
    public void win(int amount) {
        if (amount < 0) {
            throw new IllegalArgumentException("amount must not be negative");
        }
        this.chips += amount;
    }

    /**
     * Works out this player's chances of winning, tying, and losing from the
     * cards this player can see: their own hole cards and the revealed
     * community cards. Nothing else is used. The result is exact once the flop
     * has been revealed; before the flop it comes from a table worked out ahead
     * of time.
     *
     * @param board the community cards of the current hand
     * @return the chances
     * @throws IllegalStateException if the player has no hole cards
     */
    public EquityResult winChance(Board board) {
        return winChance(board, new EquityCalculator());
    }

    /**
     * Same as {@link #winChance(Board)}, with the calculator supplied by the
     * caller.
     *
     * @param board the community cards of the current hand
     * @param calculator the calculator to use
     * @return the chances
     * @throws IllegalStateException if the player has no hole cards
     */
    public EquityResult winChance(Board board, EquityCalculator calculator) {
        return calculator.calculate(getHoleCards().getCards(), board.getRevealedCards());
    }

    @Override
    public String toString() {
        return this.name + " (" + this.chips + ")";
    }
}
