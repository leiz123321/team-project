package entity;

/**
 * Where a card currently is during a hand. A card moves from one state to the
 * next as it is dealt and revealed.
 */
public enum CardState {

    /** Still in the deck; not dealt. */
    IN_DECK,

    /** Dealt to a player as one of their hole cards. */
    IN_HAND,

    /** Dealt as a community card but not turned face up yet. */
    COMMUNITY_HIDDEN,

    /** A community card that has been turned face up. */
    COMMUNITY_REVEALED
}
