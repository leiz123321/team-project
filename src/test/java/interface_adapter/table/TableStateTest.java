package interface_adapter.table;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.junit.jupiter.api.Test;

import use_case.table.TableOutputData;

class TableStateTest {
    private static final double EXACT = 0.0;
    private static final TableEventView EVENT =
            new TableEventView(TableEventView.CALL, TableState.HUMAN, 20, 40, List.of(), "You call 20");

    @Test
    void startsAsATableAtWhichNothingHasHappened() {
        TableState state = new TableState.Builder().build();

        assertEquals(0, state.getRevision());
        assertEquals(0, state.getHandNumber());
        assertEquals("", state.getHumanName());
        assertEquals("", state.getOpponentName());
        assertEquals(List.of(), state.getHumanCards());
        assertEquals(List.of(), state.getOpponentCards());
        assertEquals(List.of(), state.getBoardCards());
        assertEquals("PREFLOP", state.getStreet());
        assertEquals(0, state.getPot());
        assertEquals(0, state.getHumanChips());
        assertEquals(0, state.getOpponentChips());
        assertEquals(0, state.getHumanBet());
        assertEquals(0, state.getOpponentBet());
        assertFalse(state.isHumanDealer());
        assertFalse(state.isHandInProgress());
        assertFalse(state.isHandOver());
        assertFalse(state.isMatchOver());
        assertEquals("", state.getWinner());
        assertFalse(state.isHumanToAct());
        assertFalse(state.isOpponentToAct());
        assertFalse(state.canFold());
        assertFalse(state.canCheck());
        assertFalse(state.canCall());
        assertFalse(state.canBet());
        assertFalse(state.canRaise());
        assertEquals(0, state.getCallAmount());
        assertEquals(0, state.getMinWager());
        assertEquals(0, state.getMaxWager());
        assertEquals(-1.0, state.getWinChance(), EXACT);
        assertEquals(-1.0, state.getTieChance(), EXACT);
        assertEquals(-1.0, state.getLoseChance(), EXACT);
        assertEquals("", state.getHumanHandName());
        assertEquals("", state.getOpponentHandName());
        assertEquals(List.of(), state.getWinningCards());
        assertEquals("", state.getMessage());
        assertEquals("", state.getErrorMessage());
        assertEquals("", state.getDecidedBy());
        assertEquals(0, state.getHumanChipChange());
        assertEquals(List.of(), state.getEvents());
    }

    @Test
    void holdsEveryPartItWasBuiltFrom() {
        TableState state = fullState();

        assertEquals(7L, state.getRevision());
        assertEquals(3, state.getHandNumber());
        assertEquals("You", state.getHumanName());
        assertEquals("Opponent", state.getOpponentName());
        assertEquals(List.of("AS", "10H"), state.getHumanCards());
        assertEquals(List.of("KC", "KD"), state.getOpponentCards());
        assertEquals(List.of("QD", "7C", "2S"), state.getBoardCards());
        assertEquals("FLOP", state.getStreet());
        assertEquals(100, state.getPot());
        assertEquals(930, state.getHumanChips());
        assertEquals(890, state.getOpponentChips());
        assertEquals(20, state.getHumanBet());
        assertEquals(60, state.getOpponentBet());
        assertTrue(state.isHumanDealer());
        assertTrue(state.isHandInProgress());
        assertTrue(state.isHandOver());
        assertTrue(state.isMatchOver());
        assertEquals("SPLIT", state.getWinner());
        assertTrue(state.isHumanToAct());
        assertTrue(state.isOpponentToAct());
        assertTrue(state.canFold());
        assertTrue(state.canCheck());
        assertTrue(state.canCall());
        assertTrue(state.canBet());
        assertTrue(state.canRaise());
        assertEquals(40, state.getCallAmount());
        assertEquals(100, state.getMinWager());
        assertEquals(950, state.getMaxWager());
        assertEquals(0.5, state.getWinChance(), EXACT);
        assertEquals(0.125, state.getTieChance(), EXACT);
        assertEquals(0.375, state.getLoseChance(), EXACT);
        assertEquals("Pair", state.getHumanHandName());
        assertEquals("Full House", state.getOpponentHandName());
        assertEquals(List.of("KC", "KD", "QD", "7C", "7H"), state.getWinningCards());
        assertEquals("Opponent raises to 60.", state.getMessage());
        assertEquals("It is not your turn.", state.getErrorMessage());
        assertEquals("LLM", state.getDecidedBy());
        assertEquals(-70, state.getHumanChipChange());
        assertEquals(List.of(EVENT), state.getEvents());
    }

