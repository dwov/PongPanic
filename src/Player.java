import java.io.Serializable;

/**
 * This class handles a player. It keeps track of name, points, player number as winner status.
 *
 * @author Tilde Lundqvist & Samuel Palmhager
 */
public class Player {
    private String name;
    private int points;
    private int playerNumber;
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

    public void increasePoints() {
        points++;
        System.out.println("Spelare: " + name + "\nPoäng: " + points);
    }

    public void setWinner(boolean winner) {
        this.winner = winner;
    }

    public String[] toStringArray(){
        String[] list = new String[4];
        list[0] = String.valueOf(playerNumber);
        list[1] = name;
        list[2] = String.valueOf(points);
        list[3] = String.valueOf(winner);
        return list;
    }
}
