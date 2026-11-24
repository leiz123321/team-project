package use_case.table;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;

class TableOutputDataTest {
    private static final double EXACT = 0.0;
    private static final TableEventData EVENT =
            new TableEventData("CALL", TableOutputData.HUMAN, 20, 40, List.of(), "");

    @Test
    void startsEmpty() {
        TableOutputData table = new TableOutputData.Builder().build();

        assertEquals(0, table.getHandNumber());
        assertEquals("", table.getHumanName());
        assertEquals("", table.getOpponentName());
        assertEquals("", table.getStreet());
        assertEquals(0, table.getPot());
        assertEquals(0, table.getHumanChips());
        assertEquals(0, table.getOpponentChips());
        assertEquals(0, table.getHumanBet());
        assertEquals(0, table.getOpponentBet());
        assertEquals(List.of(), table.getHumanCards());
        assertEquals(List.of(), table.getOpponentCards());
        assertEquals(List.of(), table.getBoardCards());
        assertFalse(table.isHumanSmallBlind());
        assertFalse(table.isHandOver());
        assertFalse(table.isMatchOver());
        assertEquals("", table.getWinner());
        assertEquals("", table.getToAct());
        assertEquals(List.of(), table.getLegalActions());
        assertEquals(0, table.getCallAmount());
        assertEquals(0, table.getMinWager());
        assertEquals(0, table.getMaxWager());
        assertEquals(-1.0, table.getWinChance(), EXACT);
        assertEquals(-1.0, table.getTieChance(), EXACT);
        assertEquals(-1.0, table.getLoseChance(), EXACT);
        assertEquals("", table.getHumanHandName());
        assertEquals("", table.getOpponentHandName());
        assertEquals(List.of(), table.getWinningCards());
        assertEquals(List.of(), table.getEvents());
        assertEquals(0, table.getHumanChipChange());
        assertEquals("", table.getDecidedBy());
    }

    @Test
    void holdsEveryPartItWasBuiltFrom() {
        TableOutputData table = new TableOutputData.Builder()
                .setHandNumber(3)
                .setHumanName("You")
                .setOpponentName("Opponent")
                .setStreet("FLOP")
                .setPot(180)
                .setHumanChips(930)
                .setOpponentChips(890)
                .setHumanBet(20)
                .setOpponentBet(60)
                .setHumanCards(List.of("A♠", "10♥"))
                .setOpponentCards(List.of("K♣", "K♦"))
                .setBoardCards(List.of("Q♦", "7♣", "2♠"))
                .setHumanSmallBlind(true)
                .setHandOver(true)
                .setMatchOver(true)
                .setWinner(TableOutputData.SPLIT)
                .setToAct(TableOutputData.HUMAN)
                .setLegalActions(List.of("FOLD", "CALL", "RAISE"))
                .setCallAmount(40)
                .setMinWager(100)
                .setMaxWager(950)
                .setWinChance(0.5)
                .setTieChance(0.125)
                .setLoseChance(0.375)
                .setHumanHandName("Pair")
                .setOpponentHandName("Full House")
                .setWinningCards(List.of("K♣", "K♦", "Q♦", "7♣", "7♥"))
                .setEvents(List.of(EVENT))
                .setHumanChipChange(-70)
                .setDecidedBy(TableOutputData.DECIDED_BY_LLM)
                .build();

        assertEquals(3, table.getHandNumber());
        assertEquals("You", table.getHumanName());
        assertEquals("Opponent", table.getOpponentName());
        assertEquals("FLOP", table.getStreet());
        assertEquals(180, table.getPot());
        assertEquals(930, table.getHumanChips());
        assertEquals(890, table.getOpponentChips());
        assertEquals(20, table.getHumanBet());
        assertEquals(60, table.getOpponentBet());
        assertEquals(List.of("A♠", "10♥"), table.getHumanCards());
        assertEquals(List.of("K♣", "K♦"), table.getOpponentCards());
        assertEquals(List.of("Q♦", "7♣", "2♠"), table.getBoardCards());
        assertTrue(table.isHumanSmallBlind());
        assertTrue(table.isHandOver());
        assertTrue(table.isMatchOver());
        assertEquals("SPLIT", table.getWinner());
        assertEquals("HUMAN", table.getToAct());
        assertEquals(List.of("FOLD", "CALL", "RAISE"), table.getLegalActions());
        assertEquals(40, table.getCallAmount());
        assertEquals(100, table.getMinWager());
        assertEquals(950, table.getMaxWager());
        assertEquals(0.5, table.getWinChance(), EXACT);
        assertEquals(0.125, table.getTieChance(), EXACT);
        assertEquals(0.375, table.getLoseChance(), EXACT);
        assertEquals("Pair", table.getHumanHandName());
        assertEquals("Full House", table.getOpponentHandName());
        assertEquals(List.of("K♣", "K♦", "Q♦", "7♣", "7♥"), table.getWinningCards());
        assertEquals(List.of(EVENT), table.getEvents());
        assertEquals(-70, table.getHumanChipChange());
        assertEquals("LLM", table.getDecidedBy());
    }

