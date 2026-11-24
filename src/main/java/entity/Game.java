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
 *
 * <p>A player is asked to act only while there is something to decide. As
 * soon as the bets are matched and one of the players has no chips left,
 * nobody can bet any more: the betting is over, the community cards that are
 * still face down are revealed one street after another, and the hands are
 * compared. A player who faces a bet they have not matched is always asked,
 * even if the bet is the other player's last chips.
 *
 * <p>Everything that happens is kept twice: as a line of text in the action
 * log, which goes into the {@link HandRecord}, and as a {@link HandEvent},
 * which also covers what the log leaves out (returned bets, newly revealed
 * community cards, and the hands shown at the showdown).
 */
public class Game {
    private static final int SMALL_BLIND = 0;
    private static final int BIG_BLIND = 1;
    private static final int PLAYER_COUNT = 2;
    private static final int BOARD_SIZE = 5;
    private static final int FLOP_SIZE = 3;
    private static final List<String> NO_CARDS = List.of();
    private static final String NO_DETAIL = "";

    private final Player[] players;
    private final int bigBlind;
    private final Board board;
    private final int[] startingChips = new int[PLAYER_COUNT];
    private final int[] streetBets = new int[PLAYER_COUNT];
    private final boolean[] acted = new boolean[PLAYER_COUNT];
    private final List<String> holeCardTexts = new ArrayList<>();
    private final List<String> actionLog = new ArrayList<>();
    private final List<HandEvent> events = new ArrayList<>();
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
        recordBetting(HandEventType.BLIND, index, amount);
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
     * Returns everything that has happened in this hand as events, in order.
     * The events say more than the action log: they also cover returned bets,
     * newly revealed community cards, and the hands shown at the showdown.
     *
     * @return a copy of the list of events
     */
    public List<HandEvent> getEvents() {
        return new ArrayList<>(this.events);
    }

    public int getBigBlind() {
        return this.bigBlind;
    }

    /**
     * Returns how many chips the given player has in front of them on the
     * current betting round. Once the hand is over all bets have gone to the
     * winner, so the result is 0.
     *
     * @param player one of the two players of this hand
     * @return that player's bet on the current betting round
     * @throws IllegalArgumentException if the player is not in this hand
     */
    public int getStreetBet(Player player) {
        int index = indexOf(player);
        if (this.handOver) {
            return 0;
        }
        return this.streetBets[index];
    }

    /**
     * Returns the best hand the given player can make from their hole cards
     * and the revealed community cards.
     *
     * @param player one of the two players of this hand
     * @return the value of that player's best hand
     * @throws IllegalArgumentException if the player is not in this hand
     * @throws IllegalStateException if fewer than three community cards are
     *         revealed
     */
    public HandValue getHandValue(Player player) {
        int index = indexOf(player);
        if (this.board.getRevealedCount() < FLOP_SIZE) {
            throw new IllegalStateException("the flop has not been revealed");
        }
        return handValueOf(index);
    }

    /**
     * Returns the five cards that make the best hand the given player can make
     * from their hole cards and the revealed community cards: the hand whose
     * value {@link #getHandValue(Player)} returns.
     *
     * <p>Where cards of the same rank could stand in for each other, the
     * player's own hole cards are chosen before community cards, and a
     * community card revealed earlier before one revealed later, so the same
     * hand always gives the same five cards.
     *
     * @param player one of the two players of this hand
     * @return a new list of the five cards: the player's hole cards among
     *         them first, then the community cards in the order they were
     *         revealed
     * @throws IllegalArgumentException if the player is not in this hand
     * @throws IllegalStateException if fewer than three community cards are
     *         revealed
     */
    public List<Card> getBestFive(Player player) {
        int index = indexOf(player);
        if (this.board.getRevealedCount() < FLOP_SIZE) {
            throw new IllegalStateException("the flop has not been revealed");
        }
        return this.evaluator.bestFive(cardsOf(index));
    }

