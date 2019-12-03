package reversi.server;

import reversi.ReversiException;
import reversi.ReversiProtocol;

import java.io.Closeable;
import java.io.IOException;
import java.io.PrintStream;
import java.net.Socket;
import java.util.Scanner;

/**
 * A class that manages the requests and responses to a single client.
 *
 * @author Robert St Jacques @ RIT SE
 * @author Sean Strout @ RIT CS
 */
public class ReversiPlayer implements ReversiProtocol, Closeable {
    /** The {@link Socket} used to communicate with the client. */
    private Socket sock;

    /**
     * The {@link Scanner} used to read responses from the client.
     */
    private Scanner scanner;

    /**
     * The {@link PrintStream} used to send requests to the client.
     */
    private PrintStream printer;

    /**
     * Creates a new {@link ReversiPlayer} that will use the specified
     * {@link Socket} to communicate with the client.
     *
     * @param sock The {@link Socket} used to communicate with the client.
     *
     * @throws ReversiException If there is a problem establishing
     * communication with the client.
     */
    public ReversiPlayer(Socket sock) throws ReversiException {
        this.sock = sock;
        try {
            this.scanner = new Scanner(sock.getInputStream());
            this.printer = new PrintStream(sock.getOutputStream());
        }
        catch (IOException e) {
            throw new ReversiException(e);
        }
    }

    /**
     * Sends the initial {@link #CONNECT} request to the client
     *
     * @param DIM square dimension of board
     */
    public void connect(int DIM) {
        this.printer.println(CONNECT + " " + DIM);
    }

    /**
     * Sends a {@link #MAKE_MOVE} request to the client and returns the column
     * in which the client would like to move.
     *
     * @return The row and column in which the client would like to move.
     *
     * @throws ReversiException If the client's response is invalid, i.e.
     * not {@link #MOVE} and a column number.
     */
    public int[] makeMove() throws ReversiException {
        this.printer.println(MAKE_MOVE);
        String response = scanner.nextLine();

        if(response.startsWith(MOVE)) {
            String[] tokens = response.split(" ");
            if(tokens.length == 3) {
                int[] coord = new int[2];
                coord[0] = Integer.parseInt(tokens[1]);
                coord[1] = Integer.parseInt(tokens[2]);
                return coord;
            }
            else {
                throw new ReversiException("Invalid player response: " +
                        response);
            }
        }
        else {
            throw new ReversiException("Invalid player response: " +
                    response);
        }
    }

    /**
     * Sends a {@link #MOVE_MADE} request to the client to inform the client
     * that a move has been made on the board.
     *
     * @param row The row in which the move has been made.
     * @param column The column in which the move has been made.
     */
    public void moveMade(int row, int column) {
        this.printer.println(MOVE_MADE + " " + row + " " + column);
    }

    /**
     * Called to send a {@link #GAME_WON} request to the client because the
     * player's most recent move won the game.
     */
    public void gameWon() {
        this.printer.println(GAME_WON);
    }

    /**
     * Called to send a {@link #GAME_WON} request to the client because the
     * player's most recent move won the game.
     */
    public void gameTied() {
        this.printer.println(GAME_TIED);
    }

    /**
     * Called to send a {@link #GAME_LOST} request to the client because the
     * other player's most recent move wont the game.
     */
    public void gameLost()  {
        this.printer.println(GAME_LOST);
    }

    /**
     * Called to send an {@link #ERROR} to the client. This is called if either
     * client has invalidated themselves with a bad response.
     *
     * @param message The error message.
     */
    public void error(String message) {
        this.printer.println(ERROR + " " + message);
    }

    /**
     * Called to close the client connection after the game is over.
     */
    @Override
    public void close() {
        try {
            this.sock.close();
        }
        catch(IOException ioe) {
            // squash
        }
    }
}
