package interface_adapter.configure_bot;

import use_case.configure_bot.ConfigureBotInputBoundary;
import use_case.configure_bot.ConfigureBotInputData;

public class ConfigureBotController {

    private final ConfigureBotInputBoundary configureBotInteractor;

    public ConfigureBotController(ConfigureBotInputBoundary configureBotInteractor) {
        this.configureBotInteractor = configureBotInteractor;
    }

    /**
     * Executes the Login Use Case.
     * @param styleName the specified style
     * @param aggression the specified level of aggression
     */
    public void execute(String styleName, double aggression) {
        final ConfigureBotInputData inputData = new ConfigureBotInputData(styleName, aggression);
        configureBotInteractor.execute(inputData);
    }
}