    private int indexOf(Player player) {
        for (int i = 0; i < PLAYER_COUNT; i++) {
            if (this.players[i] == player) {
                return i;
            }
        }
        throw new IllegalArgumentException("the player is not in this hand");
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
        // The betting kinds of event have the same names as the kinds of action.
        HandEventType eventType = HandEventType.valueOf(action.getType().name());

        switch (action.getType()) {
            case FOLD:
                player.fold();
                recordBetting(eventType, me, 0);
                award(this.players[opponent]);
                return;
            case CHECK:
                recordBetting(eventType, me, 0);
                break;
            case CALL:
                int toCall = getAmountToCall();
                int paid = Math.min(toCall, player.getChips());
                pay(me, paid);
                recordBetting(eventType, me, paid);
                if (paid < toCall) {
                    refund(opponent, toCall - paid);
                }
                break;
            default:
                int total = action.getAmount();
                int raiseSize = total - this.currentBet;
                int added = total - this.streetBets[me];
                pay(me, added);
                recordBetting(eventType, me, added);
                if (raiseSize >= this.lastRaiseSize) {
                    this.lastRaiseSize = raiseSize;
                }
                this.currentBet = total;
                break;
        }

        this.acted[me] = true;
        boolean betsMatch = this.streetBets[me] == this.streetBets[opponent];
        // Once the bets match, the opponent has something left to decide only if they have not
        // acted yet and could still bet. With either player all-in nobody can bet any more: a
        // small blind who calls with their last chips leaves the big blind no option to use.
        boolean nobodyCanBet = player.getChips() == 0 || this.players[opponent].getChips() == 0;
        if (betsMatch && (this.acted[opponent] || nobodyCanBet)) {
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
        recordBetting(HandEventType.REFUND, index, amount);
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
        HandEventType eventType;
        if (revealed == 0) {
            this.board.revealFlop();
            this.street = Street.FLOP;
            eventType = HandEventType.FLOP;
        }
        else if (revealed == FLOP_SIZE) {
            this.board.revealTurn();
            this.street = Street.TURN;
            eventType = HandEventType.TURN;
        }
        else {
            this.board.revealRiver();
            this.street = Street.RIVER;
            eventType = HandEventType.RIVER;
        }
        List<Card> shown = this.board.getRevealedCards();
        List<String> newCards = textsOf(shown.subList(revealed, shown.size()));
        this.events.add(new HandEvent(eventType, null, 0, 0, newCards, NO_DETAIL));
    }

    private void showdown() {
        this.street = Street.SHOWDOWN;
        HandValue smallBlindValue = handValueOf(SMALL_BLIND);
        HandValue bigBlindValue = handValueOf(BIG_BLIND);
        recordShowdown(SMALL_BLIND, smallBlindValue);
        recordShowdown(BIG_BLIND, bigBlindValue);
        int result = smallBlindValue.compareTo(bigBlindValue);
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
            recordPayout(HandEventType.SPLIT, this.players[SMALL_BLIND], half);
            recordPayout(HandEventType.SPLIT, this.players[BIG_BLIND], this.pot - half);
            this.pot = 0;
            this.handOver = true;
        }
    }

    private HandValue handValueOf(int index) {
        return this.evaluator.evaluate(cardsOf(index));
    }

    /**
     * Returns the cards a player can make a hand from: their two hole cards,
     * then the revealed community cards in the order they were revealed.
     */
    private List<Card> cardsOf(int index) {
        List<Card> cards = new ArrayList<>(this.players[index].getHoleCards().getCards());
        cards.addAll(this.board.getRevealedCards());
        return cards;
    }

    private void award(Player player) {
        log(player.getName() + " wins " + this.pot);
        recordPayout(HandEventType.WIN, player, this.pot);
        player.win(this.pot);
        this.pot = 0;
        this.winner = player;
        this.handOver = true;
    }

    /**
     * Collects what the player to act can see into a context for the computer
     * opponent's decision.
     *
     * <p>The pot and the amount to call are the ones the decision is about. A
     * player who faces a bet larger than their stack can call only with the
     * chips they have, and the part of the bet they cannot match goes back to
     * the other player. So the amount to call is never more than the player's
     * stack, and the pot leaves out the chips that would go back.
     *
     * @param winChance that player's chance of winning, between 0 and 1
     * @return the context
     * @throws IllegalStateException if the hand is over
     */
    public DecisionContext getDecisionContext(double winChance) {
        requireInProgress();
        int owed = getAmountToCall();
        int canPay = Math.min(owed, getPlayerToAct().getChips());
        return new DecisionContext(
                getPlayerToAct().getHoleCards().getCards(),
                this.board.getRevealedCards(),
                this.pot - (owed - canPay),
                canPay,
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
        List<String> boardTexts = textsOf(this.board.getRevealedCards());
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

    /**
     * Records an event about a player's bet on the current betting round: a
     * blind, a betting action, or a returned bet.
     */
    private void recordBetting(HandEventType type, int index, int amount) {
        this.events.add(new HandEvent(type, this.players[index].getName(), amount,
                this.streetBets[index], NO_CARDS, NO_DETAIL));
    }

    private void recordShowdown(int index, HandValue value) {
        Player player = this.players[index];
        this.events.add(new HandEvent(HandEventType.SHOWDOWN, player.getName(), 0, 0,
                textsOf(player.getHoleCards().getCards()), value.getCategory().getDisplayName()));
    }

    private void recordPayout(HandEventType type, Player player, int amount) {
        this.events.add(new HandEvent(type, player.getName(), amount, 0, NO_CARDS, NO_DETAIL));
    }

    private static List<String> textsOf(List<Card> cards) {
        List<String> texts = new ArrayList<>();
        for (Card card : cards) {
            texts.add(card.toString());
        }
        return texts;
    }
}
