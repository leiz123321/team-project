package interface_adapter.player_action;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import entity.ActionType;
import use_case.player_action.PlayerActionInputBoundary;
import use_case.player_action.PlayerActionInputData;

class PlayerActionControllerTest {
    private List<String> requests;
    private PlayerActionController controller;

    @BeforeEach
    void setUp() {
        this.requests = new ArrayList<>();
        PlayerActionInputBoundary interactor = (PlayerActionInputData inputData) ->
                this.requests.add(inputData.getActionType() + " " + inputData.getAmount());
        this.controller = new PlayerActionController(interactor);
    }

    @Test
    void needsAUseCaseToRun() {
        assertThrows(IllegalArgumentException.class, () -> new PlayerActionController(null));
    }

    @Test
    void passesOnTheActionAndItsAmount() {
        this.controller.execute("RAISE", 60);
        this.controller.execute("CALL", 0);

        assertEquals(List.of("RAISE 60", "CALL 0"), this.requests);
    }

    @Test
    void hasAShortcutForEachKindOfAction() {
        this.controller.fold();
        this.controller.check();
        this.controller.call();
        this.controller.bet(30);
        this.controller.raise(90);

        assertEquals(List.of("FOLD 0", "CHECK 0", "CALL 0", "BET 30", "RAISE 90"), this.requests);
    }

    @Test
    void namesTheActionsAsTheGameDoes() {
        assertEquals(ActionType.FOLD.name(), PlayerActionController.FOLD);
        assertEquals(ActionType.CHECK.name(), PlayerActionController.CHECK);
        assertEquals(ActionType.CALL.name(), PlayerActionController.CALL);
        assertEquals(ActionType.BET.name(), PlayerActionController.BET);
        assertEquals(ActionType.RAISE.name(), PlayerActionController.RAISE);
        assertEquals(5, ActionType.values().length);
    }
}
