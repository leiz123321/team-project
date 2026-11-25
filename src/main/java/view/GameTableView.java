package view;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridLayout;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;

import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import javax.swing.SwingWorker;

import interface_adapter.opponent_action.OpponentActionController;
import interface_adapter.player_action.PlayerActionController;
import interface_adapter.start_hand.StartHandController;
import interface_adapter.table.CardIds;
import interface_adapter.table.TableEventView;
import interface_adapter.table.TableState;
import interface_adapter.table.TableViewModel;

/** A two-seat table. Presenters supply the cards, betting limits and turn order. */
public class GameTableView extends JPanel {
    private final PlayerActionController player;
    private final OpponentActionController opponent;
    private final JButton deal = new JButton("New Hand");
    private final JButton retry = new JButton("Retry computer turn");
    private final JButton fold = new JButton("Fold");
    private final JButton check = new JButton("Check");
    private final JButton call = new JButton("Call");
    private final JButton bet = new JButton("Bet");
    private final JButton raise = new JButton("Raise");
    private final JTextField amount = new JTextField(5);
    private final JLabel wagerLabel = new JLabel("Round total");
    private final JLabel heading = new JLabel();
    private final JLabel turn = new JLabel();
    private final JLabel yourSeat = new JLabel();
    private final JLabel otherSeat = new JLabel();
    private final JLabel board = new JLabel();
    private final JLabel pot = new JLabel();
    private final JLabel message = new JLabel();
    private final JLabel feedback = new JLabel(" ");
    private final JTextArea history = new JTextArea(6, 44);
    private TableState table;
    private boolean busy;

    public GameTableView(TableViewModel model, StartHandController start,
                         PlayerActionController player, OpponentActionController opponent) {
        super(new BorderLayout(10, 10));
        this.player = player;
        this.opponent = opponent;
        this.table = model.getState();
        setBorder(BorderFactory.createEmptyBorder(16, 18, 14, 18));

        JPanel top = new JPanel(new BorderLayout(16, 0));
        heading.setFont(heading.getFont().deriveFont(Font.BOLD, 20f));
        top.add(heading, BorderLayout.WEST);
        top.add(deal, BorderLayout.EAST);
        add(top, BorderLayout.NORTH);

        JPanel center = new JPanel(new BorderLayout(8, 8));
        JPanel cards = new JPanel(new GridLayout(0, 1, 0, 4));
        cards.add(otherSeat);
        cards.add(board);
        cards.add(pot);
        cards.add(yourSeat);
        center.add(cards, BorderLayout.NORTH);
        history.setEditable(false);
        history.setLineWrap(true);
        history.setWrapStyleWord(true);
        JScrollPane scroll = new JScrollPane(history);
        scroll.setBorder(BorderFactory.createTitledBorder("This hand"));
        center.add(scroll, BorderLayout.CENTER);
        add(center, BorderLayout.CENTER);

        JPanel controls = new JPanel();
        controls.setLayout(new BoxLayout(controls, BoxLayout.Y_AXIS));
        controls.add(turn);
        controls.add(message);
        controls.add(feedback);
        controls.add(row(fold, check, call, retry));
        controls.add(row(wagerLabel, amount, bet, raise));
        wagerLabel.setLabelFor(amount);
        add(controls, BorderLayout.SOUTH);

        deal.addActionListener(event -> {
            boolean newMatch = table.getHandNumber() == 0 || table.isMatchOver();
            runAction(() -> start.execute(newMatch));
        });
        fold.addActionListener(event -> runAction(player::fold));
        check.addActionListener(event -> runAction(player::check));
        call.addActionListener(event -> runAction(player::call));
        bet.addActionListener(event -> submitWager(PlayerActionController.BET));
        raise.addActionListener(event -> submitWager(PlayerActionController.RAISE));
        retry.addActionListener(event -> runAction(opponent::execute));

        model.addPropertyChangeListener(event -> {
            TableState next = model.getState();
            if (SwingUtilities.isEventDispatchThread()) {
                receive(next);
            }
            else {
                SwingUtilities.invokeLater(() -> receive(next));
            }
        });
        receive(table);
    }

    private static JPanel row(java.awt.Component... components) {
        JPanel row = new JPanel(new FlowLayout(FlowLayout.LEADING, 5, 2));
        row.setAlignmentX(LEFT_ALIGNMENT);
        for (java.awt.Component component : components) {
            row.add(component);
        }
        return row;
    }

