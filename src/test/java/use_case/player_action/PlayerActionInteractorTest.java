package use_case.player_action;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Random;
import java.util.function.Supplier;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import data_access.InMemoryGameSessionDataAccessObject;
import entity.Action;
import entity.ActionType;
import entity.EquityCalculator;
import entity.Game;
import entity.HandRecord;
import entity.Match;
import entity.Street;
import use_case.table.SamplingEquityCalculator;
import use_case.table.TableEventData;
import use_case.table.TableFixtures;
import use_case.table.TableOutputData;

class PlayerActionInteractorTest {
    private static final Instant NOW = Instant.parse("2026-11-13T02:15:30Z");
    private static final Clock CLOCK = Clock.fixed(NOW, ZoneOffset.UTC);

    private InMemoryGameSessionDataAccessObject session;
    private RecordingHistory history;
    private RecordingPresenter presenter;
    private CountingIds ids;
    private PlayerActionInteractor interactor;

    @BeforeEach
    void setUp() {
        this.session = new InMemoryGameSessionDataAccessObject();
        this.history = new RecordingHistory();
        this.presenter = new RecordingPresenter();
        this.ids = new CountingIds();
        this.interactor = new PlayerActionInteractor(this.session, this.history, this.presenter, this.ids, CLOCK);
    }

    @Test
    void rejectsMissingCollaborators() {
        EquityCalculator calculator = new EquityCalculator();

        assertThrows(IllegalArgumentException.class,
                () -> new PlayerActionInteractor(null, this.history, this.presenter, this.ids, CLOCK));
        assertThrows(IllegalArgumentException.class,
                () -> new PlayerActionInteractor(this.session, null, this.presenter, this.ids, CLOCK));
        assertThrows(IllegalArgumentException.class,
                () -> new PlayerActionInteractor(this.session, this.history, null, this.ids, CLOCK));
        assertThrows(IllegalArgumentException.class,
                () -> new PlayerActionInteractor(this.session, this.history, this.presenter, null, CLOCK));
        assertThrows(IllegalArgumentException.class,
                () -> new PlayerActionInteractor(this.session, this.history, this.presenter, this.ids, null));
        assertThrows(IllegalArgumentException.class,
                () -> new PlayerActionInteractor(this.session, this.history, this.presenter, this.ids, CLOCK,
                        null));
        assertNotNull(new PlayerActionInteractor(this.session, this.history, this.presenter, this.ids, CLOCK,
                calculator));
    }

    @Test
    void failsWhenThereIsNoMatch() {
        this.interactor.execute(new PlayerActionInputData("FOLD", 0));

        assertFailedWith("There is no hand in progress.");
    }

    @Test
    void failsWhenNoHandHasBeenDealt() {
        this.session.saveMatch(TableFixtures.newMatch());

        this.interactor.execute(new PlayerActionInputData("FOLD", 0));

        assertFailedWith("There is no hand in progress.");
    }

    @Test
    void failsWhenTheHandIsAlreadyOver() {
        Match match = deal();
        match.getCurrentGame().apply(Action.fold());

        this.interactor.execute(new PlayerActionInputData("CALL", 0));

        assertFailedWith("There is no hand in progress.");
        assertEquals(List.of(), this.history.records);
    }

    @Test
    void failsWhenItIsTheOpponentsTurn() {
        Match match = deal();
        match.getCurrentGame().apply(Action.call());

        assertChangesNothing(match, new PlayerActionInputData("FOLD", 0), "It is not your turn.");
    }

    @Test
    void failsOnATextThatIsNotAnAction() {
        Match match = deal();

        assertChangesNothing(match, new PlayerActionInputData("DANCE", 0),
                "Unknown action \"DANCE\". Choose fold, check, call, bet, or raise.");
        assertChangesNothing(match, new PlayerActionInputData(null, 0),
                "Unknown action \"\". Choose fold, check, call, bet, or raise.");
    }

    @Test
    void failsOnACheckWhenThereIsSomethingToCall() {
        Match match = deal();

        assertChangesNothing(match, new PlayerActionInputData("CHECK", 0), "You cannot check: it is 5 to call.");
    }

