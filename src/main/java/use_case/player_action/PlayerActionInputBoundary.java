package use_case.player_action;

/**
 * What a controller calls to run the use case "Take a betting action".
 */
public interface PlayerActionInputBoundary {

    /**
     * Applies the human player's action to the hand in progress. The result is
     * reported through the output boundary.
     *
     * @param inputData the kind of action and its amount
     */
    void execute(PlayerActionInputData inputData);
}
