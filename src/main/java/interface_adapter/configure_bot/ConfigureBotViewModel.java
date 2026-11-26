package interface_adapter.configure_bot;

import java.util.ArrayList;
import java.util.List;

public class ConfigureBotViewModel {
    private final List<ConfigureBotViewModelListener> listeners = new ArrayList<>();

    public static final String VIEW_NAME = "config";

    private ConfigureState state = new ConfigureState();

    public ConfigureState getState() {
        return this.state;
    }

    public void setState(ConfigureState state) {
        this.state = state;
    }

    public void addListener(ConfigureBotViewModelListener listener) {
        listeners.add(listener);
    }

    public void notifyStateChanged() {
        for (ConfigureBotViewModelListener listener : listeners) {
            listener.stateChanged();
        }
    }

    public void notifySettingsChangeUpdate() {
        for (ConfigureBotViewModelListener listener : listeners) {
            listener.settingsChanged();
        }
    }
}
