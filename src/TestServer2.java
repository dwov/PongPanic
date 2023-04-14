import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Timer;
import java.util.TimerTask;

public class TestServer2 {

    public static void main(String[] args) {
        int port = 4567; // change this to the port number you want to use
        ServerSocket serverSocket = null;

        try {
            serverSocket = new ServerSocket(port);
            System.out.println("Waiting for clients...");

            while (true) {
                Socket clientSocket = serverSocket.accept();
                System.out.println("Client connected: " + clientSocket.getInetAddress());

                // create ESreader and ESwriter for this client
                ESreader reader = new ESreader(clientSocket);
                ESwriter writer = new ESwriter(clientSocket);

                // start the reader and writer threads
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

    // inner class for reading from the client socket
    private static class ESreader implements Runnable {
        private BufferedReader in;
        private Socket clientSocket;
        private Timer timer;

        public ESreader(Socket clientSocket) throws IOException {
            this.clientSocket = clientSocket;
            this.in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
            this.timer = new Timer();
        }

        @Override
        public void run() {
            try {
                String inputLine;
                while ((inputLine = in.readLine()) != null) {
                    if (inputLine.equals("heartbeat")) {
                        System.out.println("Read line: " + inputLine);
                        timer.cancel();
                        timer = new Timer();
                        timer.schedule(new TimerTask() {
                            @Override
                            public void run() {
                                try {
                                    clientSocket.close();
                                } catch (IOException e) {
                                    e.printStackTrace();
                                }
                            }
                        }, 5000);
                    }
                }
            } catch (IOException e) {
                e.printStackTrace();
            } finally {
                try {
                    clientSocket.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    // inner class for writing to the client socket
    private static class ESwriter implements Runnable {
        private PrintWriter out;
        private Socket clientSocket;
        private Game game;
        private volatile boolean isRunning = true;

        public ESwriter(Socket clientSocket) throws IOException {
            this.clientSocket = clientSocket;
            this.out = new PrintWriter(clientSocket.getOutputStream(), true);
            this.game = new Game();
        }

        @Override
        public void run() {
            try {
                while (isRunning) {
                    Thread.sleep(1000); // wait for 1 second
                    game.updatePosition();
                    String currentPositionString = game.getCurrentPositionString();
                    if (!clientSocket.isClosed()) {
                        out.println(currentPositionString);
                        System.out.println("Wrote coordinate: " + currentPositionString);
                    } else {
                        isRunning = false;
                        System.out.println("isRunning = false");
                    }
                }
            } catch (InterruptedException e) {
                e.printStackTrace();
            } finally {
                out.close();
                try {
                    clientSocket.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }
}




