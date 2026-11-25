package interface_adapter.start_hand;

import use_case.start_hand.StartHandInputBoundary;
import use_case.start_hand.StartHandInputData;

/**
 * Passes the user's request for a hand to the use case "Start a new hand".
 */
public class StartHandController {
    private final StartHandInputBoundary interactor;

    /**
     * Creates the controller.
     *
     * @param interactor the use case to run
     * @throws IllegalArgumentException if interactor is null
     */
    public StartHandController(StartHandInputBoundary interactor) {
        if (interactor == null) {
            throw new IllegalArgumentException("interactor must not be null");
        }
        this.interactor = interactor;
    }

    /**
     * Asks for a hand to be dealt. The result arrives through the table view
     * model.
     *
     * @param newMatch true to start a new match, with both players back at
     *        their starting chips, and deal its first hand; false to deal the
     *        next hand of the current match
     */
    public void execute(boolean newMatch) {
        this.interactor.execute(new StartHandInputData(newMatch));
    }
}
