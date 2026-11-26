package interface_adapter.configure_bot;

public interface ConfigureBotViewModelListener {

    /**
     * The configure state was updated.
     */
    void stateChanged();

    /**
     * A settings change attempt occurred.
     */
    void settingsChanged();
}
