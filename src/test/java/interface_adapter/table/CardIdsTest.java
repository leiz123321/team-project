package interface_adapter.table;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.Test;

import entity.Card;
import entity.CardState;
import entity.Deck;
import entity.Suit;

class CardIdsTest {

    @Test
    void convertsTheTextOfACardToItsPictureId() {
        assertEquals("AS", CardIds.toId("A♠"));
        assertEquals("10H", CardIds.toId("10♥"));
        assertEquals("QD", CardIds.toId("Q♦"));
        assertEquals("7C", CardIds.toId("7♣"));
        assertEquals("2S", CardIds.toId("2♠"));
        assertEquals("KH", CardIds.toId("K♥"));
        assertEquals("JD", CardIds.toId("J♦"));
    }

    @Test
    void givesEveryCardOfTheDeckItsOwnId() {
        Set<String> ids = new HashSet<>();
        for (Card card : new Deck().cardsIn(CardState.IN_DECK)) {
            String id = CardIds.toId(card.toString());

            assertEquals(card.getRank().getSymbol() + letterOf(card.getSuit()), id);
            assertTrue(id.matches("(10|[2-9JQKA])[SHDC]"), id);
            ids.add(id);
        }
        assertEquals(52, ids.size());
    }

    @Test
    void rejectsTextThatIsNotACard() {
        String[] notCards = {null, "", "♠", "A", "AS", "1♠", "11♥", "a♠", "A♠ ", " A♠", "A♠♠", "back", "A♤"};
        for (String text : notCards) {
            assertThrows(IllegalArgumentException.class, () -> CardIds.toId(text), "accepted: " + text);
        }
    }

    @Test
    void convertsAListOfCardsInOrder() {
        List<String> texts = new ArrayList<>(List.of("A♠", "10♥", "Q♦"));

        List<String> ids = CardIds.toIds(texts);

        assertEquals(List.of("AS", "10H", "QD"), ids);
        assertNotSame(texts, ids);
        assertEquals(List.of(), CardIds.toIds(List.of()));
        assertThrows(IllegalArgumentException.class, () -> CardIds.toIds(List.of("A♠", "joker")));
    }

    @Test
    void namesFaceDownCards() {
        assertEquals("back", CardIds.BACK);
        assertEquals(List.of("back", "back"), CardIds.faceDown(2));
        assertEquals(List.of(), CardIds.faceDown(0));
    }

    private static String letterOf(Suit suit) {
        switch (suit) {
            case SPADE:
                return "S";
            case HEART:
                return "H";
            case DIAMOND:
                return "D";
            default:
                return "C";
        }
    }
}
