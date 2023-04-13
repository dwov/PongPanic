import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.util.Timer;

public class TestServer {
    private int port;
    private Game game = new Game();
    private final long heartbeatInterval = 5000; // 5 seconds
    private long lastHeartbeatTime;
    private Thread thread;

    public TestServer(int port) {
        this.port = port;
        new Thread(new ESConnection()).start();
        this.lastHeartbeatTime = System.currentTimeMillis();
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
                ServerSocket serverSocket = new ServerSocket(port);
                System.out.println("IS-Server startad");
                while (true) {
                    socket = serverSocket.accept();
                    System.out.println("IS-Klient ansluten");
                    socket.setSoTimeout(10000);
                    new Thread(new ESWriter(socket)).start();
                    thread = new Thread(new ESReader(socket));
                    thread.start();
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
        public ESWriter(Socket socket) throws SocketException {
            this.socket = socket;
                socket.setSoTimeout(10000);
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
            try {
                while(true) {
                    Thread.sleep(1000);
                    game.updatePosition(); //ta bort
                    String currentPosition = game.getCurrentPositionString();
                    out.println(currentPosition);
                    System.out.println(currentPosition);
                }
            } catch (InterruptedException | RuntimeException e) {
                e.printStackTrace();
                try {
                    socket.close();
                } catch (IOException ex) {
                    ex.printStackTrace();
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
            String line;
            try {
                while(true) {
                    try {
                        if (in.ready()) {
                            line = in.readLine();
                            if (line.equals("heartbeat")) {
                                lastHeartbeatTime = System.currentTimeMillis();
                            }
                            System.out.println("Läste sträng: " + line);
                            if (System.currentTimeMillis() - lastHeartbeatTime > heartbeatInterval) {
                                System.out.println("Timer har gått ut");
                                Thread.sleep(3000);
                                try {
                                    System.out.println("Stänger socket");
                                    socket.close();
                                } catch (Exception e) {
                                    throw new RuntimeException(e);
                                }
                            }
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }
}
