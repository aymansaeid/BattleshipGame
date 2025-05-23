package server;

import common.Board;
import common.Ship; // Assuming Ship class might be needed by Board, though not directly here
import java.io.*;
import java.net.Socket;
import java.util.List;
import java.util.ArrayList;

public class PlayerHandler implements Runnable {
    private final BattleshipServer server;
    private final Socket socket;
    private Board board;
    private final PrintWriter out;
    private final BufferedReader in;
    private GameLobby lobby;
    private final int playerId;
    private boolean ready = false;
    
    public PlayerHandler(BattleshipServer server, Socket socket, int boardSize, int playerId) throws IOException {
        this.server = server;
        this.socket = socket;
        this.board = new Board(boardSize, "Player " + playerId); // boardSize is used here
        this.out = new PrintWriter(socket.getOutputStream(), true);
        this.in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
        this.playerId = playerId;
    }
    
    @Override
    public void run() {
        try {
            String message;
            // Send player ID upon connection
            sendMessage("PLAYER_ID:" + playerId);

            while ((message = in.readLine()) != null) {
                System.out.println("Received from Player " + playerId + ": " + message); // Generic log for all incoming messages
                if (message.equals("READY")) {
                    ready = true;
                    System.out.println("Player " + playerId + " is now READY.");
                    if (lobby != null) {
                        lobby.checkReadyStatus();
                    }
                } 
                else if (message.startsWith("SHOT:")) {
                    handleShot(message);
                }
                else if (message.startsWith("SHIPS:")) {
                    handleShipPlacement(message);
                }
                // Handle other messages if any
            }
        } catch (IOException e) {
            System.err.println("Player " + playerId + " disconnected: " + e.getMessage());
        } finally {
            if (lobby != null) {
                lobby.removePlayer(this);
                System.out.println("Player " + playerId + " removed from lobby.");
            }
            closeConnection();
        }
    }
    
    private void handleShot(String message) {
        if (lobby == null || !ready) {
            System.out.println("Player " + playerId + " attempted to shoot but is not in a ready lobby. Message: " + message);
            sendMessage("ERROR:Cannot shoot. Not ready or not in a game."); // Optional feedback
            return;
        }
        
        // Message format is expected to be "SHOT:x,y"
        String[] parts = message.substring(5).split(",");
        try {
            if (parts.length < 2) {
                System.err.println("Malformed SHOT message (not enough parts) from Player " + playerId + ": " + message);
                sendMessage("ERROR:Malformed shot command."); // Optional feedback
                return;
            }
            int x = Integer.parseInt(parts[0].trim()); // trim to handle potential spaces
            int y = Integer.parseInt(parts[1].trim()); // trim to handle potential spaces
            
            // Log the shot attempt by the player
            System.out.println("Player " + playerId + " fired a shot at coordinates (" + x + "," + y + ").");
            
            // The lobby will process the shot, including turn validation and game logic
            lobby.processShot(this, x, y); 
            
        } catch (NumberFormatException e) {
            System.err.println("Invalid shot coordinates (NumberFormatException) from Player " + playerId + " in message: " + message);
            sendMessage("ERROR:Invalid shot coordinates format."); // Optional feedback
        } catch (ArrayIndexOutOfBoundsException e) {
            System.err.println("Malformed SHOT message (ArrayIndexOutOfBoundsException) from Player " + playerId + ": " + message);
            sendMessage("ERROR:Malformed shot command structure."); // Optional feedback
        }
    }
    
