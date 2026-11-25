package use_case.table;

import java.util.ArrayList;
import java.util.List;

import entity.ActionType;
import entity.Card;
import entity.EquityCalculator;
import entity.EquityResult;
import entity.Game;
import entity.HandEvent;
import entity.HandEventType;
import entity.Match;
import entity.Player;
import entity.Street;

/**
 * Turns the entities of a match into the description of the table that
 * crosses the output boundary. This is the single place where a {@link Match}
 * and its current {@link Game} become a {@link TableOutputData} after a deal
 * or a betting action by either player.
 *
 * <p>The table is described from the human's seat. The opponent's hole cards,
 * the name of the opponent's hand, and the cards that make the winning hand
 * are reported only when the hand ended in a showdown, and the chances are
 * worked out from the human's own cards and the revealed community cards
 * alone.
 *
 * <p>This class is a helper of the use case layer, not an interactor: it has
 * no boundaries and changes nothing.
 */
public final class TableReporter {
    private static final int FLOP_SIZE = 3;

    private final EquityCalculator calculator;

    /**
     * Creates a reporter that works out the human's chances exactly.
     */
    public TableReporter() {
        this(new EquityCalculator());
    }

    /**
     * Creates a reporter that works out the human's chances with the given
     * calculator.
     *
     * @param calculator the calculator of chances
     * @throws IllegalArgumentException if calculator is null
     */
    public TableReporter(EquityCalculator calculator) {
        if (calculator == null) {
            throw new IllegalArgumentException("calculator must not be null");
        }
        this.calculator = calculator;
    }

    /**
     * Describes the table as it is now.
     *
     * @param match the match being played
     * @param eventsAlreadyReported how many events of the current hand earlier
     *        reports have already carried; the events after them are the ones
     *        this report carries. Pass 0 for a hand that has just started
     * @param decidedBy who chose the opponent's action that this report is
     *        about: {@link TableOutputData#DECIDED_BY_LLM},
     *        {@link TableOutputData#DECIDED_BY_BUILT_IN}, or
     *        {@link TableOutputData#NONE} if the opponent did not act
     * @return the table in plain types
     * @throws IllegalArgumentException if match or decidedBy is null, or
     *         eventsAlreadyReported is negative or larger than the number of
     *         events of the current hand
     */
    public TableOutputData report(Match match, int eventsAlreadyReported, String decidedBy) {
        if (match == null || decidedBy == null) {
            throw new IllegalArgumentException("match and decidedBy must not be null");
        }
        Game game = match.getCurrentGame();
        List<HandEvent> events = new ArrayList<>();
        if (game != null) {
            events = game.getEvents();
        }
        if (eventsAlreadyReported < 0 || eventsAlreadyReported > events.size()) {
            throw new IllegalArgumentException("eventsAlreadyReported must be between 0 and " + events.size());
        }

        Player human = match.getHuman();
        Player computer = match.getComputer();
        TableOutputData.Builder builder = new TableOutputData.Builder()
                .setHandNumber(match.getHandNumber())
                .setHumanName(human.getName())
                .setOpponentName(computer.getName())
                .setStreet(Street.PREFLOP.name())
                .setHumanChips(human.getChips())
                .setOpponentChips(computer.getChips())
                .setHumanSmallBlind(match.isHumanSmallBlind())
                .setMatchOver(match.isOver())
                .setDecidedBy(decidedBy);
        if (game != null) {
            describeHand(builder, match, game);
            builder.setEvents(describeEvents(match, events.subList(eventsAlreadyReported, events.size())));
            if (game.isHandOver()) {
                describeResult(builder, match, game, events);
            }
            else {
                describeTurn(builder, match, game);
            }
        }
        return builder.build();
    }

    /**
     * Adds what the human can see of the hand: the chips in play, the human's
     * own cards, the community cards, and what the human's cards are worth.
     */
    private void describeHand(TableOutputData.Builder builder, Match match, Game game) {
        Player human = match.getHuman();
        builder.setStreet(game.getStreet().name())
                .setPot(game.getPot())
                .setHumanBet(game.getStreetBet(human))
                .setOpponentBet(game.getStreetBet(match.getComputer()))
                .setHumanCards(textsOf(human.getHoleCards().getCards()))
                .setBoardCards(textsOf(game.getBoard().getRevealedCards()))
                .setHandOver(game.isHandOver());
        if (!human.isFolded()) {
            EquityResult chances = human.winChance(game.getBoard(), this.calculator);
            builder.setWinChance(chances.getWin())
                    .setTieChance(chances.getTie())
                    .setLoseChance(chances.getLose());
        }
        if (game.getBoard().getRevealedCount() >= FLOP_SIZE) {
            builder.setHumanHandName(handNameOf(game, human));
        }
    }

