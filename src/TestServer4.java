import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.util.LinkedList;

public class TestServer4 {
    private int port;
    private Game game = new Game();
    private final Object lock = new Object();
    private Thread gameThread;
    private int startCount = 0;
    private int nameCount = 0;
    private FigureArrays figureArrays = new FigureArrays();
    private LinkedList<ESWriter> esWriters = new LinkedList<>();

    public TestServer4(int port){
        this.port = port;
        new Thread(new Connection()).start();
        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
       // new TestClient(game);
       // new TestClient(game);
    }

    public class Connection implements Runnable {
        @Override
        public void run() {
        Socket socket = null;
        ServerSocket serverSocket = null;
            try {
                serverSocket = new ServerSocket(port);
                System.out.println("Server startad");
                while (true) {
                    socket = serverSocket.accept();
                    System.out.println("Klient ansluten: " + socket.getInetAddress());

                    ESReader reader = new ESReader(socket);
                    ESWriter writer = new ESWriter(socket);

                    esWriters.add(writer);

                    Thread readerThread = new Thread(reader);
                    readerThread.start();

                    Thread writerThread = new Thread(writer);
                    writerThread.start();
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
        private ObjectOutputStream oos;
        private Buffer<String> stringBuffer = new Buffer<>();
        private volatile boolean isRunning = true;
        public ESWriter(Socket socket){
            this.socket = socket;
            try{
                oos = new ObjectOutputStream(socket.getOutputStream());
            } catch (IOException e) {
                e.getStackTrace();
            }
        }
        @Override
        public void run() {
            try {
                while (isRunning) {
                    if (!socket.isClosed()) {
                        String currentPosition = stringBuffer.get();
                        oos.writeObject(currentPosition);
                        System.out.println("Skrev koordinat: " + currentPosition);
                    } else {
                        isRunning = false;
                        System.out.println("Writer : STOP OCH BELÄGG");
                        esWriters.remove(this);
                    }
                }
            } catch (InterruptedException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            } finally {
                try {
                    oos.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
                try {
                    socket.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
        public void send(String string) {
            stringBuffer.put(string);
        }
    }

    public class ESReader implements Runnable {
        private Socket socket;
        private ObjectInputStream ois;
        private volatile boolean isRunning = true;

        public ESReader(Socket socket) {
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
            try {
                String inputLine = "";
                while (isRunning) {
                    try {
                        inputLine = (String) ois.readObject();
                    } catch (SocketException e) {
                        isRunning = false;
                        System.out.println("Reader : STOPP OCH BELÄGG");
                        break;
                    } catch (ClassNotFoundException e) {
                        e.printStackTrace();
                    }
                    if (inputLine.startsWith("timer")) {
                        String[] array = inputLine.split(":");
                        boolean ballCaught = game.bounce(Integer.parseInt(array[1]));
                        if (ballCaught) {
                            for (ESWriter esw : esWriters) {
                                esw.send("reset");
                            }
                            for (ESWriter esw : esWriters) {
                                esw.send(game.getCurrentPositionString());
                            }
                            System.out.println("BOUNCE");
                            if (game.getCurrentPosition().y == 0) {
                                for (ESWriter esw : esWriters) {
                                    esw.send("points:" + game.getP1().getPoints() + ":" + game.getCurrentPosition().y);
                                }
                                System.out.println("Skickade player 1");
                            } else {
                                for (ESWriter esw : esWriters) {
                                    esw.send("points:" + game.getP2().getPoints() + ":" + game.getCurrentPosition().y);
                                }
                                System.out.println("Skickade player 1");
                            }
                        } else {

                        }
                    } else if (inputLine.equals("start")) {
                        startCount++;
                        if (startCount == 2) {
                            System.out.println("start");
                            new Thread(new NumberSender()).start();
                            Thread.sleep(10000);
                            gameThread = new Thread(new GameThread());
                            //gameThread.start();
                            startCount = 0;
                        }
                    } else {
                        nameCount++;
                        if (nameCount == 1) {
                            game.getP1().setName(inputLine);
                        } else if (nameCount == 2) {
                            game.getP2().setName(inputLine);
                            nameCount = 0;
                        }
                        System.out.println(inputLine);
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
            } catch (InterruptedException e) {
                e.printStackTrace();
            } finally {
                try {
                    ois.close();
                    socket.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }
    public class GameThread implements Runnable {
        @Override
        public void run() {
            try {
                while(true) {
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
                e.printStackTrace();
            }
        }
    }
    public class NumberSender implements Runnable{
        @Override
        public void run() {
            try{
                //nummer 9
                sendNumber(figureArrays.getPlayer1number9());
                sendNumber(figureArrays.getPlayer2number9());
                Thread.sleep(1000);
                for (ESWriter esw : esWriters) {
                    esw.send("reset");
                }

                //nummer 8
                sendNumber(figureArrays.getPlayer1number8());
                sendNumber(figureArrays.getPlayer2number8());
                Thread.sleep(1000);
                for (ESWriter esw : esWriters) {
                    esw.send("reset");
                }

                //nummer 7
                sendNumber(figureArrays.getPlayer1number7());
                sendNumber(figureArrays.getPlayer2number7());
                Thread.sleep(1000);
                for (ESWriter esw : esWriters) {
                    esw.send("reset");
                }

                //nummer 6
                sendNumber(figureArrays.getPlayer1number6());
                sendNumber(figureArrays.getPlayer2number6());
                Thread.sleep(1000);
                for (ESWriter esw : esWriters) {
                    esw.send("reset");
                }

                //nummer 5
                sendNumber(figureArrays.getPlayer1number5());
                sendNumber(figureArrays.getPlayer2number5());
                Thread.sleep(1000);
                for (ESWriter esw : esWriters) {
                    esw.send("reset");
                }

                //nummer 4
                sendNumber(figureArrays.getPlayer1number4());
                sendNumber(figureArrays.getPlayer2number4());
                Thread.sleep(1000);
                for (ESWriter esw : esWriters) {
                    esw.send("reset");
                }

                //nummer 3
                sendNumber(figureArrays.getPlayer1number3());
                sendNumber(figureArrays.getPlayer2number3());
                Thread.sleep(1000);
                for (ESWriter esw : esWriters) {
                    esw.send("reset");
                }

                //nummer 2
                sendNumber(figureArrays.getPlayer1number2());
                sendNumber(figureArrays.getPlayer2number2());
                Thread.sleep(1000);
                for (ESWriter esw : esWriters) {
                    esw.send("reset");
                }

                //nummer 1
                sendNumber(figureArrays.getPlayer1number1());
                sendNumber(figureArrays.getPlayer2number1());
                Thread.sleep(1000);
                for (ESWriter esw : esWriters) {
                    esw.send("reset");
                }

                //nummer 0
                sendNumber(figureArrays.getPlayer1number0());
                sendNumber(figureArrays.getPlayer2number0());
                Thread.sleep(1000);
                for (ESWriter esw : esWriters) {
                    esw.send("reset");
                }

                //glad
                sendNumber(figureArrays.getPlayer1happy());
                sendNumber(figureArrays.getPlayer2happy());
                Thread.sleep(1000);
                for (ESWriter esw : esWriters) {
                    esw.send("reset");
                }

                //ledsen
                sendNumber(figureArrays.getPlayer1sad());
                sendNumber(figureArrays.getPlayer2sad());
                Thread.sleep(1000);
                for (ESWriter esw : esWriters) {
                    esw.send("reset");
                }
            } catch(InterruptedException e){
                e.printStackTrace();
            }
        }

        private void sendNumber(String[] list){
            for (ESWriter esw : esWriters) {
                for (int i = 0; i < list.length; i++) {
                    esw.send(list[i]);
                }
            }
        }
    }
}