    private void handleShipPlacement(String message) {
        System.out.println("Player " + playerId + " is attempting to place ships.");
        try {
            // Clear existing ships first
            // Re-initialize board for this player if placements are being re-sent
            this.board = new Board(this.board.getSize(), "Player " + playerId); 
            
            String[] shipData = message.substring(6).split("\\|"); // SHIPS:data
            int shipsSuccessfullyPlaced = 0;
            for (String shipCoordsString : shipData) {
                if (shipCoordsString.isEmpty()) continue;
                
                String[] singleShipAllCoords = shipCoordsString.split(";");
                List<int[]> coordinatesList = new ArrayList<>();
                
                for (String coordPairStr : singleShipAllCoords) {
                    if (coordPairStr.isEmpty()) continue;
                    String[] xyStr = coordPairStr.split(",");
                    if (xyStr.length == 2) {
                        coordinatesList.add(new int[]{
                            Integer.parseInt(xyStr[0].trim()),
                            Integer.parseInt(xyStr[1].trim())
                        });
                    }
                }
                
                if (!coordinatesList.isEmpty()) {
                    Ship ship = new Ship(coordinatesList.size()); // Ship size based on number of coordinates
                    // Determine start coordinates and orientation
                    // This logic assumes the client sends all coordinates for a ship.
                    // The Board.placeShip usually takes startX, startY, size, and isVertical.
                    // We need to infer these from the coordinatesList or ensure client sends compatible format.
                    // For simplicity, let's assume the first coordinate is the start and try to infer orientation.
                    
                    int startX = coordinatesList.get(0)[0];
                    int startY = coordinatesList.get(0)[1];
                    boolean vertical = false; // Default to horizontal
                    if (coordinatesList.size() > 1) {
                        if (coordinatesList.get(0)[0] != coordinatesList.get(1)[0]) { // X changes, so it's vertical
                            vertical = true;
                        }
                    }
                    
                    // The common.Ship probably doesn't need explicit coordinates list for placement if Board.placeShip handles it
                    if (board.placeShip(ship, startX, startY, vertical)) {
                        System.out.println(playerId + " successfully placed a ship of size " +
                                           ship.getSize() + " starting at (" + startX + "," + startY +
                                           ") " + (vertical ? "vertically." : "horizontally."));
                        shipsSuccessfullyPlaced++;
                    } else {
                        System.out.println(playerId + " failed to place a ship of size " +
                                           ship.getSize() + " starting at (" + startX + "," + startY +
                                           ") " + (vertical ? "vertically." : "horizontally.") + ". Overlap or out of bounds.");
                    }
                }
            }
            System.out.println(playerId + " finished ship placement phase. Successfully placed " + shipsSuccessfullyPlaced + " ships.");
        } catch (Exception e) {
            System.err.println("Error processing ship placement from Player " + 
                playerId + ": " + e.getMessage());
            e.printStackTrace(); // For more detailed error diagnosis
        }
    }

    // This isVertical logic might be specific to how you parse ship coordinates
    // The one in handleShipPlacement is a bit more robust if all coords are sent.
    private boolean isVertical(List<int[]> coords) {
        if (coords == null || coords.size() < 2) return false; // Default or cannot determine
        return coords.get(0)[0] != coords.get(1)[0]; // If X changes, it's vertical
    }
    
    public void setLobby(GameLobby lobby) {
        this.lobby = lobby;
    }
    
    public void sendMessage(String message) {
        if (message != null) {
            // Log turn notifications specifically
            // These message contents ("YOUR_TURN", "OPPONENT_TURN") are assumptions.
            // Adjust them if your actual turn notification messages are different.
            if (message.equals("YOUR_TURN")) {
                System.out.println("Server to Player " + playerId + ": Notifying it is YOUR turn.");
            } else if (message.equals("OPPONENT_TURN")) {
                System.out.println("Server to Player " + playerId + ": Notifying it is OPPONENT's turn.");
            }
            // You might also have other game state messages to log if desired.
            // e.g., if (message.startsWith("GAME_OVER")) { ... }
        }
        out.println(message);
    }
    
    public void closeConnection() {
        try {
            System.out.println("Closing connection for Player " + playerId + ".");
            if (socket != null && !socket.isClosed()) {
                socket.close();
            }
        } catch (IOException e) {
            System.err.println("Error closing connection for Player " + playerId + ": " + e.getMessage());
        }
    }
    
    public Board getBoard() {
        return board;
    }
    
    public int getPlayerId() {
        return playerId;
    }
    
    public boolean isReady() {
        return ready;
    }
}