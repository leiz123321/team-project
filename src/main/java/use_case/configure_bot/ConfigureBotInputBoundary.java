package use_case.configure_bot;

/**
 * The input boundary for the Configure Bot Use Case.
 */
public interface ConfigureBotInputBoundary {

    /**
     * Execute the Configure Bot Use Case.
     * @param inputData the input data for this use case
     */
    void execute(ConfigureBotInputData inputData);

}

