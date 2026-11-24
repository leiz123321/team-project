package use_case.start_hand;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class StartHandInputDataTest {

    @Test
    void saysWhetherANewMatchIsWanted() {
        assertTrue(new StartHandInputData(true).isNewMatch());
        assertFalse(new StartHandInputData(false).isNewMatch());
    }
}
