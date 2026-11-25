package use_case.start_hand;

/**
 * What a controller calls to run the use case "Start a new hand".
 */
public interface StartHandInputBoundary {

    /**
     * Starts the next hand of the match, or the first hand of a new match.
     * The result is reported through the output boundary.
     *
     * @param inputData whether to start a new match first
     */
    void execute(StartHandInputData inputData);
}
