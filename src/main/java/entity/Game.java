package entity;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * The state and rules of one hand of heads-up No-Limit Texas Hold'em.
 *
 * <p>The first player posts the small blind and acts first before the flop;
 * the second player posts the big blind and acts first on every later betting
 * round. Creating a game shuffles the deck, posts the blinds, and deals the
 * cards. The hand then advances as actions are applied, until one player
 * folds or the hands are compared at the showdown.
 */
public class Game {
    private static final int SMALL_BLIND = 0;
    private static final int BIG_BLIND = 1;
    private static final int PLAYER_COUNT = 2;
    private static final int BOARD_SIZE = 5;
    private static final int FLOP_SIZE = 3;

    private final Player[] players;
    private final int bigBlind;
    private final Board board;
    private final int[] startingChips = new int[PLAYER_COUNT];
    private final int[] streetBets = new int[PLAYER_COUNT];
    private final boolean[] acted = new boolean[PLAYER_COUNT];
    private final List<String> holeCardTexts = new ArrayList<>();
    private final List<String> actionLog = new ArrayList<>();
    private final HandEvaluator evaluator = new HandEvaluator();

    private Street street;
    private int pot;
    private int currentBet;
    private int lastRaiseSize;
    private int toAct;
    private boolean handOver;
    private Player winner;

    /**
     * Starts a new hand: shuffles the deck, posts the blinds, and deals two
     * hole cards to each player and five face-down community cards.
     *
     * @param smallBlindPlayer the player who posts the small blind
     * @param bigBlindPlayer the player who posts the big blind
     * @param smallBlind the size of the small blind
     * @param bigBlind the size of the big blind
     * @param deck the deck to deal from
     * @param random the source of randomness for the shuffle
     * @throws IllegalArgumentException if an argument is null, both players are
     *         the same, the blinds are not positive with the small blind no
     *         larger than the big blind, or a player has fewer chips than the
     *         big blind
     */
    public Game(Player smallBlindPlayer, Player bigBlindPlayer, int smallBlind, int bigBlind,
                Deck deck, Random random) {
        if (smallBlindPlayer == null || bigBlindPlayer == null || deck == null || random == null) {
            throw new IllegalArgumentException("players, deck, and random must not be null");
        }
        if (smallBlindPlayer == bigBlindPlayer) {
            throw new IllegalArgumentException("a game needs two different players");
        }
        if (smallBlind <= 0 || smallBlind > bigBlind) {
            throw new IllegalArgumentException("blinds must be positive, small blind at most the big blind");
        }
        if (smallBlindPlayer.getChips() < bigBlind || bigBlindPlayer.getChips() < bigBlind) {
            throw new IllegalArgumentException("each player needs at least the big blind");
        }
        this.players = new Player[] {smallBlindPlayer, bigBlindPlayer};
        this.bigBlind = bigBlind;

        deck.shuffle(random);
        for (int i = 0; i < PLAYER_COUNT; i++) {
            Player player = this.players[i];
            this.startingChips[i] = player.getChips();
            player.clearHand();
            player.receiveHoleCards(new HoleCards(deck.dealToHand(), deck.dealToHand()));
            this.holeCardTexts.add(player.getHoleCards().toString());
        }
        List<Card> community = new ArrayList<>();
        for (int i = 0; i < BOARD_SIZE; i++) {
            community.add(deck.dealToBoard());
        }
        this.board = new Board(community);

        this.street = Street.PREFLOP;
        postBlind(SMALL_BLIND, smallBlind);
        postBlind(BIG_BLIND, bigBlind);
        this.currentBet = bigBlind;
        this.lastRaiseSize = bigBlind;
        this.toAct = SMALL_BLIND;
    }

    private void postBlind(int index, int amount) {
        pay(index, amount);
        log(this.players[index].getName() + " posts blind " + amount);
    }

    public Street getStreet() {
        return this.street;
    }

    public int getPot() {
        return this.pot;
    }

    public Board getBoard() {
        return this.board;
    }

    /**
     * Returns the two players: the small blind first, then the big blind.
     */
    public List<Player> getPlayers() {
        return List.of(this.players[SMALL_BLIND], this.players[BIG_BLIND]);
    }

    public boolean isHandOver() {
        return this.handOver;
    }

    /**
     * Returns the player who won the hand, or null if the hand is not over or
     * the pot was split.
     */
    public Player getWinner() {
        return this.winner;
    }

    /**
     * Returns everything that has happened in this hand, in order.
     *
     * @return a copy of the log, one line per event
     */
    public List<String> getActionLog() {
        return new ArrayList<>(this.actionLog);
    }

