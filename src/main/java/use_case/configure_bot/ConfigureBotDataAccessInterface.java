package use_case.configure_bot;

import entity.BotProfile;
/**
 * The DAO interface for the Configure Bot Use Case.
 */
public interface ConfigureBotDataAccessInterface {
    /**
     * Updates the system to record the bot settings.
     * @param botProfile the profile to be updated
     */
    void changeStrategy(BotProfile botProfile);
}
