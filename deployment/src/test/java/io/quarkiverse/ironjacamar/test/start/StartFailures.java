package io.quarkiverse.ironjacamar.test.start;

/**
 * Helps asserting the exception failing the startup of an application.
 */
final class StartFailures {

    private StartFailures() {
    }

    /**
     * Concatenate the messages of a throwable and of its causes, as the startup failures are deeply wrapped.
     *
     * @param throwable the throwable
     * @return the messages of the whole cause chain
     */
    static String messagesOf(Throwable throwable) {
        StringBuilder messages = new StringBuilder();
        for (Throwable current = throwable; current != null; current = current.getCause()) {
            messages.append(current).append(System.lineSeparator());
        }
        return messages.toString();
    }
}
