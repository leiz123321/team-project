package interface_adapter.table;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Comparisons of table states for the tests of the game's presenters.
 */
public final class TableStateAssertions {
    private static final double EXACT = 0.0;

    private TableStateAssertions() {
    }

    /**
     * Checks that two states say the same about the game: everything but the
     * revision, the error message, and the events is equal.
     */
    public static void assertSameGame(TableState expected, TableState actual) {
        assertEquals(expected.getHandNumber(), actual.getHandNumber(), "handNumber");
        assertEquals(expected.getHumanName(), actual.getHumanName(), "humanName");
        assertEquals(expected.getOpponentName(), actual.getOpponentName(), "opponentName");
        assertEquals(expected.getHumanCards(), actual.getHumanCards(), "humanCards");
        assertEquals(expected.getOpponentCards(), actual.getOpponentCards(), "opponentCards");
        assertEquals(expected.getBoardCards(), actual.getBoardCards(), "boardCards");
        assertEquals(expected.getStreet(), actual.getStreet(), "street");
        assertEquals(expected.getPot(), actual.getPot(), "pot");
        assertEquals(expected.getHumanChips(), actual.getHumanChips(), "humanChips");
        assertEquals(expected.getOpponentChips(), actual.getOpponentChips(), "opponentChips");
        assertEquals(expected.getHumanBet(), actual.getHumanBet(), "humanBet");
        assertEquals(expected.getOpponentBet(), actual.getOpponentBet(), "opponentBet");
        assertEquals(expected.isHumanDealer(), actual.isHumanDealer(), "humanDealer");
        assertEquals(expected.isHandInProgress(), actual.isHandInProgress(), "handInProgress");
        assertEquals(expected.isHandOver(), actual.isHandOver(), "handOver");
        assertEquals(expected.isMatchOver(), actual.isMatchOver(), "matchOver");
        assertEquals(expected.getWinner(), actual.getWinner(), "winner");
        assertEquals(expected.isHumanToAct(), actual.isHumanToAct(), "humanToAct");
        assertEquals(expected.isOpponentToAct(), actual.isOpponentToAct(), "opponentToAct");
        assertEquals(expected.canFold(), actual.canFold(), "canFold");
        assertEquals(expected.canCheck(), actual.canCheck(), "canCheck");
        assertEquals(expected.canCall(), actual.canCall(), "canCall");
        assertEquals(expected.canBet(), actual.canBet(), "canBet");
        assertEquals(expected.canRaise(), actual.canRaise(), "canRaise");
        assertEquals(expected.getCallAmount(), actual.getCallAmount(), "callAmount");
        assertEquals(expected.getMinWager(), actual.getMinWager(), "minWager");
        assertEquals(expected.getMaxWager(), actual.getMaxWager(), "maxWager");
        assertEquals(expected.getWinChance(), actual.getWinChance(), EXACT, "winChance");
        assertEquals(expected.getTieChance(), actual.getTieChance(), EXACT, "tieChance");
        assertEquals(expected.getLoseChance(), actual.getLoseChance(), EXACT, "loseChance");
        assertEquals(expected.getHumanHandName(), actual.getHumanHandName(), "humanHandName");
        assertEquals(expected.getOpponentHandName(), actual.getOpponentHandName(), "opponentHandName");
        assertEquals(expected.getWinningCards(), actual.getWinningCards(), "winningCards");
        assertEquals(expected.getMessage(), actual.getMessage(), "message");
        assertEquals(expected.getDecidedBy(), actual.getDecidedBy(), "decidedBy");
        assertEquals(expected.getHumanChipChange(), actual.getHumanChipChange(), "humanChipChange");
    }

    /**
     * Checks that a state is the state before it republished after a failed
     * request: the next revision, the error message, no events, and nothing
     * else changed.
     */
    public static void assertFailureOf(TableState before, TableState after, String errorMessage) {
        assertEquals(before.getRevision() + 1, after.getRevision(), "revision");
        assertEquals(errorMessage, after.getErrorMessage(), "errorMessage");
        assertEquals(0, after.getEvents().size(), "events");
        assertSameGame(before, after);
    }
}
