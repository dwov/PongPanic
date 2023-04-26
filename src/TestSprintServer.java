import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.util.TimerTask;

public class TestSprintServer {
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
    private Object lock = new Object();

    public TestSprintServer(int ESPort, int androidPort){
        this.ESPort = ESPort;
        this.androidPort = androidPort;
        new Thread(new ESConnection()).start();

        highScore[0][0] = "Test1";
        highScore[0][1] = "25";
        highScore[1][0] = "Test2";
        highScore[1][1] = "20";
        highScore[2][0] = "Test3";
        highScore[2][1] = "15";

        //new Thread(new AndroidConnection()).start();
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

                    /*gameThread = new Thread(new GameThread());
                    gameThread.start();*/

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
                    } else if (inputLine.startsWith("timer")) {
                        String[] array = inputLine.split(":");
                        boolean ballCaught = game.bounce(Integer.parseInt(array[1]));
                        if (ballCaught) {
                            //stringBuffer.put(game.getCurrentPositionString());
                            stringBuffer.put(game.getCurrentPositionString());
                            System.out.println(array[1]);
                        } else {

                        }
                    }
                    synchronized (lock) {
                        if (!game.isAtEnd()) {
                            lock.notifyAll();
                        }
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
                    Thread.sleep(1000);
                    game.updatePosition();
                    if (game.getCurrentPosition().y == 0 || game.getCurrentPosition().y == 9) {
                        game.setAtEnd(true);
                    }
                    stringBuffer.put(game.getCurrentPositionString());
                    synchronized (lock) {
                        while (game.isAtEnd()) {
                            lock.wait();
                        }
                    }
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
            String inputLine;
            int startCount = 0;
            while(!androidReaderThread.isInterrupted()) {
                try {
                    inputLine = (String) ois.readObject();
                    if (inputLine.equals("start")) {
                        startCount++;
                        System.out.println(startCount + " start mottaget");
                        if (startCount == 2) {
                            System.out.println("Start om 5 sekunder");
                            Thread.sleep(5000);
                            gameThread = new Thread(new GameThread());
                            gameThread.start();
                            startCount = 0;
                        }
                    } else {
                        if (startCount == 0) {
                            game.getP1().setName(inputLine);
                            System.out.println("Player 1: " + inputLine);
                        } else if (startCount == 1) {
                            game.getP2().setName(inputLine);
                            System.out.println("Player 2: " + inputLine);
                        }
                    }
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
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }
    }
}
