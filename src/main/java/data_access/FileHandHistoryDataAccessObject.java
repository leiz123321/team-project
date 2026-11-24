package data_access;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import entity.HandRecord;
import use_case.opponent_action.OpponentActionHistoryDataAccessInterface;
import use_case.player_action.PlayerActionHistoryDataAccessInterface;

/**
 * Keeps the records of finished hands in a file, so that the history is still
 * there after the program restarts. The file holds one JSON array with one
 * object per hand, oldest first, written as UTF-8.
 *
 * <p>Append operations acquire a file lock so two running games can share
 * the same history. An unreadable history is left in place. If saving fails,
 * the game can report the error and retry the queued hands on the next save.
 */
public class FileHandHistoryDataAccessObject implements PlayerActionHistoryDataAccessInterface,
        OpponentActionHistoryDataAccessInterface {
    private static final Object WRITER_LOCK = new Object();
    private static final String TEMP_SUFFIX = ".tmp";
    private static final int INDENT = 2;

    private static final String ID = "id";
    private static final String PLAYED_AT = "playedAt";
    private static final String PLAYER_NAMES = "playerNames";
    private static final String HOLE_CARDS = "holeCards";
    private static final String BOARD_CARDS = "boardCards";
    private static final String ACTIONS = "actions";
    private static final String WINNER_NAME = "winnerName";
    private static final String CHIP_CHANGES = "chipChanges";

    private final Path file;
    private final List<HandRecord> unsaved = new ArrayList<>();

    /**
     * Creates the history kept in the given file. Nothing is read or written
     * yet; the file and its folder are created when the first hand is saved.
     *
     * @param file the file that holds the history, for example
     *        {@code data/hand_history.json}
     * @throws IllegalArgumentException if file is null or names no file
     */
    public FileHandHistoryDataAccessObject(Path file) {
        if (file == null || file.getFileName() == null) {
            throw new IllegalArgumentException("file must name a file");
        }
        this.file = file.toAbsolutePath();
    }

    /**
     * Adds the record of a finished hand to the end of the history. If the
     * file cannot be written, the record is kept in memory and written with the next
     * record that is saved, and the failure is reported by an exception. The
     * record must not be saved again after that exception, or it would be
     * listed twice.
     *
     * @param record the record to keep
     * @throws IllegalArgumentException if record is null
     * @throws UncheckedIOException if the file cannot be written; the record
     *         is then only in memory, and it is lost if the program ends
     *         before a later save succeeds
     */
    @Override
    public synchronized void save(HandRecord record) {
        if (record == null) {
            throw new IllegalArgumentException("record must not be null");
        }
        this.unsaved.add(record);
        try {
            Files.createDirectories(this.file.getParent());
            Path lockFile = this.file.resolveSibling(this.file.getFileName() + ".lock");
            // FileChannel locks serialize processes. The monitor also serializes
            // different store instances in this JVM, where overlapping locks throw.
            synchronized (WRITER_LOCK) {
                try (FileChannel channel = FileChannel.open(lockFile, StandardOpenOption.CREATE,
                        StandardOpenOption.WRITE);
                     FileLock lock = channel.lock()) {
                    List<HandRecord> records = read();
                    records.addAll(this.unsaved);
                    write(records);
                    this.unsaved.clear();
                }
            }
        }
        catch (IOException exception) {
            throw new UncheckedIOException("Could not save the hand to " + this.file, exception);
        }
    }

    public synchronized List<HandRecord> loadAll() {
        try {
            List<HandRecord> records = read();
            records.addAll(this.unsaved);
            return records;
        }
        catch (IOException exception) {
            throw new UncheckedIOException("Could not read hand history from " + this.file, exception);
        }
    }

    private List<HandRecord> read() throws IOException {
        try {
            String text = Files.readString(this.file, StandardCharsets.UTF_8);
            return text.isBlank() ? new ArrayList<>() : recordsOf(new JSONArray(text));
        }
        catch (NoSuchFileException exception) {
            return new ArrayList<>();
        }
        catch (RuntimeException exception) {
            throw new IOException("Invalid hand history", exception);
        }
    }

    private void write(List<HandRecord> records) throws IOException {
        JSONArray array = new JSONArray();
        for (HandRecord record : records) {
            array.put(jsonOf(record));
        }
        Path folder = this.file.getParent();
        Path temp = Files.createTempFile(folder, "hand-history-", TEMP_SUFFIX);
        try {
            Files.writeString(temp, array.toString(INDENT), StandardCharsets.UTF_8);
            try {
                Files.move(temp, this.file, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
            }
            catch (AtomicMoveNotSupportedException exception) {
                Files.move(temp, this.file, StandardCopyOption.REPLACE_EXISTING);
            }
        }
        finally {
            Files.deleteIfExists(temp);
        }
    }

    private static List<HandRecord> recordsOf(JSONArray array) {
        List<HandRecord> records = new ArrayList<>();
        for (int i = 0; i < array.length(); i++) {
            records.add(recordOf(array.getJSONObject(i)));
        }
        return records;
    }

    private static HandRecord recordOf(JSONObject json) {
        List<String> playerNames = textsOf(json.getJSONArray(PLAYER_NAMES));
        if (playerNames.isEmpty()) {
            throw new JSONException("a hand has at least one player");
        }
        List<Integer> chipChanges = new ArrayList<>();
        JSONArray changes = json.getJSONArray(CHIP_CHANGES);
        for (int i = 0; i < changes.length(); i++) {
            chipChanges.add(changes.getInt(i));
        }
        String winnerName = null;
        if (!json.isNull(WINNER_NAME)) {
            winnerName = json.getString(WINNER_NAME);
        }
        return new HandRecord(json.getString(ID), Instant.parse(json.getString(PLAYED_AT)), playerNames,
                textsOf(json.getJSONArray(HOLE_CARDS)), textsOf(json.getJSONArray(BOARD_CARDS)),
                textsOf(json.getJSONArray(ACTIONS)), winnerName, chipChanges);
    }

    private static List<String> textsOf(JSONArray array) {
        List<String> texts = new ArrayList<>();
        for (int i = 0; i < array.length(); i++) {
            texts.add(array.getString(i));
        }
        return texts;
    }

    private static JSONObject jsonOf(HandRecord record) {
        JSONObject json = new JSONObject();
        json.put(ID, record.getId());
        json.put(PLAYED_AT, record.getPlayedAt().toString());
        json.put(PLAYER_NAMES, new JSONArray(record.getPlayerNames()));
        json.put(HOLE_CARDS, new JSONArray(record.getHoleCards()));
        json.put(BOARD_CARDS, new JSONArray(record.getBoardCards()));
        json.put(ACTIONS, new JSONArray(record.getActions()));
        if (record.getWinnerName() == null) {
            // A split pot has no winner.
            json.put(WINNER_NAME, JSONObject.NULL);
        }
        else {
            json.put(WINNER_NAME, record.getWinnerName());
        }
        json.put(CHIP_CHANGES, new JSONArray(record.getChipChanges()));
        return json;
    }
}