    @Test
    void statesWhatACallCostsWhenTheBetIsLargerThanTheHumansStack() {
        Match match = TableFixtures.shortHumanMatch(11, 200);
        this.session.saveMatch(match);
        Game game = match.getCurrentGame();
        game.apply(Action.raise(20));
        game.apply(Action.raise(510));

        // The raise asks for 490 more, but the human has only 180 left: that is what a call costs.
        assertEquals(490, game.getAmountToCall());
        assertEquals(180, match.getHuman().getChips());
        assertChangesNothing(match, new PlayerActionInputData("CHECK", 0), "You cannot check: it is 180 to call.");

        this.interactor.execute(new PlayerActionInputData("CALL", 0));
        assertEquals("success", this.presenter.lastCall());
        assertEquals(180, this.presenter.lastTable.getEvents().get(0).getAmount());
    }

    @Test
    void namesTheActionInARefusalTheSameWayWhateverTheLanguageOfTheComputer() {
        Locale usual = Locale.getDefault();
        // Turkish writes the small letter of I without its dot.
        Locale.setDefault(Locale.forLanguageTag("tr-TR"));
        try {
            Match match = deal();
            assertChangesNothing(match, new PlayerActionInputData("RAISE", 19),
                    "A raise must be between 20 and 1000.");

            Game game = match.getCurrentGame();
            game.apply(Action.call());
            game.apply(Action.raise(1000));
            assertChangesNothing(match, new PlayerActionInputData("RAISE", 1000),
                    "You cannot raise now. You can only call or fold.");
        }
        finally {
            Locale.setDefault(usual);
        }
    }

    @Test
    void writesTheNumbersInARefusalTheSameWayWhateverTheLanguageOfTheComputer() {
        Locale usual = Locale.getDefault();
        // Arabic has digits of its own, and a number formatted for that language is written in them.
        Locale.setDefault(Locale.forLanguageTag("ar-SA-u-nu-arab"));
        try {
            Match match = deal();
            assertChangesNothing(match, new PlayerActionInputData("CHECK", 0), "You cannot check: it is 5 to call.");
            assertChangesNothing(match, new PlayerActionInputData("RAISE", 19),
                    "A raise must be between 20 and 1000.");

            Game game = match.getCurrentGame();
            game.apply(Action.call());
            game.apply(Action.raise(990));
            assertChangesNothing(match, new PlayerActionInputData("RAISE", 995),
                    "A raise must be exactly 1000: all your chips.");
        }
        finally {
            Locale.setDefault(usual);
        }
    }

    @Test
    void failsOnABetWhenThereIsAlreadyABet() {
        Match match = deal();

        assertChangesNothing(match, new PlayerActionInputData("BET", 50),
                "There is already a bet. You can raise instead.");
    }

    @Test
    void failsOnACallOrARaiseWhenNobodyHasBet() {
        Match match = dealToTheHumanOnTheFlop();

        assertChangesNothing(match, new PlayerActionInputData("CALL", 0),
                "There is nothing to call. You can check.");
        assertChangesNothing(match, new PlayerActionInputData("RAISE", 40),
                "There is no bet to raise. You can bet instead.");
    }

    @Test
    void statesTheAllowedRangeOfARaise() {
        Match match = deal();
        String range = "A raise must be between 20 and 1000.";

        assertChangesNothing(match, new PlayerActionInputData("RAISE", 19), range);
        assertChangesNothing(match, new PlayerActionInputData("RAISE", 1001), range);
        assertChangesNothing(match, new PlayerActionInputData("RAISE", 0), range);
        assertChangesNothing(match, new PlayerActionInputData("RAISE", -40), range);
    }

    @Test
    void statesTheAllowedRangeOfABet() {
        Match match = dealToTheHumanOnTheFlop();
        String range = "A bet must be between 10 and 990.";

        assertChangesNothing(match, new PlayerActionInputData("BET", 9), range);
        assertChangesNothing(match, new PlayerActionInputData("BET", 991), range);
    }

