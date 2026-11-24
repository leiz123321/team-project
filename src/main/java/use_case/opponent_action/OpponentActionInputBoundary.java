package use_case.opponent_action;

/**
 * Starts the computer opponent's next betting action.
 */
public interface OpponentActionInputBoundary {

    /**
     * Lets the computer opponent decide and take its action in the hand in
     * progress. The result is reported through the output boundary. Deciding
     * may take noticeable time.
     */
    void execute();
}
