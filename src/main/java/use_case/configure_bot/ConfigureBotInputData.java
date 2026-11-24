package use_case.configure_bot;

/**
 * The input data for the Configure Bot Use Case.
 */
public class ConfigureBotInputData {

    private final String styleName;
    private final double aggression;

    public ConfigureBotInputData(String styleName, double aggression) {
        this.styleName = styleName;
        this.aggression = aggression;
    }

    public String getStyleName() {
        return styleName;
    }

    public double getAggression() {
        return aggression;
    }

}
