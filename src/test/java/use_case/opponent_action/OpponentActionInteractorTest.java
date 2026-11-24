package use_case.opponent_action;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.function.Supplier;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import data_access.InMemoryGameSessionDataAccessObject;
import entity.Action;
import entity.ActionType;
import entity.BotProfile;
import entity.BotStrategy;
import entity.Card;
import entity.DecisionContext;
import entity.EquityCalculator;
import entity.EquityResult;
import entity.Game;
import entity.HandRecord;
import entity.Match;
import entity.Street;
import entity.strategy.BalancedStrategy;
import entity.strategy.BotStrategyFactory;
import use_case.table.SamplingEquityCalculator;
import use_case.table.TableEventData;
import use_case.table.TableFixtures;
import use_case.table.TableOutputData;

class OpponentActionInteractorTest {
    private static final Instant NOW = Instant.parse("2026-11-13T02:15:30Z");
    private static final Clock CLOCK = Clock.fixed(NOW, ZoneOffset.UTC);
    private static final Supplier<String> IDS = () -> "hand-id";
    private static final long SEED = 5;
    private static final double EXACT = 0.0;
    private static final String HAND_MOVED_ON =
            "The hand moved on while the opponent was deciding, so its move was not played.";

    private InMemoryGameSessionDataAccessObject session;
    private RecordingHistory history;
    private StubProfiles profiles;
    private ScriptedDecisions decisions;
    private RecordingPresenter presenter;
    private OpponentActionInteractor interactor;

    @BeforeEach
    void setUp() {
        this.session = new InMemoryGameSessionDataAccessObject();
        this.history = new RecordingHistory();
        this.profiles = new StubProfiles();
        this.decisions = new ScriptedDecisions();
        this.presenter = new RecordingPresenter();
        this.interactor = new OpponentActionInteractor(this.session, this.history, this.profiles,
                this.decisions, this.presenter, new Random(SEED), IDS, CLOCK);
    }

    @Test
    void rejectsMissingCollaborators() {
        Random random = new Random(SEED);

        assertThrows(IllegalArgumentException.class, () -> new OpponentActionInteractor(null, this.history,
                this.profiles, this.decisions, this.presenter, random, IDS, CLOCK));
        assertThrows(IllegalArgumentException.class, () -> new OpponentActionInteractor(this.session, null,
                this.profiles, this.decisions, this.presenter, random, IDS, CLOCK));
        assertThrows(IllegalArgumentException.class, () -> new OpponentActionInteractor(this.session,
                this.history, null, this.decisions, this.presenter, random, IDS, CLOCK));
        assertThrows(IllegalArgumentException.class, () -> new OpponentActionInteractor(this.session,
                this.history, this.profiles, null, this.presenter, random, IDS, CLOCK));
        assertThrows(IllegalArgumentException.class, () -> new OpponentActionInteractor(this.session,
                this.history, this.profiles, this.decisions, null, random, IDS, CLOCK));
        assertThrows(IllegalArgumentException.class, () -> new OpponentActionInteractor(this.session,
                this.history, this.profiles, this.decisions, this.presenter, null, IDS, CLOCK));
        assertThrows(IllegalArgumentException.class, () -> new OpponentActionInteractor(this.session,
                this.history, this.profiles, this.decisions, this.presenter, random, null, CLOCK));
        assertThrows(IllegalArgumentException.class, () -> new OpponentActionInteractor(this.session,
                this.history, this.profiles, this.decisions, this.presenter, random, IDS, null));
        assertThrows(IllegalArgumentException.class, () -> new OpponentActionInteractor(this.session,
                this.history, this.profiles, this.decisions, this.presenter, random, IDS, CLOCK, null));
        assertNotNull(new OpponentActionInteractor(this.session, this.history, this.profiles,
                this.decisions, this.presenter, random, IDS, CLOCK, new EquityCalculator()));
    }

    @Test
    void failsWhenThereIsNoHandInProgress() {
        this.interactor.execute();
        assertEquals("There is no hand in progress.", this.presenter.lastError);

        this.session.saveMatch(TableFixtures.newMatch());
        this.interactor.execute();

        Match match = dealToTheOpponent();
        match.getCurrentGame().apply(Action.fold());
        this.interactor.execute();

        assertEquals(List.of("fail", "fail", "fail"), this.presenter.calls);
        assertEquals("There is no hand in progress.", this.presenter.lastError);
        assertEquals(0, this.decisions.asked);
        assertEquals(List.of(), this.history.records);
    }

