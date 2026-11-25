package interface_adapter.table;

import java.util.List;

import use_case.table.TableOutputData;

/**
 * Everything the game screen draws, at one moment. The presenters of the game
 * publish a new state, with the next revision number, every time a use case
 * answers; the screen never works anything out about the game itself.
 *
 * <p>A state cannot be changed. A new one is made with a {@link Builder},
 * either from nothing or starting from an existing state.
 */
public final class TableState {

    /** The human player, as the winner of a hand or as the actor of an event. */
    public static final String HUMAN = TableOutputData.HUMAN;

    /** The computer opponent, as the winner of a hand or as the actor of an event. */
    public static final String OPPONENT = TableOutputData.OPPONENT;

    /** The winner of a hand whose pot was split. */
    public static final String SPLIT = TableOutputData.SPLIT;

    /** Nobody or nothing: no winner yet, no actor, no hand name, no message, no decision. */
    public static final String NONE = TableOutputData.NONE;

    /** The opponent's last action was chosen by the LLM. */
    public static final String DECIDED_BY_LLM = TableOutputData.DECIDED_BY_LLM;

    /** The opponent's last action was chosen by the built-in strategy. */
    public static final String DECIDED_BY_BUILT_IN = TableOutputData.DECIDED_BY_BUILT_IN;

    /** The value of each chance when the human's chances are not available. */
    public static final double CHANCE_UNAVAILABLE = TableOutputData.CHANCE_UNAVAILABLE;

    /** The street of a table at which no hand has been dealt yet. */
    public static final String FIRST_STREET = "PREFLOP";

    private final long revision;
    private final int handNumber;
    private final String humanName;
    private final String opponentName;
    private final List<String> humanCards;
    private final List<String> opponentCards;
    private final List<String> boardCards;
    private final String street;
    private final int pot;
    private final int humanChips;
    private final int opponentChips;
    private final int humanBet;
    private final int opponentBet;
    private final boolean humanDealer;
    private final boolean handInProgress;
    private final boolean handOver;
    private final boolean matchOver;
    private final String winner;
    private final boolean humanToAct;
    private final boolean opponentToAct;
    private final boolean foldAllowed;
    private final boolean checkAllowed;
    private final boolean callAllowed;
    private final boolean betAllowed;
    private final boolean raiseAllowed;
    private final int callAmount;
    private final int minWager;
    private final int maxWager;
    private final double winChance;
    private final double tieChance;
    private final double loseChance;
    private final String humanHandName;
    private final String opponentHandName;
    private final List<String> winningCards;
    private final String message;
    private final String errorMessage;
    private final String decidedBy;
    private final int humanChipChange;
    private final List<TableEventView> events;

    private TableState(Builder builder) {
        if (builder.humanName == null || builder.opponentName == null || builder.street == null
                || builder.winner == null || builder.humanHandName == null
                || builder.opponentHandName == null || builder.message == null
                || builder.errorMessage == null || builder.decidedBy == null) {
            throw new IllegalArgumentException("texts must not be null");
        }
        if (builder.humanCards == null || builder.opponentCards == null || builder.boardCards == null
                || builder.winningCards == null || builder.events == null) {
            throw new IllegalArgumentException("lists must not be null");
        }
        this.revision = builder.revision;
        this.handNumber = builder.handNumber;
        this.humanName = builder.humanName;
        this.opponentName = builder.opponentName;
        this.humanCards = List.copyOf(builder.humanCards);
        this.opponentCards = List.copyOf(builder.opponentCards);
        this.boardCards = List.copyOf(builder.boardCards);
        this.street = builder.street;
        this.pot = builder.pot;
        this.humanChips = builder.humanChips;
        this.opponentChips = builder.opponentChips;
        this.humanBet = builder.humanBet;
        this.opponentBet = builder.opponentBet;
        this.humanDealer = builder.humanDealer;
        this.handInProgress = builder.handInProgress;
        this.handOver = builder.handOver;
        this.matchOver = builder.matchOver;
        this.winner = builder.winner;
        this.humanToAct = builder.humanToAct;
        this.opponentToAct = builder.opponentToAct;
        this.foldAllowed = builder.foldAllowed;
        this.checkAllowed = builder.checkAllowed;
        this.callAllowed = builder.callAllowed;
        this.betAllowed = builder.betAllowed;
        this.raiseAllowed = builder.raiseAllowed;
        this.callAmount = builder.callAmount;
        this.minWager = builder.minWager;
        this.maxWager = builder.maxWager;
        this.winChance = builder.winChance;
        this.tieChance = builder.tieChance;
        this.loseChance = builder.loseChance;
        this.humanHandName = builder.humanHandName;
        this.opponentHandName = builder.opponentHandName;
        this.winningCards = List.copyOf(builder.winningCards);
        this.message = builder.message;
        this.errorMessage = builder.errorMessage;
        this.decidedBy = builder.decidedBy;
        this.humanChipChange = builder.humanChipChange;
        this.events = List.copyOf(builder.events);
    }

