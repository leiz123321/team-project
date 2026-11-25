package use_case.opponent_action;

import entity.Action;
import entity.BotProfile;
import entity.DecisionContext;

/**
 * An outside source of the computer opponent's decisions, such as an LLM API.
 * The interactor does not trust the answer: it checks that the action is
 * legal, and uses the built-in strategy when it is not or when no answer
 * comes.
 */
public interface OpponentActionDecisionDataAccessInterface {

    /**
     * Asks for the computer opponent's next action.
     *
     * @param context what the computer player can see, and nothing else
     * @param profile the playing style the opponent should follow
     * @return the chosen action; it may be null or illegal
     * @throws OpponentUnavailableException if no decision could be obtained,
     *         for example because the service is not configured, cannot be
     *         reached, took too long, or gave an answer that cannot be read
     */
    Action decide(DecisionContext context, BotProfile profile) throws OpponentUnavailableException;
}
