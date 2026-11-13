package entity;

import java.util.Objects;

/**
 * One betting decision. A bet or a raise carries an amount: the total number
 * of chips the player will have put in during the current betting round once
 * the action is applied. The other actions carry no amount.
 */
public final class Action {
    private final ActionType type;
    private final int amount;

    private Action(ActionType type, int amount) {
        this.type = type;
        this.amount = amount;
    }

    /**
     * Creates an action of the given type.
     *
     * @param type the kind of action
     * @param amount the total for a bet or a raise; ignored for the other kinds
     * @return the action
     * @throws IllegalArgumentException if type is null, or the amount of a bet
     *         or a raise is not positive
     */
    public static Action of(ActionType type, int amount) {
        if (type == null) {
            throw new IllegalArgumentException("type must not be null");
        }
        if (type == ActionType.BET || type == ActionType.RAISE) {
            if (amount <= 0) {
                throw new IllegalArgumentException("amount must be positive");
            }
            return new Action(type, amount);
        }
        return new Action(type, 0);
    }

    public static Action fold() {
        return of(ActionType.FOLD, 0);
    }

    public static Action check() {
        return of(ActionType.CHECK, 0);
    }

    public static Action call() {
        return of(ActionType.CALL, 0);
    }

    public static Action bet(int amount) {
        return of(ActionType.BET, amount);
    }

    public static Action raise(int amount) {
        return of(ActionType.RAISE, amount);
    }

    public ActionType getType() {
        return this.type;
    }

    /**
     * Returns the total for a bet or a raise, and 0 for the other actions.
     */
    public int getAmount() {
        return this.amount;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof Action)) {
            return false;
        }
        Action other = (Action) o;
        return this.type == other.type && this.amount == other.amount;
    }

    @Override
    public int hashCode() {
        return Objects.hash(this.type, this.amount);
    }

    @Override
    public String toString() {
        if (this.amount > 0) {
            return this.type + " " + this.amount;
        }
        return this.type.toString();
    }
}
