package use_case.configure_bot;

import entity.BotProfile;

public class ConfigureBotInteractor implements ConfigureBotInputBoundary{
    private final ConfigureBotDataAccessInterface dataAccessObject;
    private final ConfigureBotOutputBoundary presenter;

    public ConfigureBotInteractor(ConfigureBotDataAccessInterface dataAccessObject,
                                  ConfigureBotOutputBoundary changePasswordOutputBoundary) {
        this.dataAccessObject = dataAccessObject;
        this.presenter = changePasswordOutputBoundary;
    }

    @Override
    public void execute(ConfigureBotInputData inputData) {
        if (inputData.getStyleName() == null || inputData.getStyleName().isBlank()) {
            presenter.prepareFailView("styleName cannot be empty");
        }
        else if (Double.isNaN(inputData.getAggression())
                || inputData.getAggression() < 0.0
                || inputData.getAggression() > 1.0) {
            presenter.prepareFailView("aggression must be between 0 and 1");
        }
        else {
            final BotProfile profile = new BotProfile(inputData.getStyleName(),
                    inputData.getAggression());

            dataAccessObject.changeStrategy(profile);

            final ConfigureBotOutputData outputData =
                    new ConfigureBotOutputData(inputData.getStyleName(),
                    inputData.getAggression());

            presenter.prepareSuccessView(outputData);
        }
    }
}
