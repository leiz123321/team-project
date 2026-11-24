package interface_adapter.table;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.beans.PropertyChangeEvent;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import use_case.table.TableEventData;
import use_case.table.TableOutputData;

/**
 * Pins down what the game screen is promised about the states the presenters
 * of the game publish.
 */
class TablePublisherTest {
    private static final double EXACT = 0.0;
    private static final List<String> NO_CARDS = List.of();

    private TableViewModel viewModel;
    private TablePublisher publisher;
    private List<PropertyChangeEvent> heard;

    @BeforeEach
    void setUp() {
        this.viewModel = new TableViewModel();
        this.publisher = new TablePublisher(this.viewModel);
        this.heard = new ArrayList<>();
        this.viewModel.addPropertyChangeListener(this.heard::add);
    }

    @Test
    void needsAViewModel() {
        assertThrows(IllegalArgumentException.class, () -> new TablePublisher(null));
    }

    @Test
    void numbersTheStatesAndTellsTheScreenAboutEachOne() {
        this.publisher.publishTable(midHand().build());
        TableState first = this.viewModel.getState();
        this.publisher.publishTable(midHand().build());
        TableState second = this.viewModel.getState();
        this.publisher.publishFailure("It is not your turn.");
        TableState third = this.viewModel.getState();

        assertEquals(1, first.getRevision());
        assertEquals(2, second.getRevision());
        assertEquals(3, third.getRevision());
        assertEquals(3, this.heard.size());
        assertEquals(TableViewModel.STATE_PROPERTY, this.heard.get(0).getPropertyName());
        assertSame(first, this.heard.get(0).getNewValue());
        assertSame(second, this.heard.get(1).getNewValue());
        assertSame(third, this.heard.get(2).getNewValue());
    }

    @Test
    void sharesTheSequenceOfRevisionsWithOtherPublishersOfTheSameViewModel() {
        TablePublisher other = new TablePublisher(this.viewModel);

        this.publisher.publishTable(midHand().build());
        other.publishTable(midHand().build());
        this.publisher.publishFailure("It is not your turn.");
        other.publishTable(midHand().build());

        assertEquals(4, this.viewModel.getState().getRevision());
    }

    @Test
    void showsAsThePotOnlyTheChipsAlreadyPulledIntoTheMiddle() {
        // 100 from before the flop, and 20 and 60 in front of the players now.
        this.publisher.publishTable(midHand().setPot(180).setHumanBet(20).setOpponentBet(60).build());

        TableState state = this.viewModel.getState();
        assertEquals(100, state.getPot());
        assertEquals(20, state.getHumanBet());
        assertEquals(60, state.getOpponentBet());
    }

    @Test
    void showsAnEmptyMiddleBeforeTheFlopAndAfterTheHand() {
        this.publisher.publishTable(midHand().setPot(15).setHumanBet(5).setOpponentBet(10).build());
        assertEquals(0, this.viewModel.getState().getPot());

        this.publisher.publishTable(midHand().setPot(0).setHumanBet(0).setOpponentBet(0).setHandOver(true)
                .setToAct(TableOutputData.NONE).build());
        assertEquals(0, this.viewModel.getState().getPot());
    }

    @Test
    void passesOnThePlainFactsOfTheTable() {
        this.publisher.publishTable(midHand()
                .setHumanChipChange(0)
                .setDecidedBy(TableOutputData.DECIDED_BY_BUILT_IN)
                .build());

        TableState state = this.viewModel.getState();
        assertEquals(3, state.getHandNumber());
        assertEquals("You", state.getHumanName());
        assertEquals("Opponent", state.getOpponentName());
        assertEquals("FLOP", state.getStreet());
        assertEquals(930, state.getHumanChips());
        assertEquals(890, state.getOpponentChips());
        assertEquals(0.5, state.getWinChance(), EXACT);
        assertEquals(0.125, state.getTieChance(), EXACT);
        assertEquals(0.375, state.getLoseChance(), EXACT);
        assertEquals("Pair", state.getHumanHandName());
        assertEquals("", state.getOpponentHandName());
        assertEquals("BUILT_IN", state.getDecidedBy());
        assertEquals("", state.getWinner());
        assertEquals("", state.getErrorMessage());
        assertFalse(state.isHandOver());
        assertFalse(state.isMatchOver());
    }