    private void receive(TableState next) {
        table = next;
        List<TableEventView> events = next.getEvents();
        if (!events.isEmpty() && TableEventView.BLIND.equals(events.get(0).getType())) {
            history.setText("");
        }
        for (TableEventView event : events) {
            history.append(event.getText() + "\n");
        }
        history.setCaretPosition(history.getDocument().getLength());
        feedback.setText(next.getErrorMessage().isEmpty() ? " " : next.getErrorMessage());
        if (next.getErrorMessage().isEmpty() && (next.canBet() || next.canRaise())) {
            amount.setText(Integer.toString(next.getMinWager()));
        }
        refresh();
    }

    private void refresh() {
        boolean dealt = table.getHandNumber() > 0;
        heading.setText(dealt ? "Hand " + table.getHandNumber() + "  ·  " + table.getStreet() : "Heads-up poker");
        otherSeat.setText(seat("Computer", table.getOpponentCards(), table.getOpponentChips(), table.getOpponentBet()));
        yourSeat.setText(seat("You", table.getHumanCards(), table.getHumanChips(), table.getHumanBet()));
        board.setText("Community cards:   " + cards(table.getBoardCards()));
        int totalPot = table.getPot() + table.getHumanBet() + table.getOpponentBet();
        pot.setText(dealt ? "Pot:   " + totalPot : "Pot:   —");
        turn.setText(turnDescription());
        message.setText(table.getMessage().isEmpty() ? " " : table.getMessage());
        deal.setText(table.isMatchOver() ? "New Match" : "New Hand");
        deal.setEnabled(!busy && !table.isHandInProgress());
        boolean acting = !busy && table.isHandInProgress() && table.isHumanToAct();
        fold.setEnabled(acting && table.canFold());
        check.setEnabled(acting && table.canCheck());
        call.setEnabled(acting && table.canCall());
        call.setText(table.canCall() ? "Call " + table.getCallAmount() : "Call");
        bet.setEnabled(acting && table.canBet());
        raise.setEnabled(acting && table.canRaise());
        amount.setEnabled(bet.isEnabled() || raise.isEnabled());
        String range = table.getMinWager() + "–" + table.getMaxWager();
        wagerLabel.setText(amount.isEnabled() ? "Round total (" + range + ")" : "Round total");
        retry.setVisible(!busy && table.isOpponentToAct() && !feedback.getText().isBlank());
    }

    private String turnDescription() {
        if (table.isMatchOver()) {
            return table.getHumanChips() > table.getOpponentChips() ? "Match over — you win." : "Match over — computer wins.";
        }
        if (table.isHandOver()) {
            return "Hand complete";
        }
        if (table.isHumanToAct()) {
            return "Your turn";
        }
        if (table.isOpponentToAct()) {
            return "Computer’s turn";
        }
        return "Choose New Hand to deal.";
    }

    private String seat(String name, List<String> holeCards, int stack, int committed) {
        if (table.getHandNumber() == 0) {
            return name + ":   —";
        }
        return name + ":   " + cards(holeCards) + "     Stack: " + stack + "     In this round: " + committed;
    }

    private static String cards(List<String> ids) {
        return ids.isEmpty() ? "—" : ids.stream().map(GameTableView::card).collect(Collectors.joining("   "));
    }

    private static String card(String id) {
        if (CardIds.BACK.equals(id)) {
            return "■";
        }
        int end = id.length() - 1;
        return id.substring(0, end) + "♠♥♦♣".charAt("SHDC".indexOf(id.charAt(end)));
    }

    private void submitWager(String action) {
        try {
            int total = Integer.parseInt(amount.getText().trim());
            runAction(() -> player.execute(action, total));
        }
        catch (NumberFormatException exception) {
            feedback.setText("Enter a whole number of chips.");
            amount.requestFocusInWindow();
        }
    }

    // A single command owns the session until its presenter has finished publishing.
    private void runAction(Runnable command) {
        if (busy) {
            return;
        }
        busy = true;
        feedback.setText(" ");
        refresh();
        new SwingWorker<Void, Void>() {
            @Override
            protected Void doInBackground() {
                command.run();
                return null;
            }

            @Override
            protected void done() {
                try {
                    get();
                }
                catch (InterruptedException exception) {
                    Thread.currentThread().interrupt();
                    feedback.setText("The action was interrupted.");
                }
                catch (ExecutionException exception) {
                    feedback.setText("Could not complete the action: " + exception.getCause().getMessage());
                }
                busy = false;
                refresh();
                if (table.isOpponentToAct() && feedback.getText().isBlank()) {
                    runAction(opponent::execute);
                }
            }
        }.execute();
    }
}
