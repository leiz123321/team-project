package entity;

import java.util.List;

/**
 * One thing that happened during a hand, in a form a program can read: who
 * did what, with how many chips, and which cards were shown. {@link Game}
 * records one for every blind, betting action, returned bet, newly revealed
 * street, shown hand, and pot that is won or split.
 *
 * <p>What the fields hold depends on the type:
 * <ul>
 *   <li>{@code BLIND}, {@code CALL}, {@code BET}, {@code RAISE}: the amount is
 *       the chips that left the player's stack with this event, and the total
 *       is that player's whole bet on the current betting round afterwards.</li>
 *   <li>{@code FOLD}, {@code CHECK}: the amount is 0 and the total is the
 *       player's bet on the current betting round, which the event leaves as
 *       it was.</li>
 *   <li>{@code REFUND}: the amount went back to the player because the other
 *       player could not match it; the total is what is left of that player's
 *       bet on the current betting round.</li>
 *   <li>{@code FLOP}, {@code TURN}, {@code RIVER}: no player; the cards are
 *       the newly revealed community cards.</li>
 *   <li>{@code SHOWDOWN}: the cards are the player's two hole cards and the
 *       detail is the name of their best hand, for example "Full House".</li>
 *   <li>{@code WIN}: the player wins the amount, which is the whole pot.</li>
 *   <li>{@code SPLIT}: the amount is the player's share of a split pot.</li>
 * </ul>
 * Fields that a type does not use are 0, an empty list, or an empty text.
 * Cards are stored as text such as "A♠", so an event does not change when the
 * same cards are dealt again in a later hand.
 */
public final class HandEvent {
    private final HandEventType type;
    private final String playerName;
    private final int amount;
    private final int total;
    private final List<String> cards;
    private final String detail;

    /**
     * Creates an event.
     *
     * @param type the kind of event
     * @param playerName the name of the player the event is about, or null if
     *        no player is involved
     * @param amount the chips that moved with this event
     * @param total the player's whole bet on the current betting round after
     *        the event
     * @param cards the cards shown by this event, as text
     * @param detail extra text about the event; empty if there is none
     * @throws IllegalArgumentException if type, cards, or detail is null, or
     *         amount or total is negative
     */
    public HandEvent(HandEventType type, String playerName, int amount, int total,
                     List<String> cards, String detail) {
        if (type == null || cards == null || detail == null) {
            throw new IllegalArgumentException("type, cards, and detail must not be null");
        }
        if (amount < 0 || total < 0) {
            throw new IllegalArgumentException("amount and total must not be negative");
        }
        this.type = type;
        this.playerName = playerName;
        this.amount = amount;
        this.total = total;
        this.cards = List.copyOf(cards);
        this.detail = detail;
    }

    public HandEventType getType() {
        return this.type;
    }

    /**
     * Returns the name of the player this event is about, or null if no player
     * is involved.
     */
    public String getPlayerName() {
        return this.playerName;
    }

    public int getAmount() {
        return this.amount;
    }

    public int getTotal() {
        return this.total;
    }

    /**
     * Returns the cards shown by this event as text, as a list that cannot be
     * changed. It is empty if the event shows no cards.
     */
    public List<String> getCards() {
        return this.cards;
    }

    /**
     * Returns the extra text of this event, or an empty text if there is none.
     */
    public String getDetail() {
        return this.detail;
    }
}
