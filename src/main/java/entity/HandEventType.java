package entity;

/**
 * The kinds of event that {@link Game} records while a hand is played.
 *
 * <p>The five betting kinds, {@code FOLD} to {@code RAISE}, have the same
 * names as the constants of {@link ActionType}.
 */
public enum HandEventType {

    /** A player posts the small or the big blind. */
    BLIND,

    /** A player gives up the hand. */
    FOLD,

    /** A player passes without betting. */
    CHECK,

    /** A player matches the current bet, or puts in all their chips if they have fewer. */
    CALL,

    /** A player makes the first bet of a betting round. */
    BET,

    /** A player increases the current bet. */
    RAISE,

    /** The part of a bet that the other player could not match goes back to its owner. */
    REFUND,

    /** The first three community cards are revealed. */
    FLOP,

    /** The fourth community card is revealed. */
    TURN,

    /** The fifth community card is revealed. */
    RIVER,

    /** A player shows their hole cards at the end of the hand. */
    SHOWDOWN,

    /** A player wins the whole pot. */
    WIN,

    /** A player receives their share of a pot that is split. */
    SPLIT
}
