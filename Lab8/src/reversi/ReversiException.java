package reversi;

/**
 * A custom exception thrown from any of the Reversi classes if something
 * goes wrong.
 *
 * @author Robert St Jacques @ RIT SE
 * @author Sean Strout @ RIT CS
 */
public class ReversiException extends Exception {
    /**
     * Convenience constructor to create a new {@link ReversiException}
     * with an error message.
     *
     * @param msg The error message associated with the exception.
     */
    public ReversiException(String msg) {
        super(msg);
    }

    /**
     * Convenience constructor to create a new {@link ReversiException}
     * as a result of some other exception.
     *
     * @param cause The root cause of the exception.
     */
    public ReversiException(Throwable cause) {
        super(cause);
    }

    /**
     * * Convenience constructor to create a new {@link ReversiException}
     * as a result of some other exception.
     *
     * @param message The message associated with the exception.
     * @param cause The root cause of the exception.
     */
    public ReversiException(String message, Throwable cause) {
        super(message, cause);
    }
}
