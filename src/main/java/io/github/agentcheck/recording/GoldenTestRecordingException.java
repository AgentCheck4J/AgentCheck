package io.github.agentcheck.recording;

/**
 * Reports a failure while executing one input during golden-test recording.
 */
public final class GoldenTestRecordingException extends RuntimeException {
    /** ID of the input whose execution failed. */
    private final String testInputId;

    GoldenTestRecordingException(String testInputId, RuntimeException cause) {
        super("Could not record test input '" + testInputId + "': " + messageFrom(cause), cause);
        this.testInputId = testInputId;
    }

    /**
     * Returns the ID of the input that could not be recorded.
     *
     * @return failing test input ID
     */
    public String testInputId() {
        return testInputId;
    }

    private static String messageFrom(RuntimeException cause) {
        String message = cause.getMessage();
        return message == null || message.isBlank() ? cause.getClass().getSimpleName() : message;
    }
}
