package use_case.start_hand;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import data_access.InMemoryGameSessionDataAccessObject;
import entity.Action;
import entity.EquityCalculator;
import entity.Game;
import entity.Match;
import use_case.table.SamplingEquityCalculator;
import use_case.table.TableEventData;
import use_case.table.TableFixtures;
import use_case.table.TableOutputData;

class StartHandInteractorTest {
    private static final long SEED = 42;

    private InMemoryGameSessionDataAccessObject session;
    private RecordingPresenter presenter;
    private StartHandInteractor interactor;

    @BeforeEach
    void setUp() {
        this.session = new InMemoryGameSessionDataAccessObject();
        this.presenter = new RecordingPresenter();
        this.interactor = new StartHandInteractor(this.session, this.presenter, new Random(SEED));
    }

    @Test
    void rejectsMissingCollaborators() {
        Random random = new Random(SEED);
        EquityCalculator calculator = new EquityCalculator();

        assertThrows(IllegalArgumentException.class,
                () -> new StartHandInteractor(null, this.presenter, random));
        assertThrows(IllegalArgumentException.class,
                () -> new StartHandInteractor(this.session, null, random));
        assertThrows(IllegalArgumentException.class,
                () -> new StartHandInteractor(this.session, this.presenter, null));
        assertThrows(IllegalArgumentException.class,
                () -> new StartHandInteractor(this.session, this.presenter, random, null));
        assertNotNull(new StartHandInteractor(this.session, this.presenter, random, calculator));
    }

    @Test
    void createsTheMatchWithTheGameParametersWhenThereIsNone() {
        assertNull(this.session.getMatch());

        this.interactor.execute(new StartHandInputData(false));

        Match match = this.session.getMatch();
        assertNotNull(match);
        assertEquals("You", match.getHuman().getName());
        assertEquals("Opponent", match.getComputer().getName());
        assertEquals(5, match.getSmallBlind());
        assertEquals(10, match.getBigBlind());
        assertEquals(1, match.getHandNumber());
        // Both started with 1000 and have posted their blinds.
        assertEquals(995, match.getHuman().getChips());
        assertEquals(990, match.getComputer().getChips());
    }

    @Test
    void dealsTheFirstHandWithTheHumanAsSmallBlind() {
        this.interactor.execute(new StartHandInputData(false));

        assertEquals(List.of("success"), this.presenter.calls);
        TableOutputData table = this.presenter.lastTable;
        assertEquals(1, table.getHandNumber());
        assertTrue(table.isHumanSmallBlind());
        assertEquals("HUMAN", table.getToAct());
        assertEquals(List.of("FOLD", "CALL", "RAISE"), table.getLegalActions());
        assertEquals(TableFixtures.holeCards(this.session.getMatch().getHuman()), table.getHumanCards());
        assertEquals(List.of(), table.getOpponentCards());
        assertEquals(15, table.getPot());
        assertFalse(table.isHandOver());
        assertFalse(table.isMatchOver());
        assertEquals("", table.getDecidedBy());
    }

    @Test
    void reportsTheBlindsAsTheEventsOfTheNewHand() {
        this.interactor.execute(new StartHandInputData(false));

        List<TableEventData> events = this.presenter.lastTable.getEvents();
        assertEquals(2, events.size());
        assertEquals("BLIND", events.get(0).getType());
        assertEquals("HUMAN", events.get(0).getActor());
        assertEquals(5, events.get(0).getAmount());
        assertEquals("BLIND", events.get(1).getType());
        assertEquals("OPPONENT", events.get(1).getActor());
        assertEquals(10, events.get(1).getAmount());
    }

    @Test
    void startingANewMatchWhenThereIsNoneAlsoDealsTheFirstHand() {
        this.interactor.execute(new StartHandInputData(true));

        assertEquals(List.of("success"), this.presenter.calls);
        assertEquals(1, this.presenter.lastTable.getHandNumber());
    }

    @Test
    void dealsTheNextHandWithTheBlindsSwappedAndLeavesTheOpponentToAct() {
        this.interactor.execute(new StartHandInputData(false));
        Match match = this.session.getMatch();
        match.getCurrentGame().apply(Action.fold());

        this.interactor.execute(new StartHandInputData(false));

        assertEquals(List.of("success", "success"), this.presenter.calls);
        assertSame(match, this.session.getMatch());
        TableOutputData table = this.presenter.lastTable;
        assertEquals(2, table.getHandNumber());
        assertFalse(table.isHumanSmallBlind());
        // The opponent is the small blind and acts first; the use case does not act for it.
        assertEquals("OPPONENT", table.getToAct());
        assertEquals(List.of(), table.getLegalActions());
        assertEquals(2, match.getCurrentGame().getEvents().size());
        assertEquals("OPPONENT", table.getEvents().get(0).getActor());
        assertEquals("HUMAN", table.getEvents().get(1).getActor());
        // The human lost the small blind in hand 1 and has now posted the big blind.
        assertEquals(985, table.getHumanChips());
        assertEquals(1000, table.getOpponentChips());
    }

    @Test
    void refusesWhileTheCurrentHandIsNotFinished() {
        this.interactor.execute(new StartHandInputData(false));
        Game game = this.session.getMatch().getCurrentGame();

        this.interactor.execute(new StartHandInputData(false));

        assertEquals(List.of("success", "fail"), this.presenter.calls);
        assertEquals("The current hand is not finished.", this.presenter.lastError);
        assertSame(game, this.session.getMatch().getCurrentGame());
        assertEquals(1, this.session.getMatch().getHandNumber());
        assertEquals(2, game.getEvents().size());
    }

