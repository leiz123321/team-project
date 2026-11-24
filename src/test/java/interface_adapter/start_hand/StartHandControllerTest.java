package interface_adapter.start_hand;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;

import use_case.start_hand.StartHandInputBoundary;
import use_case.start_hand.StartHandInputData;

class StartHandControllerTest {

    @Test
    void needsAUseCaseToRun() {
        assertThrows(IllegalArgumentException.class, () -> new StartHandController(null));
    }

    @Test
    void passesOnWhetherANewMatchIsWanted() {
        List<Boolean> requests = new ArrayList<>();
        StartHandInputBoundary interactor = (StartHandInputData inputData) -> requests.add(inputData.isNewMatch());
        StartHandController controller = new StartHandController(interactor);

        controller.execute(false);
        controller.execute(true);

        assertEquals(List.of(false, true), requests);
    }
}
