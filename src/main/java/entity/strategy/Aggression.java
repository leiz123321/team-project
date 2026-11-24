package entity.strategy;

/**
 * The aggression setting of a built-in strategy, from 0 (most careful) to 1
 * (boldest), and the ways it moves the numbers a style plays by: some numbers
 * a bolder opponent has less of, such as the share of hands it folds, and
 * some it has more of, such as the size of its bets.
 *
 * <p>The middle of the scale, {@link #NEUTRAL}, changes nothing: a strategy
 * then plays exactly by its own numbers.
 */
final class Aggression {

    /** The setting at which a strategy plays by its own numbers. */
    static final double NEUTRAL = 0.5;

    /** Where the setting is on the scale: -1 at 0, 0 at the neutral setting, 1 at 1. */
    private final double lean;

    /**
     * Creates a setting.
     *
     * @param level the aggression, from 0 to 1
     * @throws IllegalArgumentException if level is not between 0 and 1
     */
    Aggression(double level) {
        // Written as "not inside the range" so that a value that is not a number is rejected too.
        if (!(level >= 0.0 && level <= 1.0)) {
            throw new IllegalArgumentException("aggression must be between 0 and 1");
        }
        this.lean = (level - NEUTRAL) / NEUTRAL;
    }

    /**
     * Returns a number a bolder opponent has less of, moved by this setting.
     *
     * @param neutral the number at the neutral setting
     * @param swing how far it moves at either end of the scale
     * @return the number to use
     */
    double lowered(double neutral, double swing) {
        return neutral - this.lean * swing;
    }

    /**
     * Returns a number a bolder opponent has more of, moved by this setting.
     *
     * @param neutral the number at the neutral setting
     * @param swing how far it moves at either end of the scale
     * @return the number to use
     */
    double raised(double neutral, double swing) {
        return neutral + this.lean * swing;
    }

    /**
     * Returns a number a bolder opponent has more of, moved by this setting
     * in proportion to itself: at the bold end of the scale it is larger by
     * the given share of itself, at the careful end smaller by as much.
     *
     * @param neutral the number at the neutral setting
     * @param swing the share of the number it gains or loses at either end
     * @return the number to use
     */
    double scaled(double neutral, double swing) {
        return neutral * (1 + this.lean * swing);
    }
}
