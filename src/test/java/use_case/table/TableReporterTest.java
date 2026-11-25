package use_case.table;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.Test;

import entity.Action;
import entity.Cards;
import entity.EquityResult;
import entity.Game;
import entity.HandEvaluator;
import entity.Match;
import entity.Player;
import entity.Street;

class TableReporterTest {
    private static final double EXACT = 0.0;
    private static final double ROUNDING = 1e-9;

    private final TableReporter reporter = new TableReporter();

    @Test
    void rejectsMissingArguments() {
        Match match = TableFixtures.dealtMatch(1);

        assertThrows(IllegalArgumentException.class, () -> new TableReporter(null));
        assertThrows(IllegalArgumentException.class, () -> this.reporter.report(null, 0, TableOutputData.NONE));
        assertThrows(IllegalArgumentException.class, () -> this.reporter.report(match, 0, null));
    }

    @Test
    void rejectsAnEventCountThatTheHandDoesNotHave() {
        Match dealt = TableFixtures.dealtMatch(1);
        Match notDealt = TableFixtures.newMatch();

        assertThrows(IllegalArgumentException.class, () -> this.reporter.report(dealt, -1, TableOutputData.NONE));
        assertThrows(IllegalArgumentException.class, () -> this.reporter.report(dealt, 3, TableOutputData.NONE));
        assertThrows(IllegalArgumentException.class,
                () -> this.reporter.report(notDealt, 1, TableOutputData.NONE));
    }

    @Test
    void describesAMatchBeforeItsFirstHand() {
        TableOutputData table = this.reporter.report(TableFixtures.newMatch(), 0, TableOutputData.NONE);

        assertEquals(0, table.getHandNumber());
        assertEquals("You", table.getHumanName());
        assertEquals("Opponent", table.getOpponentName());
        assertEquals("PREFLOP", table.getStreet());
        assertEquals(0, table.getPot());
        assertEquals(1000, table.getHumanChips());
        assertEquals(1000, table.getOpponentChips());
        assertEquals(List.of(), table.getHumanCards());
        assertEquals(List.of(), table.getOpponentCards());
        assertEquals(List.of(), table.getBoardCards());
        assertFalse(table.isHumanSmallBlind());
        assertFalse(table.isHandOver());
        assertFalse(table.isMatchOver());
        assertEquals("", table.getWinner());
        assertEquals("", table.getToAct());
        assertEquals(List.of(), table.getLegalActions());
        assertEquals(-1.0, table.getWinChance(), EXACT);
        assertEquals(-1.0, table.getTieChance(), EXACT);
        assertEquals(-1.0, table.getLoseChance(), EXACT);
        assertEquals("", table.getHumanHandName());
        assertEquals(List.of(), table.getWinningCards());
        assertEquals(List.of(), table.getEvents());
    }

    @Test
    void describesANewHandFromTheHumansSeat() {
        Match match = TableFixtures.dealtMatch(1);
        Player human = match.getHuman();

        TableOutputData table = this.reporter.report(match, 0, TableOutputData.NONE);

        assertEquals(1, table.getHandNumber());
        assertEquals("PREFLOP", table.getStreet());
        assertEquals(15, table.getPot());
        assertEquals(995, table.getHumanChips());
        assertEquals(990, table.getOpponentChips());
        assertEquals(5, table.getHumanBet());
        assertEquals(10, table.getOpponentBet());
        assertEquals(TableFixtures.holeCards(human), table.getHumanCards());
        assertEquals(2, table.getHumanCards().size());
        assertEquals(List.of(), table.getOpponentCards());
        assertEquals(List.of(), table.getBoardCards());
        assertTrue(table.isHumanSmallBlind());
        assertFalse(table.isHandOver());
        assertFalse(table.isMatchOver());
        assertEquals("", table.getWinner());
        assertEquals("", table.getHumanHandName());
        assertEquals("", table.getOpponentHandName());
        assertEquals(0, table.getHumanChipChange());
        assertEquals("", table.getDecidedBy());
    }