    @Test
    void showsWhoHasTheDealerButton() {
        this.publisher.publishTable(midHand().setHumanSmallBlind(true).build());
        assertTrue(this.viewModel.getState().isHumanDealer());

        this.publisher.publishTable(midHand().setHumanSmallBlind(false).build());
        assertFalse(this.viewModel.getState().isHumanDealer());
    }

    @Test
    void turnsCardTextIntoPictureIds() {
        this.publisher.publishTable(midHand().build());

        TableState state = this.viewModel.getState();
        assertEquals(List.of("AS", "10H"), state.getHumanCards());
        assertEquals(List.of("QD", "7C", "2S"), state.getBoardCards());
    }

    @Test
    void showsTheOpponentsCardsFaceDownWhileTheyAreHidden() {
        this.publisher.publishTable(midHand().build());
        assertEquals(List.of("back", "back"), this.viewModel.getState().getOpponentCards());

        // A hand that ended with a fold never shows them either.
        this.publisher.publishTable(midHand().setHandOver(true).setWinner(TableOutputData.HUMAN)
                .setToAct(TableOutputData.NONE).build());
        assertEquals(List.of("back", "back"), this.viewModel.getState().getOpponentCards());
    }

    @Test
    void showsTheOpponentsCardsAfterAShowdown() {
        this.publisher.publishTable(showdown(TableOutputData.OPPONENT).build());

        TableState state = this.viewModel.getState();
        assertEquals(List.of("KC", "KD"), state.getOpponentCards());
        assertEquals("Full House", state.getOpponentHandName());
        assertEquals("OPPONENT", state.getWinner());
        assertEquals(-480, state.getHumanChipChange());
        assertTrue(state.isHandOver());
    }

    @Test
    void picksOutTheWinningCardsAsPictureIdsAfterAShowdown() {
        this.publisher.publishTable(showdown(TableOutputData.OPPONENT)
                .setWinningCards(List.of("K♣", "K♦", "Q♦", "2♠", "2♦")).build());

        TableState state = this.viewModel.getState();
        assertEquals(List.of("KC", "KD", "QD", "2S", "2D"), state.getWinningCards());
        // Each of them is a card the screen is already showing.
        List<String> shown = new ArrayList<>(state.getOpponentCards());
        shown.addAll(state.getBoardCards());
        assertTrue(shown.containsAll(state.getWinningCards()));
    }

    @Test
    void picksOutTheCardsOfBothHandsWhenThePotIsSplit() {
        this.publisher.publishTable(midHand().setHandOver(true).setWinner(TableOutputData.SPLIT)
                .setStreet("SHOWDOWN").setToAct(TableOutputData.NONE)
                .setBoardCards(List.of("Q♦", "7♣", "2♠", "2♦", "9♠"))
                .setOpponentCards(List.of("A♦", "10♣")).setOpponentHandName("Pair")
                .setWinningCards(List.of("A♠", "10♥", "Q♦", "2♠", "2♦", "A♦", "10♣")).build());

        assertEquals(List.of("AS", "10H", "QD", "2S", "2D", "AD", "10C"),
                this.viewModel.getState().getWinningCards());
    }

    @Test
    void picksOutNoCardsWhileAHandIsPlayedOrAfterAFold() {
        this.publisher.publishTable(midHand().build());
        assertEquals(List.of(), this.viewModel.getState().getWinningCards());

        this.publisher.publishTable(midHand().setHandOver(true).setWinner(TableOutputData.HUMAN)
                .setToAct(TableOutputData.NONE).build());
        assertEquals(List.of(), this.viewModel.getState().getWinningCards());
    }

