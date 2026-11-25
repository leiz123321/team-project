package use_case.table;

import java.util.List;

/**
 * Everything the game screen needs to know about the poker table at one
 * moment, in plain types. Dealing a hand and applying either player's betting action
 * produce the same table representation.
 *
 * <p>The table is always described from the human's seat: the opponent's hole
 * cards, and the cards that make the winning hand, are present only when the
 * hand ended in a showdown, and the chances of winning are the human's.
 *
 * <p>An object is built with a {@link Builder} and cannot be changed afterwards.
 */
public final class TableOutputData {

    /** The human player, as an actor, as the player to act, or as the winner. */
    public static final String HUMAN = "HUMAN";

    /** The computer opponent, as an actor, as the player to act, or as the winner. */
    public static final String OPPONENT = "OPPONENT";

    /** The winner of a hand whose pot was split. */
    public static final String SPLIT = "SPLIT";

    /** Nobody or nothing: no actor, no player to act, no winner yet, no hand name, no decision. */
    public static final String NONE = "";

    /** The opponent's action was chosen by the LLM. */
    public static final String DECIDED_BY_LLM = "LLM";

    /** The opponent's action was chosen by the built-in strategy. */
    public static final String DECIDED_BY_BUILT_IN = "BUILT_IN";

    /** The value of each chance when the human's chances are not available. */
    public static final double CHANCE_UNAVAILABLE = -1.0;

    private final int handNumber;
    private final String humanName;
    private final String opponentName;
    private final String street;
    private final int pot;
    private final int humanChips;
    private final int opponentChips;
    private final int humanBet;
    private final int opponentBet;
    private final List<String> humanCards;
    private final List<String> opponentCards;
    private final List<String> boardCards;
    private final boolean humanSmallBlind;
    private final boolean handOver;
    private final boolean matchOver;
    private final String winner;
    private final String toAct;
    private final List<String> legalActions;
    private final int callAmount;
    private final int minWager;
    private final int maxWager;
    private final double winChance;
    private final double tieChance;
    private final double loseChance;
    private final String humanHandName;
    private final String opponentHandName;
    private final List<String> winningCards;
    private final List<TableEventData> events;
    private final int humanChipChange;
    private final String decidedBy;

    private TableOutputData(Builder builder) {
        if (builder.humanName == null || builder.opponentName == null || builder.street == null
                || builder.winner == null || builder.toAct == null || builder.humanHandName == null
                || builder.opponentHandName == null || builder.decidedBy == null) {
            throw new IllegalArgumentException("texts must not be null");
        }
        if (builder.humanCards == null || builder.opponentCards == null || builder.boardCards == null
                || builder.legalActions == null || builder.winningCards == null || builder.events == null) {
            throw new IllegalArgumentException("lists must not be null");
        }
        this.handNumber = builder.handNumber;
        this.humanName = builder.humanName;
        this.opponentName = builder.opponentName;
        this.street = builder.street;
        this.pot = builder.pot;
        this.humanChips = builder.humanChips;
        this.opponentChips = builder.opponentChips;
        this.humanBet = builder.humanBet;
        this.opponentBet = builder.opponentBet;
        this.humanCards = List.copyOf(builder.humanCards);
        this.opponentCards = List.copyOf(builder.opponentCards);
        this.boardCards = List.copyOf(builder.boardCards);
        this.humanSmallBlind = builder.humanSmallBlind;
        this.handOver = builder.handOver;
        this.matchOver = builder.matchOver;
        this.winner = builder.winner;
        this.toAct = builder.toAct;
        this.legalActions = List.copyOf(builder.legalActions);
        this.callAmount = builder.callAmount;
        this.minWager = builder.minWager;
        this.maxWager = builder.maxWager;
        this.winChance = builder.winChance;
        this.tieChance = builder.tieChance;
        this.loseChance = builder.loseChance;
        this.humanHandName = builder.humanHandName;
        this.opponentHandName = builder.opponentHandName;
        this.winningCards = List.copyOf(builder.winningCards);
        this.events = List.copyOf(builder.events);
        this.humanChipChange = builder.humanChipChange;
        this.decidedBy = builder.decidedBy;
    }

    /**
     * Returns the number of the hand within the match, counting from 1, or 0
     * if no hand has been started yet.
     */
    public int getHandNumber() {
        return this.handNumber;
    }

    public String getHumanName() {
        return this.humanName;
    }

    public String getOpponentName() {
        return this.opponentName;
    }

    /**
     * Returns the stage of the hand: the name of an {@code entity.Street},
     * for example "FLOP".
     */
    public String getStreet() {
        return this.street;
    }

    /**
     * Returns the whole pot, including what both players have bet on the
     * current betting round.
     */
    public int getPot() {
        return this.pot;
    }