    /**
     * Returns the number of this state: 0 for the state before anything has
     * happened, and one more for every state the presenters publish after it.
     */
    public long getRevision() {
        return this.revision;
    }

    /**
     * Returns the number of the hand within the match, counting from 1, or 0
     * if no hand has been dealt yet.
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
     * Returns the picture ids of the human's two hole cards, such as "AS", or
     * an empty list if no hand has been dealt yet. The list cannot be changed.
     */
    public List<String> getHumanCards() {
        return this.humanCards;
    }

    /**
     * Returns the picture ids of the opponent's two hole cards. Both are
     * {@link CardIds#BACK} while the cards are hidden; they are the real ids
     * only after a showdown. The list is empty if no hand has been dealt yet,
     * and it cannot be changed.
     */
    public List<String> getOpponentCards() {
        return this.opponentCards;
    }

    /**
     * Returns the picture ids of the revealed community cards, in the order
     * they were revealed: none, three, four, or five. The list cannot be changed.
     */
    public List<String> getBoardCards() {
        return this.boardCards;
    }

    /**
     * Returns the stage of the hand: "PREFLOP", "FLOP", "TURN", "RIVER", or
     * "SHOWDOWN".
     */
    public String getStreet() {
        return this.street;
    }

    /**
     * Returns the chips already pulled into the middle of the table: the whole
     * pot less what the two players have in front of them on the current
     * betting round.
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
     * Returns the chips the human has in front of them on the current betting round.
     */
    public int getHumanBet() {
        return this.humanBet;
    }

    /**
     * Returns the chips the opponent has in front of them on the current betting round.
     */
    public int getOpponentBet() {
        return this.opponentBet;
    }

    /**
     * Returns whether the human holds the dealer button in this hand, which in
     * a two-player game means the human posted the small blind.
     */
    public boolean isHumanDealer() {
        return this.humanDealer;
    }

    /**
     * Returns whether a hand has been dealt and is still being played.
     */
    public boolean isHandInProgress() {
        return this.handInProgress;
    }

    public boolean isHandOver() {
        return this.handOver;
    }

    /**
     * Returns whether the match is over: the hand is over and one player
     * cannot post the big blind for another.
     */
    public boolean isMatchOver() {
        return this.matchOver;
    }

    /**
     * Returns who won the hand: {@link #HUMAN}, {@link #OPPONENT},
     * {@link #SPLIT}, or {@link #NONE} while no hand has been won.
     */
    public String getWinner() {
        return this.winner;
    }

    public boolean isHumanToAct() {
        return this.humanToAct;
    }

    public boolean isOpponentToAct() {
        return this.opponentToAct;
    }

    /**
     * Returns whether the human may fold now. Like the other four "can"
     * methods, it is false unless it is the human's turn.
     */
    public boolean canFold() {
        return this.foldAllowed;
    }

    /**
     * Returns whether the human may check now.
     */
    public boolean canCheck() {
        return this.checkAllowed;
    }

    /**
     * Returns whether the human may call now.
     */
    public boolean canCall() {
        return this.callAllowed;
    }

    /**
     * Returns whether the human may bet now. If so, the bet must be between
     * {@link #getMinWager()} and {@link #getMaxWager()}.
     */
    public boolean canBet() {
        return this.betAllowed;
    }

    /**
     * Returns whether the human may raise now. If so, the raise must be to a
     * total between {@link #getMinWager()} and {@link #getMaxWager()}.
     */
    public boolean canRaise() {
        return this.raiseAllowed;
    }

