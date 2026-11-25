package interface_adapter.opponent_action;

import use_case.opponent_action.OpponentActionInputBoundary;

/**
 * Runs the computer's response during betting. The game screen
 * calls it whenever the table says it is the opponent's turn.
 */
public class OpponentActionController {
    private final OpponentActionInputBoundary interactor;

    /**
     * Creates the controller.
     *
     * @param interactor the interactor for the computer's turn
     * @throws IllegalArgumentException if interactor is null
     */
    public OpponentActionController(OpponentActionInputBoundary interactor) {
        if (interactor == null) {
            throw new IllegalArgumentException("interactor must not be null");
        }
        this.interactor = interactor;
    }

    /**
     * Asks for the computer opponent to take its turn. The result arrives
     * through the table view model. The opponent may take noticeable time to
     * decide, so this must not be called on the thread that draws the screen.
     */
    public void execute() {
        this.interactor.execute();
    }
}
