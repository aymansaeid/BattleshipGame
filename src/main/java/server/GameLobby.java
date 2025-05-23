package server;

import common.ShotResult;
import java.util.ArrayList;
import java.util.List;

public class GameLobby {

    private final List<PlayerHandler> players = new ArrayList<>();
    private int currentPlayerIndex = 0;
    private boolean gameStarted = false;
    private boolean gameOver = false;

    public synchronized boolean addPlayer(PlayerHandler player) {
        if (players.size() < 2 && !gameStarted) {
            players.add(player);
            player.setLobby(this);
            return true;
        }
        return false;
    }

    public synchronized void checkReadyStatus() {
        if (players.size() == 2 && players.stream().allMatch(PlayerHandler::isReady)) {
             System.out.println("Player 1 is now READY.");
        System.out.println("Player 2 is now READY.");
            startGame();
        }
    }

    public synchronized void startGame() {
        gameStarted = true;
        players.forEach(p -> p.sendMessage("GAME_START"));
        System.out.println("GAME_START");
        notifyCurrentPlayer();
    }

    public synchronized void processShot(PlayerHandler player, int x, int y) {
    if (!gameStarted || gameOver) {
        return;
    }

    int shooterIndex = players.indexOf(player);
    if (shooterIndex != currentPlayerIndex) {
        return;
    }

    System.out.println("Player " + (shooterIndex + 1) + " fired a shot at coordinates (" + x + "," + y + ")");

    PlayerHandler opponent = players.get(1 - currentPlayerIndex);
    ShotResult result = opponent.getBoard().receiveShot(x, y);

    // Send results
    player.sendMessage("SHOT_RESULT:" + x + "," + y + "," + result);
    opponent.sendMessage("OPPONENT_SHOT:" + x + "," + y + "," + result);
    
    System.out.println("Sending to Player " + (shooterIndex + 1) + ": SHOT_RESULT:" + x + "," + y + "," + result);
    System.out.println("Sending to Player " + (2 - shooterIndex) + ": OPPONENT_SHOT:" + x + "," + y + "," + result);

    if (result == ShotResult.GAME_OVER) {
        endGame(player.getPlayerId());
        return;
    }

    if (result == ShotResult.MISS) {
        switchTurn();
    } else {
        System.out.println("Sending to Player " + (shooterIndex + 1) + ": HIT_CONTINUE");
        player.sendMessage("HIT_CONTINUE");
    }
}

    private synchronized void switchTurn() {
        currentPlayerIndex = 1 - currentPlayerIndex;
        notifyCurrentPlayer();
    }

   private synchronized void notifyCurrentPlayer() {
    System.out.println("Sending to Player " + (currentPlayerIndex + 1) + ": TURN:" + (currentPlayerIndex + 1));
    players.get(currentPlayerIndex).sendMessage("TURN:" + (currentPlayerIndex + 1));
}

    public synchronized void endGame(int winnerId) {
        gameOver = true;
        players.forEach(p -> p.sendMessage("GAME_OVER:" + winnerId));
    }

    public synchronized void removePlayer(PlayerHandler disconnectedPlayer) {
        // Check if the game was active and hasn't already ended
        if (gameStarted && !gameOver) {
            System.out.println("Player " + disconnectedPlayer.getPlayerId() + " disconnected during the game.");

            // Find the remaining player, who will be the winner.
            PlayerHandler winner = null;
            for (PlayerHandler p : players) {
                if (p != disconnectedPlayer) {
                    winner = p;
                    break;
                }
            }

            // If a winner was found, end the game and declare them the victor.
            if (winner != null) {
                System.out.println("Declaring Player " + winner.getPlayerId() + " as the winner due to opponent leaving.");
                // Call the existing endGame method with the winner's ID
                endGame(winner.getPlayerId());
            }
        }

        // Finally, remove the disconnected player from the lobby's list.
        players.remove(disconnectedPlayer);
        System.out.println("Player " + disconnectedPlayer.getPlayerId() + " has been removed from the lobby.");
    }
}
