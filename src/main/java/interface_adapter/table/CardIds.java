package interface_adapter.table;

import java.util.ArrayList;
import java.util.List;

/**
 * Converts the text of a card, as it crosses the output boundary, into the id
 * of that card's picture. The text is the rank symbol followed by the suit
 * symbol, such as "A♠" or "10♥"; the id is the rank symbol followed by the
 * suit letter, such as "AS" or "10H". This is the one place that knows both
 * forms.
 */
public final class CardIds {

    /** The id of a card that lies face down. */
    public static final String BACK = "back";

    private static final List<String> RANK_SYMBOLS =
            List.of("2", "3", "4", "5", "6", "7", "8", "9", "10", "J", "Q", "K", "A");
    private static final String SUIT_SYMBOLS = "♠♥♦♣";
    private static final String SUIT_LETTERS = "SHDC";

    private CardIds() {
    }

    /**
     * Returns the picture id of a card.
     *
     * @param cardText the card as text: a rank symbol ("2" to "10", "J", "Q",
     *        "K", "A") followed by a suit symbol ("♠", "♥", "♦", "♣")
     * @return the rank symbol followed by "S", "H", "D", or "C"
     * @throws IllegalArgumentException if the text is not a card
     */
    public static String toId(String cardText) {
        if (cardText == null || cardText.isEmpty()) {
            throw new IllegalArgumentException("not a card: " + cardText);
        }
        int last = cardText.length() - 1;
        String rank = cardText.substring(0, last);
        int suit = SUIT_SYMBOLS.indexOf(cardText.charAt(last));
        if (suit < 0 || !RANK_SYMBOLS.contains(rank)) {
            throw new IllegalArgumentException("not a card: " + cardText);
        }
        return rank + SUIT_LETTERS.charAt(suit);
    }

    /**
     * Returns the picture ids of some cards, in the same order.
     *
     * @param cardTexts the cards as text
     * @return a new list of the ids
     * @throws IllegalArgumentException if a text is not a card
     */
    public static List<String> toIds(List<String> cardTexts) {
        List<String> ids = new ArrayList<>();
        for (String cardText : cardTexts) {
            ids.add(toId(cardText));
        }
        return ids;
    }

    /**
     * Returns the ids of cards that lie face down.
     *
     * @param count how many cards
     * @return a new list holding {@link #BACK} that many times
     */
    public static List<String> faceDown(int count) {
        List<String> ids = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            ids.add(BACK);
        }
        return ids;
    }
}
