package interface_adapter.start_hand;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import entity.Action;
import entity.Game;
import entity.Match;
import interface_adapter.table.CardIds;
import interface_adapter.table.TableHarness;
import interface_adapter.table.TableState;
import interface_adapter.table.TableStateAssertions;
import interface_adapter.table.TableViewModel;
import use_case.table.TableFixtures;
import use_case.table.TableOutputData;

class StartHandPresenterTest {
    private static final int MOST_STEPS = 200;

    private TableHarness table;

    @BeforeEach
    void setUp() {
        this.table = new TableHarness(42);
    }

    @Test
    void needsAViewModel() {
        assertThrows(IllegalArgumentException.class, () -> new StartHandPresenter(null));
    }

    @Test
    void showsTheNewHand() {
        this.table.getStartHand().execute(false);

        TableState state = this.table.getState();
        Match match = this.table.getMatch();
        assertEquals(1, state.getRevision());
        assertEquals(1, state.getHandNumber());
        assertEquals("You", state.getHumanName());
        assertEquals("Opponent", state.getOpponentName());
        assertEquals(CardIds.toIds(TableFixtures.holeCards(match.getHuman())), state.getHumanCards());
        assertEquals(List.of("back", "back"), state.getOpponentCards());
        assertEquals(List.of(), state.getBoardCards());
        assertEquals("PREFLOP", state.getStreet());
        assertTrue(state.isHandInProgress());
        assertFalse(state.isHandOver());
        assertFalse(state.isMatchOver());
        assertEquals("", state.getErrorMessage());
        assertEquals("", state.getDecidedBy());
        assertTrue(state.getWinChance() > 0.0 && state.getWinChance() < 1.0);
    }

    @Test
    void showsTheBlindsInFrontOfThePlayersAndNothingInTheMiddle() {
        this.table.getStartHand().execute(false);

        TableState state = this.table.getState();
        assertEquals(0, state.getPot());
        assertEquals(5, state.getHumanBet());
        assertEquals(10, state.getOpponentBet());
        assertEquals(995, state.getHumanChips());
        assertEquals(990, state.getOpponentChips());
        assertTrue(state.isHumanDealer());
        assertEquals(2, state.getEvents().size());
        assertEquals("You post the small blind of 5", state.getEvents().get(0).getText());
        assertEquals("Opponent posts the big blind of 10", state.getEvents().get(1).getText());
        assertEquals("Hand 1 — you post the small blind of 5.", state.getMessage());
    }

    @Test
    void offersTheHumanTheirFirstDecision() {
        this.table.getStartHand().execute(false);

        TableState state = this.table.getState();
        assertTrue(state.isHumanToAct());
        assertFalse(state.isOpponentToAct());
        assertTrue(state.canFold());
        assertTrue(state.canCall());
        assertTrue(state.canRaise());
        assertFalse(state.canCheck());
        assertFalse(state.canBet());
        assertEquals(5, state.getCallAmount());
        assertEquals(20, state.getMinWager());
        assertEquals(1000, state.getMaxWager());
    }

    @Test
    void leavesTheFirstDecisionOfTheSecondHandToTheOpponent() {
        this.table.getStartHand().execute(false);
        this.table.getPlayerAction().fold();

        this.table.getStartHand().execute(false);

        TableState state = this.table.getState();
        assertEquals(3, state.getRevision());
        assertEquals(2, state.getHandNumber());
        assertFalse(state.isHumanDealer());
        assertTrue(state.isOpponentToAct());
        assertFalse(state.isHumanToAct());
        assertFalse(state.canFold());
        assertEquals(10, state.getHumanBet());
        assertEquals(5, state.getOpponentBet());
        assertEquals("Hand 2 — you post the big blind of 10.", state.getMessage());
        assertEquals("Opponent posts the small blind of 5", state.getEvents().get(0).getText());
        assertEquals("You post the big blind of 10", state.getEvents().get(1).getText());
    }

    @Test
    void showsARefusalAsAnErrorOnTheUnchangedTable() {
        this.table.getStartHand().execute(false);
        TableState before = this.table.getState();

        this.table.getStartHand().execute(false);

        TableStateAssertions.assertFailureOf(before, this.table.getState(), "The current hand is not finished.");
    }

