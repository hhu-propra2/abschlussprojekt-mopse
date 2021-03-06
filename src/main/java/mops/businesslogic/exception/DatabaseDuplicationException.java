package mops.businesslogic.exception;

/**
 * Is thrown if an entry already exists.
 */
public class DatabaseDuplicationException extends DatabaseException {
    /**
     * Is thrown if the an entry already exists.
     *
     * @param message error message
     */
    public DatabaseDuplicationException(String message) {
        super(message);
    }

    /**
     * Is thrown if the an entry already exists.
     *
     * @param message   error message
     * @param exception the wrapped exception
     */
    public DatabaseDuplicationException(String message, Exception exception) {
        super(message, exception);
    }
}
