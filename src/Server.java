import java.io.*;
import java.net.*;

public class Server {
    private int ISPort;
    private int androidPort;
    private Game game = new Game();
    private String line;
    private String[][] highScore = new String[10][2];

    public Server(int ISPort, int androidPort) {
        this.ISPort = ISPort;
        this.androidPort = androidPort;
        new Thread(new Connection()).start();
        highScore[0][0] = "Test1";
        highScore[0][1] = "25";
        highScore[1][0] = "Test2";
        highScore[1][1] = "20";
        highScore[2][0] = "Test3";
        highScore[2][1] = "15";
        //new Thread(new UpdatePosition()).start();
        //new Thread(new ESConnection()).start();
        new Thread(new AndroidConnection()).start();
    }

    /**
     * Connects the server to the embedded system and starts the read/write thread.
     *
     * @author Samuel Palmhager
     */
    public class ESConnection implements Runnable {
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
                    //new Thread(new ESReader(socket)).start();
                    new Thread(new ESWriter(socket)).start();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
    /**
     * This class contains the thread which is intended to write to the embedded system
     *
     * @author Samuel Palmhager
     */
    public class ESWriter implements Runnable {
        private Socket socket;
        private PrintWriter out;
        /**
         * The constructor for the embedded system-writer which establishes a printwriter on the
         * socket-outputstream.
         *
         * @author Samuel Palmhager
         */
        public ESWriter(Socket socket) {
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
    public class ESReader implements Runnable {
        private Socket socket;
        private BufferedReader in;
        /**
         * Constructor for the embedded system-reader which establishes a bufferedreader.
         *
         * @author Samuel Palmhager
         */
        public ESReader(Socket socket) {
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
            //while(true) {
                try {
                    Thread.sleep(1000);
                    oos.writeObject(highScore);
                    System.out.println("Skrev lista till app");
                } catch (IOException | InterruptedException e) {
                    e.printStackTrace();
                    try {
                        socket.close();
                    } catch (IOException ex) {
                        e.printStackTrace();
                    }
                }
            //}
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
                    Thread.sleep(1000);
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
                        ex.printStackTrace();
                    }
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }
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