    @Test
    void startsTheMatchAgainOnRequestEvenInTheMiddleOfAHand() {
        this.interactor.execute(new StartHandInputData(false));
        Match match = this.session.getMatch();
        Game abandoned = match.getCurrentGame();
        abandoned.apply(Action.raise(400));

        this.interactor.execute(new StartHandInputData(true));

        assertEquals(List.of("success", "success"), this.presenter.calls);
        assertSame(match, this.session.getMatch());
        assertNotSame(abandoned, match.getCurrentGame());
        TableOutputData table = this.presenter.lastTable;
        assertEquals(1, table.getHandNumber());
        assertTrue(table.isHumanSmallBlind());
        assertEquals(995, table.getHumanChips());
        assertEquals(990, table.getOpponentChips());
        assertEquals(15, table.getPot());
    }

    @Test
    void reportsTheTableAndThenTheWinnerWhenTheMatchIsOver() {
        Match match = finishedMatch(TableFixtures.HUMAN_NAME);
        this.session.saveMatch(match);
        Game lastHand = match.getCurrentGame();
        int events = lastHand.getEvents().size();

        this.interactor.execute(new StartHandInputData(false));

        // The table of the finished match first, then the reason no hand was dealt.
        assertEquals(List.of("matchOver", "fail"), this.presenter.calls);
        assertEquals("The match is over. You won.", this.presenter.lastError);
        TableOutputData table = this.presenter.lastTable;
        assertTrue(table.isMatchOver());
        assertTrue(table.isHandOver());
        assertEquals("HUMAN", table.getWinner());
        assertEquals(2000, table.getHumanChips());
        assertEquals(0, table.getOpponentChips());
        assertEquals(List.of(), table.getEvents());
        // The last hand was won at a showdown, and its winning cards are still picked out.
        assertEquals(TableFixtures.texts(lastHand.getBestFive(match.getHuman())), table.getWinningCards());
        // Nothing was dealt.
        assertSame(lastHand, match.getCurrentGame());
        assertEquals(events, lastHand.getEvents().size());
        assertEquals(1, match.getHandNumber());
    }

    @Test
    void namesTheOpponentWhenTheOpponentWonTheMatch() {
        this.session.saveMatch(finishedMatch(TableFixtures.OPPONENT_NAME));

        this.interactor.execute(new StartHandInputData(false));

        assertEquals(List.of("matchOver", "fail"), this.presenter.calls);
        assertEquals("The match is over. Opponent won.", this.presenter.lastError);
        assertEquals(0, this.presenter.lastTable.getHumanChips());
        assertTrue(this.presenter.lastTable.isMatchOver());
    }

    @Test
    void startsAFreshMatchAfterOneIsOver() {
        Match match = finishedMatch(TableFixtures.OPPONENT_NAME);
        this.session.saveMatch(match);

        this.interactor.execute(new StartHandInputData(true));

        assertEquals(List.of("success"), this.presenter.calls);
        TableOutputData table = this.presenter.lastTable;
        assertFalse(table.isMatchOver());
        assertEquals(1, table.getHandNumber());
        assertEquals(995, table.getHumanChips());
        assertEquals(990, table.getOpponentChips());
        assertSame(match, this.session.getMatch());
    }

    @Test
    void dealsTheSameCardsFromTheSameSeed() {
        InMemoryGameSessionDataAccessObject otherSession = new InMemoryGameSessionDataAccessObject();
        RecordingPresenter otherPresenter = new RecordingPresenter();
        StartHandInteractor other = new StartHandInteractor(otherSession, otherPresenter, new Random(SEED));

        this.interactor.execute(new StartHandInputData(false));
        other.execute(new StartHandInputData(false));

        assertEquals(this.presenter.lastTable.getHumanCards(), otherPresenter.lastTable.getHumanCards());
        assertEquals(TableFixtures.holeCards(this.session.getMatch().getComputer()),
                TableFixtures.holeCards(otherSession.getMatch().getComputer()));
    }

    @Test
    void worksOutTheReportedChancesWithTheGivenCalculator() {
        SamplingEquityCalculator calculator = new SamplingEquityCalculator();
        StartHandInteractor sampling =
                new StartHandInteractor(this.session, this.presenter, new Random(SEED), calculator);

        sampling.execute(new StartHandInputData(false));

        assertEquals(1, calculator.getCalls());
        assertTrue(this.presenter.lastTable.getWinChance() > 0.0);
    }

    /**
     * Returns a match whose first hand ended with both players all in and the
     * named player winning everything.
     */
    private static Match finishedMatch(String winnerName) {
        long seed = TableFixtures.seedWhere(candidate -> {
            Match match = allInFirstHand(candidate);
            return match.getCurrentGame().getWinner() != null
                    && match.getCurrentGame().getWinner().getName().equals(winnerName);
        });
        return allInFirstHand(seed);
    }

    private static Match allInFirstHand(long seed) {
        Match match = TableFixtures.dealtMatch(seed);
        match.getCurrentGame().apply(Action.raise(TableFixtures.STARTING_CHIPS));
        match.getCurrentGame().apply(Action.call());
        return match;
    }

    /**
     * Remembers what the interactor reported, and in which order.
     */
    private static final class RecordingPresenter implements StartHandOutputBoundary {
        private final List<String> calls = new ArrayList<>();
        private TableOutputData lastTable;
        private String lastError;

        @Override
        public void prepareSuccessView(TableOutputData outputData) {
            this.calls.add("success");
            this.lastTable = outputData;
        }

        @Override
        public void prepareFailView(String errorMessage) {
            this.calls.add("fail");
            this.lastError = errorMessage;
        }

        @Override
        public void prepareMatchOverView(TableOutputData outputData) {
            this.calls.add("matchOver");
            this.lastTable = outputData;
        }
    }
}
