import javax.swing.*;
import java.awt.*;
import java.util.*;

public class Game {
    private Point lastPosition;
    private Point currentPosition;
    private Player p1 = new Player();
    private Player p2 = new Player();
    private boolean atEnd;

    public Game() {
        restartGame();
    }

    public void restartGame() {
        atEnd = false;
        Random r = new Random();
        //int x = r.nextInt(5);
        //int y = r.nextInt(2) + 5;
        currentPosition = new Point(2, 2);
        lastPosition = new Point(1, 1);
        p1.resetPoints();
        p2.resetPoints();
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
    }

    public boolean bounce(int millis) {
        if (millis < 200) {
            setAtEnd(false);
            bounceLeft();
        } else if (millis <= 300) {
            setAtEnd(false);
            bounceUp();
        } else if (millis <= 500 ) {
            setAtEnd(false);
            bounceRight();
        } else {
            setAtEnd(true);
            return false;
        }
        updatePosition();
        return true;
    }

    private void bounceUp() {
        if (currentPosition.y == 9) {
            setLastPosition(new Point(currentPosition.x, currentPosition.y+1));
        } else if (currentPosition.y == 0) {
            setLastPosition(new Point(currentPosition.x, currentPosition.y-1));
        }
    }

    private void bounceLeft() {
        if(currentPosition.y == 9){
            setLastPosition(new Point(currentPosition.x+1, currentPosition.y+1));
        } else if(currentPosition.y == 0){
            setLastPosition(new Point(currentPosition.x-1, currentPosition.y-1));
        }
    }

    private void bounceRight() {
        if (currentPosition.y == 9) {
            setLastPosition(new Point(currentPosition.x - 1, currentPosition.y + 1));
        } else if (currentPosition.y == 0) {
            setLastPosition(new Point(currentPosition.x + 1, currentPosition.y - 1));
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
}
