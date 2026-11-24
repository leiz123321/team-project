package use_case.player_action;

import java.time.Clock;
import java.util.List;
import java.util.Locale;
import java.util.function.Supplier;

import entity.Action;
import entity.ActionType;
import entity.EquityCalculator;
import entity.Game;
import entity.Match;
import use_case.table.TableOutputData;
import use_case.table.TableReporter;

/**
 * Runs the use case "Take a betting action": applies the human player's
 * action to the hand in progress and, when that ends the hand, keeps a record
 * of the hand in the history.
 *
 * <p>A request that cannot be carried out changes nothing and is answered
 * with a sentence that says why. The interactor never makes the computer
 * opponent act; when it is the opponent's turn afterwards, the table it
 * reports says so.
 */
public class PlayerActionInteractor implements PlayerActionInputBoundary {
    private static final String NO_HAND = "There is no hand in progress.";
    private static final String NOT_YOUR_TURN = "It is not your turn.";
    private static final String UNKNOWN_ACTION = "Unknown action \"%s\". Choose fold, check, call, bet, or raise.";
    private static final String CANNOT_CHECK = "You cannot check: it is %d to call.";
    private static final String NOTHING_TO_CALL = "There is nothing to call. You can check.";
    private static final String BET_INSTEAD = "There is no bet to raise. You can bet instead.";
    private static final String RAISE_INSTEAD = "There is already a bet. You can raise instead.";
    private static final String CANNOT_WAGER = "You cannot %s now. You can only call or fold.";
    private static final String WAGER_RANGE = "A %s must be between %d and %d.";
    private static final String WAGER_EXACT = "A %s must be exactly %d: all your chips.";
    private static final String NOT_SAVED = "The hand is over, but it could not be saved to the history.";

    private final PlayerActionGameDataAccessInterface gameDataAccess;
    private final PlayerActionHistoryDataAccessInterface historyDataAccess;
    private final PlayerActionOutputBoundary outputBoundary;
    private final Supplier<String> idSupplier;
    private final Clock clock;
    private final TableReporter reporter;

    /**
     * Creates the interactor.
     *
     * @param gameDataAccess where the match being played is kept
     * @param historyDataAccess where the record of a finished hand is kept
     * @param outputBoundary where the result is reported
     * @param idSupplier gives a new identifier for each hand record, for
     *        example a random UUID as text
     * @param clock tells the time a hand finished; pass a fixed one in tests
     * @throws IllegalArgumentException if an argument is null
     */
    public PlayerActionInteractor(PlayerActionGameDataAccessInterface gameDataAccess,
                                  PlayerActionHistoryDataAccessInterface historyDataAccess,
                                  PlayerActionOutputBoundary outputBoundary,
                                  Supplier<String> idSupplier, Clock clock) {
        this(gameDataAccess, historyDataAccess, outputBoundary, idSupplier, clock, new EquityCalculator());
    }

    /**
     * Creates the interactor with the calculator of the chances it reports
     * supplied by the caller.
     *
     * @param gameDataAccess where the match being played is kept
     * @param historyDataAccess where the record of a finished hand is kept
     * @param outputBoundary where the result is reported
     * @param idSupplier gives a new identifier for each hand record
     * @param clock tells the time a hand finished
     * @param calculator works out the human's chances for the reported table
     * @throws IllegalArgumentException if an argument is null
     */
    public PlayerActionInteractor(PlayerActionGameDataAccessInterface gameDataAccess,
                                  PlayerActionHistoryDataAccessInterface historyDataAccess,
                                  PlayerActionOutputBoundary outputBoundary,
                                  Supplier<String> idSupplier, Clock clock, EquityCalculator calculator) {
        if (gameDataAccess == null || historyDataAccess == null || outputBoundary == null
                || idSupplier == null || clock == null) {
            throw new IllegalArgumentException("the data access objects, the output boundary, "
                    + "the id supplier, and the clock must not be null");
        }
        this.gameDataAccess = gameDataAccess;
        this.historyDataAccess = historyDataAccess;
        this.outputBoundary = outputBoundary;
        this.idSupplier = idSupplier;
        this.clock = clock;
        this.reporter = new TableReporter(calculator);
    }

    @Override
    public void execute(PlayerActionInputData inputData) {
        Match match = this.gameDataAccess.getMatch();
        Game game = handInProgress(match);
        if (game == null) {
            this.outputBoundary.prepareFailView(NO_HAND);
        }
        else if (game.getPlayerToAct() != match.getHuman()) {
            this.outputBoundary.prepareFailView(NOT_YOUR_TURN);
        }
        else {
            String problem = problemWith(game, inputData);
            if (problem == null) {
                applyAndReport(match, game, Action.of(typeOf(inputData), inputData.getAmount()));
            }
            else {
                this.outputBoundary.prepareFailView(problem);
            }
        }
    }

