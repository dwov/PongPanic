package Shared;

import java.io.Serializable;

public class Player implements Serializable {
    private String name;
    private int points;
    private int playerNumber;
    private static final long serialVersionUID = 1234L;
    private boolean winner = false;

    public Player() {
        resetPoints();
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public int getPoints() {
        return points;
    }

    public void resetPoints() {
        points = 0;
    }

    public int getPlayerNumber() {
        return playerNumber;
    }
    public void setPlayerNumber(int playerNumber) {
        this.playerNumber = playerNumber;
    }
    public void setPoints(int points) {
        this.points = points;
    }
    public void increasePoints() {
        points++;
    }
    public boolean isWinner() {
        return winner;
    }

    public void setWinner(boolean winner) {
        this.winner = winner;
    }
    public String[] ourToString(){
        String[] list = new String[2];
        list[0] = getName();
        list[1] = String.valueOf(getPoints());
        return list;
    }
}
