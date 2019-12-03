package reversi2;

import reversi.ReversiException;

import java.util.EnumMap;
import java.util.Observable;

/**
 * A copy of the board state kept by client code at the site of the player.
 * It acts as a model in an MVC pattern because the local UI observes changes
 * in it.
 */
public class Board extends Observable {
    /**
     * The board size required to place starting discs
     */
    public final static int MIN_DIM = 2;

    /**
     * How many moves are left to make before EOG
     */
    private int movesLeft;

    /**
     * Used to indicate a move that has been made on the board,
     * and to keep track of whose turn it is
     */
    public enum Move {
        PLAYER_ONE, PLAYER_TWO, NONE;

        public Move opponent() {
            return this == PLAYER_ONE ?
                                PLAYER_TWO :
                                this == PLAYER_TWO ?
                                        PLAYER_ONE :
                                        this;
        }
    }

    /**
     * All eight directions that are considered when dealing with
     * Reversi moves
     */
    private enum Dir {
        N(-1,0), NE(-1,1), E(0,1), SE(1,1),
        S(1,0), SW(1,-1), W(0,-1), NW(-1,-1);

        public final int rd, cd;

        private Dir( int rowDelta, int colDelta ) {
            this.rd = rowDelta;
            this.cd = colDelta;
        }
    }

    /**
     * Possible statuses of game
     */
    public enum Status {
        NOT_OVER, I_WON, I_LOST, TIE, ERROR;

        private String message = null;

        public void setMessage( String msg ) {
            this.message = msg;
        }

        @Override
        public String toString() {
            return super.toString() +
                   this.message == null ? "" : ( '(' + this.message + ')' );
        }
    }

    /**
     * square dimension of board
     */
    private int DIM;

    /**
     * If this "copy" of the server's board is the one that can be updated,
     * i.e., if it is "this" player's turn. This is determined by the server.
     * It has a {@link reversi.ReversiProtocol#MAKE_MOVE} message that sets
     * this variable true. The act of the player choosing a move sets the
     * variable false again.
     */
    private boolean myTurn;

    /**
     * This value flips back and forth as discs are added to the board.
     */
    private Move currentPiece;

    /**
     * Current game status
     */
    private Status status;

    /**
     * The board matrix of squares
     */
    private Board.Move[][] board;

    /**
     * Initialize an empty board of a specified size.
     *
     * @param DIM square dimension of board
     * @rit.pre the board dimensions cannot be smaller than
     *          {@link Board#MIN_DIM}x{@link Board#MIN_DIM}
     * @throws ReversiException if a dimension is too small
     */
    public void allocate( int DIM ) throws ReversiException {
        if ( DIM < Board.MIN_DIM ) {
            throw new ReversiException( "Board too small to play" );
        }
        // Allocate the matrix and set its dimensions (redundant).
        this.board = new Board.Move[ DIM ][ DIM ];
        this.DIM = DIM;
        this.movesLeft = DIM * DIM;
        this.status = Status.NOT_OVER;
        this.myTurn = false;
    }

    /**
     * This method fills in the state of squares and other info on
     * the game board.
     * @rit.pre Call this only once {@link #allocate(int)} has been called.
     */
    public void initializeGame() {

        // Initialize all squares on the board to empty.
        for ( int row = 0; row < this.DIM; ++row ) {
            for ( int col = 0; col < this.DIM; ++col ) {
                this.board[ row ][ col ] = Move.NONE;
            }
        }
        // populate the center of the board with pieces
        this.board[ this.DIM / 2 - 1 ][ this.DIM / 2 - 1 ] = Move.PLAYER_ONE;
        this.board[ this.DIM / 2 ][ this.DIM / 2 ] = Move.PLAYER_ONE;
        this.board[ this.DIM / 2 - 1 ][ this.DIM / 2 ] = Move.PLAYER_TWO;
        this.board[ this.DIM / 2 ][ this.DIM / 2 - 1 ] = Move.PLAYER_TWO;
        this.movesLeft -= 4;

        // It's never my turn unless the server tells me to make a move.
        this.myTurn = false;
        // Whether it's me or the other player, Player#1 always goes first.
        this.currentPiece = Move.PLAYER_ONE;
        this.status = Status.NOT_OVER;

        // finishing setting up all instance data
        super.setChanged();
        super.notifyObservers();
    }

    /**
     * Get the square dimension of the board
     *
     * @return number of rows
     */
    public int getDIM() {
        return this.DIM;
    }


    /**
     * Information for the UI
     * @return the number of additional moves until the board is full.
     */
    public int getMovesLeft() {
        return this.movesLeft;
    }

    /**
     * Can the local user make changes to the board?
     * @return true if the server has told this player it is its time to move
     */
    public boolean isMyTurn() {
        return this.myTurn;
    }

    /**
     * The user has chosen a move.
     */
    public void didMyTurn() {
        this.myTurn = false;
    }

    /**
     * Get game status.
     * @return the Status object for the game
     */
    public Status getStatus() {
        return this.status;
    }

    /**
     * What is at this square?
     * @param row row number of square
     * @param col column number of square
     * @return the player (or {@link Board.Move#NONE}) at the given location
     */
    public Move getContents( int row, int col ) {
        return this.board[ row ][ col ];
    }

