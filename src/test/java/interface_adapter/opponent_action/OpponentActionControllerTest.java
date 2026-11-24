package interface_adapter.opponent_action;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

import use_case.opponent_action.OpponentActionInputBoundary;

class OpponentActionControllerTest {

    @Test
    void needsAUseCaseToRun() {
        assertThrows(IllegalArgumentException.class, () -> new OpponentActionController(null));
    }

    @Test
    void runsTheUseCaseOncePerRequest() {
        int[] runs = new int[1];
        OpponentActionInputBoundary interactor = () -> runs[0]++;
        OpponentActionController controller = new OpponentActionController(interactor);

        controller.execute();
        controller.execute();

        assertEquals(2, runs[0]);
    }
}
