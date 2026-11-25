package data_access;

/**
 * What came back from one HTTP request: the status code and the body.
 */
final class LlmHttpReply {
    private final int statusCode;
    private final String body;

    /**
     * Creates a reply.
     *
     * @param statusCode the HTTP status code
     * @param body the text of the reply; null is taken as no text
     */
    LlmHttpReply(int statusCode, String body) {
        this.statusCode = statusCode;
        if (body == null) {
            this.body = "";
        }
        else {
            this.body = body;
        }
    }

    int getStatusCode() {
        return this.statusCode;
    }

    /**
     * Returns the text of the reply, which is empty if there was none.
     */
    String getBody() {
        return this.body;
    }
}