    @Test
    void dropsTheWinningCardsWhenTheNextHandIsDealtAndKeepsThemWhenARequestFails() {
        this.publisher.publishTable(showdown(TableOutputData.HUMAN)
                .setWinningCards(List.of("A♠", "10♥", "Q♦", "2♠", "2♦")).build());
        TableState shown = this.viewModel.getState();

        this.publisher.publishFailure("There is no hand in progress.");
        assertEquals(List.of("AS", "10H", "QD", "2S", "2D"), this.viewModel.getState().getWinningCards());
        TableStateAssertions.assertFailureOf(shown, this.viewModel.getState(), "There is no hand in progress.");

        this.publisher.publishTable(midHand().setHandNumber(4).build());
        assertEquals(List.of(), this.viewModel.getState().getWinningCards());
    }

    @Test
    void showsNoCardsBeforeTheFirstHand() {
        this.publisher.publishTable(new TableOutputData.Builder().setStreet("PREFLOP").build());

        TableState state = this.viewModel.getState();
        assertEquals(List.of(), state.getHumanCards());
        assertEquals(List.of(), state.getOpponentCards());
        assertEquals(List.of(), state.getBoardCards());
        assertEquals(List.of(), state.getWinningCards());
        assertFalse(state.isHandInProgress());
        assertFalse(state.isHandOver());
        assertEquals("", state.getMessage());
    }

    @Test
    void saysWhetherAHandIsBeingPlayed() {
        this.publisher.publishTable(midHand().build());
        assertTrue(this.viewModel.getState().isHandInProgress());

        this.publisher.publishTable(midHand().setHandOver(true).setToAct(TableOutputData.NONE).build());
        assertFalse(this.viewModel.getState().isHandInProgress());
        assertTrue(this.viewModel.getState().isHandOver());
    }

    @Test
    void offersTheHumanTheLegalActionsOnTheirTurn() {
        this.publisher.publishTable(midHand()
                .setToAct(TableOutputData.HUMAN)
                .setLegalActions(List.of("FOLD", "CALL", "RAISE"))
                .setCallAmount(40)
                .setMinWager(100)
                .setMaxWager(950)
                .build());

        TableState state = this.viewModel.getState();
        assertTrue(state.isHumanToAct());
        assertFalse(state.isOpponentToAct());
        assertTrue(state.canFold());
        assertFalse(state.canCheck());
        assertTrue(state.canCall());
        assertFalse(state.canBet());
        assertTrue(state.canRaise());
        assertEquals(40, state.getCallAmount());
        assertEquals(100, state.getMinWager());
        assertEquals(950, state.getMaxWager());
    }

    @Test
    void offersACheckAndABetWhenNobodyHasBet() {
        this.publisher.publishTable(midHand()
                .setToAct(TableOutputData.HUMAN)
                .setLegalActions(List.of("FOLD", "CHECK", "BET"))
                .build());

        TableState state = this.viewModel.getState();
        assertTrue(state.canFold());
        assertTrue(state.canCheck());
        assertFalse(state.canCall());
        assertTrue(state.canBet());
        assertFalse(state.canRaise());
    }

    @Test
    void offersNothingOnTheOpponentsTurnOrAfterTheHand() {
        this.publisher.publishTable(midHand().setToAct(TableOutputData.OPPONENT).build());
        TableState waiting = this.viewModel.getState();
        assertTrue(waiting.isOpponentToAct());
        assertFalse(waiting.isHumanToAct());
        assertNothingAllowed(waiting);

        this.publisher.publishTable(midHand().setHandOver(true).setToAct(TableOutputData.NONE).build());
        TableState over = this.viewModel.getState();
        assertFalse(over.isOpponentToAct());
        assertFalse(over.isHumanToAct());
        assertNothingAllowed(over);
    }

    @Test
    void carriesOnlyTheEventsOfTheLatestReport() {
        this.publisher.publishTable(midHand().setEvents(List.of(
                event("CALL", TableOutputData.HUMAN, 20, 40, NO_CARDS, ""),
                event("FLOP", TableOutputData.NONE, 0, 0, List.of("Q♦", "7♣", "2♠"), ""))).build());
        assertEquals(2, this.viewModel.getState().getEvents().size());

        this.publisher.publishTable(midHand().setEvents(List.of(
                event("BET", TableOutputData.OPPONENT, 60, 60, NO_CARDS, ""))).build());

        List<TableEventView> events = this.viewModel.getState().getEvents();
        assertEquals(1, events.size());
        assertEquals("BET", events.get(0).getType());
        assertEquals("OPPONENT", events.get(0).getActor());
        assertEquals(60, events.get(0).getAmount());
        assertEquals(60, events.get(0).getTotal());
        assertEquals(List.of(), events.get(0).getCards());
    }