    /**
     * Adds whose turn it is in a hand that is still being played and, when it
     * is the human's, what the human may do. The amount to call is what a call
     * costs the human: when the bet is larger than the human's stack, a call
     * puts in the stack and the rest of the bet goes back to the opponent.
     */
    private void describeTurn(TableOutputData.Builder builder, Match match, Game game) {
        if (game.getPlayerToAct() == match.getHuman()) {
            List<String> legalActions = new ArrayList<>();
            for (ActionType type : game.getLegalActionTypes()) {
                legalActions.add(type.name());
            }
            builder.setToAct(TableOutputData.HUMAN)
                    .setLegalActions(legalActions)
                    .setCallAmount(Math.min(game.getAmountToCall(), match.getHuman().getChips()))
                    .setMinWager(game.getMinWager())
                    .setMaxWager(game.getMaxWager());
        }
        else {
            builder.setToAct(TableOutputData.OPPONENT);
        }
    }

    /**
     * Adds how a finished hand came out. The opponent's cards and the cards
     * that make the winning hand are added only here, and only when the hand
     * was decided by comparing the hands.
     */
    private void describeResult(TableOutputData.Builder builder, Match match, Game game,
                                List<HandEvent> events) {
        Player human = match.getHuman();
        Player computer = match.getComputer();
        String winner = TableOutputData.SPLIT;
        if (game.getWinner() == human) {
            winner = TableOutputData.HUMAN;
        }
        else if (game.getWinner() == computer) {
            winner = TableOutputData.OPPONENT;
        }
        builder.setWinner(winner)
                .setHumanChipChange(chipChangeOf(human, events));
        if (game.getStreet() == Street.SHOWDOWN) {
            builder.setOpponentCards(textsOf(computer.getHoleCards().getCards()))
                    .setOpponentHandName(handNameOf(game, computer))
                    .setWinningCards(textsOf(winningCardsOf(game, human, computer)));
        }
    }

    /**
     * Returns the cards that make the winning hand of a hand that ended in a
     * showdown: the winner's best five cards or, if the pot was split, the
     * human's best five followed by the cards of the opponent's best five
     * that are not among them.
     */
    private static List<Card> winningCardsOf(Game game, Player human, Player computer) {
        List<Card> cards;
        if (game.getWinner() == null) {
            cards = game.getBestFive(human);
            for (Card card : game.getBestFive(computer)) {
                if (!cards.contains(card)) {
                    cards.add(card);
                }
            }
        }
        else {
            cards = game.getBestFive(game.getWinner());
        }
        return cards;
    }

    private List<TableEventData> describeEvents(Match match, List<HandEvent> events) {
        List<TableEventData> described = new ArrayList<>();
        for (HandEvent event : events) {
            described.add(new TableEventData(event.getType().name(), actorOf(match, event),
                    event.getAmount(), event.getTotal(), event.getCards(), event.getDetail()));
        }
        return described;
    }

    private static String actorOf(Match match, HandEvent event) {
        String actor = TableOutputData.NONE;
        if (match.getHuman().getName().equals(event.getPlayerName())) {
            actor = TableOutputData.HUMAN;
        }
        else if (match.getComputer().getName().equals(event.getPlayerName())) {
            actor = TableOutputData.OPPONENT;
        }
        return actor;
    }

    /**
     * Returns how many chips the player has won (positive) or lost (negative)
     * over the given events: what came back to the player less what the player
     * put in.
     */
    private static int chipChangeOf(Player player, List<HandEvent> events) {
        int change = 0;
        for (HandEvent event : events) {
            if (player.getName().equals(event.getPlayerName())) {
                if (isPayment(event.getType())) {
                    change -= event.getAmount();
                }
                else {
                    change += event.getAmount();
                }
            }
        }
        return change;
    }

    /**
     * Returns whether an event of this type takes its amount out of the
     * player's stack. Every other event about a player gives its amount to the
     * player, or has no amount.
     */
    private static boolean isPayment(HandEventType type) {
        return type == HandEventType.BLIND || type == HandEventType.CALL
                || type == HandEventType.BET || type == HandEventType.RAISE;
    }

    private static String handNameOf(Game game, Player player) {
        return game.getHandValue(player).getCategory().getDisplayName();
    }

    private static List<String> textsOf(List<Card> cards) {
        List<String> texts = new ArrayList<>();
        for (Card card : cards) {
            texts.add(card.toString());
        }
        return texts;
    }
}
