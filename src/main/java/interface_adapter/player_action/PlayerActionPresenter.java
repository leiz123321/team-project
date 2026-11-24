package interface_adapter.player_action;

import interface_adapter.table.TablePublisher;
import interface_adapter.table.TableViewModel;
import use_case.player_action.PlayerActionOutputBoundary;
import use_case.table.TableOutputData;

/**
 * Shows the result of the use case "Take a betting action" on the game
 * screen, by publishing a new state to the table view model.
 */
public class PlayerActionPresenter implements PlayerActionOutputBoundary {
    private final TablePublisher publisher;

    /**
     * Creates the presenter.
     *
     * @param tableViewModel the view model of the game screen
     * @throws IllegalArgumentException if tableViewModel is null
     */
    public PlayerActionPresenter(TableViewModel tableViewModel) {
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
