package interface_adapter.table;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import data_access.InMemoryGameSessionDataAccessObject;
import entity.Action;
import entity.BotProfile;
import entity.DecisionContext;
import entity.HandRecord;
import entity.Match;
import interface_adapter.opponent_action.OpponentActionController;
import interface_adapter.opponent_action.OpponentActionPresenter;
import interface_adapter.player_action.PlayerActionController;
import interface_adapter.player_action.PlayerActionPresenter;
import interface_adapter.start_hand.StartHandController;
import interface_adapter.start_hand.StartHandPresenter;
import use_case.opponent_action.OpponentActionInteractor;
import use_case.opponent_action.OpponentUnavailableException;
import use_case.player_action.PlayerActionInteractor;
import use_case.start_hand.StartHandInteractor;

/**
 * The start-hand and betting flows wired as in the application:
 * controllers, interactors, the real presenters, and one table view model.
 * The tests of the presenters drive the game through the controllers and look
 * at the states the view model receives.
 *
 * <p>The opponent's decision source is scripted: it answers with the action
 * given to {@link #setOpponentAnswer(Action)}, and is unavailable when that
 * is null.
 */
public final class TableHarness {
    private static final Clock CLOCK = Clock.fixed(Instant.parse("2026-11-13T02:15:30Z"), ZoneOffset.UTC);

    private final TableViewModel viewModel = new TableViewModel();
    private final InMemoryGameSessionDataAccessObject session = new InMemoryGameSessionDataAccessObject();
    private final List<HandRecord> records = new ArrayList<>();
    private final List<TableState> states = new ArrayList<>();
    private final StartHandController startHand;
    private final PlayerActionController playerAction;
    private final OpponentActionController opponentAction;
    private Action opponentAnswer;

    /**
     * Wires the game.
     *
     * @param seed the seed of the shuffles and of the built-in opponent
     */
    public TableHarness(long seed) {
        this.viewModel.addPropertyChangeListener(event -> this.states.add((TableState) event.getNewValue()));
        this.startHand = new StartHandController(new StartHandInteractor(this.session,
                new StartHandPresenter(this.viewModel), new Random(seed)));
        this.playerAction = new PlayerActionController(new PlayerActionInteractor(this.session,
                this.records::add, new PlayerActionPresenter(this.viewModel),
                () -> "hand-" + (this.records.size() + 1), CLOCK));
        this.opponentAction = new OpponentActionController(new OpponentActionInteractor(this.session,
                this.records::add, () -> new BotProfile("Balanced", 0.5), this::decide,
                new OpponentActionPresenter(this.viewModel), new Random(seed),
                () -> "hand-" + (this.records.size() + 1), CLOCK));
    }

    private Action decide(DecisionContext context, BotProfile profile) throws OpponentUnavailableException {
        if (this.opponentAnswer == null) {
            throw new OpponentUnavailableException("no answer scripted");
        }
        return this.opponentAnswer;
    }

    public TableViewModel getViewModel() {
        return this.viewModel;
    }

    /**
     * Returns the latest state of the table.
     */
    public TableState getState() {
        return this.viewModel.getState();
    }

    /**
     * Returns every state the view model's listeners were told about, in order.
     */
    public List<TableState> getStates() {
        return this.states;
    }

    public InMemoryGameSessionDataAccessObject getSession() {
        return this.session;
    }

    public Match getMatch() {
        return this.session.getMatch();
    }

    public List<HandRecord> getRecords() {
        return this.records;
    }

    public StartHandController getStartHand() {
        return this.startHand;
    }

    public PlayerActionController getPlayerAction() {
        return this.playerAction;
    }

    public OpponentActionController getOpponentAction() {
        return this.opponentAction;
    }

    /**
     * Scripts the decision source's next answers; null makes it unavailable.
     */
    public void setOpponentAnswer(Action answer) {
        this.opponentAnswer = answer;
    }
}
