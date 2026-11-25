package use_case.table;

import java.util.List;

/**
 * One event of a hand as it crosses the output boundary: what an
 * {@code entity.HandEvent} says, in plain types, with the player replaced by
 * their seat at the table.
 */
public final class TableEventData {
    private final String type;
    private final String actor;
    private final int amount;
    private final int total;
    private final List<String> cards;
    private final String detail;

    /**
     * Creates the description of an event.
     *
     * @param type the kind of event: the name of an {@code entity.HandEventType}
     * @param actor who the event is about: {@link TableOutputData#HUMAN},
     *        {@link TableOutputData#OPPONENT}, or {@link TableOutputData#NONE}
     *        if no player is involved
     * @param amount the chips that moved with this event
     * @param total the actor's whole bet on the current betting round after
     *        the event
     * @param cards the cards shown by this event, as text such as "A♠"
     * @param detail extra text about the event, such as the name of a shown
     *        hand; empty if there is none
     * @throws IllegalArgumentException if type, actor, cards, or detail is null
     * @throws NullPointerException if cards contains null
     */
    public TableEventData(String type, String actor, int amount, int total, List<String> cards,
                          String detail) {
        if (type == null || actor == null || cards == null || detail == null) {
            throw new IllegalArgumentException("type, actor, cards, and detail must not be null");
        }
        this.type = type;
        this.actor = actor;
        this.amount = amount;
        this.total = total;
        this.cards = List.copyOf(cards);
        this.detail = detail;
    }

    /**
     * Returns the kind of event: the name of an {@code entity.HandEventType},
     * for example "RAISE" or "FLOP".
     */
    public String getType() {
        return this.type;
    }

    /**
     * Returns who the event is about: {@link TableOutputData#HUMAN},
     * {@link TableOutputData#OPPONENT}, or {@link TableOutputData#NONE} if no
     * player is involved.
     */
    public String getActor() {
        return this.actor;
    }

    /**
     * Returns the chips that moved with this event: out of the actor's stack
     * for a blind, call, bet, or raise, and to the actor for a returned bet, a
     * win, or a share of a split pot. It is 0 for every other event.
     */
    public int getAmount() {
        return this.amount;
    }

    /**
     * Returns the actor's whole bet on the current betting round after a
     * blind, a betting action, or a returned bet. It is 0 for every other
     * event.
     */
    public int getTotal() {
        return this.total;
    }

    /**
     * Returns the cards shown by this event as text such as "A♠": the newly
     * revealed community cards of a flop, turn, or river, or the actor's hole
     * cards at a showdown. The list cannot be changed and is empty for every
     * other event.
     */
    public List<String> getCards() {
        return this.cards;
    }

    /**
     * Returns the name of the actor's best hand at a showdown, for example
     * "Full House", and an empty text for every other event.
     */
    public String getDetail() {
        return this.detail;
    }
}