    @Test
    void setsEachPermissionOnItsOwn() {
        assertTrue(new TableState.Builder().setCanFold(true).build().canFold());
        assertFalse(new TableState.Builder().setCanFold(true).build().canCheck());
        assertTrue(new TableState.Builder().setCanCheck(true).build().canCheck());
        assertFalse(new TableState.Builder().setCanCheck(true).build().canCall());
        assertTrue(new TableState.Builder().setCanCall(true).build().canCall());
        assertFalse(new TableState.Builder().setCanCall(true).build().canBet());
        assertTrue(new TableState.Builder().setCanBet(true).build().canBet());
        assertFalse(new TableState.Builder().setCanBet(true).build().canRaise());
        assertTrue(new TableState.Builder().setCanRaise(true).build().canRaise());
        assertFalse(new TableState.Builder().setCanRaise(true).build().canFold());
        assertTrue(new TableState.Builder().setHumanToAct(true).build().isHumanToAct());
        assertFalse(new TableState.Builder().setHumanToAct(true).build().isOpponentToAct());
        assertTrue(new TableState.Builder().setHandOver(true).build().isHandOver());
        assertFalse(new TableState.Builder().setHandOver(true).build().isMatchOver());
        assertFalse(new TableState.Builder().setHandOver(true).build().isHandInProgress());
    }

    @Test
    void canBeCopiedAndChangedInAFewParts() {
        TableState original = fullState();

        TableState copy = new TableState.Builder(original).build();
        TableState changed = new TableState.Builder(original)
                .setRevision(8)
                .setErrorMessage("")
                .setEvents(List.of())
                .build();

        assertEquals(original.getRevision(), copy.getRevision());
        assertEquals(original.getErrorMessage(), copy.getErrorMessage());
        assertEquals(original.getEvents(), copy.getEvents());
        assertEquals(List.of("KC", "KD", "QD", "7C", "7H"), copy.getWinningCards());
        TableStateAssertions.assertSameGame(original, copy);

        assertEquals(8L, changed.getRevision());
        assertEquals("", changed.getErrorMessage());
        assertEquals(List.of(), changed.getEvents());
        TableStateAssertions.assertSameGame(original, changed);
        // The original is untouched.
        assertEquals(7L, original.getRevision());
        assertEquals(List.of(EVENT), original.getEvents());
    }

    @Test
    void keepsItsOwnCopiesOfListsWhichCannotBeChanged() {
        List<String> humanCards = new ArrayList<>(List.of("AS", "10H"));
        List<String> opponentCards = new ArrayList<>(List.of("back", "back"));
        List<String> boardCards = new ArrayList<>(List.of("QD", "7C", "2S"));
        List<String> winningCards = new ArrayList<>(List.of("AS", "10H", "QD", "7C", "2S"));
        List<TableEventView> events = new ArrayList<>(List.of(EVENT));
        TableState state = new TableState.Builder()
                .setHumanCards(humanCards)
                .setOpponentCards(opponentCards)
                .setBoardCards(boardCards)
                .setWinningCards(winningCards)
                .setEvents(events)
                .build();

        humanCards.clear();
        opponentCards.clear();
        boardCards.add("3S");
        winningCards.remove(0);
        events.clear();

        assertEquals(List.of("AS", "10H"), state.getHumanCards());
        assertEquals(List.of("back", "back"), state.getOpponentCards());
        assertEquals(List.of("QD", "7C", "2S"), state.getBoardCards());
        assertEquals(List.of("AS", "10H", "QD", "7C", "2S"), state.getWinningCards());
        assertEquals(List.of(EVENT), state.getEvents());
        assertThrows(UnsupportedOperationException.class, () -> state.getHumanCards().add("KS"));
        assertThrows(UnsupportedOperationException.class, () -> state.getOpponentCards().add("KS"));
        assertThrows(UnsupportedOperationException.class, () -> state.getBoardCards().add("KS"));
        assertThrows(UnsupportedOperationException.class, () -> state.getWinningCards().add("KS"));
        assertThrows(UnsupportedOperationException.class, () -> state.getEvents().add(EVENT));
    }

    @Test
    void rejectsMissingTexts() {
        assertThrows(IllegalArgumentException.class, () -> new TableState.Builder().setHumanName(null).build());
        assertThrows(IllegalArgumentException.class, () -> new TableState.Builder().setOpponentName(null).build());
        assertThrows(IllegalArgumentException.class, () -> new TableState.Builder().setStreet(null).build());
        assertThrows(IllegalArgumentException.class, () -> new TableState.Builder().setWinner(null).build());
        assertThrows(IllegalArgumentException.class, () -> new TableState.Builder().setHumanHandName(null).build());
        assertThrows(IllegalArgumentException.class,
                () -> new TableState.Builder().setOpponentHandName(null).build());
        assertThrows(IllegalArgumentException.class, () -> new TableState.Builder().setMessage(null).build());
        assertThrows(IllegalArgumentException.class, () -> new TableState.Builder().setErrorMessage(null).build());
        assertThrows(IllegalArgumentException.class, () -> new TableState.Builder().setDecidedBy(null).build());
    }

