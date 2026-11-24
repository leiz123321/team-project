package interface_adapter.player_action;

import use_case.player_action.PlayerActionInputBoundary;
import use_case.player_action.PlayerActionInputData;

/**
 * Passes the human player's betting action to the use case "Take a betting
 * action". The result arrives through the table view model.
 */
public class PlayerActionController {

    /** The action type of a fold. */
    public static final String FOLD = "FOLD";

    /** The action type of a check. */
    public static final String CHECK = "CHECK";

    /** The action type of a call. */
    public static final String CALL = "CALL";

    /** The action type of a bet. */
    public static final String BET = "BET";

    /** The action type of a raise. */
    public static final String RAISE = "RAISE";

    private static final int NO_AMOUNT = 0;

    private final PlayerActionInputBoundary interactor;

    /**
     * Creates the controller.
     *
     * @param interactor the use case to run
     * @throws IllegalArgumentException if interactor is null
     */
    public PlayerActionController(PlayerActionInputBoundary interactor) {
        if (interactor == null) {
            throw new IllegalArgumentException("interactor must not be null");
        }
        this.interactor = interactor;
    }

    /**
     * Asks for an action of the human player to be applied.
     *
     * @param actionType the kind of action: {@link #FOLD}, {@link #CHECK},
     *        {@link #CALL}, {@link #BET}, or {@link #RAISE}
     * @param amount the total to bet or raise to on the current betting
     *        round; ignored for a fold, a check, and a call
     */
    public void execute(String actionType, int amount) {
        this.interactor.execute(new PlayerActionInputData(actionType, amount));
    }

    /**
     * Asks for the human player to fold.
     */
    public void fold() {
        execute(FOLD, NO_AMOUNT);
    }

    /**
     * Asks for the human player to check.
     */
    public void check() {
        execute(CHECK, NO_AMOUNT);
    }

    /**
     * Asks for the human player to call.
     */
    public void call() {
        execute(CALL, NO_AMOUNT);
    }

    /**
     * Asks for the human player to bet.
     *
     * @param total the total to bet on the current betting round
     */
    public void bet(int total) {
        execute(BET, total);
    }

    /**
     * Asks for the human player to raise.
     *
     * @param total the total to raise to on the current betting round
     */
    public void raise(int total) {
        execute(RAISE, total);
    }
}
