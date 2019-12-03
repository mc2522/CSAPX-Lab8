package reversi.server;

import reversi.ReversiException;
import reversi.ReversiProtocol;

import java.io.Closeable;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

/**
 * The {@link ReversiServer} waits for incoming client connections and
 * pairs them off to play {@link ReversiGame games}.
 *
 * @author Robert St Jacques @ RIT SE
 * @author Sean Strout @ RIT CS
 */
public class ReversiServer implements ReversiProtocol, Closeable {
    /**
     * The {@link ServerSocket} used to wait for incoming client connections.
     */
    private ServerSocket server;

    /**
     * Creates a new {@link ReversiServer} that listens for incoming
     * connections on the specified port.
     *
     * @param port The port on which the server should listen for incoming
     *             connections.
     * @throws ReversiException If there is an error creating the
     *                          {@link ServerSocket}
     */
    public ReversiServer(int port) throws ReversiException {
        try {
            this.server = new ServerSocket(port);
        } catch (IOException e) {
            throw new ReversiException(e);
        }
    }

    /**
     * Closes the client {@link Socket}.
     */
    @Override
    public void close() {
        try {
            this.server.close();
        } catch (IOException ioe) {
            // squash
        }
    }

    /**
     * Waits for two clients to connect. Creates a {@link ReversiPlayer}
     * for each and then pairs them off in a {@link ReversiGame}.
     * <p>
     * This server is not threaded, so only a single game can be played.
     * The server terminates if an exception is raised, or the game ends.
     *
     * @param DIM square dimension of board
     */
    public void run(int DIM) {
        try {
            System.out.println("Waiting for player one...");
            Socket playerOneSocket = server.accept();
            try (ReversiPlayer playerOne =
                    new ReversiPlayer(playerOneSocket)) {
                playerOne.connect(DIM);
                System.out.println("Player one connected! " + playerOneSocket);
                System.out.println("Waiting for player two...");
                Socket playerTwoSocket = server.accept();
                try (ReversiPlayer playerTwo =
                        new ReversiPlayer(playerTwoSocket)) {
                    playerTwo.connect(DIM);
                    System.out.println("Player two connected! " + playerTwoSocket);

                    System.out.println("Starting F!");
                    ReversiGame game =
                            new ReversiGame(DIM, playerOne, playerTwo);
                    game.run();
                }
            }
        } catch (IOException e) {
            System.err.println("Something has gone horribly wrong!");
            e.printStackTrace();
        } catch (ReversiException e) {
            System.err.println("Failed to create players!");
            e.printStackTrace();
        }
    }

    /**
     * Starts a new {@link ReversiServer}. Simply creates the server and
     * calls {@link #run(int)}
     *
     * @param args Used to specify the port on which the server should listen
     *             for incoming client connections.
     * @throws ReversiException If there is an error starting the server.
     */
    public static void main(String[] args) throws ReversiException {
        if (args.length != 2) {
            System.out.println("Usage: java ReversiServer DIM port");
            System.exit(1);
        }

        try (ReversiServer server = new ReversiServer(Integer.parseInt(args[1]))) {
            server.run(Integer.parseInt(args[0]));
        } catch (ReversiException e) {
            System.err.println("Failed to start server!");
            e.printStackTrace();
        }
    }
}
