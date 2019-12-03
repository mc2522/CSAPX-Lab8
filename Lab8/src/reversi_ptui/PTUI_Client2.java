package reversi_ptui;

import javafx.stage.Stage;
import reversi.ReversiException;
import reversi2.Board;
import reversi2.NetworkClient;

import java.io.PrintWriter;
import java.util.*;

/**
 * The PTUI_Client2 application is the plain text UI for Reversi.
 *
 * @author James Heliotis
 * @author Sean Strout @ RIT CS
 */
public class PTUI_Client2 extends ConsoleApplication implements Observer {

    /**
     * conc_model for the game
     */
    private Board model;

    /**
     * Connection to network interface to server
     */
    private NetworkClient serverConn;

    /**
     * What to read to see what user types
     */
    private Scanner userIn;

    /**
     * Where to send text that the user can see
     */
    private PrintWriter userOut;

    /**
     * Create the board model, create the network connection based on
     * command line parameters, and use the first message received to
     * allocate the board size the server is also using.
     */
    @Override
    public void init() {
        try {
            List< String > args = super.getArguments();

            // Get host info from command line
            String host = args.get( 0 );
            int port = Integer.parseInt( args.get( 1 ) );

            // Create uninitialized board.
            this.model = new Board();
            // Create the network connection.
            this.serverConn = new NetworkClient( host, port, this.model );

            this.model.initializeGame();
        }
        catch( ReversiException |
                ArrayIndexOutOfBoundsException |
                NumberFormatException e ) {
            System.out.println( e );
            throw new RuntimeException( e );
        }
    }

    /**
     * This method continues running until the game is over.
     * It is not like {@link javafx.application.Application#start(Stage)}.
     * That method returns as soon as the setup is done.
     * This method waits for a notification from {@link #endGame()},
     * called indirectly from a model update from {@link NetworkClient}.
     *
     * @param userIn what to read to see what user types
     * @param userOut where to send messages so user can see them
     */
    @Override
    public synchronized void go( Scanner userIn, PrintWriter userOut ) {

        this.userIn = userIn;
        this.userOut = userOut;

        // Connect UI to model. Can't do it sooner because streams not set up.
        this.model.addObserver( this );
        // Manually force a display of all board state, since it's too late
        // to trigger update().
        this.refresh();
        while ( this.model.getStatus() == Board.Status.NOT_OVER ) {
            try {
                this.wait();
            }
            catch( InterruptedException ie ) {}
        }

    }

    /**
     * GUI is closing, so close the network connection. Server will
     * get the message.
     */
    @Override
    public void stop() {
        this.userIn.close();
        this.userOut.close();
        this.serverConn.close();
    }

    private synchronized void endGame() {
        this.notify();
    }

    /**
     * Update all GUI Nodes to match the state of the model.
     */
    private void refresh() {
        if ( !this.model.isMyTurn() ) {
            this.userOut.println( this.model );
            this.userOut.println( this.model.getMovesLeft() + " moves left." );
            Board.Status status = this.model.getStatus();
            switch ( status ) {
                case ERROR:
                    this.userOut.println( status );
                    this.endGame();
                    break;
                case I_WON:
                    this.userOut.println( "You won. Yay!" );
                    this.endGame();
                    break;
                case I_LOST:
                    this.userOut.println( "You lost. Boo!" );
                    this.endGame();
                    break;
                case TIE:
                    this.userOut.println( "Tie game. Meh." );
                    this.endGame();
                    break;
                default:
                    this.userOut.println();
            }
        }
        else {
            boolean done = false;
            do {
                this.userOut.print("type move as row◻︎column: ");
                this.userOut.flush();
                int row = this.userIn.nextInt();
                int col = this.userIn.nextInt();
                if (this.model.isValidMove(row, col)) {
                    this.userOut.println(this.userIn.nextLine());
                    this.serverConn.sendMove(row, col);
                    done = true;
                }
            } while (!done);
        }
    }

    /**
     * Update the UI when the model calls notify.
     * Currently no information is passed as to what changed,
     * so everything is redone.
     *
     * @param t An Observable -- assumed to be the model.
     * @param o An Object -- not used.
     */
    @Override
    public void update( Observable t, Object o ) {

        assert t == this.model: "Update from non-model Observable";

        this.refresh();

    }

    /**
     * Launch the JavaFX GUI.
     *
     * @param args not used, here, but named arguments are passed to the GUI.
     *             <code>--host=<i>hostname</i> --port=<i>portnum</i></code>
     */
    public static void main( String[] args ) {
        if (args.length != 2) {
            System.out.println("Usage: java PTUI_Client2 host port");
            System.exit(0);
        } else {
            ConsoleApplication.launch(PTUI_Client2.class, args);
        }
    }

}
