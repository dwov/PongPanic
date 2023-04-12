public class Player {
    private String name;
    private int points;

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

    public void increasePoints() {
        points++;
    }
}