    @Test
    void failsWhenItIsTheHumansTurn() {
        Match match = TableFixtures.dealtMatch(11);
        this.session.saveMatch(match);

        this.interactor.execute();

        assertEquals(List.of("fail"), this.presenter.calls);
        assertEquals("It is not the opponent's turn.", this.presenter.lastError);
        assertEquals(0, this.decisions.asked);
        assertEquals(2, match.getCurrentGame().getEvents().size());
        assertSame(match.getHuman(), match.getCurrentGame().getPlayerToAct());
    }

    @Test
    void appliesALegalAnswerOfTheDecisionSource() {
        dealToTheOpponent();
        this.decisions.answer = Action.raise(30);

        this.interactor.execute();

        assertEquals(List.of("success"), this.presenter.calls);
        TableOutputData table = this.presenter.lastTable;
        assertEquals("LLM", table.getDecidedBy());
        assertEquals(1, table.getEvents().size());
        TableEventData raise = table.getEvents().get(0);
        assertEquals("RAISE", raise.getType());
        assertEquals("OPPONENT", raise.getActor());
        assertEquals(20, raise.getAmount());
        assertEquals(30, raise.getTotal());
        assertEquals(30, table.getOpponentBet());
        assertEquals(970, table.getOpponentChips());
        assertEquals("HUMAN", table.getToAct());
        assertEquals(List.of(), table.getOpponentCards());
        assertEquals(List.of(), this.history.records);
    }

    @Test
    void showsTheDecisionSourceOnlyWhatTheOpponentCanSee() {
        Match match = dealToTheOpponent();
        Game game = match.getCurrentGame();
        this.decisions.answer = Action.check();

        this.interactor.execute();

        DecisionContext context = this.decisions.lastContext;
        assertEquals(match.getComputer().getHoleCards().getCards(), context.getHoleCards());
        assertEquals(List.of(), context.getBoardCards());
        assertEquals(20, context.getPot());
        assertEquals(0, context.getAmountToCall());
        assertEquals(List.of(ActionType.FOLD, ActionType.CHECK, ActionType.RAISE), context.getLegalActionTypes());
        assertEquals(20, context.getMinWager());
        assertEquals(1000, context.getMaxWager());
        assertSame(this.profiles.profile, this.decisions.lastProfile);

        // The check ended the betting round, so the opponent, first to act after the flop, decides again.
        this.interactor.execute();
        DecisionContext onTheFlop = this.decisions.lastContext;
        assertEquals(game.getBoard().getRevealedCards(), onTheFlop.getBoardCards());
        assertEquals(3, onTheFlop.getBoardCards().size());
        List<Card> seen = new ArrayList<>(onTheFlop.getHoleCards());
        seen.addAll(onTheFlop.getBoardCards());
        for (Card humanCard : match.getHuman().getHoleCards().getCards()) {
            assertFalse(seen.contains(humanCard), "the opponent was shown " + humanCard);
        }
    }

    @Test
    void givesTheDecisionSourceTheOpponentsOwnChanceOfWinning() {
        Match match = dealToTheOpponent();
        Game game = match.getCurrentGame();
        EquityResult beforeTheFlop = match.getComputer().winChance(game.getBoard());
        this.decisions.answer = Action.check();

        this.interactor.execute();
        assertEquals(beforeTheFlop.getEquity(), this.decisions.lastContext.getWinChance(), EXACT);

        EquityResult onTheFlop = match.getComputer().winChance(game.getBoard());
        this.interactor.execute();
        assertEquals(onTheFlop.getEquity(), this.decisions.lastContext.getWinChance(), EXACT);
        // Half of the ties are in it, so it is more than the chance of winning outright.
        assertTrue(onTheFlop.getTie() > 0.0);
        assertTrue(this.decisions.lastContext.getWinChance() > onTheFlop.getWin());
    }

    @Test
    void countsHalfOfTheTiesTowardsTheOpponentsChanceOfWinning() {
        assertEquals(0.4, chanceGivenToTheDecisionSource(new EquityResult(0.2, 0.4, 0.4)), EXACT);
    }

