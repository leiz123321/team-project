package use_case.opponent_action;

import java.time.Clock;
import java.util.Random;
import java.util.function.Supplier;

import entity.Action;
import entity.BotProfile;
import entity.BotStrategy;
import entity.DecisionContext;
import entity.EquityCalculator;
import entity.EquityResult;
import entity.Game;
import entity.Match;
import entity.Player;
import entity.strategy.BotStrategyFactory;
import use_case.table.TableOutputData;
import use_case.table.TableReporter;

/**
 * Handles the opponent's turn in the betting flow: gets the decision, applies it to the hand in progress and, when that ends
 * the hand, keeps a record of the hand in the history.
 *
 * <p>The decision is asked of an outside source first, such as an LLM. That
 * source is given only what the opponent can see: its own hole cards, the
 * revealed community cards, the betting, and a chance of winning worked out
 * from those cards alone. Its answer is not trusted. When it gives none, or
 * one that is not legal, the built-in strategy for the opponent's profile
 * decides instead; and if even that were to go wrong, the opponent checks
 * when it can and calls when it cannot, so the game always goes on.
 *
 * <p>The outside source may take seconds to answer. An answer that comes back
 * after the hand has moved on, because a new match was started or the turn
 * was already played, is not played: it was made for a table that is gone.
 */
public class OpponentActionInteractor implements OpponentActionInputBoundary {
    private static final String NO_HAND = "There is no hand in progress.";
    private static final String NOT_OPPONENTS_TURN = "It is not the opponent's turn.";
    private static final String HAND_MOVED_ON =
            "The hand moved on while the opponent was deciding, so its move was not played.";
    private static final String NOT_SAVED = "The hand is over, but it could not be saved to the history.";
    private static final double DEFAULT_AGGRESSION = 0.5;

    private final OpponentActionGameDataAccessInterface gameDataAccess;
    private final OpponentActionHistoryDataAccessInterface historyDataAccess;
    private final OpponentActionProfileDataAccessInterface profileDataAccess;
    private final OpponentActionDecisionDataAccessInterface decisionDataAccess;
    private final OpponentActionOutputBoundary outputBoundary;
    private final Random random;
    private final Supplier<String> idSupplier;
    private final Clock clock;
    private final EquityCalculator calculator;
    private final TableReporter reporter;

    /**
     * Creates the interactor.
     *
     * @param gameDataAccess where the match being played is kept
     * @param historyDataAccess where the record of a finished hand is kept
     * @param profileDataAccess where the opponent's playing style is kept
     * @param decisionDataAccess the outside source of the opponent's decisions
     * @param outputBoundary where the result is reported
     * @param random the source of randomness for the built-in strategy; pass a
     *        seeded one in tests
     * @param idSupplier gives a new identifier for each hand record, for
     *        example a random UUID as text
     * @param clock tells the time a hand finished; pass a fixed one in tests
     * @throws IllegalArgumentException if an argument is null
     */
    public OpponentActionInteractor(OpponentActionGameDataAccessInterface gameDataAccess,
                                    OpponentActionHistoryDataAccessInterface historyDataAccess,
                                    OpponentActionProfileDataAccessInterface profileDataAccess,
                                    OpponentActionDecisionDataAccessInterface decisionDataAccess,
                                    OpponentActionOutputBoundary outputBoundary,
                                    Random random, Supplier<String> idSupplier, Clock clock) {
        this(gameDataAccess, historyDataAccess, profileDataAccess, decisionDataAccess, outputBoundary,
                random, idSupplier, clock, new EquityCalculator());
    }

    /**
     * Creates the interactor with the calculator of chances supplied by the
     * caller. The calculator works out both the opponent's own chance of
     * winning, which goes into its decision, and the human's chances for the
     * reported table.
     *
     * @param gameDataAccess where the match being played is kept
     * @param historyDataAccess where the record of a finished hand is kept
     * @param profileDataAccess where the opponent's playing style is kept
     * @param decisionDataAccess the outside source of the opponent's decisions
     * @param outputBoundary where the result is reported
     * @param random the source of randomness for the built-in strategy
     * @param idSupplier gives a new identifier for each hand record
     * @param clock tells the time a hand finished
     * @param calculator the calculator of chances
     * @throws IllegalArgumentException if an argument is null
     */
    public OpponentActionInteractor(OpponentActionGameDataAccessInterface gameDataAccess,
                                    OpponentActionHistoryDataAccessInterface historyDataAccess,
                                    OpponentActionProfileDataAccessInterface profileDataAccess,
                                    OpponentActionDecisionDataAccessInterface decisionDataAccess,
                                    OpponentActionOutputBoundary outputBoundary,
                                    Random random, Supplier<String> idSupplier, Clock clock,
                                    EquityCalculator calculator) {
        if (gameDataAccess == null || historyDataAccess == null || profileDataAccess == null
                || decisionDataAccess == null || outputBoundary == null) {
            throw new IllegalArgumentException("the data access objects and the output boundary must not be null");
        }
        if (random == null || idSupplier == null || clock == null) {
            throw new IllegalArgumentException("random, the id supplier, and the clock must not be null");
        }
        this.gameDataAccess = gameDataAccess;
        this.historyDataAccess = historyDataAccess;
        this.profileDataAccess = profileDataAccess;
        this.decisionDataAccess = decisionDataAccess;
        this.outputBoundary = outputBoundary;
        this.random = random;
        this.idSupplier = idSupplier;
        this.clock = clock;
        this.reporter = new TableReporter(calculator);
        this.calculator = calculator;
    }

