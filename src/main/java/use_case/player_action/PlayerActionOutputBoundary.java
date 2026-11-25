package use_case.player_action;

import use_case.table.TableOutputData;

/**
 * How the use case "Take a betting action" reports its result.
 */
public interface PlayerActionOutputBoundary {

    /**
     * Shows the table after the human player's action was applied.
     *
     * @param outputData the table; its events are only those caused by this
     *        action
     */
    void prepareSuccessView(TableOutputData outputData);

    /**
     * Reports that the request failed. Almost always the action was not
     * applied and nothing about the game has changed. The one exception comes
     * right after {@link #prepareSuccessView}: the action was applied and
     * ended the hand, the table has been shown, but the record of the hand
     * could not be saved to the history.
     *
     * @param errorMessage the reason, as a sentence for the user
     */
    void prepareFailView(String errorMessage);
}
