import Shared.Player;

import java.awt.*;
import java.util.*;

public class Game {
    private Point lastPosition;
    private Point currentPosition;
    private Player p1 = new Player();
    private Player p2 = new Player();
    private boolean atEnd;
    private int delay;
    private int bounceCounter;

    public Game() {
        restartGame();
    }

    public void restartGame() {
        atEnd = false;
        Random r = new Random();
        //int x = r.nextInt(5);
        //int y = r.nextInt(2) + 5;
        currentPosition = new Point(3, 3);
        lastPosition = new Point(4, 4);
        p1.resetPoints();
        p2.resetPoints();
        p1.setWinner(false);
        p2.setWinner(false);
        delay = 1000;
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
        if (millis < ((delay/5)*2)) {
            setAtEnd(false);
            bounceLeft();
        } else if (millis <= ((delay/5)*3)) {
            setAtEnd(false);
            bounceUp();
        } else if (millis <= delay) {
            setAtEnd(false);
            bounceRight();
        } else {
            setAtEnd(true);
            return false;
        }
        bounceCounter++;
        if (bounceCounter % 10 == 0) {
            delay/=2;
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