    @Test
    void countsAPotTheOpponentIsSureToSplitAsASureWin() {
        // Folding would give its half of the pot away, and nothing it puts in can be lost.
        assertEquals(1.0, chanceGivenToTheDecisionSource(new EquityResult(0.0, 1.0, 0.0)), EXACT);
    }

    @Test
    void countsAHandThatMayTieButCannotLoseAsASureWin() {
        assertEquals(1.0, chanceGivenToTheDecisionSource(new EquityResult(0.3, 0.7, 0.0)), EXACT);
    }

    /** Runs the opponent's turn with a calculator that gives these chances whatever the cards. */
    private double chanceGivenToTheDecisionSource(EquityResult chances) {
        EquityCalculator fixed = new EquityCalculator() {
            @Override
            public EquityResult calculate(List<Card> holeCards, List<Card> boardCards) {
                return chances;
            }
        };
        OpponentActionInteractor withTheseChances = new OpponentActionInteractor(this.session, this.history,
                this.profiles, this.decisions, this.presenter, new Random(SEED), IDS, CLOCK, fixed);
        dealToTheOpponent();
        this.decisions.answer = Action.check();

        withTheseChances.execute();

        return this.decisions.lastContext.getWinChance();
    }

    @Test
    void reportsTheNewStreetWhenTheOpponentsActionEndsTheBettingRound() {
        dealToTheOpponent();
        this.decisions.answer = Action.check();

        this.interactor.execute();

        TableOutputData table = this.presenter.lastTable;
        assertEquals("FLOP", table.getStreet());
        assertEquals(2, table.getEvents().size());
        assertEquals("CHECK", table.getEvents().get(0).getType());
        assertEquals("FLOP", table.getEvents().get(1).getType());
        assertEquals("OPPONENT", table.getToAct());
    }

    @Test
    void letsTheBuiltInStrategyDecideWhenTheSourceIsUnavailable() {
        dealToTheOpponent();
        this.decisions.unavailable = new OpponentUnavailableException("no key configured");

        this.interactor.execute();

        assertDecidedByTheBuiltInStrategy();
    }

    @Test
    void letsTheBuiltInStrategyDecideWhenTheSourceBreaks() {
        dealToTheOpponent();
        this.decisions.broken = new IllegalStateException("unexpected reply");

        this.interactor.execute();

        assertDecidedByTheBuiltInStrategy();
    }

    @Test
    void letsTheBuiltInStrategyDecideWhenTheSourceGivesNoAnswer() {
        dealToTheOpponent();
        this.decisions.answer = null;

        this.interactor.execute();

        assertDecidedByTheBuiltInStrategy();
    }

    @Test
    void letsTheBuiltInStrategyDecideWhenTheAnswerIsNotAllowedNow() {
        dealToTheOpponent();
        // Nothing to call, so a call is not legal.
        this.decisions.answer = Action.call();

        this.interactor.execute();

        assertDecidedByTheBuiltInStrategy();
    }

    @Test
    void letsTheBuiltInStrategyDecideWhenTheAnswerIsARaiseOfMoreThanTheOpponentHas() {
        Match match = dealToTheOpponent();
        this.decisions.answer = Action.raise(1001);

        this.interactor.execute();

        assertDecidedByTheBuiltInStrategy();
        assertEquals(2000, match.getHuman().getChips() + match.getComputer().getChips()
                + match.getCurrentGame().getPot());
    }

    @Test
    void usesTheBuiltInStrategyOfTheStoredProfile() {
        for (String style : BotStrategyFactory.styleNames()) {
            for (double aggression : new double[] {0.0, 0.5, 1.0}) {
                setUp();
                this.profiles.profile = new BotProfile(style, aggression);
                Match match = TableFixtures.dealtMatch(21);
                this.session.saveMatch(match);
                // Facing a raise, the styles and aggressions part ways.
                match.getCurrentGame().apply(Action.raise(60));
                this.decisions.unavailable = new OpponentUnavailableException("offline");

                this.interactor.execute();

                assertDecidedByTheBuiltInStrategy();
            }
        }
    }

    @Test
    void fallsBackToTheBalancedStyleWhenTheProfileCannotBeRead() {
        dealToTheOpponent();
        this.profiles.broken = true;
        this.decisions.unavailable = new OpponentUnavailableException("offline");

        this.interactor.execute();

        assertPlayedLikeTheBalancedStyle();
    }

