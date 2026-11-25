package data_access;

import entity.Match;
import use_case.opponent_action.OpponentActionGameDataAccessInterface;
import use_case.player_action.PlayerActionGameDataAccessInterface;
import use_case.start_hand.StartHandGameDataAccessInterface;

/**
 * Holds the match being played while the program is running. Nothing is
 * written to disk: the match is gone when the program ends.
 *
 * <p>The three use cases of the game share one object of this class, so they
 * all see the same match.
 */
public class InMemoryGameSessionDataAccessObject implements StartHandGameDataAccessInterface,
        PlayerActionGameDataAccessInterface, OpponentActionGameDataAccessInterface {

    // The use cases may run on a worker thread while another thread asks whether a match exists.
    private volatile Match match;

    /**
     * Returns the match being played.
     *
     * @return the match, or null if none has been saved yet
     */
    @Override
    public Match getMatch() {
        return this.match;
    }

    /**
     * Keeps the match, in place of any match kept before.
     *
     * @param match the match being played; null means there is no match any more
     */
    @Override
    public void saveMatch(Match match) {
        this.match = match;
    }
}
