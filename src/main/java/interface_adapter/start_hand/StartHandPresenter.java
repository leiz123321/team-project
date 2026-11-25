package interface_adapter.start_hand;

import interface_adapter.table.TablePublisher;
import interface_adapter.table.TableViewModel;
import use_case.start_hand.StartHandOutputBoundary;
import use_case.table.TableOutputData;

/**
 * Shows the result of the use case "Start a new hand" on the game screen, by
 * publishing a new state to the table view model.
 */
public class StartHandPresenter implements StartHandOutputBoundary {
    private final TablePublisher publisher;

    /**
     * Creates the presenter.
     *
     * @param tableViewModel the view model of the game screen
     * @throws IllegalArgumentException if tableViewModel is null
     */
    public StartHandPresenter(TableViewModel tableViewModel) {
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

    /**
     * Shows the table of a match that is over. The use case reports the reason
     * no hand was dealt right after, through {@link #prepareFailView(String)},
     * so the screen gets two states: this table, then the same table with the
     * error message.
     *
     * @param outputData the table as the last hand left it
     */
    @Override
    public void prepareMatchOverView(TableOutputData outputData) {
        this.publisher.publishTable(outputData);
    }
}
