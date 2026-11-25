package entity;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Random;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GameSettlementTest {
    private static final String ACES_AGAINST_KINGS = "A♠ A♥ K♠ K♥ 2♦ 7♣ 9♦ J♠ 3♥";
    private static final String KINGS_AGAINST_ACES = "K♠ K♥ A♠ A♥ 2♦ 7♣ 9♦ J♠ 3♥";
    private Player small;
    private Player big;

    private Game deal(int smallChips, int bigChips, String cards) {
        small = new Player("You", smallChips);
        big = new Player("Opponent", bigChips);
        return new Game(small, big, 5, 10, new StackedDeck(cards), new Random(207L));
    }

    @Test
    void aShortAllInCallReturnsTheUnmatchedChipsBeforePayingThePot() {
        Game game = deal(1000, 200, ACES_AGAINST_KINGS);
        game.apply(Action.raise(500));
        game.apply(Action.call());

        List<HandEvent> events = game.getEvents();
        HandEvent refund = events.get(4);
        assertEquals(HandEventType.REFUND, refund.getType());
        assertEquals("You", refund.getPlayerName());
        assertEquals(300, refund.getAmount());
        assertEquals(200, refund.getTotal());
        HandEvent payout = events.get(events.size() - 1);
        assertEquals(HandEventType.WIN, payout.getType());
        assertEquals(400, payout.getAmount());
        assertEquals(1200, small.getChips());
        assertEquals(0, big.getChips());
    }

    @Test
    void theShortStackCanWinOnlyWhatItMatched() {
        Game game = deal(1000, 200, KINGS_AGAINST_ACES);
        game.apply(Action.raise(500));
        game.apply(Action.call());

        assertSame(big, game.getWinner());
        assertEquals(800, small.getChips());
        assertEquals(400, big.getChips());
    }

    @Test
    void anAllInBeforeTheFlopRunsOutTheBoardThenShowsBothHandsThenPays() {
        Game game = deal(1000, 1000, ACES_AGAINST_KINGS);
        game.apply(Action.raise(1000));
        game.apply(Action.call());

        List<HandEvent> events = game.getEvents();
        assertEquals(List.of(HandEventType.BLIND, HandEventType.BLIND, HandEventType.RAISE,
                HandEventType.CALL, HandEventType.FLOP, HandEventType.TURN, HandEventType.RIVER,
                HandEventType.SHOWDOWN, HandEventType.SHOWDOWN, HandEventType.WIN),
                events.stream().map(HandEvent::getType).collect(Collectors.toList()));
        assertEquals(List.of("2♦", "7♣", "9♦"), events.get(4).getCards());
        assertEquals(List.of("J♠"), events.get(5).getCards());
        assertEquals(List.of("3♥"), events.get(6).getCards());
        assertEquals(List.of("A♠", "A♥"), events.get(7).getCards());
        assertEquals(List.of("K♠", "K♥"), events.get(8).getCards());
        assertTrue(game.isHandOver());
        assertEquals(2000, small.getChips());
        assertEquals(0, big.getChips());
    }
}
