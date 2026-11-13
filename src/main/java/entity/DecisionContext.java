package entity;

import java.util.List;

/**
 * What the computer opponent may look at when deciding: only the cards and
 * betting information that the computer player can see.
 */
public final class DecisionContext {
    private final List<Card> holeCards;
    private final List<Card> boardCards;
    private final int pot;
    private final int amountToCall;
    private final List<ActionType> legalActionTypes;
    private final int minWager;
    private final int maxWager;
    private final double winChance;

    /**
     * Creates a context.
     *
     * @param holeCards the deciding player's own hole cards
     * @param boardCards the revealed community cards
     * @param pot the chips in the pot
     * @param amountToCall the chips the player must add to stay in the hand
     * @param legalActionTypes the kinds of action the player may take
     * @param minWager the smallest total the player may bet or raise to
     * @param maxWager the largest total the player may bet or raise to
     * @param winChance the player's chance of winning, between 0 and 1
     * @throws IllegalArgumentException if a list is null or winChance is not
     *         between 0 and 1
     */
    public DecisionContext(List<Card> holeCards, List<Card> boardCards, int pot, int amountToCall,
                           List<ActionType> legalActionTypes, int minWager, int maxWager,
                           double winChance) {
        if (holeCards == null || boardCards == null || legalActionTypes == null) {
            throw new IllegalArgumentException("lists must not be null");
        }
        if (winChance < 0.0 || winChance > 1.0) {
            throw new IllegalArgumentException("winChance must be between 0 and 1");
        }
        this.holeCards = List.copyOf(holeCards);
        this.boardCards = List.copyOf(boardCards);
        this.pot = pot;
        this.amountToCall = amountToCall;
        this.legalActionTypes = List.copyOf(legalActionTypes);
        this.minWager = minWager;
        this.maxWager = maxWager;
        this.winChance = winChance;
    }

    public List<Card> getHoleCards() {
        return this.holeCards;
    }

    public List<Card> getBoardCards() {
        return this.boardCards;
    }

    public int getPot() {
        return this.pot;
    }

    public int getAmountToCall() {
        return this.amountToCall;
    }

    public List<ActionType> getLegalActionTypes() {
        return this.legalActionTypes;
    }

    /**
     * Returns whether the player may take an action of the given kind.
     *
     * @param type the kind of action
     * @return true if it is allowed
     */
    public boolean isLegal(ActionType type) {
        return this.legalActionTypes.contains(type);
    }

    /**
     * Returns whether the player may put more chips in than a call: a bet when
     * nobody has bet yet, or a raise otherwise.
     */
    public boolean canWager() {
        return isLegal(ActionType.BET) || isLegal(ActionType.RAISE);
    }

    /**
     * Makes a bet or a raise, whichever is legal, to the given total.
     *
     * @param total the total to bet or raise to
     * @return the action
     * @throws IllegalStateException if neither a bet nor a raise is legal
     */
    public Action wager(int total) {
        if (isLegal(ActionType.BET)) {
            return Action.bet(total);
        }
        if (isLegal(ActionType.RAISE)) {
            return Action.raise(total);
        }
        throw new IllegalStateException("neither a bet nor a raise is legal");
    }

    public int getMinWager() {
        return this.minWager;
    }

    public int getMaxWager() {
        return this.maxWager;
    }

    public double getWinChance() {
        return this.winChance;
    }
}