    @Override
    public void execute() {
        Match match = this.gameDataAccess.getMatch();
        Game game = handInProgress(match);
        if (game == null) {
            this.outputBoundary.prepareFailView(NO_HAND);
        }
        else if (game.getPlayerToAct() != match.getComputer()) {
            this.outputBoundary.prepareFailView(NOT_OPPONENTS_TURN);
        }
        else {
            takeTurn(match, game);
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
     * Decides the opponent's action and plays it, unless the hand has moved
     * on by the time the outside source answers.
     */
    private void takeTurn(Match match, Game game) {
        int eventsBefore = game.getEvents().size();
        Player opponent = match.getComputer();
        BotProfile profile = loadProfile();
        // The opponent's own view of the hand: its cards and the revealed community cards, nothing else.
        double winChance = worthOf(opponent.winChance(game.getBoard(), this.calculator));
        DecisionContext context = game.getDecisionContext(winChance);
        Action answer = askDecisionSource(context, profile);

        if (hasMovedOn(game, eventsBefore)) {
            this.outputBoundary.prepareFailView(HAND_MOVED_ON);
        }
        else {
            String decidedBy = TableOutputData.DECIDED_BY_LLM;
            Action action = answer;
            if (!game.isLegal(action)) {
                decidedBy = TableOutputData.DECIDED_BY_BUILT_IN;
                action = askBuiltInStrategy(context, profile);
            }
            if (!game.isLegal(action)) {
                action = safeAction(game);
            }
            applyAndReport(match, game, action, decidedBy);
        }
    }

    /**
     * Returns the chance of winning to decide by. A tie wins half the pot, so
     * half of the ties count. A hand that cannot lose counts as a sure
     * winner: whatever it does, it gets at least its half of the pot back,
     * so it must never be folded and may be bet like the best hand there is.
     */
    private static double worthOf(EquityResult chances) {
        double worth = chances.getEquity();
        if (chances.getLose() == 0.0) {
            worth = 1.0;
        }
        return worth;
    }

    /**
     * Returns whether the hand is no longer where it was when the decision
     * was asked for: it is not the hand in progress any more, or something
     * has happened in it since. Every action adds at least one event, so an
     * unchanged number of events means that nothing has happened.
     */
    private boolean hasMovedOn(Game game, int eventsBefore) {
        return handInProgress(this.gameDataAccess.getMatch()) != game || game.getEvents().size() != eventsBefore;
    }

    /**
     * Applies a legal action of the opponent and reports the table with the
     * events the action caused. If the action ended the hand, the hand goes
     * into the history first; if that fails, the table is still reported and
     * the failure after it.
     */
    private void applyAndReport(Match match, Game game, Action action, String decidedBy) {
        int eventsBefore = game.getEvents().size();
        game.apply(action);
        boolean saved = true;
        if (game.isHandOver()) {
            saved = save(game);
        }
        this.outputBoundary.prepareSuccessView(this.reporter.report(match, eventsBefore, decidedBy));
        if (!saved) {
            this.outputBoundary.prepareFailView(NOT_SAVED);
        }
    }

    /**
     * Returns the opponent's profile, or the balanced style at the neutral
     * aggression if the stored profile cannot be read.
     */
    private BotProfile loadProfile() {
        BotProfile profile;
        try {
            profile = this.profileDataAccess.load();
        }
        catch (RuntimeException exception) {
            profile = null;
        }
        if (profile == null) {
            profile = new BotProfile(BotStrategyFactory.STYLE_BALANCED, DEFAULT_AGGRESSION);
        }
        return profile;
    }

    /**
     * Returns the outside source's answer, or null if it gave none. The answer
     * may be illegal; the caller checks it.
     */
    private Action askDecisionSource(DecisionContext context, BotProfile profile) {
        Action action;
        try {
            action = this.decisionDataAccess.decide(context, profile);
        }
        catch (OpponentUnavailableException | RuntimeException exception) {
            action = null;
        }
        return action;
    }

    /**
     * Returns the built-in strategy's answer, or null if the strategy failed.
     */
    private Action askBuiltInStrategy(DecisionContext context, BotProfile profile) {
        Action action;
        try {
            action = builtInStrategy(profile).decide(context);
        }
        catch (RuntimeException exception) {
            action = null;
        }
        return action;
    }

    /**
     * Returns the built-in strategy for a profile. It is a method of its own
     * so that a test can put a faulty strategy in its place and check that the
     * game still goes on.
     *
     * @param profile the opponent's playing style
     * @return the strategy that plays that style
     */
    BotStrategy builtInStrategy(BotProfile profile) {
        return BotStrategyFactory.create(profile, this.random);
    }

    /**
     * Returns an action that is always legal: a check when nothing has to be
     * called, and a call otherwise. A fold is the last resort.
     */
    private static Action safeAction(Game game) {
        Action action = Action.fold();
        if (game.isLegal(Action.check())) {
            action = Action.check();
        }
        else if (game.isLegal(Action.call())) {
            action = Action.call();
        }
        return action;
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