    @Test
    void showsTheFinishedMatchAndThenWhyNoHandWasDealt() {
        playUntilTheMatchIsOver();
        TableState lastHand = this.table.getState();
        assertTrue(lastHand.isMatchOver());
        int statesBefore = this.table.getStates().size();

        this.table.getStartHand().execute(false);

        // Two states: the table of the finished match, then the same table with the reason.
        assertEquals(statesBefore + 2, this.table.getStates().size());
        TableState finished = this.table.getStates().get(statesBefore);
        TableState refused = this.table.getStates().get(statesBefore + 1);
        boolean humanWon = lastHand.getHumanChips() > lastHand.getOpponentChips();
        String winner = "Opponent";
        String addressed = "Opponent";
        if (humanWon) {
            winner = "You";
            addressed = "you";
        }
        assertEquals(lastHand.getRevision() + 1, finished.getRevision());
        assertTrue(finished.isMatchOver());
        assertTrue(finished.isHandOver());
        assertFalse(finished.isHandInProgress());
        assertEquals(List.of(), finished.getEvents());
        assertEquals("", finished.getErrorMessage());
        assertEquals("The match is over — " + addressed + " won.", finished.getMessage());
        assertEquals(lastHand.getHumanChips(), finished.getHumanChips());
        assertEquals(lastHand.getOpponentCards(), finished.getOpponentCards());
        TableStateAssertions.assertFailureOf(finished, refused, "The match is over. " + winner + " won.");
    }

    @Test
    void showsAFreshTableWhenANewMatchIsStarted() {
        playUntilTheMatchIsOver();

        this.table.getStartHand().execute(true);

        TableState state = this.table.getState();
        assertEquals(1, state.getHandNumber());
        assertFalse(state.isMatchOver());
        assertTrue(state.isHandInProgress());
        assertEquals(995, state.getHumanChips());
        assertEquals(990, state.getOpponentChips());
        assertEquals(List.of("back", "back"), state.getOpponentCards());
        assertEquals("", state.getWinner());
        assertEquals(0, state.getHumanChipChange());
    }

    @Test
    void publishesEachViewItIsAskedFor() {
        TableViewModel viewModel = new TableViewModel();
        StartHandPresenter presenter = new StartHandPresenter(viewModel);
        TableOutputData finished = new TableOutputData.Builder().setStreet("SHOWDOWN").setHandOver(true)
                .setMatchOver(true).setHumanChips(2000).build();

        presenter.prepareSuccessView(new TableOutputData.Builder().setStreet("PREFLOP").setHandNumber(1).build());
        assertEquals(1, viewModel.getState().getRevision());
        assertEquals(1, viewModel.getState().getHandNumber());

        presenter.prepareMatchOverView(finished);
        assertEquals(2, viewModel.getState().getRevision());
        assertTrue(viewModel.getState().isMatchOver());

        presenter.prepareFailView("The match is over. You won.");
        assertEquals(3, viewModel.getState().getRevision());
        assertEquals("The match is over. You won.", viewModel.getState().getErrorMessage());
        assertTrue(viewModel.getState().isMatchOver());
    }

    /**
     * Plays hands in which both players put everything in before the flop
     * until one of them has nothing left.
     */
    private void playUntilTheMatchIsOver() {
        this.table.getStartHand().execute(false);
        for (int step = 0; !this.table.getState().isMatchOver(); step++) {
            assertTrue(step < MOST_STEPS, "the match does not come to an end");
            if (this.table.getState().isHandOver()) {
                this.table.getStartHand().execute(false);
            }
            else if (this.table.getState().isHumanToAct() && this.table.getState().canRaise()) {
                this.table.getPlayerAction().raise(this.table.getState().getMaxWager());
            }
            else if (this.table.getState().isHumanToAct()) {
                this.table.getPlayerAction().call();
            }
            else {
                this.table.setOpponentAnswer(allInOrCall());
                this.table.getOpponentAction().execute();
            }
        }
    }

    private Action allInOrCall() {
        Game game = this.table.getMatch().getCurrentGame();
        Action allIn = Action.raise(game.getMaxWager());
        if (game.isLegal(allIn)) {
            return allIn;
        }
        return Action.call();
    }
}
