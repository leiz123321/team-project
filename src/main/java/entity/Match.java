package entity;

import java.util.Random;

/**
 * A heads-up match: the same two players, one human and one computer, playing
 * hand after hand with fixed blinds until one of them can no longer post the
 * big blind.
 *
 * <p>The small blind, which is also the dealer button, alternates every hand;
 * the human posts it in the first hand. The match keeps one deck for all of
 * its hands and remembers the hand being played.
 */
public class Match {
    private final int startingChips;
    private final int smallBlind;
    private final int bigBlind;
    private final Deck deck = new Deck();
    private final Player human;
    private final Player computer;

    private int handNumber;
    private Game currentGame;

    /**
     * Creates a match in which no hand has been played yet.
     *
     * @param humanName the human player's name
     * @param computerName the computer player's name
     * @param startingChips the chips each player starts the match with
     * @param smallBlind the size of the small blind
     * @param bigBlind the size of the big blind
     * @throws IllegalArgumentException if a name is null or blank, the names
     *         are the same, the blinds are not positive with the small blind
     *         no larger than the big blind, or the starting chips are fewer
     *         than the big blind
     */
    public Match(String humanName, String computerName, int startingChips, int smallBlind, int bigBlind) {
        if (smallBlind <= 0 || smallBlind > bigBlind) {
            throw new IllegalArgumentException("blinds must be positive, small blind at most the big blind");
        }
        if (startingChips < bigBlind) {
            throw new IllegalArgumentException("each player needs at least the big blind");
        }
        this.human = new Player(humanName, startingChips);
        this.computer = new Player(computerName, startingChips);
        if (humanName.equals(computerName)) {
            throw new IllegalArgumentException("the two players need different names");
        }
        this.startingChips = startingChips;
        this.smallBlind = smallBlind;
        this.bigBlind = bigBlind;
    }

    public Player getHuman() {
        return this.human;
    }

    public Player getComputer() {
        return this.computer;
    }

    /**
     * Returns the number of the current hand, counting from 1, or 0 if no hand
     * has been started yet.
     */
    public int getHandNumber() {
        return this.handNumber;
    }

    public int getSmallBlind() {
        return this.smallBlind;
    }

    public int getBigBlind() {
        return this.bigBlind;
    }

    /**
     * Returns the hand being played or the last one played, or null if no hand
     * has been started yet.
     */
    public Game getCurrentGame() {
        return this.currentGame;
    }

    /**
     * Returns whether the human posts the small blind, and so holds the dealer
     * button, in the current hand. It is false before the first hand.
     */
    public boolean isHumanSmallBlind() {
        return this.handNumber % 2 == 1;
    }

    /**
     * Returns whether the match is over: a player has fewer chips than the big
     * blind and so cannot start another hand. While a hand is being played the
     * match is never over, even if a player has put all their chips in, because
     * that player may still win them back.
     */
    public boolean isOver() {
        return !isHandInProgress()
                && (this.human.getChips() < this.bigBlind || this.computer.getChips() < this.bigBlind);
    }

    /**
     * Returns the player who has more chips, or null if both have the same.
     */
    public Player getLeader() {
        Player leader = null;
        if (this.human.getChips() > this.computer.getChips()) {
            leader = this.human;
        }
        else if (this.computer.getChips() > this.human.getChips()) {
            leader = this.computer;
        }
        return leader;
    }

    /**
     * Starts the next hand. The small blind goes to the player who did not
     * post it in the hand before, and to the human in the first hand. The new
     * hand takes every card back into the deck and shuffles it.
     *
     * @param random the source of randomness for the shuffle; pass a seeded
     *        one in tests
     * @return the new hand, which is also the current game from now on
     * @throws IllegalStateException if the current hand is not finished or the
     *         match is over
     * @throws IllegalArgumentException if random is null
     */
    public Game startNextHand(Random random) {
        if (isHandInProgress()) {
            throw new IllegalStateException("the current hand is not finished");
        }
        if (isOver()) {
            throw new IllegalStateException("the match is over");
        }
        Player smallBlindPlayer = this.computer;
        Player bigBlindPlayer = this.human;
        if (!isHumanSmallBlind()) {
            // The blinds swap: whoever is not the small blind now is the small blind next.
            smallBlindPlayer = this.human;
            bigBlindPlayer = this.computer;
        }
        Game game = new Game(smallBlindPlayer, bigBlindPlayer, this.smallBlind, this.bigBlind,
                this.deck, random);
        this.handNumber++;
        this.currentGame = game;
        return game;
    }

    /**
     * Starts the match again from the beginning: both players are back at the
     * starting chips with no cards, the hand number is 0, and there is no
     * current game. A hand that was being played is abandoned.
     */
    public void restart() {
        resetStack(this.human);
        resetStack(this.computer);
        this.deck.reset();
        this.handNumber = 0;
        this.currentGame = null;
    }

    /**
     * Puts a player back at the starting chips with no cards. A stack cannot be
     * set directly: chips are added with {@code win} and taken away with
     * {@code bet}.
     */
    private void resetStack(Player player) {
        player.clearHand();
        int missing = this.startingChips - player.getChips();
        if (missing > 0) {
            player.win(missing);
        }
        else if (missing < 0) {
            player.bet(-missing);
        }
    }

    private boolean isHandInProgress() {
        return this.currentGame != null && !this.currentGame.isHandOver();
    }
}
