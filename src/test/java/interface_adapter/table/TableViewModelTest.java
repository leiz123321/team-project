package interface_adapter.table;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;

import java.beans.PropertyChangeEvent;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;


class TableViewModelTest {

    @Test
    void isTheViewModelOfTheTableScreen() {
        TableViewModel viewModel = new TableViewModel();

        assertEquals("table", TableViewModel.VIEW_NAME);
        assertEquals("table", viewModel.getViewName());
    }

    @Test
    void startsWithTheStateOfATableAtWhichNothingHasHappened() {
        TableState state = new TableViewModel().getState();

        assertNotNull(state);
        assertEquals(0, state.getRevision());
        assertEquals(0, state.getHandNumber());
        assertEquals(List.of(), state.getOpponentCards());
        assertEquals("", state.getMessage());
        assertEquals("", state.getErrorMessage());
    }

    @Test
    void tellsItsListenersWhenANewStateIsPublished() {
        TableViewModel viewModel = new TableViewModel();
        List<PropertyChangeEvent> heard = new ArrayList<>();
        viewModel.addPropertyChangeListener(heard::add);
        TableState next = new TableState.Builder().setRevision(1).build();

        viewModel.setState(next);
        viewModel.firePropertyChanged();

        assertEquals(1, heard.size());
        assertEquals(TableViewModel.STATE_PROPERTY, heard.get(0).getPropertyName());
        assertSame(next, heard.get(0).getNewValue());
    }
}