    /**
     * Returns the player whose turn it is.
     *
     * @throws IllegalStateException if the hand is over
     */
    public Player getPlayerToAct() {
        requireInProgress();
        return this.players[this.toAct];
    }

    /**
     * Returns how many chips the player to act must add to stay in the hand.
     *
     * @throws IllegalStateException if the hand is over
     */
    public int getAmountToCall() {
        requireInProgress();
        return this.currentBet - this.streetBets[this.toAct];
    }

    /**
     * Returns the kinds of action the player to act may take.
     *
     * @throws IllegalStateException if the hand is over
     */
    public List<ActionType> getLegalActionTypes() {
        requireInProgress();
        List<ActionType> legal = new ArrayList<>();
        legal.add(ActionType.FOLD);
        if (getAmountToCall() == 0) {
            legal.add(ActionType.CHECK);
        }
        else {
            legal.add(ActionType.CALL);
        }
        if (canWager()) {
            if (this.currentBet == 0) {
                legal.add(ActionType.BET);
            }
            else {
                legal.add(ActionType.RAISE);
            }
        }
        return legal;
    }

    /**
     * Returns the smallest total the player to act may bet or raise to. It is
     * only meaningful when a bet or a raise is legal.
     *
     * @throws IllegalStateException if the hand is over
     */
    public int getMinWager() {
        requireInProgress();
        int fullWager;
        if (this.currentBet == 0) {
            fullWager = this.bigBlind;
        }
        else {
            fullWager = this.currentBet + this.lastRaiseSize;
        }
        return Math.min(fullWager, getMaxWager());
    }

    /**
     * Returns the largest total the player to act may bet or raise to, which
     * puts all of their chips in. It is only meaningful when a bet or a raise
     * is legal.
     *
     * @throws IllegalStateException if the hand is over
     */
    public int getMaxWager() {
        requireInProgress();
        return this.streetBets[this.toAct] + this.players[this.toAct].getChips();
    }

    /**
     * Returns whether the player to act may take the given action now.
     *
     * @param action the action to check
     * @return true if the action is allowed
     */
    public boolean isLegal(Action action) {
        if (this.handOver || action == null) {
            return false;
        }
        boolean legal;
        switch (action.getType()) {
            case FOLD:
                legal = true;
                break;
            case CHECK:
                legal = getAmountToCall() == 0;
                break;
            case CALL:
                legal = getAmountToCall() > 0;
                break;
            case BET:
                legal = this.currentBet == 0 && isLegalWager(action.getAmount());
                break;
            default:
                legal = this.currentBet > 0 && isLegalWager(action.getAmount());
                break;
        }
        return legal;
    }

    private boolean isLegalWager(int total) {
        return canWager() && total >= getMinWager() && total <= getMaxWager();
    }

    private boolean canWager() {
        Player player = this.players[this.toAct];
        Player opponent = this.players[1 - this.toAct];
        return opponent.getChips() > 0 && player.getChips() > getAmountToCall();
    }

    /**
     * Applies an action for the player whose turn it is, then moves the hand
     * forward: to the other player, to the next betting round, or to the end
     * of the hand.
     *
     * @param action the action to apply
     * @throws IllegalStateException if the hand is over
     * @throws IllegalArgumentException if the action is not legal now
     */
    public void apply(Action action) {
        requireInProgress();
        if (!isLegal(action)) {
            throw new IllegalArgumentException(action + " is not legal now");
        }
        int me = this.toAct;
        int opponent = 1 - me;
        Player player = this.players[me];
        log(player.getName() + " " + action);

        switch (action.getType()) {
            case FOLD:
                player.fold();
                award(this.players[opponent]);
                return;
            case CHECK:
                break;
            case CALL:
                int toCall = getAmountToCall();
                int paid = Math.min(toCall, player.getChips());
                pay(me, paid);
                if (paid < toCall) {
                    refund(opponent, toCall - paid);
                }
                break;
            default:
                int total = action.getAmount();
                int raiseSize = total - this.currentBet;
                pay(me, total - this.streetBets[me]);
                if (raiseSize >= this.lastRaiseSize) {
                    this.lastRaiseSize = raiseSize;
                }
                this.currentBet = total;
                break;
        }

        this.acted[me] = true;
        boolean betsMatch = this.streetBets[me] == this.streetBets[opponent];
        boolean opponentDone = this.acted[opponent] || this.players[opponent].getChips() == 0;
        if (betsMatch && opponentDone) {
            endBettingRound();
        }
        else {
            this.toAct = opponent;
        }
    }

