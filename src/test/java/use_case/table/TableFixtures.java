package use_case.table;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.function.LongPredicate;

import entity.Action;
import entity.Card;
import entity.Game;
import entity.Match;
import entity.Player;
import entity.Street;

/**
 * Shared set-up for the tests of the game's use cases: matches with the real
 * game parameters, hands dealt from a seed, and ways to play a hand forward
 * without caring which cards came.
 */
public final class TableFixtures {

    /** The human player's name. */
    public static final String HUMAN_NAME = "You";

    /** The computer opponent's name. */
    public static final String OPPONENT_NAME = "Opponent";

    /** The chips each player starts a match with. */
    public static final int STARTING_CHIPS = 1000;

    /** The size of the small blind. */
    public static final int SMALL_BLIND = 5;

    /** The size of the big blind. */
    public static final int BIG_BLIND = 10;

    private static final int SEEDS_TO_TRY = 20000;

    private TableFixtures() {
    }

    /**
     * Returns a match with the real game parameters in which no hand has been
     * dealt yet.
     */
    public static Match newMatch() {
        return new Match(HUMAN_NAME, OPPONENT_NAME, STARTING_CHIPS, SMALL_BLIND, BIG_BLIND);
    }

    /**
     * Returns a match whose first hand has just been dealt from the given
     * seed. The human is the small blind and is first to act.
     */
    public static Match dealtMatch(long seed) {
        Match match = newMatch();
        match.startNextHand(new Random(seed));
        return match;
    }

    /**
     * Returns a match in its second hand, dealt from the given seed, after the
     * human folded the first. The opponent is the small blind and is first to
     * act; the human has 995 chips and the opponent 1005 before the blinds.
     */
    public static Match secondHandMatch(long seed) {
        Match match = dealtMatch(seed);
        match.getCurrentGame().apply(Action.fold());
        match.startNextHand(new Random(seed));
        return match;
    }

    /**
     * Returns a match whose small blind has exactly the big blind in the hand
     * that has just been dealt from the given seed, and is first to act: with
     * 5 chips posted and 5 left, that player can only fold or call with their
     * last chips. The other player has the remaining 1990 chips of the match.
     *
     * @param seed the seed of the deal
     * @param humanIsSmallBlind true for the first hand of a match, in which
     *        the human is the small blind; false for a second hand, after the
     *        human folded the first, in which the opponent is
     */
    public static Match shortSmallBlindMatch(long seed, boolean humanIsSmallBlind) {
        Match match = newMatch();
        Player shortStack = match.getHuman();
        Player bigStack = match.getComputer();
        if (!humanIsSmallBlind) {
            match.startNextHand(new Random(seed));
            match.getCurrentGame().apply(Action.fold());
            shortStack = match.getComputer();
            bigStack = match.getHuman();
        }
        // A stack cannot be set: chips leave it as a bet and reach the other stack as winnings.
        int moved = shortStack.getChips() - BIG_BLIND;
        shortStack.bet(moved);
        bigStack.win(moved);
        match.startNextHand(new Random(seed));
        return match;
    }

    /**
     * Returns a match whose first hand has just been dealt from the given
     * seed to a human who came to it with the given chips; the opponent has
     * the rest of the 2000 chips of the match. The human is the small blind
     * and is first to act.
     *
     * @param seed the seed of the deal
     * @param humanChips the human's chips before the blinds are posted
     */
    public static Match shortHumanMatch(long seed, int humanChips) {
        Match match = newMatch();
        // A stack cannot be set: chips leave it as a bet and reach the other stack as winnings.
        int moved = match.getHuman().getChips() - humanChips;
        match.getHuman().bet(moved);
        match.getComputer().win(moved);
        match.startNextHand(new Random(seed));
        return match;
    }

    /**
     * Lets the player to act check if that is legal, and call otherwise.
     */
    public static void actPassively(Game game) {
        if (game.isLegal(Action.check())) {
            game.apply(Action.check());
        }
        else {
            game.apply(Action.call());
        }
    }

    /**
     * Has both players check and call until the hand reaches the given street.
     */
    public static void checkTo(Game game, Street street) {
        while (game.getStreet() != street) {
            actPassively(game);
        }
    }

    /**
     * Has both players check and call until the hand is over, which ends it
     * in a showdown.
     */
    public static void checkDown(Game game) {
        while (!game.isHandOver()) {
            actPassively(game);
        }
    }

    /**
     * Returns the first seed, counting up from 0, for which the condition
     * holds. Tests use it to find a deal with a wanted property, such as a
     * split pot, without depending on how the deck happens to be shuffled.
     *
     * @throws IllegalStateException if no seed is found
     */
    public static long seedWhere(LongPredicate wanted) {
        for (long seed = 0; seed < SEEDS_TO_TRY; seed++) {
            if (wanted.test(seed)) {
                return seed;
            }
        }
        throw new IllegalStateException("no seed gives the wanted deal");
    }

    /**
     * Returns the cards as text, in the same order.
     */
    public static List<String> texts(List<Card> cards) {
        List<String> texts = new ArrayList<>();
        for (Card card : cards) {
            texts.add(card.toString());
        }
        return texts;
    }

    /**
     * Returns the player's hole cards as text.
     */
    public static List<String> holeCards(Player player) {
        return texts(player.getHoleCards().getCards());
    }
}
