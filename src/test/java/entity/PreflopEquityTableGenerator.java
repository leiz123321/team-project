package entity;

import java.util.List;
import java.util.Locale;
import java.util.Random;
import java.util.stream.IntStream;

/**
 * Produces the numbers in {@link PreflopEquityTable}. Run {@code main} and
 * paste the output over the two arrays in that class.
 *
 * <p>Each of the 169 kinds of starting hand is played against a random
 * opponent hand and random community cards a fixed number of times with a
 * fixed seed, so running this again prints the same numbers.
 */
public final class PreflopEquityTableGenerator {
    private static final int RANKS = 13;
    private static final int DEALS_PER_HAND = 20_000_000;
    private static final long SEED = 207L;

    private PreflopEquityTableGenerator() {
    }

    /**
     * Prints the win and tie tables as Java array literals.
     *
     * @param args not used
     */
    public static void main(String[] args) {
        double[][] win = new double[RANKS][RANKS];
        double[][] tie = new double[RANKS][RANKS];
        IntStream.range(0, RANKS * RANKS).parallel().forEach(cell -> {
            int row = cell / RANKS;
            int column = cell % RANKS;
            Rank high = Rank.values()[Math.max(row, column)];
            Rank low = Rank.values()[Math.min(row, column)];
            // Row above column is suited, row below column is offsuit, equal is a pair.
            Suit secondSuit = Suit.HEART;
            if (row > column) {
                secondSuit = Suit.SPADE;
            }
            List<Card> hole = List.of(new Card(high, Suit.SPADE), new Card(low, secondSuit));
            EquityResult result = new EquityCalculator()
                    .simulate(hole, List.of(), new Random(SEED + cell), DEALS_PER_HAND);
            win[row][column] = result.getWin();
            tie[row][column] = result.getTie();
        });
        print("WIN", win);
        print("TIE", tie);
    }

    private static void print(String name, double[][] table) {
        System.out.println("    private static final double[][] " + name + " = {");
        for (double[] row : table) {
            StringBuilder line = new StringBuilder("        {");
            for (int i = 0; i < row.length; i++) {
                if (i > 0) {
                    line.append(", ");
                }
                line.append(String.format(Locale.ROOT, "%.4f", row[i]));
            }
            System.out.println(line + "},");
        }
        System.out.println("    };");
    }
}
