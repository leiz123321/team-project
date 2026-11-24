package use_case.opponent_action;

import entity.HandRecord;

/**
 * Saves the hand record when the computer opponent's action ends the hand.
 */
public interface OpponentActionHistoryDataAccessInterface {

    /**
     * Adds the record of a finished hand to the history.
     *
     * @param record the record to keep
     */
    void save(HandRecord record);
}
