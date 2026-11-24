package use_case.player_action;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class PlayerActionInputDataTest {

    @Test
    void holdsTheActionAndItsAmount() {
        PlayerActionInputData inputData = new PlayerActionInputData("RAISE", 60);

        assertEquals("RAISE", inputData.getActionType());
        assertEquals(60, inputData.getAmount());
    }

    @Test
    void treatsAMissingActionAsAnEmptyText() {
        PlayerActionInputData inputData = new PlayerActionInputData(null, 0);

        assertEquals("", inputData.getActionType());
        assertEquals(0, inputData.getAmount());
    }
}
