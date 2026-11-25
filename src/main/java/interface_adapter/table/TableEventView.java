package interface_adapter.table;

import java.util.List;

/**
 * One thing that happened at the table, ready to be shown: who did what, with
 * how many chips, which cards became visible, and a sentence about it.
 *
 * <p>What the numbers and cards hold depends on the type:
 * <ul>
 *   <li>{@link #BLIND}, {@link #CALL}, {@link #BET}, {@link #RAISE}: the
 *       amount is the chips that left the actor's stack, and the total is the
 *       actor's whole bet on the current betting round afterwards.</li>
 *   <li>{@link #FOLD}, {@link #CHECK}: the amount is 0.</li>
 *   <li>{@link #REFUND}: the amount went back from the actor's bet to the
 *       actor's stack; the total is what is left of the bet.</li>
 *   <li>{@link #FLOP}, {@link #TURN}, {@link #RIVER}: no actor; the cards are
 *       the newly revealed community cards.</li>
 *   <li>{@link #SHOWDOWN}: the cards are the actor's two hole cards.</li>
 *   <li>{@link #WIN}, {@link #SPLIT}: the amount goes from the pot to the
 *       actor's stack.</li>
 * </ul>
 */
public final class TableEventView {

    /** The type of the event in which a player posts a blind. */
    public static final String BLIND = "BLIND";

    /** The type of the event in which a player folds. */
    public static final String FOLD = "FOLD";

    /** The type of the event in which a player checks. */
    public static final String CHECK = "CHECK";

    /** The type of the event in which a player calls. */
    public static final String CALL = "CALL";

    /** The type of the event in which a player makes the first bet of a betting round. */
    public static final String BET = "BET";

    /** The type of the event in which a player raises. */
    public static final String RAISE = "RAISE";

    /** The type of the event in which the unmatched part of a bet goes back to its owner. */
    public static final String REFUND = "REFUND";

    /** The type of the event in which the first three community cards are revealed. */
    public static final String FLOP = "FLOP";

    /** The type of the event in which the fourth community card is revealed. */
    public static final String TURN = "TURN";

    /** The type of the event in which the fifth community card is revealed. */
    public static final String RIVER = "RIVER";

    /** The type of the event in which a player shows their hole cards. */
    public static final String SHOWDOWN = "SHOWDOWN";

    /** The type of the event in which a player wins the whole pot. */
    public static final String WIN = "WIN";

    /** The type of the event in which a player receives their share of a split pot. */
    public static final String SPLIT = "SPLIT";

    private final String type;
    private final String actor;
    private final int amount;
    private final int total;
    private final List<String> cards;
    private final String text;

    /**
     * Creates an event.
     *
     * @param type the kind of event: one of the constants of this class
     * @param actor who the event is about: {@link TableState#HUMAN},
     *        {@link TableState#OPPONENT}, or {@link TableState#NONE}
     * @param amount the chips that moved with this event
     * @param total the actor's whole bet on the current betting round after
     *        the event
     * @param cards the picture ids of the cards that became visible
     * @param text a sentence about the event, without a full stop
     * @throws IllegalArgumentException if type, actor, cards, or text is null
     * @throws NullPointerException if cards contains null
     */
    public TableEventView(String type, String actor, int amount, int total, List<String> cards, String text) {
        if (type == null || actor == null || cards == null || text == null) {
            throw new IllegalArgumentException("type, actor, cards, and text must not be null");
        }
        this.type = type;
        this.actor = actor;
        this.amount = amount;
        this.total = total;
        this.cards = List.copyOf(cards);
        this.text = text;
    }

    public String getType() {
        return this.type;
    }

    /**
     * Returns who the event is about: {@link TableState#HUMAN},
     * {@link TableState#OPPONENT}, or {@link TableState#NONE} if no player is
     * involved.
     */
    public String getActor() {
        return this.actor;
    }

    public int getAmount() {
        return this.amount;
    }

    public int getTotal() {
        return this.total;
    }

    /**
     * Returns the picture ids of the cards that became visible with this
     * event, such as "AS", as a list that cannot be changed. It is empty for
     * an event that shows no cards.
     */
    public List<String> getCards() {
        return this.cards;
    }

    /**
     * Returns a sentence about the event without a full stop, for example
     * "You call 20", "Flop: A♠ 10♥ Q♦", or "Opponent shows K♣ K♦ — Full House".
     */
    public String getText() {
        return this.text;
    }
}
