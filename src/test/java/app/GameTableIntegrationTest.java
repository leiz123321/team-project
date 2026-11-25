package app;

import java.awt.Component;
import java.awt.Container;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.FutureTask;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BooleanSupplier;
import java.util.stream.Stream;

import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import data_access.FileHandHistoryDataAccessObject;
import entity.Action;
import entity.ActionType;
import entity.HandRecord;
import view.GameTableView;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GameTableIntegrationTest {
    @TempDir
    Path temp;

    @Test
    void invalidRaiseThenCallReachesFlopAndFoldIsSaved() throws Exception {
        Path historyFile = temp.resolve("hands.json");
        AtomicInteger computerActions = new AtomicInteger();
        GameTableView table = onEdt(() -> AppBuilder.createTable(historyFile, (context, profile) -> {
            assertFalse(SwingUtilities.isEventDispatchThread());
            computerActions.incrementAndGet();
            return context.isLegal(ActionType.CHECK) ? Action.check() : Action.call();
        }));
        click(table, "New Hand");
        await(() -> button(table, "Raise").isEnabled(), "human to act after the blinds");
        onEdt(() -> {
            assertTrue(hasLabel(table, "Pot:   15"));
            assertTrue(hasLabel(table, "Computer:   ■   ■"));
            assertFalse(button(table, "Check").isEnabled());
            assertFalse(button(table, "New Hand").isEnabled());
            field(table).setText("0");
            button(table, "Raise").doClick(0);
            return null;
        });
        await(() -> hasLabel(table, "must be between") && button(table, "Call 5").isEnabled(),
                "presentation of the invalid raise");
        assertEquals(0, computerActions.get());
        assertTrue(onEdt(() -> hasLabel(table, "Pot:   15")));
        click(table, "Call 5");
        // The computer checks twice: once to close preflop, then first to act on the flop.
        await(() -> hasLabel(table, "FLOP") && button(table, "Check").isEnabled(), "human turn on the flop");
        assertEquals(2, computerActions.get());
        assertTrue(onEdt(() -> hasLabel(table, "Pot:   20")));
        click(table, "Fold");
        await(() -> button(table, "New Hand").isEnabled() && hasLabel(table, "Hand complete"), "hand settlement");

        List<HandRecord> records = new FileHandHistoryDataAccessObject(historyFile).loadAll();
        assertEquals(1, records.size());
        assertEquals(3, records.get(0).getBoardCards().size());
        assertEquals(List.of(-10, 10), records.get(0).getChipChanges());
        click(table, "New Hand");
        await(() -> hasLabel(table, "Hand 2") && button(table, "Check").isEnabled(), "second-hand blind rotation");
        assertEquals(3, computerActions.get());
    }

    private static void click(GameTableView table, String label) throws Exception {
        onEdt(() -> {
            JButton button = button(table, label);
            assertTrue(button.isEnabled(), label);
            button.doClick(0);
            return null;
        });
    }

    private static JButton button(GameTableView table, String label) {
        return contents(table).filter(JButton.class::isInstance).map(JButton.class::cast)
                .filter(button -> label.equals(button.getText())).findFirst().orElseThrow();
    }

    private static JTextField field(GameTableView table) {
        return contents(table).filter(JTextField.class::isInstance).map(JTextField.class::cast)
                .findFirst().orElseThrow();
    }

    private static boolean hasLabel(GameTableView table, String text) {
        return contents(table).filter(JLabel.class::isInstance).map(JLabel.class::cast)
                .anyMatch(label -> label.getText().contains(text));
    }

    private static Stream<Component> contents(Component root) {
        if (root instanceof Container) {
            return Stream.concat(Stream.of(root), Stream.of(((Container) root).getComponents())
                    .flatMap(GameTableIntegrationTest::contents));
        }
        return Stream.of(root);
    }

    private static void await(BooleanSupplier condition, String description) throws Exception {
        long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(30);
        while (!onEdt(condition::getAsBoolean)) {
            assertTrue(System.nanoTime() < deadline, description);
            Thread.sleep(20);
        }
    }

    private static <T> T onEdt(Callable<T> task) throws Exception {
        FutureTask<T> result = new FutureTask<>(task);
        SwingUtilities.invokeAndWait(result);
        return result.get();
    }
}
