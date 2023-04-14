import java.io.*;
import java.net.*;
import java.util.*;

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
        Socket socket;
            try {
                ServerSocket serverSocket = new ServerSocket(port);
                System.out.println("Server startad");
                while (true) {
                    socket = serverSocket.accept();
                    System.out.println("Klient ansluten");
                    ESWriter writer = new ESWriter(socket);
                    new Thread(writer).start();
                    new Thread(new ESReader(socket, writer));
                }
            } catch(IOException e){
                e.getStackTrace();
            }
        }
    }

    public class ESWriter implements Runnable {
        private Socket socket;
        private PrintWriter out;
        public ESWriter(Socket socket){
            this.socket = socket;
            try{
                out = new PrintWriter(socket.getOutputStream());
                System.out.println("Writer startad");
            } catch (IOException e) {
                e.getStackTrace();
            }
        }
        @Override
        public void run() {
            try {
                while (!socket.isClosed()) {
                    Thread.sleep(1000);
                    game.updatePosition();
                    out.println(game.getCurrentPositionString());
                    System.out.println("Skickade koordinat: " + game.getCurrentPositionString());
                }
            } catch (InterruptedException | RuntimeException e) {
                e.getStackTrace();
                try {
                    socket.close();
                } catch (IOException ex) {
                    ex.printStackTrace();
                }
            }
        }

        public void closeSocket() {
            System.out.println("I closeSocket-metod");
            try {
                socket.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
    public class ESReader implements Runnable {
        private Socket socket;
        private BufferedReader in;
        private ESWriter writer;
        private Timer timer = new Timer();
        private TimerTask task = new TimerTask() {
            @Override
            public void run() {
                counter++;
                if (counter >= 5) {
                    try {
                        socket.close();
                        writer.closeSocket();
                        System.out.println("Stängde socket");
                        timer.cancel();
                        System.out.println("Stängde av timer");
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }
        };
        private int counter = 0;

        public ESReader(Socket socket, ESWriter writer) {
            timer.schedule(task, 0, 1000);
            this.socket = socket;
            this.writer = writer;
            try {
                in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                System.out.println("Reader startad");
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
            String line;
            try {
                while(!socket.isClosed()) {
                    line = in.readLine();
                    if (line.equals("heartbeat")) {
                        counter = 0;
                    }
                    System.out.println("Läste sträng: " + line);
                }
            } catch (IOException | RuntimeException e) {
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
