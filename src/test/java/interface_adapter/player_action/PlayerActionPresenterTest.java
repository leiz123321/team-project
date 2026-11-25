package interface_adapter.player_action;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import entity.Action;
import entity.Game;
import entity.Match;
import interface_adapter.table.CardIds;
import interface_adapter.table.TableEventView;
import interface_adapter.table.TableHarness;
import interface_adapter.table.TableState;
import interface_adapter.table.TableStateAssertions;
import interface_adapter.table.TableViewModel;
import use_case.table.TableFixtures;
import use_case.table.TableOutputData;

class PlayerActionPresenterTest {
    private static final double EXACT = 0.0;
    private static final int MOST_STEPS = 20;

    private TableHarness table;

    @BeforeEach
    void setUp() {
        this.table = new TableHarness(42);
        this.table.getStartHand().execute(false);
    }

    @Test
    void needsAViewModel() {
        assertThrows(IllegalArgumentException.class, () -> new PlayerActionPresenter(null));
    }

    @Test
    void showsTheHumansCall() {
        this.table.getPlayerAction().call();

        TableState state = this.table.getState();
        assertEquals(2, state.getRevision());
        assertEquals("You call 5.", state.getMessage());
        assertEquals(1, state.getEvents().size());
        assertEquals("CALL", state.getEvents().get(0).getType());
        assertEquals("HUMAN", state.getEvents().get(0).getActor());
        assertEquals("You call 5", state.getEvents().get(0).getText());
        assertEquals(10, state.getHumanBet());
        assertEquals(10, state.getOpponentBet());
        assertEquals(0, state.getPot());
        assertEquals(990, state.getHumanChips());
        assertTrue(state.isOpponentToAct());
        assertFalse(state.isHumanToAct());
        assertFalse(state.canFold());
        assertEquals("", state.getDecidedBy());
        assertEquals("", state.getErrorMessage());
    }

    @Test
    void showsTheHumansRaise() {
        this.table.getPlayerAction().raise(60);

        TableState state = this.table.getState();
        assertEquals("You raise to 60.", state.getMessage());
        assertEquals(60, state.getHumanBet());
        assertEquals(940, state.getHumanChips());
        assertEquals(55, state.getEvents().get(0).getAmount());
        assertEquals(60, state.getEvents().get(0).getTotal());
    }

    @Test
    void showsAnIllegalActionAsAnErrorOnTheUnchangedTable() {
        TableState before = this.table.getState();

        this.table.getPlayerAction().raise(5);

        TableState after = this.table.getState();
        TableStateAssertions.assertFailureOf(before, after, "A raise must be between 20 and 1000.");
        assertEquals("Hand 1 — you post the small blind of 5.", after.getMessage());
        assertTrue(after.isHumanToAct());
        assertTrue(after.canRaise());
    }

    @Test
    void dropsTheErrorOnceTheHumanActsLegally() {
        this.table.getPlayerAction().check();
        assertEquals("You cannot check: it is 5 to call.", this.table.getState().getErrorMessage());

        this.table.getPlayerAction().call();

        assertEquals("", this.table.getState().getErrorMessage());
        assertEquals(3, this.table.getState().getRevision());
    }

    @Test
    void showsTheEndOfTheHandWhenTheHumanFolds() {
        this.table.getPlayerAction().fold();

        TableState state = this.table.getState();
        assertEquals("You fold and Opponent wins 15.", state.getMessage());
        assertTrue(state.isHandOver());
        assertFalse(state.isHandInProgress());
        assertFalse(state.isMatchOver());
        assertEquals("OPPONENT", state.getWinner());
        assertEquals(-5, state.getHumanChipChange());
        assertEquals(List.of("back", "back"), state.getOpponentCards());
        assertEquals("", state.getOpponentHandName());
        assertEquals(-1.0, state.getWinChance(), EXACT);
        assertFalse(state.isHumanToAct());
        assertFalse(state.isOpponentToAct());
        assertEquals(0, state.getPot());
        assertEquals(0, state.getHumanBet());
        assertEquals(0, state.getOpponentBet());
        assertEquals(995, state.getHumanChips());
        assertEquals(1005, state.getOpponentChips());
        assertEquals(List.of("You fold", "Opponent wins 15"),
                List.of(state.getEvents().get(0).getText(), state.getEvents().get(1).getText()));
    }