    private void pay(int index, int amount) {
        if (amount > 0) {
            this.players[index].bet(amount);
        }
        this.streetBets[index] += amount;
        this.pot += amount;
    }

    /**
     * Returns the part of a bet that the other player could not match because
     * they ran out of chips.
     */
    private void refund(int index, int amount) {
        this.players[index].win(amount);
        this.streetBets[index] -= amount;
        this.pot -= amount;
        this.currentBet = this.streetBets[index];
    }

    private void endBettingRound() {
        boolean someoneAllIn = this.players[SMALL_BLIND].getChips() == 0
                || this.players[BIG_BLIND].getChips() == 0;
        if (this.street == Street.RIVER) {
            showdown();
        }
        else if (someoneAllIn) {
            while (this.board.getRevealedCount() < BOARD_SIZE) {
                revealNext();
            }
            showdown();
        }
        else {
            revealNext();
            for (int i = 0; i < PLAYER_COUNT; i++) {
                this.streetBets[i] = 0;
                this.acted[i] = false;
            }
            this.currentBet = 0;
            this.lastRaiseSize = this.bigBlind;
            this.toAct = BIG_BLIND;
        }
    }

    private void revealNext() {
        int revealed = this.board.getRevealedCount();
        if (revealed == 0) {
            this.board.revealFlop();
            this.street = Street.FLOP;
        }
        else if (revealed == FLOP_SIZE) {
            this.board.revealTurn();
            this.street = Street.TURN;
        }
        else {
            this.board.revealRiver();
            this.street = Street.RIVER;
        }
    }

    private void showdown() {
        this.street = Street.SHOWDOWN;
        int result = handValueOf(SMALL_BLIND).compareTo(handValueOf(BIG_BLIND));
        if (result > 0) {
            award(this.players[SMALL_BLIND]);
        }
        else if (result < 0) {
            award(this.players[BIG_BLIND]);
        }
        else {
            // An odd chip goes to the big blind, who is first to act after the flop.
            int half = this.pot / 2;
            this.players[SMALL_BLIND].win(half);
            this.players[BIG_BLIND].win(this.pot - half);
            log("split pot " + this.pot);
            this.pot = 0;
            this.handOver = true;
        }
    }

    private HandValue handValueOf(int index) {
        List<Card> cards = new ArrayList<>(this.players[index].getHoleCards().getCards());
        cards.addAll(this.board.getRevealedCards());
        return this.evaluator.evaluate(cards);
    }

    private void award(Player player) {
        log(player.getName() + " wins " + this.pot);
        player.win(this.pot);
        this.pot = 0;
        this.winner = player;
        this.handOver = true;
    }

    /**
     * Collects what the player to act can see into a context for the computer
     * opponent's decision.
     *
     * @param winChance that player's chance of winning, between 0 and 1
     * @return the context
     * @throws IllegalStateException if the hand is over
     */
    public DecisionContext getDecisionContext(double winChance) {
        requireInProgress();
        return new DecisionContext(
                getPlayerToAct().getHoleCards().getCards(),
                this.board.getRevealedCards(),
                this.pot,
                getAmountToCall(),
                getLegalActionTypes(),
                getMinWager(),
                getMaxWager(),
                winChance);
    }

    /**
     * Builds the record of this finished hand for the history.
     *
     * @param id an identifier for the record
     * @param playedAt when the hand was played
     * @return the record
     * @throws IllegalStateException if the hand is not over
     */
    public HandRecord toRecord(String id, Instant playedAt) {
        if (!this.handOver) {
            throw new IllegalStateException("the hand is not over");
        }
        List<String> names = new ArrayList<>();
        List<Integer> chipChanges = new ArrayList<>();
        for (int i = 0; i < PLAYER_COUNT; i++) {
            names.add(this.players[i].getName());
            chipChanges.add(this.players[i].getChips() - this.startingChips[i]);
        }
        List<String> boardTexts = new ArrayList<>();
        for (Card card : this.board.getRevealedCards()) {
            boardTexts.add(card.toString());
        }
        String winnerName = null;
        if (this.winner != null) {
            winnerName = this.winner.getName();
        }
        return new HandRecord(id, playedAt, names, this.holeCardTexts, boardTexts,
                this.actionLog, winnerName, chipChanges);
    }

    private void requireInProgress() {
        if (this.handOver) {
            throw new IllegalStateException("the hand is over");
        }
    }

    private void log(String event) {
        this.actionLog.add(this.street + ": " + event);
    }
}
