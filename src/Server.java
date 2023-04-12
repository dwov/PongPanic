import java.io.*;
import java.net.*;
import java.util.*;

public class Server {
    private int ISPort;
    private int androidPort;
    private String[][] highScore = new String[10][2];
    private Game game = new Game();
    private String line;

    public Server(int ISPort, int androidPort) {
        this.ISPort = ISPort;
        this.androidPort = androidPort;
        //new Thread(new Connection()).start();
        new Thread(new UpdatePosition()).start();
        new Thread(new ISConnection()).start();
        new Thread(new AndroidConnection()).start();
    }

    /**
     * Connects the server to the embedded system and starts the read/write thread.
     *
     * @author Samuel Palmhager
     */
    public class ISConnection implements Runnable {
        @Override
        public void run() {
            Socket socket;
            try {
                ServerSocket serverSocket = new ServerSocket(ISPort);
                System.out.println("IS-Server startad");
                System.out.println(game.getCurrentPositionString());

                while (true) {
                    socket = serverSocket.accept();
                    System.out.println("IS-Klient ansluten");
                    //new Thread(new ISReader(socket)).start();
                    new Thread(new ISWriter(socket)).start();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
    /**
     * This class contains the thread which is intended to write to the embedder system
     *
     * @author Samuel Palmhager
     */
    public class ISWriter implements Runnable {
        private Socket socket;
        private PrintWriter out;
        /**
         * The constructor for the embedded system-writer which establishes a printwriter on the
         * socket-outputstream.
         *
         * @author Samuel Palmhager
         */
        public ISWriter(Socket socket) {
            this.socket = socket;
            try {
                out = new PrintWriter(socket.getOutputStream(), true);
            } catch (IOException e) {
                e.printStackTrace();
                try {
                    socket.close();
                } catch (IOException ex) {
                    e.printStackTrace();
                }
            }
        }

        /**
         * The thread which is intended to write coordinates to the embedded system.
         *
         * @author Samuel Palmhager
         */
        @Override
        public void run() {
            while(true) {
                try {
                    Thread.sleep(1000);
                    out.println(game.getCurrentPositionString());
                    System.out.println(game.getCurrentPositionString());
                } catch (InterruptedException e) {
                    e.printStackTrace();
                    try {
                        socket.close();
                    } catch (IOException ex) {
                        e.printStackTrace();
                    }
                }
            }
        }
    }
    /**
     * This class is meant for reading Strings from the embedded system
     *
     * @author Samuel Palmhager
     */
    public class ISReader implements Runnable {
        private Socket socket;
        private BufferedReader in;
        /**
         * Constructor for the embedded system-reader which establishes a bufferedreader.
         *
         * @author Samuel Palmhager
         */
        public ISReader(Socket socket) {
            this.socket = socket;
            try {
                in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            } catch (IOException e) {
                e.printStackTrace();
                try {
                    socket.close();
                } catch (IOException ex) {
                    e.printStackTrace();
                }
            }
        }
        /**
         * The thread which is intended to read a String from the embedded system
         *
         * @author Samuel Palmhager
         */
        @Override
        public void run() {
            while(true) {
                try {
                    line = in.readLine();
                    System.out.println("Läste sträng: " + line);
                } catch (IOException e) {
                    e.printStackTrace();
                    try {
                        socket.close();
                    } catch (IOException ex) {
                        e.printStackTrace();
                    }
                }
            }
        }
    }

    /**
     * This class is meant to establish a connection between the server and the client.
     *
     * @author Samuel Palmhager
     */
    public class AndroidConnection implements Runnable {

        /**
         *The thread which sets up the connection and also starts the threads for reader/writing
         * to the client.
         *
         * @author Samuel Palmhager
         */
        @Override
        public void run() {
            readHighScore();
            Socket socket;
            try {
                ServerSocket serverSocket = new ServerSocket(androidPort);
                System.out.println("Android-server startad");
                while(true) {
                    socket = serverSocket.accept();
                    System.out.println("Android-klient ansluten");
                    new Thread(new AndroidReader(socket)).start();
                    new Thread(new AndroidWriter(socket)).start();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * This class is meant to write to the client.
     *
     * @author Samuel Palmhager
     */
    public class AndroidWriter implements Runnable {
        private Socket socket;
        private ObjectOutputStream oos;

        /**
         * Constructor for the writer class which sets up an objectoutputstream.
         *
         * @author Samuel Palmhager
         */
        public AndroidWriter(Socket socket) {
            this.socket = socket;
            try {
                oos = new ObjectOutputStream(socket.getOutputStream());
            } catch (IOException e) {
                e.printStackTrace();
                try {
                    socket.close();
                } catch (IOException ex) {
                    e.printStackTrace();
                }
            }
        }

        /**
         * Thread which is intended to write the highscorelist to the client.
         *
         * @author Samuel Palmhager
         */

        @Override
        public void run() {
            while(true) {
                try {
                    Thread.sleep(1000);
                    oos.writeObject("Server skriver till app");
                    System.out.println("Skrev objekt till app");
                } catch (IOException | InterruptedException e) {
                    e.printStackTrace();
                    try {
                        socket.close();
                    } catch (IOException ex) {
                        e.printStackTrace();
                    }
                }
            }
        }
    }

    /**
     * The class which is intended to read objects from the client.
     *
     * @author Samuel Palmhager
     */
    public class AndroidReader implements Runnable {
        private Socket socket;
        private ObjectInputStream ois;

        /**
         * Constructor for the client-reader which establishes an objectinputstream.
         *
         * @author Samuel Palmhager
         */
        public AndroidReader(Socket socket) {
            this.socket = socket;
            try {
                ois = new ObjectInputStream(socket.getInputStream());
            } catch (IOException e) {
                e.printStackTrace();
                try {
                    socket.close();
                } catch (IOException ex) {
                    e.printStackTrace();
                }
            }
        }
        /**
         * This thread is meant to read eventual usernames from the client.
         *
         * @author Samuel Palmhager
         */
        @Override
        public void run() {
            String name;
            while(true) {
                try {
                    name = (String) ois.readObject();
                    System.out.println("Player 1: " + name);
                    /*game.getP1().setName(name);

                    name = (String) ois.readObject();
                    System.out.println("Player 2: " + name);
                    game.getP2().setName(name);*/
                } catch (IOException | ClassNotFoundException e) {
                    e.printStackTrace();
                    try {
                        socket.close();
                    } catch (IOException ex) {
                        e.printStackTrace();
                    }
                }
            }
        }
    }



    /**
     * Saves high score list to file.
     */
    private void writeHighScore() {
        try {
            FileWriter writer = new FileWriter("HighScore.txt");
            int nbrHighScores = getNbrHighScores();
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
    private void readHighScore() {
        try {
            File file = new File("src/HighScore.txt");
            highScore = new String[10][2];
            Scanner reader = new Scanner(file);
            int counter = 0;
            while (reader.hasNextLine()) {
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
    private void sortHighScores() {
        boolean swapped = true;
        int j = 0;
        String tempName;
        int tempPoints;
        while (swapped) {
            swapped = false;
            j++;
            for (int i = 0; i < highScore.length - j; i++) {
                if (highScore[i][1] != null && highScore[i + 1][1] != null && Integer.parseInt(highScore[i][1]) >= Integer.parseInt(highScore[i + 1][1])) {
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
     * Finds and returns the lowest high score.
     * @return an int
     */
    private int getLowestHighScore() {
        for (int i = highScore.length-1; i >= 0; i--) {
            if (highScore[i][1] != null) {
                return Integer.parseInt(highScore[i][1]);
            }
        }
        return 1;
    }

    /**
     * Calculates and returns the number of high scores currently on the list.
     * @return an int
     */
    private int getNbrHighScores() {
        int sum = 0;
        for (int i = 0; i < highScore.length; i++) {
            if (highScore[i][1] != null) {
                sum++;
            }
        }
        return sum;
    }

    public class Connection implements Runnable {

        @Override
        public void run() {
            Socket socket;
            try {
                ServerSocket serverSocket = new ServerSocket(4567);
                System.out.println("Testserver startad");
                while(true) {
                    socket = serverSocket.accept();
                    System.out.println("Testklient ansluten");
                    new Thread(new Writer(socket)).start();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public class Writer implements Runnable {
        private Socket socket;
        private ObjectOutputStream oos;

        /**
         * Constructor for the writer class which sets up an objectoutputstream.
         *
         * @author Samuel Palmhager
         */
        public Writer(Socket socket) {
            this.socket = socket;
            try {
                oos = new ObjectOutputStream(socket.getOutputStream());

            } catch (IOException e) {
                e.printStackTrace();
                try {
                    socket.close();
                } catch (IOException ex) {
                    e.printStackTrace();
                }
            }
        }

        /**
         * Thread which is intended to write the highscorelist to the client.
         *
         * @author Samuel Palmhager
         */

        @Override
        public void run() {
            while(true) {
                try {
                    Thread.sleep(1000);
                    oos.writeObject(game.getCurrentPositionString());
                    System.out.println(game.getCurrentPositionString());
                } catch (IOException | InterruptedException e) {
                    e.printStackTrace();
                    try {
                        socket.close();
                    } catch (IOException ex) {
                        e.printStackTrace();
                    }
                }
            }
        }
    }

    public class UpdatePosition implements Runnable {

        @Override
        public void run() {
            while(true) {
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
                game.updatePosition();
            }
        }
    }
}