    @Test
    void givesTheCardsOfAnEventAsPictureIds() {
        this.publisher.publishTable(midHand().setEvents(List.of(
                event("FLOP", TableOutputData.NONE, 0, 0, List.of("Q♦", "7♣", "2♠"), ""))).build());

        TableEventView flop = this.viewModel.getState().getEvents().get(0);
        assertEquals("FLOP", flop.getType());
        assertEquals("", flop.getActor());
        assertEquals(List.of("QD", "7C", "2S"), flop.getCards());
        assertEquals("Flop: Q♦ 7♣ 2♠", flop.getText());
    }

    @Test
    void putsEveryKindOfEventIntoWords() {
        assertEquals("You fold", textOf(event("FOLD", TableOutputData.HUMAN, 0, 5, NO_CARDS, "")));
        assertEquals("Opponent folds", textOf(event("FOLD", TableOutputData.OPPONENT, 0, 5, NO_CARDS, "")));
        assertEquals("You check", textOf(event("CHECK", TableOutputData.HUMAN, 0, 0, NO_CARDS, "")));
        assertEquals("Opponent checks", textOf(event("CHECK", TableOutputData.OPPONENT, 0, 0, NO_CARDS, "")));
        assertEquals("You call 20", textOf(event("CALL", TableOutputData.HUMAN, 20, 40, NO_CARDS, "")));
        assertEquals("Opponent calls 20", textOf(event("CALL", TableOutputData.OPPONENT, 20, 40, NO_CARDS, "")));
        assertEquals("You bet 30", textOf(event("BET", TableOutputData.HUMAN, 30, 30, NO_CARDS, "")));
        assertEquals("Opponent bets 30", textOf(event("BET", TableOutputData.OPPONENT, 30, 30, NO_CARDS, "")));
        assertEquals("You raise to 60", textOf(event("RAISE", TableOutputData.HUMAN, 40, 60, NO_CARDS, "")));
        assertEquals("Opponent raises to 60",
                textOf(event("RAISE", TableOutputData.OPPONENT, 40, 60, NO_CARDS, "")));
        assertEquals("You take back 10", textOf(event("REFUND", TableOutputData.HUMAN, 10, 995, NO_CARDS, "")));
        assertEquals("Opponent takes back 10",
                textOf(event("REFUND", TableOutputData.OPPONENT, 10, 995, NO_CARDS, "")));
        assertEquals("Flop: A♠ 10♥ Q♦",
                textOf(event("FLOP", TableOutputData.NONE, 0, 0, List.of("A♠", "10♥", "Q♦"), "")));
        assertEquals("Turn: 7♣", textOf(event("TURN", TableOutputData.NONE, 0, 0, List.of("7♣"), "")));
        assertEquals("River: 2♦", textOf(event("RIVER", TableOutputData.NONE, 0, 0, List.of("2♦"), "")));
        assertEquals("You show A♠ A♥ — Pair",
                textOf(event("SHOWDOWN", TableOutputData.HUMAN, 0, 0, List.of("A♠", "A♥"), "Pair")));
        assertEquals("Opponent shows K♣ K♦ — Full House",
                textOf(event("SHOWDOWN", TableOutputData.OPPONENT, 0, 0, List.of("K♣", "K♦"), "Full House")));
        assertEquals("You win 240", textOf(event("WIN", TableOutputData.HUMAN, 240, 0, NO_CARDS, "")));
        assertEquals("Opponent wins 240", textOf(event("WIN", TableOutputData.OPPONENT, 240, 0, NO_CARDS, "")));
        assertEquals("You take 120 of the split pot",
                textOf(event("SPLIT", TableOutputData.HUMAN, 120, 0, NO_CARDS, "")));
        assertEquals("Opponent takes 120 of the split pot",
                textOf(event("SPLIT", TableOutputData.OPPONENT, 120, 0, NO_CARDS, "")));
    }

