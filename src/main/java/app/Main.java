package app;

import javax.swing.JFrame;
import javax.swing.SwingUtilities;
import javax.swing.WindowConstants;

/** Opens the heads-up poker table. */
public class Main {
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            JFrame frame = new JFrame("Heads-up poker");
            frame.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
            frame.setContentPane(AppBuilder.createTable());
            frame.pack();
            frame.setMinimumSize(frame.getSize());
            frame.setLocationByPlatform(true);
            frame.setVisible(true);
        });
    }
}
