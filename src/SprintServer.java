import java.io.*;
import java.net.*;
import java.util.*;

public class SprintServer {
    private int ESPort;
    private int androidPort;
    private Game game = new Game();
    private String[][] highScore = new String[10][2];
    private Buffer<String> stringBuffer = new Buffer<>();
    private Thread gameThread;
    private Thread ESWriterThread;
    private Thread ESReaderThread;
    private Thread androidWriterThread;
    private Thread androidReaderThread;

    public SprintServer(int ESPort, int androidPort){
        this.ESPort = ESPort;
        this.androidPort = androidPort;

        populateHighscoreList();

        new Thread(new ESConnection()).start();
        new Thread(new AndroidConnection()).start();
    }

    private void populateHighscoreList() {
        highScore[0][0] = "Test1";
        highScore[0][1] = "55";
        highScore[1][0] = "Test2";
        highScore[1][1] = "50";
        highScore[2][0] = "Test3";
        highScore[2][1] = "45";
        highScore[3][0] = "Test4";
        highScore[3][1] = "40";
        highScore[4][0] = "Test5";
        highScore[4][1] = "35";
        highScore[5][0] = "Test6";
        highScore[5][1] = "30";
        highScore[6][0] = "Test7";
        highScore[6][1] = "25";
        highScore[7][0] = "Test8";
        highScore[7][1] = "20";
        highScore[8][0] = "Test9";
        highScore[8][1] = "15";
        highScore[9][0] = "Test10";
        highScore[9][1] = "10";
    }

    public class ESConnection implements Runnable {
        @Override
        public void run() {
            Socket socket = null;
            ServerSocket serverSocket = null;
            try {
                serverSocket = new ServerSocket(ESPort);
                System.out.println("Server startad");
                while (true) {
                    socket = serverSocket.accept();
                    System.out.println("Klient ansluten: " + socket.getInetAddress());

                    gameThread = new Thread(new GameThread());
                    gameThread.start();

                    ESReaderThread = new Thread(new ESReader(socket));
                    ESReaderThread.start();

                    ESWriterThread = new Thread(new ESWriter(socket));
                    ESWriterThread.start();
                }
            } catch (IOException e) {
                e.printStackTrace();
            } finally {
                if (serverSocket != null) {
                    try {
                        serverSocket.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }
        }
    }

    public class ESWriter implements Runnable {
        private Socket socket;
        private PrintWriter out;
        public ESWriter(Socket socket){
            this.socket = socket;
            try{
                out = new PrintWriter(socket.getOutputStream(), true);
            } catch (IOException e) {
                e.getStackTrace();
            }
        }
        @Override
        public void run() {
            try {
                while (true) {
                    String string = stringBuffer.get();
                    out.println(string);
                    System.out.println("Skrev koordinat: " + string);
                }
            } catch (InterruptedException e) {
                System.out.println("buffern blev interrupted");
            } finally {
                try {
                    out.close();
                    socket.close();
                    System.out.println("stänger writer och socket");
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    public class ESReader implements Runnable {
        private Socket socket;
        private BufferedReader in;
        private java.util.Timer timer = new java.util.Timer();

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

        @Override
        public void run() {
            try {
                String inputLine;
                while (true) {
                    try {
                        inputLine = in.readLine();
                    } catch (SocketException e) {
                        System.out.println("socket exception");
                        break;
                    }
                    if (inputLine.equals("heartbeat")) {
                        System.out.println("Läste heartbeat: " + inputLine);
                        timer.cancel();
                        timer = new java.util.Timer();
                        timer.schedule(new TimerTask() {
                            @Override
                            public void run() {
                                try {
                                    gameThread.interrupt();
                                    ESWriterThread.interrupt();
                                    in.close();
                                    socket.close();
                                } catch (IOException e) {
                                    e.printStackTrace();
                                }
                            }
                        }, 5000);
                    }
                }
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
    public class GameThread implements Runnable {
        @Override
        public void run() {
            try {
                while(!gameThread.isInterrupted()) {
                    Thread.sleep(500);
                    game.updatePosition();
                    stringBuffer.put(game.getCurrentPositionString());
                }
            } catch (InterruptedException e) {
                game.restartGame();
                System.out.println("spel startas om");
            }
        }
    }

    public class AndroidConnection implements Runnable {

        @Override
        public void run() {
            Socket socket;
            try {
                ServerSocket serverSocket = new ServerSocket(androidPort);
                System.out.println("Android-server startad");
                while(true) {
                    socket = serverSocket.accept();
                    System.out.println("Android-klient ansluten");
                    androidReaderThread = new Thread(new AndroidReader(socket));
                    androidReaderThread.start();
                    androidWriterThread = new Thread(new AndroidWriter(socket));
                    androidWriterThread.start();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public class AndroidWriter implements Runnable {
        private Socket socket;
        private ObjectOutputStream oos;

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

        @Override
        public void run() {
            try {
                try {
                    oos.writeObject(highScore);
                    System.out.println("Skrev lista till app");
                } catch (SocketException e) {
                    oos.close();
                    socket.close();
                    System.out.println("stängde oos och socket");
                }
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

    public class AndroidReader implements Runnable {
        private Socket socket;
        private ObjectInputStream ois;

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

        @Override
        public void run() {
            String name;
            while(!androidReaderThread.isInterrupted()) {
                try {
                    name = (String) ois.readObject();
                    System.out.println("Player 1: " + name);
                } catch (IOException e) {
                    System.out.println("readObject interrupted");
                    try {
                        androidWriterThread.interrupt();
                        androidReaderThread.interrupt();
                        System.out.println("trådar interrupted");
                        ois.close();
                        socket.close();
                        System.out.println("stängde ois och socket");
                    } catch (IOException ex) {
                        ex.printStackTrace();
                    }
                } catch (ClassNotFoundException e) {
                    e.printStackTrace();
                }
            }
        }
    }
}
