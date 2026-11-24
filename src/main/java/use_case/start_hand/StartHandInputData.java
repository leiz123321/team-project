package use_case.start_hand;

/**
 * The input of the use case "Start a new hand".
 */
public final class StartHandInputData {
    private final boolean newMatch;

    /**
     * Creates the input.
     *
     * @param newMatch true to start a new match, with both players back at
     *        their starting chips, before dealing the hand; false to deal the
     *        next hand of the current match
     */
    public StartHandInputData(boolean newMatch) {
        this.newMatch = newMatch;
    }

    public boolean isNewMatch() {
        return this.newMatch;
    }
}