    @Test
    void statesTheOnlyAllowedRaiseWhenTheHumanCanOnlyGoAllIn() {
        Match match = deal();
        Game game = match.getCurrentGame();
        game.apply(Action.call());
        // The opponent keeps 10 chips back, so the human may still raise, but only by putting everything in.
        game.apply(Action.raise(990));

        assertChangesNothing(match, new PlayerActionInputData("RAISE", 995),
                "A raise must be exactly 1000: all your chips.");

        this.interactor.execute(new PlayerActionInputData("RAISE", 1000));
        assertEquals("success", this.presenter.lastCall());
        assertEquals(0, this.presenter.lastTable.getHumanChips());
        assertEquals(1000, this.presenter.lastTable.getHumanBet());
    }

    @Test
    void failsOnAnyBetOrRaiseWhenTheOpponentIsAllIn() {
        Match match = deal();
        Game game = match.getCurrentGame();
        game.apply(Action.call());
        game.apply(Action.raise(1000));

        assertChangesNothing(match, new PlayerActionInputData("RAISE", 1000),
                "You cannot raise now. You can only call or fold.");
        assertChangesNothing(match, new PlayerActionInputData("BET", 100),
                "You cannot bet now. You can only call or fold.");
    }

    @Test
    void appliesACallAndReportsOnlyItsEvent() {
        deal();

        this.interactor.execute(new PlayerActionInputData("CALL", 0));

        assertEquals(List.of("success"), this.presenter.calls);
        TableOutputData table = this.presenter.lastTable;
        assertEquals(1, table.getEvents().size());
        TableEventData call = table.getEvents().get(0);
        assertEquals("CALL", call.getType());
        assertEquals("HUMAN", call.getActor());
        assertEquals(5, call.getAmount());
        assertEquals(10, call.getTotal());
        assertEquals(990, table.getHumanChips());
        assertEquals(20, table.getPot());
        assertEquals("OPPONENT", table.getToAct());
        assertEquals("", table.getDecidedBy());
        assertFalse(table.isHandOver());
        assertEquals(List.of(), this.history.records);
    }

    @Test
    void appliesARaiseToTheGivenTotal() {
        deal();

        this.interactor.execute(new PlayerActionInputData("RAISE", 40));

        TableOutputData table = this.presenter.lastTable;
        assertEquals(1, table.getEvents().size());
        assertEquals("RAISE", table.getEvents().get(0).getType());
        assertEquals(35, table.getEvents().get(0).getAmount());
        assertEquals(40, table.getEvents().get(0).getTotal());
        assertEquals(40, table.getHumanBet());
        assertEquals(960, table.getHumanChips());
        assertEquals("OPPONENT", table.getToAct());
    }

    @Test
    void appliesABetOnTheFlop() {
        dealToTheHumanOnTheFlop();

        this.interactor.execute(new PlayerActionInputData("BET", 15));

        TableOutputData table = this.presenter.lastTable;
        assertEquals("success", this.presenter.lastCall());
        assertEquals("BET", table.getEvents().get(0).getType());
        assertEquals(15, table.getEvents().get(0).getTotal());
        assertEquals(35, table.getPot());
        assertEquals(15, table.getHumanBet());
    }

    @Test
    void ignoresTheAmountOfAFoldACheckAndACall() {
        deal();

        this.interactor.execute(new PlayerActionInputData("CALL", 777));

        assertEquals(List.of("success"), this.presenter.calls);
        assertEquals(5, this.presenter.lastTable.getEvents().get(0).getAmount());
    }

    @Test
    void readsTheActionWhateverItsCaseAndSpacing() {
        deal();

        this.interactor.execute(new PlayerActionInputData(" call ", 0));

        assertEquals(List.of("success"), this.presenter.calls);
        assertEquals("CALL", this.presenter.lastTable.getEvents().get(0).getType());
    }

    @Test
    void reportsTheNewStreetWhenTheActionEndsTheBettingRound() {
        Match match = TableFixtures.secondHandMatch(7);
        this.session.saveMatch(match);
        // The opponent is the small blind in the second hand and calls; the human has the last word.
        match.getCurrentGame().apply(Action.call());

        this.interactor.execute(new PlayerActionInputData("CHECK", 0));

        TableOutputData table = this.presenter.lastTable;
        assertEquals("FLOP", table.getStreet());
        assertEquals(2, table.getEvents().size());
        assertEquals("CHECK", table.getEvents().get(0).getType());
        assertEquals("FLOP", table.getEvents().get(1).getType());
        assertEquals(table.getBoardCards(), table.getEvents().get(1).getCards());
        assertEquals("HUMAN", table.getToAct());
        assertEquals(List.of("FOLD", "CHECK", "BET"), table.getLegalActions());
    }