    /**
     * When a new valid move is made, this helper routine flips all the pieces
     * on the board that are affected.
     *
     * @param row the row
     * @param col the column
     */
    private void flipPieces( int row, int col ) {
        // figure out who made the move and who the other player is
        Board.Move me = this.board[ row ][ col ];
        Board.Move opp = me.opponent();

        // generate row and column deltas [-1, 0, +1] to check the eight
        // directions
        // on the board.  this code comes courtesy of a port of python code from
        // the great and mighty oracle himself, Jim. <3
        //
        // Changed by The Oracle to make it even more gorgeous.
        for ( Dir d: Dir.values() ) {

            int r = row + d.rd;
            int c = col + d.cd;

            // Continue in the current direction until we go off the end
            // of the
            // board or we reach a square that does not contain an
            // opponent's disc.
            while ( r >= 0 && r < this.DIM && c >= 0 && c < this.DIM &&
                    this.board[ r ][ c ] == opp ) {
                r += d.rd;
                c += d.cd;
            }

            // If we did not go off the board and the square we stopped on
            // contains one of this player's discs, flips the ones in
            // between.
            if ( r >= 0 && r < this.DIM && c >= 0 && c < this.DIM &&
                 this.board[ r ][ c ] == me ) {
                // restart
                r = row + d.rd;
                c = col + d.cd;

                while ( r >= 0 && r < this.DIM && c >= 0 &&
                        c < this.DIM && this.board[ r ][ c ] == opp ) {
                    this.board[ r ][ c ] = me;
                    r += d.rd;
                    c += d.cd;
                }
            }
        }
    }

    /**
     * Check that there is an occupied neighbor - we relax the official rules
     * here that say the neighbor must be the same color.
     *
     * @param row the row
     * @param col the column
     * @return whether there is an occupied neighbor or not
     */
    private boolean occupiedNeighbor( int row, int col ) {
        for ( Dir d : Dir.values() ) {
            int r = row + d.rd;
            int c = col + d.cd;
            if ( r >= 0 && r < this.DIM && c >= 0 &&
                 c < this.DIM &&
                 this.board[ r ][ c ] != Move.NONE ) {
                return true;
            }
        }
        return false;
    }

    /**
     * Will this move be accepted as valid by the server?
     * This method is added so that a bad move is caught before it is sent
     * to the server, and the server quits.
     *
     * @param row the row
     * @param col the column
     * @return true iff the chosen square is adjacent to an occupied square
     */
    public boolean isValidMove( int row, int col ) {
        return
                ( row >= 0 && row < this.DIM ) &&
                ( col >= 0 && col < this.DIM ) &&
                ( this.board[ row ][ col ] == Move.NONE ) &&
                this.occupiedNeighbor( row, col );
    }

    /**
     * Called when a move is made in the requested.
     */
    public void makeMove() {
        // Presumably all of this primes the UI to prompt for, and get, a move.
        this.myTurn = true;
        super.setChanged();
        super.notifyObservers();
    }

    /**
     * The UI calls this to announce the player's choice of a move.
     * @param row the row
     * @param col the column
     */
    public void moveMade( int row, int col ) {
        // gets called as a result of the message from the server.
        // place piece on board
        this.movesLeft -= 1;
        this.board[ row ][ col ] = this.currentPiece;

        // flip opposite neighbors
        flipPieces( row, col );

        this.currentPiece = this.currentPiece.opponent();
        this.myTurn = false;

        super.setChanged();
        super.notifyObservers();
    }

    /**
     * Called when the game has been won by this player.
     */
    public void gameWon() {
        this.status = Status.I_WON;
        super.setChanged();
        super.notifyObservers();
    }

    /**
     * Called when the game has been won by the other player.
     */
    public void gameLost() {
        this.status = Status.I_LOST;
        super.setChanged();
        super.notifyObservers();
    }

    /**
     * Called when the game has been tied.
     */
    public void gameTied() {
        this.status = Status.TIE;
        super.setChanged();
        super.notifyObservers();
    }

    /**
     * Called when an error is sent from the server.
     *
     * @param arguments The error message sent from the reversi.server.
     */
    public void error( String arguments ) {
        this.status = Status.ERROR;
        this.status.setMessage( arguments );
        super.setChanged();
        super.notifyObservers();
    }

    /**
     * Tell user s/he may close at any time.
     */
    public void close() {
        // Tell user s/he may close at any time?
        // Currently it will say win/lose/tie/error.
        super.setChanged();
        super.notifyObservers();
    }

    static EnumMap< Move, Character > cmap = new EnumMap<>( Board.Move.class );

    static {
        cmap = new EnumMap<>( Board.Move.class );
        cmap.put( Move.PLAYER_ONE, '0' );
        cmap.put( Move.PLAYER_TWO, 'X' );
        cmap.put( Move.NONE,       '.' );
    }

    /**
     * Returns a string representation of the board, suitable for printing out.
     * The starting board for a 4x4 game would be:<br>
     * <br><tt>
     * 0  1  2  3<br>
     * 0[.][.][.][.]<br>
     * 1[.][O][X][.]<br>
     * 2[.][X][O][.]<br>
     * 3[.][.][.][.]<br>
     * </tt>
     *
     * @return the string representation
     */
    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();

        // build the top row with column numbers
        builder.append( ' ' );
        for ( int c = 0; c < this.DIM; ++c ) {
            builder.append( " " + c + ' ' );
        }
        builder.append( '\n' );

        // build remaining rows with row numbers and column values
        for ( int r = 0; r < this.DIM; ++r ) {
            builder.append( r );
            for ( int c = 0; c < this.DIM; ++c ) {
                builder.append( '[' );
                builder.append( cmap.get( this.board[ r ][ c ] ) );
                builder.append( ']' );
            }
            builder.append( '\n' );
        }

        return builder.toString();
    }

}