    @Test
    void tellsTheHumanWhatTheyMayDoWhenItIsTheirTurn() {
        Match match = TableFixtures.dealtMatch(1);

        TableOutputData table = this.reporter.report(match, 0, TableOutputData.NONE);

        assertEquals("HUMAN", table.getToAct());
        assertEquals(List.of("FOLD", "CALL", "RAISE"), table.getLegalActions());
        assertEquals(5, table.getCallAmount());
        assertEquals(20, table.getMinWager());
        assertEquals(1000, table.getMaxWager());
    }

    @Test
    void tellsTheHumanTheSizeOfARaiseTheyFace() {
        Match match = TableFixtures.dealtMatch(1);
        match.getCurrentGame().apply(Action.call());
        match.getCurrentGame().apply(Action.raise(40));

        TableOutputData table = this.reporter.report(match, 0, TableOutputData.NONE);

        assertEquals("HUMAN", table.getToAct());
        assertEquals(List.of("FOLD", "CALL", "RAISE"), table.getLegalActions());
        assertEquals(30, table.getCallAmount());
        assertEquals(70, table.getMinWager());
        assertEquals(1000, table.getMaxWager());
        assertEquals(50, table.getPot());
        assertEquals(10, table.getHumanBet());
        assertEquals(40, table.getOpponentBet());
    }

    @Test
    void tellsTheHumanWhatACallCostsWhenTheBetIsLargerThanTheirStack() {
        Match match = TableFixtures.shortHumanMatch(1, 200);
        Game game = match.getCurrentGame();
        game.apply(Action.raise(20));
        game.apply(Action.raise(510));

        TableOutputData table = this.reporter.report(match, 0, TableOutputData.NONE);

        // The raise asks for 490 more, but the human has only 180 left: that is what a call costs.
        assertEquals(490, game.getAmountToCall());
        assertEquals("HUMAN", table.getToAct());
        assertEquals(List.of("FOLD", "CALL"), table.getLegalActions());
        assertEquals(180, table.getHumanChips());
        assertEquals(180, table.getCallAmount());

        // The call bears it out: it takes the 180, and the 310 it could not match go back.
        game.apply(Action.call());
        List<TableEventData> events = this.reporter.report(match, 4, TableOutputData.NONE).getEvents();
        assertEvent(events.get(0), "CALL", "HUMAN", 180, 200);
        assertEvent(events.get(1), "REFUND", "OPPONENT", 310, 200);
    }

    @Test
    void tellsTheHumanTheWholeBetWhenTheirStackJustCoversIt() {
        Match match = TableFixtures.shortHumanMatch(1, 200);
        Game game = match.getCurrentGame();
        game.apply(Action.raise(20));
        game.apply(Action.raise(200));

        TableOutputData table = this.reporter.report(match, 0, TableOutputData.NONE);

        assertEquals(180, table.getHumanChips());
        assertEquals(180, table.getCallAmount());
        assertEquals(game.getAmountToCall(), table.getCallAmount());
    }

    @Test
    void offersTheHumanNothingWhenItIsTheOpponentsTurn() {
        Match match = TableFixtures.dealtMatch(1);
        match.getCurrentGame().apply(Action.call());

        TableOutputData table = this.reporter.report(match, 0, TableOutputData.NONE);

        assertEquals("OPPONENT", table.getToAct());
        assertEquals(List.of(), table.getLegalActions());
        assertEquals(0, table.getCallAmount());
        assertEquals(0, table.getMinWager());
        assertEquals(0, table.getMaxWager());
    }

    @Test
    void reportsTheBlindsAsTheEventsOfANewHand() {
        Match match = TableFixtures.dealtMatch(1);

        List<TableEventData> events = this.reporter.report(match, 0, TableOutputData.NONE).getEvents();

        assertEquals(2, events.size());
        assertEvent(events.get(0), "BLIND", "HUMAN", 5, 5);
        assertEvent(events.get(1), "BLIND", "OPPONENT", 10, 10);
    }

