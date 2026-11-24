package interface_adapter.table;

import java.util.List;

import use_case.table.TableOutputData;

/**
 * Publishes new states of the table to the {@link TableViewModel}. The
 * start-hand, player-action, and opponent-action presenters share this class
 * so the game screen receives one sequence of states.
 *
 * <p>Every published state has the revision of the state before it plus one.
 * The use cases of the game run one at a time, so two states are never
 * published at once.
 */
public final class TablePublisher {
    private static final int HOLE_CARD_COUNT = 2;
    private static final String FOLD = "FOLD";
    private static final String CHECK = "CHECK";
    private static final String CALL = "CALL";
    private static final String BET = "BET";
    private static final String RAISE = "RAISE";

    private final TableViewModel viewModel;

    /**
     * Creates a publisher.
     *
     * @param viewModel the view model of the game screen
     * @throws IllegalArgumentException if viewModel is null
     */
    public TablePublisher(TableViewModel viewModel) {
        if (viewModel == null) {
            throw new IllegalArgumentException("viewModel must not be null");
        }
        this.viewModel = viewModel;
    }

    /**
     * Publishes the table a use case reported as the next state: cards become
     * picture ids, the opponent's hidden cards become card backs, the bets of
     * the current betting round are taken out of the pot, and what happened is
     * put into words. The state has no error message.
     *
     * @param table the table as the use case reported it
     */
    public void publishTable(TableOutputData table) {
        List<TableEventView> events = TableNarrator.describeEvents(table);
        List<String> legalActions = table.getLegalActions();
        boolean dealt = !table.getHumanCards().isEmpty();
        boolean inProgress = dealt && !table.isHandOver();

        List<String> opponentCards = CardIds.toIds(table.getOpponentCards());
        if (dealt && opponentCards.isEmpty()) {
            opponentCards = CardIds.faceDown(HOLE_CARD_COUNT);
        }

        TableState state = new TableState.Builder()
                .setRevision(nextRevision())
                .setHandNumber(table.getHandNumber())
                .setHumanName(table.getHumanName())
                .setOpponentName(table.getOpponentName())
                .setHumanCards(CardIds.toIds(table.getHumanCards()))
                .setOpponentCards(opponentCards)
                .setBoardCards(CardIds.toIds(table.getBoardCards()))
                .setStreet(table.getStreet())
                .setPot(table.getPot() - table.getHumanBet() - table.getOpponentBet())
                .setHumanChips(table.getHumanChips())
                .setOpponentChips(table.getOpponentChips())
                .setHumanBet(table.getHumanBet())
                .setOpponentBet(table.getOpponentBet())
                .setHumanDealer(table.isHumanSmallBlind())
                .setHandInProgress(inProgress)
                .setHandOver(table.isHandOver())
                .setMatchOver(table.isMatchOver())
                .setWinner(table.getWinner())
                .setHumanToAct(TableOutputData.HUMAN.equals(table.getToAct()))
                .setOpponentToAct(TableOutputData.OPPONENT.equals(table.getToAct()))
                .setCanFold(legalActions.contains(FOLD))
                .setCanCheck(legalActions.contains(CHECK))
                .setCanCall(legalActions.contains(CALL))
                .setCanBet(legalActions.contains(BET))
                .setCanRaise(legalActions.contains(RAISE))
                .setCallAmount(table.getCallAmount())
                .setMinWager(table.getMinWager())
                .setMaxWager(table.getMaxWager())
                .setWinChance(table.getWinChance())
                .setTieChance(table.getTieChance())
                .setLoseChance(table.getLoseChance())
                .setHumanHandName(table.getHumanHandName())
                .setOpponentHandName(table.getOpponentHandName())
                .setWinningCards(CardIds.toIds(table.getWinningCards()))
                .setMessage(TableNarrator.summarise(table, events))
                .setDecidedBy(table.getDecidedBy())
                .setHumanChipChange(table.getHumanChipChange())
                .setEvents(events)
                .build();
        publish(state);
    }

    /**
     * Publishes that a request failed. The new state is the state before it
     * with the next revision, the given error message, and no events; nothing
     * else changes, because a failed request changes nothing about the game
     * that the state before it does not already show.
     *
     * @param errorMessage why the request failed, as a sentence for the user
     * @throws IllegalArgumentException if errorMessage is null
     */
    public void publishFailure(String errorMessage) {
        TableState state = new TableState.Builder(this.viewModel.getState())
                .setRevision(nextRevision())
                .setErrorMessage(errorMessage)
                .setEvents(List.of())
                .build();
        publish(state);
    }

    private long nextRevision() {
        return this.viewModel.getState().getRevision() + 1;
    }

    private void publish(TableState state) {
        this.viewModel.setState(state);
        this.viewModel.firePropertyChanged();
    }
}
