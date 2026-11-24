package data_access;

import java.util.List;
import java.util.Locale;
import java.util.StringJoiner;

import entity.ActionType;
import entity.BotProfile;
import entity.Card;
import entity.DecisionContext;
import entity.strategy.BotStrategyFactory;

/**
 * Writes the two texts that are sent to the model for one decision.
 *
 * <p>The system prompt says what the game is, how the opponent should play
 * (taken from the {@link BotProfile}), and what the answer must look like.
 * The user message describes the turn. It is written from the
 * {@link DecisionContext} and from nothing else, so the model can only be
 * told what the computer player is allowed to see: never the human's cards
 * and never a card that has not been revealed.
 */
final class LlmPrompt {
    private static final String LINE_BREAK = "\n";
    private static final String PARAGRAPH_BREAK = "\n\n";

    private static final String GAME = "You are the computer player in a game of heads-up no-limit Texas Hold'em"
            + " against one human, and it is your turn to act.";

    private static final String WHAT_YOU_SEE = "Each turn you are told only what you can see: your two hole cards,"
            + " the community cards revealed so far, the pot, the chips you must add to call, the legal actions,"
            + " the smallest and largest total you may bet or raise to, and your chance of winning. You cannot see"
            + " the human's cards. Every amount is a number of chips.";

    private static final String HOW_TO_CHOOSE = String.join(LINE_BREAK,
            "How to choose:",
            "- Choose exactly one of the legal actions listed for the turn. An action that is not listed is"
                    + " thrown away.",
            "- For BET and RAISE, \"amount\" is the total you will have put in during this betting round once the"
                    + " action is applied, not the chips you add on top. It must be between the smallest and"
                    + " largest total given; the largest total puts all your chips in. For FOLD, CHECK and CALL,"
                    + " \"amount\" is 0.",
            "- Your chance of winning assumes that the human holds a random hand. A human who bets or raises big"
                    + " usually holds better than that.",
            "- A call pays for itself when your chance of winning is above the break-even chance shown with the"
                    + " amount to call.",
            "- Never fold when CHECK is legal.");

    private static final String ANSWER_FORM = "Answer with one JSON object and nothing else, exactly in this form:"
            + LINE_BREAK
            + "{\"action\":\"FOLD|CHECK|CALL|BET|RAISE\","
            + "\"amount\":<total to bet or raise to, an integer; 0 if not betting>,"
            + "\"reason\":\"<at most 15 words>\"}";

    private static final String TIGHT = "Your playing style is Tight. You play few hands: you go on only with good"
            + " cards, fold weak ones without regret, and when you bet you usually have the hand to show for it.";
    private static final String AGGRESSIVE = "Your playing style is Aggressive. You keep the pressure on: you would"
            + " rather bet or raise than check or call, and you bluff when the human shows weakness.";
    private static final String BALANCED = "Your playing style is Balanced. You play by the book: you defend"
            + " against a bet with enough of your hands that bluffing you does not pay, bet strong hands for"
            + " value, and bluff in measure.";
    private static final String RANDOM = "Your playing style is Random. You are unpredictable: you vary your play"
            + " from turn to turn so that the human cannot read a pattern in it.";

    private static final double VERY_LOW_BELOW = 0.15;
    private static final double LOW_BELOW = 0.4;
    private static final double MODERATE_UP_TO = 0.6;
    private static final double HIGH_UP_TO = 0.85;

    private static final String VERY_LOW = "very low";
    private static final String VERY_LOW_ADVICE = "Bet or raise only with very strong hands, keep your bets small,"
            + " and do not bluff.";
    private static final String LOW = "low";
    private static final String LOW_ADVICE = "Prefer checking and calling, bet strong hands for value with modest"
            + " bets, and bluff rarely.";
    private static final String MODERATE = "moderate";
    private static final String MODERATE_ADVICE = "Bet your good hands, at about half to two thirds of the pot, and"
            + " bluff now and then.";
    private static final String HIGH = "high";
    private static final String HIGH_ADVICE = "Bet and raise often, at about two thirds of the pot up to the whole"
            + " pot, and bluff regularly.";
    private static final String VERY_HIGH = "very high";
    private static final String VERY_HIGH_ADVICE = "Bet and raise at most chances, the whole pot or more, and bluff"
            + " often.";

    private static final int PERCENT = 100;
    private static final int FLOP_SIZE = 3;
    private static final int TURN_SIZE = 4;
    private static final int RIVER_SIZE = 5;

    private LlmPrompt() {
    }