    /**
     * Returns the chips in the human's stack, not counting chips already bet.
     */
    public int getHumanChips() {
        return this.humanChips;
    }

    /**
     * Returns the chips in the opponent's stack, not counting chips already bet.
     */
    public int getOpponentChips() {
        return this.opponentChips;
    }

    /**
     * Returns the chips the human has bet on the current betting round.
     */
    public int getHumanBet() {
        return this.humanBet;
    }

    /**
     * Returns the chips the opponent has bet on the current betting round.
     */
    public int getOpponentBet() {
        return this.opponentBet;
    }

    /**
     * Returns the human's two hole cards as text such as "A♠", or an empty
     * list if the human has no cards. The list cannot be changed.
     */
    public List<String> getHumanCards() {
        return this.humanCards;
    }

    /**
     * Returns the opponent's two hole cards as text, but only when the hand
     * ended in a showdown. Otherwise the list is empty: the human must never
     * be able to see them. The list cannot be changed.
     */
    public List<String> getOpponentCards() {
        return this.opponentCards;
    }

    /**
     * Returns the revealed community cards as text, in the order they were
     * revealed. The list cannot be changed.
     */
    public List<String> getBoardCards() {
        return this.boardCards;
    }

    /**
     * Returns whether the human posted the small blind in this hand, and so
     * holds the dealer button.
     */
    public boolean isHumanSmallBlind() {
        return this.humanSmallBlind;
    }

    public boolean isHandOver() {
        return this.handOver;
    }

    /**
     * Returns whether the match is over: the hand is over and a player cannot
     * post the big blind for another one.
     */
    public boolean isMatchOver() {
        return this.matchOver;
    }

    /**
     * Returns who won the hand: {@link #HUMAN}, {@link #OPPONENT},
     * {@link #SPLIT}, or {@link #NONE} while the hand is being played.
     */
    public String getWinner() {
        return this.winner;
    }

    /**
     * Returns whose turn it is: {@link #HUMAN}, {@link #OPPONENT}, or
     * {@link #NONE} when the hand is over.
     */
    public String getToAct() {
        return this.toAct;
    }

    /**
     * Returns the kinds of action the human may take now, as names of
     * {@code entity.ActionType}. The list is empty unless it is the human's
     * turn, and it cannot be changed.
     */
    public List<String> getLegalActions() {
        return this.legalActions;
    }

    /**
     * Returns how many chips the human must add to call. It is 0 unless it is
     * the human's turn.
     */
    public int getCallAmount() {
        return this.callAmount;
    }

    /**
     * Returns the smallest total the human may bet or raise to. It is only
     * meaningful when a bet or a raise is among the legal actions.
     */
    public int getMinWager() {
        return this.minWager;
    }

    /**
     * Returns the largest total the human may bet or raise to. It is only
     * meaningful when a bet or a raise is among the legal actions.
     */
    public int getMaxWager() {
        return this.maxWager;
    }

    /**
     * Returns the human's chance of winning, from 0 to 1, worked out from the
     * human's own cards and the revealed community cards. It is
     * {@link #CHANCE_UNAVAILABLE}, like the other two chances, when the human
     * has folded or has no cards.
     */
    public double getWinChance() {
        return this.winChance;
    }

    /**
     * Returns the human's chance of tying, from 0 to 1, or
     * {@link #CHANCE_UNAVAILABLE}.
     */
    public double getTieChance() {
        return this.tieChance;
    }

    /**
     * Returns the human's chance of losing, from 0 to 1, or
     * {@link #CHANCE_UNAVAILABLE}.
     */
    public double getLoseChance() {
        return this.loseChance;
    }

    /**
     * Returns the name of the human's best hand, for example "Two Pair", once
     * at least three community cards are revealed, and {@link #NONE} before.
     */
    public String getHumanHandName() {
        return this.humanHandName;
    }

    /**
     * Returns the name of the opponent's best hand when the hand ended in a
     * showdown, and {@link #NONE} otherwise.
     */
    public String getOpponentHandName() {
        return this.opponentHandName;
    }

    /**
     * Returns the cards that make the winning hand, as text such as "A♠", when
     * the hand ended in a showdown. If one player won, these are the five
     * cards of that player's best hand: the winner's hole cards among them
     * first, then the community cards in the order they were revealed. If the
     * pot was split, they are the five cards of the human's best hand followed
     * by the cards of the opponent's best hand that are not already there, so
     * no card is listed twice. The list is empty while the hand is being
     * played and when it ended with a fold, and it cannot be changed.
     */
    public List<String> getWinningCards() {
        return this.winningCards;
    }

    /**
     * Returns the events that happened since the table of the same hand was
     * last reported, in order. For a new hand these are all the events from
     * the start of the hand. The list cannot be changed.
     */
    public List<TableEventData> getEvents() {
        return this.events;
    }