    @Test
    void rejectsMissingLists() {
        assertThrows(IllegalArgumentException.class, () -> new TableState.Builder().setHumanCards(null).build());
        assertThrows(IllegalArgumentException.class, () -> new TableState.Builder().setOpponentCards(null).build());
        assertThrows(IllegalArgumentException.class, () -> new TableState.Builder().setBoardCards(null).build());
        assertThrows(IllegalArgumentException.class, () -> new TableState.Builder().setWinningCards(null).build());
        assertThrows(IllegalArgumentException.class, () -> new TableState.Builder().setEvents(null).build());
    }

    @Test
    void hasExactlyTheGettersTheGameScreenIsWrittenAgainst() {
        List<String> promised = List.of(
                "long getRevision", "int getHandNumber", "String getHumanName", "String getOpponentName",
                "List getHumanCards", "List getOpponentCards", "List getBoardCards", "String getStreet",
                "int getPot", "int getHumanChips", "int getOpponentChips", "int getHumanBet", "int getOpponentBet",
                "boolean isHumanDealer", "boolean isHandInProgress", "boolean isHandOver", "boolean isMatchOver",
                "String getWinner", "boolean isHumanToAct", "boolean isOpponentToAct",
                "boolean canFold", "boolean canCheck", "boolean canCall", "boolean canBet", "boolean canRaise",
                "int getCallAmount", "int getMinWager", "int getMaxWager",
                "double getWinChance", "double getTieChance", "double getLoseChance",
                "String getHumanHandName", "String getOpponentHandName", "List getWinningCards",
                "String getMessage", "String getErrorMessage", "String getDecidedBy", "int getHumanChipChange",
                "List getEvents");

        List<String> declared = new ArrayList<>();
        for (Method method : TableState.class.getDeclaredMethods()) {
            if (Modifier.isPublic(method.getModifiers())) {
                assertEquals(0, method.getParameterCount(), method.getName());
                declared.add(method.getReturnType().getSimpleName() + " " + method.getName());
            }
        }

        List<String> expected = new ArrayList<>(promised);
        Collections.sort(expected);
        Collections.sort(declared);
        assertEquals(expected, declared);
    }

    @Test
    void usesTheSameWordsAsTheUseCases() {
        assertEquals(TableOutputData.HUMAN, TableState.HUMAN);
        assertEquals(TableOutputData.OPPONENT, TableState.OPPONENT);
        assertEquals(TableOutputData.SPLIT, TableState.SPLIT);
        assertEquals(TableOutputData.NONE, TableState.NONE);
        assertEquals(TableOutputData.DECIDED_BY_LLM, TableState.DECIDED_BY_LLM);
        assertEquals(TableOutputData.DECIDED_BY_BUILT_IN, TableState.DECIDED_BY_BUILT_IN);
        assertEquals(TableOutputData.CHANCE_UNAVAILABLE, TableState.CHANCE_UNAVAILABLE, EXACT);
        assertEquals("PREFLOP", TableState.FIRST_STREET);
    }

    /**
     * Returns a state in which every part has a value of its own, so a getter
     * that returned another part's value would be found out. The parts need
     * not make sense together.
     */
    private static TableState fullState() {
        return new TableState.Builder()
                .setRevision(7)
                .setHandNumber(3)
                .setHumanName("You")
                .setOpponentName("Opponent")
                .setHumanCards(List.of("AS", "10H"))
                .setOpponentCards(List.of("KC", "KD"))
                .setBoardCards(List.of("QD", "7C", "2S"))
                .setStreet("FLOP")
                .setPot(100)
                .setHumanChips(930)
                .setOpponentChips(890)
                .setHumanBet(20)
                .setOpponentBet(60)
                .setHumanDealer(true)
                .setHandInProgress(true)
                .setHandOver(true)
                .setMatchOver(true)
                .setWinner(TableState.SPLIT)
                .setHumanToAct(true)
                .setOpponentToAct(true)
                .setCanFold(true)
                .setCanCheck(true)
                .setCanCall(true)
                .setCanBet(true)
                .setCanRaise(true)
                .setCallAmount(40)
                .setMinWager(100)
                .setMaxWager(950)
                .setWinChance(0.5)
                .setTieChance(0.125)
                .setLoseChance(0.375)
                .setHumanHandName("Pair")
                .setOpponentHandName("Full House")
                .setWinningCards(List.of("KC", "KD", "QD", "7C", "7H"))
                .setMessage("Opponent raises to 60.")
                .setErrorMessage("It is not your turn.")
                .setDecidedBy(TableState.DECIDED_BY_LLM)
                .setHumanChipChange(-70)
                .setEvents(List.of(EVENT))
                .build();
    }
}