    @Test
    void reportsOnlyTheEventsAfterThoseAlreadyReported() {
        Match match = TableFixtures.dealtMatch(1);
        match.getCurrentGame().apply(Action.call());

        List<TableEventData> newEvents = this.reporter.report(match, 2, TableOutputData.NONE).getEvents();

        assertEquals(1, newEvents.size());
        assertEvent(newEvents.get(0), "CALL", "HUMAN", 5, 10);
        assertEquals(3, this.reporter.report(match, 0, TableOutputData.NONE).getEvents().size());
        assertEquals(List.of(), this.reporter.report(match, 3, TableOutputData.NONE).getEvents());
    }

    @Test
    void reportsTheFlopOnceTheFirstBettingRoundIsOver() {
        Match match = TableFixtures.dealtMatch(1);
        Game game = match.getCurrentGame();
        game.apply(Action.call());
        game.apply(Action.check());

        TableOutputData table = this.reporter.report(match, 3, TableOutputData.NONE);

        assertEquals("FLOP", table.getStreet());
        assertEquals(20, table.getPot());
        assertEquals(0, table.getHumanBet());
        assertEquals(0, table.getOpponentBet());
        assertEquals(3, table.getBoardCards().size());
        assertEquals(TableFixtures.texts(game.getBoard().getRevealedCards()), table.getBoardCards());
        assertEquals("OPPONENT", table.getToAct());
        assertEquals(2, table.getEvents().size());
        assertEvent(table.getEvents().get(0), "CHECK", "OPPONENT", 0, 10);
        TableEventData flop = table.getEvents().get(1);
        assertEvent(flop, "FLOP", "", 0, 0);
        assertEquals(table.getBoardCards(), flop.getCards());
    }

    @Test
    void namesTheHumansHandFromTheFlopOn() {
        Match match = TableFixtures.dealtMatch(1);
        Game game = match.getCurrentGame();
        assertEquals("", this.reporter.report(match, 0, TableOutputData.NONE).getHumanHandName());

        TableFixtures.checkTo(game, Street.FLOP);
        String onTheFlop = game.getHandValue(match.getHuman()).getCategory().getDisplayName();
        assertEquals(onTheFlop, this.reporter.report(match, 0, TableOutputData.NONE).getHumanHandName());

        TableFixtures.checkTo(game, Street.RIVER);
        String onTheRiver = game.getHandValue(match.getHuman()).getCategory().getDisplayName();
        TableOutputData table = this.reporter.report(match, 0, TableOutputData.NONE);
        assertEquals(onTheRiver, table.getHumanHandName());
        assertEquals("", table.getOpponentHandName());
    }

    @Test
    void givesTheHumansChancesFromTheirOwnCardsAndTheBoard() {
        Match match = TableFixtures.dealtMatch(1);
        Game game = match.getCurrentGame();
        TableFixtures.checkTo(game, Street.TURN);
        EquityResult expected = match.getHuman().winChance(game.getBoard());

        TableOutputData table = this.reporter.report(match, 0, TableOutputData.NONE);

        assertEquals(expected.getWin(), table.getWinChance(), EXACT);
        assertEquals(expected.getTie(), table.getTieChance(), EXACT);
        assertEquals(expected.getLose(), table.getLoseChance(), EXACT);
        assertEquals(1.0, table.getWinChance() + table.getTieChance() + table.getLoseChance(), ROUNDING);
    }

    @Test
    void usesTheCalculatorItWasGiven() {
        SamplingEquityCalculator calculator = new SamplingEquityCalculator();
        TableReporter sampling = new TableReporter(calculator);
        Match match = TableFixtures.dealtMatch(1);

        sampling.report(match, 0, TableOutputData.NONE);
        assertEquals(1, calculator.getCalls());

        match.getCurrentGame().apply(Action.fold());
        sampling.report(match, 0, TableOutputData.NONE);
        assertEquals(1, calculator.getCalls());
    }

