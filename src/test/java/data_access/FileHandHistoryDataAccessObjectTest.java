package data_access;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.stream.Collectors;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import entity.HandRecord;

class FileHandHistoryDataAccessObjectTest {
    private static final Instant PLAYED_AT = Instant.parse("2026-09-01T18:33:12Z");

    @TempDir
    Path folder;
    private Path file;
    private FileHandHistoryDataAccessObject history;

    @BeforeEach
    void setUp() {
        file = folder.resolve("hand_history.json");
        history = new FileHandHistoryDataAccessObject(file);
    }

    private static HandRecord wonHand(String id) {
        return new HandRecord(id, PLAYED_AT, List.of("You", "Opponent"), List.of("A♠ K♠", "7♣ 2♦"),
                List.of("Q♠", "J♠", "10♥", "3♦", "4♣"),
                List.of("PREFLOP: You posts blind 5", "PREFLOP: Opponent posts blind 10", "PREFLOP: You CALL",
                        "PREFLOP: Opponent CHECK", "SHOWDOWN: You wins 20"),
                "You", List.of(10, -10));
    }

    private static HandRecord splitHand(String id) {
        return new HandRecord(id, PLAYED_AT.plusSeconds(90), List.of("Opponent", "You"),
                List.of("2♥ 3♠", "2♣ 3♦"), List.of("A♠", "K♠", "Q♦", "J♣", "10♥"),
                List.of("RIVER: split pot 41"), null, List.of(1, -1));
    }

    private static void assertSameRecord(HandRecord expected, HandRecord actual) {
        assertEquals(expected.getId(), actual.getId());
        assertEquals(expected.getPlayedAt(), actual.getPlayedAt());
        assertEquals(expected.getPlayerNames(), actual.getPlayerNames());
        assertEquals(expected.getHoleCards(), actual.getHoleCards());
        assertEquals(expected.getBoardCards(), actual.getBoardCards());
        assertEquals(expected.getActions(), actual.getActions());
        assertEquals(expected.getWinnerName(), actual.getWinnerName());
        assertEquals(expected.getChipChanges(), actual.getChipChanges());
    }

    private static List<String> idsOf(List<HandRecord> records) {
        return records.stream().map(HandRecord::getId).collect(Collectors.toList());
    }

    @Test
    void recordsComeBackUnchangedAndOldestFirstAfterARestart() {
        List<HandRecord> saved = List.of(wonHand("first"), splitHand("second"), wonHand("third"));
        for (HandRecord record : saved) {
            this.history.save(record);
        }

        List<HandRecord> loaded = new FileHandHistoryDataAccessObject(this.file).loadAll();

        assertEquals(saved.size(), loaded.size());
        for (int i = 0; i < saved.size(); i++) {
            assertSameRecord(saved.get(i), loaded.get(i));
        }
    }

    @Test
    void aSplitPotStillHasNoWinnerAfterARoundTrip() {
        this.history.save(splitHand("split"));

        assertNull(new FileHandHistoryDataAccessObject(this.file).loadAll().get(0).getWinnerName());
    }

    @Test
    void aFailedSavePreservesTheFileAndTheQueuedHand() throws IOException {
        Files.writeString(file, "unreadable history");
        assertThrows(UncheckedIOException.class, () -> history.save(wonHand("first")));
        assertEquals("unreadable history", Files.readString(file));

        Files.writeString(file, "[]");
        history.save(wonHand("second"));
        assertEquals(List.of("first", "second"), idsOf(new FileHandHistoryDataAccessObject(file).loadAll()));
    }

    @Test
    void simultaneousTablesAppendEveryFinishedHand() throws Exception {
        ExecutorService pool = Executors.newFixedThreadPool(4);
        try {
            List<Future<?>> writes = new ArrayList<>();
            Set<String> expected = new HashSet<>();
            for (int i = 0; i < 12; i++) {
                String id = "hand-" + i;
                expected.add(id);
                writes.add(pool.submit(() -> new FileHandHistoryDataAccessObject(file).save(wonHand(id))));
            }
            for (Future<?> write : writes) {
                write.get();
            }
            List<String> actual = idsOf(history.loadAll());
            assertEquals(expected.size(), actual.size());
            assertEquals(expected, new HashSet<>(actual));
        }
        finally {
            pool.shutdownNow();
        }
    }
}
