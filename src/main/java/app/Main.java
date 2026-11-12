package app;

//these are just here for testing
import entity.Card;
import entity.Deck;

public class Main {
    public static void main(String[] args) {
        // testing things lol
        Deck deck = new Deck();
        for (Card c : deck.getCards()) {
            System.out.println(c);
        }
    }
}
