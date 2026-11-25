package use_case.player_action;

import entity.Match;

/**
 * Where the use case "Take a betting action" finds the match being played.
 */
public interface PlayerActionGameDataAccessInterface {

    /**
     * Returns the match being played.
     *
     * @return the match, or null if none has been saved yet
     */
    Match getMatch();
}