    @Test
    void fallsBackToTheBalancedStyleWhenThereIsNoProfile() {
        dealToTheOpponent();
        this.profiles.profile = null;
        this.decisions.unavailable = new OpponentUnavailableException("offline");

        this.interactor.execute();

        assertPlayedLikeTheBalancedStyle();
    }

    @Test
    void checksWhenEvenTheBuiltInStrategyGivesAnIllegalAction() {
        dealToTheOpponent();
        this.decisions.unavailable = new OpponentUnavailableException("offline");
        OpponentActionInteractor faulty = withBuiltInStrategy(context -> Action.raise(5000));

        faulty.execute();

        assertEquals(List.of("success"), this.presenter.calls);
        assertEquals("CHECK", this.presenter.lastTable.getEvents().get(0).getType());
        assertEquals("BUILT_IN", this.presenter.lastTable.getDecidedBy());
    }

    @Test
    void callsWhenTheBuiltInStrategyFailsAndThereIsABetToCall() {
        Match match = TableFixtures.dealtMatch(11);
        this.session.saveMatch(match);
        match.getCurrentGame().apply(Action.raise(40));
        this.decisions.unavailable = new OpponentUnavailableException("offline");
        OpponentActionInteractor faulty = withBuiltInStrategy(context -> {
            throw new IllegalStateException("strategy broke");
        });

        faulty.execute();

        assertEquals(List.of("success"), this.presenter.calls);
        TableEventData event = this.presenter.lastTable.getEvents().get(0);
        assertEquals("CALL", event.getType());
        assertEquals(30, event.getAmount());
        assertEquals("BUILT_IN", this.presenter.lastTable.getDecidedBy());
    }

    @Test
    void checksWhenTheBuiltInStrategyGivesNoAction() {
        dealToTheOpponent();
        this.decisions.answer = Action.bet(3);
        OpponentActionInteractor faulty = withBuiltInStrategy(context -> null);

        faulty.execute();

        assertEquals("CHECK", this.presenter.lastTable.getEvents().get(0).getType());
        assertEquals("BUILT_IN", this.presenter.lastTable.getDecidedBy());
    }

    @Test
    void savesARecordWhenTheOpponentsActionEndsTheHand() {
        Match match = dealToTheOpponent();
        Game game = match.getCurrentGame();
        this.decisions.answer = Action.fold();

        this.interactor.execute();

        assertEquals(List.of("success"), this.presenter.calls);
        TableOutputData table = this.presenter.lastTable;
        assertTrue(table.isHandOver());
        assertEquals("HUMAN", table.getWinner());
        assertEquals(10, table.getHumanChipChange());
        assertEquals("LLM", table.getDecidedBy());
        assertEquals(List.of(), table.getOpponentCards());
        assertEquals(2, table.getEvents().size());
        assertEquals("FOLD", table.getEvents().get(0).getType());
        assertEquals("WIN", table.getEvents().get(1).getType());

        assertEquals(1, this.history.records.size());
        HandRecord record = this.history.records.get(0);
        assertEquals("hand-id", record.getId());
        assertEquals(NOW, record.getPlayedAt());
        assertEquals("You", record.getWinnerName());
        assertEquals(List.of(10, -10), record.getChipChanges());
        assertEquals(game.getActionLog(), record.getActions());
    }

    @Test
    void savesARecordWhenTheOpponentsCheckLeadsToAShowdown() {
        // In the second hand the human is the big blind, so the opponent has the last word on the river.
        Match match = TableFixtures.secondHandMatch(11);
        this.session.saveMatch(match);
        Game game = match.getCurrentGame();
        TableFixtures.checkTo(game, Street.RIVER);
        game.apply(Action.check());
        this.decisions.answer = Action.check();

        this.interactor.execute();

        TableOutputData table = this.presenter.lastTable;
        assertTrue(table.isHandOver());
        assertEquals("SHOWDOWN", table.getStreet());
        assertEquals(TableFixtures.holeCards(match.getComputer()), table.getOpponentCards());
        assertFalse(table.getOpponentHandName().isEmpty());
        assertEquals(1, this.history.records.size());
        assertEquals(5, this.history.records.get(0).getBoardCards().size());
        // The cards of the winning hand are picked out with the showdown.
        assertTrue(table.getWinningCards().size() >= 5);
        if (game.getWinner() != null) {
            assertEquals(TableFixtures.texts(game.getBestFive(game.getWinner())), table.getWinningCards());
        }
    }

