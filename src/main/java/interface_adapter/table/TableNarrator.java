package interface_adapter.table;

import java.util.ArrayList;
import java.util.List;

import use_case.table.TableEventData;
import use_case.table.TableOutputData;

/**
 * Puts what happened at the table into words: a sentence for every event, and
 * one short sentence about the whole step for the caption of the game screen.
 *
 * <p>The human is always addressed as "you"; the opponent goes by its name.
 */
final class TableNarrator {
    private static final String YOU = "You";
    private static final String LOWER_YOU = "you";
    private static final String SMALL_BLIND = "small";
    private static final String BIG_BLIND = "big";
    private static final String CARD_SEPARATOR = " ";
    private static final String DASH = " — ";
    private static final String FULL_STOP = ".";
    private static final int SEATS = 2;
    private static final int HUMAN_SEAT = 0;
    private static final int OPPONENT_SEAT = 1;
    private static final int NO_SEAT = -1;

    private TableNarrator() {
    }

    /**
     * Describes the events of a table for the screen: card text becomes
     * picture ids, and every event gets its sentence.
     *
     * @param table the table as a use case reported it
     * @return the events of that report, in the same order
     */
    static List<TableEventView> describeEvents(TableOutputData table) {
        List<TableEventData> events = table.getEvents();
        boolean[] allIn = findAllIns(table);
        List<TableEventView> views = new ArrayList<>();
        for (int i = 0; i < events.size(); i++) {
            TableEventData event = events.get(i);
            views.add(new TableEventView(event.getType(), event.getActor(), event.getAmount(),
                    event.getTotal(), CardIds.toIds(event.getCards()), textOf(table, event, allIn[i])));
        }
        return views;
    }

    /**
     * Returns one short sentence about what the events of a table amount to:
     * the result if the hand is over, the blinds if the hand has just been
     * dealt, and otherwise the action that was taken.
     *
     * @param table the table as a use case reported it
     * @param eventViews the events of that table as {@link #describeEvents} gave them
     * @return the sentence, or an empty text if nothing happened
     */
    static String summarise(TableOutputData table, List<TableEventView> eventViews) {
        String summary;
        if (eventViews.isEmpty()) {
            summary = summariseStandstill(table);
        }
        else if (table.isHandOver()) {
            summary = summariseResult(table);
        }
        else if (TableEventView.BLIND.equals(eventViews.get(0).getType())) {
            summary = summariseDeal(table);
        }
        else {
            summary = eventViews.get(0).getText() + FULL_STOP;
        }
        return summary;
    }

    /**
     * Finds the events that left their actor without chips. The report only
     * has the stacks as they are after all of its events, so the stacks after
     * each event are found by undoing the events from the last to the first.
     */
    private static boolean[] findAllIns(TableOutputData table) {
        List<TableEventData> events = table.getEvents();
        boolean[] allIn = new boolean[events.size()];
        int[] chips = new int[SEATS];
        chips[HUMAN_SEAT] = table.getHumanChips();
        chips[OPPONENT_SEAT] = table.getOpponentChips();
        for (int i = events.size() - 1; i >= 0; i--) {
            TableEventData event = events.get(i);
            int seat = seatOf(event.getActor());
            if (seat != NO_SEAT) {
                if (isPayment(event.getType())) {
                    allIn[i] = chips[seat] == 0;
                    chips[seat] += event.getAmount();
                }
                else {
                    chips[seat] -= event.getAmount();
                }
            }
        }
        return allIn;
    }

    private static int seatOf(String actor) {
        int seat = NO_SEAT;
        if (TableOutputData.HUMAN.equals(actor)) {
            seat = HUMAN_SEAT;
        }
        else if (TableOutputData.OPPONENT.equals(actor)) {
            seat = OPPONENT_SEAT;
        }
        return seat;
    }

    /**
     * Returns whether an event of this type takes its amount out of the
     * actor's stack. Every other event gives its amount to the actor, or has
     * no amount.
     */
    private static boolean isPayment(String type) {
        return TableEventView.BLIND.equals(type) || TableEventView.CALL.equals(type)
                || TableEventView.BET.equals(type) || TableEventView.RAISE.equals(type);
    }