    @Test
    void namesTheBlindEachPlayerPosts() {
        List<TableEventData> humanFirst = List.of(
                event("BLIND", TableOutputData.HUMAN, 5, 5, NO_CARDS, ""),
                event("BLIND", TableOutputData.OPPONENT, 10, 10, NO_CARDS, ""));
        this.publisher.publishTable(midHand().setHumanSmallBlind(true).setEvents(humanFirst).build());
        assertEquals("You post the small blind of 5", this.viewModel.getState().getEvents().get(0).getText());
        assertEquals("Opponent posts the big blind of 10", this.viewModel.getState().getEvents().get(1).getText());

        List<TableEventData> opponentFirst = List.of(
                event("BLIND", TableOutputData.OPPONENT, 5, 5, NO_CARDS, ""),
                event("BLIND", TableOutputData.HUMAN, 10, 10, NO_CARDS, ""));
        this.publisher.publishTable(midHand().setHumanSmallBlind(false).setEvents(opponentFirst).build());
        assertEquals("Opponent posts the small blind of 5", this.viewModel.getState().getEvents().get(0).getText());
        assertEquals("You post the big blind of 10", this.viewModel.getState().getEvents().get(1).getText());
    }

    @Test
    void keepsTheTypeAsTheTextOfAnEventItDoesNotKnow() {
        assertEquals("MISDEAL", textOf(event("MISDEAL", TableOutputData.NONE, 0, 0, NO_CARDS, "")));
    }

    @Test
    void saysWhenAPlayerPutsTheirLastChipsIn() {
        this.publisher.publishTable(midHand().setHumanChips(0).setEvents(List.of(
                event("CALL", TableOutputData.HUMAN, 480, 500, NO_CARDS, ""))).build());
        assertEquals("You call 480 and are all in", this.viewModel.getState().getEvents().get(0).getText());
        assertEquals("You call 480 and are all in.", this.viewModel.getState().getMessage());

        this.publisher.publishTable(midHand().setOpponentChips(0).setEvents(List.of(
                event("RAISE", TableOutputData.OPPONENT, 990, 1000, NO_CARDS, ""))).build());
        assertEquals("Opponent raises to 1000 and is all in",
                this.viewModel.getState().getEvents().get(0).getText());

        this.publisher.publishTable(midHand().setHumanChips(0).setEvents(List.of(
                event("BET", TableOutputData.HUMAN, 400, 400, NO_CARDS, ""))).build());
        assertEquals("You bet 400 and are all in", this.viewModel.getState().getEvents().get(0).getText());

        this.publisher.publishTable(midHand().setOpponentChips(0).setHumanSmallBlind(true).setEvents(List.of(
                event("BLIND", TableOutputData.HUMAN, 5, 5, NO_CARDS, ""),
                event("BLIND", TableOutputData.OPPONENT, 10, 10, NO_CARDS, ""))).build());
        assertEquals("You post the small blind of 5", this.viewModel.getState().getEvents().get(0).getText());
        assertEquals("Opponent posts the big blind of 10 and is all in",
                this.viewModel.getState().getEvents().get(1).getText());
    }

    @Test
    void doesNotCallAPlayerAllInWhoStillHasChips() {
        this.publisher.publishTable(midHand().setHumanChips(1).setOpponentChips(0).setEvents(List.of(
                event("CALL", TableOutputData.HUMAN, 480, 500, NO_CARDS, ""))).build());

        assertEquals("You call 480", this.viewModel.getState().getEvents().get(0).getText());
    }

    @Test
    void stillSeesAnAllInWhenThePlayerWinsThePotInTheSameStep() {
        // The human called with their last 480 and then won 960: the stack is full again, the call was all in.
        this.publisher.publishTable(showdown(TableOutputData.HUMAN).setHumanChips(960).setOpponentChips(1040)
                .build());

        List<TableEventView> events = this.viewModel.getState().getEvents();
        assertEquals("You call 480 and are all in", events.get(0).getText());
        assertEquals("You win 960", events.get(events.size() - 1).getText());
    }