    @Test
    void savesARecordWhenTheHumanFolds() {
        Match match = deal();
        Game game = match.getCurrentGame();
        List<String> humanCards = TableFixtures.holeCards(match.getHuman());
        List<String> opponentCards = TableFixtures.holeCards(match.getComputer());

        this.interactor.execute(new PlayerActionInputData("FOLD", 0));

        assertEquals(List.of("success"), this.presenter.calls);
        TableOutputData table = this.presenter.lastTable;
        assertTrue(table.isHandOver());
        assertEquals("OPPONENT", table.getWinner());
        assertEquals(-5, table.getHumanChipChange());
        assertEquals(List.of(), table.getOpponentCards());
        assertEquals(2, table.getEvents().size());
        assertEquals("FOLD", table.getEvents().get(0).getType());
        assertEquals("WIN", table.getEvents().get(1).getType());

        assertEquals(1, this.history.records.size());
        HandRecord record = this.history.records.get(0);
        assertEquals("hand-1", record.getId());
        assertEquals(NOW, record.getPlayedAt());
        assertEquals(List.of("You", "Opponent"), record.getPlayerNames());
        assertEquals(List.of(String.join(" ", humanCards), String.join(" ", opponentCards)),
                record.getHoleCards());
        assertEquals(List.of(), record.getBoardCards());
        assertEquals(game.getActionLog(), record.getActions());
        assertEquals("Opponent", record.getWinnerName());
        assertEquals(List.of(-5, 5), record.getChipChanges());
    }

    @Test
    void savesARecordWhenTheHumansCheckLeadsToAShowdown() {
        Match match = deal();
        Game game = match.getCurrentGame();
        TableFixtures.checkTo(game, Street.RIVER);
        game.apply(Action.check());

        this.interactor.execute(new PlayerActionInputData("CHECK", 0));

        TableOutputData table = this.presenter.lastTable;
        assertTrue(table.isHandOver());
        assertEquals("SHOWDOWN", table.getStreet());
        assertEquals(TableFixtures.holeCards(match.getComputer()), table.getOpponentCards());
        assertEquals("CHECK", table.getEvents().get(0).getType());
        assertEquals("SHOWDOWN", table.getEvents().get(1).getType());
        assertEquals("SHOWDOWN", table.getEvents().get(2).getType());
        assertEquals(1, this.history.records.size());
        assertEquals(table.getBoardCards(), this.history.records.get(0).getBoardCards());
        assertEquals(5, this.history.records.get(0).getBoardCards().size());
    }

    @Test
    void picksOutTheWinningCardsOnlyWhenTheHumansActionLeadsToAShowdown() {
        Match match = deal();
        Game game = match.getCurrentGame();
        TableFixtures.checkTo(game, Street.RIVER);
        game.apply(Action.check());

        this.interactor.execute(new PlayerActionInputData("BET", 40));
        assertEquals(List.of(), this.presenter.lastTable.getWinningCards());
        game.apply(Action.raise(120));

        this.interactor.execute(new PlayerActionInputData("CALL", 0));

        TableOutputData table = this.presenter.lastTable;
        assertEquals("SHOWDOWN", table.getStreet());
        List<String> shown = new ArrayList<>(table.getHumanCards());
        shown.addAll(table.getOpponentCards());
        shown.addAll(table.getBoardCards());
        assertTrue(table.getWinningCards().size() >= 5);
        assertTrue(shown.containsAll(table.getWinningCards()));
        if (game.getWinner() != null) {
            assertEquals(TableFixtures.texts(game.getBestFive(game.getWinner())), table.getWinningCards());
        }
    }

