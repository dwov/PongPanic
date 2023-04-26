import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;

public class TestServer4 {
    private int port;
    private Game game = new Game();
    private Buffer<String> stringBuffer = new Buffer<>();
    private final Object lock = new Object();

    public TestServer4(int port){
        this.port = port;
        new Thread(new Connection()).start();
        try {
            Thread.sleep(3000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        new Thread(new GameThread()).start();
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
                        game.bounce(Integer.parseInt(array[1]));
                        stringBuffer.put(game.getCurrentPositionString());
                        stringBuffer.put(game.getCurrentPositionString());
                        System.out.println("BOUNCE");
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
                    Thread.sleep(500);
                    game.updatePosition();
                    if (game.getCurrentPosition().y == 0 || game.getCurrentPosition().y == 9) {
                        game.setAtEnd(true);
                    }
                    stringBuffer.put(game.getCurrentPositionString());
                    stringBuffer.put(game.getCurrentPositionString());
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
}
