package entity;

import java.util.ArrayList;
import java.util.List;

/**
 * Makes cards for the tests from the text the entities print, such as "A♠" or
 * "10♥". This is a helper for the tests, not a test.
 */
public final class Cards {

    private Cards() {
    }

    /**
     * Makes a new card, which starts in the deck.
     *
     * @param text the rank symbol followed by the suit symbol, for example "10♥"
     * @return the card
     * @throws IllegalArgumentException if the text is not a card
     */
    public static Card card(String text) {
        String rankSymbol = text.substring(0, text.length() - 1);
        String suitSymbol = text.substring(text.length() - 1);
        for (Rank rank : Rank.values()) {
            for (Suit suit : Suit.values()) {
                if (rank.getSymbol().equals(rankSymbol) && suit.getSymbol().equals(suitSymbol)) {
                    return new Card(rank, suit);
                }
            }
        }
        throw new IllegalArgumentException("not a card: " + text);
    }

    /**
     * Makes new cards, all still in the deck.
     *
     * @param texts card texts separated by spaces, for example "A♠ K♠ 10♥"; may be empty
     * @return a new list of the cards, in the order given
     */
    public static List<Card> cards(String texts) {
        List<Card> result = new ArrayList<>();
        for (String text : texts.trim().split("\\s+")) {
            if (!text.isEmpty()) {
                result.add(card(text));
            }
        }
        return result;
    }

    /**
     * Makes new cards that have been dealt to a player's hand.
     *
     * @param texts card texts separated by spaces
     * @return a new list of the cards, in the order given
     */
    public static List<Card> inHand(String texts) {
        List<Card> result = cards(texts);
        for (Card card : result) {
            card.dealToHand();
        }
        return result;
    }

    /**
     * Makes new cards that have been dealt as face-down community cards.
     *
     * @param texts card texts separated by spaces
     * @return a new list of the cards, in the order given
     */
    public static List<Card> faceDown(String texts) {
        List<Card> result = cards(texts);
        for (Card card : result) {
            card.dealToBoard();
        }
        return result;
    }

    /**
     * Makes one new card of every rank and suit, all still in the deck.
     *
     * @return a new list of 52 different cards
     */
    public static List<Card> fullDeck() {
        List<Card> result = new ArrayList<>();
        for (Suit suit : Suit.values()) {
            for (Rank rank : Rank.values()) {
                result.add(new Card(rank, suit));
            }
        }
        return result;
    }

    /**
     * Returns the text of each card.
     *
     * @param cards the cards
     * @return a new list of the texts, in the same order
     */
    public static List<String> texts(List<Card> cards) {
        List<String> result = new ArrayList<>();
        for (Card card : cards) {
            result.add(card.toString());
        }
        return result;
    }
}