    @Test
    void picksOutNoWinningCardsWhenTheHumanFoldsAfterTheFlop() {
        Match match = deal();
        Game game = match.getCurrentGame();
        TableFixtures.checkTo(game, Street.TURN);
        game.apply(Action.bet(60));

        this.interactor.execute(new PlayerActionInputData("FOLD", 0));

        TableOutputData table = this.presenter.lastTable;
        assertTrue(table.isHandOver());
        assertEquals("TURN", table.getStreet());
        assertEquals(List.of(), table.getWinningCards());
        assertEquals(List.of(), table.getOpponentCards());
    }

    @Test
    void endsTheHandWithoutAskingTheOpponentWhenTheHumanCallsTheBigBlindWithTheirLastChips() {
        // The human, small blind with exactly 10 chips, has posted 5 and has 5 left.
        Match match = TableFixtures.shortSmallBlindMatch(11, true);
        this.session.saveMatch(match);
        Game game = match.getCurrentGame();
        assertEquals(List.of(ActionType.FOLD, ActionType.CALL), game.getLegalActionTypes());

        this.interactor.execute(new PlayerActionInputData("CALL", 0));

        // The opponent could only have checked: there is nobody left to bet against.
        assertEquals(List.of("success"), this.presenter.calls);
        TableOutputData table = this.presenter.lastTable;
        assertTrue(table.isHandOver());
        assertEquals("", table.getToAct());
        assertEquals("SHOWDOWN", table.getStreet());
        List<String> types = new ArrayList<>();
        for (TableEventData event : table.getEvents()) {
            types.add(event.getType());
        }
        assertEquals(List.of("CALL", "FLOP", "TURN", "RIVER", "SHOWDOWN", "SHOWDOWN"), types.subList(0, 6));
        assertTrue(List.of(List.of("WIN"), List.of("SPLIT", "SPLIT")).contains(types.subList(6, types.size())),
                types.toString());
        assertEquals("HUMAN", table.getEvents().get(0).getActor());
        assertEquals(5, table.getEvents().get(0).getAmount());
        assertEquals(10, table.getEvents().get(0).getTotal());
        assertEquals(5, table.getBoardCards().size());
        assertEquals(2, table.getOpponentCards().size());
        assertFalse(table.getWinningCards().isEmpty());
        assertEquals(2000, table.getHumanChips() + table.getOpponentChips());
        assertEquals("OPPONENT".equals(table.getWinner()), table.isMatchOver());
        assertEquals(1, this.history.records.size());
        assertEquals(game.getActionLog(), this.history.records.get(0).getActions());
        assertEquals(5, this.history.records.get(0).getBoardCards().size());
    }

    @Test
    void stillAsksTheHumanWhoFacesTheOpponentsAllInRaise() {
        Match match = deal();
        Game game = match.getCurrentGame();

        this.interactor.execute(new PlayerActionInputData("CALL", 0));
        game.apply(Action.raise(1000));
        this.interactor.execute(new PlayerActionInputData("CHECK", 0));

        // The check is refused, not skipped: calling or folding is the human's decision.
        assertEquals(List.of("success", "fail"), this.presenter.calls);
        assertEquals("You cannot check: it is 990 to call.", this.presenter.lastError);
        assertFalse(game.isHandOver());
        assertSame(match.getHuman(), game.getPlayerToAct());
        assertEquals(List.of(ActionType.FOLD, ActionType.CALL), game.getLegalActionTypes());
    }

    @Test
    void runsOutTheBoardWhenTheHumanCallsAnAllIn() {
        Match match = deal();
        Game game = match.getCurrentGame();
        game.apply(Action.call());
        game.apply(Action.raise(1000));

        this.interactor.execute(new PlayerActionInputData("CALL", 0));

        TableOutputData table = this.presenter.lastTable;
        assertTrue(table.isHandOver());
        assertTrue(table.isMatchOver() || "SPLIT".equals(table.getWinner()));
        List<String> types = new ArrayList<>();
        for (TableEventData event : table.getEvents()) {
            types.add(event.getType());
        }
        assertEquals(List.of("CALL", "FLOP", "TURN", "RIVER", "SHOWDOWN", "SHOWDOWN"), types.subList(0, 6));
        assertEquals(2000, table.getHumanChips() + table.getOpponentChips());
        assertEquals(1, this.history.records.size());
    }