    /**
     * Returns how many chips the human won (positive) or lost (negative) in
     * this hand once it is over, and 0 while it is being played.
     */
    public int getHumanChipChange() {
        return this.humanChipChange;
    }

    /**
     * Returns who chose the opponent's action reported here:
     * {@link #DECIDED_BY_LLM}, {@link #DECIDED_BY_BUILT_IN}, or {@link #NONE}
     * if the opponent did not act.
     */
    public String getDecidedBy() {
        return this.decidedBy;
    }

    /**
     * Collects the parts of a {@link TableOutputData}. Whatever is not set
     * keeps its starting value: 0 for a number, false for a flag,
     * {@link TableOutputData#NONE} for a text, an empty list for a list, and
     * {@link TableOutputData#CHANCE_UNAVAILABLE} for each of the three chances.
     */
    public static final class Builder {
        private int handNumber;
        private String humanName = NONE;
        private String opponentName = NONE;
        private String street = NONE;
        private int pot;
        private int humanChips;
        private int opponentChips;
        private int humanBet;
        private int opponentBet;
        private List<String> humanCards = List.of();
        private List<String> opponentCards = List.of();
        private List<String> boardCards = List.of();
        private boolean humanSmallBlind;
        private boolean handOver;
        private boolean matchOver;
        private String winner = NONE;
        private String toAct = NONE;
        private List<String> legalActions = List.of();
        private int callAmount;
        private int minWager;
        private int maxWager;
        private double winChance = CHANCE_UNAVAILABLE;
        private double tieChance = CHANCE_UNAVAILABLE;
        private double loseChance = CHANCE_UNAVAILABLE;
        private String humanHandName = NONE;
        private String opponentHandName = NONE;
        private List<String> winningCards = List.of();
        private List<TableEventData> events = List.of();
        private int humanChipChange;
        private String decidedBy = NONE;

        /**
         * Sets the number of the hand within the match.
         *
         * @param value the hand number, counting from 1
         * @return this builder
         */
        public Builder setHandNumber(int value) {
            this.handNumber = value;
            return this;
        }

        /**
         * Sets the human player's name.
         *
         * @param value the name
         * @return this builder
         */
        public Builder setHumanName(String value) {
            this.humanName = value;
            return this;
        }

        /**
         * Sets the computer opponent's name.
         *
         * @param value the name
         * @return this builder
         */
        public Builder setOpponentName(String value) {
            this.opponentName = value;
            return this;
        }

        /**
         * Sets the stage of the hand.
         *
         * @param value the name of an {@code entity.Street}
         * @return this builder
         */
        public Builder setStreet(String value) {
            this.street = value;
            return this;
        }

        /**
         * Sets the whole pot, including the bets of the current betting round.
         *
         * @param value the chips in the pot
         * @return this builder
         */
        public Builder setPot(int value) {
            this.pot = value;
            return this;
        }

        /**
         * Sets the chips in the human's stack.
         *
         * @param value the chips, not counting chips already bet
         * @return this builder
         */
        public Builder setHumanChips(int value) {
            this.humanChips = value;
            return this;
        }

        /**
         * Sets the chips in the opponent's stack.
         *
         * @param value the chips, not counting chips already bet
         * @return this builder
         */
        public Builder setOpponentChips(int value) {
            this.opponentChips = value;
            return this;
        }

        /**
         * Sets the human's bet on the current betting round.
         *
         * @param value the chips bet
         * @return this builder
         */
        public Builder setHumanBet(int value) {
            this.humanBet = value;
            return this;
        }

        /**
         * Sets the opponent's bet on the current betting round.
         *
         * @param value the chips bet
         * @return this builder
         */
        public Builder setOpponentBet(int value) {
            this.opponentBet = value;
            return this;
        }

        /**
         * Sets the human's hole cards.
         *
         * @param value the two cards as text, or an empty list
         * @return this builder
         */
        public Builder setHumanCards(List<String> value) {
            this.humanCards = value;
            return this;
        }

        /**
         * Sets the opponent's hole cards. Set them only when the hand ended in
         * a showdown.
         *
         * @param value the two cards as text, or an empty list
         * @return this builder
         */
        public Builder setOpponentCards(List<String> value) {
            this.opponentCards = value;
            return this;
        }

        /**
         * Sets the revealed community cards.
         *
         * @param value the cards as text, in the order they were revealed
         * @return this builder
         */
        public Builder setBoardCards(List<String> value) {
            this.boardCards = value;
            return this;
        }

        /**
         * Sets whether the human posted the small blind in this hand.
         *
         * @param value true if the human is the small blind
         * @return this builder
         */
        public Builder setHumanSmallBlind(boolean value) {
            this.humanSmallBlind = value;
            return this;
        }