    @Test
    void seesTheAllInOfAPlayerWhoseBetWasPartlyReturned() {
        // The opponent bet their last 500, the human could only call 300 of it, and 200 went back.
        this.publisher.publishTable(midHand().setHumanChips(300).setOpponentChips(0).setEvents(List.of(
                event("BET", TableOutputData.OPPONENT, 500, 500, NO_CARDS, ""))).build());
        assertEquals("Opponent bets 500 and is all in", this.viewModel.getState().getEvents().get(0).getText());

        this.publisher.publishTable(midHand().setHumanChips(0).setOpponentChips(200).setEvents(List.of(
                event("CALL", TableOutputData.HUMAN, 300, 300, NO_CARDS, ""),
                event("REFUND", TableOutputData.OPPONENT, 200, 300, NO_CARDS, ""))).build());
        List<TableEventView> events = this.viewModel.getState().getEvents();
        assertEquals("You call 300 and are all in", events.get(0).getText());
        assertEquals("Opponent takes back 200", events.get(1).getText());
    }

    @Test
    void announcesANewHandWithTheBlindTheHumanPosts() {
        this.publisher.publishTable(midHand().setHandNumber(3).setHumanSmallBlind(true).setEvents(List.of(
                event("BLIND", TableOutputData.HUMAN, 5, 5, NO_CARDS, ""),
                event("BLIND", TableOutputData.OPPONENT, 10, 10, NO_CARDS, ""))).build());
        assertEquals("Hand 3 — you post the small blind of 5.", this.viewModel.getState().getMessage());

        this.publisher.publishTable(midHand().setHandNumber(4).setHumanSmallBlind(false).setEvents(List.of(
                event("BLIND", TableOutputData.OPPONENT, 5, 5, NO_CARDS, ""),
                event("BLIND", TableOutputData.HUMAN, 10, 10, NO_CARDS, ""))).build());
        assertEquals("Hand 4 — you post the big blind of 10.", this.viewModel.getState().getMessage());
    }

    @Test
    void saysWhatActionWasJustTaken() {
        assertEquals("Opponent raises to 60.",
                messageFor(event("RAISE", TableOutputData.OPPONENT, 40, 60, NO_CARDS, "")));
        assertEquals("You check.", messageFor(event("CHECK", TableOutputData.HUMAN, 0, 0, NO_CARDS, "")));
        assertEquals("You bet 30.", messageFor(event("BET", TableOutputData.HUMAN, 30, 30, NO_CARDS, "")));
    }

    @Test
    void namesTheActionAndNotTheCardsWhenTheActionBringsANewStreet() {
        this.publisher.publishTable(midHand().setEvents(List.of(
                event("CALL", TableOutputData.OPPONENT, 20, 40, NO_CARDS, ""),
                event("FLOP", TableOutputData.NONE, 0, 0, List.of("Q♦", "7♣", "2♠"), ""))).build());

        assertEquals("Opponent calls 20.", this.viewModel.getState().getMessage());
    }

    @Test
    void saysWhoWinsWhenTheOtherPlayerFolds() {
        this.publisher.publishTable(midHand().setHandOver(true).setWinner(TableOutputData.HUMAN)
                .setToAct(TableOutputData.NONE).setEvents(List.of(
                        event("FOLD", TableOutputData.OPPONENT, 0, 10, NO_CARDS, ""),
                        event("WIN", TableOutputData.HUMAN, 30, 0, NO_CARDS, ""))).build());
        assertEquals("Opponent folds and you win 30.", this.viewModel.getState().getMessage());

        this.publisher.publishTable(midHand().setHandOver(true).setWinner(TableOutputData.OPPONENT)
                .setToAct(TableOutputData.NONE).setEvents(List.of(
                        event("FOLD", TableOutputData.HUMAN, 0, 5, NO_CARDS, ""),
                        event("WIN", TableOutputData.OPPONENT, 45, 0, NO_CARDS, ""))).build());
        assertEquals("You fold and Opponent wins 45.", this.viewModel.getState().getMessage());
    }

