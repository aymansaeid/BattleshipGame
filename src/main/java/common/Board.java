package common;

import java.util.ArrayList;
import java.util.List;

public class Board {
    private final int size;
    private final Cell[][] grid;
    private final List<Ship> ships;
    private int shipsSunk;
    private int totalHits; // Track total hits for debugging
    private int playerId;

    public Board(int size, int playerId) {
        this.size = size;
        this.grid = new Cell[size][size];
        this.ships = new ArrayList<>();
        this.shipsSunk = 0;
        this.totalHits = 0;
        this.playerId = playerId;

        // Initialize grid
        for (int i = 0; i < size; i++) {
            for (int j = 0; j < size; j++) {
                grid[i][j] = new Cell(i, j);
            }
        }
    }

   public boolean placeShip(Ship ship, int x, int y, boolean isVertical) {
    System.out.println("Player " + " attempting to place ship at " + x + "," + y + 
        " size " + ship.getSize() + " vertical: " + isVertical);
    
    if (!canPlaceShip(ship, x, y, isVertical)) {
        System.out.println("Player "+ " failed - invalid position");
        return false;
    }
    
    if (isVertical) {
        for (int i = 0; i < ship.getSize(); i++) {
            grid[x + i][y].setShip(ship);
            ship.addCoordinate(x + i, y);
            System.out.println("Player " + " placed ship part at " + (x + i) + "," + y);
        }
    } else {
        for (int i = 0; i < ship.getSize(); i++) {
            grid[x][y + i].setShip(ship);
            ship.addCoordinate(x, y + i);
            System.out.println("Player " + " placed ship part at " + x + "," + (y + i));
        }
    }
    
    ships.add(ship);
    System.out.println("Player " + " successfully placed entire ship");
    return true;
}

    private boolean canPlaceShip(Ship ship, int x, int y, boolean isVertical) {
    System.out.println("Player" + " checking placement at " + x + "," + y + 
        " size " + ship.getSize() + " vertical: " + isVertical);
    
    if (isVertical) {
        if (x + ship.getSize() > size) {
            System.out.println("Player " + " placement failed - out of bounds vertically");
            return false;
        }
        for (int i = 0; i < ship.getSize(); i++) {
            if (grid[x + i][y].hasShip()) {
                System.out.println("Player " + " placement failed - collision at " + (x + i) + "," + y);
                return false;
            }
        }
    } else {
        if (y + ship.getSize() > size) {
            System.out.println("Player " + " placement failed - out of bounds horizontally");
            return false;
        }
        for (int i = 0; i < ship.getSize(); i++) {
            if (grid[x][y + i].hasShip()) {
                System.out.println("Player " + " placement failed - collision at " + x + "," + (y + i));
                return false;
            }
        }
    }

    System.out.println("Player placement valid");
    return true;
}
    public ShotResult receiveShot(int x, int y) {
        // Validate coordinates
        if (x < 0 || x >= size || y < 0 || y >= size) {
            return ShotResult.INVALID;
        }
        
        Cell cell = grid[x][y];
        
        // Check if already hit
        if (cell.isHit()) {
            return ShotResult.ALREADY_HIT;
        }
        
        cell.setHit(true);
        totalHits++;
        
        // Check for ship hit
        if (cell.hasShip()) {
            Ship ship = cell.getShip();
            ship.hit();
            
            // Check if ship sunk
            if (ship.isSunk()) {
                shipsSunk++;
                System.out.printf("Ship sunk! %d/%d ships remaining%n", 
                    ships.size() - shipsSunk, ships.size());
                
                // Check if all ships sunk
                if (shipsSunk == ships.size()) {
                    System.out.println("ALL SHIPS SUNK! GAME OVER");
                    return ShotResult.GAME_OVER;
                }
                return ShotResult.SUNK;
            }
            return ShotResult.HIT;
        }
        
        return ShotResult.MISS;
    }

    public boolean allShipsSunk() {
        return !ships.isEmpty() && shipsSunk >= ships.size();
    }

    // Debug method
    public void printBoard() {
        for (int i = 0; i < size; i++) {
            for (int j = 0; j < size; j++) {
                Cell cell = grid[i][j];
                char c = cell.hasShip() ? 'S' : '.';
                if (cell.isHit()) {
                    c = cell.hasShip() ? 'X' : 'O';
                }
                System.out.print(c + " ");
            }
            System.out.println();
        }
    }

    public int getSize() { return size; }
    public Cell getCell(int x, int y) { return grid[x][y]; }
    public List<Ship> getShips() { return new ArrayList<>(ships); }
    public int getTotalHits() { return totalHits; }
}