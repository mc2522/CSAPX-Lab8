package reversi_gui;

import javafx.application.Application;

import java.util.*;
import java.util.List;

import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
import javafx.stage.Stage;
import reversi.ReversiException;
import reversi2.Board;
import reversi2.NetworkClient;

/**
 * This application is the UI for Reversi.
 *
 * @author Mike Cao
 */
public class GUI_Client2 extends Application implements Observer  {

    /**
     * Connection to network interface to server
     */
    private NetworkClient serverConn;

    /**
     * Declares a board to be initialized in init.
     */
    private Board model;

    /**
     * gridpane for buttons to be used in the game.
     */
    private GridPane grid;

    /**
     * labels to be used in the GUI.
     */
    private Label remaining = new Label();
    private Label current = new Label();
    private Label status = new Label("Running");

    /**
     * Images for empty, player 1, player 2 pieces.
     */
    private Image empty = new Image(getClass().getResourceAsStream("empty.jpg"));
    private Image p1 = new Image(getClass().getResourceAsStream("othelloP1.jpg"));
    private Image p2 = new Image(getClass().getResourceAsStream("othelloP2.jpg"));

    /**
     * Size of 1 DIM.
     */
    private static final double SIZE = 81.25;

    /**
     * Create the board model, create the network connection based on command line parameters, and use the first message received to allocate the board size the server is also using.
     */
    public void init() {
        // Get host info from command line
        List<String> args = getParameters().getRaw();

        // get host info and username from command line
        String host = args.get(0);
        int port = Integer.parseInt(args.get(1));

        try {
            this.model = new Board();
            this.serverConn = new NetworkClient(host, port, this.model);
            this.model.initializeGame();
        } catch( ReversiException |
                ArrayIndexOutOfBoundsException |
                NumberFormatException e ) {
            System.out.println( e );
            throw new RuntimeException( e );
        }
    }

    /**
     * Creates the GUI.
     * @param mainStage
     */
    public synchronized void start( Stage mainStage ) {
        BorderPane pane = new BorderPane();
        grid = build(this.model.getDIM());

        //Update pieces to empty, p1, or p2.
        updatePieces();

        //Create regions for the Hboxes to add spaces.
        Region region1 = new Region();
        HBox.setHgrow(region1, Priority.ALWAYS);
        Region region2 = new Region();
        HBox.setHgrow(region2, Priority.ALWAYS);
        Region region3 = new Region();
        HBox.setHgrow(region3, Priority.ALWAYS);

        //Sets the grid in the center of the BorderPane.
        pane.setCenter(this.grid);
        //Sets the HBox with the remaining moves and the status in the bottom of the BorderPane.
        pane.setBottom(new HBox(remaining, region1, status));
        //Sets the Hbox with the current move in the top of the BorderPane.
        pane.setTop(new HBox(region2, current, region3));

        refresh();

        //Add observer
        this.model.addObserver(this);

        //Set the title to Reversi.
        mainStage.setTitle("Reversi");
        double dimensions = this.model.getDIM()*SIZE;
        Scene scene = new Scene(pane, dimensions, dimensions);
        mainStage.setScene(scene);
        mainStage.show();
    }

    /**
     * Checks if the move made is valid. Changes current's text depending on the results.
     * @param button
     */
    private void validMove(Button button) {
        int row = Integer.parseInt(button.getId().split(" ")[0]);
        int col = Integer.parseInt(button.getId().split(" ")[1]);

        if(this.model.isMyTurn()) {
            if(this.model.isValidMove(row, col)) {
                this.serverConn.sendMove(row, col);
            } else {
                Platform.runLater(() ->
                        current.setText("INVALID"));
            }
        } else {
            Platform.runLater(() ->
                    current.setText("Wait for your turn."));
        }
    }

    /**
     * GUI is closing, so close the network connection. Server will
     * get the message.
     */
    @Override
    public void stop() {
        this.serverConn.close();
    }

    /**
     * Update all GUI Nodes to match the state of the model.
     */
    private void refresh() {
        //Update the label for the turn.
        updateTurn();

        //Update the moves remaining.
        updateMovesRemaining();

        //Update the pieces on the board.
        updatePieces();

        if (this.model.getStatus() != Board.Status.NOT_OVER)
            finish(this.model.getStatus());
    }

    /**
     * Builds the grid pane of buttons.
     * @param DIM
     * @return GridPane of buttons
     */
    private GridPane build(int DIM) {
        GridPane grid = new GridPane();
        for (int r = 0; r < DIM; r++) {
            for (int c = 0; c < DIM; c++) {
                Button button = new Button();
                button.setId(r + " " + c);

                button.setOnMouseClicked((event) ->
                        validMove(button));

                grid.add(button, r, c);
            }
        }
        return grid;
    }

    /**
     * Update the UI when the model calls notify.
     * Currently no information is passed as to what changed,
     * so everything is redone.
     *
     * @param t An Observable -- assumed to be the model.
     * @param o An Object -- not used.
     */
    public void update( Observable t, Object o ) {

        assert t == this.model: "Update from non-model Observable";

        this.refresh();

    }

    /**
     * Displays a message on the results of the game.
     * @param status
     */
    private void finish(Board.Status status) {
        String result = "";

        //Assign result to the result of the game.
        switch (status) {
            case TIE:
                result = "Game over. You tied!";
                break;
            case I_WON:
                result = "Game over. You won!";
                break;
            case I_LOST:
                result = "Game over. You lost!";
                break;
            case ERROR:
                result = "ERROR!";
                break;
        }

        final String message = result;
        Platform.runLater(() ->
                current.setText(message));

        Platform.runLater(() ->
                status.setMessage("Stopped."));
    }

    /**
     * Updates the pieces on the board.
     */
    private void updatePieces() {
        for(Node child: this.grid.getChildren()) {
            if(child instanceof Button) {
                Button button = (Button)child;

                int row = Integer.parseInt(button.getId().split(" ")[0]);
                int col = Integer.parseInt(button.getId().split(" ")[1]);

                //Sets the images of the button depending on the move.
                if (this.model.getContents(row, col) == Board.Move.PLAYER_ONE) {
                    Platform.runLater(() ->
                            button.setGraphic(new ImageView(p1)));
                }
                 else if (this.model.getContents(row, col) == Board.Move.PLAYER_TWO) {
                    Platform.runLater(() ->
                            button.setGraphic(new ImageView(p2)));
                }
                else {
                    Platform.runLater(() ->
                            button.setGraphic(new ImageView(empty)));
                }
            }
        }
    }

    /**
     * Sets the text in remaining to display the remaining number of moves for the player.
     */
    private void updateMovesRemaining() {
        int movesRemaining = this.model.getMovesLeft();
        Platform.runLater(() ->
                remaining.setText("Moves left: " + movesRemaining));
    }

    /**
     * Sets the text in current to display whether or not it's the player's turn.
     */
    private void updateTurn() {
        if (this.model.isMyTurn()) {
            Platform.runLater(() ->
                    current.setText("It's your turn."));
        } else {
            Platform.runLater(() ->
                    current.setText("Wait for your turn."));
        }
    }

    /**
     * Launch the JavaFX GUI.
     *
     * @param args not used, here, but named arguments are passed to the GUI.
     *             <code>--host=<i>hostname</i> --port=<i>portnum</i></code>
     */
     public static void main (String[]args ){
         if (args.length != 2) {
             System.out.println("Usage: java GUI_Client2 host port");
             System.exit(0);
         } else {
             Application.launch(args);
         }
     }
}