        /**
         * Sets whether the hand is over.
         *
         * @param value true if the hand is over
         * @return this builder
         */
        public Builder setHandOver(boolean value) {
            this.handOver = value;
            return this;
        }

        /**
         * Sets whether the match is over.
         *
         * @param value true if the match is over
         * @return this builder
         */
        public Builder setMatchOver(boolean value) {
            this.matchOver = value;
            return this;
        }

        /**
         * Sets who won the hand.
         *
         * @param value {@link TableOutputData#HUMAN}, {@link TableOutputData#OPPONENT},
         *        {@link TableOutputData#SPLIT}, or {@link TableOutputData#NONE}
         * @return this builder
         */
        public Builder setWinner(String value) {
            this.winner = value;
            return this;
        }

        /**
         * Sets whose turn it is.
         *
         * @param value {@link TableOutputData#HUMAN}, {@link TableOutputData#OPPONENT},
         *        or {@link TableOutputData#NONE}
         * @return this builder
         */
        public Builder setToAct(String value) {
            this.toAct = value;
            return this;
        }

        /**
         * Sets the kinds of action the human may take now.
         *
         * @param value names of {@code entity.ActionType}; empty unless it is
         *        the human's turn
         * @return this builder
         */
        public Builder setLegalActions(List<String> value) {
            this.legalActions = value;
            return this;
        }

        /**
         * Sets how many chips the human must add to call.
         *
         * @param value the chips to add
         * @return this builder
         */
        public Builder setCallAmount(int value) {
            this.callAmount = value;
            return this;
        }

        /**
         * Sets the smallest total the human may bet or raise to.
         *
         * @param value the smallest total
         * @return this builder
         */
        public Builder setMinWager(int value) {
            this.minWager = value;
            return this;
        }

        /**
         * Sets the largest total the human may bet or raise to.
         *
         * @param value the largest total
         * @return this builder
         */
        public Builder setMaxWager(int value) {
            this.maxWager = value;
            return this;
        }

        /**
         * Sets the human's chance of winning.
         *
         * @param value the chance, from 0 to 1
         * @return this builder
         */
        public Builder setWinChance(double value) {
            this.winChance = value;
            return this;
        }

        /**
         * Sets the human's chance of tying.
         *
         * @param value the chance, from 0 to 1
         * @return this builder
         */
        public Builder setTieChance(double value) {
            this.tieChance = value;
            return this;
        }

        /**
         * Sets the human's chance of losing.
         *
         * @param value the chance, from 0 to 1
         * @return this builder
         */
        public Builder setLoseChance(double value) {
            this.loseChance = value;
            return this;
        }

        /**
         * Sets the name of the human's best hand.
         *
         * @param value the name, for example "Two Pair"
         * @return this builder
         */
        public Builder setHumanHandName(String value) {
            this.humanHandName = value;
            return this;
        }

        /**
         * Sets the name of the opponent's best hand. Set it only when the hand
         * ended in a showdown.
         *
         * @param value the name, for example "Flush"
         * @return this builder
         */
        public Builder setOpponentHandName(String value) {
            this.opponentHandName = value;
            return this;
        }

        /**
         * Sets the cards that make the winning hand. Set them only when the
         * hand ended in a showdown.
         *
         * @param value the cards as text: the winner's best five, or both
         *        players' best fives without repeats if the pot was split
         * @return this builder
         */
        public Builder setWinningCards(List<String> value) {
            this.winningCards = value;
            return this;
        }

        /**
         * Sets the events that happened since the table of the same hand was
         * last reported.
         *
         * @param value the events, in order
         * @return this builder
         */
        public Builder setEvents(List<TableEventData> value) {
            this.events = value;
            return this;
        }

        /**
         * Sets how many chips the human won or lost in this hand.
         *
         * @param value the chips won (positive) or lost (negative)
         * @return this builder
         */
        public Builder setHumanChipChange(int value) {
            this.humanChipChange = value;
            return this;
        }

        /**
         * Sets who chose the opponent's action reported here.
         *
         * @param value {@link TableOutputData#DECIDED_BY_LLM},
         *        {@link TableOutputData#DECIDED_BY_BUILT_IN}, or
         *        {@link TableOutputData#NONE}
         * @return this builder
         */
        public Builder setDecidedBy(String value) {
            this.decidedBy = value;
            return this;
        }

        /**
         * Builds the description of the table from what has been set.
         *
         * @return the table, which cannot be changed
         * @throws IllegalArgumentException if a text or a list was set to null
         * @throws NullPointerException if a list contains null
         */
        public TableOutputData build() {
            return new TableOutputData(this);
        }
    }
}
