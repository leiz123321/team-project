package use_case.player_action;

import entity.HandRecord;

/**
 * Where the use case "Take a betting action" keeps the record of a hand that
 * the human player's action has finished.
 */
public interface PlayerActionHistoryDataAccessInterface {

    /**
     * Adds the record of a finished hand to the history.
     *
     * @param record the record to keep
     */
    void save(HandRecord record);
}