    @Test
    void picksOutNoWinningCardsWhenTheOpponentFoldsAfterTheFlop() {
        Match match = dealToTheOpponent();
        Game game = match.getCurrentGame();
        TableFixtures.checkTo(game, Street.FLOP);
        game.apply(Action.check());
        game.apply(Action.bet(40));
        this.decisions.answer = Action.fold();

        this.interactor.execute();

        TableOutputData table = this.presenter.lastTable;
        assertTrue(table.isHandOver());
        assertEquals("HUMAN", table.getWinner());
        assertEquals("FLOP", table.getStreet());
        assertEquals(List.of(), table.getWinningCards());
    }

    @Test
    void endsTheHandWithoutAskingTheHumanWhenTheOpponentCallsTheBigBlindWithItsLastChips() {
        // The opponent, small blind with exactly 10 chips, has posted 5 and has 5 left.
        Match match = TableFixtures.shortSmallBlindMatch(11, false);
        this.session.saveMatch(match);
        Game game = match.getCurrentGame();
        this.decisions.answer = Action.call();

        this.interactor.execute();

        // The source was shown that it could only fold or call, and the call ended the betting.
        assertEquals(List.of(ActionType.FOLD, ActionType.CALL), this.decisions.lastContext.getLegalActionTypes());
        assertEquals(5, this.decisions.lastContext.getAmountToCall());
        assertEquals(List.of("success"), this.presenter.calls);
        TableOutputData table = this.presenter.lastTable;
        assertEquals("LLM", table.getDecidedBy());
        assertTrue(table.isHandOver());
        assertEquals("", table.getToAct());
        assertEquals(List.of(), table.getLegalActions());
        assertEquals("SHOWDOWN", table.getStreet());
        List<String> types = new ArrayList<>();
        for (TableEventData event : table.getEvents()) {
            types.add(event.getType());
        }
        assertEquals(List.of("CALL", "FLOP", "TURN", "RIVER", "SHOWDOWN", "SHOWDOWN"), types.subList(0, 6));
        assertTrue(List.of(List.of("WIN"), List.of("SPLIT", "SPLIT")).contains(types.subList(6, types.size())),
                types.toString());
        assertEquals("OPPONENT", table.getEvents().get(0).getActor());
        assertEquals(5, table.getEvents().get(0).getAmount());
        assertEquals(10, table.getEvents().get(0).getTotal());
        assertEquals(TableFixtures.holeCards(match.getComputer()), table.getOpponentCards());
        assertFalse(table.getWinningCards().isEmpty());
        assertEquals(2000, table.getHumanChips() + table.getOpponentChips());
        assertEquals("HUMAN".equals(table.getWinner()), table.isMatchOver());
        assertEquals(1, this.history.records.size());
        assertEquals(game.getActionLog(), this.history.records.get(0).getActions());
    }

    @Test
    void letsTheBuiltInStrategyPlayAnOpponentWhoCanOnlyFoldOrCallWithItsLastChips() {
        Match match = TableFixtures.shortSmallBlindMatch(11, false);
        this.session.saveMatch(match);
        this.decisions.unavailable = new OpponentUnavailableException("no key");

        this.interactor.execute();

        // Whatever the strategy chose, it was legal, and the hand is over: a fold or a run-out.
        assertEquals(List.of("success"), this.presenter.calls);
        TableOutputData table = this.presenter.lastTable;
        assertEquals("BUILT_IN", table.getDecidedBy());
        assertTrue(List.of("FOLD", "CALL").contains(table.getEvents().get(0).getType()));
        assertTrue(table.isHandOver());
        assertEquals("", table.getToAct());
        assertEquals(1, this.history.records.size());
    }