    @Test
    void saysWhoWinsAShowdownAndWithWhat() {
        this.publisher.publishTable(showdown(TableOutputData.HUMAN).build());
        assertEquals("You win 960 — Flush.", this.viewModel.getState().getMessage());

        this.publisher.publishTable(showdown(TableOutputData.OPPONENT).build());
        assertEquals("Opponent wins 960 — Full House.", this.viewModel.getState().getMessage());
    }

    @Test
    void saysWhatTheHumanTakesFromASplitPot() {
        this.publisher.publishTable(midHand().setHandOver(true).setWinner(TableOutputData.SPLIT)
                .setStreet("SHOWDOWN").setToAct(TableOutputData.NONE)
                .setOpponentCards(List.of("A♦", "10♣")).setOpponentHandName("Pair").setEvents(List.of(
                        event("CHECK", TableOutputData.HUMAN, 0, 0, NO_CARDS, ""),
                        event("SHOWDOWN", TableOutputData.OPPONENT, 0, 0, List.of("A♦", "10♣"), "Pair"),
                        event("SHOWDOWN", TableOutputData.HUMAN, 0, 0, List.of("A♠", "10♥"), "Pair"),
                        event("SPLIT", TableOutputData.OPPONENT, 240, 0, NO_CARDS, ""),
                        event("SPLIT", TableOutputData.HUMAN, 241, 0, NO_CARDS, ""))).build());

        assertEquals("Split pot — you take 241.", this.viewModel.getState().getMessage());
    }

    @Test
    void saysWhoWonAMatchThatIsOver() {
        this.publisher.publishTable(midHand().setHandOver(true).setMatchOver(true).setToAct(TableOutputData.NONE)
                .setHumanChips(2000).setOpponentChips(0).build());
        assertEquals("The match is over — you won.", this.viewModel.getState().getMessage());
        assertTrue(this.viewModel.getState().isMatchOver());

        this.publisher.publishTable(midHand().setHandOver(true).setMatchOver(true).setToAct(TableOutputData.NONE)
                .setHumanChips(0).setOpponentChips(2000).build());
        assertEquals("The match is over — Opponent won.", this.viewModel.getState().getMessage());
    }

    @Test
    void hasNothingToSayWhenNothingHappened() {
        this.publisher.publishTable(midHand().build());

        assertEquals("", this.viewModel.getState().getMessage());
        assertEquals(List.of(), this.viewModel.getState().getEvents());
    }

    @Test
    void republishesTheOldStateWithTheErrorWhenARequestFails() {
        this.publisher.publishTable(midHand()
                .setToAct(TableOutputData.HUMAN)
                .setLegalActions(List.of("FOLD", "CALL", "RAISE"))
                .setCallAmount(40)
                .setMinWager(100)
                .setMaxWager(950)
                .setDecidedBy(TableOutputData.DECIDED_BY_LLM)
                .setEvents(List.of(event("RAISE", TableOutputData.OPPONENT, 40, 60, NO_CARDS, "")))
                .build());
        TableState before = this.viewModel.getState();

        this.publisher.publishFailure("A raise must be between 100 and 950.");

        TableState after = this.viewModel.getState();
        TableStateAssertions.assertFailureOf(before, after, "A raise must be between 100 and 950.");
        assertEquals("Opponent raises to 60.", after.getMessage());
        assertEquals(List.of(), after.getEvents());
        assertEquals(1, before.getEvents().size());
        assertEquals("", before.getErrorMessage());
    }

    @Test
    void clearsTheErrorWithTheNextStateThatSucceeds() {
        this.publisher.publishTable(midHand().build());
        this.publisher.publishFailure("It is not your turn.");
        this.publisher.publishFailure("There is nothing to call. You can check.");
        assertEquals("There is nothing to call. You can check.", this.viewModel.getState().getErrorMessage());

        this.publisher.publishTable(midHand().build());

        assertEquals("", this.viewModel.getState().getErrorMessage());
        assertEquals(4, this.viewModel.getState().getRevision());
    }