    /**
     * Returns the hand being played, or null if no hand is in progress.
     */
    private static Game handInProgress(Match match) {
        Game game = null;
        if (match != null && match.getCurrentGame() != null && !match.getCurrentGame().isHandOver()) {
            game = match.getCurrentGame();
        }
        return game;
    }

    /**
     * Returns the kind of action that was asked for, or null if the text names
     * no kind of action. Upper and lower case and spaces around the name make
     * no difference.
     */
    private static ActionType typeOf(PlayerActionInputData inputData) {
        String name = inputData.getActionType().trim();
        ActionType found = null;
        for (ActionType type : ActionType.values()) {
            if (type.name().equalsIgnoreCase(name)) {
                found = type;
            }
        }
        return found;
    }

    /**
     * Returns why the human may not take the requested action now, as a
     * sentence for the user, or null if the action is legal.
     */
    private static String problemWith(Game game, PlayerActionInputData inputData) {
        ActionType type = typeOf(inputData);
        List<ActionType> legal = game.getLegalActionTypes();
        String problem = null;
        if (type == null) {
            problem = String.format(Locale.ROOT, UNKNOWN_ACTION, inputData.getActionType());
        }
        else if (!legal.contains(type)) {
            problem = notAllowed(type, game, legal);
        }
        else if (isWager(type)) {
            problem = wrongAmount(type, game, inputData.getAmount());
        }
        return problem;
    }

    /**
     * Says why a kind of action is not allowed now. A fold is always allowed,
     * so the kind is a check, a call, a bet, or a raise. The amount to call is
     * what a call costs the human, which is never more than the human has left.
     */
    private static String notAllowed(ActionType type, Game game, List<ActionType> legal) {
        String problem;
        if (type == ActionType.CHECK) {
            int toCall = Math.min(game.getAmountToCall(), game.getPlayerToAct().getChips());
            problem = String.format(Locale.ROOT, CANNOT_CHECK, toCall);
        }
        else if (type == ActionType.CALL) {
            problem = NOTHING_TO_CALL;
        }
        else if (legal.contains(ActionType.BET)) {
            problem = BET_INSTEAD;
        }
        else if (legal.contains(ActionType.RAISE)) {
            problem = RAISE_INSTEAD;
        }
        else {
            problem = String.format(Locale.ROOT, CANNOT_WAGER, nameOf(type));
        }
        return problem;
    }

    /**
     * Says what the allowed totals of a bet or a raise are if the given total
     * is not one of them, and returns null if it is.
     */
    private static String wrongAmount(ActionType type, Game game, int amount) {
        int smallest = game.getMinWager();
        int largest = game.getMaxWager();
        String problem = null;
        if (amount < smallest || amount > largest) {
            if (smallest == largest) {
                problem = String.format(Locale.ROOT, WAGER_EXACT, nameOf(type), largest);
            }
            else {
                problem = String.format(Locale.ROOT, WAGER_RANGE, nameOf(type), smallest, largest);
            }
        }
        return problem;
    }

    private static boolean isWager(ActionType type) {
        return type == ActionType.BET || type == ActionType.RAISE;
    }

    /**
     * Returns the name of a kind of action as it is written in a sentence. The
     * same letters come out whatever the language of the computer: in the
     * Turkish locale "RAISE" would otherwise lose the dot on its i.
     */
    private static String nameOf(ActionType type) {
        return type.name().toLowerCase(Locale.ROOT);
    }

    /**
     * Applies a legal action and reports the table with the events the action
     * caused. If the action ended the hand, the hand goes into the history
     * first; if that fails, the table is still reported and the failure after it.
     */
    private void applyAndReport(Match match, Game game, Action action) {
        int eventsBefore = game.getEvents().size();
        game.apply(action);
        boolean saved = true;
        if (game.isHandOver()) {
            saved = save(game);
        }
        this.outputBoundary.prepareSuccessView(
                this.reporter.report(match, eventsBefore, TableOutputData.NONE));
        if (!saved) {
            this.outputBoundary.prepareFailView(NOT_SAVED);
        }
    }

    /**
     * Keeps the record of the finished hand and returns whether that worked.
     * The game has already moved on when the record is saved, so a history
     * that cannot be written must not stop the table from being reported.
     */
    private boolean save(Game game) {
        boolean saved = true;
        try {
            this.historyDataAccess.save(game.toRecord(this.idSupplier.get(), this.clock.instant()));
        }
        catch (RuntimeException exception) {
            saved = false;
        }
        return saved;
    }
}