    @Test
    void pullsTheBetsIntoTheMiddleWhenTheBettingRoundEnds() {
        this.table.getPlayerAction().fold();
        this.table.getStartHand().execute(false);
        // The opponent, small blind in the second hand, calls; the human's check ends the round.
        this.table.getMatch().getCurrentGame().apply(Action.call());

        this.table.getPlayerAction().check();

        TableState state = this.table.getState();
        assertEquals("FLOP", state.getStreet());
        assertEquals(20, state.getPot());
        assertEquals(0, state.getHumanBet());
        assertEquals(0, state.getOpponentBet());
        assertEquals(3, state.getBoardCards().size());
        assertEquals(CardIds.toIds(TableFixtures.texts(
                this.table.getMatch().getCurrentGame().getBoard().getRevealedCards())), state.getBoardCards());
        assertEquals(2, state.getEvents().size());
        assertEquals("You check", state.getEvents().get(0).getText());
        assertEquals("FLOP", state.getEvents().get(1).getType());
        assertEquals(state.getBoardCards(), state.getEvents().get(1).getCards());
        assertTrue(state.getEvents().get(1).getText().startsWith("Flop: "));
        assertEquals("You check.", state.getMessage());
        assertFalse(state.getHumanHandName().isEmpty());
        assertTrue(state.isHumanToAct());
        assertTrue(state.canCheck());
        assertTrue(state.canBet());
    }

    @Test
    void picksOutTheWinningCardsWhenTheHumansCheckLeadsToAShowdown() {
        Game game = this.table.getMatch().getCurrentGame();
        this.table.setOpponentAnswer(Action.check());
        this.table.getPlayerAction().call();
        for (int step = 0; !this.table.getState().isHandOver(); step++) {
            assertTrue(step < MOST_STEPS, "the hand does not come to an end");
            assertEquals(List.of(), this.table.getState().getWinningCards());
            if (this.table.getState().isHumanToAct()) {
                this.table.getPlayerAction().check();
            }
            else {
                this.table.getOpponentAction().execute();
            }
        }

        // In the first hand the human has the last word on the river, so this state is the human's.
        TableState state = this.table.getState();
        assertEquals("SHOWDOWN", state.getStreet());
        assertEquals("You check", state.getEvents().get(0).getText());
        List<String> faceUp = new ArrayList<>(state.getHumanCards());
        faceUp.addAll(state.getOpponentCards());
        faceUp.addAll(state.getBoardCards());
        assertTrue(state.getWinningCards().size() >= 5);
        assertTrue(faceUp.containsAll(state.getWinningCards()));
        if (game.getWinner() != null) {
            assertEquals(CardIds.toIds(TableFixtures.texts(game.getBestFive(game.getWinner()))),
                    state.getWinningCards());
        }
    }

    @Test
    void showsTheBoardRunOutWhenTheHumanCallsTheBigBlindWithTheirLastChips() {
        // Two folded hands later the human is the small blind again, and is left with exactly 10 chips.
        this.table.getPlayerAction().fold();
        this.table.getStartHand().execute(false);
        this.table.setOpponentAnswer(Action.fold());
        this.table.getOpponentAction().execute();
        Match match = this.table.getMatch();
        match.getHuman().bet(990);
        match.getComputer().win(990);
        this.table.getStartHand().execute(false);
        assertTrue(this.table.getState().isHumanToAct());
        assertTrue(this.table.getState().canCall());
        assertFalse(this.table.getState().canRaise());
        assertEquals(5, this.table.getState().getHumanChips());

        this.table.getPlayerAction().call();

        // The opponent, who has nobody left to bet against, is not asked to check: the hand is over.
        TableState state = this.table.getState();
        assertTrue(state.isHandOver());
        assertFalse(state.isHandInProgress());
        assertFalse(state.isOpponentToAct());
        assertEquals("SHOWDOWN", state.getStreet());
        assertEquals(5, state.getBoardCards().size());
        assertEquals("You call 5 and are all in", state.getEvents().get(0).getText());
        List<String> types = new ArrayList<>();
        for (TableEventView event : state.getEvents()) {
            types.add(event.getType());
        }
        assertEquals(List.of("CALL", "FLOP", "TURN", "RIVER", "SHOWDOWN", "SHOWDOWN"), types.subList(0, 6));
        assertFalse(state.getOpponentCards().contains("back"));
        assertFalse(state.getWinningCards().isEmpty());
        assertEquals(2000, state.getHumanChips() + state.getOpponentChips());
        assertEquals(3, this.table.getRecords().size());
    }

    @Test
    void publishesEachViewItIsAskedFor() {
        TableViewModel viewModel = new TableViewModel();
        PlayerActionPresenter presenter = new PlayerActionPresenter(viewModel);

        presenter.prepareSuccessView(new TableOutputData.Builder().setStreet("TURN").setHandNumber(4).build());
        assertEquals(1, viewModel.getState().getRevision());
        assertEquals("TURN", viewModel.getState().getStreet());

        presenter.prepareFailView("It is not your turn.");
        assertEquals(2, viewModel.getState().getRevision());
        assertEquals("It is not your turn.", viewModel.getState().getErrorMessage());
        assertEquals("TURN", viewModel.getState().getStreet());
    }
}