    @Test
    void hasNoChancesOnceTheHumanHasFolded() {
        Match match = TableFixtures.dealtMatch(1);
        match.getCurrentGame().apply(Action.fold());

        TableOutputData table = this.reporter.report(match, 2, TableOutputData.NONE);

        assertEquals(-1.0, table.getWinChance(), EXACT);
        assertEquals(-1.0, table.getTieChance(), EXACT);
        assertEquals(-1.0, table.getLoseChance(), EXACT);
        assertTrue(table.isHandOver());
        assertEquals("OPPONENT", table.getWinner());
        assertEquals("", table.getToAct());
        assertEquals(List.of(), table.getLegalActions());
        assertEquals(-5, table.getHumanChipChange());
        assertEquals(995, table.getHumanChips());
        assertEquals(1005, table.getOpponentChips());
        assertEquals(0, table.getPot());
        assertEquals(0, table.getHumanBet());
        assertEquals(0, table.getOpponentBet());
        assertEquals(2, table.getEvents().size());
        assertEvent(table.getEvents().get(0), "FOLD", "HUMAN", 0, 5);
        assertEvent(table.getEvents().get(1), "WIN", "OPPONENT", 15, 0);
    }

    @Test
    void countsEveryBetAndRaiseOfTheHumanInTheChipChange() {
        Match match = TableFixtures.dealtMatch(1);
        Game game = match.getCurrentGame();
        TableFixtures.checkTo(game, Street.FLOP);
        // On the flop: the opponent checks, the human bets 30, is raised to 90, and raises to 200.
        game.apply(Action.check());
        game.apply(Action.bet(30));
        game.apply(Action.raise(90));
        game.apply(Action.raise(200));
        game.apply(Action.fold());

        TableOutputData table = this.reporter.report(match, 0, TableOutputData.NONE);

        // The human gets their own 210 back and wins the opponent's big blind and raise to 90.
        assertEquals("HUMAN", table.getWinner());
        assertEquals(100, table.getHumanChipChange());
        assertEquals(1100, table.getHumanChips());
        assertEquals(900, table.getOpponentChips());
        assertEquals("FLOP", table.getStreet());
        assertEquals(List.of(), table.getOpponentCards());
    }

    @Test
    void countsWhatTheHumanPutInBeforeFoldingAsLost() {
        Match match = TableFixtures.dealtMatch(1);
        Game game = match.getCurrentGame();
        game.apply(Action.raise(40));
        game.apply(Action.raise(120));
        game.apply(Action.call());
        game.apply(Action.bet(100));
        game.apply(Action.fold());

        TableOutputData table = this.reporter.report(match, 0, TableOutputData.NONE);

        assertEquals("OPPONENT", table.getWinner());
        assertEquals(-120, table.getHumanChipChange());
        assertEquals(880, table.getHumanChips());
        assertEquals(1120, table.getOpponentChips());
    }

    @Test
    void keepsTheOpponentsCardsHiddenWhenTheOpponentFolds() {
        Match match = TableFixtures.dealtMatch(1);
        Game game = match.getCurrentGame();
        game.apply(Action.raise(30));
        game.apply(Action.fold());

        TableOutputData table = this.reporter.report(match, 0, TableOutputData.NONE);

        assertTrue(table.isHandOver());
        assertEquals("PREFLOP", table.getStreet());
        assertEquals("HUMAN", table.getWinner());
        assertEquals(10, table.getHumanChipChange());
        assertEquals(List.of(), table.getOpponentCards());
        assertEquals("", table.getOpponentHandName());
        assertTrue(table.getWinChance() >= 0.0);
        assertFalse(table.isMatchOver());
    }

    @Test
    void neverShowsTheOpponentsCardsBeforeAShowdown() {
        Match match = TableFixtures.dealtMatch(1);
        Game game = match.getCurrentGame();
        List<String> secret = TableFixtures.holeCards(match.getComputer());

        while (game.getStreet() != Street.SHOWDOWN) {
            TableOutputData table = this.reporter.report(match, 0, TableOutputData.NONE);
            assertEquals(List.of(), table.getOpponentCards());
            assertEquals("", table.getOpponentHandName());
            for (String card : allCardsIn(table)) {
                assertFalse(secret.contains(card), card + " is the opponent's");
            }
            TableFixtures.actPassively(game);
        }
    }