    /**
     * Returns how many chips the human must add to call. It is 0 unless it is
     * the human's turn.
     */
    public int getCallAmount() {
        return this.callAmount;
    }

    /**
     * Returns the smallest total the human may bet or raise to on the current
     * betting round. It is only meaningful when the human can bet or raise.
     */
    public int getMinWager() {
        return this.minWager;
    }

    /**
     * Returns the largest total the human may bet or raise to on the current
     * betting round, which puts all of the human's chips in. It is only
     * meaningful when the human can bet or raise.
     */
    public int getMaxWager() {
        return this.maxWager;
    }

    /**
     * Returns the human's chance of winning, from 0 to 1, or
     * {@link #CHANCE_UNAVAILABLE} when the chances are not available: before a
     * hand is dealt and after the human has folded. The same holds for the
     * chances of tying and losing.
     */
    public double getWinChance() {
        return this.winChance;
    }

    public double getTieChance() {
        return this.tieChance;
    }

    public double getLoseChance() {
        return this.loseChance;
    }

    /**
     * Returns the name of the human's best hand, for example "Two Pair", once
     * the flop is revealed, and {@link #NONE} before.
     */
    public String getHumanHandName() {
        return this.humanHandName;
    }

    /**
     * Returns the name of the opponent's best hand after a showdown, and
     * {@link #NONE} otherwise.
     */
    public String getOpponentHandName() {
        return this.opponentHandName;
    }

    /**
     * Returns the picture ids of the cards that make the winning hand, for the
     * screen to pick out after a showdown. Each of them is also among the
     * human's cards, the opponent's cards, or the community cards of this
     * state. If one player won, these are the five cards of that player's best
     * hand; if the pot was split, they are the cards of both players' best
     * hands, each card once, so there are five or more. The list is empty
     * before a hand is dealt, while a hand is being played, and after a hand
     * that ended with a fold, and it cannot be changed.
     */
    public List<String> getWinningCards() {
        return this.winningCards;
    }

    /**
     * Returns one short sentence about what just happened, for example
     * "Opponent raises to 60." It is {@link #NONE} if nothing has happened yet.
     */
    public String getMessage() {
        return this.message;
    }

    /**
     * Returns why the last request failed, as a sentence for the user, or
     * {@link #NONE} if it did not fail. A state with an error message differs
     * from the state before it only in the revision, this message, and the
     * events, of which it has none.
     */
    public String getErrorMessage() {
        return this.errorMessage;
    }

    /**
     * Returns who chose the opponent's last action: {@link #DECIDED_BY_LLM},
     * {@link #DECIDED_BY_BUILT_IN}, or {@link #NONE} if this state does not
     * come from an action of the opponent.
     */
    public String getDecidedBy() {
        return this.decidedBy;
    }

    /**
     * Returns how many chips the human won (positive) or lost (negative) in
     * this hand once it is over, and 0 while it is being played.
     */
    public int getHumanChipChange() {
        return this.humanChipChange;
    }

    /**
     * Returns what happened between the state before this one and this state,
     * in order. Each event is in exactly one state. The list cannot be changed.
     */
    public List<TableEventView> getEvents() {
        return this.events;
    }

    /**
     * Collects the parts of a {@link TableState}. A new builder holds the state
     * of a table at which nothing has happened: revision 0, no cards, no chips,
     * no messages, and no chances.
     */
    public static final class Builder {
        private long revision;
        private int handNumber;
        private String humanName = NONE;
        private String opponentName = NONE;
        private List<String> humanCards = List.of();
        private List<String> opponentCards = List.of();
        private List<String> boardCards = List.of();
        private String street = FIRST_STREET;
        private int pot;
        private int humanChips;
        private int opponentChips;
        private int humanBet;
        private int opponentBet;
        private boolean humanDealer;
        private boolean handInProgress;
        private boolean handOver;
        private boolean matchOver;
        private String winner = NONE;
        private boolean humanToAct;
        private boolean opponentToAct;
        private boolean foldAllowed;
        private boolean checkAllowed;
        private boolean callAllowed;
        private boolean betAllowed;
        private boolean raiseAllowed;
        private int callAmount;
        private int minWager;
        private int maxWager;
        private double winChance = CHANCE_UNAVAILABLE;
        private double tieChance = CHANCE_UNAVAILABLE;
        private double loseChance = CHANCE_UNAVAILABLE;
        private String humanHandName = NONE;
        private String opponentHandName = NONE;
        private List<String> winningCards = List.of();
        private String message = NONE;
        private String errorMessage = NONE;
        private String decidedBy = NONE;
        private int humanChipChange;
        private List<TableEventView> events = List.of();

