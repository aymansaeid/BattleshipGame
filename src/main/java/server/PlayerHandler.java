package server;

import common.Board;
import common.Ship;
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
        this.board = new Board(boardSize, "Player " + playerId);
        this.out = new PrintWriter(socket.getOutputStream(), true);
        this.in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
        this.playerId = playerId;
    }

    @Override
    public void run() {
        try {
            String message;
            while ((message = in.readLine()) != null) {
                if (message.equals("READY")) {
                    ready = true;
                    if (lobby != null) {
                        lobby.checkReadyStatus();
                    }
                } else if (message.startsWith("SHOT:")) {
                    handleShot(message);
                } else if (message.startsWith("SHIPS:")) {
                    handleShipPlacement(message);
                }
            }
        } catch (IOException e) {
            System.err.println("Player " + playerId + " disconnected: " + e.getMessage());
        } finally {
            if (lobby != null) {
                // This line is the trigger for ending the game.
                lobby.removePlayer(this);
            }
            closeConnection();
        }
    }

    private void handleShot(String message) {
        if (lobby == null || !ready) {
            return;
        }

        String[] parts = message.substring(5).split(",");
        try {
            int x = Integer.parseInt(parts[0]);
            int y = Integer.parseInt(parts[1]);
            lobby.processShot(this, x, y);
        } catch (NumberFormatException e) {
            System.err.println("Invalid shot coordinates from Player " + playerId);
        }
    }

    private void handleShipPlacement(String message) {
        try {
            // Clear existing ships first
            board = new Board(board.getSize(), "Player " + playerId);

            String[] shipData = message.substring(6).split("\\|");
            for (String shipCoords : shipData) {
                if (shipCoords.isEmpty()) {
                    continue;
                }

                String[] coords = shipCoords.split(";");
                List<int[]> coordinates = new ArrayList<>();

                for (String coord : coords) {
                    if (coord.isEmpty()) {
                        continue;
                    }
                    String[] xy = coord.split(",");
                    coordinates.add(new int[]{
                        Integer.parseInt(xy[0]),
                        Integer.parseInt(xy[1])
                    });
                }

                if (!coordinates.isEmpty()) {
                    Ship ship = new Ship(coordinates.size());
                    boolean isVertical = isVertical(coordinates);

                    // Try placing the ship
                    boolean placementSuccess = false;
                    if (coordinates.size() >= 2) {
                        int x = coordinates.get(0)[0];
                        int y = coordinates.get(0)[1];
                        placementSuccess = board.placeShip(ship, x, y, isVertical);
                    }

                    if (placementSuccess) {
                        System.out.println("player "+playerId + " successfully placed ship at "
                                + coordinates.get(0)[0] + "," + coordinates.get(0)[1]
                                + " size: " + coordinates.size());
                    } else {
                        System.out.println("player "+playerId + " failed to place ship at "
                                + coordinates.get(0)[0] + "," + coordinates.get(0)[1]
                                + " size: " + coordinates.size());
                    }
                }
            }
        } catch (Exception e) {
            System.err.println("Error processing ship placement from Player "
                    + playerId + ": " + e.getMessage());
        }
    }

    private boolean isVertical(List<int[]> coords) {
        return coords.size() >= 2 && coords.get(0)[0] != coords.get(1)[0];
    }

    public void setLobby(GameLobby lobby) {
        this.lobby = lobby;
    }

    public void sendMessage(String message) {
        out.println(message);
    }

    public void closeConnection() {
        try {
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
