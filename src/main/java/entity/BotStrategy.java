package entity;

/**
 * The built-in computer opponent. Decides an action without any network access.
 */
public interface BotStrategy {

    /**
     * Chooses the computer opponent's next action.
     *
     * @param context what the computer player can see
     * @return a legal action
     */
    Action decide(DecisionContext context);
}