        /**
         * Creates a builder that holds the state of a table at which nothing
         * has happened.
         */
        public Builder() {
            // Every field starts at the value given where it is declared.
        }

        /**
         * Creates a builder that holds a copy of an existing state, to make a
         * state that differs from it in a few parts.
         *
         * @param state the state to start from
         */
        public Builder(TableState state) {
            this.revision = state.revision;
            this.handNumber = state.handNumber;
            this.humanName = state.humanName;
            this.opponentName = state.opponentName;
            this.humanCards = state.humanCards;
            this.opponentCards = state.opponentCards;
            this.boardCards = state.boardCards;
            this.street = state.street;
            this.pot = state.pot;
            this.humanChips = state.humanChips;
            this.opponentChips = state.opponentChips;
            this.humanBet = state.humanBet;
            this.opponentBet = state.opponentBet;
            this.humanDealer = state.humanDealer;
            this.handInProgress = state.handInProgress;
            this.handOver = state.handOver;
            this.matchOver = state.matchOver;
            this.winner = state.winner;
            this.humanToAct = state.humanToAct;
            this.opponentToAct = state.opponentToAct;
            this.foldAllowed = state.foldAllowed;
            this.checkAllowed = state.checkAllowed;
            this.callAllowed = state.callAllowed;
            this.betAllowed = state.betAllowed;
            this.raiseAllowed = state.raiseAllowed;
            this.callAmount = state.callAmount;
            this.minWager = state.minWager;
            this.maxWager = state.maxWager;
            this.winChance = state.winChance;
            this.tieChance = state.tieChance;
            this.loseChance = state.loseChance;
            this.humanHandName = state.humanHandName;
            this.opponentHandName = state.opponentHandName;
            this.winningCards = state.winningCards;
            this.message = state.message;
            this.errorMessage = state.errorMessage;
            this.decidedBy = state.decidedBy;
            this.humanChipChange = state.humanChipChange;
            this.events = state.events;
        }

        /**
         * Sets the number of the state.
         *
         * @param value the revision
         * @return this builder
         */
        public Builder setRevision(long value) {
            this.revision = value;
            return this;
        }

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
         * Sets the human's hole cards.
         *
         * @param value the picture ids of the two cards, or an empty list
         * @return this builder
         */
        public Builder setHumanCards(List<String> value) {
            this.humanCards = value;
            return this;
        }

        /**
         * Sets the opponent's hole cards.
         *
         * @param value the picture ids of the two cards, {@link CardIds#BACK}
         *        twice while they are hidden, or an empty list
         * @return this builder
         */
        public Builder setOpponentCards(List<String> value) {
            this.opponentCards = value;
            return this;
        }

        /**
         * Sets the revealed community cards.
         *
         * @param value the picture ids, in the order the cards were revealed
         * @return this builder
         */
        public Builder setBoardCards(List<String> value) {
            this.boardCards = value;
            return this;
        }

        /**
         * Sets the stage of the hand.
         *
         * @param value "PREFLOP", "FLOP", "TURN", "RIVER", or "SHOWDOWN"
         * @return this builder
         */
        public Builder setStreet(String value) {
            this.street = value;
            return this;
        }

        /**
         * Sets the chips already pulled into the middle of the table.
         *
         * @param value the whole pot less both bets of the current betting round
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
         * Sets whether the human holds the dealer button.
         *
         * @param value true if the human is the dealer
         * @return this builder
         */
        public Builder setHumanDealer(boolean value) {
            this.humanDealer = value;
            return this;
        }