    @Test
    void showsBothHandsAfterAShowdownTheHumanWins() {
        long seed = TableFixtures.seedWhere(candidate -> checkedDownWinnerIs(candidate, TableFixtures.HUMAN_NAME));
        Match match = TableFixtures.dealtMatch(seed);
        Game game = match.getCurrentGame();
        TableFixtures.checkTo(game, Street.RIVER);
        int eventsBefore = game.getEvents().size();
        TableFixtures.checkDown(game);

        TableOutputData table = this.reporter.report(match, eventsBefore, TableOutputData.NONE);

        assertTrue(table.isHandOver());
        assertEquals("SHOWDOWN", table.getStreet());
        assertEquals("HUMAN", table.getWinner());
        assertEquals(10, table.getHumanChipChange());
        assertEquals(1010, table.getHumanChips());
        assertEquals(990, table.getOpponentChips());
        assertEquals(TableFixtures.holeCards(match.getComputer()), table.getOpponentCards());
        assertEquals(game.getHandValue(match.getComputer()).getCategory().getDisplayName(),
                table.getOpponentHandName());
        assertEquals(game.getHandValue(match.getHuman()).getCategory().getDisplayName(),
                table.getHumanHandName());
        assertEquals(5, table.getBoardCards().size());

        // The river was checked through: opponent, then human, then both hands are shown, small blind first.
        List<TableEventData> events = table.getEvents();
        assertEquals(List.of("CHECK", "CHECK", "SHOWDOWN", "SHOWDOWN", "WIN"), typesOf(events));
        TableEventData humanShows = events.get(2);
        assertEquals("HUMAN", humanShows.getActor());
        assertEquals(table.getHumanCards(), humanShows.getCards());
        assertEquals(table.getHumanHandName(), humanShows.getDetail());
        TableEventData opponentShows = events.get(3);
        assertEquals("OPPONENT", opponentShows.getActor());
        assertEquals(table.getOpponentCards(), opponentShows.getCards());
        assertEquals(table.getOpponentHandName(), opponentShows.getDetail());
        assertEvent(events.get(4), "WIN", "HUMAN", 20, 0);
    }

    @Test
    void reportsAShowdownTheOpponentWins() {
        long seed = TableFixtures.seedWhere(
            candidate -> checkedDownWinnerIs(candidate, TableFixtures.OPPONENT_NAME));
        Match match = TableFixtures.dealtMatch(seed);
        TableFixtures.checkDown(match.getCurrentGame());

        TableOutputData table = this.reporter.report(match, 0, TableOutputData.NONE);

        assertEquals("OPPONENT", table.getWinner());
        assertEquals(-10, table.getHumanChipChange());
        assertEquals(990, table.getHumanChips());
        assertEquals(1010, table.getOpponentChips());
        assertEquals(2, table.getOpponentCards().size());
    }

    @Test
    void reportsASplitPot() {
        long seed = TableFixtures.seedWhere(candidate -> checkedDownWinnerIs(candidate, null));
        Match match = TableFixtures.dealtMatch(seed);
        TableFixtures.checkDown(match.getCurrentGame());

        TableOutputData table = this.reporter.report(match, 0, TableOutputData.NONE);

        assertTrue(table.isHandOver());
        assertEquals("SPLIT", table.getWinner());
        assertEquals(0, table.getHumanChipChange());
        assertEquals(1000, table.getHumanChips());
        assertEquals(1000, table.getOpponentChips());
        assertEquals(2, table.getOpponentCards().size());
        List<TableEventData> events = table.getEvents();
        assertEvent(events.get(events.size() - 2), "SPLIT", "HUMAN", 10, 0);
        assertEvent(events.get(events.size() - 1), "SPLIT", "OPPONENT", 10, 0);
    }

    @Test
    void picksOutTheFiveCardsOfTheHumansHandWhenTheHumanWinsAShowdown() {
        long seed = TableFixtures.seedWhere(candidate -> checkedDownWinnerIs(candidate, TableFixtures.HUMAN_NAME));
        Match match = checkedDown(seed);
        Player human = match.getHuman();

        TableOutputData table = this.reporter.report(match, 0, TableOutputData.NONE);

        assertEquals("HUMAN", table.getWinner());
        assertEquals(TableFixtures.texts(match.getCurrentGame().getBestFive(human)), table.getWinningCards());
        assertAreTheHandOf(match, human, table.getWinningCards());
    }