    @Test
    void stillAsksTheOpponentWhoFacesTheHumansAllInRaise() {
        Match match = dealToTheOpponent();
        Game game = match.getCurrentGame();
        game.apply(Action.raise(60));
        game.apply(Action.raise(1000));
        this.decisions.answer = Action.fold();

        this.interactor.execute();

        // The opponent, with the human all in, was asked and could fold.
        assertEquals(1, this.decisions.asked);
        assertEquals(List.of(ActionType.FOLD, ActionType.CALL), this.decisions.lastContext.getLegalActionTypes());
        assertEquals(940, this.decisions.lastContext.getAmountToCall());
        assertEquals("HUMAN", this.presenter.lastTable.getWinner());
        assertEquals(List.of(), this.presenter.lastTable.getWinningCards());
    }

    @Test
    void stillReportsTheTableWhenTheRecordCannotBeSaved() {
        dealToTheOpponent();
        this.history.failing = true;
        this.decisions.answer = Action.fold();

        this.interactor.execute();

        assertEquals(List.of("success", "fail"), this.presenter.calls);
        assertTrue(this.presenter.lastTable.isHandOver());
        assertEquals("The hand is over, but it could not be saved to the history.", this.presenter.lastError);
    }

    @Test
    void doesNotPlayAnAnswerThatArrivesAfterANewMatchHasStarted() {
        Match match = dealToTheOpponent();
        Game abandoned = match.getCurrentGame();
        this.decisions.answer = Action.raise(300);
        // The source is slow, and the user starts a new match before it answers.
        this.decisions.whileDeciding = () -> {
            match.restart();
            match.startNextHand(new Random(SEED));
        };

        this.interactor.execute();

        assertEquals(List.of("fail"), this.presenter.calls);
        assertEquals(HAND_MOVED_ON, this.presenter.lastError);
        Game current = match.getCurrentGame();
        assertNotSame(abandoned, current);
        // The new hand is untouched: only the blinds have been posted, and no chip has gone missing.
        assertEquals(2, current.getEvents().size());
        assertEquals(995, match.getHuman().getChips());
        assertEquals(990, match.getComputer().getChips());
        assertEquals(15, current.getPot());
        assertEquals(3, abandoned.getEvents().size());
        assertEquals(List.of(), this.history.records);
    }

    @Test
    void doesNotPlayAnAnswerThatArrivesAfterTheMatchWasReplaced() {
        Match match = dealToTheOpponent();
        Game abandoned = match.getCurrentGame();
        this.decisions.answer = Action.raise(300);
        this.decisions.whileDeciding = () -> this.session.saveMatch(TableFixtures.newMatch());

        this.interactor.execute();

        assertEquals(List.of("fail"), this.presenter.calls);
        assertEquals(HAND_MOVED_ON, this.presenter.lastError);
        assertEquals(3, abandoned.getEvents().size());
        assertEquals(990, match.getComputer().getChips());
    }

    @Test
    void doesNotPlayAnAnswerThatArrivesAfterTheHandIsOver() {
        Match match = dealToTheOpponent();
        Game game = match.getCurrentGame();
        this.decisions.answer = Action.raise(30);
        // The hand is ended for the opponent while the source is still thinking.
        this.decisions.whileDeciding = () -> game.apply(Action.fold());

        this.interactor.execute();

        assertEquals(List.of("fail"), this.presenter.calls);
        assertEquals(HAND_MOVED_ON, this.presenter.lastError);
        assertTrue(game.isHandOver());
        assertEquals(1010, match.getHuman().getChips());
        assertEquals(990, match.getComputer().getChips());
        assertEquals(List.of(), this.history.records);
    }

    @Test
    void playsOnlyOnceWhenTheSameTurnIsAskedForTwice() {
        Match match = dealToTheOpponent();
        Game game = match.getCurrentGame();
        this.decisions.answer = Action.check();
        // The turn is asked for again while the first request is still waiting for its answer.
        this.decisions.whileDeciding = () -> {
            this.decisions.whileDeciding = null;
            this.interactor.execute();
        };

        this.interactor.execute();

        // The second request checked and brought the flop, where the opponent is first to act again.
        // The first request's answer was made for the turn before the flop, so it is not played.
        assertEquals(List.of("success", "fail"), this.presenter.calls);
        assertEquals(HAND_MOVED_ON, this.presenter.lastError);
        assertEquals(2, this.decisions.asked);
        assertEquals(Street.FLOP, game.getStreet());
        assertSame(match.getComputer(), game.getPlayerToAct());
        assertEquals(5, game.getEvents().size());
        assertEquals(2, this.presenter.lastTable.getEvents().size());
        assertEquals(20, game.getPot());
    }