    @Test
    void canReportAFailureBeforeAnyHandWasDealt() {
        TableState before = this.viewModel.getState();

        this.publisher.publishFailure("There is no hand in progress.");

        TableStateAssertions.assertFailureOf(before, this.viewModel.getState(), "There is no hand in progress.");
        assertEquals(1, this.viewModel.getState().getRevision());
    }

    @Test
    void publishesNothingWhenTheReportCannotBeShown() {
        this.publisher.publishTable(midHand().build());
        TableState before = this.viewModel.getState();

        assertThrows(IllegalArgumentException.class, () -> this.publisher.publishFailure(null));
        assertThrows(IllegalArgumentException.class,
                () -> this.publisher.publishTable(midHand().setHumanCards(List.of("A♠", "joker")).build()));
        assertThrows(IllegalArgumentException.class,
                () -> this.publisher.publishTable(midHand().setWinningCards(List.of("A♠", "back")).build()));

        assertSame(before, this.viewModel.getState());
        assertEquals(1, this.heard.size());
    }

    /**
     * Returns a table on the flop of the third hand, with nobody to act and no
     * events; each test sets what it is about.
     */
    private static TableOutputData.Builder midHand() {
        return new TableOutputData.Builder()
                .setHandNumber(3)
                .setHumanName("You")
                .setOpponentName("Opponent")
                .setStreet("FLOP")
                .setPot(180)
                .setHumanChips(930)
                .setOpponentChips(890)
                .setHumanBet(0)
                .setOpponentBet(0)
                .setHumanCards(List.of("A♠", "10♥"))
                .setBoardCards(List.of("Q♦", "7♣", "2♠"))
                .setHumanSmallBlind(true)
                .setWinChance(0.5)
                .setTieChance(0.125)
                .setLoseChance(0.375)
                .setHumanHandName("Pair");
    }

    /**
     * Returns the table after the human called with their last 480 chips and
     * the hands were shown, won by the given player.
     */
    private static TableOutputData.Builder showdown(String winner) {
        int humanChipChange = -480;
        int humanChips = 0;
        if (TableOutputData.HUMAN.equals(winner)) {
            humanChipChange = 480;
            humanChips = 960;
        }
        return midHand()
                .setStreet("SHOWDOWN")
                .setPot(0)
                .setHumanChips(humanChips)
                .setOpponentChips(2000 - humanChips)
                .setBoardCards(List.of("Q♦", "7♣", "2♠", "2♦", "9♠"))
                .setOpponentCards(List.of("K♣", "K♦"))
                .setHandOver(true)
                .setWinner(winner)
                .setHumanHandName("Flush")
                .setOpponentHandName("Full House")
                .setHumanChipChange(humanChipChange)
                .setEvents(List.of(
                        event("CALL", TableOutputData.HUMAN, 480, 480, NO_CARDS, ""),
                        event("TURN", TableOutputData.NONE, 0, 0, List.of("2♦"), ""),
                        event("RIVER", TableOutputData.NONE, 0, 0, List.of("9♠"), ""),
                        event("SHOWDOWN", TableOutputData.HUMAN, 0, 0, List.of("A♠", "10♥"), "Flush"),
                        event("SHOWDOWN", TableOutputData.OPPONENT, 0, 0, List.of("K♣", "K♦"), "Full House"),
                        event("WIN", winner, 960, 0, NO_CARDS, "")));
    }

    private static TableEventData event(String type, String actor, int amount, int total, List<String> cards,
                                        String detail) {
        return new TableEventData(type, actor, amount, total, cards, detail);
    }

    /**
     * Publishes a table whose only event is the given one, with both players
     * holding chips, and returns the text that event gets.
     */
    private String textOf(TableEventData event) {
        this.publisher.publishTable(midHand().setEvents(List.of(event)).build());
        return this.viewModel.getState().getEvents().get(0).getText();
    }

    private String messageFor(TableEventData event) {
        this.publisher.publishTable(midHand().setEvents(List.of(event)).build());
        return this.viewModel.getState().getMessage();
    }

    private static void assertNothingAllowed(TableState state) {
        assertFalse(state.canFold());
        assertFalse(state.canCheck());
        assertFalse(state.canCall());
        assertFalse(state.canBet());
        assertFalse(state.canRaise());
    }
}