    @Test
    void picksOutTheFiveCardsOfTheOpponentsHandWhenTheOpponentWinsAShowdown() {
        long seed = TableFixtures.seedWhere(
            candidate -> checkedDownWinnerIs(candidate, TableFixtures.OPPONENT_NAME));
        Match match = checkedDown(seed);
        Player computer = match.getComputer();

        TableOutputData table = this.reporter.report(match, 0, TableOutputData.NONE);

        assertEquals("OPPONENT", table.getWinner());
        assertEquals(TableFixtures.texts(match.getCurrentGame().getBestFive(computer)), table.getWinningCards());
        assertAreTheHandOf(match, computer, table.getWinningCards());
    }

    @Test
    void picksOutTheCommunityCardsWhenBothPlayersPlayThemAndSplitThePot() {
        long seed = TableFixtures.seedWhere(candidate -> splitsWithHoleCardsInPlay(candidate, false));
        Match match = checkedDown(seed);
        List<String> board = TableFixtures.texts(match.getCurrentGame().getBoard().getRevealedCards());

        TableOutputData table = this.reporter.report(match, 0, TableOutputData.NONE);

        assertEquals("SPLIT", table.getWinner());
        assertEquals(5, table.getWinningCards().size());
        assertTrue(table.getWinningCards().containsAll(board), "the five cards are the community cards");
        assertEquals(TableFixtures.texts(match.getCurrentGame().getBestFive(match.getHuman())),
                table.getWinningCards());
    }

    @Test
    void picksOutTheCardsOfBothHandsOnceEachWhenAPotIsSplitWithHoleCardsInPlay() {
        long seed = TableFixtures.seedWhere(candidate -> splitsWithHoleCardsInPlay(candidate, true));
        Match match = checkedDown(seed);
        Game game = match.getCurrentGame();
        List<String> humanFive = TableFixtures.texts(game.getBestFive(match.getHuman()));
        List<String> opponentFive = TableFixtures.texts(game.getBestFive(match.getComputer()));

        TableOutputData table = this.reporter.report(match, 0, TableOutputData.NONE);

        // The human's five come first, then what the opponent's five add to them.
        List<String> winning = table.getWinningCards();
        List<String> added = new ArrayList<>(opponentFive);
        added.removeAll(humanFive);
        assertEquals("SPLIT", table.getWinner());
        assertFalse(added.isEmpty());
        assertEquals(humanFive, winning.subList(0, 5));
        assertEquals(added, winning.subList(5, winning.size()));
        assertTrue(winning.containsAll(opponentFive));
        assertEquals(winning.size(), new HashSet<>(winning).size(), "no card is listed twice");
        assertAreTheHandOf(match, match.getHuman(), humanFive);
        assertAreTheHandOf(match, match.getComputer(), opponentFive);
    }

    @Test
    void picksOutNoCardsWhileAHandIsBeingPlayed() {
        Match match = TableFixtures.dealtMatch(1);
        Game game = match.getCurrentGame();

        while (!game.isHandOver()) {
            assertEquals(List.of(), this.reporter.report(match, 0, TableOutputData.NONE).getWinningCards(),
                    "on the " + game.getStreet());
            TableFixtures.actPassively(game);
        }

        assertFalse(this.reporter.report(match, 0, TableOutputData.NONE).getWinningCards().isEmpty(),
                "after the showdown");
    }

    @Test
    void picksOutNoCardsWhenAHandEndsWithAFoldEvenAfterTheFlop() {
        Match match = TableFixtures.dealtMatch(1);
        Game game = match.getCurrentGame();
        TableFixtures.checkTo(game, Street.RIVER);
        game.apply(Action.bet(50));
        game.apply(Action.fold());

        TableOutputData table = this.reporter.report(match, 0, TableOutputData.NONE);

        assertTrue(table.isHandOver());
        assertEquals("RIVER", table.getStreet());
        assertEquals(List.of(), table.getWinningCards());
        assertFalse(table.getHumanHandName().isEmpty());
    }

