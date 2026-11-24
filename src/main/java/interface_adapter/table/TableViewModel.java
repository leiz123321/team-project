package interface_adapter.table;

import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;

/**
 * The view model of the game screen. Its state is the whole table; the
 * start-hand, player-action, and opponent-action presenters publish to it, and the
 * game screen listens to it.
 *
 * <p>It starts with the state of a table at which nothing has happened, so
 * its state is never null.
 */
public final class TableViewModel {

    /** The view name of the game screen. */
    public static final String VIEW_NAME = "table";
    public static final String STATE_PROPERTY = "state";

    private final PropertyChangeSupport changes = new PropertyChangeSupport(this);
    private volatile TableState state = new TableState.Builder().build();

    /**
     * Returns the name used to display the table.
     */
    public String getViewName() {
        return VIEW_NAME;
    }

    public TableState getState() {
        return state;
    }

    public void setState(TableState state) {
        this.state = state;
    }

    public void firePropertyChanged() {
        changes.firePropertyChange(STATE_PROPERTY, null, state);
    }

    public void addPropertyChangeListener(PropertyChangeListener listener) {
        changes.addPropertyChangeListener(listener);
    }

    public void removePropertyChangeListener(PropertyChangeListener listener) {
        changes.removePropertyChangeListener(listener);
    }
}
