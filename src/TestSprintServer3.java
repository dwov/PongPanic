import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.util.LinkedList;
import java.util.TimerTask;

public class TestSprintServer3 {
    private int ESPort;
    private int androidPort;
    private Game game = new Game();
    private HighScore highScore = new HighScore();
    private Thread gameThread;
    private Thread ESWriterThread;
    private Thread ESReaderThread;
    private Thread androidWriterThread;
    private Thread androidReaderThread;
    private Object lock = new Object();
    private int startCount = 0;
    private int nameCount = 0;
    private Thread numberSenderThread;
    private FigureArrays figureArrays = new FigureArrays();
    private LinkedList<ESWriter> esWriters = new LinkedList<>();
    private LinkedList<AndroidWriter> androidWriters = new LinkedList<>();

    public TestSprintServer3(int ESPort, int androidPort){
        this.ESPort = ESPort;
        this.androidPort = androidPort;

        gameThread = new Thread(new GameThread());

        new Thread(new ESConnection()).start();
        new Thread(new AndroidConnection()).start();
    }

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

                    sendFigure(figureArrays.getPlayer1number1());
                    sendFigure(figureArrays.getPlayer2number2());

                    ESReaderThread = new Thread(new ESReader(socket, esWriter));
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
                    System.out.println("Skrev koordinat: " + string);
                }
            } catch (InterruptedException e) {
                System.out.println("buffern blev interrupted");
                esWriters.remove(this);
                System.out.println("tog bort esWriter");
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
        public synchronized void send(String string) {
            stringBuffer.put(string);
        }
    }

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
                        System.out.println("Läste heartbeat");
                        timer.cancel();
                        timer = new java.util.Timer();
                        timer.schedule(new TimerTask() {
                            @Override
                            public void run() {
                                try {
                                    esWriters.remove(esWriter);
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
                        System.out.println("timer: " + array[1]);
                        if (game.getCurrentPosition().y == 0 || game.getCurrentPosition().y == 9) {
                            boolean ballCaught = game.bounce(Integer.parseInt(array[1]));
                            System.out.println(array[1]);
                            if (ballCaught) {
                                for (ESWriter esw : esWriters) {
                                    esw.send("reset");
                                }
                                for (ESWriter esw : esWriters) {
                                    esw.send(game.getCurrentPositionString());
                                }
                                if (game.getCurrentPosition().y == 1) {
                                    for (AndroidWriter aw : androidWriters) {
                                        aw.send(game.getP1().toStringArray());
                                    }
                                    System.out.println("Skickade player 1");
                                } else {
                                    for (AndroidWriter aw : androidWriters) {
                                        aw.send(game.getP2().toStringArray());
                                    }
                                    System.out.println("Skickade player 2");
                                }
                            } else {
                                for (ESWriter esw : esWriters) {
                                    esw.send("reset");
                                }
                                String[][] score = new String[1][2];
                                if (game.getCurrentPosition().y == 0) {
                                    game.getP2().setWinner(true);
                                    sendFigure(figureArrays.getPlayer2happy());
                                    sendFigure(figureArrays.getPlayer1sad());
                                    for (AndroidWriter aw : androidWriters) {
                                        aw.send(game.getP2().toStringArray());
                                    }
                                    score[0][0] = game.getP2().getName();
                                    score[0][1] = game.getP2().getPoints() + "";
                                } else {
                                    game.getP1().setWinner(true);
                                    sendFigure(figureArrays.getPlayer1happy());
                                    sendFigure(figureArrays.getPlayer2sad());
                                    for (AndroidWriter aw : androidWriters) {
                                        aw.send(game.getP1().toStringArray());
                                    }
                                    score[0][0] = game.getP1().getName();
                                    score[0][1] = game.getP1().getPoints() + "";
                                }
                                System.out.println("Skickade vinnare");
                                highScore.addHighScore(score);
                                System.out.println("Uppdaterade highscorelista");
                                gameThread.interrupt();
                                System.out.println("game-tråd interrupted");

                                try {
                                    Thread.sleep(5000);
                                } catch (InterruptedException e) {
                                    throw new RuntimeException(e);
                                }
                                for (ESWriter esw : esWriters) {
                                    esw.send("reset");
                                }
                                sendFigure(figureArrays.getPlayer1number1());
                                sendFigure(figureArrays.getPlayer2number2());
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
    public class GameThread implements Runnable {
        @Override
        public void run() {
            try {
                while(!gameThread.isInterrupted()) {
                    int delay = game.getDelay();
                    System.out.println("Delay: " + delay);
                    Thread.sleep(delay);
                    game.updatePosition();
                    for (ESWriter esw : esWriters) {
                        esw.send("reset");
                    }
                    for (ESWriter esw : esWriters) {
                        esw.send(game.getCurrentPositionString());
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
            send(highScore.getHighScore());
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
        public void send(Object object) {
            objectBuffer.put(object);
        }
    }

    public class AndroidReader implements Runnable {
        private Socket socket;
        private ObjectInputStream ois;
        private AndroidWriter androidWriter;

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
                    if (inputLine.equals("start")) {
                        startCount++;
                        System.out.println(startCount + " start mottaget");
                        if (startCount == 1) {
                            game.getP1().setPlayerNumber(startCount);
                            for (AndroidWriter aw : androidWriters) {
                                aw.send(game.getP1());
                            }
                            System.out.println("Skickade player 1");
                        } else if (startCount == 2) {
                            game.getP2().setPlayerNumber(startCount);
                            for (AndroidWriter aw : androidWriters) {
                                aw.send(game.getP2());
                            }
                            System.out.println("Skickade player 2");
                            try {
                                Thread.sleep(1000);
                            } catch(InterruptedException e) {
                                e.printStackTrace();
                            }
                            for (AndroidWriter aw : androidWriters) {
                                aw.send("start");
                            }
                            System.out.println("Skickade start");
                            for (ESWriter aw : esWriters) {
                                aw.send("reset");
                            }
                            numberSenderThread = new Thread(new NumberSender());
                            numberSenderThread.start();
                            System.out.println("Startade game-tråd");
                            startCount = 0;
                        }
                    } else {
                        nameCount++;
                        System.out.println(nameCount + " namn mottaget");
                        if (nameCount == 1) {
                            game.getP1().setName(inputLine);
                            System.out.println("Player 1: " + inputLine);
                        } else if (nameCount == 2) {
                            game.getP2().setName(inputLine);
                            System.out.println("Player 2: " + inputLine);
                            nameCount = 0;
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

    public class NumberSender implements Runnable {

        @Override
        public void run() {
            try{
                //nummer 9
                sendFigure(figureArrays.getPlayer1number9());
                sendFigure(figureArrays.getPlayer2number9());
                Thread.sleep(1000);
                for (ESWriter esw : esWriters) {
                    esw.send("reset");
                }

                //nummer 8
                sendFigure(figureArrays.getPlayer1number8());
                sendFigure(figureArrays.getPlayer2number8());
                Thread.sleep(1000);
                for (ESWriter esw : esWriters) {
                    esw.send("reset");
                }

                //nummer 7
                sendFigure(figureArrays.getPlayer1number7());
                sendFigure(figureArrays.getPlayer2number7());
                Thread.sleep(1000);
                for (ESWriter esw : esWriters) {
                    esw.send("reset");
                }

                //nummer 6
                sendFigure(figureArrays.getPlayer1number6());
                sendFigure(figureArrays.getPlayer2number6());
                Thread.sleep(1000);
                for (ESWriter esw : esWriters) {
                    esw.send("reset");
                }

                //nummer 5
                sendFigure(figureArrays.getPlayer1number5());
                sendFigure(figureArrays.getPlayer2number5());
                Thread.sleep(1000);
                for (ESWriter esw : esWriters) {
                    esw.send("reset");
                }

                //nummer 4
                sendFigure(figureArrays.getPlayer1number4());
                sendFigure(figureArrays.getPlayer2number4());
                Thread.sleep(1000);
                for (ESWriter esw : esWriters) {
                    esw.send("reset");
                }

                //nummer 3
                sendFigure(figureArrays.getPlayer1number3());
                sendFigure(figureArrays.getPlayer2number3());
                Thread.sleep(1000);
                for (ESWriter esw : esWriters) {
                    esw.send("reset");
                }

                //nummer 2
                sendFigure(figureArrays.getPlayer1number2());
                sendFigure(figureArrays.getPlayer2number2());
                Thread.sleep(1000);
                for (ESWriter esw : esWriters) {
                    esw.send("reset");
                }

                //nummer 1
                sendFigure(figureArrays.getPlayer1number1());
                sendFigure(figureArrays.getPlayer2number1());
                Thread.sleep(1000);
                for (ESWriter esw : esWriters) {
                    esw.send("reset");
                }

                //nummer 0
                sendFigure(figureArrays.getPlayer1number0());
                sendFigure(figureArrays.getPlayer2number0());
                Thread.sleep(1000);
                for (ESWriter esw : esWriters) {
                    esw.send("reset");
                }

                //starta game-tråd
                gameThread = new Thread(new GameThread());
                gameThread.start();
                System.out.println("GameThread startad");
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }
    private void sendFigure(String[] list){
        for (ESWriter esw : esWriters) {
            for (int i = 0; i < list.length; i++) {
                esw.send(list[i]);
            }
        }
    }
}
