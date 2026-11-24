package use_case.player_action;

/**
 * The input of the use case "Take a betting action": the action the human
 * player asks for.
 */
public final class PlayerActionInputData {
    private final String actionType;
    private final int amount;

    /**
     * Creates the input.
     *
     * @param actionType the kind of action: the name of an
     *        {@code entity.ActionType}, for example "CALL"; null is treated as
     *        an empty text
     * @param amount the total to bet or raise to on the current betting
     *        round; ignored for a fold, a check, and a call
     */
    public PlayerActionInputData(String actionType, int amount) {
        if (actionType == null) {
            this.actionType = "";
        }
        else {
            this.actionType = actionType;
        }
        this.amount = amount;
    }

    /**
     * Returns the kind of action that was asked for. It is never null, but it
     * may be a text that names no kind of action.
     */
    public String getActionType() {
        return this.actionType;
    }

    /**
     * Returns the total to bet or raise to on the current betting round. It
     * means nothing for a fold, a check, or a call.
     */
    public int getAmount() {
        return this.amount;
    }
}
