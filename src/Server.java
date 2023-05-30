import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.util.LinkedList;
import java.util.Timer;
import java.util.TimerTask;

/**
 * This class handles a pong game with connected embedded systems and android clients.
 * The Server class contains several inner classes to control separate threads which reads
 * and writes data to the different devices, among other things.
 *
 * @author Tilde Lundqvist & Samuel Palmhager
 */
public class Server {
    private int ESPort;
    private int androidPort;
    private Game game = new Game();
    private HighScore highScore = new HighScore();
    private Thread coordinateThread;
    private Thread ESWriterThread;
    private Thread ESReaderThread;
    private Thread androidWriterThread;
    private Thread androidReaderThread;
    private Object lock = new Object();
    private Thread numberSenderThread;
    private LinkedList<ESWriter> esWriters = new LinkedList<>();
    private LinkedList<AndroidWriter> androidWriters = new LinkedList<>();
    private int startCount = 0;

    /**
     * Creates a Server object with ESPort and androidPort as connection points and
     * starts threads handling the connection to embedded systems as well as android clients.
     * @param ESPort the port which the embedded systems should connect to
     * @param androidPort the port which the android clients should connect to
     */
    public Server(int ESPort, int androidPort){
        this.ESPort = ESPort;
        this.androidPort = androidPort;

        coordinateThread = new Thread(new CoordinateSender());

        new Thread(new ESConnection()).start();
        new Thread(new AndroidConnection()).start();
    }

