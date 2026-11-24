package interface_adapter.opponent_action;

import interface_adapter.table.TablePublisher;
import interface_adapter.table.TableViewModel;
import use_case.opponent_action.OpponentActionOutputBoundary;
import use_case.table.TableOutputData;

/**
 * Shows the result of the computer's betting action on
 * the game screen, by publishing a new state to the table view model.
 */
public class OpponentActionPresenter implements OpponentActionOutputBoundary {
    private final TablePublisher publisher;

    /**
     * Creates the presenter.
     *
     * @param tableViewModel the view model of the game screen
     * @throws IllegalArgumentException if tableViewModel is null
     */
    public OpponentActionPresenter(TableViewModel tableViewModel) {
        this.publisher = new TablePublisher(tableViewModel);
    }

    @Override
    public void prepareSuccessView(TableOutputData outputData) {
        this.publisher.publishTable(outputData);
    }

    @Override
    public void prepareFailView(String errorMessage) {
        this.publisher.publishFailure(errorMessage);
    }
}
