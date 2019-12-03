package reversi.server;

import reversi.Reversi;
import reversi.ReversiException;

/**
 * The server side representation of the game state and players in the game.
 *
 * @author Robert St Jacques @ RIT SE
 * @author Sean Strout @ RIT CS
 */
public class ReversiGame {
    /** first player */
    private ReversiPlayer playerOne;
    /** second player */
    private ReversiPlayer playerTwo;
    /** the game board */
    private Reversi game;

    /**
     * Create the server side game.
     *
     * @param DIM squaew dimension of board
     * @param playerOne first player
     * @param playerTwo second player
     */
    public ReversiGame(int DIM, ReversiPlayer playerOne, ReversiPlayer playerTwo) {
        this.playerOne = playerOne;
        this.playerTwo = playerTwo;
        this.game = new Reversi(DIM);
    }

    /**
     * Conduct the gameplay.
     */
    public void run() {
        boolean go = true;
        while(go) {
            try {
                if(makeMove(this.playerOne, this.playerTwo)) {
                    go = false;
                }
                else if(makeMove(this.playerTwo, this.playerOne)) {
                    go = false;
                }
            }
            catch(ReversiException e) {
                this.playerOne.error(e.getMessage());
                this.playerTwo.error(e.getMessage());
                go = false;
            }
        }

        this.playerOne.close();
        this.playerTwo.close();
    }

    /**
     * Make a single move in the game.
     *
     * @param turn the player whose turn it currently is
     * @param other the other player
     * @return whether the game ended or not on this move
     * @throws ReversiException if there is any game playing problem
     */
    private boolean makeMove(ReversiPlayer turn, ReversiPlayer other)
        throws ReversiException {

        // get the move from the player whose turn it is
        int[] coord = turn.makeMove();
        this.game.makeMove(coord[0], coord[1]);

        // communicate the move to both players
        turn.moveMade(coord[0], coord[1]);
        other.moveMade(coord[0], coord[1]);

        // check if the game ended on this move
        if(this.game.gameOver()) {
            // determine winner
            switch (this.game.getWinner()) {
                case NONE:
                    turn.gameTied();
                    other.gameTied();
                    break;
                case PLAYER_ONE:
                    other.gameWon();
                    turn.gameLost();
                    break;
                case PLAYER_TWO:
                    turn.gameWon();
                    other.gameLost();
            }
            return true;
        }
        else {
            return false;
        }
    }
}
