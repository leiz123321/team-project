package use_case.start_hand;

import java.util.Random;

import entity.EquityCalculator;
import entity.Game;
import entity.Match;
import entity.Player;
import use_case.table.TableOutputData;
import use_case.table.TableReporter;

/**
 * Runs the use case "Start a new hand": deals the next hand of the match, or
 * the first hand of a new match.
 *
 * <p>The interactor only deals. It never makes the computer opponent act: when
 * the opponent is first to act, the reported table identifies the opponent as the next
 * player. The betting flow then handles that turn.
 */
public class StartHandInteractor implements StartHandInputBoundary {

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

    private static final String HAND_NOT_FINISHED = "The current hand is not finished.";
    private static final String MATCH_OVER = "The match is over. %s won.";

    private final StartHandGameDataAccessInterface gameDataAccess;
    private final StartHandOutputBoundary outputBoundary;
    private final Random random;
    private final TableReporter reporter;

    /**
     * Creates the interactor.
     *
     * @param gameDataAccess where the match being played is kept
     * @param outputBoundary where the result is reported
     * @param random the source of randomness for the shuffle; pass a seeded
     *        one in tests
     * @throws IllegalArgumentException if an argument is null
     */
    public StartHandInteractor(StartHandGameDataAccessInterface gameDataAccess,
                               StartHandOutputBoundary outputBoundary, Random random) {
        this(gameDataAccess, outputBoundary, random, new EquityCalculator());
    }

    /**
     * Creates the interactor with the calculator of the chances it reports
     * supplied by the caller.
     *
     * @param gameDataAccess where the match being played is kept
     * @param outputBoundary where the result is reported
     * @param random the source of randomness for the shuffle
     * @param calculator works out the human's chances for the reported table
     * @throws IllegalArgumentException if an argument is null
     */
    public StartHandInteractor(StartHandGameDataAccessInterface gameDataAccess,
                               StartHandOutputBoundary outputBoundary, Random random,
                               EquityCalculator calculator) {
        if (gameDataAccess == null || outputBoundary == null || random == null) {
            throw new IllegalArgumentException("gameDataAccess, outputBoundary, and random must not be null");
        }
        this.gameDataAccess = gameDataAccess;
        this.outputBoundary = outputBoundary;
        this.random = random;
        this.reporter = new TableReporter(calculator);
    }

    @Override
    public void execute(StartHandInputData inputData) {
        Match match = this.gameDataAccess.getMatch();
        if (match == null) {
            match = new Match(HUMAN_NAME, OPPONENT_NAME, STARTING_CHIPS, SMALL_BLIND, BIG_BLIND);
            this.gameDataAccess.saveMatch(match);
        }
        else if (inputData.isNewMatch()) {
            // A hand that was being played is abandoned and is not kept in the history.
            match.restart();
        }

        Game current = match.getCurrentGame();
        boolean handInProgress = false;
        int eventsOfLastHand = 0;
        if (current != null) {
            handInProgress = !current.isHandOver();
            eventsOfLastHand = current.getEvents().size();
        }

        if (handInProgress) {
            this.outputBoundary.prepareFailView(HAND_NOT_FINISHED);
        }
        else if (match.isOver()) {
            // Every event of the last hand has been reported already, so this table carries none.
            this.outputBoundary.prepareMatchOverView(
                    this.reporter.report(match, eventsOfLastHand, TableOutputData.NONE));
            this.outputBoundary.prepareFailView(String.format(MATCH_OVER, winnerOf(match).getName()));
        }
        else {
            match.startNextHand(this.random);
            this.outputBoundary.prepareSuccessView(this.reporter.report(match, 0, TableOutputData.NONE));
        }
    }

    /**
     * Returns the winner of a match that is over: the player who can still
     * post the big blind.
     */
    private static Player winnerOf(Match match) {
        Player winner = match.getHuman();
        if (winner.getChips() < match.getBigBlind()) {
            winner = match.getComputer();
        }
        return winner;
    }
}
