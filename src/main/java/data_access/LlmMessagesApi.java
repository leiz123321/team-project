package data_access;

import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONStringer;
import org.json.JSONWriter;

import entity.ActionType;
import use_case.opponent_action.OpponentUnavailableException;

/**
 * The wire format of the Anthropic Messages API, which is the LLM service
 * {@link LlmOpponentDataAccessObject} talks to: where to post, which headers
 * to send, what the request body looks like, and how to find the model's
 * answer in the reply.
 *
 * <p>The request asks for a structured output: it carries a JSON schema, and
 * the service then only lets the model answer with a JSON object that fits
 * it. The schema lists the action types that are legal this turn, so an
 * answer from this service cannot name an illegal one. The model's thinking
 * is kept short ({@value #EFFORT} effort), because a person is waiting for
 * the opponent to act.
 */
final class LlmMessagesApi {
    private static final String PATH = "/v1/messages";
    private static final String API_VERSION = "2026-10-21";

    /**
     * The most tokens the model may produce. The answer itself needs far
     * fewer, but on the current models any thinking before the answer counts
     * too, and an answer cut off by the limit is lost.
     */
    private static final int MAX_TOKENS = 1024;
    private static final String EFFORT = "low";

    private static final int OK = 200;
    private static final int FIRST_STATUS_AFTER_SUCCESS = 300;
    private static final int UNAUTHORIZED = 401;
    private static final int FORBIDDEN = 403;
    private static final int NOT_FOUND = 404;
    private static final int TOO_MANY_REQUESTS = 429;
    private static final int FIRST_SERVER_ERROR = 500;

    private static final String STOPPED_BY_REFUSAL = "refusal";
    private static final String STOPPED_BY_LIMIT = "max_tokens";
    private static final String TEXT_BLOCK = "text";

    private LlmMessagesApi() {
    }

    /**
     * Returns the address requests are posted to.
     *
     * @param baseUrl the root address of the service, without a slash at the end
     * @return the address of the Messages API
     */
    static String endpoint(String baseUrl) {
        return baseUrl + PATH;
    }

    /**
     * Returns the headers of a request.
     *
     * @param apiKey the API key
     * @return the headers, by name
     */
    static Map<String, String> headers(String apiKey) {
        Map<String, String> headers = new LinkedHashMap<>();
        headers.put("content-type", "application/json");
        headers.put("anthropic-version", API_VERSION);
        headers.put("x-api-key", apiKey);
        return headers;
    }

    /**
     * Writes the body of a request for one decision.
     *
     * @param model the name of the model to ask
     * @param systemPrompt the system prompt
     * @param userMessage the description of the turn
     * @param legalActionTypes the action types the answer may name; at least one
     * @return the body, a JSON text
     */
    static String requestBody(String model, String systemPrompt, String userMessage,
                              List<ActionType> legalActionTypes) {
        JSONStringer body = new JSONStringer();
        body.object();
        body.key("model").value(model);
        body.key("max_tokens").value(MAX_TOKENS);
        body.key("system").value(systemPrompt);
        body.key("messages").array();
        body.object().key("role").value("user").key("content").value(userMessage).endObject();
        body.endArray();
        body.key("output_config").object();
        body.key("effort").value(EFFORT);
        body.key("format").object().key("type").value("json_schema").key("schema");
        writeAnswerSchema(body, legalActionTypes);
        body.endObject();
        body.endObject();
        body.endObject();
        return body.toString();
    }

    /**
     * Finds the model's answer in a reply.
     *
     * @param reply the reply of the service
     * @return the text the model wrote
     * @throws OpponentUnavailableException if the service reported an error,
     *         the reply cannot be read, the model declined to answer, its
     *         answer was cut off, or it wrote nothing
     */
    static String answerText(LlmHttpReply reply) throws OpponentUnavailableException {
        int status = reply.getStatusCode();
        if (status < OK || status >= FIRST_STATUS_AFTER_SUCCESS) {
            throw new OpponentUnavailableException(describeFailure(status));
        }
        JSONObject message;
        try {
            message = new JSONObject(reply.getBody());
        }
        catch (JSONException exception) {
            throw new OpponentUnavailableException("The LLM service sent a reply that cannot be read.");
        }
        String stopReason = message.optString("stop_reason", "");
        if (STOPPED_BY_REFUSAL.equals(stopReason)) {
            throw new OpponentUnavailableException("The model declined to answer.");
        }
        if (STOPPED_BY_LIMIT.equals(stopReason)) {
            throw new OpponentUnavailableException("The model's answer was cut off.");
        }
        JSONArray content = message.optJSONArray("content");
        if (content == null) {
            throw new OpponentUnavailableException("The LLM service sent a reply without an answer in it.");
        }
        // The answer is found by its type, not its position: thinking blocks may come before it.
        StringBuilder text = new StringBuilder();
        for (int i = 0; i < content.length(); i++) {
            JSONObject block = content.optJSONObject(i);
            if (block != null && TEXT_BLOCK.equals(block.optString("type", ""))) {
                text.append(block.optString(TEXT_BLOCK, ""));
            }
        }
        if (text.toString().isBlank()) {
            throw new OpponentUnavailableException("The model wrote no answer.");
        }
        return text.toString();
    }

    /**
     * Writes the schema of the answer: an object with exactly the three
     * entries of the answer form. The action types are written in the order
     * they are declared, so that the same turn always gives the same schema.
     *
     * @param body the request body being written, at the place of the schema
     * @param legalActionTypes the action types the answer may name
     */
    private static void writeAnswerSchema(JSONWriter body, List<ActionType> legalActionTypes) {
        Set<ActionType> legal = EnumSet.noneOf(ActionType.class);
        legal.addAll(legalActionTypes);
        body.object();
        body.key("type").value("object");
        body.key("properties").object();
        body.key("action").object().key("type").value("string").key("enum").array();
        for (ActionType type : legal) {
            body.value(type.name());
        }
        body.endArray().endObject();
        body.key("amount").object().key("type").value("integer").endObject();
        body.key("reason").object().key("type").value("string").endObject();
        body.endObject();
        body.key("required").array().value("action").value("amount").value("reason").endArray();
        body.key("additionalProperties").value(false);
        body.endObject();
    }

    /**
     * Describes an error status in words of this class only. Nothing from the
     * reply is quoted, so the description stays short and can never repeat
     * something that was sent.
     *
     * @param status the HTTP status code of the reply
     * @return a sentence that names the status and what it usually means
     */
    private static String describeFailure(int status) {
        String meaning;
        if (status == UNAUTHORIZED || status == FORBIDDEN) {
            meaning = "the API key was not accepted";
        }
        else if (status == NOT_FOUND) {
            meaning = "the model or the address was not found";
        }
        else if (status == TOO_MANY_REQUESTS) {
            meaning = "too many requests";
        }
        else if (status >= FIRST_SERVER_ERROR) {
            meaning = "the service has a problem";
        }
        else {
            meaning = "the request was not accepted";
        }
        return "The LLM service answered with HTTP " + status + ": " + meaning + ".";
    }
}
