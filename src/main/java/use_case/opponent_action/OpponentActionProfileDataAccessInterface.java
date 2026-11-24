package use_case.opponent_action;

import entity.BotProfile;

/**
 * Provides the current playing style for the computer opponent's turn.
 */
public interface OpponentActionProfileDataAccessInterface {

    /**
     * Returns the computer opponent's current profile.
     *
     * @return the stored profile, or the default profile if none is stored;
     *         never null
     */
    BotProfile load();
}
