package use_case.opponent_action;

import entity.Match;

/**
 * Provides the active match for the computer opponent's turn.
 */
public interface OpponentActionGameDataAccessInterface {

    /**
     * Returns the match being played.
     *
     * @return the match, or null if none has been saved yet
     */
    Match getMatch();
}
