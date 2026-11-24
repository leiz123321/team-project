package use_case.configure_bot;

/**
 * The output data for the Configure Bot Use Case.
 */
public class ConfigureBotOutputData {

    private final String styleName;
    private final double aggression;

    public ConfigureBotOutputData(String styleName, double aggression) {
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