    @Test
    void worksOutBothPlayersChancesWithTheGivenCalculator() {
        SamplingEquityCalculator calculator = new SamplingEquityCalculator();
        OpponentActionInteractor sampling = new OpponentActionInteractor(this.session, this.history,
                this.profiles, this.decisions, this.presenter, new Random(SEED), IDS, CLOCK, calculator);
        dealToTheOpponent();
        this.decisions.answer = Action.raise(30);

        sampling.execute();

        // Once for the opponent's decision and once for the human's chances in the reported table.
        assertEquals(2, calculator.getCalls());
    }

    /**
     * Deals the first hand and has the human, the small blind, call. The
     * opponent may then check or raise.
     */
    private Match dealToTheOpponent() {
        Match match = TableFixtures.dealtMatch(11);
        this.session.saveMatch(match);
        match.getCurrentGame().apply(Action.call());
        return match;
    }

    /**
     * Returns an interactor like the one under test whose built-in strategy
     * is replaced by the given one, whatever the profile.
     */
    private OpponentActionInteractor withBuiltInStrategy(BotStrategy strategy) {
        return new OpponentActionInteractor(this.session, this.history, this.profiles, this.decisions,
                this.presenter, new Random(SEED), IDS, CLOCK) {
            @Override
            BotStrategy builtInStrategy(BotProfile profile) {
                return strategy;
            }
        };
    }

    /**
     * Checks that the action just applied is the one the built-in strategy for
     * the stored profile chooses in the situation the decision source was shown.
     */
    private void assertDecidedByTheBuiltInStrategy() {
        BotStrategy strategy = BotStrategyFactory.create(this.profiles.profile, new Random(SEED));
        assertAppliedActionOf(strategy);
    }

    private void assertPlayedLikeTheBalancedStyle() {
        assertEquals("Balanced", this.decisions.lastProfile.getStyleName());
        assertEquals(0.5, this.decisions.lastProfile.getAggression(), EXACT);
        assertAppliedActionOf(new BalancedStrategy(0.5));
    }

    private void assertAppliedActionOf(BotStrategy strategy) {
        Action expected = strategy.decide(this.decisions.lastContext);
        assertEquals("success", this.presenter.calls.get(this.presenter.calls.size() - 1));
        TableOutputData table = this.presenter.lastTable;
        assertEquals("BUILT_IN", table.getDecidedBy());
        TableEventData applied = table.getEvents().get(0);
        assertEquals("OPPONENT", applied.getActor());
        assertEquals(expected.getType().name(), applied.getType());
        if (expected.getAmount() > 0) {
            assertEquals(expected.getAmount(), applied.getTotal());
        }
    }

    /**
     * A decision source that does what the test tells it to and remembers what
     * it was shown. It can also be given something to do while it is deciding,
     * which stands for what happens at the table while a slow source thinks.
     */
    private static final class ScriptedDecisions implements OpponentActionDecisionDataAccessInterface {
        private Action answer;
        private OpponentUnavailableException unavailable;
        private RuntimeException broken;
        private DecisionContext lastContext;
        private BotProfile lastProfile;
        private int asked;
        private Runnable whileDeciding;

        @Override
        public Action decide(DecisionContext context, BotProfile profile) throws OpponentUnavailableException {
            this.asked++;
            this.lastContext = context;
            this.lastProfile = profile;
            if (this.whileDeciding != null) {
                this.whileDeciding.run();
            }
            if (this.unavailable != null) {
                throw this.unavailable;
            }
            if (this.broken != null) {
                throw this.broken;
            }
            return this.answer;
        }
    }

    /**
     * A profile store that holds one profile and can be told to fail.
     */
    private static final class StubProfiles implements OpponentActionProfileDataAccessInterface {
        private BotProfile profile = new BotProfile("Tight", 0.3);
        private boolean broken;

        @Override
        public BotProfile load() {
            if (this.broken) {
                throw new IllegalStateException("the profile cannot be read");
            }
            return this.profile;
        }
    }

    /**
     * A history kept in a list, which can be told to fail.
     */
    private static final class RecordingHistory implements OpponentActionHistoryDataAccessInterface {
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
     * Remembers what the interactor reported, and in which order.
     */
    private static final class RecordingPresenter implements OpponentActionOutputBoundary {
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
    }
}