    /**
     * Writes the system prompt: the game, the way this opponent plays, and
     * the form of the answer. A style name that is not one of the known
     * styles is treated as the balanced style, as the built-in strategies do.
     *
     * @param profile the opponent's playing style and aggression
     * @return the system prompt
     */
    static String systemPrompt(BotProfile profile) {
        String persona = describeStyle(profile.getStyleName()) + LINE_BREAK
                + describeAggression(profile.getAggression());
        return String.join(PARAGRAPH_BREAK, GAME, persona, WHAT_YOU_SEE, HOW_TO_CHOOSE, ANSWER_FORM);
    }

    /**
     * Writes the user message: the turn as the computer player sees it. Every
     * card and every number in it comes from the context.
     *
     * @param context what the computer player can see
     * @return the user message
     */
    static String userMessage(DecisionContext context) {
        StringJoiner lines = new StringJoiner(LINE_BREAK);
        lines.add("Your hole cards: " + cardTexts(context.getHoleCards()));
        lines.add("Community cards: " + describeBoard(context.getBoardCards()));
        lines.add("Pot: " + context.getPot());
        lines.add("To call: " + describeCall(context));
        lines.add("Legal actions: " + actionNames(context.getLegalActionTypes()));
        if (context.canWager()) {
            String wager = "raise to";
            if (context.isLegal(ActionType.BET)) {
                wager = "bet";
            }
            lines.add("Smallest total you may " + wager + ": " + context.getMinWager());
            lines.add("Largest total you may " + wager + ": " + context.getMaxWager() + " (all your chips)");
        }
        lines.add("Your chance of winning: " + percent(context.getWinChance()));
        return lines.toString();
    }

    private static String describeStyle(String styleName) {
        String description = BALANCED;
        if (BotStrategyFactory.STYLE_TIGHT.equalsIgnoreCase(styleName)) {
            description = TIGHT;
        }
        else if (BotStrategyFactory.STYLE_AGGRESSIVE.equalsIgnoreCase(styleName)) {
            description = AGGRESSIVE;
        }
        else if (BotStrategyFactory.STYLE_RANDOM.equalsIgnoreCase(styleName)) {
            description = RANDOM;
        }
        return description;
    }

    private static String describeAggression(double aggression) {
        String level;
        String advice;
        if (aggression < VERY_LOW_BELOW) {
            level = VERY_LOW;
            advice = VERY_LOW_ADVICE;
        }
        else if (aggression < LOW_BELOW) {
            level = LOW;
            advice = LOW_ADVICE;
        }
        else if (aggression <= MODERATE_UP_TO) {
            level = MODERATE;
            advice = MODERATE_ADVICE;
        }
        else if (aggression <= HIGH_UP_TO) {
            level = HIGH;
            advice = HIGH_ADVICE;
        }
        else {
            level = VERY_HIGH;
            advice = VERY_HIGH_ADVICE;
        }
        return "Your aggression is " + level + " (" + Math.round(aggression * PERCENT) + " out of " + PERCENT
                + "). " + advice;
    }

    private static String describeBoard(List<Card> boardCards) {
        String description = cardTexts(boardCards);
        if (boardCards.isEmpty()) {
            description = "none yet (before the flop)";
        }
        else if (boardCards.size() == FLOP_SIZE) {
            description += " (flop)";
        }
        else if (boardCards.size() == TURN_SIZE) {
            description += " (turn)";
        }
        else if (boardCards.size() == RIVER_SIZE) {
            description += " (river)";
        }
        return description;
    }

    /**
     * Describes the chips to add for a call and, when there are any, the
     * chance of winning at which calling neither gains nor loses: the chips
     * to add as a share of the pot after the call.
     *
     * @param context what the computer player can see
     * @return the text that follows "To call:"
     */
    private static String describeCall(DecisionContext context) {
        int toCall = context.getAmountToCall();
        String description = String.valueOf(toCall);
        long potAfterCall = (long) context.getPot() + toCall;
        if (toCall > 0 && potAfterCall > 0) {
            double breakEven = (double) toCall / potAfterCall;
            description += " (a call breaks even at a " + percent(breakEven) + " chance of winning)";
        }
        return description;
    }

    private static String cardTexts(List<Card> cards) {
        StringJoiner texts = new StringJoiner(" ");
        for (Card card : cards) {
            texts.add(card.toString());
        }
        return texts.toString();
    }

    private static String actionNames(List<ActionType> types) {
        StringJoiner names = new StringJoiner(", ");
        for (ActionType type : types) {
            names.add(type.name());
        }
        return names.toString();
    }

    private static String percent(double chance) {
        return String.format(Locale.ROOT, "%.1f%%", chance * PERCENT);
    }
}
