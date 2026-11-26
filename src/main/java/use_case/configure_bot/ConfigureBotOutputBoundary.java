package use_case.configure_bot;

/**
 * The output boundary for the Configure Bot Use Case.
 */
public interface ConfigureBotOutputBoundary {

    /**
     * Prepares the success view for the Configure Bot Use Case.
     * @param outputData the output data
     */
    void prepareSuccessView(ConfigureBotOutputData outputData);

    /**
     * Prepares the failure view for the Configure Bot Use Case.
     * @param errorMessage the explanation of the failure
     */
    void prepareFailView(String errorMessage);
}

