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
            startGame();
        }
    }

    public synchronized void startGame() {
        gameStarted = true;
        players.forEach(p -> p.sendMessage("GAME_START"));
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

        PlayerHandler opponent = players.get(1 - currentPlayerIndex);
        ShotResult result = opponent.getBoard().receiveShot(x, y);

        // Send results
        player.sendMessage("SHOT_RESULT:" + x + "," + y + "," + result);
        opponent.sendMessage("OPPONENT_SHOT:" + x + "," + y + "," + result);

        if (result == ShotResult.GAME_OVER) {
            endGame(player.getPlayerId());
            return;
        }

        if (result == ShotResult.MISS) {
            switchTurn();
        } else {
            player.sendMessage("TURN:" + (currentPlayerIndex + 1));
        }
    }

    private synchronized void switchTurn() {
        currentPlayerIndex = 1 - currentPlayerIndex;
        notifyCurrentPlayer();
    }

    private synchronized void notifyCurrentPlayer() {
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
