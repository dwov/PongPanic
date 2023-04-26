public class Player {
    private String name;
    private int points;
    private int playerNumber;

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

    public void increasePoints5() {
        points+=5;
    }
    public void increasePoints10() {
        points+=10;
    }
}
