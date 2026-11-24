package use_case.start_hand;

import entity.Match;

/**
 * Where the use case "Start a new hand" finds and keeps the match being
 * played.
 */
public interface StartHandGameDataAccessInterface {

    /**
     * Returns the match being played.
     *
     * @return the match, or null if none has been saved yet
     */
    Match getMatch();

    /**
     * Keeps the match so that the other use cases of the game can find it.
     *
     * @param match the match being played
     */
    void saveMatch(Match match);
}
