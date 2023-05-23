import java.awt.*;
import java.util.*;

/**
 * This class handles a pong game. The Game class keeps track of a ball in the form of a coordinate
 * as well as two players in the form of Player objects. It has methods for updating the position
 * and handling bounce among others. It also handles the players individual points.
 */
public class Game {
    private Point lastPosition;
    private Point currentPosition;
    private Player p1 = new Player();
    private Player p2 = new Player();
    private boolean atEnd;
    private int delay;
    private int bounceCounter;
    private Random r = new Random();
    public Game() {
        restartGame();
    }

    public void restartGame() {
        atEnd = false;
        int x = r.nextInt(5);
        int y = r.nextInt(2) + 4;
        currentPosition = new Point(x, y);
        if (y == 4) {
            lastPosition = new Point(x, 3);
        } else {
            lastPosition = new Point(x, 6);
        }
        p1.resetPoints();
        p2.resetPoints();
        p1.setWinner(false);
        p2.setWinner(false);
        p1.setName(null);
        p2.setName(null);
        p1.setPlayerNumber(1);
        p2.setPlayerNumber(2);
        delay = 750;
        bounceCounter = 0;
    }

    public Point getCurrentPosition() {
        return currentPosition;
    }

    private void setCurrentPosition(Point current) {
        currentPosition = current;
    }

    private void setLastPosition(Point last) {
        lastPosition = last;
    }

    public void updatePosition() {
        int ly = lastPosition.y;
        int cy = currentPosition.y;
        int lx = lastPosition.x;
        int cx = currentPosition.x;
        if ((ly < cy && cy < 9) || cy == 0) {
            cy++;
        } else if ((cy < ly && cy > 0) || cy == 9) {
            cy--;
        }
        if (lx < cx) {
            if (cx == 4) {
                cx--;
            } else {
                cx++;
            }
        } else if (lx > cx) {
            if (cx == 0) {
                cx++;
            } else {
                cx--;
            }
        }
        setLastPosition(currentPosition);
        setCurrentPosition(new Point(cx, cy));
        if (currentPosition.y == 0 || currentPosition.y == 9) {
            setAtEnd(true);
        }
    }

    public boolean bounce(int millis) {
        if (millis < ((delay/5)*3)) {
            setAtEnd(false);
            boolean bounceLeft = 0 == r.nextInt(2);
            if (bounceLeft) {
                bounceLeft();
            } else {
                bounceRight();
            }
        } else if (millis <= delay) {
            setAtEnd(false);
            bounceUp();
        } else {
            setAtEnd(true);
            return false;
        }
        bounceCounter++;
        if (bounceCounter % 2 == 0) {
            delay = 9*delay/10;
            System.out.println("Delay: " + delay);
        }
        updatePosition();
        return true;
    }

    private void bounceUp() {
        if (currentPosition.y == 9) {
            setLastPosition(new Point(currentPosition.x, currentPosition.y+1));
            p2.increasePoints();
        } else if (currentPosition.y == 0) {
            setLastPosition(new Point(currentPosition.x, currentPosition.y-1));
            p1.increasePoints();
        }
    }

    private void bounceLeft() {
        if(currentPosition.y == 9){
            setLastPosition(new Point(currentPosition.x+1, currentPosition.y+1));
            p2.increasePoints();
        } else if(currentPosition.y == 0){
            setLastPosition(new Point(currentPosition.x-1, currentPosition.y-1));
            p1.increasePoints();
        }
    }

    private void bounceRight() {
        if (currentPosition.y == 9) {
            setLastPosition(new Point(currentPosition.x - 1, currentPosition.y + 1));
            p2.increasePoints();
        } else if (currentPosition.y == 0) {
            setLastPosition(new Point(currentPosition.x + 1, currentPosition.y - 1));
            p1.increasePoints();
        }
    }

    public String getCurrentPositionString() {
        return currentPosition.x + "," + currentPosition.y;
    }

    public Player getP1() {
        return p1;
    }

    public Player getP2() {
        return p2;
    }

    public boolean isAtEnd() {
        return atEnd;
    }

    public void setAtEnd(boolean atEnd) {
        this.atEnd = atEnd;
        System.out.println("atEnd blir " + atEnd);
    }

    public int getDelay() {
        return delay;
    }
}