    private static String textOf(TableOutputData table, TableEventData event, boolean allIn) {
        boolean human = TableOutputData.HUMAN.equals(event.getActor());
        String who = table.getOpponentName();
        if (human) {
            who = YOU;
        }
        String cards = String.join(CARD_SEPARATOR, event.getCards());
        String text;
        switch (event.getType()) {
            case TableEventView.BLIND:
                text = who + verb(human, " post") + " the " + blindOf(human == table.isHumanSmallBlind())
                        + " blind of " + event.getAmount();
                break;
            case TableEventView.FOLD:
                text = who + verb(human, " fold");
                break;
            case TableEventView.CHECK:
                text = who + verb(human, " check");
                break;
            case TableEventView.CALL:
                text = who + verb(human, " call") + " " + event.getAmount();
                break;
            case TableEventView.BET:
                text = who + verb(human, " bet") + " " + event.getTotal();
                break;
            case TableEventView.RAISE:
                text = who + verb(human, " raise") + " to " + event.getTotal();
                break;
            case TableEventView.REFUND:
                text = who + verb(human, " take") + " back " + event.getAmount();
                break;
            case TableEventView.FLOP:
                text = "Flop: " + cards;
                break;
            case TableEventView.TURN:
                text = "Turn: " + cards;
                break;
            case TableEventView.RIVER:
                text = "River: " + cards;
                break;
            case TableEventView.SHOWDOWN:
                text = who + verb(human, " show") + " " + cards + DASH + event.getDetail();
                break;
            case TableEventView.WIN:
                text = who + verb(human, " win") + " " + event.getAmount();
                break;
            case TableEventView.SPLIT:
                text = who + verb(human, " take") + " " + event.getAmount() + " of the split pot";
                break;
            default:
                text = event.getType();
                break;
        }
        if (allIn && human) {
            text += " and are all in";
        }
        else if (allIn) {
            text += " and is all in";
        }
        return text;
    }

    private static String blindOf(boolean smallBlind) {
        String blind = BIG_BLIND;
        if (smallBlind) {
            blind = SMALL_BLIND;
        }
        return blind;
    }

    /**
     * Returns a verb in the form that goes with its subject: as it is after
     * "You", and with an "s" after the opponent's name.
     */
    private static String verb(boolean human, String verb) {
        String form = verb + "s";
        if (human) {
            form = verb;
        }
        return form;
    }

    /**
     * Says how the hand ended: by a fold, by a split pot, or by one player
     * showing the better hand.
     */
    private static String summariseResult(TableOutputData table) {
        List<TableEventData> events = table.getEvents();
        String opponent = table.getOpponentName();
        String folder = actorOf(events, TableEventView.FOLD);
        String summary;
        if (TableOutputData.HUMAN.equals(folder)) {
            summary = "You fold and " + opponent + " wins "
                    + amountOf(events, TableEventView.WIN, TableOutputData.OPPONENT);
        }
        else if (TableOutputData.OPPONENT.equals(folder)) {
            summary = opponent + " folds and you win "
                    + amountOf(events, TableEventView.WIN, TableOutputData.HUMAN);
        }
        else if (TableOutputData.SPLIT.equals(table.getWinner())) {
            summary = "Split pot" + DASH + "you take "
                    + amountOf(events, TableEventView.SPLIT, TableOutputData.HUMAN);
        }
        else if (TableOutputData.HUMAN.equals(table.getWinner())) {
            summary = "You win " + amountOf(events, TableEventView.WIN, TableOutputData.HUMAN)
                    + DASH + table.getHumanHandName();
        }
        else {
            summary = opponent + " wins " + amountOf(events, TableEventView.WIN, TableOutputData.OPPONENT)
                    + DASH + table.getOpponentHandName();
        }
        return summary + FULL_STOP;
    }

    /**
     * Says which hand has been dealt and which blind the human posted.
     */
    private static String summariseDeal(TableOutputData table) {
        return "Hand " + table.getHandNumber() + DASH + LOWER_YOU + " post the "
                + blindOf(table.isHumanSmallBlind()) + " blind of "
                + amountOf(table.getEvents(), TableEventView.BLIND, TableOutputData.HUMAN) + FULL_STOP;
    }

    /**
     * Says what a table without new events has to say: who won the match if it
     * is over, and nothing otherwise.
     */
    private static String summariseStandstill(TableOutputData table) {
        String summary = TableOutputData.NONE;
        if (table.isMatchOver() && table.getHumanChips() > table.getOpponentChips()) {
            summary = "The match is over" + DASH + LOWER_YOU + " won" + FULL_STOP;
        }
        else if (table.isMatchOver()) {
            summary = "The match is over" + DASH + table.getOpponentName() + " won" + FULL_STOP;
        }
        return summary;
    }

    /**
     * Returns the actor of the first event of the given type, or an empty text
     * if there is no such event.
     */
    private static String actorOf(List<TableEventData> events, String type) {
        String actor = TableOutputData.NONE;
        for (int i = events.size() - 1; i >= 0; i--) {
            if (type.equals(events.get(i).getType())) {
                actor = events.get(i).getActor();
            }
        }
        return actor;
    }

    /**
     * Returns the amount of the first event of the given type about the given
     * actor, or 0 if there is no such event.
     */
    private static int amountOf(List<TableEventData> events, String type, String actor) {
        int amount = 0;
        for (int i = events.size() - 1; i >= 0; i--) {
            TableEventData event = events.get(i);
            if (type.equals(event.getType()) && actor.equals(event.getActor())) {
                amount = event.getAmount();
            }
        }
        return amount;
    }
}
