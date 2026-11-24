package interface_adapter.configure_bot;

public class ConfigureState {
    private String styleName = "";
    private double aggression = 0.0;
    private String configError;

    public ConfigureState(ConfigureState copy) {
        styleName = copy.styleName;
        aggression = copy.aggression;
        configError = copy.configError;
    }

    // Because of the previous copy constructor, the default constructor must be explicit.
    public ConfigureState() {}

    public String getStyleName() {
        return styleName;
    }

    public void setStyleName(String styleName) {
        this.styleName = styleName;
    }

    public double getAggression() {
        return aggression;
    }

    public void setAggression(double aggression) {this.aggression = aggression; }

    public String getConfigError() {
        return configError;
    }

    public void setConfigError(String configError) {
        this.configError = configError;
    }
}
