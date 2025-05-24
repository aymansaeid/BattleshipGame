package server;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class BattleshipServer {
    private static final int PORT = 12345;
    private static final int BOARD_SIZE = 10;
    private final List<GameLobby> lobbies = new ArrayList<>();
    private final List<PlayerHandler> waitingPlayers = new ArrayList<>();
    private int playerCounter = 1; // For generating unique player IDs
    
    public void startServer() {
        try (ServerSocket serverSocket = new ServerSocket(PORT)) {
            System.out.println("Server started on port " + PORT);
            
            while (true) {
                Socket clientSocket = serverSocket.accept();
                int playerId = playerCounter++;
                PlayerHandler player = new PlayerHandler(this, clientSocket, BOARD_SIZE, playerId);
                new Thread(player).start();
                System.out.println("Player " + playerId + " connected");
                addWaitingPlayer(player);
            }
        } catch (IOException e) {
            System.err.println("Server error: " + e.getMessage());
        }
    }
    
    private synchronized void addWaitingPlayer(PlayerHandler player) {
        // First clean up any disconnected players
        cleanupDisconnectedPlayers();
        
        waitingPlayers.add(player);
        if (waitingPlayers.size() >= 2) {
            createNewLobby();
        }
    }
    
    private synchronized void cleanupDisconnectedPlayers() {
        Iterator<PlayerHandler> iterator = waitingPlayers.iterator();
        while (iterator.hasNext()) {
            PlayerHandler player = iterator.next();
            if (!player.isConnected()) {
                System.out.println("Removing disconnected player " + player.getPlayerId() + " from waiting list");
                iterator.remove();
            }
        }
    }
    
    private synchronized void createNewLobby() {
        // Double-check that both players are still connected
        if (waitingPlayers.size() < 2) {
            return;
        }
        
        PlayerHandler p1 = waitingPlayers.get(0);
        PlayerHandler p2 = waitingPlayers.get(1);
        
        if (!p1.isConnected() || !p2.isConnected()) {
            // Remove disconnected players and return
            cleanupDisconnectedPlayers();
            return;
        }
        
        // Both players are connected, create the lobby
        GameLobby lobby = new GameLobby();
        waitingPlayers.remove(p1);
        waitingPlayers.remove(p2);
        
        lobby.addPlayer(p1);
        lobby.addPlayer(p2);
        lobbies.add(lobby);
        
        p1.sendMessage("PLAYER_ID:1");
        p2.sendMessage("PLAYER_ID:2");
        
        lobby.startGame();
        System.out.println("New game lobby created with Player " + 
            p1.getPlayerId() + " and Player " + p2.getPlayerId());
    }
    
    public synchronized void removeLobby(GameLobby lobby) {
        lobbies.remove(lobby);
    }
    
    public synchronized void playerDisconnected(PlayerHandler player) {
        // Remove from waiting list if present
        waitingPlayers.remove(player);
        System.out.println("Player " + player.getPlayerId() + " disconnected");
    }
    
    public static void main(String[] args) {
        new BattleshipServer().startServer();
    }
}