    @Test
    void setsEachFlagOnItsOwn() {
        assertTrue(new TableOutputData.Builder().setHumanSmallBlind(true).build().isHumanSmallBlind());
        assertFalse(new TableOutputData.Builder().setHumanSmallBlind(true).build().isHandOver());
        assertTrue(new TableOutputData.Builder().setHandOver(true).build().isHandOver());
        assertFalse(new TableOutputData.Builder().setHandOver(true).build().isMatchOver());
        assertTrue(new TableOutputData.Builder().setMatchOver(true).build().isMatchOver());
        assertFalse(new TableOutputData.Builder().setMatchOver(true).build().isHumanSmallBlind());
    }

    @Test
    void keepsItsOwnCopiesOfListsWhichCannotBeChanged() {
        List<String> humanCards = new ArrayList<>(List.of("A♠", "10♥"));
        List<String> legalActions = new ArrayList<>(List.of("FOLD"));
        List<String> winningCards = new ArrayList<>(List.of("A♠", "10♥", "Q♦", "7♣", "2♠"));
        List<TableEventData> events = new ArrayList<>(List.of(EVENT));
        TableOutputData table = new TableOutputData.Builder()
                .setHumanCards(humanCards)
                .setLegalActions(legalActions)
                .setWinningCards(winningCards)
                .setEvents(events)
                .build();

        humanCards.clear();
        legalActions.add("CHECK");
        winningCards.remove(0);
        events.clear();

        assertEquals(List.of("A♠", "10♥"), table.getHumanCards());
        assertEquals(List.of("FOLD"), table.getLegalActions());
        assertEquals(List.of("A♠", "10♥", "Q♦", "7♣", "2♠"), table.getWinningCards());
        assertEquals(List.of(EVENT), table.getEvents());
        assertThrows(UnsupportedOperationException.class, () -> table.getHumanCards().add("K♠"));
        assertThrows(UnsupportedOperationException.class, () -> table.getOpponentCards().add("K♠"));
        assertThrows(UnsupportedOperationException.class, () -> table.getBoardCards().add("K♠"));
        assertThrows(UnsupportedOperationException.class, () -> table.getLegalActions().add("BET"));
        assertThrows(UnsupportedOperationException.class, () -> table.getWinningCards().add("K♠"));
        assertThrows(UnsupportedOperationException.class, () -> table.getEvents().add(EVENT));
    }

    @Test
    void rejectsMissingTexts() {
        assertThrows(IllegalArgumentException.class, () -> new TableOutputData.Builder().setHumanName(null).build());
        assertThrows(IllegalArgumentException.class,
                () -> new TableOutputData.Builder().setOpponentName(null).build());
        assertThrows(IllegalArgumentException.class, () -> new TableOutputData.Builder().setStreet(null).build());
        assertThrows(IllegalArgumentException.class, () -> new TableOutputData.Builder().setWinner(null).build());
        assertThrows(IllegalArgumentException.class, () -> new TableOutputData.Builder().setToAct(null).build());
        assertThrows(IllegalArgumentException.class,
                () -> new TableOutputData.Builder().setHumanHandName(null).build());
        assertThrows(IllegalArgumentException.class,
                () -> new TableOutputData.Builder().setOpponentHandName(null).build());
        assertThrows(IllegalArgumentException.class, () -> new TableOutputData.Builder().setDecidedBy(null).build());
    }

    @Test
    void rejectsMissingLists() {
        assertThrows(IllegalArgumentException.class,
                () -> new TableOutputData.Builder().setHumanCards(null).build());
        assertThrows(IllegalArgumentException.class,
                () -> new TableOutputData.Builder().setOpponentCards(null).build());
        assertThrows(IllegalArgumentException.class,
                () -> new TableOutputData.Builder().setBoardCards(null).build());
        assertThrows(IllegalArgumentException.class,
                () -> new TableOutputData.Builder().setLegalActions(null).build());
        assertThrows(IllegalArgumentException.class,
                () -> new TableOutputData.Builder().setWinningCards(null).build());
        assertThrows(IllegalArgumentException.class, () -> new TableOutputData.Builder().setEvents(null).build());
    }

    @Test
    void namesTheSeatsAndTheSourcesOfADecision() {
        assertEquals("HUMAN", TableOutputData.HUMAN);
        assertEquals("OPPONENT", TableOutputData.OPPONENT);
        assertEquals("SPLIT", TableOutputData.SPLIT);
        assertEquals("", TableOutputData.NONE);
        assertEquals("LLM", TableOutputData.DECIDED_BY_LLM);
        assertEquals("BUILT_IN", TableOutputData.DECIDED_BY_BUILT_IN);
        assertEquals(-1.0, TableOutputData.CHANCE_UNAVAILABLE, EXACT);
    }
}