    /**
     * This class handles connection to embedded systems and starts relating reader- and writer threads.
     */
    public class ESConnection implements Runnable {
        @Override
        public void run() {
            Socket socket = null;
            ServerSocket serverSocket = null;
            try {
                serverSocket = new ServerSocket(ESPort);
                System.out.println("IS-server startad");
                while (true) {
                    socket = serverSocket.accept();
                    System.out.println("IS-klient ansluten");

                    ESWriter esWriter = new ESWriter(socket);
                    esWriters.add(esWriter);

                    ESReader esReader = new ESReader(socket, esWriter);

                    ESReaderThread = new Thread(esReader);
                    ESReaderThread.start();

                    ESWriterThread = new Thread(esWriter);
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

    /**
     * This class writes data to embedded systems via a buffer.
     */
    public class ESWriter implements Runnable {
        private Socket socket;
        private PrintWriter out;
        private Buffer<String> stringBuffer = new Buffer<>();
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
                }
            } catch (InterruptedException e) {
                System.out.println("buffern blev interrupted");
                esWriters.remove(this);
                System.out.println("tog bort esWriter");
                System.out.println("IP: " + socket.getInetAddress().getHostAddress());
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
        public synchronized void putInBuffer(String string) {
            stringBuffer.put(string);
        }
    }

    /**
     * This class reads data from embedded systems and handles it accordingly.
     */
    public class ESReader implements Runnable {
        private Socket socket;
        private BufferedReader in;
        private java.util.Timer timer = new java.util.Timer();
        private ESWriter esWriter;

        public ESReader(Socket socket, ESWriter esWriter) {
            this.socket = socket;
            this.esWriter = esWriter;
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
                        System.out.println("Läste heartbeat: " + socket.getInetAddress().getHostAddress());
                        timer.cancel();
                        timer = new Timer();
                        timer.schedule(new TimerTask() {
                            @Override
                            public void run() {
                                try {
                                    esWriters.remove(esWriter);
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
                        System.out.println("timer: " + array[1]);
                        if (game.getCurrentPosition().y == 0 || game.getCurrentPosition().y == 9) {
                            boolean ballCaught = game.bounce(Integer.parseInt(array[1]));
                            if (ballCaught) {
                                for (ESWriter esw : esWriters) {
                                    esw.putInBuffer("reset");
                                }
                                for (ESWriter esw : esWriters) {
                                    esw.putInBuffer(game.getCurrentPositionString());
                                }
                                if (game.getCurrentPosition().y == 1) {
                                    for (AndroidWriter aw : androidWriters) {
                                        aw.putInBuffer(game.getP1().toStringArray());
                                    }
                                    System.out.println("Skickade player 1");
                                } else {
                                    for (AndroidWriter aw : androidWriters) {
                                        aw.putInBuffer(game.getP2().toStringArray());
                                    }
                                    System.out.println("Skickade player 2");
                                }
                            } else {
                                for (ESWriter esw : esWriters) {
                                    esw.putInBuffer("reset");
                                }
                                String[][] score = new String[1][2];
                                if (game.getCurrentPosition().y == 0) {
                                    game.getP2().setWinner(true);
                                    sendFigure(FigureArrays.getPlayer2happy());
                                    sendFigure(FigureArrays.getPlayer1sad());
                                    for (AndroidWriter aw : androidWriters) {
                                        aw.putInBuffer(game.getP2().toStringArray());
                                    }
                                    score[0][0] = game.getP2().getName();
                                    score[0][1] = game.getP2().getPoints() + "";
                                } else {
                                    game.getP1().setWinner(true);
                                    sendFigure(FigureArrays.getPlayer1happy());
                                    sendFigure(FigureArrays.getPlayer2sad());
                                    for (AndroidWriter aw : androidWriters) {
                                        aw.putInBuffer(game.getP1().toStringArray());
                                    }
                                    score[0][0] = game.getP1().getName();
                                    score[0][1] = game.getP1().getPoints() + "";
                                }
                                System.out.println("Skickade vinnare");
                                highScore.addHighScore(score);
                                System.out.println("Uppdaterade highscorelista");
                                coordinateThread.interrupt();
                                System.out.println("speltråd interrupted");

                                androidWriters.clear();

                                Thread playerNumberThread = new Thread(new ShowPlayerNumber());
                                playerNumberThread.start();
                            }
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

    /**
     * This class waits ten seconds before sending player number to embedded systems.
     */
    public class ShowPlayerNumber implements Runnable {
        @Override
        public void run() {
            try {
                Thread.sleep(10000);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
            for (ESWriter esw : esWriters) {
                esw.putInBuffer("reset");
            }
            sendFigure(FigureArrays.getPlayer1number1());
            sendFigure(FigureArrays.getPlayer2number2());
        }
    }

    /**
     * This class updates the pong ball's position and sends it to the embedded systems
     * with a certain delay in between each coordinate.
     */
    public class CoordinateSender implements Runnable {
        @Override
        public void run() {
            try {
                while(!coordinateThread.isInterrupted()) {
                    int delay = game.getDelay();
                    Thread.sleep(delay);
                    game.updatePosition();
                    for (ESWriter esw : esWriters) {
                        esw.putInBuffer("reset");
                    }
                    for (ESWriter esw : esWriters) {
                        esw.putInBuffer(game.getCurrentPositionString());
                    }
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

    /**
     * This class handles connection to android clients and starts relating reader- and writer threads.
     */
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

                    AndroidWriter androidWriter = new AndroidWriter(socket);
                    androidWriters.add(androidWriter);
                    androidWriterThread = new Thread(androidWriter);
                    androidWriterThread.start();

                    androidReaderThread = new Thread(new AndroidReader(socket, androidWriter));
                    androidReaderThread.start();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * This class writes data to android clients via a buffer.
     */
    public class AndroidWriter implements Runnable {
        private Socket socket;
        private ObjectOutputStream oos;
        private Buffer<Object> objectBuffer = new Buffer<>();

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
            putInBuffer(highScore.getHighScore());
        }

        @Override
        public void run() {
            try {
                while (true) {
                    try {
                        oos.writeObject(objectBuffer.get());
                    } catch (SocketException e) {
                        oos.close();
                        socket.close();
                        System.out.println("stängde oos och socket");
                    } catch (InterruptedException e) {
                        e.printStackTrace();
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
        public void putInBuffer(Object object) {
            objectBuffer.put(object);
        }
    }

    /**
     * This class reads data from android clients and handles it accordingly.
     */
    public class AndroidReader implements Runnable {
        private Socket socket;
        private ObjectInputStream ois;
        private AndroidWriter androidWriter;
        private java.util.Timer timer1 = new java.util.Timer();
        private java.util.Timer timer2 = new java.util.Timer();

        public AndroidReader(Socket socket, AndroidWriter androidWriter) {
            this.socket = socket;
            this.androidWriter = androidWriter;
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
            while(!androidReaderThread.isInterrupted()) {
                try {
                    inputLine = (String) ois.readObject();
                    if (inputLine.startsWith("start:")) {
                        System.out.println("Tog emot start");
                        String[] array = inputLine.split(":");
                        if (array[1].equals(game.getP1().getName())) {
                            System.out.println("Player 1: " + inputLine);
                            timer1.cancel();
                            timer1 = new java.util.Timer();
                            System.out.println("Avslutade timer1");
                        } else {
                            System.out.println("Player 2: " + inputLine);
                            timer2.cancel();
                            timer2 = new java.util.Timer();
                            System.out.println("Avslutade timer2");
                        }
                        startCount++;
                        if (startCount == 2) {
                            numberSenderThread = new Thread(new NumberSender());
                            numberSenderThread.start();
                            System.out.println("Startade nedräkning");
                            startCount = 0;
                        }
                    } else {
                        System.out.println("Tog emot namn");
                        if (game.getP1().getName() == null) {
                            System.out.println("Player 1: " + inputLine);
                            game.getP1().setName(inputLine);

                            for (AndroidWriter aw : androidWriters) {
                                aw.putInBuffer(game.getP1().toStringArray());
                            }

                            System.out.println("Startar timer");
                            timer1.cancel();
                            timer1 = new java.util.Timer();
                            timer1.schedule(new TimerTask() {
                                @Override
                                public void run() {
                                    for (AndroidWriter aw : androidWriters) {
                                        aw.putInBuffer(game.getP1().getName());
                                    }
                                    game.getP1().setName(null);
                                    System.out.println("Timer1 out");
                                }
                            }, 10000);
                        } else {
                            System.out.println("Player 2: " + inputLine);
                            game.getP2().setName(inputLine);

                            for (AndroidWriter aw : androidWriters) {
                                aw.putInBuffer(game.getP2().toStringArray());
                            }

                            System.out.println("Startar timer");
                            timer2.cancel();
                            timer2 = new java.util.Timer();
                            timer2.schedule(new TimerTask() {
                                @Override
                                public void run() {
                                    for (AndroidWriter aw : androidWriters) {
                                        aw.putInBuffer(game.getP2().getName());
                                    }
                                    game.getP2().setName(null);
                                    System.out.println("Timer2 out");
                                }
                            }, 10000);
                        }
                    }
                } catch (IOException e) {
                    System.out.println("readObject interrupted");
                    try {
                        androidWriters.remove(androidWriter);
                        System.out.println("tog bort androidWriter");
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

    /**
     * This class starts a countdown from nine to zero and sends corresponding coordinates to embedded systems.
     * After countdown a CoordinateSender object is created and thread started to begin game.
     */
    public class NumberSender implements Runnable {

        @Override
        public void run() {
            try{
                //reset
                for (ESWriter aw : esWriters) {
                    aw.putInBuffer("reset");
                }

                //nummer 9
                sendFigure(FigureArrays.getPlayer1number9(), FigureArrays.getPlayer2number9());
                Thread.sleep(1100);
                for (ESWriter esw : esWriters) {
                    esw.putInBuffer("reset");
                }

                //nummer 8
                sendFigure(FigureArrays.getPlayer1number8(), FigureArrays.getPlayer2number8());
                Thread.sleep(1100);
                for (ESWriter esw : esWriters) {
                    esw.putInBuffer("reset");
                }

                //nummer 7
                sendFigure(FigureArrays.getPlayer1number7(), FigureArrays.getPlayer2number7());
                Thread.sleep(1100);
                for (ESWriter esw : esWriters) {
                    esw.putInBuffer("reset");
                }

                //nummer 6
                sendFigure(FigureArrays.getPlayer1number6(), FigureArrays.getPlayer2number6());
                Thread.sleep(1100);
                for (ESWriter esw : esWriters) {
                    esw.putInBuffer("reset");
                }

                //nummer 5
                sendFigure(FigureArrays.getPlayer1number5(), FigureArrays.getPlayer2number5());
                Thread.sleep(1100);
                for (ESWriter esw : esWriters) {
                    esw.putInBuffer("reset");
                }

                //nummer 4
                sendFigure(FigureArrays.getPlayer1number4(), FigureArrays.getPlayer2number4());
                Thread.sleep(1100);
                for (ESWriter esw : esWriters) {
                    esw.putInBuffer("reset");
                }

                //nummer 3
                sendFigure(FigureArrays.getPlayer1number3(), FigureArrays.getPlayer2number3());
                Thread.sleep(1100);
                for (ESWriter esw : esWriters) {
                    esw.putInBuffer("reset");
                }

                //nummer 2
                sendFigure(FigureArrays.getPlayer1number2(), FigureArrays.getPlayer2number2());
                Thread.sleep(1100);
                for (ESWriter esw : esWriters) {
                    esw.putInBuffer("reset");
                }

                //nummer 1
                sendFigure(FigureArrays.getPlayer1number1(), FigureArrays.getPlayer2number1());
                Thread.sleep(1100);
                for (ESWriter esw : esWriters) {
                    esw.putInBuffer("reset");
                }

                //nummer 0
                sendFigure(FigureArrays.getPlayer1number0(), FigureArrays.getPlayer2number0());
                Thread.sleep(1100);
                for (ESWriter esw : esWriters) {
                    esw.putInBuffer("reset");
                }

                //skicka start till klienter
                for (AndroidWriter aw : androidWriters) {
                    aw.putInBuffer("start");
                }
                System.out.println("Skickade start");

                //starta speltråd
                coordinateThread = new Thread(new CoordinateSender());
                coordinateThread.start();
                System.out.println("speltråd startad");
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * This method sends coordinates from list1 and list2 to embedded systems.
     * @param list1 the first list of coordinates
     * @param list2 the second list of coordinates
     */
    private void sendFigure(String[] list1, String[] list2){
        for (ESWriter esw : esWriters) {
            for (int i = 0; i < list1.length; i++) {
                esw.putInBuffer(list1[i]);
                esw.putInBuffer(list2[i]);
            }
        }
    }

    /**
     * This method sends coordinates from list to embedded systems.
     * @param list the list of coordinates
     */
    private void sendFigure(String[] list){
        for (ESWriter esw : esWriters) {
            for (int i = 0; i < list.length; i++) {
                esw.putInBuffer(list[i]);
            }
        }
    }
}
