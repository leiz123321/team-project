package use_case.table;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.junit.jupiter.api.Test;

class TableEventDataTest {

    @Test
    void holdsWhatItWasGiven() {
        TableEventData event = new TableEventData("SHOWDOWN", TableOutputData.OPPONENT, 3, 4,
                List.of("K♣", "K♦"), "Full House");

        assertEquals("SHOWDOWN", event.getType());
        assertEquals("OPPONENT", event.getActor());
        assertEquals(3, event.getAmount());
        assertEquals(4, event.getTotal());
        assertEquals(List.of("K♣", "K♦"), event.getCards());
        assertEquals("Full House", event.getDetail());
    }

    @Test
    void keepsItsOwnCopyOfTheCardsWhichCannotBeChanged() {
        List<String> cards = new ArrayList<>(List.of("A♠"));
        TableEventData event = new TableEventData("TURN", TableOutputData.NONE, 0, 0, cards, "");

        cards.add("K♠");

        assertEquals(List.of("A♠"), event.getCards());
        assertThrows(UnsupportedOperationException.class, () -> event.getCards().add("Q♠"));
    }

    @Test
    void rejectsMissingParts() {
        List<String> none = List.of();

        assertThrows(IllegalArgumentException.class,
                () -> new TableEventData(null, TableOutputData.HUMAN, 0, 0, none, ""));
        assertThrows(IllegalArgumentException.class, () -> new TableEventData("FOLD", null, 0, 0, none, ""));
        assertThrows(IllegalArgumentException.class,
                () -> new TableEventData("FOLD", TableOutputData.HUMAN, 0, 0, null, ""));
        assertThrows(IllegalArgumentException.class,
                () -> new TableEventData("FOLD", TableOutputData.HUMAN, 0, 0, none, null));
        assertThrows(NullPointerException.class,
                () -> new TableEventData("FLOP", TableOutputData.NONE, 0, 0, Arrays.asList("A♠", null), ""));
    }
}