    @Test
    void givesEveryRecordItsOwnId() {
        Match match = deal();
        this.interactor.execute(new PlayerActionInputData("FOLD", 0));
        match.startNextHand(new Random(3));
        match.getCurrentGame().apply(Action.call());

        this.interactor.execute(new PlayerActionInputData("FOLD", 0));

        assertEquals(2, this.history.records.size());
        assertEquals("hand-1", this.history.records.get(0).getId());
        assertEquals("hand-2", this.history.records.get(1).getId());
    }

    @Test
    void stillReportsTheTableWhenTheRecordCannotBeSaved() {
        deal();
        this.history.failing = true;

        this.interactor.execute(new PlayerActionInputData("FOLD", 0));

        // The fold happened, so the table comes first; the trouble with the history after it.
        assertEquals(List.of("success", "fail"), this.presenter.calls);
        assertTrue(this.presenter.lastTable.isHandOver());
        assertEquals("The hand is over, but it could not be saved to the history.", this.presenter.lastError);
    }

    @Test
    void worksOutTheReportedChancesWithTheGivenCalculator() {
        SamplingEquityCalculator calculator = new SamplingEquityCalculator();
        PlayerActionInteractor sampling = new PlayerActionInteractor(this.session, this.history,
                this.presenter, this.ids, CLOCK, calculator);
        deal();

        sampling.execute(new PlayerActionInputData("CALL", 0));

        assertEquals(1, calculator.getCalls());
    }

    private Match deal() {
        Match match = TableFixtures.dealtMatch(11);
        this.session.saveMatch(match);
        return match;
    }

    /**
     * Deals the first hand and plays it to the flop, where the opponent, who
     * is first to act, checks. The human may then check or bet.
     */
    private Match dealToTheHumanOnTheFlop() {
        Match match = deal();
        Game game = match.getCurrentGame();
        TableFixtures.checkTo(game, Street.FLOP);
        game.apply(Action.check());
        return match;
    }

    private void assertFailedWith(String message) {
        assertEquals(List.of("fail"), this.presenter.calls);
        assertEquals(message, this.presenter.lastError);
    }

    /**
     * Runs the request and checks that it fails with the given message and
     * leaves the game, the history, and the table untouched.
     */
    private void assertChangesNothing(Match match, PlayerActionInputData request, String message) {
        Game game = match.getCurrentGame();
        int humanChips = match.getHuman().getChips();
        int opponentChips = match.getComputer().getChips();
        int pot = game.getPot();
        int events = game.getEvents().size();
        Object toAct = game.getPlayerToAct();
        int successes = this.presenter.count("success");
        int failures = this.presenter.count("fail");

        this.interactor.execute(request);

        assertEquals(message, this.presenter.lastError);
        assertEquals("fail", this.presenter.lastCall());
        assertEquals(failures + 1, this.presenter.count("fail"));
        assertEquals(successes, this.presenter.count("success"));
        assertEquals(humanChips, match.getHuman().getChips());
        assertEquals(opponentChips, match.getComputer().getChips());
        assertEquals(pot, game.getPot());
        assertEquals(events, game.getEvents().size());
        assertEquals(toAct, game.getPlayerToAct());
        assertFalse(game.isHandOver());
        assertEquals(List.of(), this.history.records);
    }

    /**
     * Remembers what the interactor reported, and in which order.
     */
    private static final class RecordingPresenter implements PlayerActionOutputBoundary {
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

        private String lastCall() {
            return this.calls.get(this.calls.size() - 1);
        }

        private int count(String call) {
            int count = 0;
            for (String made : this.calls) {
                if (made.equals(call)) {
                    count++;
                }
            }
            return count;
        }
    }

    /**
     * A history kept in a list, which can be told to fail.
     */
    private static final class RecordingHistory implements PlayerActionHistoryDataAccessInterface {
        private final List<HandRecord> records = new ArrayList<>();
        private boolean failing;

        @Override
        public void save(HandRecord record) {
            if (this.failing) {
                throw new IllegalStateException("the history cannot be written");
            }
            this.records.add(record);
        }
    }

    /**
     * Gives the ids "hand-1", "hand-2", and so on.
     */
    private static final class CountingIds implements Supplier<String> {
        private int count;

        @Override
        public String get() {
            this.count++;
            return "hand-" + this.count;
        }
    }
}
