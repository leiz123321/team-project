package use_case.opponent_action;

/**
 * Thrown by a source of the computer opponent's decisions when it cannot
 * decide. The opponent interactor then uses its built-in strategy.
 */
public class OpponentUnavailableException extends Exception {
    private static final long serialVersionUID = 1L;

    /**
     * Creates the exception.
     *
     * @param message why no decision could be obtained
     */
    public OpponentUnavailableException(String message) {
        super(message);
    }

    /**
     * Creates the exception.
     *
     * @param message why no decision could be obtained
     * @param cause the failure underneath, such as a network error
     */
    public OpponentUnavailableException(String message, Throwable cause) {
        super(message, cause);
    }
}