    @Test
    void reportsAHandThatEndsWhenTheSmallBlindCallsWithTheirLastChipsFromEitherSeat() {
        for (boolean humanIsSmallBlind : new boolean[] {true, false}) {
            Match match = TableFixtures.shortSmallBlindMatch(4, humanIsSmallBlind);
            String caller = "OPPONENT";
            if (humanIsSmallBlind) {
                caller = "HUMAN";
            }
            assertEquals(caller, this.reporter.report(match, 0, TableOutputData.NONE).getToAct());

            match.getCurrentGame().apply(Action.call());
            TableOutputData table = this.reporter.report(match, 2, TableOutputData.NONE);

            // The big blind is not asked: the call ends the betting and the board is run out.
            assertTrue(table.isHandOver(), caller);
            assertEquals("", table.getToAct());
            assertEquals(List.of(), table.getLegalActions());
            assertEquals("SHOWDOWN", table.getStreet());
            assertEquals(5, table.getBoardCards().size());
            List<String> types = typesOf(table.getEvents());
            assertEquals(List.of("CALL", "FLOP", "TURN", "RIVER", "SHOWDOWN", "SHOWDOWN"), types.subList(0, 6));
            assertTrue(List.of(List.of("WIN"), List.of("SPLIT", "SPLIT")).contains(types.subList(6, types.size())),
                    types.toString());
            assertEvent(table.getEvents().get(0), "CALL", caller, 5, 10);
            assertEquals(2000, table.getHumanChips() + table.getOpponentChips());
            assertEquals(2, table.getOpponentCards().size());
            assertFalse(table.getWinningCards().isEmpty());
        }
    }

    @Test
    void countsAReturnedBetInTheChipChangeAndEndsTheMatchWhenTheHumanIsOut() {
        long seed = TableFixtures.seedWhere(candidate -> allInWinnerIs(candidate, TableFixtures.OPPONENT_NAME));
        Match match = allInSecondHand(seed);

        TableOutputData table = this.reporter.report(match, 3, TableOutputData.NONE);

        // The opponent raised to 1005; the human had only 995 in all, so 10 went back to the opponent.
        List<TableEventData> events = table.getEvents();
        assertEvent(events.get(0), "CALL", "HUMAN", 985, 995);
        assertEvent(events.get(1), "REFUND", "OPPONENT", 10, 995);
        assertEquals(List.of("FLOP", "TURN", "RIVER", "SHOWDOWN", "SHOWDOWN", "WIN"),
                typesOf(events.subList(2, events.size())));
        assertEquals("OPPONENT", events.get(5).getActor());
        assertEquals("HUMAN", events.get(6).getActor());
        assertEquals(-995, table.getHumanChipChange());
        assertEquals(0, table.getHumanChips());
        assertEquals(2000, table.getOpponentChips());
        assertFalse(table.isHumanSmallBlind());
        assertTrue(table.isMatchOver());
        assertEquals(2, table.getHandNumber());
    }

    @Test
    void doesNotEndTheMatchWhileBothPlayersCanStillPostTheBigBlind() {
        long seed = TableFixtures.seedWhere(candidate -> allInWinnerIs(candidate, TableFixtures.HUMAN_NAME));
        Match match = allInSecondHand(seed);

        TableOutputData table = this.reporter.report(match, 0, TableOutputData.NONE);

        // The human doubles up to 1990; the opponent keeps the 10 that came back.
        assertEquals("HUMAN", table.getWinner());
        assertEquals(995, table.getHumanChipChange());
        assertEquals(1990, table.getHumanChips());
        assertEquals(10, table.getOpponentChips());
        assertFalse(table.isMatchOver());
    }

    @Test
    void describesAMatchThatWasStartedAgain() {
        Match match = TableFixtures.dealtMatch(1);
        match.restart();

        TableOutputData table = this.reporter.report(match, 0, TableOutputData.NONE);

        assertEquals(0, table.getHandNumber());
        assertEquals(List.of(), table.getHumanCards());
        assertEquals(1000, table.getHumanChips());
        assertEquals(1000, table.getOpponentChips());
        assertEquals("", table.getToAct());
    }

