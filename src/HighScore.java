import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Scanner;

/**
 * This class handles a list of a maximum of ten high scores.
 * @author Tilde Lundqvist
 */
public class HighScore {
    private String[][] highScore = new String[10][2];
    private int nbrHighScores;

    /**
     * Creates a HighScore object, reads scores from file,
     * and calculates the number of scores currently in the list.
     */
    public HighScore() {
        readFromFile();
        int sum = 0;
        for (int i = 0; i < highScore.length; i++) {
            if (highScore[i][1] != null) {
                sum++;
            }
        }
        nbrHighScores = sum;
    }

    /**
     * Saves high score list to file.
     */
    private void writeToFile() {
        try {
            FileWriter writer = new FileWriter("src/HighScore.txt");
            for (int i = 0; i < nbrHighScores; i++) {
                writer.write(highScore[i][0] + " ");
                writer.write(highScore[i][1]);
                if (i != nbrHighScores-1) {
                    writer.write("\n");
                }
            }
            writer.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Reads high score list from file.
     */
    private void readFromFile() {
        try {
            File file = new File("src/HighScore.txt");
            highScore = new String[10][2];
            Scanner reader = new Scanner(file);
            int counter = 0;
            while (reader.hasNextLine() && counter != 10) {
                highScore[counter][0] = reader.next();
                highScore[counter][1] = reader.next();
                counter++;
            }
            reader.close();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
    }

    /**
     * Sorts the high score list.
     */
    private void firstSortHighScoreList() {
        boolean swapped = true;
        int j = 0;
        String tempName;
        int tempPoints;
        while (swapped) {
            swapped = false;
            j++;
            for (int i = 0; i < highScore.length - j; i++) {
                if (highScore[i][1] != null && highScore[i + 1][1] != null && Integer.parseInt(highScore[i][1]) <= Integer.parseInt(highScore[i + 1][1])) {
                    tempName = highScore[i][0];
                    tempPoints = Integer.parseInt(highScore[i][1]);
                    highScore[i][0] = highScore[i + 1][0];
                    highScore[i + 1][0] = tempName;
                    highScore[i][1] = highScore[i + 1][1];
                    highScore[i + 1][1] = tempPoints + "";
                    swapped = true;
                }
            }
        }
    }

    /**
     * Inserts the last high score in the list to its correct position.
     */
    private void insertLastHighScore() {
        String[][] score = new String[1][2];
        score[0][0] = highScore[nbrHighScores-1][0];
        score[0][1] = highScore[nbrHighScores-1][1];
        for (int i = 0; i < nbrHighScores; i++) {
            if (Integer.parseInt(score[0][1]) >= Integer.parseInt(highScore[i][1])) {
                for (int j = nbrHighScores-1; j > i; j--) {
                    highScore[j][0] = highScore[j-1][0];
                    highScore[j][1] = highScore[j-1][1];
                }
                highScore[i][0] = score[0][0];
                highScore[i][1] = score[0][1];
                return;
            }
        }
    }

    /**
     * If qualified, adds score last in high score list, inserts to correct position, and writes to file.
     * @param score the score to insert
     */
    public void addHighScore(String[][] score) {
        if (nbrHighScores == 10) {
            if (Integer.parseInt(highScore[9][1]) <= Integer.parseInt(score[0][1])) {
                highScore[9][0] = score[0][0];
                highScore[9][1] = score[0][1];
            } else {
                return;
            }
        } else {
            highScore[nbrHighScores][0] = score[0][0];
            highScore[nbrHighScores][1] = score[0][1];
            nbrHighScores++;
        }
        insertLastHighScore();
        writeToFile();
    }

    /**
     * Returns the high score list as a string array.
     * @return a string array
     */
    public String[][] getHighScore() {
        return highScore;
    }
}
