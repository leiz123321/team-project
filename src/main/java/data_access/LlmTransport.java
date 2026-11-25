package data_access;

import java.io.IOException;
import java.util.Map;

/**
 * Sends one HTTP request for {@link LlmOpponentDataAccessObject}. The real
 * implementation, {@link LlmHttpTransport}, uses the network; tests put a
 * scripted one in its place.
 */
interface LlmTransport {

    /**
     * Posts a JSON text and waits for the whole reply. A reply with an error
     * status is returned like any other; only a request that gets no complete
     * reply fails.
     *
     * @param url the address to post to
     * @param headers the request headers, by name
     * @param body the JSON text to send
     * @return the status code and the body of the reply
     * @throws java.net.http.HttpTimeoutException if no complete reply came in
     *         time
     * @throws IOException if the request could not be sent or the reply could
     *         not be read
     */
    LlmHttpReply post(String url, Map<String, String> headers, String body) throws IOException;
}