        /**
         * Sets whether a hand is being played.
         *
         * @param value true if a hand has been dealt and is not over
         * @return this builder
         */
        public Builder setHandInProgress(boolean value) {
            this.handInProgress = value;
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
         * @param value {@link TableState#HUMAN}, {@link TableState#OPPONENT},
         *        {@link TableState#SPLIT}, or {@link TableState#NONE}
         * @return this builder
         */
        public Builder setWinner(String value) {
            this.winner = value;
            return this;
        }

        /**
         * Sets whether it is the human's turn.
         *
         * @param value true if the human is to act
         * @return this builder
         */
        public Builder setHumanToAct(boolean value) {
            this.humanToAct = value;
            return this;
        }

        /**
         * Sets whether it is the opponent's turn.
         *
         * @param value true if the opponent is to act
         * @return this builder
         */
        public Builder setOpponentToAct(boolean value) {
            this.opponentToAct = value;
            return this;
        }

        /**
         * Sets whether the human may fold now.
         *
         * @param value true if a fold is allowed
         * @return this builder
         */
        public Builder setCanFold(boolean value) {
            this.foldAllowed = value;
            return this;
        }

        /**
         * Sets whether the human may check now.
         *
         * @param value true if a check is allowed
         * @return this builder
         */
        public Builder setCanCheck(boolean value) {
            this.checkAllowed = value;
            return this;
        }

        /**
         * Sets whether the human may call now.
         *
         * @param value true if a call is allowed
         * @return this builder
         */
        public Builder setCanCall(boolean value) {
            this.callAllowed = value;
            return this;
        }

        /**
         * Sets whether the human may bet now.
         *
         * @param value true if a bet is allowed
         * @return this builder
         */
        public Builder setCanBet(boolean value) {
            this.betAllowed = value;
            return this;
        }

        /**
         * Sets whether the human may raise now.
         *
         * @param value true if a raise is allowed
         * @return this builder
         */
        public Builder setCanRaise(boolean value) {
            this.raiseAllowed = value;
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
         * @param value the chance, from 0 to 1, or {@link TableState#CHANCE_UNAVAILABLE}
         * @return this builder
         */
        public Builder setWinChance(double value) {
            this.winChance = value;
            return this;
        }

        /**
         * Sets the human's chance of tying.
         *
         * @param value the chance, from 0 to 1, or {@link TableState#CHANCE_UNAVAILABLE}
         * @return this builder
         */
        public Builder setTieChance(double value) {
            this.tieChance = value;
            return this;
        }

        /**
         * Sets the human's chance of losing.
         *
         * @param value the chance, from 0 to 1, or {@link TableState#CHANCE_UNAVAILABLE}
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
         * Sets the name of the opponent's best hand.
         *
         * @param value the name, for example "Flush"
         * @return this builder
         */
        public Builder setOpponentHandName(String value) {
            this.opponentHandName = value;
            return this;
        }

        /**
         * Sets the cards that make the winning hand.
         *
         * @param value the picture ids of the winner's best five cards, or of
         *        both players' best fives without repeats if the pot was
         *        split, or an empty list
         * @return this builder
         */
        public Builder setWinningCards(List<String> value) {
            this.winningCards = value;
            return this;
        }

        /**
         * Sets the sentence about what just happened.
         *
         * @param value the sentence
         * @return this builder
         */
        public Builder setMessage(String value) {
            this.message = value;
            return this;
        }

        /**
         * Sets why the last request failed.
         *
         * @param value the reason as a sentence, or {@link TableState#NONE}
         * @return this builder
         */
        public Builder setErrorMessage(String value) {
            this.errorMessage = value;
            return this;
        }

        /**
         * Sets who chose the opponent's last action.
         *
         * @param value {@link TableState#DECIDED_BY_LLM},
         *        {@link TableState#DECIDED_BY_BUILT_IN}, or {@link TableState#NONE}
         * @return this builder
         */
        public Builder setDecidedBy(String value) {
            this.decidedBy = value;
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
         * Sets what happened between the state before and this state.
         *
         * @param value the events, in order
         * @return this builder
         */
        public Builder setEvents(List<TableEventView> value) {
            this.events = value;
            return this;
        }

        /**
         * Builds the state from what has been set.
         *
         * @return the state, which cannot be changed
         * @throws IllegalArgumentException if a text or a list was set to null
         * @throws NullPointerException if a list contains null
         */
        public TableState build() {
            return new TableState(this);
        }
    }
}
