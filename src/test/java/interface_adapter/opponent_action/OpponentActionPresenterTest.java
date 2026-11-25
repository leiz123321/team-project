package interface_adapter.opponent_action;

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
import entity.Street;
import interface_adapter.table.CardIds;
import interface_adapter.table.TableEventView;
import interface_adapter.table.TableHarness;
import interface_adapter.table.TableState;
import interface_adapter.table.TableStateAssertions;
import interface_adapter.table.TableViewModel;
import use_case.table.TableFixtures;
import use_case.table.TableOutputData;

class OpponentActionPresenterTest {
    private static final int MOST_STEPS = 20;

    private TableHarness table;

    @BeforeEach
    void setUp() {
        this.table = new TableHarness(42);
        this.table.getStartHand().execute(false);
    }

    @Test
    void needsAViewModel() {
        assertThrows(IllegalArgumentException.class, () -> new OpponentActionPresenter(null));
    }

    @Test
    void showsTheOpponentsRaiseAndWhoDecidedIt() {
        this.table.getPlayerAction().call();
        this.table.setOpponentAnswer(Action.raise(30));

        this.table.getOpponentAction().execute();

        TableState state = this.table.getState();
        assertEquals(3, state.getRevision());
        assertEquals("Opponent raises to 30.", state.getMessage());
        assertEquals("LLM", state.getDecidedBy());
        assertEquals(1, state.getEvents().size());
        assertEquals("Opponent raises to 30", state.getEvents().get(0).getText());
        assertEquals("OPPONENT", state.getEvents().get(0).getActor());
        assertEquals(30, state.getOpponentBet());
        assertEquals(10, state.getHumanBet());
        assertEquals(0, state.getPot());
        assertEquals(970, state.getOpponentChips());
        assertEquals(List.of("back", "back"), state.getOpponentCards());
        assertTrue(state.isHumanToAct());
        assertTrue(state.canFold());
        assertTrue(state.canCall());
        assertTrue(state.canRaise());
        assertEquals(20, state.getCallAmount());
        assertEquals(50, state.getMinWager());
        assertEquals(1000, state.getMaxWager());
    }

    @Test
    void saysWhenTheBuiltInOpponentDecided() {
        this.table.getPlayerAction().call();
        this.table.setOpponentAnswer(null);

        this.table.getOpponentAction().execute();

        TableState state = this.table.getState();
        assertEquals("BUILT_IN", state.getDecidedBy());
        assertEquals("OPPONENT", state.getEvents().get(0).getActor());
        assertTrue(state.getMessage().startsWith("Opponent "));
    }

    @Test
    void forgetsWhoDecidedOnceTheHumanHasActed() {
        this.table.getPlayerAction().call();
        this.table.setOpponentAnswer(Action.raise(30));
        this.table.getOpponentAction().execute();

        this.table.getPlayerAction().call();

        assertEquals("", this.table.getState().getDecidedBy());
    }

    @Test
    void showsARefusalAsAnErrorOnTheUnchangedTable() {
        TableState before = this.table.getState();

        this.table.getOpponentAction().execute();

        TableStateAssertions.assertFailureOf(before, this.table.getState(), "It is not the opponent's turn.");
    }

    @Test
    void turnsTheOpponentsCardsOverOnlyAtTheShowdown() {
        // In the second hand the opponent is the small blind and has the last word on the river.
        this.table.getPlayerAction().fold();
        this.table.getStartHand().execute(false);
        Game game = this.table.getMatch().getCurrentGame();
        List<String> opponentCards = CardIds.toIds(TableFixtures.holeCards(this.table.getMatch().getComputer()));
        this.table.setOpponentAnswer(Action.call());
        this.table.getOpponentAction().execute();
        this.table.setOpponentAnswer(Action.check());
        for (int step = 0; game.getStreet() != Street.RIVER; step++) {
            assertTrue(step < MOST_STEPS, "the hand does not reach the river");
            assertEquals(List.of("back", "back"), this.table.getState().getOpponentCards());
            if (this.table.getState().isHumanToAct()) {
                this.table.getPlayerAction().check();
            }
            else {
                this.table.getOpponentAction().execute();
            }
        }
        this.table.getPlayerAction().check();
        assertEquals(List.of("back", "back"), this.table.getState().getOpponentCards());
        assertEquals("", this.table.getState().getOpponentHandName());
        assertEquals(List.of(), this.table.getState().getWinningCards());

        this.table.getOpponentAction().execute();

        TableState state = this.table.getState();
        assertTrue(state.isHandOver());
        assertEquals("SHOWDOWN", state.getStreet());
        assertEquals(opponentCards, state.getOpponentCards());
        assertFalse(state.getOpponentHandName().isEmpty());
        // The cards of the winning hand are picked out among the cards now face up.
        List<String> faceUp = new ArrayList<>(state.getHumanCards());
        faceUp.addAll(state.getOpponentCards());
        faceUp.addAll(state.getBoardCards());
        assertTrue(state.getWinningCards().size() >= 5);
        assertTrue(faceUp.containsAll(state.getWinningCards()));
        if (game.getWinner() != null) {
            assertEquals(CardIds.toIds(TableFixtures.texts(game.getBestFive(game.getWinner()))),
                    state.getWinningCards());
        }
        assertEquals(5, state.getBoardCards().size());
        assertEquals("Opponent checks", state.getEvents().get(0).getText());
        assertEquals("SHOWDOWN", state.getEvents().get(1).getType());
        assertEquals(opponentCards, state.getEvents().get(1).getCards());
        assertEquals("SHOWDOWN", state.getEvents().get(2).getType());
        assertEquals(state.getHumanCards(), state.getEvents().get(2).getCards());
        assertEquals(2, this.table.getRecords().size());
    }

