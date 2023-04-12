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
        currentPosition = new Point(3, 2);
        lastPosition = new Point(4, 3);
    }

    public Point getLastPosition() {
        return lastPosition;
    }

    public Point getCurrentPosition() {
        return currentPosition;
    }

    private void setCurrentPosition(Point current) {
        lastPosition = this.currentPosition;
        this.currentPosition = current;
    }

    public void updatePosition() {
        int ly = lastPosition.y;
        int cy = currentPosition.y;
        int lx = lastPosition.x;
        int cx = currentPosition.x;
        if ((ly < cy && cy<9) | cy == 0) {
            cy++;
        } else if((cy < ly && cy>0) | cy == 9) {
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
        setCurrentPosition(new Point(cx, cy));
        if (cy == 0 || cy == 9) {
            atEnd = true;
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
    }

    public void updatePosition2() {
        int ly = lastPosition.y;
        int cy = currentPosition.y;
        int lx = lastPosition.x;
        int cx = currentPosition.x;
        if ((ly < cy && cy<4) | cy == 0) {
            cy++;
        } else if((cy < ly && cy>0) | cy == 4) {
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
        setCurrentPosition(new Point(cx, cy));
        if (cy == 0 || cy == 4) {
            atEnd = true;
        }
    }

}
