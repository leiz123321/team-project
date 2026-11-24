package use_case.opponent_action;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;

import java.io.IOException;

import org.junit.jupiter.api.Test;

class OpponentUnavailableExceptionTest {

    @Test
    void carriesTheReason() {
        OpponentUnavailableException exception = new OpponentUnavailableException("no key configured");

        assertEquals("no key configured", exception.getMessage());
        assertNull(exception.getCause());
    }

    @Test
    void carriesTheFailureUnderneath() {
        IOException cause = new IOException("connection refused");

        OpponentUnavailableException exception = new OpponentUnavailableException("cannot be reached", cause);

        assertEquals("cannot be reached", exception.getMessage());
        assertSame(cause, exception.getCause());
    }

    @Test
    void mustBeHandledByWhoeverAsksForADecision() {
        Exception exception = new OpponentUnavailableException("took too long");

        assertFalse(exception instanceof RuntimeException);
    }
}
