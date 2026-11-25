package interface_adapter.configure_bot;

import use_case.configure_bot.ConfigureBotOutputBoundary;
import use_case.configure_bot.ConfigureBotOutputData;

public class ConfigureBotPresenter implements ConfigureBotOutputBoundary{

    private final ConfigureBotViewModel configureBotViewModel;

    public ConfigureBotPresenter(ConfigureBotViewModel configureBotViewModel) {
        this.configureBotViewModel = configureBotViewModel;
    }

    @Override
    public void prepareSuccessView(ConfigureBotOutputData outputData) {
        configureBotViewModel.getState().setStyleName(outputData.getStyleName());
        configureBotViewModel.getState().setAggression(outputData.getAggression());
        configureBotViewModel.getState().setConfigError("");
        configureBotViewModel.notifySettingsChangeUpdate();
    }

    @Override
    public void prepareFailView(String errorMessage) {
        configureBotViewModel.getState().setConfigError(errorMessage);
        configureBotViewModel.notifySettingsChangeUpdate();
    }
}