    @Test
    void passesOnWhoDecidedTheOpponentsAction() {
        Match match = TableFixtures.dealtMatch(1);

        assertEquals("LLM", this.reporter.report(match, 0, TableOutputData.DECIDED_BY_LLM).getDecidedBy());
        assertEquals("BUILT_IN",
                this.reporter.report(match, 0, TableOutputData.DECIDED_BY_BUILT_IN).getDecidedBy());
    }

    /**
     * Deals the first hand from the seed, checks it down, and says whether the
     * named player won; a null name asks for a split pot.
     */
    private static boolean checkedDownWinnerIs(long seed, String winnerName) {
        return isWinner(checkedDown(seed).getCurrentGame().getWinner(), winnerName);
    }

    /**
     * Deals the first hand from the seed and checks it down to the showdown.
     */
    private static Match checkedDown(long seed) {
        Match match = TableFixtures.dealtMatch(seed);
        TableFixtures.checkDown(match.getCurrentGame());
        return match;
    }

    /**
     * Deals the first hand from the seed, checks it down, and says whether the
     * pot is split in the wanted way: with a hole card in at least one of the
     * two best hands, or with both players playing the five community cards.
     */
    private static boolean splitsWithHoleCardsInPlay(long seed, boolean wanted) {
        Match match = checkedDown(seed);
        Game game = match.getCurrentGame();
        Set<String> cards = new HashSet<>(TableFixtures.texts(game.getBestFive(match.getHuman())));
        cards.addAll(TableFixtures.texts(game.getBestFive(match.getComputer())));
        // The two hands share all five cards only if those are the community cards.
        return game.getWinner() == null && cards.size() > 5 == wanted;
    }

    /**
     * Checks that the cards are five different ones, each of them one of the
     * player's hole cards or a community card, and that together they are
     * worth exactly what the player's best hand is worth.
     */
    private static void assertAreTheHandOf(Match match, Player player, List<String> cards) {
        Game game = match.getCurrentGame();
        List<String> available = TableFixtures.holeCards(player);
        available.addAll(TableFixtures.texts(game.getBoard().getRevealedCards()));
        assertEquals(5, cards.size());
        assertEquals(5, new HashSet<>(cards).size(), "five different cards");
        assertTrue(available.containsAll(cards), cards + " are among " + available);
        assertEquals(game.getHandValue(player), new HandEvaluator().evaluate(Cards.cards(String.join(" ", cards))));
    }

    private static boolean allInWinnerIs(long seed, String winnerName) {
        return isWinner(allInSecondHand(seed).getCurrentGame().getWinner(), winnerName);
    }

    private static boolean isWinner(Player winner, String winnerName) {
        if (winner == null) {
            return winnerName == null;
        }
        return winner.getName().equals(winnerName);
    }

    /**
     * Plays a second hand in which the opponent, with 1005 chips, puts
     * everything in and the human, with 995, calls.
     */
    private static Match allInSecondHand(long seed) {
        Match match = TableFixtures.secondHandMatch(seed);
        Game game = match.getCurrentGame();
        game.apply(Action.raise(1005));
        game.apply(Action.call());
        return match;
    }

    private static void assertEvent(TableEventData event, String type, String actor, int amount, int total) {
        assertEquals(type, event.getType());
        assertEquals(actor, event.getActor());
        assertEquals(amount, event.getAmount());
        assertEquals(total, event.getTotal());
    }

    private static List<String> typesOf(List<TableEventData> events) {
        List<String> types = new ArrayList<>();
        for (TableEventData event : events) {
            types.add(event.getType());
        }
        return types;
    }

    private static List<String> allCardsIn(TableOutputData table) {
        List<String> cards = new ArrayList<>(table.getHumanCards());
        cards.addAll(table.getBoardCards());
        for (TableEventData event : table.getEvents()) {
            cards.addAll(event.getCards());
        }
        return cards;
    }
}
