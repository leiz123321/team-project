package interface_adapter.table;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.junit.jupiter.api.Test;

import entity.HandEventType;

class TableEventViewTest {

    @Test
    void holdsWhatItWasGiven() {
        TableEventView event = new TableEventView(TableEventView.SHOWDOWN, TableState.OPPONENT, 3, 4,
                List.of("KC", "KD"), "Opponent shows K♣ K♦ — Full House");

        assertEquals("SHOWDOWN", event.getType());
        assertEquals("OPPONENT", event.getActor());
        assertEquals(3, event.getAmount());
        assertEquals(4, event.getTotal());
        assertEquals(List.of("KC", "KD"), event.getCards());
        assertEquals("Opponent shows K♣ K♦ — Full House", event.getText());
    }

    @Test
    void keepsItsOwnCopyOfTheCardsWhichCannotBeChanged() {
        List<String> cards = new ArrayList<>(List.of("AS"));
        TableEventView event = new TableEventView(TableEventView.TURN, TableState.NONE, 0, 0, cards, "Turn: A♠");

        cards.add("KS");

        assertEquals(List.of("AS"), event.getCards());
        assertThrows(UnsupportedOperationException.class, () -> event.getCards().add("QS"));
    }

    @Test
    void rejectsMissingParts() {
        List<String> none = List.of();

        assertThrows(IllegalArgumentException.class,
                () -> new TableEventView(null, TableState.HUMAN, 0, 0, none, "You fold"));
        assertThrows(IllegalArgumentException.class,
                () -> new TableEventView(TableEventView.FOLD, null, 0, 0, none, "You fold"));
        assertThrows(IllegalArgumentException.class,
                () -> new TableEventView(TableEventView.FOLD, TableState.HUMAN, 0, 0, null, "You fold"));
        assertThrows(IllegalArgumentException.class,
                () -> new TableEventView(TableEventView.FOLD, TableState.HUMAN, 0, 0, none, null));
    }

    @Test
    void hasAConstantForEveryKindOfEventTheGameRecords() throws IllegalAccessException {
        List<String> constants = new ArrayList<>();
        for (Field field : TableEventView.class.getFields()) {
            if (Modifier.isStatic(field.getModifiers())) {
                assertEquals(field.getName(), field.get(null));
                constants.add(field.getName());
            }
        }

        List<String> recorded = new ArrayList<>();
        for (HandEventType type : HandEventType.values()) {
            recorded.add(type.name());
        }
        Collections.sort(recorded);
        Collections.sort(constants);
        assertEquals(recorded, constants);
    }
}
