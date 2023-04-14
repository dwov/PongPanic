import java.io.*;
import java.net.*;
import java.util.*;

/**
 * This is the backup of the Sprint 1 demo :)
 */
public class TestServer {
    private int port;
    private Game game = new Game();

    public TestServer(int port){
        this.port = port;
        new Thread(new Connection()).start();
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
        private PrintWriter out;
        private volatile boolean isRunning = true;
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
                while (isRunning) {
                    Thread.sleep(1000); // wait for 1 second
                    game.updatePosition();
                    String currentPositionString = game.getCurrentPositionString();
                    if (!socket.isClosed()) {
                        out.println(currentPositionString);
                        System.out.println("Skrev koordinat: " + currentPositionString);
                    } else {
                        isRunning = false;
                        System.out.println("Writer : STOP OCH BELÄGG");
                    }
                }
            } catch (InterruptedException e) {
                e.printStackTrace();
            } finally {
                out.close();
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
        private BufferedReader in;
        private Timer timer = new Timer();
        private volatile boolean isRunning = true;

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
                while (isRunning) {
                    try {
                        inputLine = in.readLine();
                    } catch (SocketException e) {
                        isRunning = false;
                        System.out.println("Reader : STOPP OCH BELÄGG");
                        break;
                    }
                    if (inputLine.equals("heartbeat")) {
                        System.out.println("Läste heartbeat: " + inputLine);
                        timer.cancel();
                        timer = new Timer();
                        timer.schedule(new TimerTask() {
                            @Override
                            public void run() {
                                try {
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
            } finally {
                try {
                    in.close();
                    socket.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }
}
