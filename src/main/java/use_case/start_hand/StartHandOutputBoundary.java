package use_case.start_hand;

import use_case.table.TableOutputData;

/**
 * How the use case "Start a new hand" reports its result.
 */
public interface StartHandOutputBoundary {

    /**
     * Shows the table at the start of the new hand.
     *
     * @param outputData the table; its events are all the events of the new
     *        hand so far, which are the two blinds
     */
    void prepareSuccessView(TableOutputData outputData);

    /**
     * Reports that no hand was started. Nothing about the game has changed.
     *
     * @param errorMessage the reason, as a sentence for the user
     */
    void prepareFailView(String errorMessage);

    /**
     * Shows the table of a match that is over, when a hand was asked for but
     * the match cannot go on. The interactor calls this first and then
     * {@link #prepareFailView(String)} with a message that names the winner
     * of the match.
     *
     * @param outputData the table as the last hand left it, with
     *        {@code isMatchOver()} true
     */
    void prepareMatchOverView(TableOutputData outputData);
}