    @Test
    void showsTheBoardRunOutWhenTheOpponentCallsTheBigBlindWithItsLastChips() {
        // Before the second hand, in which it is the small blind, the opponent is left with exactly 10 chips.
        this.table.getPlayerAction().fold();
        Match match = this.table.getMatch();
        match.getComputer().bet(995);
        match.getHuman().win(995);
        this.table.getStartHand().execute(false);
        assertTrue(this.table.getState().isOpponentToAct());
        assertEquals(5, this.table.getState().getOpponentChips());
        this.table.setOpponentAnswer(Action.call());

        this.table.getOpponentAction().execute();

        // The human, who has nobody left to bet against, is not asked to check: the hand is over.
        TableState state = this.table.getState();
        assertTrue(state.isHandOver());
        assertFalse(state.isHandInProgress());
        assertFalse(state.isHumanToAct());
        assertFalse(state.canCheck());
        assertEquals("SHOWDOWN", state.getStreet());
        assertEquals(5, state.getBoardCards().size());
        assertEquals("Opponent calls 5 and is all in", state.getEvents().get(0).getText());
        List<String> types = new ArrayList<>();
        for (TableEventView event : state.getEvents()) {
            types.add(event.getType());
        }
        assertEquals(List.of("CALL", "FLOP", "TURN", "RIVER", "SHOWDOWN", "SHOWDOWN"), types.subList(0, 6));
        assertFalse(state.getOpponentCards().contains("back"));
        assertFalse(state.getWinningCards().isEmpty());
        assertEquals(2000, state.getHumanChips() + state.getOpponentChips());
        assertEquals(2, this.table.getRecords().size());
    }

    @Test
    void sharesOneSequenceOfStatesWithTheOtherPresenters() {
        this.table.getPlayerAction().call();
        this.table.setOpponentAnswer(Action.raise(30));
        this.table.getOpponentAction().execute();
        this.table.getPlayerAction().check();
        this.table.getOpponentAction().execute();
        this.table.getPlayerAction().fold();
        this.table.getStartHand().execute(false);

        List<TableState> states = this.table.getStates();
        assertEquals(7, states.size());
        for (int i = 0; i < states.size(); i++) {
            assertEquals(i + 1, states.get(i).getRevision());
        }
        assertEquals("You cannot check: it is 20 to call.", states.get(3).getErrorMessage());
        assertEquals("It is not the opponent's turn.", states.get(4).getErrorMessage());
        assertEquals("", states.get(5).getErrorMessage());
        assertEquals(2, states.get(6).getHandNumber());
    }

    @Test
    void publishesEachViewItIsAskedFor() {
        TableViewModel viewModel = new TableViewModel();
        OpponentActionPresenter presenter = new OpponentActionPresenter(viewModel);

        presenter.prepareSuccessView(new TableOutputData.Builder().setStreet("RIVER")
                .setDecidedBy(TableOutputData.DECIDED_BY_BUILT_IN).build());
        assertEquals(1, viewModel.getState().getRevision());
        assertEquals("BUILT_IN", viewModel.getState().getDecidedBy());

        presenter.prepareFailView("There is no hand in progress.");
        assertEquals(2, viewModel.getState().getRevision());
        assertEquals("There is no hand in progress.", viewModel.getState().getErrorMessage());
        assertEquals("BUILT_IN", viewModel.getState().getDecidedBy());
    }
}
