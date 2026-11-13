package entity;

import java.time.Instant;
import java.util.List;

/**
 * An immutable record of a finished hand, as kept in the history. Cards are
 * stored as text such as "A♠", so the record does not change when the cards
 * are dealt again in a later hand.
 */
public final class HandRecord {
    private final String id;
    private final Instant playedAt;
    private final List<String> playerNames;
    private final List<String> holeCards;
    private final List<String> boardCards;
    private final List<String> actions;
    private final String winnerName;
    private final List<Integer> chipChanges;

    /**
     * Creates a record. The lists about players are in the same order: the
     * small blind first, then the big blind.
     *
     * @param id an identifier for the record
     * @param playedAt when the hand was played
     * @param playerNames the players' names
     * @param holeCards each player's hole cards as text
     * @param boardCards the community cards that were revealed, as text
     * @param actions everything that happened in the hand, in order
     * @param winnerName the winner's name, or null if the pot was split
     * @param chipChanges how many chips each player won (positive) or lost (negative)
     * @throws IllegalArgumentException if a required argument is null or blank,
     *         or the lists about players do not have the same size
     */
    public HandRecord(String id, Instant playedAt, List<String> playerNames, List<String> holeCards,
                      List<String> boardCards, List<String> actions, String winnerName,
                      List<Integer> chipChanges) {
        if (id == null || id.isBlank() || playedAt == null) {
            throw new IllegalArgumentException("id and playedAt are required");
        }
        if (playerNames == null || holeCards == null || boardCards == null
                || actions == null || chipChanges == null) {
            throw new IllegalArgumentException("lists must not be null");
        }
        if (playerNames.size() != holeCards.size() || playerNames.size() != chipChanges.size()) {
            throw new IllegalArgumentException("the lists about players must have the same size");
        }
        this.id = id;
        this.playedAt = playedAt;
        this.playerNames = List.copyOf(playerNames);
        this.holeCards = List.copyOf(holeCards);
        this.boardCards = List.copyOf(boardCards);
        this.actions = List.copyOf(actions);
        this.winnerName = winnerName;
        this.chipChanges = List.copyOf(chipChanges);
    }

    public String getId() {
        return this.id;
    }

    public Instant getPlayedAt() {
        return this.playedAt;
    }

    public List<String> getPlayerNames() {
        return this.playerNames;
    }

    public List<String> getHoleCards() {
        return this.holeCards;
    }

    public List<String> getBoardCards() {
        return this.boardCards;
    }

    public List<String> getActions() {
        return this.actions;
    }

    /**
     * Returns the winner's name, or null if the pot was split.
     */
    public String getWinnerName() {
        return this.winnerName;
    }

    public List<Integer> getChipChanges() {
        return this.chipChanges;
    }
}
