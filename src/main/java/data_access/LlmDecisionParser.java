package data_access;

import java.math.BigDecimal;

import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONParserConfiguration;

import entity.Action;
import entity.ActionType;
import entity.DecisionContext;
import use_case.opponent_action.OpponentUnavailableException;

/**
 * Turns the model's answer into an {@link Action}.
 *
 * <p>The answer must be one JSON object of the form
 * {@code {"action":"RAISE","amount":120,"reason":"..."}}. White space around
 * it is allowed, and so is a code fence around it, because models often add
 * one; anything else around it, or JSON that is not strictly valid, is not.
 * The reason is not used.
 *
 * <p>An action type that is not legal this turn is refused, so that the
 * built-in strategy decides instead. The amount of a legal bet or raise is
 * moved into the legal range when it lies outside it: a model that asks for
 * too little or too much still means to bet.
 */
final class LlmDecisionParser {
    private static final String FENCE = "```";
    private static final JSONParserConfiguration STRICT = new JSONParserConfiguration().withStrictMode();
    private static final BigDecimal LARGEST_INT = BigDecimal.valueOf(Integer.MAX_VALUE);
    private static final BigDecimal SMALLEST_INT = BigDecimal.valueOf(Integer.MIN_VALUE);

    private LlmDecisionParser() {
    }

    /**
     * Reads the action out of the model's answer.
     *
     * @param answer the text the model wrote
     * @param context the turn the answer is for
     * @return the action; its type is legal for the context, and the amount of
     *         a bet or a raise is within the legal range
     * @throws OpponentUnavailableException if the answer is not one JSON
     *         object, names no action type or one that does not exist or is
     *         not legal, or gives no whole number to bet or raise to
     */
    static Action parse(String answer, DecisionContext context) throws OpponentUnavailableException {
        JSONObject decision = readObject(withoutCodeFence(answer));
        ActionType type = readActionType(decision);
        if (!context.isLegal(type)) {
            throw new OpponentUnavailableException("The model chose " + type + ", which is not legal now.");
        }
        int amount = 0;
        if (type == ActionType.BET || type == ActionType.RAISE) {
            int asked = readAmount(decision);
            amount = Math.min(Math.max(asked, context.getMinWager()), context.getMaxWager());
            if (amount <= 0) {
                throw new OpponentUnavailableException("There is no legal amount to bet or raise to.");
            }
        }
        return Action.of(type, amount);
    }

    /**
     * Removes a code fence around the text: the three backticks before it,
     * with a language name if there is one, and the three backticks after it.
     * Text without a complete fence is returned as it is, less the white space
     * around it.
     *
     * @param answer the text the model wrote
     * @return the text inside the fence, or the whole text if there is no fence
     */
    private static String withoutCodeFence(String answer) {
        String text = answer.strip();
        if (text.length() >= 2 * FENCE.length() && text.startsWith(FENCE) && text.endsWith(FENCE)) {
            String inside = text.substring(FENCE.length(), text.length() - FENCE.length());
            int start = 0;
            while (start < inside.length() && Character.isLetterOrDigit(inside.charAt(start))) {
                start++;
            }
            text = inside.substring(start).strip();
        }
        return text;
    }

    private static JSONObject readObject(String text) throws OpponentUnavailableException {
        try {
            return new JSONObject(text, STRICT);
        }
        catch (JSONException exception) {
            throw new OpponentUnavailableException("The model's answer is not a single JSON object.");
        }
    }

    private static ActionType readActionType(JSONObject decision) throws OpponentUnavailableException {
        Object name = decision.opt("action");
        if (!(name instanceof String)) {
            throw new OpponentUnavailableException("The model's answer names no action.");
        }
        try {
            return ActionType.valueOf((String) name);
        }
        catch (IllegalArgumentException exception) {
            throw new OpponentUnavailableException("The model's answer names an action that does not exist.");
        }
    }

    /**
     * Reads the amount, which must be a whole number. A number too large or
     * too small for an int becomes the largest or the smallest int, which the
     * caller then moves into the legal range like any other amount.
     *
     * @param decision the model's answer
     * @return the amount to bet or raise to that the model asked for
     * @throws OpponentUnavailableException if the answer gives no whole number
     */
    private static int readAmount(JSONObject decision) throws OpponentUnavailableException {
        Object value = decision.opt("amount");
        if (!(value instanceof Number)) {
            throw new OpponentUnavailableException("The model's answer gives no amount to bet or raise to.");
        }
        try {
            BigDecimal number = new BigDecimal(value.toString());
            int amount;
            if (number.compareTo(LARGEST_INT) > 0) {
                amount = Integer.MAX_VALUE;
            }
            else if (number.compareTo(SMALLEST_INT) < 0) {
                amount = Integer.MIN_VALUE;
            }
            else {
                amount = number.intValueExact();
            }
            return amount;
        }
        catch (ArithmeticException | NumberFormatException exception) {
            throw new OpponentUnavailableException("The model's amount to bet or raise to is not a whole number.");
        }
    }
}
