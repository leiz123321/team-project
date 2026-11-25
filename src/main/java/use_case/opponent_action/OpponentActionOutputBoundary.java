package use_case.opponent_action;

import use_case.table.TableOutputData;

/**
 * Reports the result of the computer opponent's betting action.
 */
public interface OpponentActionOutputBoundary {

    /**
     * Shows the table after the computer opponent's action was applied.
     *
     * @param outputData the table; its events are only those caused by this
     *        action, and {@code getDecidedBy()} says who chose it
     */
    void prepareSuccessView(TableOutputData outputData);

    /**
     * Reports that the request failed. Almost always the computer opponent
     * did not act and nothing about the game has changed. The one exception
     * comes right after {@link #prepareSuccessView}: the opponent acted and
     * ended the hand, the table has been shown, but the record of the hand
     * could not be saved to the history.
     *
     * @param errorMessage the reason, as a sentence for the user
     */
    void prepareFailView(String errorMessage);
}
